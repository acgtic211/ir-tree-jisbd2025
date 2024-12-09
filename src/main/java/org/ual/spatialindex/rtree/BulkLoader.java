package org.ual.spatialindex.rtree;

import org.ual.spatialindex.spatialindex.NodeData;
import org.ual.spatialindex.spatialindex.Region;

import java.util.*;

public class BulkLoader {

    public void bulkLoadUsingSTR(RTree rTree, ArrayList<RTree.Data> streamData, int index, int leaf, int pageSize, int numberOfPages){
        Iterator<RTree.Data> stream = streamData.iterator();

        if (! stream.hasNext())
            throw new IllegalArgumentException("RTree::BulkLoader::bulkLoadUsingSTR: Empty data stream given.");

        Node n = rTree.readNode(rTree.rootID);
        rTree.deleteNode(n);

        ExternalSorter es = new ExternalSorter(pageSize, numberOfPages);

        while (stream.hasNext()) {
            RTree.Data d = stream.next();
            if (d == null)
                throw new IllegalArgumentException("bulkLoadUsingSTR: RTree bulk load expects SpatialIndex::RTree::Data entries.");

            es.insert(new Record(d.shape, d.id, d.data, 0));
        }
        es.sort();

        rTree.stats.data = es.getTotalEntries();

        // create index levels.
        int level = 0;

        while (true) {
            rTree.stats.nodesInLevel.add(0);

            ExternalSorter es2 = new ExternalSorter(pageSize, numberOfPages);
            createLevel(rTree, es, 0, leaf, index, level++, es2, pageSize, numberOfPages);
            es = es2;

            if (es.getTotalEntries() == 1)
                break;

            es.sort();
        }

        rTree.stats.treeHeight = level;
        //rTree.storeHeader();
    }


    protected void createLevel(RTree rTree, ExternalSorter es, int dimension, int leaf, int index, int level,
            ExternalSorter es2, int pageSize, int numberOfPages) {
        int b = (level == 0) ? leaf : index; // b = branching factor.
        int P = (int)(Math.ceil((double)(es.getTotalEntries()) / (double)(b))); // P = number of nodes in this level.
        int S = (int)(Math.ceil(Math.sqrt((double)(P)))); // S = number of nodes in a stripe.

        if (S == 1 || dimension == rTree.dimension - 1 || S * b == es.getTotalEntries()) {
            ArrayList<Record> nodeRecords = new ArrayList<>();
            Record r;

            while (true) {
                r = es.getNextRecord();
                if (r == null)
                    break;

                nodeRecords.add(r);

                if (nodeRecords.size() == b) {
                    Node n = createNode(rTree, nodeRecords, level);
                    nodeRecords.clear();
                    rTree.writeNode(n);
                    es2.insert(new Record(n.nodeMBR, n.identifier,null, 0));
                    rTree.rootID = n.identifier;
                    // special case when the root has exactly index entries.
                }
            }

            if (! nodeRecords.isEmpty()) {
                Node n = createNode(rTree, nodeRecords, level);
                rTree.writeNode(n);
                es2.insert(new Record(n.nodeMBR, n.identifier, null, 0));
                rTree.rootID = n.identifier;
            }
        } else {
            boolean more = true;

            while(more) {
                Record pR;
                ExternalSorter es3 = new ExternalSorter(pageSize, numberOfPages);

                for (int i = 0; i < S * b; ++i) {
                    pR = es.getNextRecord();
                    if (pR == null) {
                        more = false;
                        break;
                    }

                    pR.m_s = dimension + 1;
                    es3.insert(pR);
                }
                es3.sort();
                createLevel(rTree, es3, dimension + 1, leaf, index, level, es2, pageSize, numberOfPages);
            }
        }
    }


    protected Node createNode(RTree rTree, ArrayList<Record> e, int level) {
        Node n;

        if (level == 0)
            n = new Leaf(rTree, -1);
        else
            n = new Index(rTree, -1, level);

        for (int cChild = 0; cChild < e.size(); ++cChild) {
            n.insertEntry(e.get(cChild).m_pData, e.get(cChild).m_r, e.get(cChild).m_id);
        }

        e.clear();

        return n;
    }

    protected class TemporaryStorage {
        private LinkedList<Record> m_buffer;

        TemporaryStorage() {
            m_buffer = new LinkedList<>();
        }

        public boolean eof() {
            return m_buffer.isEmpty();
        }

        public Record get() {
            return m_buffer.poll();
        }
    }


    protected class ExternalSorter {
        private boolean m_bInsertionPhase;
        private int m_pageSize;
        private int m_bufferPages;
        private TemporaryStorage m_sortedFile = new TemporaryStorage();
        private ArrayList<TemporaryStorage> m_runs =  new ArrayList<>();
        private ArrayList<Record> m_buffer = new ArrayList<>();//vector<Record>
        private int m_totalEntries;
        private int m_stI;

        public ExternalSorter(int u32PageSize, int u32BufferPages) {
            this.m_bInsertionPhase = true;
            this.m_pageSize = u32PageSize;
            this.m_bufferPages = u32BufferPages;
            this.m_totalEntries = 0;
            this.m_stI = 0;
        }

        public void insert(Record r) {
            if (m_bInsertionPhase == false)
                throw new IllegalStateException("ExternalSorter::insert: Input has already been sorted.");

            m_buffer.add(r);
            ++m_totalEntries;

            // this will create the initial, sorted buckets before the
            // external merge sort.
            if (m_buffer.size() >= m_pageSize * m_bufferPages) {
                Collections.sort(m_buffer);
                TemporaryStorage tf = new TemporaryStorage();
                for (int j = 0; j < m_buffer.size(); ++j) {
                    m_buffer.get(j).storeToFile(tf);
                }
                m_buffer.clear();
                m_runs.add(tf);
            }
        }

        public void sort() {
            if (m_bInsertionPhase == false)
                throw new IllegalStateException("ExternalSorter::sort: Input has already been sorted.");

            if (m_runs.isEmpty()) {
                // The data fits in main memory. No need to store to disk.
                Collections.sort(m_buffer);
                m_bInsertionPhase = false;
                return;
            }

            if (m_buffer.size() > 0) {
                // Whatever remained in the buffer (if not filled) needs to be stored
                // as the final bucket.
                Collections.sort(m_buffer);
                TemporaryStorage tf = new TemporaryStorage();
                for (int j = 0; j < m_buffer.size(); ++j) {
                    m_buffer.get(j).storeToFile(tf);
                }
                m_buffer.clear();
                m_runs.add(tf);
            }

            if (m_runs.size() == 1) {
                m_sortedFile = m_runs.get(0);
            } else {
                Record r = null;

                while (m_runs.size() > 1) {
                    TemporaryStorage tf = new TemporaryStorage();
                    ArrayList<TemporaryStorage> buckets = new ArrayList<>(); // vector
                    ArrayList<Queue<Record>> buffers = new ArrayList<>(); // vector
                    PriorityQueue<PQEntry> pq = new PriorityQueue<>(); //vector

                    // initialize buffers and priority queue.
                    Iterator<TemporaryStorage> it = m_runs.iterator();//begin();
                    int counter = 0;

                    while (it.hasNext()) {
                        TemporaryStorage ts = it.next();//new
                        buckets.add(ts);
                        buffers.add(new LinkedList<>());//Queue<Record>());

                        r = new Record();
                        r.loadFromFile(ts);//it);
                        // a run cannot be empty initially, so this should never fail.
                        pq.add(new PQEntry(r, counter));

                        for (int j = 0; j < m_pageSize - 1; ++j) {
                            // fill the buffer with the rest of the page of records.
                            r = new Record();
                            if(r.loadFromFile(ts))
                                break;
                            buffers.get(buffers.size() - 1).add(r);
                        }
                        counter++;
                    }

                    // exhaust buckets, buffers, and priority queue.
                    while (! pq.isEmpty()) {
                        PQEntry e = pq.poll();
                        e.m_r.storeToFile(tf);

                        if (! buckets.get(e.m_index).eof() && buffers.get(e.m_index).isEmpty()) {
                            for (int j = 0; j < m_pageSize; ++j) {
                                r = new Record();
                                if(r.loadFromFile(buckets.get(e.m_index)))
                                    break;
                                buffers.get(e.m_index).add(r);
                            }
                        }

                        if (! buffers.get(e.m_index).isEmpty()) {
                            e.m_r = buffers.get(e.m_index).poll();
                            pq.add(e);
                        }
                    }

                    // check if another pass is needed.
                    int count = Math.min((int)(m_runs.size()), m_bufferPages);
                    for (int i = 0; i < count; ++i) {
                        m_runs.remove(0);
                    }

                    if (m_runs.size() == 0) {
                        m_sortedFile = tf;
                        break;
                    } else {
                        m_runs.add(tf);
                    }
                }
            }

            m_bInsertionPhase = false;
        }

        public Record getNextRecord() {
            if (m_bInsertionPhase == true)
                throw new IllegalStateException("ExternalSorter::getNextRecord: Input has not been sorted yet.");

            Record ret;

            if (m_sortedFile.get() == null) {
                if (m_stI < m_buffer.size()) {
                    ret = m_buffer.get(m_stI);
                    m_buffer.set(m_stI, null);
                    ++m_stI;
                } else {
                    ret = null;
                }
            } else {
                ret = new Record();
                ret.loadFromFile(m_sortedFile);
            }

            return ret;
        }

        public int getTotalEntries() {
            return m_totalEntries;
        }




    }

    private class PQEntry implements Comparable<PQEntry> {
        public Record m_r;
        public int m_index;

        public PQEntry(Record r, int u32Index) /*: m_r(r), m_u32Index(u32Index) */{
            this.m_r = r;
            this.m_index = u32Index;
        }


        @Override
        public int compareTo(PQEntry o) {
            if(m_r.compareTo(o.m_r) < 0)
                return -1;
            else if(m_r.compareTo(o.m_r) > 0)
                return 1;
            else
                return 0;
        }
    }


    protected class Record implements Comparable<Record> {
        public Region m_r;
        public int m_id; //id_type
        public int m_len;
        public NodeData m_pData;
        public int m_s;


        public Record() {}


        public Record(Region r, int id, NodeData pData, int s) {
            this.m_r = r;
            this.m_id = id;
            this.m_pData = pData; //pointer to data!
            this.m_s = s;
        }

        public void storeToFile(TemporaryStorage f) {
            Record r = new Record(m_r, m_id, m_pData, m_s);
            f.m_buffer.add(r);
        }

        public /*void*/boolean loadFromFile(TemporaryStorage f) {
            Record rec = f.get();

            if(rec == null)
                return false;
            else {
                m_id = rec.m_id;
                m_s = rec.m_s;
                m_r = rec.m_r;
                m_len = rec.m_len;
                m_pData = rec.m_pData;
                return true;
            }
        }

        @Override
        public int compareTo(Record r) {
            if (m_s != r.m_s)
                throw new IllegalStateException("ExternalSorter::Record::compareTo: Incompatible sorting dimensions.");

            if (m_r.high[m_s] + m_r.low[m_s] < r.m_r.high[m_s] + r.m_r.low[m_s])
                return -1;
            else if (m_r.high[m_s] + m_r.low[m_s] > r.m_r.high[m_s] + r.m_r.low[m_s])
                return 1;
            return 0;
        }
    }
}
