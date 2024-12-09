package org.ual.utils.main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ual.algorithm.kmean.KMean;
import org.ual.build.*;
import org.ual.document.WeightCompute;
import org.ual.documentindex.InvertedFile;
import org.ual.spatialindex.rtree.RTree;
import org.ual.spatialindex.rtreeenhanced.RTreeEnhanced;
import org.ual.spatialindex.spatialindex.ISpatialIndex;
import org.ual.spatialindex.storage.AbstractDocumentStore;
import org.ual.spatialindex.storage.HashMapDocumentStore;
import org.ual.spatialindex.storage.TreeMapDocumentStore;

import java.util.HashMap;

public class IndexLogic {
    static AbstractDocumentStore weightIndex;
    public static ISpatialIndex spatialIndex;
    static InvertedFile invertedFile;
    static HashMap<Integer, Integer> clusterTree;

    StatisticsLogic statisticsLogic;

    private static final Logger logger = LogManager.getLogger(IndexLogic.class);

    public IndexLogic(StatisticsLogic statisticsLogic) {
        invertedFile = new InvertedFile();
        this.statisticsLogic = statisticsLogic;
    }

    public void createHashMapDS(String keywordsFilePath) {
        weightIndex = new HashMapDocumentStore();
        logger.info("Computing Term Weights in Memory using HashMap");
        WeightCompute.ComputeTermWeights(keywordsFilePath, weightIndex);
        logger.info("{} Keywords computed.", weightIndex.getSize());
    }

    public void createTreeMapDS(String keywordsFilePath) {
        weightIndex = new TreeMapDocumentStore();
        logger.info("Computing Term Weights in Memory using TreeMap");
        WeightCompute.ComputeTermWeights(keywordsFilePath, weightIndex);
        logger.info("{} Keywords computed.", weightIndex.getSize());
    }
    
    public void createIRtree(String locationsFilePath, int fanout, double fillFactor, int dimension) {
        // Build RTree index with location data
        createRtree(locationsFilePath, fanout, fillFactor, dimension);

        // TODO Testing bulk loading
        //createRtreeWithBulkLoading(locationsFilePath, fanout, fillFactor, dimension);

        // Build IRTree index with spatio-textual data
        logger.info("Creating IR-Tree");
        invertedFile = BuildIRTree.buildTreeIR((RTree) spatialIndex, weightIndex);
        logger.info("Done");
    }

    public void createDIRtree(String locationsFilePath, int fanout, double fillFactor, int dimension, int maxWord, double betaArea) {
        // Build Enhanced RTree index with location data
        createEnhancedRtree(locationsFilePath, fanout, fillFactor, dimension, maxWord, betaArea);

        // Build DIRTree index with spatio-textual data
        logger.info("Creating DIR-Tree");
        invertedFile = BuildDIRTree.buildTreeDIR((RTreeEnhanced) spatialIndex, weightIndex);
        logger.info("Done");
    }

    public void createCIRtree(String locationsFilePath, int fanout, double fillFactor, int dimension, int numClusters, int numMoves) {
        // Calculate cluster file with Kmean
        createClusterTree(numClusters, numMoves);

        // Build RTree index with location data
        createRtree(locationsFilePath, fanout, fillFactor, dimension);

        // Build IRTree index with spatio-textual data
        logger.info("Creating CIR-Tree...");
        BuildCIRTree.buildTreeCIR((RTree) spatialIndex, weightIndex, clusterTree, invertedFile, numClusters);
        logger.info("Done");
    }

    public void createCDIRtree(String locationsFilePath, int fanout, double fillFactor, int dimension, int maxWord, double betaArea, int numClusters, int numMoves) {
        // Calculate cluster file with Kmean
        createClusterTree(numClusters, numMoves);

        // Build Enhanced RTree index with location data
        createEnhancedRtree(locationsFilePath, fanout, fillFactor, dimension, maxWord, betaArea);

        // Build IRTree index with spatio-textual data
        logger.info("Creating CDIR-Tree...");
        BuildCDIRTree.buildTreeCDIR((RTreeEnhanced) spatialIndex, weightIndex, clusterTree, invertedFile, numClusters);
        logger.info("Done");
    }

    private void createRtree(String locationsFilePath, int fanout, double fillFactor, int dimension) {
        logger.info("Creating R-Tree with parameters: \nfanout:{} \nfillfactor:{} \ndimensions:{}", fanout, fillFactor, dimension);
        spatialIndex = BuildRTree.buildRTree(locationsFilePath, fanout, fillFactor, dimension);
        logger.info("Done");
    }

    private void createRtreeWithBulkLoading(String locationsFilePath, int fanout, double fillFactor, int dimension) {
        logger.info("Creating R-Tree with parameters: \nfanout:{} \nfillfactor:{} \ndimensions:{}", fanout, fillFactor, dimension);
        spatialIndex = BuildRTree.buildRTreeSTR(locationsFilePath, fanout, fillFactor, dimension);
        logger.info("Done");
    }

    private void createEnhancedRtree(String locationsFilePath, int fanout, double fillFactor, int dimension, int maxWord, double betaArea) {
        logger.info("Creating Enhanced R-Tree with parameters: \nfanout:{} \nfillfactor:{} \ndimensions:{} \nmaxWord:{} \nbetaArea:{}", fanout, fillFactor, dimension, maxWord, betaArea);
        spatialIndex = BuildRTreeEnhanced.buildEnhancedRTree(locationsFilePath, fanout, fillFactor, dimension, maxWord, weightIndex, betaArea);
        logger.info("Done");
    }

    private void createClusterTree(int numClusters, int numMoves){
        logger.info("Creating cluster tree with Kmean medoids...");
        clusterTree = KMean.calculateKMean(weightIndex, numClusters, numMoves);
        logger.info("Done");
    }


}
