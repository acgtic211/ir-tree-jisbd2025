package org.ual;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ual.algorithm.aggregator.AggregatorFactory;
import org.ual.algorithm.aggregator.IAggregator;
import org.ual.spatialindex.parameters.Dataset;
import org.ual.spatialindex.parameters.ParametersFactory;
import org.ual.spatialindex.parameters.DatasetParameters;
import org.ual.spatialindex.rtree.RTree;
import org.ual.utils.ResultQueryTotal;
import org.ual.utils.main.IndexLogic;
import org.ual.utils.main.QueryLogic;
import org.ual.utils.main.StatisticsLogic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;

public class NewMain {
    static int fanout = 50;
    static double fillFactor = 0.7;
    static int dimension = 2;
    static double betaArea = 0.1;   // RtreeEnhanced betaArea 0.9
    static int maxWord = 2; // RtreeEnhanced maxWord 10000
    static int numClusters = 4; // CIRtree clusters
    static int numMoves = 4; // KMean numMoves

//    static String keywordsFilePath = "src/main/resources/data/postal_doc.txt";
//    static String locationsFilePath = "src/main/resources/data/postal_loc.txt";
//    static String keywordsFilePath = "src/main/resources/data/sports_doc.txt";
//    static String locationsFilePath = "src/main/resources/data/sports_loc.txt";
//    static String keywordsFilePath = "src/main/resources/data/hotel_doc";
//    static String locationsFilePath = "src/main/resources/data/hotel_loc";
//    static String keywordsFilePath = "src/main/resources/data/key_test.txt";
//    static String locationsFilePath = "src/main/resources/data/loc_test.txt";
//    static String keywordsFilePath = "src/main/resources/data/keywords.txt";
//    static String locationsFilePath = "src/main/resources/data/locations.txt";

    static DatasetParameters parameters;
    static Dataset dataset;

    // Results and Temp paths
    static String tempDirectoryPath = "src/main/resources/temp/";
    static String resultsDirectoryPath = "src/main/resources/results/";
    static String metricsDirectoryPath = "src/main/resources/results/metrics/";
    static String logDirectoryPath = "src/main/resources/log/";

    // Datastructures
    enum DataStructureType {
        HashMap,
        TreeMap
    }
    static DataStructureType selectedDataStructure;

    // Spatial Index Type
    enum SpatialIndexType {
        IR,
        IR_Bulk,
        DIR,
        CIR,
        CDIR
    }
    static SpatialIndexType selectedSpatialIndex;

    // BulkLoader Method
    static RTree.BulkLoadMethod bulkLoadMethod = RTree.BulkLoadMethod.BLM_STR;

    enum QueryTypeGroup {
        AGGREGATE,
        RANGE,
        KNN
    }
    static QueryTypeGroup selectedQueryTypeGroup;

    static IAggregator selectedAggregator;

    // Specify aggregate query types to use
    static QueryLogic.AggregateQueryType[] aggregateQueryTypes = {
            QueryLogic.AggregateQueryType.GNNK,
            QueryLogic.AggregateQueryType.SGNNK };

    // Specify knn query types to use
    static QueryLogic.KnnQueryType[] knnQueryTypes = {
            QueryLogic.KnnQueryType.BkSK,
            QueryLogic.KnnQueryType.TkSK};

    // Specify range query types to use
    static QueryLogic.RangeQueryType[] rangeQueryTypes = {
            QueryLogic.RangeQueryType.BRSK };

    // Specify join query types to use
    // TODO implement JOIN

    enum QueryParameters {
        GroupSize,
        PercentQuery,
        NumberOfKeywords,
        SpaceAreaPercentage,
        KeywordSpaceSizePercentage,
        TopK,
        Alpha,
        Radius,
        All
    }
    static QueryParameters selectedQueryParameter;

    // Number of keywords
    static int numberOfQueries = 20;

    static int[] groupSizes = {10, 20, 40, 60, 80}; // Group Size
    static int groupSizeDefault = 10;
    static int[] mPercentages = {40, 50, 60, 70, 80};
    static int mPercentageDefault = 60;
    static int[] numberOfKeywords = {1, 2, 4, 8, 10};
    static int numberOfKeywordsDefault = 4;
    static double[] querySpaceAreaPercentages = {.001, .01, .02, .03, .04, .05};
    static double querySpaceAreaPercentageDefault = 0.01;
    static int[] keywordSpaceSizePercentages = {1, 2, 3, 4, 5};
    static int keywordSpaceSizePercentageDefault = 3;
    static int[] topks = {1, 10, 20, 30, 40, 50};
    //static int[] topks = {1, 10, 100, 200, 400, 600, 800, 1000};
    static int topkDefault = 10;
    static double[] alphas = {0.1, 0.3, 0.5, 0.7, 0.9};
    static double alphaDefault = 0.5;
    static float[] radius = {1f, 2f, 5f, 10f, 20f};
    //static float[] radius = {1f, 2f, 4f, 6f, 8f, 10f, 12f, 14f, 16f, 18f, 20f, 40f, 60f, 80f, 100f, 120f};
    static float radiusDefault = 10f;

    static ResultQueryTotal globalQueryResults;

    private static final Logger logger = LogManager.getLogger(NewMain.class);


    public static void main(String[] argv) {
        // ****************************************************** //
        //                    CLEANING DATA                       //
        // ****************************************************** //
        // Create directories (log, temp and results) if not present
        createDirectoryTree();

        // Clear old indexes from temp directory
        clearTempDirectory();

        // Clear results directory
        clearResultsDirectory();

        // ****************************************************** //
        //                    PROCESSING DATA                     //
        // ****************************************************** //

        // Select Dataset
        chooseDataSetMenu();

        // Init Statistics Logic
        StatisticsLogic statisticsLogic = new StatisticsLogic(metricsDirectoryPath);

        // Init Index Logic
        IndexLogic indexLogic = new IndexLogic(statisticsLogic, parameters);

        // Select Document Index Structure Type & Compute weights and store in memory
        chooseDocumentDataStructureMenu();

        if (selectedDataStructure == DataStructureType.HashMap) {
            indexLogic.createHashMapDS();
        } else if (selectedDataStructure == DataStructureType.TreeMap) {
            indexLogic.createTreeMapDS();
        } else {
            logger.error("Data Structure type selected is invalid: {}; exiting...", selectedDataStructure.toString());
            System.exit(-1);
        }

        // Select SpatialIndex Tree Type
        chooseSpatialTypeMenu();

        if(selectedSpatialIndex == SpatialIndexType.IR) {
            indexLogic.createIRtree(fanout, fillFactor, dimension);
        } else if(selectedSpatialIndex == SpatialIndexType.IR_Bulk) {
            indexLogic.createIRtreeWithBulkLoading(fanout, fillFactor, dimension, bulkLoadMethod);//fanout, fanout);//, 10000, 100);// TODO expose more parameters
        } else if(selectedSpatialIndex == SpatialIndexType.DIR) {
            indexLogic.createDIRtree(fanout, fillFactor, dimension, maxWord, betaArea);
        } else if(selectedSpatialIndex == SpatialIndexType.CIR) {
            indexLogic.createCIRtree(fanout, fillFactor, dimension, numClusters, numMoves);
        } else if(selectedSpatialIndex == SpatialIndexType.CDIR) {
            indexLogic.createCDIRtree(fanout, fillFactor, dimension, maxWord, betaArea, numClusters, numMoves);
        } else {
            logger.error("Invalid Spatial-Index type selected: {}", selectedSpatialIndex.toString());
            System.exit(-1);
        }


        // ****************************************************** //
        //                       QUERIES                          //
        // ****************************************************** //

        QueryLogic queryLogic = new QueryLogic(indexLogic, statisticsLogic, resultsDirectoryPath, parameters, true);
        queryLogic.initQueryVariables(groupSizes, groupSizeDefault, mPercentages, mPercentageDefault, numberOfKeywords, numberOfKeywordsDefault,
                querySpaceAreaPercentages, querySpaceAreaPercentageDefault, keywordSpaceSizePercentages, keywordSpaceSizePercentageDefault, topks, topkDefault,
                alphas, alphaDefault, radius, radiusDefault, numberOfQueries);

//        // TEST
//        queryLogic.processKnnQuery(new QueryLogic.KnnQueryType[]{QueryLogic.KnnQueryType.BkSK});

        do {
            // Choose number of iterations
            int numIterations = chooseNumberIterationsMenu();

            // Launch Query Group Selector Menu
            chooseQueryTypeGroupMenu();


            // ****************************************************** //
            //                      AGGREGATE                         //
            // ****************************************************** //
            if (selectedQueryTypeGroup == QueryTypeGroup.AGGREGATE) {
                // Launch Aggregator Menu
                chooseAggregateTypeMenu();

                // Query generation and evaluation
                // Result set
                globalQueryResults = new ResultQueryTotal("Aggregate");
                queryLogic.setQueryResults(globalQueryResults);

                // Launch Query parameter Menu
                ArrayList<QueryLogic.QueryType> queryTypes = new ArrayList<>(7);
                chooseAggregateQueryTypeMenu(queryTypes);

                // Process aggregate queries
                for (int i = 0; i < numIterations; i++) {
                    queryLogic.processAggregateQuery(aggregateQueryTypes, queryTypes, selectedAggregator);
                    queryLogic.printStats();
                }
            }


            // ****************************************************** //
            //                         RANGE                          //
            // ****************************************************** //
            if (selectedQueryTypeGroup == QueryTypeGroup.RANGE) {
                // Query generation and evaluation
                // Result set
                globalQueryResults = new ResultQueryTotal("Range");
                queryLogic.setQueryResults(globalQueryResults);

                // Launch Query parameter Menu
                ArrayList<QueryLogic.QueryType> queryTypes = new ArrayList<>(7);
                chooseRangeQueryTypeMenu(queryTypes);

                // Process aggregate queries
                for(int i = 0; i < numIterations; i++) {
                    queryLogic.processRangeQuery(rangeQueryTypes, queryTypes);
                    queryLogic.printStats();
                }
            }

            // ****************************************************** //
            //                         KNN                            //
            // ****************************************************** //
            if (selectedQueryTypeGroup == QueryTypeGroup.KNN) {
                // Query generation and evaluation
                // Result set
                globalQueryResults = new ResultQueryTotal("KNN");
                queryLogic.setQueryResults(globalQueryResults);

                // Launch Query Menu
                ArrayList<QueryLogic.QueryType> queryTypes = new ArrayList<>(7);
                chooseKnnQueryTypeMenu(queryTypes);

                // Process aggregate queries
                for(int i = 0; i < numIterations; i++) {
                    queryLogic.processKnnQuery(knnQueryTypes, queryTypes);
                    queryLogic.printStats();
                }
            }


            logger.info("---------------------------------");
            logger.info(" All queries have been evaluated ");
            logger.info("---------------------------------");

            //queryLogic.printStats();



//            logger.info("Query Times:");
//            for (long time : memTimes)
//                logger.info("{} ms", time);
//
//
//            // Always write data stats
//            writeResults();
//            logger.info("Writing results in disk ...");

//            boolean exitLoop = printExitLoopMenu();
//
//            // Temp fix to do multiple queries
//            if (exitLoop) {
//                logger.info("Writing results in disk ...");
//                //writeResults();
//                //writeResults(memResults, "[MEM]");
//            } else {
//                logger.info("Discarding Results ...");
//                break; // TODO DELETE
//            }


        logger.info("Exiting...");

        } while (chooseExitLoopMenu());

    }


    //******************************************************************//
    //                              MENUS                               //
    //******************************************************************//

    public static void chooseDataSetMenu() {
        System.out.println("\nChoose dataset:");
        System.out.println("-------------------------\n");
        System.out.println("\t1 - Postal codes");
        System.out.println("\t2 - Sports");
        System.out.println("\t3 - Hotel");
        System.out.println("\t4 - Test");
        System.out.println("\t");
        System.out.println("\t0 - Quit");
        System.out.println("-------------------------\n");

        // Start Simple Menu
        Scanner input = new Scanner(System.in);

        // Aggregator selection
        int selection = input.nextInt();

        switch (selection) {
            case 1:
                dataset = Dataset.POSTAL_CODES_SET;
                parameters = ParametersFactory.getParameters(dataset);
                break;
            case 2:
                dataset = Dataset.SPORTS_SET;
                parameters = ParametersFactory.getParameters(Dataset.SPORTS_SET);
                break;
            case 3:
                dataset = Dataset.HOTEL_SET;
                parameters = ParametersFactory.getParameters(Dataset.HOTEL_SET);
                break;
            case 4:
                dataset = Dataset.TESTING_SET;
                parameters = ParametersFactory.getParameters(Dataset.TESTING_SET);
                break;
            default:
                logger.error("Wrong dataset selected. Exiting...");
                System.exit(0);
        }

        logger.info("Dataset selected: {}", dataset.toString());
    }


    public static void chooseDocumentDataStructureMenu() {
        System.out.println("\nChoose document index type:");
        System.out.println("-------------------------\n");
        System.out.println("\t1 - HashMap");
        System.out.println("\t2 - TreeMap");
        System.out.println("\t");
        System.out.println("\t0 - Quit");
        System.out.println("-------------------------\n");

        // Start Simple Menu
        Scanner input = new Scanner(System.in);

        // Aggregator selection
        int selection = input.nextInt();

        switch (selection) {
            case 1:
                selectedDataStructure = DataStructureType.HashMap;
                break;
            case 2:
                selectedDataStructure = DataStructureType.TreeMap;
                break;
            default:
                logger.error("Wrong document index type selected. Exiting...");
                System.exit(0);
        }

        logger.info("Document index type selected: {}", selectedDataStructure.toString());
    }


    public static void chooseSpatialTypeMenu() {
        System.out.println("\nChoose Spatio-Textual tree type");
        System.out.println("-------------------------\n");
        System.out.println("\t1 - IR-Tree");
        System.out.println("\t2 - IR-Tree with Bulk Loading");
        System.out.println("\t3 - DIR-Tree");
        System.out.println("\t4 - CIR-Tree");
        System.out.println("\t5 - CDIR-Tree");
        System.out.println("\t");
        System.out.println("\t0 - Quit");
        System.out.println("-------------------------\n");

        // Start Simple Menu
        Scanner input = new Scanner(System.in);

        // Tree selection
        int selection = input.nextInt();

        switch (selection) {
            case 1:
                selectedSpatialIndex = SpatialIndexType.IR;
                break;
            case 2:
                selectedSpatialIndex = SpatialIndexType.IR_Bulk;
                break;
            case 3:
                selectedSpatialIndex = SpatialIndexType.DIR;
                break;
            case 4:
                selectedSpatialIndex = SpatialIndexType.CIR;
                break;
            case 5:
                selectedSpatialIndex = SpatialIndexType.CDIR;
                break;
            default:
                logger.error("Wrong Spatio-Textual tree type selected. Exiting...");
                System.exit(0);
        }
        logger.info("Spatio-Textual tree type selected: {}", selectedSpatialIndex.toString());
    }

    public static void chooseQueryTypeGroupMenu() {
        System.out.println("\nChoose Spatial Query type");
        System.out.println("-------------------------\n");
        System.out.println("\t1 - Aggregate SK");
        System.out.println("\t2 - Range SK");
        System.out.println("\t3 - Knn SK");
        System.out.println("\t");
        System.out.println("\t0 - Quit");
        System.out.println("-------------------------\n");

        // Start Simple Menu
        Scanner input = new Scanner(System.in);

        // Aggregator selection
        int selection = input.nextInt();

        switch (selection) {
            case 1:
                selectedQueryTypeGroup = QueryTypeGroup.AGGREGATE;
                break;
            case 2:
                selectedQueryTypeGroup = QueryTypeGroup.RANGE;
                break;
            case 3:
                selectedQueryTypeGroup = QueryTypeGroup.KNN;
                break;
            default:
                logger.error("Wrong Spatial Query type selected. Exiting...");
                System.exit(0);
        }
        logger.info("Spatial Query type selected: {}", selectedQueryTypeGroup.toString());
    }

    public static void chooseAggregateTypeMenu() {
        System.out.println("\nChoose aggregator type");
        System.out.println("-------------------------\n");
        System.out.println("\t1 - SUM");
        System.out.println("\t2 - MAX");
        System.out.println("\t");
        System.out.println("\t0 - Quit");
        System.out.println("-------------------------\n");

        // Start Simple Menu
        Scanner input = new Scanner(System.in);
        String aggregator = "";

        // Aggregator selection
        int selection = input.nextInt();

        switch (selection) {
            case 1:
                aggregator = "SUM";
                selectedAggregator = AggregatorFactory.getAggregator(aggregator);
                break;
            case 2:
                aggregator = "MAX";
                selectedAggregator = AggregatorFactory.getAggregator(aggregator);
                break;
            default:
                logger.error("Wrong aggregator type selected. Exiting...");
                System.exit(0);
        }
        logger.info("Aggregator type selected: {}", aggregator);
    }


    public static void chooseAggregateQueryTypeMenu(ArrayList<QueryLogic.QueryType> queryTypes) {
        System.out.println("\nChoose query parameter for Aggregate Query");
        System.out.println("-------------------------\n");
        System.out.println("\t1 - Group Size");
        System.out.println("\t2 - Percent");
        System.out.println("\t3 - Number of Keywords");
        System.out.println("\t4 - Space Area Percentage");
        System.out.println("\t5 - Keyword Space Size Percentage");
        System.out.println("\t6 - Top K");
        System.out.println("\t7 - Alpha");
        System.out.println("\t");
        System.out.println("\t8 - Run All Queries");
        System.out.println("\t");
        System.out.println("\t0 - Quit");
        System.out.println("-------------------------\n");

        // Start Simple Menu
        Scanner input = new Scanner(System.in);

        // Aggregator selection
        int selection = input.nextInt();

        switch (selection) {
            case 1:
                queryTypes.add(QueryLogic.QueryType.GroupSize);
                selectedQueryParameter = QueryParameters.GroupSize;
                break;
            case 2:
                queryTypes.add(QueryLogic.QueryType.Percentage);
                selectedQueryParameter = QueryParameters.PercentQuery;
                break;
            case 3:
                queryTypes.add(QueryLogic.QueryType.NumberOfKeywords);
                selectedQueryParameter = QueryParameters.NumberOfKeywords;
                break;
            case 4:
                queryTypes.add(QueryLogic.QueryType.SpaceAreaPercentage);
                selectedQueryParameter = QueryParameters.SpaceAreaPercentage;
                break;
            case 5:
                queryTypes.add(QueryLogic.QueryType.KeywordSpaceSizePercentage);
                selectedQueryParameter = QueryParameters.KeywordSpaceSizePercentage;
                break;
            case 6:
                queryTypes.add(QueryLogic.QueryType.TopK);
                selectedQueryParameter = QueryParameters.TopK;
                break;
            case 7:
                queryTypes.add(QueryLogic.QueryType.Alpha);
                selectedQueryParameter = QueryParameters.Alpha;
                break;
            case 8:
                queryTypes.add(QueryLogic.QueryType.GroupSize);
                queryTypes.add(QueryLogic.QueryType.Percentage);
                queryTypes.add(QueryLogic.QueryType.NumberOfKeywords);
                queryTypes.add(QueryLogic.QueryType.SpaceAreaPercentage);
                queryTypes.add(QueryLogic.QueryType.KeywordSpaceSizePercentage);
                queryTypes.add(QueryLogic.QueryType.TopK);
                queryTypes.add(QueryLogic.QueryType.Alpha);
                selectedQueryParameter = QueryParameters.All;
                break;
            default:
                logger.error("Wrong query parameter selected for aggregate queries. Exiting...");
                System.exit(0);
        }
        logger.info("Query parameter selected for Aggregate: {}", selectedQueryParameter.toString());
    }

    public static void chooseRangeQueryTypeMenu(ArrayList<QueryLogic.QueryType> queryTypes) {
        System.out.println("\nChoose query parameter for Range Query");
        System.out.println("-------------------------\n");
        System.out.println("\t1 - Number of Keywords");
        System.out.println("\t2 - Space Area Percentage");
        System.out.println("\t3 - Keyword Space Size Percentage");
        System.out.println("\t4 - Range");
        System.out.println("\t5 - Alpha");
        System.out.println("\t");
        System.out.println("\t6 - Run All Queries");
        System.out.println("\t");
        System.out.println("\t0 - Quit");
        System.out.println("-------------------------\n");

        // Start Simple Menu
        Scanner input = new Scanner(System.in);

        // Aggregator selection
        int selection = input.nextInt();

        switch (selection) {
            case 1:
                queryTypes.add(QueryLogic.QueryType.NumberOfKeywords);
                selectedQueryParameter = QueryParameters.NumberOfKeywords;
                break;
            case 2:
                queryTypes.add(QueryLogic.QueryType.SpaceAreaPercentage);
                selectedQueryParameter = QueryParameters.SpaceAreaPercentage;
                break;
            case 3:
                queryTypes.add(QueryLogic.QueryType.KeywordSpaceSizePercentage);
                selectedQueryParameter = QueryParameters.KeywordSpaceSizePercentage;
                break;
            case 4:
                queryTypes.add(QueryLogic.QueryType.Radius);
                selectedQueryParameter = QueryParameters.Radius;
                break;
            case 5:
                queryTypes.add(QueryLogic.QueryType.Alpha);
                selectedQueryParameter = QueryParameters.Alpha;
                break;
            case 6:
                queryTypes.add(QueryLogic.QueryType.NumberOfKeywords);
                queryTypes.add(QueryLogic.QueryType.SpaceAreaPercentage);
                queryTypes.add(QueryLogic.QueryType.KeywordSpaceSizePercentage);
                queryTypes.add(QueryLogic.QueryType.Radius);
                queryTypes.add(QueryLogic.QueryType.Alpha);
                selectedQueryParameter = QueryParameters.All;
                break;
            default:
                logger.error("Wrong query parameter selected for range queries. Exiting...");
                System.exit(0);
        }
        logger.info("Query parameter selected for Range: {}", selectedQueryParameter.toString());
    }


    public static void chooseKnnQueryTypeMenu(ArrayList<QueryLogic.QueryType> queryTypes) {
        System.out.println("\nChoose query parameter for KNN Query");
        System.out.println("-------------------------\n");
        System.out.println("\t1 - Number of Keywords");
        System.out.println("\t2 - Space Area Percentage");
        System.out.println("\t3 - Keyword Space Size Percentage");
        System.out.println("\t4 - TopK");
        System.out.println("\t5 - Alpha");
        System.out.println("\t");
        System.out.println("\t6 - Run All Queries");
        System.out.println("\t");
        System.out.println("\t0 - Quit");
        System.out.println("-------------------------\n");

        // Start Simple Menu
        Scanner input = new Scanner(System.in);

        // Aggregator selection
        int selection = input.nextInt();

        switch (selection) {
            case 1:
                queryTypes.add(QueryLogic.QueryType.NumberOfKeywords);
                selectedQueryParameter = QueryParameters.NumberOfKeywords;
                break;
            case 2:
                queryTypes.add(QueryLogic.QueryType.SpaceAreaPercentage);
                selectedQueryParameter = QueryParameters.SpaceAreaPercentage;
                break;
            case 3:
                queryTypes.add(QueryLogic.QueryType.KeywordSpaceSizePercentage);
                selectedQueryParameter = QueryParameters.KeywordSpaceSizePercentage;
                break;
            case 4:
                queryTypes.add(QueryLogic.QueryType.TopK);
                selectedQueryParameter = QueryParameters.TopK;
                break;
            case 5:
                queryTypes.add(QueryLogic.QueryType.Alpha);
                selectedQueryParameter = QueryParameters.Alpha;
                break;
            case 6:
                queryTypes.add(QueryLogic.QueryType.NumberOfKeywords);
                queryTypes.add(QueryLogic.QueryType.SpaceAreaPercentage);
                queryTypes.add(QueryLogic.QueryType.KeywordSpaceSizePercentage);
                queryTypes.add(QueryLogic.QueryType.TopK);
                queryTypes.add(QueryLogic.QueryType.Alpha);
                selectedQueryParameter = QueryParameters.All;
                break;
            default:
                logger.error("Wrong query parameter selected for knn queries. Exiting...");
                System.exit(0);
        }
        logger.info("Query parameter selected for KNN: {}", selectedQueryParameter.toString());
    }

    private static int chooseNumberIterationsMenu() {
        System.out.println("\nInsert number of iterations: ");

        // Start Simple Menu
        Scanner input = new Scanner(System.in);

        // Number of iterations selection
        int selection = input.nextInt();

        return selection;
    }

    private static boolean chooseExitLoopMenu() {
        System.out.println("\nGenerate another query?");
        System.out.println("-------------------------\n");
        System.out.println("\t 1 - Yes");
        System.out.println("\t 2 - No");
        System.out.println("-------------------------\n");

        // Start Simple Menu
        Scanner input = new Scanner(System.in);
        boolean write = false;

        // Aggregator selection
        int selection = input.nextInt();

        switch (selection) {
            case 1:
                write = true;
                break;
            case 2:
                break;
            default:
        }

        return write;
    }



    //******************************************************************//
    //                         UTILITY METHODS                          //
    //******************************************************************//

    public static void createDirectoryTree() {
        // Create directories if not present
        try {
            logger.info("Creating directory tree in resources...");
            Files.createDirectories(Paths.get(metricsDirectoryPath));
            Files.createDirectories(Paths.get(tempDirectoryPath));
            Files.createDirectories(Paths.get(logDirectoryPath));
            logger.info("Done");
        } catch (IOException e) {
            logger.error("Fail to create directories", e);
            throw new RuntimeException(e);
        }
    }

    public static void clearTempDirectory() {
        // Delete old temp files
        File file = new File(tempDirectoryPath);
        logger.info("Deleting existing temp files ...");
        deleteFilesInPath(file);
        logger.info("Done");
    }

    public static void clearResultsDirectory() {
        File file = new File(resultsDirectoryPath);
        logger.info("Deleting existing result files ...");
        deleteFilesInPath(file);
        logger.info("Done");
    }

    private static void deleteFilesInPath(File file) {
        // Check if file is a directory
        if (file.isDirectory()) {
            String[] dirFiles = file.list();

            if (dirFiles != null) {
                for (String filePath : dirFiles) {
                    File dirFile = new File(file, filePath);
                    //System.out.println(dirFile);

                    if (!dirFile.isDirectory()) {
                        dirFile.delete();
                    } else {
                        deleteFilesInPath(dirFile); // recursive call to delete cpu and io directories
                    }
                }
            }
        } else {
            //file.delete();
            logger.error("Path is not a directory...");
        }
    }
}
