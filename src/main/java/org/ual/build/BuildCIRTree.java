package org.ual.build;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ual.documentindex.InvertedFile;
import org.ual.spatialindex.rtree.RTree;
import org.ual.spatialindex.storage.AbstractDocumentStore;
import org.ual.utils.main.StatisticsLogic;

import java.util.HashMap;

public class BuildCIRTree {
    private static final Logger logger = LogManager.getLogger(BuildCIRTree.class);

    //     /*
//    1. Store text description of objects (build/StoreDocument)
//    2. Build a R-Tree (build/BuildRTree)
//    3. Build CIR-Tree based on R-Tree (build/ClusterEnhacement)

    //        // Words are required to be sorted in terms of their frequencies and represented as integers. Small integers represents high frequencies.
//        // create a new B+Tree data structure and use a StringComparator to order the records based on people's name.
//     */


    public static void buildTreeCIR(RTree tree, AbstractDocumentStore dms, HashMap<Integer, Integer> clusterMap, InvertedFile invertedFile, int numClusters) {
        RTree.numOfClusters = numClusters;

        long initMem = StatisticsLogic.getClearedMem();
        long startTime = System.currentTimeMillis();

        tree.cirClusterEnhance(clusterMap, dms, invertedFile);

        long endTime = System.currentTimeMillis();
        long endMem = StatisticsLogic.getMemUsed();
        StatisticsLogic.irTreePeakMemUsed = (endMem - initMem);
        StatisticsLogic.irTreeMemUsed = StatisticsLogic.getClearedMem() - initMem;//StatisticsLogic.cleanMem((int) tree.getStatistics().getNumberOfNodes(), initMem); //Call gc()
        StatisticsLogic.irTreeBuildTime = (endTime - startTime);

        logger.info("CIRtree build in: {} ms", StatisticsLogic.irTreeBuildTime);
        logger.info("CIRtree peak memory usage: {} Megabytes", (StatisticsLogic.irTreePeakMemUsed/1024)/1024);
        logger.info("CIRtree memory usage: {} Megabytes", (StatisticsLogic.irTreeMemUsed/1024)/1024);



//        StatisticsLogic.cleanMem((int) tree.getStatistics().getNumberOfNodes(), initMem); //Call gc()
//        long memUsed = StatisticsLogic.getMemUsed();
//
//        //logger.info("Time: {} minutes", ((end - start) / 1000.0f) / 60.0f);
//        logger.info("CIRtree build in: {} ms", (end - start));
//        logger.info("CIRtree memory usage: {} Megabytes", ((memUsed)/1024)/1024);

        boolean ret = tree.isIndexValid();
        if (!ret)
            logger.error("Structure is INVALID!");
    }
}
