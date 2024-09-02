package org.ual.build;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ual.documentindex.InvertedFile;
import org.ual.spatialindex.rtreeenhanced.RTreeEnhanced;
import org.ual.spatialindex.storage.AbstractDocumentStore;

import java.util.HashMap;

public class BuildCDIRTree {
    private static final Logger logger = LogManager.getLogger(BuildCDIRTree.class);

    /**
     * Build the CDIR-tree using the R-tree enhanced variant from the DIR-tree and the cluster enhancement from the
     * CIR-tree.
     *
     * @param tree
     * @param dms
     * @param clusterMap
     * @param invertedFile
     * @param numClusters
     */
    public static void buildTreeCDIR(RTreeEnhanced tree, AbstractDocumentStore dms, HashMap<Integer, Integer> clusterMap, InvertedFile invertedFile, int numClusters) {
        RTreeEnhanced.numOfClusters = numClusters;

        long start = System.currentTimeMillis();
        tree.cirClusterEnhance(clusterMap, dms, invertedFile);
        long end = System.currentTimeMillis();

        //logger.info("Time: {} minutes", ((end - start) / 1000.0f) / 60.0f);
        logger.info("Time: {} ms", (end - start));

        boolean ret = tree.isIndexValid();
        if (!ret)
            logger.error("Structure is INVALID!");
    }
}
