package org.ual.spatialindex.rtree;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ual.documentindex.InvertedFile;
import org.ual.query.Query;
//import org.ual.spatialindex.parameters.Parameters;
import org.ual.spatialindex.parameters.DatasetParameters;
import org.ual.spatialindex.spatialindex.*;
import org.ual.spatialindex.storage.IStore;
import org.ual.spatialindex.storage.WeightEntry;
import org.ual.spatialindex.storagemanager.IStorageManager;
import org.ual.spatialindex.storagemanager.InvalidPageException;
import org.ual.spatialindex.storagemanager.PropertySet;

import java.io.*;
import java.util.*;

public class RTree implements ISpatialIndex {
    public static double alphaDistribution;
    public static int numOfClusters = 0;

    DatasetParameters datasetParameters;
    RWLock rwLock;
    IStorageManager storageManager;

    protected int rootID;
    int headerID;

    int treeVariant;

    double fillFactor;

    int indexCapacity;

    int leafCapacity;

    int nearMinimumOverlapFactor;
    // The R*-Tree 'p' constant, for calculating nearly minimum overlap cost.
    // [Beckmann, Kriegel, Schneider, Seeger 'The R*-tree: An efficient and
    // Robust Access Method
    // for Points and Rectangles, Section 4.1]

    double splitDistributionFactor;
    // The R*-Tree 'm' constant, for calculating spliting distributions.
    // [Beckmann, Kriegel, Schneider, Seeger 'The R*-tree: An efficient and
    // Robust Access Method
    // for Points and Rectangles, Section 4.2]

    double reinsertFactor;
    // The R*-Tree 'p' constant, for removing entries at reinserts.
    // [Beckmann, Kriegel, Schneider, Seeger 'The R*-tree: An efficient and
    // Robust Access Method
    // for Points and Rectangles, Section 4.3]

    int dimension;

    Region infiniteRegion;

    Statistics stats;

    public int numOfVisitedNodes;

    private static final Logger logger = LogManager.getLogger(RTree.class);

    ArrayList<INodeCommand> writeNodeCommands = new ArrayList<>();
    ArrayList<INodeCommand> readNodeCommands = new ArrayList<>();
    ArrayList<INodeCommand> deleteNodeCommands = new ArrayList<>();

    public ArrayList<Data> pseudoNodes =  new ArrayList<>();

    // Bulk loading methods enumeration.
    public enum BulkLoadMethod {
        BLM_STR
    }



    public RTree(PropertySet ps, IStorageManager sm, DatasetParameters datasetParameters) {
        this.datasetParameters = datasetParameters;
        rwLock = new RWLock();
        storageManager = sm;
        rootID = IStorageManager.NewPage;
        headerID = IStorageManager.NewPage;
        treeVariant = SpatialIndex.RtreeVariantQuadratic;
        fillFactor = 0.7f;
        indexCapacity = 100;
        leafCapacity = 100;
        nearMinimumOverlapFactor = 32;
        splitDistributionFactor = 0.4f;
        reinsertFactor = 0.3f;
        dimension = 2;

        infiniteRegion = new Region();
        stats = new Statistics();

        Object var = ps.getProperty("IndexIdentifier");
        if (var != null) {
            if (!(var instanceof Integer))
                throw new IllegalArgumentException("Property IndexIdentifier must an Integer");

            headerID = (Integer) var;
            try {
                initOld(ps);
            } catch (IOException e) {
                System.err.println(e);
                throw new IllegalStateException("initOld failed with IOException");
            }
        } else {
            try {
                initNew(ps);
            } catch (IOException e) {
                System.err.println(e);
                throw new IllegalStateException("initNew failed with IOException");
            }
            Integer i = headerID;
            ps.setProperty("IndexIdentifier", i);
        }
    }

    // New Constructor to in-memory IRTree// TODO remove
    public RTree(RTree rtree) {
        datasetParameters = rtree.datasetParameters;
        rwLock = rtree.rwLock;
        storageManager = rtree.storageManager;
        rootID = rtree.rootID;
        headerID = rtree.headerID;
        treeVariant = rtree.treeVariant;
        fillFactor = 0.7f;
        indexCapacity = 100;
        leafCapacity = 100;
        nearMinimumOverlapFactor = 32;
        splitDistributionFactor = 0.4f;
        reinsertFactor = 0.3f;
        dimension = 2;

        infiniteRegion = rtree.infiniteRegion;
        stats = rtree.stats;

    }

    //
    // ISpatialIndex interface
    //

    public void insertData(final NodeData data, final IShape shape, int id) {
        if (shape.getDimension() != dimension)
            throw new IllegalArgumentException("insertData: Shape has the wrong number of dimensions.");

        rwLock.writeLock();

        try {
            Region mbr = shape.getMBR();
            insertDataImpl(data, mbr, id);
        } finally {
            rwLock.writeUnlock();
        }
    }

    public boolean deleteData(final IShape shape, int id) {
        if (shape.getDimension() != dimension)
            throw new IllegalArgumentException("deleteData: Shape has the wrong number of dimensions.");

        rwLock.writeLock();

        try {
            Region mbr = shape.getMBR();
            return deleteDataImpl(mbr, id);
        } finally {
            rwLock.writeUnlock();
        }
    }

    public void containmentQuery(final IShape query, final IVisitor visitor) {
        if (query.getDimension() != dimension)
            throw new IllegalArgumentException("containmentQuery: Shape has the wrong number of dimensions.");
        rangeQuery(SpatialIndex.ContainmentQuery, query, visitor);
    }

    public void intersectionQuery(final IShape query, final IVisitor visitor) {
        if (query.getDimension() != dimension)
            throw new IllegalArgumentException("intersectionQuery: Shape has the wrong number of dimensions.");
        rangeQuery(SpatialIndex.IntersectionQuery, query, visitor);
    }

    public void pointLocationQuery(final IShape query, final IVisitor visitor) {
        if (query.getDimension() != dimension)
            throw new IllegalArgumentException("pointLocationQuery: Shape has the wrong number of dimensions.");

        Region region;
        if (query instanceof Point) {
            region = new Region((Point) query, (Point) query);
        } else if (query instanceof Region) {
            region = (Region) query;
        } else {
            throw new IllegalArgumentException("pointLocationQuery: IShape can be Point or Region only.");
        }

        rangeQuery(SpatialIndex.IntersectionQuery, region, visitor);
    }


    public void nearestNeighborQuery(int k, final IShape query, final IVisitor visitor,
                                     final INearestNeighborComparator nnc) {
        if (query.getDimension() != dimension)
            throw new IllegalArgumentException("nearestNeighborQuery: Shape has the wrong number of dimensions.");

        rwLock.readLock();

        try {
            // I need a priority queue here. It turns out that TreeSet sorts unique keys only and since I am
            // sorting according to distances, it is not assured that all distances will be unique. TreeMap
            // also sorts unique keys. Thus, I am simulating a priority queue using an ArrayList and binarySearch.
            ArrayList<NNEntry> queue = new ArrayList<>();

            Node node = readNode(rootID);
            queue.add(new NNEntry(node, 0.0));

            int count = 0;
            double kNearest = 0.0;

            while (!queue.isEmpty()) {
                NNEntry first = queue.remove(0);

                if (first.node instanceof Node) {
                    node = (Node) first.node;
                    visitor.visitNode((INode) node);

                    for (int child = 0; child < node.children; child++) {
                        IEntry entry;

                        if (node.level == 0) {
                            entry = new Data(node.nodeData.data[child], node.mbr[child], node.identifiers[child]);
                        } else {
                            entry = (IEntry) readNode(node.identifiers[child]);
                        }

                        NNEntry e2 = new NNEntry(entry, nnc.getMinimumDistance(query, entry));

                        // Why don't I use a TreeSet here? See comment above...
                        //TODO BIG BUG!!! For Comparator CONG use MinDist; Kamil cost...
                        //int loc = Collections.binarySearch(queue, e2, new NNEntryComparator());
                        int loc = Collections.binarySearch(queue, e2, new NNEntryComparatorMinDistance());
                        if (loc >= 0)
                            queue.add(loc, e2);
                        else
                            queue.add((-loc - 1), e2);
                    }
                } else {
                    // report all nearest neighbors with equal furthest distances.
                    // (neighbors can be more than k, if many happen to have the same
                    // furthest distance).
                    if (count >= k && first.cost > kNearest)
                        break;

                    visitor.visitData((IData) first.node);
                    stats.queryResults++;
                    count++;
                    kNearest = first.cost;
                }
            }
        } finally {
            rwLock.readUnlock();
        }
    }

    public void nearestNeighborQuery(int k, final IShape query, final IVisitor visitor) {
        if (query.getDimension() != dimension)
            throw new IllegalArgumentException("nearestNeighborQuery: Shape has the wrong number of dimensions.");
        NNComparator nnc = new NNComparator();
        nearestNeighborQuery(k, query, visitor, nnc);
    }

    public void queryStrategy(final IQueryStrategy queryStrategy) {
        rwLock.readLock();

        int[] next = new int[] { rootID };

        try {
            while (true) {
                Node n = readNode(next[0]);
                boolean[] hasNext = new boolean[] { false };
                queryStrategy.getNextEntry(n, next, hasNext);
                if (!hasNext[0])
                    break;
            }
        } finally {
            rwLock.readUnlock();
        }
    }

    public PropertySet getIndexProperties() {
        PropertySet ret = new PropertySet();

        // dimension
        ret.setProperty("Dimension", dimension);

        // index capacity
        ret.setProperty("IndexCapacity", indexCapacity);

        // leaf capacity
        ret.setProperty("LeafCapacity", leafCapacity);

        // R-tree variant
        ret.setProperty("TreeVariant", treeVariant);

        // fill factor
        ret.setProperty("FillFactor", fillFactor);

        // near minimum overlap factor
        ret.setProperty("NearMinimumOverlapFactor", nearMinimumOverlapFactor);

        // split distribution factor
        ret.setProperty("SplitDistributionFactor", splitDistributionFactor);

        // reinsert factor
        ret.setProperty("ReinsertFactor", reinsertFactor);

        return ret;
    }

    public void addWriteNodeCommand(INodeCommand nc) {
        writeNodeCommands.add(nc);
    }

    public void addReadNodeCommand(INodeCommand nc) {
        readNodeCommands.add(nc);
    }

    public void addDeleteNodeCommand(INodeCommand nc) {
        deleteNodeCommands.add(nc);
    }


    public boolean isIndexValid() {
        boolean ret = true;
        Stack<ValidateEntry> entryStack = new Stack<>();
        Node root = readNode(rootID);

        if (root.level != stats.treeHeight - 1) {
            System.err.println("Invalid tree height");
            return false;
        }

        HashMap<Integer, Integer> nodesInLevel = new HashMap<>();
        nodesInLevel.put(root.level, 1);

        ValidateEntry e = new ValidateEntry(root.nodeMBR, root);
        entryStack.push(e);

        while (!entryStack.empty()) {
            e = entryStack.pop();

            Region tmpRegion = (Region) infiniteRegion.clone();

            for (int dim = 0; dim < dimension; dim++) {
                tmpRegion.low[dim] = Double.POSITIVE_INFINITY;
                tmpRegion.high[dim] = Double.NEGATIVE_INFINITY;

                for (int child = 0; child < e.node.children; child++) {
                    tmpRegion.low[dim] = Math.min(tmpRegion.low[dim], e.node.mbr[child].low[dim]);
                    tmpRegion.high[dim] = Math.max(tmpRegion.high[dim], e.node.mbr[child].high[dim]);
                }
            }

            if (!(tmpRegion.equals(e.node.nodeMBR))) {
                System.err.println("Invalid parent information");
                ret = false;
            } else if (!(tmpRegion.equals(e.parentMBR))) {
                System.err.println("Error in parent");
                ret = false;
            }

            if (e.node.level != 0) {
                for (int child = 0; child < e.node.children; child++) {
                    ValidateEntry tmpEntry = new ValidateEntry(e.node.mbr[child],
                            readNode(e.node.identifiers[child]));

                    if (!nodesInLevel.containsKey(tmpEntry.node.level)) {
                        nodesInLevel.put(tmpEntry.node.level, 1);
                    } else {
                        int i = nodesInLevel.get(tmpEntry.node.level);
                        nodesInLevel.put(tmpEntry.node.level, i + 1);
                    }

                    entryStack.push(tmpEntry);
                }
            }
        }

        int nodes = 0;
        for (int cLevel = 0; cLevel < stats.treeHeight; cLevel++) {
            int i1 = ((Integer) nodesInLevel.get(new Integer(cLevel))).intValue();
            int i2 = ((Integer) stats.nodesInLevel.get(cLevel)).intValue();
            if (i1 != i2) {
                System.err.println("Invalid nodesInLevel information");
                ret = false;
            }

            nodes += i2;
        }

        if (nodes != stats.nodes) {
            System.err.println("Invalid number of nodes information");
            ret = false;
        }

        return ret;
    }

    public IStatistics getStatistics() {
        return (IStatistics) stats.clone();
    }

//    public void flush() throws IllegalStateException {
//        try {
//            storeHeader();
//            //storageManager.flush();
//        } catch (IOException e) {
//            System.err.println(e);
//            throw new IllegalStateException("flush failed with IOException");
//        }
//    }

    // TEST

    // New method to store pseudo-nodes for bulk loading
    public void storePseudoNodes(NodeData data, Region region, int id) {
        pseudoNodes.add(new Data(data, region, id));
    }

    // index and leaf capacity can be the same as the fanout for now
    public void bulkLoadRTree(BulkLoadMethod bulkLoadMethod, int indexCapacity, int leafCapacity) {
        int index = (int) Math.floor(indexCapacity * fillFactor);
        int leaf = (int) Math.floor(leafCapacity * fillFactor);

        if(bulkLoadMethod == BulkLoadMethod.BLM_STR) {
            BulkLoader bl = new BulkLoader();
            bl.bulkLoadUsingSTR(this, pseudoNodes, index, leaf);
        } else {
            logger.error("createAndBulkLoadNewRTree: Unknown bulk load method.");
            throw new IllegalArgumentException("createAndBulkLoadNewRTree: Unknown bulk load method.");
        }

    }

    // TODO DELETE TEST
    // index and leaf capacity can be the same as the fanout for now
    public void bulkLoadRTreeNEW(BulkLoadMethod bulkLoadMethod, int indexCapacity, int leafCapacity, int pageSize, int numberOfPages) {
        int index = (int) Math.floor(indexCapacity * fillFactor);
        int leaf = (int) Math.floor(leafCapacity * fillFactor);

        if(bulkLoadMethod == BulkLoadMethod.BLM_STR) {
            BulkLoaderNEW bl = new BulkLoaderNEW();
            bl.bulkLoadWithSTR(this, pseudoNodes, index, leaf/*, pageSize, numberOfPages*/);
        } else {
            logger.error("createAndBulkLoadNewRTree: Unknown bulk load method.");
            throw new IllegalArgumentException("createAndBulkLoadNewRTree: Unknown bulk load method.");
        }

    }


    // TODO DELETE
    public void /*SpatialIndex::ISpatialIndex* SpatialIndex::RTree*/ createAndBulkLoadNewRTree(
            BulkLoadMethod m,
            /*IDataStream& stream,*/
            IStorageManager sm,
            /*double fillFactor,
            int indexCapacity,
            int leafCapacity,
            int dimension,
            RTreeVariant rv,*/
            int indexIdentifier)
    {
        //SpatialIndex::ISpatialIndex* tree = createNewRTree(sm, fillFactor, indexCapacity, leafCapacity, dimension, rv, indexIdentifier);

        //uint32_t bindex = static_cast<uint32_t>(std::floor(static_cast<double>(indexCapacity * fillFactor)));
        //uint32_t bleaf = static_cast<uint32_t>(std::floor(static_cast<double>(leafCapacity * fillFactor)));

        int index = (int)Math.floor(indexCapacity * fillFactor);
        int leaf = (int)Math.floor(leafCapacity * fillFactor);

        BulkLoader_TEST bl = new BulkLoader_TEST();

        if (Objects.requireNonNull(m) == BulkLoadMethod.BLM_STR) {//bl.bulkLoadUsingSTR(static_cast<RTree*>(tree), stream, bindex, bleaf, 10000, 100);
            bl.bulkLoadUsingSTR(this, pseudoNodes, index, leaf);
        } else {
            System.err.println("createAndBulkLoadNewRTree: Unknown bulk load method.");
            //throw Tools::IllegalArgumentException ("createAndBulkLoadNewRTree: Unknown bulk load method.");
        }

        //return tree;
    }

//    public void /*SpatialIndex::ISpatialIndex* SpatialIndex::RTree*/ createAndBulkLoadNewRTree(
//            BulkLoadMethod m,
//            IDataStream& stream,
//            IStorageManager sm,
//            PropertySet ps,
//            id_type& indexIdentifier)
//    {
//        Tools::Variant var;
//        RTreeVariant rv(RV_RSTAR);
//        double fillFactor(0.7);
//        uint32_t indexCapacity(100);
//        uint32_t leafCapacity(100);
//        uint32_t dimension(2);
//        uint32_t pageSize(10000);
//        uint32_t numberOfPages(100);
//
//        // tree variant
//        var = ps.getProperty("TreeVariant");
//        if (var.m_varType != Tools::VT_EMPTY)
//        {
//            if (
//                    var.m_varType != Tools::VT_LONG ||
//                            (var.m_val.lVal != RV_LINEAR &&
//                                    var.m_val.lVal != RV_QUADRATIC &&
//                                    var.m_val.lVal != RV_RSTAR))
//                throw Tools::IllegalArgumentException("createAndBulkLoadNewRTree: Property TreeVariant must be Tools::VT_LONG and of RTreeVariant type");
//
//            rv = static_cast<RTreeVariant>(var.m_val.lVal);
//        }
//
//        // fill factor
//        // it cannot be larger than 50%, since linear and quadratic split algorithms
//        // require assigning to both nodes the same number of entries.
//        var = ps.getProperty("FillFactor");
//        if (var.m_varType != Tools::VT_EMPTY)
//        {
//            if (var.m_varType != Tools::VT_DOUBLE)
//                throw Tools::IllegalArgumentException("createAndBulkLoadNewRTree: Property FillFactor was not of type Tools::VT_DOUBLE");
//
//            if (var.m_val.dblVal <= 0.0)
//                throw Tools::IllegalArgumentException("createAndBulkLoadNewRTree: Property FillFactor was less than 0.0");
//
//            if (((rv == RV_LINEAR || rv == RV_QUADRATIC) && var.m_val.dblVal > 0.5))
//                throw Tools::IllegalArgumentException( "createAndBulkLoadNewRTree: Property FillFactor must be in range (0.0, 0.5) for LINEAR or QUADRATIC index types");
//            if ( var.m_val.dblVal >= 1.0)
//                throw Tools::IllegalArgumentException("createAndBulkLoadNewRTree: Property FillFactor must be in range (0.0, 1.0) for RSTAR index type");
//            fillFactor = var.m_val.dblVal;
//        }
//
//        // index capacity
//        var = ps.getProperty("IndexCapacity");
//        if (var.m_varType != Tools::VT_EMPTY)
//        {
//            if (var.m_varType != Tools::VT_ULONG || var.m_val.ulVal < 4)
//                throw Tools::IllegalArgumentException("createAndBulkLoadNewRTree: Property IndexCapacity must be Tools::VT_ULONG and >= 4");
//
//            indexCapacity = var.m_val.ulVal;
//        }
//
//        // leaf capacity
//        var = ps.getProperty("LeafCapacity");
//        if (var.m_varType != Tools::VT_EMPTY)
//        {
//            if (var.m_varType != Tools::VT_ULONG || var.m_val.ulVal < 4)
//                throw Tools::IllegalArgumentException("createAndBulkLoadNewRTree: Property LeafCapacity must be Tools::VT_ULONG and >= 4");
//
//            leafCapacity = var.m_val.ulVal;
//        }
//
//        // dimension
//        var = ps.getProperty("Dimension");
//        if (var.m_varType != Tools::VT_EMPTY)
//        {
//            if (var.m_varType != Tools::VT_ULONG)
//                throw Tools::IllegalArgumentException("createAndBulkLoadNewRTree: Property Dimension must be Tools::VT_ULONG");
//            if (var.m_val.ulVal <= 1)
//                throw Tools::IllegalArgumentException("createAndBulkLoadNewRTree: Property Dimension must be greater than 1");
//
//            dimension = var.m_val.ulVal;
//        }
//
//        // page size
//        var = ps.getProperty("ExternalSortBufferPageSize");
//        if (var.m_varType != Tools::VT_EMPTY)
//        {
//            if (var.m_varType != Tools::VT_ULONG)
//                throw Tools::IllegalArgumentException("createAndBulkLoadNewRTree: Property ExternalSortBufferPageSize must be Tools::VT_ULONG");
//            if (var.m_val.ulVal <= 1)
//                throw Tools::IllegalArgumentException("createAndBulkLoadNewRTree: Property ExternalSortBufferPageSize must be greater than 1");
//
//            pageSize = var.m_val.ulVal;
//        }
//
//        // number of pages
//        var = ps.getProperty("ExternalSortBufferTotalPages");
//        if (var.m_varType != Tools::VT_EMPTY)
//        {
//            if (var.m_varType != Tools::VT_ULONG)
//                throw Tools::IllegalArgumentException("createAndBulkLoadNewRTree: Property ExternalSortBufferTotalPages must be Tools::VT_ULONG");
//            if (var.m_val.ulVal <= 1)
//                throw Tools::IllegalArgumentException("createAndBulkLoadNewRTree: Property ExternalSortBufferTotalPages must be greater than 1");
//
//            numberOfPages = var.m_val.ulVal;
//        }
//
//        SpatialIndex::ISpatialIndex* tree = createNewRTree(sm, fillFactor, indexCapacity, leafCapacity, dimension, rv, indexIdentifier);
//
//        uint32_t bindex = static_cast<uint32_t>(std::floor(static_cast<double>(indexCapacity * fillFactor)));
//        uint32_t bleaf = static_cast<uint32_t>(std::floor(static_cast<double>(leafCapacity * fillFactor)));
//
//        BulkLoader bl;
//
//        switch (m)
//        {
//            case BLM_STR:
//                bl.bulkLoadUsingSTR(tree, stream, bindex, bleaf, pageSize, numberOfPages);
//                break;
//            default:
//                throw Tools::IllegalArgumentException("createAndBulkLoadNewRTree: Unknown bulk load method.");
//                break;
//        }
//
//        return tree;
//    }



    //
    // Internals
    //

    private void initNew(PropertySet ps) throws IOException {
        Object var;

        // tree variant.
        var = ps.getProperty("TreeVariant");
        if (var != null) {
            if (var instanceof Integer) {
                int i = (Integer) var;
                if (i != SpatialIndex.RtreeVariantLinear && i != SpatialIndex.RtreeVariantQuadratic
                        && i != SpatialIndex.RtreeVariantRstar)
                    throw new IllegalArgumentException("Property TreeVariant not a valid variant");
                treeVariant = i;
            } else {
                throw new IllegalArgumentException("Property TreeVariant must be an Integer");
            }
        }

        // fill factor.
        var = ps.getProperty("FillFactor");
        if (var != null) {
            if (var instanceof Double) {
                double f = (Double) var;
                if (f <= 0.0f || f >= 1.0f)
                    throw new IllegalArgumentException("Property FillFactor must be in (0.0, 1.0)");
                fillFactor = f;
            } else {
                throw new IllegalArgumentException("Property FillFactor must be a Double");
            }
        }

        // index capacity.
        var = ps.getProperty("IndexCapacity");
        if (var != null) {
            if (var instanceof Integer) {
                int i = (Integer) var;
                if (i < 3)
                    throw new IllegalArgumentException("Property IndexCapacity must be >= 3");
                indexCapacity = i;
            } else {
                throw new IllegalArgumentException("Property IndexCapacity must be an Integer");
            }
        }

        // leaf capacity.
        var = ps.getProperty("LeafCapacity");
        if (var != null) {
            if (var instanceof Integer) {
                int i = (Integer) var;
                if (i < 3)
                    throw new IllegalArgumentException("Property LeafCapacity must be >= 3");
                leafCapacity = i;
            } else {
                throw new IllegalArgumentException("Property LeafCapacity must be an Integer");
            }
        }

        // near minimum overlap factor.
        var = ps.getProperty("NearMinimumOverlapFactor");
        if (var != null) {
            if (var instanceof Integer) {
                int i = (Integer) var;
                if (i < 1 || i > indexCapacity || i > leafCapacity)
                    throw new IllegalArgumentException(
                            "Property NearMinimumOverlapFactor must be less than both index and leaf capacities");
                nearMinimumOverlapFactor = i;
            } else {
                throw new IllegalArgumentException("Property NearMinimumOverlapFactor must be an Integer");
            }
        }

        // split distribution factor.
        var = ps.getProperty("SplitDistributionFactor");
        if (var != null) {
            if (var instanceof Double) {
                double f = (Double) var;
                if (f <= 0.0f || f >= 1.0f)
                    throw new IllegalArgumentException("Property SplitDistributionFactor must be in (0.0, 1.0)");
                splitDistributionFactor = f;
            } else {
                throw new IllegalArgumentException("Property SplitDistriburionFactor must be a Double");
            }
        }

        // reinsert factor.
        var = ps.getProperty("ReinsertFactor");
        if (var != null) {
            if (var instanceof Double) {
                double f = (Double) var;
                if (f <= 0.0f || f >= 1.0f)
                    throw new IllegalArgumentException("Property ReinsertFactor must be in (0.0, 1.0)");
                reinsertFactor = f;
            } else {
                throw new IllegalArgumentException("Property ReinsertFactor must be a Double");
            }
        }

        // dimension
        var = ps.getProperty("Dimension");
        if (var != null) {
            if (var instanceof Integer) {
                int i = (Integer) var;
                if (i <= 1)
                    throw new IllegalArgumentException("Property Dimension must be >= 1");
                dimension = i;
            } else {
                throw new IllegalArgumentException("Property Dimension must be an Integer");
            }
        }

        infiniteRegion.low = new double[dimension];
        infiniteRegion.high = new double[dimension];

        for (int dim = 0; dim < dimension; dim++) {
            infiniteRegion.low[dim] = Double.POSITIVE_INFINITY;
            infiniteRegion.high[dim] = Double.NEGATIVE_INFINITY;
        }

        stats.treeHeight = 1;
        stats.nodesInLevel.add(new Integer(0));

        Leaf root = new Leaf(this, -1);
        rootID = writeNode(root);

        //TEST
        //storeHeader();
    }


    private void initOld(PropertySet ps) throws IOException {
        //TEST
        //loadHeader();

        // only some of the properties may be changed.
        // the rest are just ignored.

        Object var;

        // tree variant.
        var = ps.getProperty("TreeVariant");
        if (var != null) {
            if (var instanceof Integer) {
                int i = (Integer) var;
                if (i != SpatialIndex.RtreeVariantLinear && i != SpatialIndex.RtreeVariantQuadratic
                        && i != SpatialIndex.RtreeVariantRstar)
                    throw new IllegalArgumentException("Property TreeVariant not a valid variant");
                treeVariant = i;
            } else {
                throw new IllegalArgumentException("Property TreeVariant must be an Integer");
            }
        }

        // near minimum overlap factor.
        var = ps.getProperty("NearMinimumOverlapFactor");
        if (var != null) {
            if (var instanceof Integer) {
                int i = (Integer) var;
                if (i < 1 || i > indexCapacity || i > leafCapacity)
                    throw new IllegalArgumentException(
                            "Property NearMinimumOverlapFactor must be less than both index and leaf capacities");
                nearMinimumOverlapFactor = i;
            } else {
                throw new IllegalArgumentException("Property NearMinimumOverlapFactor must be an Integer");
            }
        }

        // split distribution factor.
        var = ps.getProperty("SplitDistributionFactor");
        if (var != null) {
            if (var instanceof Double) {
                double f = (Double) var;
                if (f <= 0.0f || f >= 1.0f)
                    throw new IllegalArgumentException("Property SplitDistributionFactor must be in (0.0, 1.0)");
                splitDistributionFactor = f;
            } else {
                throw new IllegalArgumentException("Property SplitDistriburionFactor must be a Double");
            }
        }

        // reinsert factor.
        var = ps.getProperty("ReinsertFactor");
        if (var != null) {
            if (var instanceof Double) {
                double f = (Double) var;
                if (f <= 0.0f || f >= 1.0f)
                    throw new IllegalArgumentException("Property ReinsertFactor must be in (0.0, 1.0)");
                reinsertFactor = f;
            } else {
                throw new IllegalArgumentException("Property ReinsertFactor must be a Double");
            }
        }

        infiniteRegion.low = new double[dimension];
        infiniteRegion.high = new double[dimension];

        for (int dim = 0; dim < dimension; dim++) {
            infiniteRegion.low[dim] = Double.POSITIVE_INFINITY;
            infiniteRegion.high[dim] = Double.NEGATIVE_INFINITY;
        }
    }

    // TODO
//    private void storeHeader() throws IOException {
//        //ByteArrayOutputStream bs = new ByteArrayOutputStream();
//        //DataOutputStream ds = new DataOutputStream(bs);
//        NodeEntry entry = new NodeEntry();
//
//        entry.rootID = rootID;
//        entry.treeVariant = treeVariant;
//        entry.fillFactor = fillFactor;
//        entry.indexCapacity = indexCapacity;
//        entry.leafCapacity = leafCapacity;
//        entry.nearMinimumOverlapFactor = nearMinimumOverlapFactor;
//        entry.splitDistributionFactor = splitDistributionFactor;
//        entry.reinsertFactor = reinsertFactor;
//        entry.dimension = dimension;
//        entry.stats.nodes = stats.nodes;
//        entry.stats.data = stats.data;
//        entry.stats.treeHeight = stats.treeHeight;
//
//        for (int level = 0; level < stats.treeHeight; level++) {
//            entry.stats.nodesInLevel.add(stats.nodesInLevel.get(level));
//        }
//
//        headerID = storageManager.storeNode(headerID, entry);
//
//        //NodeEntry.NodeHeader headerData = new NodeEntry.NodeHeader();
//
////        headerData.rootID = rootID;
////        headerData.treeVariant = treeVariant;
////        headerData.fillFactor = fillFactor;
////        headerData.indexCapacity = indexCapacity;
////        headerData.leafCapacity = leafCapacity;
////        headerData.nearMinimumOverlapFactor = nearMinimumOverlapFactor;
////        headerData.splitDistributionFactor = splitDistributionFactor;
////        headerData.reinsertFactor = reinsertFactor;
////        headerData.dimension = dimension;
////        headerData.stats.nodes = stats.nodes;
////        headerData.stats.data = stats.data;
////        headerData.stats.treeHeight = stats.treeHeight;
//
////        for (int level = 0; level < stats.treeHeight; level++) {
////            headerData.stats.nodesInLevel.add(stats.nodesInLevel.get(level));
////        }
////
////        headerID = storageManager.storeNode(headerID, headerData);
//        //headerID = storageManager.storeNode(headerID, headerData);
//    }

    //TODO
//    private void loadHeader() throws IOException {
//        NodeEntry entry = storageManager.loadNode(headerID);
//
//        rootID = entry.rootID;
//        treeVariant = entry.treeVariant;
//        fillFactor = entry.fillFactor;
//        indexCapacity = entry.indexCapacity;
//        leafCapacity = entry.leafCapacity;
//        nearMinimumOverlapFactor = entry.nearMinimumOverlapFactor;
//        splitDistributionFactor = entry.splitDistributionFactor;
//        reinsertFactor = entry.reinsertFactor;
//        dimension = entry.dimension;
//        stats.nodes = entry.stats.nodes;
//        stats.data = entry.stats.data;
//        stats.treeHeight = entry.stats.treeHeight;
//
//        for (int level = 0; level < stats.treeHeight; level++) {
//            stats.nodesInLevel.add(entry.stats.nodesInLevel.get(level));
//        }
//
//
//        //        NodeHeaderData headerData = storageManager.loadNode(headerID);
////
////        rootID = headerData.rootID;
////        treeVariant = headerData.treeVariant;
////        fillFactor = headerData.fillFactor;
////        indexCapacity = headerData.indexCapacity;
////        leafCapacity = headerData.leafCapacity;
////        nearMinimumOverlapFactor = headerData.nearMinimumOverlapFactor;
////        splitDistributionFactor = headerData.splitDistributionFactor;
////        reinsertFactor = headerData.reinsertFactor;
////        dimension = headerData.dimension;
////        stats.nodes = headerData.stats.nodes;
////        stats.data = headerData.stats.data;
////        stats.treeHeight = headerData.stats.treeHeight;
////
////        for (int level = 0; level < stats.treeHeight; level++) {
////            stats.nodesInLevel.add(headerData.stats.nodesInLevel.get(level));
////        }
//    }



    protected void insertDataImpl(NodeData data, Region mbr, int id) {
        assert mbr.getDimension() == dimension;

        boolean[] overflowTable;

        Stack<Integer> pathBuffer = new Stack<>();

        Node root = readNode(rootID);

        overflowTable = new boolean[root.level];
        for (int cLevel = 0; cLevel < root.level; cLevel++)
            overflowTable[cLevel] = false;

        Node leaf = root.chooseSubtree(mbr, 0, pathBuffer);
        leaf.insertData(data, mbr, id, pathBuffer, overflowTable);

        stats.data++;
    }

    protected void insertDataImpl(NodeData data, Region mbr, int id, int level, boolean[] overflowTable) {
        assert mbr.getDimension() == dimension;

        Stack<Integer> pathBuffer = new Stack<>();

        Node root = readNode(rootID);
        Node n = root.chooseSubtree(mbr, level, pathBuffer);
        n.insertData(data, mbr, id, pathBuffer, overflowTable);
    }

    protected boolean deleteDataImpl(final Region mbr, int id) {
        assert mbr.getDimension() == dimension;

        boolean bRet = false;

        Stack<Integer> pathBuffer = new Stack<>();

        Node root = readNode(rootID);
        Leaf l = root.findLeaf(mbr, id, pathBuffer);

        if (l != null) {
            l.deleteData(id, pathBuffer);
            stats.data--;
            bRet = true;
        }

        return bRet;
    }

    protected int writeNode(Node n) throws IllegalStateException {
        Node node;
        //NodeEntry entry = new NodeEntry();

        try {
            node = n.store();
            //entry.node = node;
        } catch (IOException e) {
            System.err.println(e);
            throw new IllegalStateException("writeNode failed with IOException");
        }

        int page;
        if (n.identifier < 0)
            page = IStorageManager.NewPage;
        else
            page = n.identifier;

        try {
            page = storageManager.storeNode(page, node); //(page, buffer);
        } catch (InvalidPageException e) {
            System.err.println(e);
            throw new IllegalStateException("writeNode failed with InvalidPageException");
        }

        if (n.identifier < 0) {
            n.identifier = page;
            stats.nodes++;
            int i = stats.nodesInLevel.get(n.level);
            stats.nodesInLevel.set(n.level, i + 1);
        }

        stats.writes++;

        for (INodeCommand writeNodeCommand : writeNodeCommands) {
            writeNodeCommand.execute(n);
        }

        return page;
    }

    protected Node readNode(int id) {
        Node nodeData;

        int nodeType = -1;
        Node n = null;

        try {
            nodeData = (Node) storageManager.loadNode(id);
            nodeType = nodeData.type;

            if (nodeType == SpatialIndex.PersistentIndex)
                n = new Index(this, -1, 0);
            else if (nodeType == SpatialIndex.PersistentLeaf)
                n = new Leaf(this, -1);
            else
                throw new IllegalStateException("readNode failed reading the correct node type information");

            n.rTree = this;
            n.identifier = id;
            n.load(nodeData);

            stats.reads++;
        } catch (InvalidPageException e) {
            logger.error(e);
            throw new IllegalStateException("readNode failed with InvalidPageException");
        } catch (IOException e) {
            logger.error(e);
            throw new IllegalStateException("readNode failed with IOException");
        }

        for (INodeCommand readNodeCommand : readNodeCommands) {
            readNodeCommand.execute(n);
        }

        return n;
    }

    protected void deleteNode(Node n) {
        try {
            storageManager.deleteNode(n.identifier);
        } catch (InvalidPageException e) {
            logger.error(e);
            throw new IllegalStateException("deleteNode failed with InvalidPageException");
        }

        stats.nodes--;
        int i = stats.nodesInLevel.get(n.level);
        stats.nodesInLevel.set(n.level, i - 1);

        for (INodeCommand deleteNodeCommand : deleteNodeCommands) {
            deleteNodeCommand.execute(n);
        }
    }

    private void rangeQuery(int type, final IShape query, final IVisitor v) {
        rwLock.readLock();

        try {
            Stack<Node> st = new Stack<>();
            Node root = readNode(rootID);

            if (root.children > 0 && query.intersects(root.nodeMBR))
                st.push(root);

            while (!st.empty()) {
                Node n = st.pop();

                if (n.level == 0) {
                    v.visitNode(n);

                    for (int child = 0; child < n.children; child++) {
                        boolean b;
                        if (type == SpatialIndex.ContainmentQuery)
                            b = query.contains(n.mbr[child]);
                        else
                            b = query.intersects(n.mbr[child]);

                        if (b) {
                            Data data = new Data(n.nodeData.data[child], n.mbr[child], n.identifiers[child]);
                            v.visitData(data);
                            stats.queryResults++;
                        }
                    }
                } else {
                    v.visitNode((INode) n);

                    for (int child = 0; child < n.children; child++) {
                        if (query.intersects(n.mbr[child])) {
                            st.push(readNode(n.identifiers[child]));
                        }
                    }
                }
            }
        } finally {
            rwLock.readUnlock();
        }
    }

    public String toString() {
        String s = "Dimension: " + dimension + "\n" + "Fill factor: " + fillFactor + "\n" + "Index capacity: "
                + indexCapacity + "\n" + "Leaf capacity: " + leafCapacity + "\n";

        if (treeVariant == SpatialIndex.RtreeVariantRstar) {
            s += "Near minimum overlap factor: " + nearMinimumOverlapFactor + "\n" + "Reinsert factor: "
                    + reinsertFactor + "\n" + "Split distribution factor: " + splitDistributionFactor + "\n";
        }

        s += "Utilization: " + 100 * stats.getNumberOfData() / ((long) stats.getNumberOfNodesInLevel(0) * leafCapacity)
                + "%" + "\n" + stats;

        return s;
    }

    public String printTree() {
        rwLock.readLock();

        StringBuilder sb = new StringBuilder();
        try {
            Stack<Node> st = new Stack<>();
            Node root = readNode(rootID);

            st.push(root);

            while (!st.empty()) {
                Node n = st.pop();

                if (n.level == 0) {
                    sb.append("Leaf: ");
                    sb.append(n.printNode());
                    sb.append("\n");
                } else {
                    sb.append("Index: ");
                    sb.append(n.printNode());
                    sb.append("\n");

                    for (int child = 0; child < n.children; child++) {
                        st.push(readNode(n.identifiers[child]));
                    }
                }
            }
        } finally {
            rwLock.readUnlock();
        }

        return sb.toString();
    }

    class NNComparator implements INearestNeighborComparator {
        public double getMinimumDistance(IShape query, IEntry e) {
            IShape s = e.getShape();
            return query.getMinimumDistance(s);
        }
    }

    class ValidateEntry {
        Region parentMBR;
        Node node;

        ValidateEntry(Region r, Node pNode) {
            parentMBR = r;
            node = pNode;
        }
    }

    class Data implements IData {
        int id;
        Region shape;
        NodeData data;

        Data(NodeData data, Region mbr, int id) {
            this.id = id;
            shape = mbr;
            this.data = data;
        }

        public int getIdentifier() {
            return id;
        }

        public IShape getShape() {
            return new Region(shape);
        }

        public NodeData getData() {
            //byte[] data = new byte[this.data.length];
            //System.arraycopy(this.data, 0, data, 0, this.data.length);
            return data;
        }

        @Override
        public String toString() {
            return "Data [m_id=" + id + ", m_shape=" + shape + ", m_pData=" + Arrays.toString(data.data) + "]";
        }

    }


    /**
     * **********************************************************************************
     * IR
     * **********************************************************************************
     *
     */

    public ArrayList<WeightEntry> ir(IStore ds, InvertedFile invertedFile) {

        Node n = readNode(rootID);

        return irTraversal(ds, invertedFile, n);

        // Creating IR-Tree...
        // processing index node 0
        // processing index node 53
        // Can't find document 32293

    }

    private ArrayList<WeightEntry> irTraversal(IStore ds, InvertedFile invertedFile, Node n) {
        if (n.level == 0) {
            invertedFile.create(n.identifier);

            int child;
            for (child = 0; child < n.children; child++) {
                int docID = n.identifiers[child];

                ArrayList<WeightEntry> document = ds.read(docID).weights;
                if (document == null) { // TODO DELETE THIS
                    System.out.println("Can't find document " + docID);
                    System.exit(-1);
                }
                invertedFile.addDocument(n.identifier, docID, document);
            }

            ArrayList<WeightEntry> pseudoDoc = invertedFile.store(n.identifier);

            return pseudoDoc;

        } else {

            invertedFile.create(n.identifier);
            logger.debug("Processing index node: {}", n.identifier);
            //System.out.println("processing index node " + n.identifier);

            int child;
            for (child = 0; child < n.children; child++) {
                Node nn = readNode(n.identifiers[child]);
                ArrayList<WeightEntry> pseudoDoc = irTraversal(ds, invertedFile, nn);
                int docID = n.identifiers[child];

                if (pseudoDoc == null) {
                    System.out.println("Can't find document " + docID);
                    System.exit(-1);

                }
                invertedFile.addDocument(n.identifier, docID, pseudoDoc);

            }

            ArrayList<WeightEntry> pseudoDoc = invertedFile.store(n.identifier);

            return pseudoDoc;

        }
    }

    public ArrayList<ArrayList<WeightEntry>>  cirClusterEnhance(HashMap<Integer, Integer> clusterTree, IStore ds, InvertedFile invertedFile) {
        Node n = readNode(rootID);
        //tempHashMap = new HashMap(invertedFile.getInvertedLists());//AbstractDocumentStore
        return cirClusterTraversal(clusterTree, ds, invertedFile, n);
    }

    private ArrayList<ArrayList<WeightEntry>> cirClusterTraversal(HashMap<Integer, Integer> clusterTree, IStore ds, InvertedFile invertedFile, Node n) {
        //invertedFile.setInvertedLists(tempHashMap); //FIX
        if (n.level == 0) {
            invertedFile.create(n.identifier);

            int child;
            for (child = 0; child < n.children; child++) {
                int docID = n.identifiers[child];

                ArrayList<WeightEntry> document = ds.read(docID).weights;
                if (document == null) {
                    logger.error("Couldn't find document {}", docID);
                    System.exit(-1);
                }

                Integer var = clusterTree.get(docID);
                if (var == null) {
                    logger.error("Couldn't find cluster for {}", docID);
                    System.exit(-1);
                }
                int cluster = var;
                logger.debug("Adding DOC => nodeID: {} docID: {} DOC: {} Cluster: {}", n.identifier, docID, document, cluster);
                invertedFile.addDocument(n.identifier, docID, document, cluster);
            }
            ArrayList<ArrayList<WeightEntry>> pseudoDoc = invertedFile.storeClusterEnhance(n.identifier);
            //tempHashMap = new HashMap(invertedFile.getInvertedLists()); // FIX
            return pseudoDoc;
        } else {
            invertedFile.create(n.identifier);
            logger.debug("Processing index node: {}", n.identifier);

            int child;
            for (child = 0; child < n.children; child++) {
                Node nn = readNode(n.identifiers[child]);
                //tempHashMap = new HashMap(invertedFile.getInvertedLists()); // FIX
                ArrayList<ArrayList<WeightEntry>> pseudoDoc = cirClusterTraversal(clusterTree, ds, invertedFile, nn);
                int docID = n.identifiers[child];

                if (pseudoDoc == null) {
                    logger.error("Couldn't find document {}", docID);
                    System.exit(-1);

                }

                for (int i = 0; i < pseudoDoc.size(); i++) {
                    if (pseudoDoc.get(i).isEmpty())
                        continue;
                    invertedFile.addDocument(n.identifier, docID, pseudoDoc.get(i), i);
                    logger.debug("Adding inner DOC: {} - NodeID: {}", docID, n.identifier);
                }
            }
            ArrayList<ArrayList<WeightEntry>> pseudoDoc = invertedFile.storeClusterEnhance(n.identifier);
            //tempHashMap = new HashMap(invertedFile.getInvertedLists()); // FIX
            return pseudoDoc;
        }
    }

    public void lkt(InvertedFile invertedFile, Query q, int topk) throws Exception {
        PriorityQueue<NNEntry> queue = new PriorityQueue<>(100, new NNEntryComparator());
        RtreeEntry e = new RtreeEntry(rootID, false);
        queue.add(new NNEntry(e, 0.0));

        int count = 0;
        double knearest = 0.0;
        while (!queue.isEmpty()) {
            NNEntry first = queue.poll();
            e = (RtreeEntry) first.node;

            numOfVisitedNodes++;

            if (e.isLeafEntry) {
                if (count >= topk && first.cost > knearest)
                    break;

                count++;
                System.out.println(e.getIdentifier() + "," + first.cost);
                knearest = first.cost;
            } else {
                Node n = readNode(e.getIdentifier());

                HashMap<Integer, Double> filter;

                invertedFile.load(n.identifier);
                if (numOfClusters != 0)
                    filter = invertedFile.rankingSumClusterEnhance(q.keywords, q.keywordWeights);
                else
                    filter = invertedFile.rankingSum((ArrayList<Integer>) q.keywords);

                for (int child = 0; child < n.children; child++) {
                    double irscore;
                    Double var = filter.get(n.identifiers[child]);
                    if (var == null)
                        continue;
                    else
                        irscore = var;

                    if (n.level == 0) {
                        e = new RtreeEntry(n.identifiers[child], true);
                    } else {
                        e = new RtreeEntry(n.identifiers[child], false);
                    }
                    double mind = combinedScore(n.mbr[child].getMinimumDistance(q.location), irscore);

                    queue.add(new NNEntry(e, mind));

                }
            }
        }

    }

    public double combinedScore(double spatial, double ir) {
        double spatialCost = spatial / datasetParameters.maxD;
        double keywordMismatchCost = (1 - ir);

        if (spatialCost < 0)
            spatialCost = 0;
        if (keywordMismatchCost < 0)
            keywordMismatchCost = 0;

        double totalCost = alphaDistribution * spatialCost + (1 - alphaDistribution) * keywordMismatchCost;
//		System.out.println(totalCost + " " + spatialCost + " " + keywordMismatchCost);

        return totalCost;
    }

    public int getIO() {
        return storageManager.getIO();
    }


}

