package org.ual.build;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ual.documentindex.InvertedFile;
import org.ual.spatialindex.rtree.RTree;
import org.ual.spatialindex.storage.AbstractDocumentStore;
import org.ual.spatialindex.storage.WeightEntry;
import org.ual.utils.main.StatisticsLogic;

import java.util.ArrayList;
import java.util.Comparator;

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

        // Start the timers
        long initMem = StatisticsLogic.getClearedMem();
        long startTime = System.currentTimeMillis();
        StatisticsLogic.startMemoryMonitoring();//Start monitoring

        // Build the inverted file
        ArrayList<WeightEntry> invertedIndex = tree.ir(dms, invertedFile);

        // Stop the timers
        long endTime = System.currentTimeMillis();
        //long endMem = StatisticsLogic.getMemUsed();
        StatisticsLogic.stopMemoryMonitoring(); //Stop monitoring

        //StatisticsLogic.irTreePeakMemUsed = (endMem - initMem);
        StatisticsLogic.irTreePeakMemUsed = (StatisticsLogic.getMaxMemoryUsage() - initMem); // This should be more accureate than the previous line, taking into account the peak memory spkikes triggering gc() calls
        StatisticsLogic.irTreeMemUsed = StatisticsLogic.getClearedMem() - initMem;
        StatisticsLogic.irTreeJVMPeakMemUsed = StatisticsLogic.getMaxMemoryUsage();
        StatisticsLogic.irTreeBuildTime = (endTime - startTime);

        logger.info("IRtree build in: {} ms", StatisticsLogic.irTreeBuildTime);
        logger.info("IRtree memory usage: {} Megabytes", (StatisticsLogic.irTreeMemUsed/1024)/1024);
        logger.info("IRtree peak memory usage: {} Megabytes", (StatisticsLogic.irTreePeakMemUsed/1024)/1024);
        logger.info("IRtree JVM peak memory usage: {} Megabytes", (StatisticsLogic.irTreeJVMPeakMemUsed/1024)/1024);

        boolean ret = tree.isIndexValid();
        if (!ret)
            logger.error("Structure is INVALID!");

        return invertedFile;
    }
}
