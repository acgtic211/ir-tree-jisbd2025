package org.ual.build;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ual.documentindex.InvertedFile;
import org.ual.spatialindex.rtree.RTree;
import org.ual.spatialindex.storage.AbstractDocumentStore;
import org.ual.spatialindex.storage.WeightEntry;
import org.ual.utils.main.StatisticsLogic;

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
        long initMem = StatisticsLogic.getMemUsed();
        ArrayList<WeightEntry> invertedIndex = tree.ir(dms, invertedFile);
        long end = System.currentTimeMillis();
        long endMem = StatisticsLogic.getMemUsed();
        StatisticsLogic.irTreePeakMemUsed = (endMem - initMem);
        StatisticsLogic.irTreeMemUsed = StatisticsLogic.cleanMem((int) tree.getStatistics().getNumberOfNodes(), initMem); //Call gc()
        StatisticsLogic.irTreeBuildTime = (end - start);

        logger.info("IRtree build in: {} ms", StatisticsLogic.irTreeBuildTime);
        logger.info("IRtree peak memory usage: {} Megabytes", (StatisticsLogic.irTreePeakMemUsed/1024)/1024);
        logger.info("IRtree memory usage: {} Megabytes", (StatisticsLogic.irTreeMemUsed/1024)/1024);

        //StatisticsLogic.cleanMem((int) tree.getStatistics().getNumberOfNodes(), initMem); //Call gc()
        //long memUsed = StatisticsLogic.getMemUsed();

        //logger.info("Time: {} minutes", ((end - start) / 1000.0f) / 60.0f);
        //logger.info("IRtree build in: {} ms", (end - start));
        //logger.info("IRtree memory usage: {} Megabytes", ((endMem - initMem)/1024)/1024);
        //logger.info("IRtree memory usage: {} Megabytes", ((memUsed)/1024)/1024);

        boolean ret = tree.isIndexValid();
        if (!ret)
            logger.error("Structure is INVALID!");

        return invertedFile;
    }
}
