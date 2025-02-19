package org.ual.build;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ual.documentindex.InvertedFile;
import org.ual.spatialindex.rtreeenhanced.RTreeEnhanced;
import org.ual.spatialindex.storage.AbstractDocumentStore;
import org.ual.spatialindex.storage.WeightEntry;
import org.ual.utils.main.StatisticsLogic;

import java.util.ArrayList;

/**
 * 1. Store text description of objects (build/StoreDocument)
 * 2. Build a R-Tree like structure that takes docuemtns similarity insto account (build/BuildDIRTree)
 * 3. Integrate inverted files into the DIRtree in (2) (build/BuildIRTree)
 */
public class BuildDIRTree {
    private static final Logger logger = LogManager.getLogger(BuildDIRTree.class);

    /**
     * Build a DIR-Tree using and enhanced R-Tree and a document store that contains the weights index
     * @param tree RtreeEnhanced
     * @param dms
     * @return inverted list (not used)
     */
    public static InvertedFile buildTreeDIR(RTreeEnhanced tree, AbstractDocumentStore dms) {
        // In memory inverted file
        InvertedFile invertedFile = new InvertedFile();

        long initMem = StatisticsLogic.getClearedMem();
        long startTime = System.currentTimeMillis();

        ArrayList<WeightEntry> invertedIndex = tree.ir(dms, invertedFile);

        long endTime = System.currentTimeMillis();
        long endMem = StatisticsLogic.getMemUsed();
        StatisticsLogic.irTreePeakMemUsed = (endMem - initMem);
        StatisticsLogic.irTreeMemUsed = StatisticsLogic.getClearedMem() - initMem;//StatisticsLogic.cleanMem((int) tree.getStatistics().getNumberOfNodes(), initMem); //Call gc()
        StatisticsLogic.irTreeBuildTime = (endTime - startTime);

        logger.info("DIRtree build in: {} ms", StatisticsLogic.irTreeBuildTime);
        logger.info("DIRtree peak memory usage: {} Megabytes", (StatisticsLogic.irTreePeakMemUsed/1024)/1024);
        logger.info("DIRtree memory usage: {} Megabytes", (StatisticsLogic.irTreeMemUsed/1024)/1024);


//        StatisticsLogic.cleanMem((int) tree.getStatistics().getNumberOfNodes(), initMem); //Call gc()
//        long memUsed = StatisticsLogic.getMemUsed();
//
//        //logger.info("Time: {} minutes", ((end - start) / 1000.0f) / 60.0f);
//        logger.info("DIRtree build in: {} ms", (end - start));
//        logger.info("DIRtree memory usage: {} Megabytes", ((memUsed)/1024)/1024);

        boolean ret = tree.isIndexValid();
        if (!ret)
            logger.error("Structure is INVALID!");

        return invertedFile;
    }
}
