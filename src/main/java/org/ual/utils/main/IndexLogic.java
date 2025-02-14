package org.ual.utils.main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ual.algorithm.kmean.KMean;
import org.ual.build.*;
import org.ual.document.WeightCompute;
import org.ual.documentindex.InvertedFile;
import org.ual.spatialindex.parameters.DatasetParameters;
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
    static InvertedFile invertedFile; // TODO CHANGE TO PRIVATE
    static HashMap<Integer, Integer> clusterTree;
    DatasetParameters datasetParameters;

    StatisticsLogic statisticsLogic;

    private static final Logger logger = LogManager.getLogger(IndexLogic.class);

    public IndexLogic(StatisticsLogic statisticsLogic, DatasetParameters datasetParameters) {
        this.datasetParameters = datasetParameters;
        invertedFile = new InvertedFile();
        this.statisticsLogic = statisticsLogic;
    }

//    @Deprecated
//    public void createHashMapDS(String keywordsFilePath) {
//        weightIndex = new HashMapDocumentStore();
//        logger.info("Computing Term Weights in Memory using HashMap");
//        WeightCompute.ComputeTermWeights(keywordsFilePath, weightIndex);
//        logger.info("{} Keywords computed.", weightIndex.getSize());
//    }

    public void createHashMapDS() {
        weightIndex = new HashMapDocumentStore();
        logger.info("Computing Term Weights in Memory using HashMap");
        WeightCompute.ComputeTermWeights(datasetParameters.keywordFile, weightIndex);
        logger.info("{} Keywords computed.", weightIndex.getSize());
    }

//    @Deprecated
//    public void createTreeMapDS(String keywordsFilePath) {
//        weightIndex = new TreeMapDocumentStore();
//        logger.info("Computing Term Weights in Memory using TreeMap");
//        WeightCompute.ComputeTermWeights(keywordsFilePath, weightIndex);
//        logger.info("{} Keywords computed.", weightIndex.getSize());
//    }

    public void createTreeMapDS() {
        weightIndex = new TreeMapDocumentStore();
        logger.info("Computing Term Weights in Memory using TreeMap");
        WeightCompute.ComputeTermWeights(datasetParameters.keywordFile, weightIndex);
        logger.info("{} Keywords computed.", weightIndex.getSize());
    }

//    @Deprecated
//    public void createIRtree(String locationsFilePath, int fanout, double fillFactor, int dimension) {
//        // Build RTree index with location data
//        createRtree(locationsFilePath, fanout, fillFactor, dimension);
//
//        // TODO Testing bulk loading
//        //createRtreeWithBulkLoading(locationsFilePath, fanout, fillFactor, dimension);
//
//        // Build IRTree index with spatio-textual data
//        logger.info("Creating IR-Tree");
//        invertedFile = BuildIRTree.buildTreeIR((RTree) spatialIndex, weightIndex);
//        logger.info("Done");
//    }

    public void createIRtree(int fanout, double fillFactor, int dimension) {
        // Build RTree index with location data
        createRtree(fanout, fillFactor, dimension);

        // TODO Testing bulk loading
        //createRtreeWithBulkLoading(locationsFilePath, fanout, fillFactor, dimension);

        // Build IRTree index with spatio-textual data
        logger.info("Creating IR-Tree");
        invertedFile = BuildIRTree.buildTreeIR((RTree) spatialIndex, weightIndex);
        logger.info("Done");
    }

    //TODO MISSING BULK LOADING METHOD
    public void createIRtreeWithBulkLoading(int fanout, double fillFactor, int dimension, RTree.BulkLoadMethod bulkLoadMethod) {//int indexCapacity, int leafCapacity, int pageSize, int numPages) {
        // Build RTree index with location data
        //createRtree(datasetParameters.locationFile, fanout, fillFactor, dimension);

        // TODO Testing bulk loading
        createRtreeWithBulkLoading(fanout, fillFactor, dimension, bulkLoadMethod);//indexCapacity, leafCapacity, pageSize, numPages);

        // Build IRTree index with spatio-textual data
        logger.info("Creating IR-Tree");
        invertedFile = BuildIRTree.buildTreeIR((RTree) spatialIndex, weightIndex);
        logger.info("Done");
    }

    public void createDIRtree(int fanout, double fillFactor, int dimension, int maxWord, double betaArea) {
        // Build Enhanced RTree index with location data
        createEnhancedRtree(fanout, fillFactor, dimension, maxWord, betaArea);

        // Build DIRTree index with spatio-textual data
        logger.info("Creating DIR-Tree");
        invertedFile = BuildDIRTree.buildTreeDIR((RTreeEnhanced) spatialIndex, weightIndex);
        logger.info("Done");
    }

    public void createCIRtree(int fanout, double fillFactor, int dimension, int numClusters, int numMoves) {
        // Calculate cluster file with Kmean
        createClusterTree(numClusters, numMoves);

        // Build RTree index with location data
        createRtree(fanout, fillFactor, dimension);

        // Build IRTree index with spatio-textual data
        logger.info("Creating CIR-Tree...");
        BuildCIRTree.buildTreeCIR((RTree) spatialIndex, weightIndex, clusterTree, invertedFile, numClusters);
        logger.info("Done");
    }

    public void createCDIRtree(int fanout, double fillFactor, int dimension, int maxWord, double betaArea, int numClusters, int numMoves) {
        // Calculate cluster file with Kmean
        createClusterTree(numClusters, numMoves);

        // Build Enhanced RTree index with location data
        createEnhancedRtree(fanout, fillFactor, dimension, maxWord, betaArea);

        // Build IRTree index with spatio-textual data
        logger.info("Creating CDIR-Tree...");
        BuildCDIRTree.buildTreeCDIR((RTreeEnhanced) spatialIndex, weightIndex, clusterTree, invertedFile, numClusters);
        logger.info("Done");
    }

    private void createRtree(int fanout, double fillFactor, int dimension) {
        logger.info("Creating R-Tree with parameters: \nfanout:{} \nfillfactor:{} \ndimensions:{}", fanout, fillFactor, dimension);
        spatialIndex = BuildRTree.buildRTree(datasetParameters, fanout, fillFactor, dimension);
        logger.info("Done");
    }

    // TODO FIX FANOUT
    private void createRtreeWithBulkLoading(int fanout, double fillFactor, int dimension, RTree.BulkLoadMethod bulkLoadMethod) {//int indexCapacity, int leafCapacity, int pageSize, int numPages) {
        logger.info("Creating R-Tree with parameters: \nfanout:{} \nfillfactor:{} \ndimensions:{}", fanout, fillFactor, dimension);
        spatialIndex = BuildRTree.buildRTreeSTR(datasetParameters, fanout, fillFactor, dimension, bulkLoadMethod);//indexCapacity, leafCapacity, pageSize, numPages);
        logger.info("Done");
    }

    private void createEnhancedRtree(int fanout, double fillFactor, int dimension, int maxWord, double betaArea) {
        logger.info("Creating Enhanced R-Tree with parameters: \nfanout:{} \nfillfactor:{} \ndimensions:{} \nmaxWord:{} \nbetaArea:{}", fanout, fillFactor, dimension, maxWord, betaArea);
        spatialIndex = BuildRTreeEnhanced.buildEnhancedRTree(datasetParameters, fanout, fillFactor, dimension, maxWord, weightIndex, betaArea);
        logger.info("Done");
    }

    private void createClusterTree(int numClusters, int numMoves){
        logger.info("Creating cluster tree with Kmean medoids...");
        clusterTree = KMean.calculateKMean(weightIndex, numClusters, numMoves);
        logger.info("Done");
    }


}
