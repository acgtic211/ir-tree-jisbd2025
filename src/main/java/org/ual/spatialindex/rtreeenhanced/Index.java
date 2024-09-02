package org.ual.spatialindex.rtreeenhanced;

import org.ual.spatialindex.spatialindex.NodeData;
import org.ual.spatialindex.spatialindex.Region;
import org.ual.spatialindex.spatialindex.SpatialIndex;

import java.util.*;

public class Index extends Node {
    public Index(RTreeEnhanced tree, int id, int level) {
        super(tree, id, level, tree.indexCapacity);
    }

    protected Node chooseSubtree(Region mbr, int level, Stack<Integer> pathBuffer, HashSet<Integer> doc) {
        if (this.level == level)
            return this;

        pathBuffer.push(identifier);

        int child = 0;

        switch (tree.treeVariant) {
            case SpatialIndex.RtreeVariantLinear:
            case SpatialIndex.RtreeVariantQuadratic:
                child = findLeastEnlargement(mbr, doc);
                break;
            case SpatialIndex.RtreeVariantRstar:
                if (this.level == 1) {
                    // if this node points to leaves...
                    child = findLeastOverlap(mbr);
                } else {
                    child = findLeastEnlargement(mbr, doc);
                }
                break;
            default:
                throw new IllegalStateException("Unknown RTree variant.");
        }

        Node n = tree.readNode(identifiers[child]);
        Node ret = n.chooseSubtree(mbr, level, pathBuffer, doc);

        return ret;
    }

    protected Leaf findLeaf(Region mbr, int id, Stack<Integer> pathBuffer) {
        pathBuffer.push(identifier);

        for (int child = 0; child < children; child++) {
            if (this.mbr[child].contains(mbr)) {
                Node n = tree.readNode(identifiers[child]);
                Leaf leaf = n.findLeaf(mbr, id, pathBuffer);
                if (leaf != null)
                    return leaf;
            }
        }

        pathBuffer.pop();

        return null;
    }


    protected Node[] split(NodeData data, Region mbr, int id, HashSet<Integer> doc) {
        tree.stats.splits++;

        ArrayList<Integer> g1 = new ArrayList<>(), g2 = new ArrayList<>();

        switch (tree.treeVariant) {
            case SpatialIndex.RtreeVariantLinear:
            case SpatialIndex.RtreeVariantQuadratic:
                rtreeSplit(data, mbr, id, g1, g2, doc);
                break;
            case SpatialIndex.RtreeVariantRstar:
                rstarSplit(data, mbr, id, g1, g2);
                break;
            default:
                throw new IllegalStateException("Unknown RTree variant.");
        }

        Node left = new Index(tree, identifier, level);
        Node right = new Index(tree, -1, level);

        int index;

        for (index = 0; index < g1.size(); index++) {
            int i = g1.get(index);
            left.insertEntry(null, this.mbr[i], identifiers[i], docList[i]);
        }

        for (index = 0; index < g2.size(); index++) {
            int i = g2.get(index);
            right.insertEntry(null, this.mbr[i], identifiers[i], docList[i]);
        }

        Node[] ret = new Node[2];
        ret[0] = left;
        ret[1] = right;

        return ret;
    }

    protected int findLeastEnlargement(Region r, HashSet<Integer> doc) {
        double area = Double.POSITIVE_INFINITY;
        double minscore = Double.POSITIVE_INFINITY;
        int best = -1;

        for (int child = 0; child < children; child++) {
            Region t = mbr[child].combinedRegion(r);

            double a = mbr[child].getArea();
            double enl = t.getArea() - a;

            double score = RTreeEnhanced.betaArea * enl + (1 - RTreeEnhanced.betaArea) * docDistance(doc, docList[child]);

            if(score < minscore) {
                minscore = score;
                best = child;
            } else if(score == minscore) {
                if (enl < area) {
                    area = enl;
                    best = child;
                } else if (enl == area) {
                    if (a < mbr[best].getArea())
                        best = child;
                }
            }

        }

        return best;
    }

    protected int findLeastOverlap(Region r) {
        OverlapEntry[] entries = new OverlapEntry[children];

        double leastOverlap = Double.POSITIVE_INFINITY;
        double me = Double.POSITIVE_INFINITY;
        int best = -1;

        // find combined region and enlargement of every entry and store it.
        for (int child = 0; child < children; child++) {
            OverlapEntry e = new OverlapEntry();

            e.id = child;
            e.original = mbr[child];
            e.combined = mbr[child].combinedRegion(r);
            e.originalArea = e.original.getArea();
            e.combinedArea = e.combined.getArea();
            e.enlargement = e.combinedArea - e.originalArea;
            entries[child] = e;

            if (e.enlargement < me) {
                me = e.enlargement;
                best = child;
            } else if (e.enlargement == me && e.originalArea < entries[best].originalArea) {
                best = child;
            }
        }

        if (me < SpatialIndex.EPSILON || me > SpatialIndex.EPSILON) {
            int iterations;

            if (children > tree.nearMinimumOverlapFactor) {
                // sort entries in increasing order of enlargement.
                Arrays.sort(entries, new OverlapEntryComparator());
                iterations = tree.nearMinimumOverlapFactor;
            } else {
                iterations = children;
            }

            // calculate overlap of most important original entries (near minimum overlap cost).
            for (int index = 0; index < iterations; index++) {
                double dif = 0.0;
                OverlapEntry e = entries[index];

                for (int child = 0; child < children; child++) {
                    if (e.id != child) {
                        double f = e.combined.getIntersectingArea(mbr[child]);
                        if (f != 0.0)
                            dif +=  f - e.original.getIntersectingArea(mbr[child]);
                    }
                } // for (child)

                if (dif < leastOverlap) {
                    leastOverlap = dif;
                    best = index;
                } else if (dif == leastOverlap) {
                    if (e.enlargement == entries[best].enlargement) {
                        // keep the one with least area.
                        if (e.original.getArea() < entries[best].original.getArea())
                            best = index;
                    } else {
                        // keep the one with least enlargement.
                        if (e.enlargement < entries[best].enlargement)
                            best = index;
                    }
                }
            } // for (index)
        }

        return entries[best].id;
    }

    protected void adjustTree(Node n, Stack<Integer> pathBuffer) {
        tree.stats.adjustments++;

        // find entry pointing to old node;
        int child;
        for (child = 0; child < children; child++) {
            if (identifiers[child] == n.identifier)
                break;
        }

        // MBR needs recalculation if either:
        //   1. the NEW child MBR is not contained.
        //   2. the OLD child MBR is touching.
        boolean b = nodeMBR.contains(n.nodeMBR);
        boolean recalc = (! b) ? true : nodeMBR.touches(mbr[child]);

        mbr[child] = (Region) n.nodeMBR.clone();
        docList[child] = (HashSet<Integer>) n.doc.clone();

        if (recalc) {
            for (int dim = 0; dim < tree.dimension; dim++) {
                nodeMBR.low[dim] = Double.POSITIVE_INFINITY;
                nodeMBR.high[dim] = Double.NEGATIVE_INFINITY;

                for (int cChild = 0; cChild < children; cChild++) {
                    nodeMBR.low[dim] = Math.min(nodeMBR.low[dim], mbr[cChild].low[dim]);
                    nodeMBR.high[dim] = Math.max(nodeMBR.high[dim], mbr[cChild].high[dim]);
                }
            }
        }

        doc.clear();

        for (int cChild = 0; cChild < children; cChild++) {
            doc.addAll(docList[cChild]);
        }

        tree.writeNode(this);

        if (! pathBuffer.empty()) {
            int cParent = pathBuffer.pop();
            Index p = (Index) tree.readNode(cParent);
            p.adjustTree(this, pathBuffer);
        }
    }

    protected void adjustTree(Node n1, Node n2, Stack<Integer> pathBuffer, boolean[] overflowTable) {
        tree.stats.adjustments++;

        // find entry pointing to old node;
        int child;
        for (child = 0; child < children; child++) {
            if (identifiers[child] == n1.identifier)
                break;
        }

        // MBR needs recalculation if either:
        //   1. the NEW child MBR is not contained.
        //   2. the OLD child MBR is touching.
        boolean b = nodeMBR.contains(n1.nodeMBR);
        boolean recalc = (! b) ? true : nodeMBR.touches(mbr[child]);

        mbr[child] = (Region) n1.nodeMBR.clone();
        docList[child] = (HashSet<Integer>) n1.doc.clone();

        if (recalc) {
            for (int dim = 0; dim < tree.dimension; dim++) {
                nodeMBR.low[dim] = Double.POSITIVE_INFINITY;
                nodeMBR.high[dim] = Double.NEGATIVE_INFINITY;

                for (int cChild = 0; cChild < children; cChild++) {
                    nodeMBR.low[dim] = Math.min(nodeMBR.low[dim], mbr[cChild].low[dim]);
                    nodeMBR.high[dim] = Math.max(nodeMBR.high[dim], mbr[cChild].high[dim]);
                }
            }
        }

        doc.clear();

        for (int cChild = 0; cChild < children; cChild++) {
            doc.addAll(docList[cChild]);
        }
        // No write necessary here. insertData will write the node if needed.
        //m_pTree.writeNode(this);


        boolean adjusted = insertData(null, (Region) n2.nodeMBR.clone(), n2.identifier, pathBuffer, overflowTable, (HashSet)n2.doc.clone());

        // if n2 is contained in the node and there was no split or reinsert,
        // we need to adjust only if recalculation took place.
        // In all other cases insertData above took care of adjustment.
        if (! pathBuffer.empty()) {
            int parent = pathBuffer.pop();
            Index p = (Index) tree.readNode(parent);
            p.adjustTree(this, pathBuffer);
        }
    }

    class OverlapEntry {
        int id;
        double enlargement;
        Region original;
        Region combined;
        double originalArea;
        double combinedArea;
    }

    class OverlapEntryComparator implements Comparator<OverlapEntry> {
        @Override
        public int compare(OverlapEntry o1, OverlapEntry o2) {
            if (o1.enlargement < o2.enlargement)
                return -1;
            if (o1.enlargement > o2.enlargement)
                return 1;
            return 0;
        }
    }


}
