package org.ual;

import org.apache.logging.log4j.Level;
import org.ual.algorithm.kmean.KMean;
import org.ual.build.*;
import org.ual.document.*;
import org.ual.documentindex.InvertedFile;
import org.ual.io.ResultWriter;
import org.ual.query.Query;
import org.ual.querygeneration.*;
import org.ual.querytype.knn.BooleanKnnQuery;
import org.ual.querytype.knn.TopkKnnQuery;
import org.ual.querytype.aggregate.GNNKQuery;
import org.ual.querytype.aggregate.SGNNKQuery;
import org.ual.querytype.range.BRQuery;
import org.ual.spatialindex.rtreeenhanced.RTreeEnhanced;
import org.ual.spatialindex.rtree.RTree;
import org.ual.spatialindex.spatialindex.ISpatialIndex;
import org.ual.spatialindex.spatialindex.ISpatioTextualIndex;
import org.ual.spatialindex.storage.*;
import org.ual.spatialindex.storagemanager.PropertySet;
import org.ual.spatiotextualindex.dirtree.DIRTree;
import org.ual.spatiotextualindex.irtree.IRTree;
import org.ual.utils.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ual.utils.io.QueryResultWriter;
import org.ual.utils.io.StatisticsResultWriter;

/**
 *
 *
 */
public class AppMain {
    // Query Parameters
    static int[] aggregateQueryTypes = {5, 4, 3, 2, 1, 0}; // GNNK & SGNNK variants
    static int[] rangeQueryTypes = {0}; // BRQ
    static int[] knnQueryTypes = {1, 0}; // BkQ & TkQ
    static int[] joinQueryTypes = {}; // Soon...
    static int[] ns = {10, 20, 40, 60, 80}; // Group Size
    static int nDefault = 10;
    static int[] mPercentages = {40, 50, 60, 70, 80};
    static int mPercentageDefault = 60;
    static int[] numberOfKeywords = {1, 2, 4, 8, 10};
    static int numberOfKeywordsDefault = 4;
    static double[] querySpaceAreaPercentages = {.001, .01, .02, .03, .04, .05};
    static double querySpaceAreaPercentageDefault = 0.01;
    static int[] keywordSpaceSizePercentages = {1, 2, 3, 4, 5};
    static int keywordSpaceSizePercentageDefault = 3;
    static int[] topks = {1, 10, 20, 30, 40, 50};
    static int topkDefault = 10;
    static double[] alphas = {0.1, 0.3, 0.5, 0.7, 1.0};
    static double alphaDefault = 0.5;
    static float[] radius = {1f, 2f, 5f, 10f, 20f};
    static float radiusDefault = 10f;

    // Query Type Maps
//    static Map<Integer, String> aggregateTypeMap = new HashMap<>();
//    static Map<Integer, String> rangeTypeMap = new HashMap<>();
//    static Map<Integer, String> knnTypeMap = new HashMap<>();

    static ResultQueryTotal globalQueryResults;

    // Index Parameters
    static int fanout = 50; // Rtree fanout
    static double betaArea = 0.9;   // RtreeEnhanced betaArea
    static int maxWord = 10000; // RtreeEnhanced maxWord
    static int numClusters = 4; // CIRtree clusters
    static int numMoves = 4; // KMean numMoves

    // Data sets
    //static String keywordsFilePath = "src/main/resources/data/keywords.txt";    // old words.txt
    //static String locationsFilePath = "src/main/resources/data/locations.txt"; // old loc.txt

    //static String keywordsFilePath = "src/main/resources/data/key_test.txt"; // small debug set
    //static String locationsFilePath = "src/main/resources/data/loc_test.txt"; // small debug set

    //static String keywordsFilePath = "src/main/resources/data/hotel_doc";
    //static String locationsFilePath = "src/main/resources/data/hotel_loc";

    static String keywordsFilePath = "src/main/resources/data/icde19_real_doc.txt";
    static String locationsFilePath = "src/main/resources/data/icde19_real_loc.txt";


    // Results and Temp paths
    static String tempDirectoryPath = "src/main/resources/temp/";
    static String resultsDirectoryPath = "src/main/resources/results/";
    static String metricsDirectoryPath = "src/main/resources/results/metrics/";
    static String logDirectoryPath = "src/main/resources/log/";

    // NEW MEMORY VARIABLES
    static List<Weight> weights = new ArrayList<>();
    static AbstractDocumentStore weightIndex;
    static ISpatialIndex spatialIndex;
    static InvertedFile invertedFile;
    static HashMap<Integer, Integer> clusterBTree;


    // Variables
    static final Random RANDOM = new Random(1);
    static final int NUMBER_OF_QUERIES = 20;

    static List<Long> memTimes = new ArrayList<>();
    static boolean writeDebugQueryResults = false;  // Writes query results to disk for debug
    static String currentQueryParam = ""; // Hold the current parameter name in use

    private static final Logger logger = LogManager.getLogger(AppMain.class);



    public static void main( String[] args ) throws Exception {
        // Variables
        String aggregator = ""; // $3
        String oper = "";
        boolean write = false;


        // ****************************************************** //
        //                    CLEANING DATA                       //
        // ****************************************************** //

        // Create directories (log, temp and results) if not present
        CreateDirectoryTree();

        // Clear old indexes from temp directory
        ClearTempDirectory();

        // Clear results directory
        ClearResultsDirectory();

        // ****************************************************** //
        //                    PROCESSING DATA                     //
        // ****************************************************** //

        // Select Document Index Structure Type
        oper = launchDataStructureMenu();
        logger.info("Data Structure type: {}", oper);

       if (oper.equals("HashMap")){
            weightIndex = new HashMapDocumentStore();
        } else {
            weightIndex = new TreeMapDocumentStore();
        }

        // Compute weights and store in memory
        ComputeTermWeightsInMemory();

        // Select SpatialIndex Tree Type
        oper = launchSpatialTypeMenu();
        logger.info("Spatial-Index Tree type: {}", oper);

        if(oper.equals("IR")) {
            CreateRTreeInMemory(); // Build RTree index with location data
            CreateIRTreeInMemory(); // Build IRTree index with spatio-textual data
        } else if(oper.equals("DIR")) {
            CreateRTreeEnhancedInMemory(); // Build RTree index with location data
            CreateDIRTreeInMemory(); // Build IRTree index with spatio-textual data
        } else if(oper.equals("CIR")) {
            CalculateKMeanClusterInMemory(); // Calculate cluster file with Kmean
            CreateRTreeInMemory(); // Build RTree index with location data
            CreateCIRTreeInMemory(); // Build IRTree index with spatio-textual data
        } else if(oper.equals("CDIR")) {
            CalculateKMeanClusterInMemory(); // Calculate cluster file with Kmean
            CreateRTreeEnhancedInMemory(); // Build RTree index with location data
            CreateCDIRTreeInMemory(); // Build IRTree index with spatio-textual data
        } else {
            logger.error("Invalid Spatial-Index type selected: {}", oper);
            System.exit(-1);
        }


        // ****************************************************** //
        //                       QUERIES                          //
        // ****************************************************** //

        // TODO TEMP CHANGE TO DO CONSECUTIVE QUERIES
        do {
            // Launch Query Selector Menu
            String querySelection = launchQuerySelectorMenu();
            logger.info("Selected Spatial Query type: {}", querySelection);


            // ****************************************************** //
            //                      AGGREGATE                         //
            // ****************************************************** //

            if (Objects.equals(querySelection, "AGGREGATE")) {
                // Launch Aggregator Menu
                aggregator = launchAggregateMenu();
                logger.info("Using aggregator: {}", aggregator);


                // Query generation and evaluation
                // Result set
                globalQueryResults = new ResultQueryTotal("Aggregate");

                // Launch Query Menu
                String queryType = launchAggregateQueryTypeMenu();
                logger.info("Processing query type: {}", queryType);

                if (queryType.equals("GroupSize")) {
                    ProcessAggregateGroupSizeQuery(aggregator);
                } else if (queryType.equals("PercentQuery")) {
                    ProcessAggregatePercentageQuery(aggregator);
                } else if (queryType.equals("NumKeywords")) {
                    ProcessAggregateNumKeywordsQuery(aggregator);
                } else if (queryType.equals("SpaceAreaPercentage")) {
                    ProcessAggregateSpaceAreaPercentQuery(aggregator);
                } else if (queryType.equals("KeywordSpaceSizePercentage")) {
                    ProcessAggregateKeywordSpaceSizePercentQuery(aggregator);
                } else if (queryType.equals("TopK")) {
                    ProcessAggregateTopKQuery(aggregator);
                } else if (queryType.equals("Alpha")) {
                    ProcessAggregateAlphaQuery(aggregator);
                } else if (queryType.equals("All")) {
                    ProcessAggregateGroupSizeQuery(aggregator);
                    ProcessAggregatePercentageQuery(aggregator);
                    ProcessAggregateNumKeywordsQuery(aggregator);
                    ProcessAggregateSpaceAreaPercentQuery(aggregator);
                    ProcessAggregateKeywordSpaceSizePercentQuery(aggregator);
                    ProcessAggregateTopKQuery(aggregator);
                    ProcessAggregateAlphaQuery(aggregator);
                } else {
                    logger.info("Exiting...");
                    System.exit(0);
                }
            }

            // ****************************************************** //
            //                         RANGE                          //
            // ****************************************************** //

            if (Objects.equals(querySelection, "RANGE")) {
                // Query generation and evaluation
                // Result set
                globalQueryResults = new ResultQueryTotal("Range");

                // Launch Query Menu
                String queryType = launchRangeQueryTypeMenu();
                logger.info("Processing query type: {}", queryType);

                if (queryType.equals("NumKeywords")) {
                    ProcessRangeNumKeywordsQuery();
                } else if (queryType.equals("SpaceAreaPercentage")) {
                    ProcessRangeSpaceAreaPercentQuery();
                } else if (queryType.equals("KeywordSpaceSizePercentage")) {
                    ProcessRangeKeywordSpaceSizePercentQuery();
                } else if (queryType.equals("Range")) {
                    ProcessRangeRadiusQuery();
                } else if (queryType.equals("Alpha")) {
                    ProcessRangeAlphaQuery();
                } else if (queryType.equals("All")) {
                    ProcessRangeNumKeywordsQuery();
                    ProcessRangeSpaceAreaPercentQuery();
                    ProcessRangeKeywordSpaceSizePercentQuery();
                    ProcessRangeRadiusQuery();
                    ProcessRangeAlphaQuery();
                } else {
                    logger.info("Exiting...");
                    System.exit(0);
                }
            }

            // ****************************************************** //
            //                         KNN                            //
            // ****************************************************** //

            if (Objects.equals(querySelection, "KNN")) {
                // Query generation and evaluation
                // Result set
                globalQueryResults = new ResultQueryTotal("KNN");

                // Launch Query Menu
                String queryType = launchKnnQueryTypeMenu();
                logger.info("Processing query type: {}", queryType);

                if (queryType.equals("NumKeywords")) {
                    ProcessKNNNumKeywordsQuery();
                } else if (queryType.equals("SpaceAreaPercentage")) {
                    ProcessKNNSpaceAreaPercentQuery();
                } else if (queryType.equals("KeywordSpaceSizePercentage")) {
                    ProcessKNNKeywordSpaceSizePercentQuery();
                } else if (queryType.equals("TopK")) {
                    ProcessKNNTopKQuery();
                } else if (queryType.equals("Alpha")) {
                    ProcessKNNAlphaQuery();
                } else if (queryType.equals("All")) {
                    ProcessKNNNumKeywordsQuery();
                    ProcessKNNSpaceAreaPercentQuery();
                    ProcessKNNKeywordSpaceSizePercentQuery();
                    ProcessKNNTopKQuery();
                    ProcessKNNAlphaQuery();
                } else {
                    logger.info("Exiting...");
                    System.exit(0);
                }
            }


            logger.info("---------------------------------");
            logger.info(" All queries have been evaluated ");
            logger.info("---------------------------------");

            logger.info("Query Times:");
            for (long time : memTimes)
                logger.info("{} ms", time);


            // Always write data stats
            writeResults();
            logger.info("Writing results in disk ...");

            boolean writeResult = launchWriteResultMenu();
            if (writeResult) {
                logger.info("Writing results in disk ...");
                writeResults();
                //writeResults(memResults, "[MEM]");
            } else {
                logger.info("Discarding Results ...");
                break; // TODO DELETE
            }

        }while(true);

        logger.info("Exiting...");
    }



    //******************************************************************//
    //                              MENUS                               //
    //******************************************************************//

    public static String launchDataStructureMenu() {
        System.out.println("Choose document index type");
        System.out.println("-------------------------\n");
        System.out.println("\t1 - HashMap");
        System.out.println("\t2 - TreeMap");
        System.out.println("\t");
        System.out.println("\t4 - Quit");
        System.out.println("-------------------------\n");

        // Start Simple Menu
        Scanner input = new Scanner(System.in);
        String oper = "";

        // Aggregator selection
        int selection = input.nextInt();

        switch (selection) {
            case 1:
                oper = "ArrayList";
                break;
            case 2:
                oper = "HashMap";
                break;
            case 3:
                oper = "TreeMap";
                break;
            default:
                System.exit(0);
        }

        return oper;
    }

    public static String launchSpatialTypeMenu() {
        System.out.println("Choose Spatio-Textual tree type");
        System.out.println("-------------------------\n");
        System.out.println("\t1 - IR-Tree");
        System.out.println("\t2 - DIR-Tree");
        System.out.println("\t3 - CIR-Tree");
        System.out.println("\t4 - CDIR-Tree");
        System.out.println("\t");
        System.out.println("\t5 - Quit");
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

    public static String launchQuerySelectorMenu() {
        System.out.println("Choose Spatial Query type");
        System.out.println("-------------------------\n");
        System.out.println("\t1 - Aggregate");
        System.out.println("\t2 - Range");
        System.out.println("\t3 - Knn");
        System.out.println("\t");
        System.out.println("\t4 - Quit");
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

    public static String launchAggregateMenu() {
        System.out.println("Choose aggregator type");
        System.out.println("-------------------------\n");
        System.out.println("\t1 - SUM");
        System.out.println("\t2 - MAX");
        System.out.println("\t3 - Quit");
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

    public static String launchAggregateQueryTypeMenu() {
        System.out.println("Choose query type");
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
        System.out.println("\t9 - Quit");
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

    public static String launchRangeQueryTypeMenu() {
        System.out.println("Choose query type");
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

    public static String launchKnnQueryTypeMenu() {
        System.out.println("Choose query type");
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

    // TODO refactor to ask to continue running queries
    private static boolean launchWriteResultMenu() {
        System.out.println("Do another query?");
        System.out.println("-------------------------\n");
        System.out.println("\t 1  - Yes");
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
                write = false;
                break;
            default:
                write = false;
        }

        return write;
    }



    //******************************************************************//
    //                         UTILITY METHODS                          //
    //******************************************************************//

    public static void CreateDirectoryTree() {
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

    public static void ClearTempDirectory() {
        // Delete old temp files
        File file = new File(tempDirectoryPath);
        logger.info("Deleting existing temp files ...");
        deleteFilesInPath(file);
        logger.info("Done");
    }

    public static void ClearResultsDirectory() {
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


    //******************************************************************//
    //                         COMPUTE METHODS                          //
    //******************************************************************//
    //******************************************************************//
    //                            IN MEMORY                             //
    //******************************************************************//

    public static void ComputeTermWeightsInMemory() {
        // Compute new weights from keyword file
        // Compute term weights in memory
        logger.info("Computing Term Weights in Memory...");
        WeightCompute.ComputeTermWeights(keywordsFilePath, weightIndex);
        logger.info("{} Keywords computed in Memory.", weightIndex.getSize());
    }

//    public static void StoreDocumentDataInMemory() {
//        // Store text descriptions of objects
//        System.out.println("Creating index from weights...");
//        weightsIndex = StoreDocument.storeDocumentData(weights);
//        System.out.println("Done");
//    }

    public static void CalculateKMeanClusterInMemory() {
        logger.info("Creating cluster tree with Kmean medoids...");
        clusterBTree = KMean.calculateKMean(weightIndex, numClusters, numMoves);
        logger.info("Done");
    }

    public static void CreateRTreeInMemory() throws Exception {
        logger.info("Creating R-Tree with locations...");
        spatialIndex = BuildRTree.buildRTree(locationsFilePath, fanout);    //TODO: reduce fanout (50)
        logger.info("Done");
    }

    // R-tree that takes documents similarity into account
    public static void CreateRTreeEnhancedInMemory() throws Exception {
        logger.info("Creating DR-Tree...");
        // (CONG) BetaArea 0.9, (KAMAL) MaxWord  ???
        spatialIndex = BuildRTreeEnhanced.buildRTreeEnhanced(locationsFilePath, weightIndex, fanout, betaArea, maxWord);
        logger.info("Done");
    }

    public static void CreateIRTreeInMemory() {
        logger.info("Creating IR-Tree...");
        invertedFile = BuildIRTree.buildTreeIR((RTree) spatialIndex, weightIndex);
        logger.info("Done");
    }

    public static void CreateDIRTreeInMemory() {
        logger.info("Creating DIR-Tree...");
        invertedFile = BuildDIRTree.buildTreeDIR((RTreeEnhanced) spatialIndex, weightIndex);
        logger.info("Done");
    }

    public static void CreateCIRTreeInMemory() {
        logger.info("Creating CIR-Tree...");
        if(invertedFile == null)
            invertedFile = new InvertedFile();
        BuildCIRTree.buildTreeCIR((RTree) spatialIndex, weightIndex, clusterBTree, invertedFile, numClusters);
        logger.info("Done");
    }

    public static void CreateCDIRTreeInMemory() {
        logger.info("Creating CDIR-Tree...");
        if(invertedFile == null)
            invertedFile = new InvertedFile();
        BuildCDIRTree.buildTreeCDIR((RTreeEnhanced) spatialIndex, weightIndex, clusterBTree, invertedFile, numClusters);
        logger.info("Done");
    }




    //******************************************************************//
    //                         QUERY EVALUATION                         //
    //******************************************************************//
    //******************************************************************//
    //                         AGGREGATE QUERIES                        //
    //******************************************************************//

    public static void ProcessAggregateGroupSizeQuery(String aggregator) throws Exception {
        // Query evaluation
        logger.info("Evaluating based on Group Size...");
        currentQueryParam = "Group Size";

        long startTime = System.currentTimeMillis();
        for(int n : ns){
            ArrayList<ResultQueryCost> results = evaluateAggregateQueries(n, mPercentageDefault, numberOfKeywordsDefault, querySpaceAreaPercentageDefault,
                    keywordSpaceSizePercentageDefault, topkDefault, alphaDefault, aggregator);
            ResultQueryParameter param = new ResultQueryParameter("Group Size", Integer.toString(n), results);
            globalQueryResults.groupSizes.add(param);
        }
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("Group Size done in {} ms", totalTime);
        memTimes.add(totalTime);
    }

    public static void ProcessAggregatePercentageQuery(String aggregator) throws Exception {
        // Percentage
        logger.info("Evaluating based on Percentage...");
        currentQueryParam = "Percentage";

        long startTime = System.currentTimeMillis();
        for(int percentage : mPercentages) {
            ArrayList<ResultQueryCost> results = evaluateAggregateQueries(nDefault, percentage, numberOfKeywordsDefault, querySpaceAreaPercentageDefault,
                    keywordSpaceSizePercentageDefault, topkDefault, alphaDefault, aggregator);
            ResultQueryParameter param = new ResultQueryParameter("Percentage", Integer.toString(percentage), results);
            globalQueryResults.percentages.add(param);
        }
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("Percentage done in {} ms", totalTime);
        memTimes.add(totalTime);
    }

    public static void ProcessAggregateNumKeywordsQuery(String aggregator) throws Exception {
        // Number of Keywords
        logger.info("Evaluating based on Number of Keywords...");
        currentQueryParam = "Number of Keywords";

        long startTime = System.currentTimeMillis();
        for(int numberOfKeyword : numberOfKeywords){
            ArrayList<ResultQueryCost> results = evaluateAggregateQueries(nDefault, mPercentageDefault, numberOfKeyword, querySpaceAreaPercentageDefault,
                    keywordSpaceSizePercentageDefault, topkDefault, alphaDefault, aggregator);
            ResultQueryParameter param = new ResultQueryParameter("Number of Keywords", Integer.toString(numberOfKeyword), results);
            globalQueryResults.numKeywords.add(param);
        }
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("Number of Keywords done in {} ms", totalTime);
        memTimes.add(totalTime);
    }

    public static void ProcessAggregateSpaceAreaPercentQuery(String aggregator) throws Exception {
        // Query Space Area Percentage
        logger.info("Evaluating based on Query Space Area Percentage...");
        currentQueryParam = "Query Space Area Percentage";

        long startTime = System.currentTimeMillis();
        for(double querySpaceAreaPercentage : querySpaceAreaPercentages){
            ArrayList<ResultQueryCost> results = evaluateAggregateQueries(nDefault, mPercentageDefault, numberOfKeywordsDefault, querySpaceAreaPercentage,
                    keywordSpaceSizePercentageDefault, topkDefault, alphaDefault, aggregator);
            ResultQueryParameter param = new ResultQueryParameter("Query Space Area Percentage", Double.toString(querySpaceAreaPercentage), results);
            globalQueryResults.querySpaceAreas.add(param);
        }
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("Space Area Percentage done in {} ms", totalTime);
        memTimes.add(totalTime);
    }

    public static void ProcessAggregateKeywordSpaceSizePercentQuery(String aggregator) throws Exception {
        // Keyword Space Size Percentage
        logger.info("Evaluating based on Keyword Space Size Percentage...");
        currentQueryParam = "Keyword Space Size Percentage";

        long startTime = System.currentTimeMillis();
        for(double keywordSpaceSizePercentage : keywordSpaceSizePercentages){
            ArrayList<ResultQueryCost> results = evaluateAggregateQueries(nDefault, mPercentageDefault, numberOfKeywordsDefault, querySpaceAreaPercentageDefault,
                    keywordSpaceSizePercentage, topkDefault, alphaDefault, aggregator);
            ResultQueryParameter param = new ResultQueryParameter("Keyword Space Size Percentage", Double.toString(keywordSpaceSizePercentage), results);
            globalQueryResults.keyboardSpaceSizes.add(param);
        }
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("Keyword Space Size Percentage done in {} ms", totalTime);
        memTimes.add(totalTime);
    }

    public static void ProcessAggregateTopKQuery(String aggregator) throws Exception {
        // Top K
        logger.info("Evaluating based on Top K...");
        currentQueryParam = "Top K";

        long startTime = System.currentTimeMillis();
        for(int topk : topks){
            ArrayList<ResultQueryCost> results = evaluateAggregateQueries(nDefault, mPercentageDefault, numberOfKeywordsDefault, querySpaceAreaPercentageDefault,
                    keywordSpaceSizePercentageDefault, topk, alphaDefault, aggregator);
            ResultQueryParameter param = new ResultQueryParameter("Top K", Integer.toString(topk), results);
            globalQueryResults.topks.add(param);
        }
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("Top K done in {} ms", totalTime);
        memTimes.add(totalTime);
    }

    public static void ProcessAggregateAlphaQuery(String aggregator) throws Exception {
        // Alpha
        logger.info("Evaluating based on Alpha...");
        currentQueryParam = "Alphas";

        long startTime = System.currentTimeMillis();
        for(double alpha : alphas){
            ArrayList<ResultQueryCost> results = evaluateAggregateQueries(nDefault, mPercentageDefault, numberOfKeywordsDefault, querySpaceAreaPercentageDefault,
                    keywordSpaceSizePercentageDefault, topkDefault, alpha, aggregator);
            ResultQueryParameter param = new ResultQueryParameter("Alphas", Double.toString(alpha), results);
            globalQueryResults.alphas.add(param);
        }
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("Alpha done in {} ms", totalTime);
        memTimes.add(totalTime);
    }

    //******************************************************************//
    //                          RANGE QUERIES                           //
    //******************************************************************//

    public static void ProcessRangeNumKeywordsQuery() throws Exception {
        // Number of Keywords
        logger.info("Evaluating based on Number of Keywords...");
        currentQueryParam = "Number of Keywords";

        long startTime = System.currentTimeMillis();
        for(int numberOfKeyword : numberOfKeywords){
            ArrayList<ResultQueryCost> results = evaluateRangeQueries(numberOfKeyword, querySpaceAreaPercentageDefault,
                    keywordSpaceSizePercentageDefault, alphaDefault, radiusDefault);
            ResultQueryParameter param = new ResultQueryParameter("Number of Keywords", Integer.toString(numberOfKeyword), results);
            globalQueryResults.numKeywords.add(param);
        }
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("[Range] Number of Keywords done in {} ms", totalTime);
        memTimes.add(totalTime);
    }

    public static void ProcessRangeSpaceAreaPercentQuery() throws Exception {
        // Query Space Area Percentage
        logger.info("Evaluating based on Query Space Area Percentage...");
        currentQueryParam = "Query Space Area Percentage";

        long startTime = System.currentTimeMillis();
        for(double querySpaceAreaPercentage : querySpaceAreaPercentages){
            ArrayList<ResultQueryCost> results = evaluateRangeQueries(numberOfKeywordsDefault, querySpaceAreaPercentage,
                    keywordSpaceSizePercentageDefault, alphaDefault, radiusDefault);
            //result.typeName = "Query Space Area Percentage";
            //result.typeValue = Double.toString(querySpaceAreaPercentage);
            ResultQueryParameter param = new ResultQueryParameter("Query Space Area Percentage", Double.toString(querySpaceAreaPercentage), results);
            globalQueryResults.percentages.add(param);
            //memResults.querySpaceAreas.add(result);
        }
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("Space Area Percentage done in {} ms", totalTime);
        memTimes.add(totalTime);
    }

    public static void ProcessRangeKeywordSpaceSizePercentQuery() throws Exception {
        // Keyword Space Size Percentage
        logger.info("Evaluating based on Keyword Space Size Percentage...");
        currentQueryParam = "Keyword Space Size Percentage";

        long startTime = System.currentTimeMillis();
        for(double keywordSpaceSizePercentage : keywordSpaceSizePercentages){
            ArrayList<ResultQueryCost> results = evaluateRangeQueries(numberOfKeywordsDefault, querySpaceAreaPercentageDefault,
                    keywordSpaceSizePercentage, alphaDefault, radiusDefault);
            ResultQueryParameter param = new ResultQueryParameter("Keyword Space Size Percentage", Double.toString(keywordSpaceSizePercentage), results);
            globalQueryResults.keyboardSpaceSizes.add(param);
        }
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("Keyword Space Size Percentage done in {} ms", totalTime);
        memTimes.add(totalTime);
    }

    public static void ProcessRangeAlphaQuery() throws Exception {
        // Alpha
        logger.info("Evaluating based on Alpha...");
        currentQueryParam = "Alpha";

        long startTime = System.currentTimeMillis();
        for(double alpha : alphas){
            ArrayList<ResultQueryCost> results = evaluateRangeQueries(numberOfKeywordsDefault, querySpaceAreaPercentageDefault,
                    keywordSpaceSizePercentageDefault, alpha, radiusDefault);
            ResultQueryParameter param = new ResultQueryParameter("Alpha", Double.toString(alpha), results);
            globalQueryResults.alphas.add(param);
        }
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("Alpha done in {} ms", totalTime);
        memTimes.add(totalTime);
    }

    public static void ProcessRangeRadiusQuery() throws Exception {
        // Radius
        logger.info("Evaluating based on Radius...");
        currentQueryParam = "Radius";

        long startTime = System.currentTimeMillis();
        for(float radius : radius){
            ArrayList<ResultQueryCost> results = evaluateRangeQueries(numberOfKeywordsDefault, querySpaceAreaPercentageDefault,
                    keywordSpaceSizePercentageDefault, alphaDefault, radius);
            ResultQueryParameter param = new ResultQueryParameter("Radius", Float.toString(radius), results);
            globalQueryResults.radii.add(param);
        }
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("Radius done in {} ms", totalTime);
        memTimes.add(totalTime);
    }


    //******************************************************************//
    //                            KNN QUERIES                           //
    //******************************************************************//


    public static void ProcessKNNNumKeywordsQuery() throws Exception {
        // Number of Keywords
        logger.info("Evaluating based on Number of Keywords...");
        currentQueryParam = "Number of Keywords";

        long startTime = System.currentTimeMillis();
        for(int numberOfKeyword : numberOfKeywords){
            ArrayList<ResultQueryCost> results = evaluateKNNQueries(numberOfKeyword, querySpaceAreaPercentageDefault,
                    keywordSpaceSizePercentageDefault, topkDefault, alphaDefault);

            ResultQueryParameter param = new ResultQueryParameter("Number of Keywords", Integer.toString(numberOfKeyword), results);
            globalQueryResults.numKeywords.add(param);
        }
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("Number of Keywords done in {} ms", totalTime);
        memTimes.add(totalTime);
    }

    public static void ProcessKNNSpaceAreaPercentQuery() throws Exception {
        // Query Space Area Percentage
        logger.info("Evaluating based on Query Space Area Percentage...");
        currentQueryParam = "Query Space Area Percentage";

        long startTime = System.currentTimeMillis();
        for(double querySpaceAreaPercentage : querySpaceAreaPercentages){
            ArrayList<ResultQueryCost> results = evaluateKNNQueries(numberOfKeywordsDefault, querySpaceAreaPercentage,
                    keywordSpaceSizePercentageDefault, topkDefault, alphaDefault);
            ResultQueryParameter param = new ResultQueryParameter("Query Space Area Percentage", Double.toString(querySpaceAreaPercentage), results);
            globalQueryResults.querySpaceAreas.add(param);
        }
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("Space Area Percentage done in {} ms", totalTime);
        memTimes.add(totalTime);
    }

    public static void ProcessKNNKeywordSpaceSizePercentQuery() throws Exception {
        // Keyword Space Size Percentage
        logger.info("Evaluating based on Keyword Space Size Percentage...");
        currentQueryParam = "Keyword Space Size Percentage";

        long startTime = System.currentTimeMillis();
        for(double keywordSpaceSizePercentage : keywordSpaceSizePercentages){
            ArrayList<ResultQueryCost> results = evaluateKNNQueries(numberOfKeywordsDefault, querySpaceAreaPercentageDefault,
                    keywordSpaceSizePercentage, topkDefault, alphaDefault);
            ResultQueryParameter param = new ResultQueryParameter("Keyword Space Size Percentage", Double.toString(keywordSpaceSizePercentage), results);
            globalQueryResults.keyboardSpaceSizes.add(param);
        }
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("Keyword Space Size Percentage done in {} ms", totalTime);
        memTimes.add(totalTime);
    }

    public static void ProcessKNNTopKQuery() throws Exception {
        // Top K
        logger.info("Evaluating based on Top K...");
        currentQueryParam = "Top K";

        long startTime = System.currentTimeMillis();
        for(int topk : topks){
            ArrayList<ResultQueryCost> results = evaluateKNNQueries(numberOfKeywordsDefault, querySpaceAreaPercentageDefault,
                    keywordSpaceSizePercentageDefault, topk, alphaDefault);
            ResultQueryParameter param = new ResultQueryParameter("Top K", Integer.toString(topk), results);
            globalQueryResults.topks.add(param);
        }
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("Top K done in {} ms", totalTime);
        memTimes.add(totalTime);
    }

    public static void ProcessKNNAlphaQuery() throws Exception {
        // Alpha
        logger.info("Evaluating based on Alpha...");
        currentQueryParam = "Alpha";

        long startTime = System.currentTimeMillis();
        for(double alpha : alphas){
            ArrayList<ResultQueryCost> results = evaluateKNNQueries(numberOfKeywordsDefault, querySpaceAreaPercentageDefault,
                    keywordSpaceSizePercentageDefault, topkDefault, alpha);
            ResultQueryParameter param = new ResultQueryParameter("Alpha", Double.toString(alpha), results);
            globalQueryResults.alphas.add(param);
        }
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("Alpha done in {} ms", totalTime);
        memTimes.add(totalTime);
    }


    //******************************************************************//
    //                         QUERY EVALUATION                         //
    //******************************************************************//
    //******************************************************************//
    //                            AGGREGATE                             //
    //******************************************************************//

    static ArrayList<ResultQueryCost> evaluateAggregateQueries(int n, int mPercentage, int numberOfKeywords, double querySpaceAreaPercentage,
                                               double keywordSpacePercentage, int topk, double alpha, String aggregator) throws Exception {
        // TODO

        boolean isNormalize = true; // Assuming to be true

        // 1. Generate queries
        List<GNNKQuery> gnnkQueries = GNNKQueryGenerator.generateGNNKQuery(NUMBER_OF_QUERIES , n, numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage, aggregator);
        List<SGNNKQuery> sgnnkQueries = SGNNKQueryGenerator.generateSGNNKQuery(NUMBER_OF_QUERIES, n, mPercentage, numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage, aggregator);

        // 2. Run algorithms and collect result
        HashMap<Integer, Double> cpuCosts = new HashMap<>();
        HashMap<Integer, Double> ioCosts = new HashMap<>();
        ArrayList<ResultQueryCost> resultData = new ArrayList<>();

        // Print Query Types
        logger.info("Query types:\n" +
                "\t * 0 - gnnk\n" +
                "\t * 1 - gnnk baseline\n" +
                "\t * 2 - sgnnk\n" +
                "\t * 3 - sgnnk baseline\n" +
                "\t * 4 - sgnnk extended\n" +
                "\t * 5 - sgnnk * (n - m + 1)");

        //System.err.println("MEM PROCESSING");

        for(int queryType: aggregateQueryTypes) {
            logger.info("Aggregate Query Type: {}", queryType);
            //queryProcessingResult.querySubtype.putIfAbsent(queryType, new QueryEvaluationResult("tmp"));

            ResultQueryCost result = calculateAggregateQueriesCost(gnnkQueries, sgnnkQueries, topk, alpha, queryType, isNormalize);

//            cpuCosts.put(queryType, result[0]);
//            ioCosts.put(queryType, result[1]);
            resultData.add(result);
        }

        // TODO CHECK MISSING RESULTS
//        String gnnkcpu = cpuCosts.get(0) + " " + cpuCosts.get(1);
//        String gnnkio = ioCosts.get(0) + " " + ioCosts.get(1);
//        String sgnnkcpu = cpuCosts.get(2) + " " + cpuCosts.get(3);
//        String sgnnkio = ioCosts.get(2) + " " + ioCosts.get(3);
//        String sgnnkecpu = cpuCosts.get(4) + " " + cpuCosts.get(5);
//        String sgnnkeio = ioCosts.get(4) + " " + ioCosts.get(5);
//
//        ResultData resultData = new ResultData();
//        resultData.cpuResults.put("[GNNK-CPU]", gnnkcpu);
//        resultData.ioResults.put("[GNNK-IO]", gnnkio);
//        resultData.cpuResults.put("[SGNNK-CPU]", sgnnkcpu);
//        resultData.ioResults.put("[SGNNK-IO]", sgnnkio);
//        resultData.cpuResults.put("[SGNNK EXT-CPU]", sgnnkecpu);
//        resultData.ioResults.put("[SGNNK EXT-IO]", sgnnkeio);

        return resultData;
    }


    // InvertedIndex, queries...
    private static ResultQueryCost calculateAggregateQueriesCost(List<GNNKQuery> gnnkQueries, List<SGNNKQuery> sgnnkQueries, int topk,
                                                                 double alphaDistribution, int queryType, boolean isNormalize) throws Exception {

        PropertySet propertySet2 = new PropertySet();
        int indexIdentifier = 1; // (in this case I know that it is equal to 1)
        propertySet2.setProperty("IndexIdentifier", indexIdentifier);

        boolean printInConsole = false; // FIX GLOBAL VARIABLE
        long startTime = System.currentTimeMillis();
        QueryResultWriter resultWriter = new QueryResultWriter();

        // Choose between Rtree and DRtree
        ISpatioTextualIndex tree;
        if(spatialIndex instanceof RTree) {
            RTree.alphaDistribution = alphaDistribution;
            tree = new IRTree((RTree) spatialIndex);
        } else {
            RTreeEnhanced.alphaDistribution = alphaDistribution;
            tree = new DIRTree((RTreeEnhanced) spatialIndex);
        }

        // TODO (Optional) Set Results file prefix base on query type
        String prefix = "";
        switch (queryType){
            case 0:
                prefix = "[GNNK]";
                break;
            case 1:
                prefix = "[GNNK-BL]";
                break;
            case 2:
                prefix = "[SGNNK]";
                break;
            case 3:
                prefix = "[SGNNK-BL]";
                break;
            case 4:
                prefix = "[SGNNK-EXT]";
                break;
            case 5:
                prefix = "[SGNNK-NM1]";
                break;
            default:
                break;
        }


        int numberOfQueries;
        double totalCost = 0; // It doesn't keep track of MSGNNK cost - too much extra work
        double spatialCost = 0;
        double irCost = 0;

        //double[] resultCost = new double[2];
        ResultQueryCost resultCost = new ResultQueryCost();


        // [0] GNNK and [1] GNNK Baseline
        if (queryType == 0 || queryType == 1) {
            numberOfQueries = gnnkQueries.size();
            resultCost.queryName = prefix;  // TODO FIX
            //writer = new ResultWriter(gnnkQueries.size(), resultsDirectoryPath, printInConsole, prefix);

            for (GNNKQuery q : gnnkQueries) {
                List<GNNKQuery.Result> results;
                if (queryType == 1) {
                    //logger.debug("GNNK Baseline: grpSz {}", q.groupSize);
                    results = tree.gnnkBaseline(invertedFile, q, topk);
                } else {
                    //logger.debug("GNNK: grpSz {}", q.groupSize);
                    results = tree.gnnk(invertedFile, q, topk);
                }

                if(writeDebugQueryResults) {
                    resultWriter.writeGNNKResult(results);
                    resultWriter.write("========================================", true);
                    //writer.writeGNNKResult(results);
                    //writer.write("========================================");
                }

                totalCost += results.get(0).cost.totalCost;
                spatialCost += results.get(0).cost.spatialCost;
                irCost += results.get(0).cost.irCost;
            }
        } else {
            numberOfQueries = sgnnkQueries.size();
            resultCost.queryName = prefix;  // TODO FIX
            //writer = new ResultWriter(sgnnkQueries.size(), resultsDirectoryPath, printInConsole, prefix);

            for (SGNNKQuery q : sgnnkQueries) {
                // [4] SGNNK Extended
                if (queryType == 4) {
                    //logger.debug("SGNNK Extended: grpSz {} -  SubGrpSz {}", q.groupSize, q.subGroupSize);
                    Map<Integer, List<SGNNKQuery.Result>> results = tree.sgnnkExtended(invertedFile, q, topk);
                    List<Integer> subroupSizes = new ArrayList<>(results.keySet());
                    Collections.sort(subroupSizes);

                    if(writeDebugQueryResults) {
                        for (Integer subgroupSize : subroupSizes) {
                            resultWriter.write("Size " + subgroupSize, true);
                            resultWriter.writeSGNNKResult(results.get(subgroupSize));
                            //writer.write("Size " + subgroupSize);
                            //writer.writeSGNNKResult(results.get(subgroupSize));
                        }
                    }
                }
                // [5] SGNNK * (n - m + 1)
                else if (queryType == 5) {
                    //logger.debug("SGNNK Extended Baseline: grpSz {} -  SubGrpSz {}", q.groupSize, q.subGroupSize);
                    int holdSubGroupSize = q.subGroupSize;  // Fix for index out of bounds
                    while (q.subGroupSize <= q.groupSize) {
                        if(writeDebugQueryResults)
                            resultWriter.write("Size " + q.subGroupSize, true);
                            //writer.write("Size " + q.subGroupSize);
                        List<SGNNKQuery.Result> results = tree.sgnnk(invertedFile, q, topk);
                        if(writeDebugQueryResults)
                            resultWriter.writeSGNNKResult(results);
                            //writer.writeSGNNKResult(results);

                        totalCost += results.get(0).cost.totalCost;
                        spatialCost += results.get(0).cost.spatialCost;
                        irCost += results.get(0).cost.irCost;

                        q.subGroupSize++; // This will cause an index out of bound in type 3 query if not reset
                    }
                    q.subGroupSize = holdSubGroupSize; // Restore the original value

                }
                // [2] SGNNK & [3] SGNNK Baseline
                else {
                    List<SGNNKQuery.Result> results;

                    if (queryType == 3) {
                        //logger.debug("SGNNK Baseline: grpSz {} -  SubGrpSz {}", q.groupSize, q.subGroupSize);
                        results = tree.sgnnkBaseline(invertedFile, q, topk);
                    } else {    // QueryType 2
                        //logger.debug("SGNNK: grpSz {} -  SubGrpSz {}", q.groupSize, q.subGroupSize);
                        results = tree.sgnnk(invertedFile, q, topk);
                    }

                    totalCost += results.get(0).cost.totalCost;
                    spatialCost += results.get(0).cost.spatialCost;
                    irCost += results.get(0).cost.irCost;

                    if(writeDebugQueryResults)
                        resultWriter.writeSGNNKResult(results);
                        //writer.writeSGNNKResult(results);
                }
                if(writeDebugQueryResults)
                    resultWriter.write("========================================", true);
                    //writer.write("========================================");
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;

        int averageTime = (int) (totalTime / numberOfQueries);
        int averageFileIO = (tree.getIO() + invertedFile.getIO()) / numberOfQueries;
        double averageNodesVisited = tree.getVisitedNodes() * 1.0 / numberOfQueries; //numOfVisitedNodes
        double averageSpatialCost = spatialCost / numberOfQueries;
        double averageIRCost = irCost / numberOfQueries;    // TODO CHECK THIS

        if (isNormalize)
            averageSpatialCost /= 10;

        if(writeDebugQueryResults) {
            resultWriter.write(prefix + " Average nodes visited: " + averageNodesVisited, true);
            resultWriter.write(prefix + " Total time millisecond: " + totalTime, true);
            resultWriter.writeToDisk(resultsDirectoryPath, prefix + currentQueryParam);
            //writer.write(prefix + " Average nodes visited: " + averageNodesVisited);
            //writer.write(prefix + " Total time millisecond: " + totalTime);
        }
        //writer.close();


        logger.debug("Average time millisecond: {}", averageTime);
        logger.debug("Average total IO: {}", averageFileIO);
//			System.out.println("Average tree IO: " + tree.getIO() * 1.0 / count);
//			System.out.println("Average inverted index IO: " + ivIO * 1.0 / count);
        logger.printf(Level.INFO,"TotalTime= %dms avgT= %dms avgIO= %d avgSpatCost= %.6f avgIRCost= %.6f", totalTime, averageTime, averageFileIO, averageSpatialCost, averageIRCost);

        resultCost.totalTime = totalTime;
        resultCost.averageTime = averageTime;
        resultCost.averageNodesVisited = averageNodesVisited;
        resultCost.averageSpatialCost = averageSpatialCost;
        resultCost.averageIRCost = averageIRCost;

//        resultCost[0] = spatialCost;
//        resultCost[1] = totalCost;

        return resultCost;
    }


    //******************************************************************//
    //                             RANGE                                //
    //******************************************************************//

    static ArrayList<ResultQueryCost> evaluateRangeQueries(int numberOfKeywords, double querySpaceAreaPercentage,
                                               double keywordSpacePercentage, double alpha, float radius) throws Exception {

        boolean isNormalize = true; // Assuming to be true

        // 1. Generate queries
        List<BRQuery> brQueries = BRQueryGenerator.generateBRQueries(NUMBER_OF_QUERIES , numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage);
        logger.debug("Boolean QueryList: {}", brQueries);

        // 2. Run algorithms and collect result
        HashMap<Integer, Double> cpuCosts = new HashMap<>();
        HashMap<Integer, Double> ioCosts = new HashMap<>();
        ArrayList<ResultQueryCost> resultData = new ArrayList<>();

        // Print Query Types
        logger.info("Range Query types:\n" +
                "\t * 0 - Boolean Range");

        for(int queryType: rangeQueryTypes) {
            logger.info("Range Query Type: {}", queryType);

            ResultQueryCost result = calculateRangeQueriesCost(brQueries, alpha, radius, queryType, isNormalize);
//            cpuCosts.put(queryType, result[0]);
//            ioCosts.put(queryType, result[1]);
            resultData.add(result);
        }

        return resultData;
    }

    // InvertedIndex, queries...
    private static ResultQueryCost calculateRangeQueriesCost(List<BRQuery> brQueries, double alphaDistribution,
                                                      float radius, int queryType, boolean isNormalize) throws Exception {

        PropertySet propertySet2 = new PropertySet();
        int indexIdentifier = 1; // (in this case I know that it is equal to 1)
        propertySet2.setProperty("IndexIdentifier", indexIdentifier);

        boolean printInConsole = false; // TODO FIX GLOBAL VARIABLE
        long startTime = System.currentTimeMillis();
        //ResultWriter writer;
        QueryResultWriter resultWriter = new QueryResultWriter();

        // Choose between Rtree and DRtree
        ISpatioTextualIndex tree;
        if(spatialIndex instanceof RTree) {
            RTree.alphaDistribution = alphaDistribution;
            tree = new IRTree((RTree) spatialIndex);
        } else {
            RTreeEnhanced.alphaDistribution = alphaDistribution;
            tree = new DIRTree((RTreeEnhanced) spatialIndex);
        }

        int numberOfQueries = 0;
        double totalCost = 0; // It doesn't keep track of MSGNNK cost - too much extra work
        double spatialCost = 0;
        double irCost = 0;

        //double[] resultCost = new double[2];
        ResultQueryCost resultCost = new ResultQueryCost();

        String prefix = ""; // Filename prefix

        // Right now there is only one range query type so this is unnecessary
        if (queryType == 0) {
            numberOfQueries = brQueries.size();
            prefix = "[BRQ]";
            resultCost.queryName = "BRQ";
            //writer = new ResultWriter(brQueries.size(), resultsDirectoryPath, printInConsole, prefix);

            for (BRQuery q : brQueries) {
                List<BRQuery.Result> results;
                results = tree.booleanRangeQuery(invertedFile, q, radius);

                if(writeDebugQueryResults) {
                    resultWriter.writeBRQResult(results);
                    resultWriter.write("========================================", true);
                    //writer.writeBRQResult(results);
                    //writer.write("========================================");
                }
            }
        } else {
            // New query types will be here
            resultCost.queryName = "???";
            //writer = new ResultWriter(brQueries.size(), resultsDirectoryPath, printInConsole, "[???]");
        }

        long totalTime = System.currentTimeMillis() - startTime;

        int averageTime = (int) (totalTime / numberOfQueries);
        int averageFileIO = (tree.getIO() + invertedFile.getIO()) / numberOfQueries;
        double averageNodesVisited = tree.getVisitedNodes() * 1.0 / numberOfQueries; //numOfVisitedNodes
        double averageSpatialCost = spatialCost / numberOfQueries;
        double averageIRCost = irCost / numberOfQueries;    // TODO CHECK THIS

        if (isNormalize)
            averageSpatialCost /= 10;

        if(writeDebugQueryResults) {
            resultWriter.write(prefix + " Average nodes visited: " + averageNodesVisited, true);
            resultWriter.write(prefix + " Total time millisecond: " + totalTime, true);
            resultWriter.writeToDisk(resultsDirectoryPath, prefix + currentQueryParam);
            //writer.write(prefix + " Average nodes visited: " + averageNodesVisited);
            //writer.write(prefix + " Total time millisecond: " + totalTime);
        }
        //writer.close();

        logger.debug(prefix + " Average time millisecond: {}", averageTime);
        logger.debug(prefix + " Average total IO: {}", averageFileIO);
//			System.out.println("Average tree IO: " + tree.getIO() * 1.0 / count);
//			System.out.println("Average inverted index IO: " + ivIO * 1.0 / count);
        logger.printf(Level.INFO,prefix + "TotalTime= %dms avgT= %dms avgIO= %d avgSpatCost= %.6f avgIRCost= %.6f", totalTime, averageTime, averageFileIO, averageSpatialCost, averageIRCost);

        resultCost.totalTime = totalTime;
        resultCost.averageTime = averageTime;
        resultCost.averageNodesVisited = averageNodesVisited;
        resultCost.averageSpatialCost = averageSpatialCost;
        resultCost.averageIRCost = averageIRCost;

        return resultCost;
    }


    //******************************************************************//
    //                             KNN                                  //
    //******************************************************************//

    static ArrayList<ResultQueryCost> evaluateKNNQueries(int numberOfKeywords, double querySpaceAreaPercentage,
                                           double keywordSpacePercentage, int topk, double alpha) throws Exception {

        boolean isNormalize = true; // Assuming to be true

        // 1. Generate queries
        List<BooleanKnnQuery> bkQueries = BKQueryGenerator.generateBKQueries(NUMBER_OF_QUERIES , numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage);
        List<TopkKnnQuery> topkQueries = TopKQueryGenerator.generateTKQueries(NUMBER_OF_QUERIES , numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage);

        logger.debug("Boolean QueryList: {}", bkQueries);
        logger.debug("TopK QueryList: {}", topkQueries);

        // 2. Run algorithms and collect result
        HashMap<Integer, Double> cpuCosts = new HashMap<>();
        HashMap<Integer, Double> ioCosts = new HashMap<>();
        ArrayList<ResultQueryCost> resultData = new ArrayList<>();


        // Print Query Types
        logger.info("Query types:\n" +
                "\t * 0 - Boolean Knn\n" +
                "\t * 1 - Top K");

        for(int queryType: knnQueryTypes) {
            logger.info("Range Query Type: {}", queryType);

            ResultQueryCost result = calculateKNNQueriesCost(bkQueries, topkQueries, alpha, topk, queryType, isNormalize);
            resultData.add(result);
        }

        return resultData;
    }


    // InvertedIndex, queries...
    private static ResultQueryCost calculateKNNQueriesCost(List<BooleanKnnQuery> bkQueries, List<TopkKnnQuery> topkQueries, double alphaDistribution,
                                                           int topk, int queryType, boolean isNormalize) throws Exception {

        PropertySet propertySet2 = new PropertySet();
        int indexIdentifier = 1; // (in this case I know that it is equal to 1)
        propertySet2.setProperty("IndexIdentifier", indexIdentifier);

        boolean printInConsole = false; // TODO FIX GLOBAL VARIABLE
        long startTime = System.currentTimeMillis();
        //ResultWriter writer;
        QueryResultWriter resultWriter = new QueryResultWriter();

        // Choose between Rtree and DRtree
        ISpatioTextualIndex tree;
        if(spatialIndex instanceof RTree) {
            RTree.alphaDistribution = alphaDistribution;
            tree = new IRTree((RTree) spatialIndex);
        } else {
            RTreeEnhanced.alphaDistribution = alphaDistribution;
            tree = new DIRTree((RTreeEnhanced) spatialIndex);
        }

        int numberOfQueries = 0;
        double totalCost = 0; // It doesn't keep track of MSGNNK cost - too much extra work
        double spatialCost = 0;
        double irCost = 0;

        //double[] resultCost = new double[2];
        ResultQueryCost resultCost = new ResultQueryCost();

        String prefix = "";

        // (0) BKQ
        if (queryType == 0) {
            numberOfQueries = bkQueries.size();
            prefix = "[BKQ]";
            resultCost.queryName = "BKQ";
            //writer = new ResultWriter(bkQueries.size(), resultsDirectoryPath, printInConsole, prefix);

            for (BooleanKnnQuery q : bkQueries) {
                List<BooleanKnnQuery.Result> results = new ArrayList<>();

                results = tree.booleanKnnQuery(invertedFile, q, topk);

                if(writeDebugQueryResults) {
                    resultWriter.writeBKQResult(results);
                    resultWriter.write("========================================", true);
                    //writer.writeBKQResult(results);
                    //writer.write("========================================");
                }
            }
        // (1) TopK
        } else {
            numberOfQueries = topkQueries.size();
            prefix = "[TKQ]";
            resultCost.queryName = "TKQ";
            //writer = new ResultWriter(topkQueries.size(), resultsDirectoryPath, printInConsole, prefix);

            for (TopkKnnQuery q : topkQueries) {
                List<TopkKnnQuery.Result> results;
                results = tree.topkKnnQuery(invertedFile, q, topk);

                if(writeDebugQueryResults) {
                    resultWriter.writeTKQResult(results);
                    resultWriter.write("========================================", true);
                    //writer.writeTKQResult(results);
                    //writer.write("========================================");
                }
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;

        int averageTime = (int) (totalTime / numberOfQueries);
        int averageFileIO = (tree.getIO() + invertedFile.getIO()) / numberOfQueries;
        double averageNodesVisited = tree.getVisitedNodes() * 1.0 / numberOfQueries; //numOfVisitedNodes
        double averageSpatialCost = spatialCost / numberOfQueries;
        double averageIRCost = irCost / numberOfQueries;    // TODO CHECK THIS

        if (isNormalize)
            averageSpatialCost /= 10;

        if(writeDebugQueryResults) {
            resultWriter.write(prefix + " Average nodes visited: " + averageNodesVisited, true);
            resultWriter.write(prefix + " Total time millisecond: " + totalTime, true);
            resultWriter.writeToDisk(resultsDirectoryPath, prefix + currentQueryParam);
            //writer.write(prefix + " Average nodes visited: " + averageNodesVisited);
            //writer.write(prefix + " Total time millisecond: " + totalTime);
        }
        //writer.close();

        logger.debug(prefix + " Average time millisecond: {}", averageTime);
        logger.debug(prefix + " Average total IO: {}", averageFileIO);
//			System.out.println("Average tree IO: " + tree.getIO() * 1.0 / count);
//			System.out.println("Average inverted index IO: " + ivIO * 1.0 / count);
        logger.printf(Level.INFO,prefix + "TotalTime= %dms avgT= %dms avgIO= %d avgSpatCost= %.6f avgIRCost= %.6f",totalTime, averageTime, averageFileIO, averageSpatialCost, averageIRCost);


        resultCost.totalTime = totalTime;
        resultCost.averageTime = averageTime;
        resultCost.averageNodesVisited = averageNodesVisited;
        resultCost.averageSpatialCost = averageSpatialCost;
        resultCost.averageIRCost = averageIRCost;

        return resultCost;
    }


    //******************************************************************//
    //                         RESULT WRITER                            //
    //******************************************************************//

    public static void writeResults() {
        logger.info("Writing Results...");

        StatisticsResultWriter.writeCSV(globalQueryResults.groupSizes, "group-size", globalQueryResults.queryType, metricsDirectoryPath);
        StatisticsResultWriter.writeCSV(globalQueryResults.percentages, "subgroup-size", globalQueryResults.queryType, metricsDirectoryPath);
        StatisticsResultWriter.writeCSV(globalQueryResults.numKeywords, "number-of-keyword", globalQueryResults.queryType, metricsDirectoryPath);
        StatisticsResultWriter.writeCSV(globalQueryResults.querySpaceAreas, "query-space-area", globalQueryResults.queryType, metricsDirectoryPath);
        StatisticsResultWriter.writeCSV(globalQueryResults.keyboardSpaceSizes, "keyword-space-size", globalQueryResults.queryType, metricsDirectoryPath);
        StatisticsResultWriter.writeCSV(globalQueryResults.topks, "topk", globalQueryResults.queryType, metricsDirectoryPath);
        StatisticsResultWriter.writeCSV(globalQueryResults.radii, "range", globalQueryResults.queryType, metricsDirectoryPath);
        StatisticsResultWriter.writeCSV(globalQueryResults.alphas, "alpha", globalQueryResults.queryType, metricsDirectoryPath);

        //TODO Call new method StatisticsResultWriter.resultWriter
        resultWriter(globalQueryResults.groupSizes, "group-size");
        resultWriter(globalQueryResults.percentages,"subgroup-size");
        resultWriter(globalQueryResults.numKeywords, "number-of-keyword");
        resultWriter(globalQueryResults.querySpaceAreas, "query-space-area");
        resultWriter(globalQueryResults.keyboardSpaceSizes, "keyword-space-size");
        resultWriter(globalQueryResults.topks, "topk");
        resultWriter(globalQueryResults.radii, "range");
        resultWriter(globalQueryResults.alphas, "alpha");

        logger.info("Done");

    }

    private static void resultWriter(List<ResultQueryParameter> resultsByParam, String paramName) {
        if(resultsByParam.isEmpty()) {
            logger.info("Skipping param: {} because is empty", paramName);
            return;
        }

        logger.info("Writing data for results in {}", globalQueryResults.queryType);
        String fileName = "[" + globalQueryResults.queryType + "]" + paramName; // [Aggregate] alpha

        // CPU
        try (FileWriter fw = new FileWriter(metricsDirectoryPath + fileName + "-CPU.dat", true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw)) {
            for(ResultQueryParameter resultData : resultsByParam) {
                out.println("Parameter: " + resultData.paramName + " - Value: " + resultData.paramValue);
                out.println("");
                //out.println("Type: " + resultData.typeName + " Value: " + resultData.typeValue);
                for(ResultQueryCost resultCost : resultData.results) {
                    out.printf("[%s] totalTime= %dms | avgTime= %dms | avgNodesVisited= %f | avgSpatCost= %.6f | avgIRCost= %.6f \n",
                            resultCost.queryName, resultCost.totalTime, resultCost.averageTime, resultCost.averageNodesVisited, resultCost.averageSpatialCost,
                            resultCost.averageIRCost);
                }
                out.println("");
                out.println("==================================================");
            }
        } catch (IOException e) {
            logger.error("Fail to write results", e);
        }
    }

    //******************************************************************//
    //                         DEBUG METHODS                            //
    //******************************************************************//

    static void printWeights() {
        int id = 0;
        logger.debug("Printing term weights:");
        //for(LinkedHashMap<String, Double> row : weights) {
        for(Weight weight : weights) {
            logger.debug("ID({})", weight.wordId);
            for(WeightEntry entry : weight.weights) {
                logger.debug("Wd({}), Wt({})", entry.word, entry.weight);
            }
            id++;
            logger.debug("");
        }
    }

    static void writeGNNKQuery(List<GNNKQuery> queryList, String outputDirectory) {
        try (FileWriter fw = new FileWriter(outputDirectory + "gnnk-query.txt", false);
             BufferedWriter bw = new BufferedWriter(fw); PrintWriter out = new PrintWriter(bw)) {
            out.println(NUMBER_OF_QUERIES);
            for (int i = 0; i < queryList.size(); i++) {
                GNNKQuery q = queryList.get(i);
                out.println(q.aggregator.getName());
                out.println(q.groupSize);
                for(int j =0; j <q.queries.size(); j++){
                    Query query = q.queries.get(j);
                    String str = query.id + "," + query.weight + "," + query.location.coords[0] + "," + query.location.coords[1] + ",";
                    for (int k = 0; k < query.keywords.size(); k++) {
                        if (k != query.keywords.size() - 1)
                            str = str.concat(query.keywords.get(k) + " " + query.keywordWeights.get(k) + ",");
                            //out.print(query.keywords.get(k) + " " + query.keywordWeights.get(k) + ",");
                        else
                            str = str.concat(query.keywords.get(k) + " " + query.keywordWeights.get(k));
                            //out.print(query.keywords.get(k) + " " + query.keywordWeights.get(k) + "\n");
                    }
                    out.println(str);
                }

            }
        } catch (IOException e) {
            logger.error("Fail to write GNNK Queries", e);
        }
    }

    static void writeSGNNKQuery(List<SGNNKQuery> queryList, String outputDirectory) {
        try (FileWriter fw = new FileWriter(outputDirectory + "sgnnk-query.txt", false);
             BufferedWriter bw = new BufferedWriter(fw); PrintWriter out = new PrintWriter(bw)) {
            out.println(NUMBER_OF_QUERIES);
            for (int i = 0; i < queryList.size(); i++) {
                SGNNKQuery q = queryList.get(i);
                out.println(q.aggregator.getName());
                out.println(q.groupSize);
                out.println(q.subGroupSize);
                for(int j =0; j <q.queries.size(); j++){
                    Query query = q.queries.get(j);
                    String str = query.id + "," + query.weight + "," + query.location.coords[0] + "," + query.location.coords[1] + ",";
                    for (int k = 0; k < query.keywords.size(); k++) {
                        if (k != query.keywords.size() - 1)
                            str = str.concat(query.keywords.get(k) + " " + query.keywordWeights.get(k) + ",");
                            //out.print(query.keywords.get(k) + " " + query.keywordWeights.get(k) + ",");
                        else
                            str = str.concat(query.keywords.get(k) + " " + query.keywordWeights.get(k));
                        //out.print(query.keywords.get(k) + " " + query.keywordWeights.get(k) + "\n");
                    }
                    out.println(str);
                }

            }
        } catch (IOException e) {
            logger.error("Fail to write SGNNK Queries", e);
        }
    }
}
