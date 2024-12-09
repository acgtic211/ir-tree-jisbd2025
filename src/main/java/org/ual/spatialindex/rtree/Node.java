package org.ual.spatialindex.rtree;

import org.ual.spatialindex.spatialindex.*;

import java.io.*;
import java.util.*;

public abstract class Node implements INode { //implements INode
    protected RTree rTree;
    // Parent of all nodes.

    public int level = -1;
    // The level of the node in the tree.
    // Leaves are always at level 0.

    public int identifier = -1;
    // The unique ID of this node.

    public int children = 0;
    // The number of children pointed by this node.

    protected int capacity = -1;
    // Specifies the node capacity.

    protected Region nodeMBR;
    // The minimum bounding region enclosing all data contained in the node.

    //protected byte[][] data = null;
    protected NodeData nodeData;
    // The data stored in the node.

    public Region[] mbr;
    // The corresponding data MBRs.

    public int[] identifiers;
    // The corresponding data identifiers.

    protected int[] dataLength;

    int totalDataLength = 0;

    // TODO TEST
    public int type;


    //
    // Abstract methods
    //

    protected abstract Node chooseSubtree(Region mbr, int level, Stack<Integer> pathBuffer);

    protected abstract Leaf findLeaf(Region mbr, int id, Stack<Integer> pathBuffer);

    protected abstract Node[] split(NodeData data, Region mbr, int id);

    //
    // IEntry interface
    //

    public int getIdentifier() {
        return identifier;
    }

    public IShape getShape() {
        return (IShape) nodeMBR.clone();
    }

    //
    // INode interface
    //

    public int getChildrenCount() {
        return children;
    }

    public int getChildIdentifier(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= children)
            throw new IndexOutOfBoundsException("" + index);

        return identifiers[index];
    }

    public IShape getChildShape(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= children)
            throw new IndexOutOfBoundsException("" + index);

        return new Region(mbr[index]);
    }

    public int getLevel() {
        return level;
    }

    public boolean isLeaf() {
        return (level == 0);
    }

    public boolean isIndex() {
        return (level != 0);
    }

    //
    // Internal
    //

    protected Node(RTree rTree, int id, int level, int capacity) {
        this.rTree = rTree;
        this.level = level;
        this.identifier = id;
        this.capacity = capacity;
        this.nodeMBR = (Region) rTree.infiniteRegion.clone();

        this.dataLength = new int[this.capacity + 1];
        this.nodeData = new NodeData(this.capacity + 1);
        this.mbr = new Region[this.capacity + 1];
        this.identifiers = new int[this.capacity + 1];
    }


    protected void insertEntry(NodeData entryData, Region mbr, int id) throws IllegalStateException {
        if (children >= capacity)
            throw new IllegalStateException("m_children >= m_nodeCapacity");

        if (entryData != null) {
            dataLength[children] = entryData.data.length;
            //nodeData.data = new ArrayList<>(entryData.data);
            nodeData.data = entryData.data;
        } else {
            dataLength[children] = 0;
        }

        //dataLength[children] = (entryData != null) ? entryData.data.length : 0;
        //data.data.set(children, entryData);
        //data.data = entryData.data;
        this.mbr[children] = mbr;
        identifiers[children] = id;

        totalDataLength += dataLength[children];
        children++;

        Region.combinedRegion(nodeMBR, mbr);
    }


    protected void deleteEntry(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= children)
            throw new IndexOutOfBoundsException("" + index);

        boolean touches = nodeMBR.touches(mbr[index]);

        totalDataLength -= dataLength[index];
        nodeData.data[index] = null;

        if (children > 1 && index != children - 1) {
            dataLength[index] = dataLength[children - 1];
            //nodeData.data.set(index, nodeData.data.get(children - 1));
            //nodeData.data.set(children - 1, null);
            nodeData.data[index] = nodeData.data[children - 1];
            nodeData.data[children - 1] = null;
            mbr[index] = mbr[children - 1];
            mbr[children - 1] = null;
            identifiers[index] = identifiers[children - 1];
        }

        children--;

        if (children == 0) {
            nodeMBR = (Region) rTree.infiniteRegion.clone();
        } else if (touches) {
            for (int dim = 0; dim < rTree.dimension; dim++) {
                nodeMBR.low[dim] = Double.POSITIVE_INFINITY;
                nodeMBR.high[dim] = Double.NEGATIVE_INFINITY;

                for (int child = 0; child < children; child++) {
                    nodeMBR.low[dim] = Math.min(nodeMBR.low[dim], mbr[child].low[dim]);
                    nodeMBR.high[dim] = Math.max(nodeMBR.high[dim], mbr[child].high[dim]);
                }
            }
        }
    }


    protected boolean insertData(NodeData data, Region mbr, int id, Stack<Integer> pathBuffer, boolean[] overflowTable) {
        if (children < capacity) {
            boolean adjusted = false;
            boolean containsMBR = nodeMBR.contains(mbr);

            insertEntry(data, mbr, id);
            rTree.writeNode(this);

            if (!containsMBR && !pathBuffer.empty()) {
                int parent = pathBuffer.pop();
                Index p = (Index) rTree.readNode(parent);
                p.adjustTree(this, pathBuffer);
                adjusted = true;
            }

            return adjusted;
        } else if (rTree.treeVariant == SpatialIndex.RtreeVariantRstar && !pathBuffer.empty()
                && !overflowTable[level]) {
            overflowTable[level] = true;

            ArrayList<Integer> reinsertList = new ArrayList<>(), keepList = new ArrayList<>();
            reinsertData(data, mbr, id, reinsertList, keepList);

            int reinsertSize = reinsertList.size();
            int keepSize = keepList.size();

            NodeData reinsertData = new NodeData(reinsertSize);
            Region[] reinsertMbr = new Region[reinsertSize];
            int[] reinsertId = new int[reinsertSize];
            int[] reinsertLen = new int[reinsertSize];
            NodeData keepData = new NodeData(capacity + 1);
            Region[] keepMbr = new Region[capacity + 1];
            int[] keepId = new int[capacity + 1];
            int[] keepLen = new int[capacity + 1];

            int index;

            for (index = 0; index < reinsertSize; index++) {
                int i = reinsertList.get(index);
                reinsertLen[index] = dataLength[i];
                reinsertData.data[index] = this.nodeData.data[i];
                //reinsertData.data.set(index, this.nodeData.data.get(i));
                reinsertMbr[index] = this.mbr[i];
                reinsertId[index] = identifiers[i];
            }

            for (index = 0; index < keepSize; index++) {
                int i = keepList.get(index);
                keepLen[index] = dataLength[i];
                //keepData.data.set(index, this.nodeData.data.get(i));
                keepData.data[index] = this.nodeData.data[i];
                keepMbr[index] = this.mbr[i];
                keepId[index] = identifiers[i];
            }

            dataLength = keepLen;
            this.nodeData = keepData;
            this.mbr = keepMbr;
            identifiers = keepId;
            children = keepSize;
            totalDataLength = 0;
            for (int child = 0; child < children; child++)
                totalDataLength += dataLength[child];

            for (int dim = 0; dim < rTree.dimension; dim++) {
                nodeMBR.low[dim] = Double.POSITIVE_INFINITY;
                nodeMBR.high[dim] = Double.NEGATIVE_INFINITY;

                for (int child = 0; child < children; child++) {
                    nodeMBR.low[dim] = Math.min(nodeMBR.low[dim], this.mbr[child].low[dim]);
                    nodeMBR.high[dim] = Math.max(nodeMBR.high[dim], this.mbr[child].high[dim]);
                }
            }

            rTree.writeNode(this);

            // Divertion from R*-Tree algorithm here. First adjust
            // the path to the root, then start reinserts, to avoid complicated
            // handling  of changes to the same node from multiple insertions.
            int parent = pathBuffer.pop();
            Index idx = (Index) rTree.readNode(parent);
            idx.adjustTree(this, pathBuffer);

            for (index = 0; index < reinsertSize; index++) {
                rTree.insertDataImpl(reinsertData.data[index], reinsertMbr[index], reinsertId[index], level,
                        overflowTable);
            }

            return true;
        } else {
            Node[] nodes = split(data, mbr, id);
            Node n = nodes[0];
            Node nn = nodes[1];

            if (pathBuffer.empty()) {
                n.identifier = -1;
                nn.identifier = -1;
                rTree.writeNode(n);
                rTree.writeNode(nn);

                Index index = new Index(rTree, rTree.rootID, level + 1);

                index.insertEntry(null, (Region) n.nodeMBR.clone(), n.identifier);
                index.insertEntry(null, (Region) nn.nodeMBR.clone(), nn.identifier);

                rTree.writeNode(index);

                rTree.stats.nodesInLevel.set(level, 2);
                rTree.stats.nodesInLevel.add(1);
                rTree.stats.treeHeight = level + 2;
            } else {
                n.identifier = identifier;
                nn.identifier = -1;

                rTree.writeNode(n);
                rTree.writeNode(nn);

                int parent = pathBuffer.pop();
                Index index = (Index) rTree.readNode(parent);
                index.adjustTree(n, nn, pathBuffer, overflowTable);
            }

            return true;
        }
    }


    protected void reinsertData(NodeData data, Region mbr, int id, ArrayList<Integer> reinsert, ArrayList<Integer> keep) {
        Node.ReinsertEntry[] reinsertEntries = new Node.ReinsertEntry[capacity + 1];

        dataLength[children] = (data != null) ? data.data.length : 0;
        this.nodeData.data[children] = data;
        this.mbr[children] = mbr;
        identifiers[children] = id;

        double[] nodeMBRCenter = nodeMBR.getCenter();

        for (int child = 0; child < capacity + 1; child++) {
            Node.ReinsertEntry e = new Node.ReinsertEntry(child, 0.0f);

            double[] center = this.mbr[child].getCenter();

            // calculate relative distance of every entry from the node MBR
            // (ignore square root.)
            for (int dim = 0; dim < rTree.dimension; dim++) {
                double d = nodeMBRCenter[dim] - center[dim];
                e.dist += d * d;
            }

            reinsertEntries[child] = e;
        }

        // sort by increasing order of distances.
        Arrays.sort(reinsertEntries, new ReinsertEntryComparator());

        int reinsertSize = (int) Math.floor((capacity + 1) * rTree.reinsertFactor);
        int count;

        for (count = 0; count < reinsertSize; count++) {
            reinsert.add(reinsertEntries[count].id);
        }

        for (count = reinsertSize; count < capacity + 1; count++) {
            keep.add(reinsertEntries[count].id);
        }
    }

    //TODO
    protected void rtreeSplit(NodeData data, Region mbr, int id, ArrayList<Integer> group1, ArrayList<Integer> group2) {
        int child;
        int minimumLoad = (int) Math.floor(capacity * rTree.fillFactor);

        // use this mask array for marking visited entries.
        boolean[] mask = new boolean[capacity + 1];
        for (child = 0; child < capacity + 1; child++)
            mask[child] = false;

        // insert new data in the node for easier manipulation. Data arrays are always
        // by one larger than node capacity.
        dataLength[capacity] = (data != null) ? data.data.length : 0;
        this.nodeData.data[capacity] = data;
        this.mbr[capacity] = mbr;
        identifiers[capacity] = id;

        // initialize each group with the seed entries.
        int[] seeds = pickSeeds();

        group1.add(seeds[0]);
        group2.add(seeds[1]);

        mask[seeds[0]] = true;
        mask[seeds[1]] = true;

        // find MBR of each group.
        Region mbr1 = (Region) this.mbr[seeds[0]].clone();
        Region mbr2 = (Region) this.mbr[seeds[1]].clone();

        // count how many entries are left unchecked (exclude the seeds here.)
        int remaining = capacity + 1 - 2;

        while (remaining > 0) {
            if (minimumLoad - group1.size() == remaining) {
                // all remaining entries must be assigned to group1 to comply
                // with minimun load requirement.
                for (child = 0; child < capacity + 1; child++) {
                    if (!mask[child]) {
                        group1.add(child);
                        mask[child] = true;
                        remaining--;
                    }
                }
            } else if (minimumLoad - group2.size() == remaining) {
                // all remaining entries must be assigned to group2 to comply
                // with minimun load requirement.
                for (child = 0; child < capacity + 1; child++) {
                    if (!mask[child]) {
                        group2.add(child);
                        mask[child] = true;
                        remaining--;
                    }
                }
            } else {
                // For all remaining entries compute the difference of the cost of grouping an
                // entry in either group. When done, choose the entry that yielded the maximum
                // difference. In case of linear split, select any entry (e.g. the first one.)
                int sel = -1;
                double md1 = 0.0, md2 = 0.0;
                double m = Double.NEGATIVE_INFINITY;
                double d1, d2, d;
                double a1 = mbr1.getArea();
                double a2 = mbr2.getArea();

                for (child = 0; child < capacity + 1; child++) {
                    if (mask[child] == false) {
                        Region a = mbr1.combinedRegion(this.mbr[child]);
                        d1 = a.getArea() - a1;
                        Region b = mbr2.combinedRegion(this.mbr[child]);
                        d2 = b.getArea() - a2;
                        d = Math.abs(d1 - d2);

                        if (d > m) {
                            m = d;
                            md1 = d1;
                            md2 = d2;
                            sel = child;
                            if (rTree.treeVariant == SpatialIndex.RtreeVariantLinear
                                    || rTree.treeVariant == SpatialIndex.RtreeVariantRstar)
                                break;
                        }
                    }
                }

                // determine the group where we should add the new entry.
                int group = -1;

                if (md1 < md2) {
                    group1.add(sel);
                    group = 1;
                } else if (md2 < md1) {
                    group2.add(sel);
                    group = 2;
                } else if (a1 < a2) {
                    group1.add(sel);
                    group = 1;
                } else if (a2 < a1) {
                    group2.add(sel);
                    group = 2;
                } else if (group1.size() < group2.size()) {
                    group1.add(sel);
                    group = 1;
                } else if (group2.size() < group1.size()) {
                    group2.add(sel);
                    group = 2;
                } else {
                    group1.add(sel);
                    group = 1;
                }
                mask[sel] = true;
                remaining--;
                if (group == 1) {
                    Region.combinedRegion(mbr1, this.mbr[sel]);
                } else {
                    Region.combinedRegion(mbr2, this.mbr[sel]);
                }
            }
        }
    }


    protected void rstarSplit(NodeData data, Region mbr, int id, ArrayList<Integer> group1, ArrayList<Integer> group2) {
        RstarSplitEntry[] dataLow = new RstarSplitEntry[capacity + 1];
        RstarSplitEntry[] dataHigh = new RstarSplitEntry[capacity + 1];

        dataLength[children] = (data != null) ? data.data.length : 0;
        this.nodeData.data[capacity] = data;
        this.mbr[capacity] = mbr;
        identifiers[capacity] = id;

        int nodeSPF = (int) (Math.floor((capacity + 1) * rTree.splitDistributionFactor));
        int splitDistribution = (capacity + 1) - (2 * nodeSPF) + 2;

        int child, dim, index;

        for (child = 0; child < capacity + 1; child++) {
            RstarSplitEntry e = new RstarSplitEntry(this.mbr[child], child, 0);

            dataLow[child] = e;
            dataHigh[child] = e;
        }

        double minimumMargin = Double.POSITIVE_INFINITY;
        int splitAxis = -1;
        int sortOrder = -1;

        // chooseSplitAxis.
        for (dim = 0; dim < rTree.dimension; dim++) {
            Arrays.sort(dataLow, new RstarSplitEntryComparatorLow());
            Arrays.sort(dataHigh, new RstarSplitEntryComparatorHigh());

            // calculate sum of margins and overlap for all distributions.
            double marginL = 0.0f;
            double marginH = 0.0f;

            for (child = 1; child <= splitDistribution; child++) {
                int l = nodeSPF - 1 + child;

                Region[] tl1 = new Region[l];
                Region[] th1 = new Region[l];

                for (index = 0; index < l; index++) {
                    tl1[index] = dataLow[index].region;
                    th1[index] = dataHigh[index].region;
                }

                Region bbl1 = Region.combinedRegion(tl1);
                Region bbh1 = Region.combinedRegion(th1);

                Region[] tl2 = new Region[capacity + 1 - l];
                Region[] th2 = new Region[capacity + 1 - l];

                int tmpIndex = 0;
                for (index = l; index < capacity + 1; index++) {
                    tl2[tmpIndex] = dataLow[index].region;
                    th2[tmpIndex] = dataHigh[index].region;
                    tmpIndex++;
                }

                Region bbl2 = Region.combinedRegion(tl2);
                Region bbh2 = Region.combinedRegion(th2);

                marginL += bbl1.getMargin() + bbl2.getMargin();
                marginH += bbh1.getMargin() + bbh2.getMargin();
            } // for (cChild)

            double margin = Math.min(marginL, marginH);

            // keep minimum margin as split axis.
            if (margin < minimumMargin) {
                minimumMargin = margin;
                splitAxis = dim;
                sortOrder = (marginL < marginH) ? 0 : 1;
            }

            // increase the dimension according to which the data entries should
            // be sorted.
            for (child = 0; child < capacity + 1; child++) {
                dataLow[child].sortDim = dim + 1;
            }
        } // for (cDim)

        for (child = 0; child < capacity + 1; child++) {
            dataLow[child].sortDim = splitAxis;
        }

        if (sortOrder == 0)
            Arrays.sort(dataLow, new RstarSplitEntryComparatorLow());
        else
            Arrays.sort(dataLow, new RstarSplitEntryComparatorHigh());

        double ma = Double.POSITIVE_INFINITY;
        double mo = Double.POSITIVE_INFINITY;
        int splitPoint = -1;

        for (child = 1; child <= splitDistribution; child++) {
            int l = nodeSPF - 1 + child;

            Region[] t1 = new Region[l];

            for (index = 0; index < l; index++) {
                t1[index] = dataLow[index].region;
            }

            Region bb1 = Region.combinedRegion(t1);

            Region[] t2 = new Region[capacity + 1 - l];

            int tmpIndex = 0;
            for (index = l; index < capacity + 1; index++) {
                t2[tmpIndex] = dataLow[index].region;
                tmpIndex++;
            }

            Region bb2 = Region.combinedRegion(t2);

            double o = bb1.getIntersectingArea(bb2);

            if (o < mo) {
                splitPoint = child;
                mo = o;
                ma = bb1.getArea() + bb2.getArea();
            } else if (o == mo) {
                double a = bb1.getArea() + bb2.getArea();

                if (a < ma) {
                    splitPoint = child;
                    ma = a;
                }
            }
        } // for (cChild)

        int l1 = nodeSPF - 1 + splitPoint;

        for (index = 0; index < l1; index++) {
            group1.add(dataLow[index].id);
        }

        for (index = l1; index <= capacity; index++) {
            group2.add(dataLow[index].id);
        }
    }


    protected int[] pickSeeds() {
        double separation = Double.NEGATIVE_INFINITY;
        double inefficiency = Double.NEGATIVE_INFINITY;
        int dim, child, index, i1 = 0, i2 = 0;

        switch (rTree.treeVariant) {
            case SpatialIndex.RtreeVariantLinear:
            case SpatialIndex.RtreeVariantRstar:
                for (dim = 0; dim < rTree.dimension; dim++) {
                    double leastLower = mbr[0].low[dim];
                    double greatestUpper = mbr[0].high[dim];
                    int greatestLower = 0;
                    int leastUpper = 0;
                    double width;

                    for (child = 1; child < capacity + 1; child++) {
                        if (mbr[child].low[dim] > mbr[greatestLower].low[dim])
                            greatestLower = child;
                        if (mbr[child].high[dim] < mbr[leastUpper].high[dim])
                            leastUpper = child;

                        leastLower = Math.min(mbr[child].low[dim], leastLower);
                        greatestUpper = Math.max(mbr[child].high[dim], greatestUpper);
                    }

                    width = greatestUpper - leastLower;
                    if (width <= 0)
                        width = 1;

                    double f = (mbr[greatestLower].low[dim] - mbr[leastUpper].high[dim]) / width;

                    if (f > separation) {
                        i1 = leastUpper;
                        i2 = greatestLower;
                        separation = f;
                    }
                } // for (cDim)

                if (i1 == i2) {
                    i2 = (i2 != capacity) ? i2 + 1 : i2 - 1;
                }

                break;
            case SpatialIndex.RtreeVariantQuadratic:
                // for each pair of Regions (account for overflow Region too!)
                for (child = 0; child < capacity; child++) {
                    double a = mbr[child].getArea();

                    for (index = child + 1; index < capacity + 1; index++) {
                        // get the combined MBR of those two entries.
                        Region r = mbr[child].combinedRegion(mbr[index]);

                        // find the inefficiency of grouping these entries together.
                        double d = r.getArea() - a - mbr[index].getArea();

                        if (d > inefficiency) {
                            inefficiency = d;
                            i1 = child;
                            i2 = index;
                        }
                    } // for (cIndex)
                } // for (cChild)

                break;
            default:
                throw new IllegalStateException("Unknown RTree variant.");
        }

        int[] ret = new int[2];
        ret[0] = i1;
        ret[1] = i2;
        return ret;
    }

    protected void condenseTree(Stack<Node> toReinsert, Stack<Integer> pathBuffer) {
        int minimumLoad = (int) (Math.floor(capacity * rTree.fillFactor));

        if (pathBuffer.empty()) {
            // eliminate root if it has only one child.
            if (level != 0 && children == 1) {
                Node n = rTree.readNode(identifiers[0]);
                rTree.deleteNode(n);
                n.identifier = rTree.rootID;
                rTree.writeNode(n);

                rTree.stats.nodesInLevel.remove(rTree.stats.nodesInLevel.size() - 1);
                rTree.stats.treeHeight -= 1;
                // HACK: pending deleteNode for deleted child will decrease
                // nodesInLevel, later on.
                rTree.stats.nodesInLevel.set(rTree.stats.treeHeight - 1, 2);
            }
        } else {
            int cParent = pathBuffer.pop();
            Index p = (Index) rTree.readNode(cParent);

            // find the entry in the parent, that points to this node.
            int child;

            for (child = 0; child != p.children; child++) {
                if (p.identifiers[child] == identifier)
                    break;
            }

            if (children < minimumLoad) {
                // used space less than the minimum
                // 1. eliminate node entry from the parent. deleteEntry will fix
                // the parent's MBR.
                p.deleteEntry(child);
                // 2. add this node to the stack in order to reinsert its
                // entries.
                toReinsert.push(this);
            } else {
                // adjust the entry in 'p' to contain the new bounding region of
                // this node.
                p.mbr[child] = (Region) nodeMBR.clone();

                // global recalculation necessary since the MBR can only shrink
                // in size,
                // due to data removal.
                for (int dim = 0; dim < rTree.dimension; dim++) {
                    p.nodeMBR.low[dim] = Double.POSITIVE_INFINITY;
                    p.nodeMBR.high[dim] = Double.NEGATIVE_INFINITY;

                    for (int cChild = 0; cChild < p.children; cChild++) {
                        p.nodeMBR.low[dim] = Math.min(p.nodeMBR.low[dim], p.mbr[cChild].low[dim]);
                        p.nodeMBR.high[dim] = Math.max(p.nodeMBR.high[dim], p.mbr[cChild].high[dim]);
                    }
                }
            }

            // write parent node back to storage.
            rTree.writeNode(p);

            p.condenseTree(toReinsert, pathBuffer);
        }
    }

    // TODO CHECK LOGIC
    protected void load(Node data) throws IOException {
        nodeMBR = (Region) rTree.infiniteRegion.clone();

        // skip the node type information, it is not needed.
        //ds.readInt();

        level = data.level;
        children = data.children;

        for (int child = 0; child < children; child++) {
            mbr[child] = new Region();
            mbr[child].low = new double[rTree.dimension];
            mbr[child].high = new double[rTree.dimension];

            for (int dim = 0; dim < rTree.dimension; dim++) {
                mbr[child].low[dim] = data.mbr[child].low[dim];
                mbr[child].high[dim] = data.mbr[child].high[dim];
            }

            identifiers[child] = data.identifiers[child];

            dataLength[child] = data.dataLength[child];
            if (dataLength[child] > 0) {
                totalDataLength += dataLength[child];
                this.nodeData.data[child] = new NodeData(dataLength[child]);
                //ds.read(this.data[child]);
            } else {
                this.nodeData.data[child] = null;
            }

            Region.combinedRegion(nodeMBR, mbr[child]);
        }
    }

    // TODO PROBABLY ABSTRACT AND DEFERED IMPLEMENTATION (DELETE??)
    protected Node store() throws IOException {
//        ByteArrayOutputStream bs = new ByteArrayOutputStream();
//        DataOutputStream ds = new DataOutputStream(bs);

        if (level == 0)
            type = SpatialIndex.PersistentLeaf;
        else
            type = SpatialIndex.PersistentIndex;

        return this;

//        NodeNew node;// = new NodeNew();
//
//        int type;
//        if (level == 0)
//            type = SpatialIndex.PersistentLeaf;
//        else
//            type = SpatialIndex.PersistentIndex;
//        node.type = type;
//        ds.writeInt(type);
//
//        ds.writeInt(level);
//        ds.writeInt(children);
//
//        for (int cChild = 0; cChild < children; cChild++) {
//            for (int cDim = 0; cDim < rTree.dimension; cDim++) {
//                ds.writeDouble(mbr[cChild].low[cDim]);
//                ds.writeDouble(mbr[cChild].high[cDim]);
//            }
//
//            ds.writeInt(identifiers[cChild]);
//
//            ds.writeInt(dataLength[cChild]);
//            if (dataLength[cChild] > 0)
//                ds.write(data[cChild]);
//        }
//
//        ds.flush();
//        return bs.toByteArray();
    }

    /**
     * INNER CLASSES
     */


    class ReinsertEntry {
        int id;
        double dist;

        public ReinsertEntry(int id, double dist) {
            this.id = id;
            this.dist = dist;
        }
    } // ReinsertEntry

    class ReinsertEntryComparator implements Comparator<ReinsertEntry> {
        @Override
        public int compare(ReinsertEntry o1, ReinsertEntry o2) {
            if (o1.dist < o2.dist)
                return -1;
            if (o1.dist > o2.dist)
                return 1;
            return 0;
        }
    } // ReinsertEntryComparator

    class RstarSplitEntry {
        Region region;
        int id;
        int sortDim;

        RstarSplitEntry(Region r, int id, int dimension) {
            region = r;
            this.id = id;
            sortDim = dimension;
        }
    }

    class RstarSplitEntryComparatorLow implements Comparator<RstarSplitEntry> {
        @Override
        public int compare(RstarSplitEntry o1, RstarSplitEntry o2) {
            if (o1.region.low[o1.sortDim] < o2.region.low[o2.sortDim])
                return -1;
            if (o1.region.low[o1.sortDim] > o2.region.low[o2.sortDim])
                return 1;
            return 0;
        }
    }

    class RstarSplitEntryComparatorHigh implements Comparator<RstarSplitEntry> {
        @Override
        public int compare(RstarSplitEntry o1, RstarSplitEntry o2) {
            if (o1.region.high[o1.sortDim] < o2.region.high[o2.sortDim])
                return -1;
            if (o1.region.high[o1.sortDim] > o2.region.high[o2.sortDim])
                return 1;
            return 0;
        }
    }

    @Override
    public String toString() {
        return "Node{" +
                "rTree=" + rTree +
                ", level=" + level +
                ", identifier=" + identifier +
                ", children=" + children +
                ", capacity=" + capacity +
                ", nodeMBR=" + nodeMBR +
                ", nodeData=" + nodeData +
                ", mbr=" + Arrays.toString(mbr) +
                ", identifiers=" + Arrays.toString(identifiers) +
                ", dataLength=" + Arrays.toString(dataLength) +
                ", totalDataLength=" + totalDataLength +
                ", type=" + type +
                '}';
    }

    public String printNode() {
        StringBuilder sb = new StringBuilder();
        sb.append("Node{" + "\n" +
                "\t level=" + level + "\n" +
                "\t identifier=" + identifier + "\n" +
                "\t children=" + children + "\n" +
                "\t capacity=" + capacity + "\n" +
                "\t nodeMBR=" + nodeMBR + "\n" +
                "\t nodeData=" + nodeData + "\n" +
                "\t mbr=" + Arrays.toString(mbr) + "\n" +
                "\t identifiers=" + Arrays.toString(identifiers) + "\n" +
                "\t dataLength=" + Arrays.toString(dataLength) + "\n" +
                "\t totalDataLength=" + totalDataLength + "\n" +
                "\t type=" + type + "\n" +
                '}' + "\n");
        for (int i = 0; i < children; i++) {
            sb.append("Child " + i + ": " + mbr[i] + "\n");
        }
        return sb.toString();
    }
}


