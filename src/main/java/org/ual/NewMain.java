package org.ual;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    static String keywordsFilePath = "src/main/resources/data/postal_doc.txt";
    static String locationsFilePath = "src/main/resources/data/postal_loc.txt";
//    static String keywordsFilePath = "src/main/resources/data/sports_doc.txt";
//    static String locationsFilePath = "src/main/resources/data/sports_loc.txt";
//    static String keywordsFilePath = "src/main/resources/data/hotel_doc";
//    static String locationsFilePath = "src/main/resources/data/hotel_loc";
//    static String keywordsFilePath = "src/main/resources/data/key_test.txt";
//    static String locationsFilePath = "src/main/resources/data/loc_test.txt";
//    static String keywordsFilePath = "src/main/resources/data/keywords.txt";
//    static String locationsFilePath = "src/main/resources/data/locations.txt";

    // Results and Temp paths
    static String tempDirectoryPath = "src/main/resources/temp/";
    static String resultsDirectoryPath = "src/main/resources/results/";
    static String metricsDirectoryPath = "src/main/resources/results/metrics/";
    static String logDirectoryPath = "src/main/resources/log/";

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

        // Statistics Logic
        StatisticsLogic statisticsLogic = new StatisticsLogic(metricsDirectoryPath);

        // Index Logic
        IndexLogic indexLogic = new IndexLogic(statisticsLogic);

        // Select Document Index Structure Type & Compute weights and store in memory
        String oper = printDataStructureMenu();
        logger.info("Data Structure type: {}", oper);

        if (oper.equals("HashMap")){
            indexLogic.createHashMapDS(keywordsFilePath);
        } else if (oper.equals("TreeMap")){
            indexLogic.createTreeMapDS(keywordsFilePath);
        } else {
            logger.error("Data Structure type selected is invalid: {}", oper);
            System.exit(-1);
        }

        // Select SpatialIndex Tree Type
        oper = printSpatialTypeMenu();
        logger.info("Spatial-Index Tree type: {}", oper);

        if(oper.equals("IR")) {
            indexLogic.createIRtree(locationsFilePath, fanout, fillFactor, dimension);
        } else if(oper.equals("DIR")) {
            indexLogic.createDIRtree(locationsFilePath, fanout, fillFactor, dimension, maxWord, betaArea);
        } else if(oper.equals("CIR")) {
            indexLogic.createCIRtree(locationsFilePath, fanout, fillFactor, dimension, numClusters, numMoves);
        } else if(oper.equals("CDIR")) {
            indexLogic.createCDIRtree(locationsFilePath, fanout, fillFactor, dimension, maxWord, betaArea, numClusters, numMoves);
        } else {
            logger.error("Invalid Spatial-Index type selected: {}", oper);
            System.exit(-1);
        }


        // ****************************************************** //
        //                       QUERIES                          //
        // ****************************************************** //

        QueryLogic queryLogic = new QueryLogic(indexLogic, statisticsLogic, resultsDirectoryPath, true);
        queryLogic.initQueryVariables(groupSizes, groupSizeDefault, mPercentages, mPercentageDefault, numberOfKeywords, numberOfKeywordsDefault,
                querySpaceAreaPercentages, querySpaceAreaPercentageDefault, keywordSpaceSizePercentages, keywordSpaceSizePercentageDefault, topks, topkDefault,
                alphas, alphaDefault, radius, radiusDefault, numberOfQueries);

//        // TEST
//        queryLogic.processKnnQuery(new QueryLogic.KnnQueryType[]{QueryLogic.KnnQueryType.BkSK});

        do {
            // Launch Query Selector Menu
            String querySelection = printQuerySelectorMenu();
            logger.info("Selected Spatial Keyword Query type: {}", querySelection);


            // ****************************************************** //
            //                      AGGREGATE                         //
            // ****************************************************** //
            if (Objects.equals(querySelection, "AGGREGATE")) {
                // Launch Aggregator Menu
                String aggregator = printAggregateMenu();
                logger.info("Using aggregator: {}", aggregator);


                // Query generation and evaluation
                // Result set
                globalQueryResults = new ResultQueryTotal("Aggregate");
                queryLogic.setQueryResults(globalQueryResults);

                ArrayList<QueryLogic.QueryType> queryTypes = new ArrayList<>(7);


                // Launch Query Menu
                String queryType = printAggregateQueryTypeMenu();
                logger.info("Processing query type: {}", queryType);

                switch (queryType) {
                    case "GroupSize":
                        queryTypes.add(QueryLogic.QueryType.GroupSize);
                        break;
                    case "PercentQuery":
                        queryTypes.add(QueryLogic.QueryType.Percentage);
                        break;
                    case "NumKeywords":
                        queryTypes.add(QueryLogic.QueryType.NumberOfKeywords);
                        break;
                    case "SpaceAreaPercentage":
                        queryTypes.add(QueryLogic.QueryType.SpaceAreaPercentage);
                        break;
                    case "KeywordSpaceSizePercentage":
                        queryTypes.add(QueryLogic.QueryType.KeywordSpaceSizePercentage);
                        break;
                    case "TopK":
                        queryTypes.add(QueryLogic.QueryType.TopK);
                        break;
                    case "Alpha":
                        queryTypes.add(QueryLogic.QueryType.Alpha);
                        break;
                    case "All":
                        queryTypes.add(QueryLogic.QueryType.GroupSize);
                        queryTypes.add(QueryLogic.QueryType.Percentage);
                        queryTypes.add(QueryLogic.QueryType.NumberOfKeywords);
                        queryTypes.add(QueryLogic.QueryType.SpaceAreaPercentage);
                        queryTypes.add(QueryLogic.QueryType.KeywordSpaceSizePercentage);
                        queryTypes.add(QueryLogic.QueryType.TopK);
                        queryTypes.add(QueryLogic.QueryType.Alpha);
                        break;
                    default:
                        logger.info("Exiting...");
                        System.exit(0);
                }

                // Process aggregate queries
                queryLogic.processAggregateQuery(aggregateQueryTypes, queryTypes, aggregator);
            }

            // ****************************************************** //
            //                         RANGE                          //
            // ****************************************************** //

            if (Objects.equals(querySelection, "RANGE")) {
                // Query generation and evaluation
                // Result set
                globalQueryResults = new ResultQueryTotal("Range");
                queryLogic.setQueryResults(globalQueryResults);

                ArrayList<QueryLogic.QueryType> queryTypes = new ArrayList<>(7);

                // Launch Query Menu
                String queryType = printRangeQueryTypeMenu();
                logger.info("Processing query type: {}", queryType);

                switch (queryType) {
                    case "NumKeywords":
                        queryTypes.add(QueryLogic.QueryType.NumberOfKeywords);
                        break;
                    case "SpaceAreaPercentage":
                        queryTypes.add(QueryLogic.QueryType.SpaceAreaPercentage);
                        break;
                    case "KeywordSpaceSizePercentage":
                        queryTypes.add(QueryLogic.QueryType.KeywordSpaceSizePercentage);
                        break;
                    case "Range":
                        queryTypes.add(QueryLogic.QueryType.Radius);
                        break;
                    case "Alpha":
                        queryTypes.add(QueryLogic.QueryType.Alpha);
                        break;
                    case "All":
                        queryTypes.add(QueryLogic.QueryType.NumberOfKeywords);
                        queryTypes.add(QueryLogic.QueryType.SpaceAreaPercentage);
                        queryTypes.add(QueryLogic.QueryType.KeywordSpaceSizePercentage);
                        queryTypes.add(QueryLogic.QueryType.Radius);
                        queryTypes.add(QueryLogic.QueryType.Alpha);
                        break;
                    default:
                        logger.info("Exiting...");
                        System.exit(0);
                }

                // Process aggregate queries
                queryLogic.processRangeQuery(rangeQueryTypes, queryTypes);
            }

            // ****************************************************** //
            //                         KNN                            //
            // ****************************************************** //

            if (Objects.equals(querySelection, "KNN")) {
                // Query generation and evaluation
                // Result set
                globalQueryResults = new ResultQueryTotal("KNN");
                queryLogic.setQueryResults(globalQueryResults);

                ArrayList<QueryLogic.QueryType> queryTypes = new ArrayList<>(7);

                // Launch Query Menu
                String queryType = printKnnQueryTypeMenu();
                logger.info("Processing query type: {}", queryType);

                switch (queryType) {
                    case "NumKeywords":
                        queryTypes.add(QueryLogic.QueryType.NumberOfKeywords);
                        break;
                    case "SpaceAreaPercentage":
                        queryTypes.add(QueryLogic.QueryType.SpaceAreaPercentage);
                        break;
                    case "KeywordSpaceSizePercentage":
                        queryTypes.add(QueryLogic.QueryType.KeywordSpaceSizePercentage);
                        break;
                    case "TopK":
                        queryTypes.add(QueryLogic.QueryType.TopK);
                        break;
                    case "Alpha":
                        queryTypes.add(QueryLogic.QueryType.Alpha);
                        break;
                    case "All":
                        queryTypes.add(QueryLogic.QueryType.NumberOfKeywords);
                        queryTypes.add(QueryLogic.QueryType.SpaceAreaPercentage);
                        queryTypes.add(QueryLogic.QueryType.KeywordSpaceSizePercentage);
                        queryTypes.add(QueryLogic.QueryType.TopK);
                        queryTypes.add(QueryLogic.QueryType.Alpha);
                        break;
                    default:
                        logger.info("Exiting...");
                        System.exit(0);
                }

                // Process aggregate queries
                queryLogic.processKnnQuery(knnQueryTypes, queryTypes);
            }


            logger.info("---------------------------------");
            logger.info(" All queries have been evaluated ");
            logger.info("---------------------------------");

            queryLogic.printStats();

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

        } while (printExitLoopMenu());

    }


    //******************************************************************//
    //                              MENUS                               //
    //******************************************************************//

    public static String printDataStructureMenu() {
        System.out.println("\nChoose document index type:");
        System.out.println("-------------------------\n");
        System.out.println("\t1 - HashMap");
        System.out.println("\t2 - TreeMap");
        System.out.println("\t");
        System.out.println("\t0 - Quit");
        System.out.println("-------------------------\n");

        // Start Simple Menu
        Scanner input = new Scanner(System.in);
        String oper = "";

        // Aggregator selection
        int selection = input.nextInt();

        switch (selection) {
            case 1:
                oper = "HashMap";
                break;
            case 2:
                oper = "TreeMap";
                break;
            default:
                System.exit(0);
        }

        return oper;
    }

    public static String printSpatialTypeMenu() {
        System.out.println("\nChoose Spatio-Textual tree type");
        System.out.println("-------------------------\n");
        System.out.println("\t1 - IR-Tree");
        System.out.println("\t2 - DIR-Tree");
        System.out.println("\t3 - CIR-Tree");
        System.out.println("\t4 - CDIR-Tree");
        System.out.println("\t");
        System.out.println("\t0 - Quit");
        System.out.println("-------------------------\n");

        // Start Simple Menu
        Scanner input = new Scanner(System.in);
        String oper = "";

        // Tree selection
        int selection = input.nextInt();

        switch (selection) {
            case 1:
                oper = "IR";
                break;
            case 2:
                oper = "DIR";
                break;
            case 3:
                oper = "CIR";
                break;
            case 4:
                oper = "CDIR";
                break;
            default:
                System.exit(0);
        }

        return oper;
    }

    public static String printQuerySelectorMenu() {
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
        String query = "";

        // Aggregator selection
        int selection = input.nextInt();

        switch (selection) {
            case 1:
                query = "AGGREGATE";
                break;
            case 2:
                query = "RANGE";
                break;
            case 3:
                query = "KNN";
                break;
            default:
                System.exit(0);
        }

        return query;
    }

    public static String printAggregateMenu() {
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
                break;
            case 2:
                aggregator = "MAX";
                break;
            default:
                System.exit(0);
        }

        return aggregator;
    }

    public static String printAggregateQueryTypeMenu() {
        System.out.println("\nChoose query type");
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
        String oper = "";

        // Aggregator selection
        int selection = input.nextInt();

        switch (selection) {
            case 1:
                oper = "GroupSize";
                break;
            case 2:
                oper = "PercentQuery";
                break;
            case 3:
                oper = "NumKeywords";
                break;
            case 4:
                oper = "SpaceAreaPercentage";
                break;
            case 5:
                oper = "KeywordSpaceSizePercentage";
                break;
            case 6:
                oper = "TopK";
                break;
            case 7:
                oper = "Alpha";
                break;
            case 8:
                oper = "All";
                break;
            default:
                System.exit(0);
        }

        return oper;
    }

    public static String printRangeQueryTypeMenu() {
        System.out.println("\nChoose query type");
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
        String oper = "";

        // Aggregator selection
        int selection = input.nextInt();

        switch (selection) {
            case 1:
                oper = "NumKeywords";
                break;
            case 2:
                oper = "SpaceAreaPercentage";
                break;
            case 3:
                oper = "KeywordSpaceSizePercentage";
                break;
            case 4:
                oper = "Range";
                break;
            case 5:
                oper = "Alpha";
                break;
            case 6:
                oper = "All";
                break;
            default:
                System.exit(0);
        }

        return oper;
    }

    public static String printKnnQueryTypeMenu() {
        System.out.println("\nChoose query type");
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
        String oper = "";

        // Aggregator selection
        int selection = input.nextInt();

        switch (selection) {
            case 1:
                oper = "NumKeywords";
                break;
            case 2:
                oper = "SpaceAreaPercentage";
                break;
            case 3:
                oper = "KeywordSpaceSizePercentage";
                break;
            case 4:
                oper = "TopK";
                break;
            case 5:
                oper = "Alpha";
                break;
            case 6:
                oper = "All";
                break;
            default:
                System.exit(0);
        }

        return oper;
    }

    private static boolean printExitLoopMenu() {
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
