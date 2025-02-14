package org.ual.spatialindex.rtree;

import org.ual.spatialindex.spatialindex.NodeData;
import org.ual.spatialindex.spatialindex.Region;

import java.util.*;

public class BulkLoader {

    public void bulkLoadUsingSTR(RTree rTree, ArrayList<RTree.Data> spatialData, int index, int leaf){
        if(spatialData.isEmpty())
            throw new IllegalArgumentException("BulkLoadUsingSTR: spatialData cannot be empty");

        // Read the root node and delete it.
        Node n = rTree.readNode(rTree.rootID);
        rTree.deleteNode(n);

        // Initialize the first sorter.
        ExternalSorter firstSorter = new ExternalSorter();

        // Insert all data entries into de first sorter (root)
        for(RTree.Data data : spatialData) {
            firstSorter.insert(new Record(data.shape, data.id, data.data, 0));
        }
        // Sort the first sorter.
        firstSorter.sort();

        // Update the statistics.
        rTree.stats.data = firstSorter.getTotalEntries();

        // Create the rest of the levels (leafs).
        int level = 0;

        // The first level is a leaf level.
        while (true) {
            rTree.stats.nodesInLevel.add(0);

            // Create a new sorter for the next level.
            ExternalSorter secondSorter = new ExternalSorter();
            // Create the next level.
            createLevel(rTree, firstSorter, 0, leaf, index, level++, secondSorter);
            // Overwrite the first sorter with the second sorter.
            firstSorter = secondSorter;

            // If the first sorter has only one entry, then we are done.
            if (firstSorter.getTotalEntries() == 1)
                break;

            // Sort the first sorter.
            firstSorter.sort();
        }

        // Update the statistics with the tree height.
        rTree.stats.treeHeight = level;
    }


    protected void createLevel(RTree rTree, ExternalSorter firstSorter, int dimension, int leaf, int index, int level,
            ExternalSorter secondSorter) {
        // b is set to leaf or index depending on the level.
        int b = (level == 0) ? leaf : index; // b = branching factor.
        // P = number of nodes in this level, calculated by the total number of entries divided by the branching factor.
        int P = (int)(Math.ceil((double)(firstSorter.getTotalEntries()) / (double)(b))); // P = number of nodes in this level.
        // S = number of nodes in a stripe, calculated by the square root of P.
        int S = (int)(Math.ceil(Math.sqrt(P))); // S = number of nodes in a stripe.

        // Leaf Level or Last Dimension
        // If S is 1, the current dimension is the last dimension, or the total entries exactly fit into the nodes
        if (S == 1 || dimension == rTree.dimension - 1 || S * b == firstSorter.getTotalEntries()) {
            ArrayList<Record> nodeRecords = new ArrayList<>();
            Record record;

            // Iterate over all records in the first sorter, adding them to the node records.
            while (true) {
                // Get the next record.
                record = firstSorter.getNextRecord();
                if (record == null)
                    break;

                // Add the record to the node records.
                nodeRecords.add(record);

                // If the node size is equal to the branching factor, then create a new node.
                if (nodeRecords.size() == b) {
                    Node node = createNode(rTree, nodeRecords, level);
                    nodeRecords.clear();
                    rTree.writeNode(node);
                    secondSorter.insert(new Record(node.nodeMBR, node.identifier,null, 0));
                    rTree.rootID = node.identifier;
                    // special case when the root has exactly index entries.
                }
            }

            // If there are any remaining records, then create a new node.
            if (! nodeRecords.isEmpty()) {
                Node node = createNode(rTree, nodeRecords, level);
                rTree.writeNode(node);
                secondSorter.insert(new Record(node.nodeMBR, node.identifier, null, 0));
                rTree.rootID = node.identifier;
            }
        } else {
            // If the conditions for direct processing are not met, create levels recursively. (not root or leaf)
            boolean more = true;

            while(more) {
                Record record;
                ExternalSorter thirdSorter = new ExternalSorter();

                // Create the stripes.
                for (int i = 0; i < S * b; ++i) {
                    record = firstSorter.getNextRecord();
                    if (record == null) {
                        more = false;
                        break;
                    }

                    record.sortingDimension = dimension + 1;
                    thirdSorter.insert(record);
                }
                thirdSorter.sort();
                createLevel(rTree, thirdSorter, dimension + 1, leaf, index, level, secondSorter);
            }
        }
    }


    protected Node createNode(RTree rTree, ArrayList<Record> records, int level) {
        Node node;

        // Create a leaf or an index node.
        if (level == 0)
            node = new Leaf(rTree, -1);
        else
            node = new Index(rTree, -1, level);

        // Insert all records into the node.
        for (Record record : records) {
            node.insertEntry(record.nodeData, record.region, record.id);
        }

        records.clear();  // clear the records vector.

        return node;
    }


    protected class ExternalSorter {
        private boolean insertionPhase;
        private LinkedList<Record> sortedRecords = new LinkedList<>();
        private ArrayList<Record> buffer = new ArrayList<>();//vector<Record>
        private int totalEntries;
        private int stI;

        public ExternalSorter() {
            this.insertionPhase = true;
            this.totalEntries = 0;
            this.stI = 0;
        }

        public void insert(Record record) {
            if (insertionPhase == false)
                throw new IllegalStateException("ExternalSorter::insert: Input has already been sorted.");

            buffer.add(record); // add the record to the buffer.
            ++totalEntries; // increment the total number of entries.
        }

        public void sort() {
            if (insertionPhase == false)
                throw new IllegalStateException("ExternalSorter.sort: Input has already been sorted.");

            // The data fits in main memory. No need to store to disk.
            Collections.sort(buffer);
            insertionPhase = false;
        }

        public Record getNextRecord() {
            if (insertionPhase == true)
                throw new IllegalStateException("ExternalSorter.getNextRecord: Input has not been sorted yet.");

            Record ret;

            if (sortedRecords.peek() == null) {
                if (stI < buffer.size()) {
                    ret = buffer.get(stI);
                    buffer.set(stI, null);
                    ++stI;
                } else {
                    ret = null;
                }
            } else {
                ret = sortedRecords.poll();
            }

            return ret;
        }

        public int getTotalEntries() {
            return totalEntries;
        }
    }


    protected class Record implements Comparable<Record> {
        public Region region;
        public int id; //id_type
        public int length;
        public NodeData nodeData;
        public int sortingDimension; //sorting dimension (m_s)


        public Record(Region region, int id, NodeData nodeData, int sortingDimension) {
            this.region = region;
            this.id = id;
            this.nodeData = nodeData; //pointer to data!
            this.sortingDimension = sortingDimension;
        }

        @Override
        public int compareTo(Record record) {
            if (sortingDimension != record.sortingDimension)
                throw new IllegalStateException("ExternalSorter::Record::compareTo: Incompatible sorting dimensions.");

            if (region.high[sortingDimension] + region.low[sortingDimension] < record.region.high[sortingDimension] + record.region.low[sortingDimension])
                return -1;
            else if (region.high[sortingDimension] + region.low[sortingDimension] > record.region.high[sortingDimension] + record.region.low[sortingDimension])
                return 1;
            return 0;
        }
    }
}
