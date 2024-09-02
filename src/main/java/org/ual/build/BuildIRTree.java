package org.ual.build;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ual.documentindex.InvertedFile;
import org.ual.spatialindex.rtree.RTree;
import org.ual.spatialindex.storage.AbstractDocumentStore;
import org.ual.spatialindex.storage.WeightEntry;

import java.util.ArrayList;

public class BuildIRTree {
    private static final Logger logger = LogManager.getLogger(BuildIRTree.class);

    /**
     * Build an IR-Tree based on a R-tree and a document store with the term weights
     * @param tree
     * @param dms
     * @return inverted list
     */
    public static InvertedFile buildTreeIR(RTree tree, AbstractDocumentStore dms) {
        // In memory inverted file
        InvertedFile invertedFile = new InvertedFile();

        long start = System.currentTimeMillis();
        ArrayList<WeightEntry> invertedIndex = tree.ir(dms, invertedFile);
        long end = System.currentTimeMillis();

        //logger.info("Time: {} minutes", ((end - start) / 1000.0f) / 60.0f);
        logger.info("Time: {} ms", (end - start));

        boolean ret = tree.isIndexValid();
        if (!ret)
            logger.error("Structure is INVALID!");

        return invertedFile;
    }
}
