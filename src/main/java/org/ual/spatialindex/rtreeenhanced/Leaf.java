package org.ual.spatialindex.rtreeenhanced;

import org.ual.spatialindex.spatialindex.NodeData;
import org.ual.spatialindex.spatialindex.Region;
import org.ual.spatialindex.spatialindex.SpatialIndex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;

public class Leaf extends Node {
    public Leaf(RTreeEnhanced tree, int id) {
        super(tree, id, 0, tree.leafCapacity);
    }

    @Override
    protected Node chooseSubtree(Region mbr, int level, Stack<Integer> pathBuffer, HashSet<Integer> doc) {
        return this;
    }

    @Override
    protected Leaf findLeaf(Region mbr, int id, Stack<Integer> pathBuffer) {
        for (int child = 0; child < children; child++) {
            if (identifiers[child] == id && mbr.equals(this.mbr[child]))
                return this;
        }
        return null;
    }

    @Override
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

        Node left = new Leaf(tree, -1);
        Node right = new Leaf(tree, -1);

        int index;

        for (index = 0; index < g1.size(); index++) {
            int i = g1.get(index);
            left.insertEntry(nodeData.data[i], this.mbr[i], identifiers[i], docList[i]);

            // we don't want to delete the data array from this node's destructor!
            nodeData.data[i] = null;
        }

        for (index = 0; index < g2.size(); index++) {
            int i = g2.get(index);
            right.insertEntry(nodeData.data[i], this.mbr[i], identifiers[i], docList[i]);

            // we don't want to delete the data array from this node's destructor!
            nodeData.data[i] = null;
        }

        Node[] ret = new Node[2];
        ret[0] = left;
        ret[1] = right;

        return ret;
    }


//    protected void deleteData(int id, Stack<Integer> pathBuffer)
//    {
//        int child;
//        for (child = 0; child < children; child++)
//        {
//            if (identifiers[child] == id)
//                break;
//        }
//
//        deleteEntry(child);
//        rTree.writeNode(this);
//
//        Stack<Node> toReinsert = new Stack<>();
//        condenseTree(toReinsert, pathBuffer);
//
//        // re-insert eliminated nodes.
//        while (! toReinsert.empty())
//        {
//            Node n = toReinsert.pop();
//            rTree.deleteNode(n);
//
//            for (child = 0; child < n.children; child++)
//            {
//                // keep this in the for loop. The tree height might change after insertions.
//                boolean[] overflowTable = new boolean[rTree.stats.treeHeight];
//                for (int level = 0; level < rTree.stats.treeHeight; level++) overflowTable[level] = false;
//
//                rTree.insertDataImpl(n.data.data.get(child),
//                        n.mbr[child], n.identifiers[child],
//                        n.level, overflowTable);
//                n.data.data.set(child, null);
//            }
//        }
//    }

}
