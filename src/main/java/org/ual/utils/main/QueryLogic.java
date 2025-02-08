package org.ual.utils.main;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ual.algorithm.aggregator.IAggregator;
import org.ual.querygeneration.*;
import org.ual.querytype.AggregateSKNNQuery;
import org.ual.querytype.SKNNQuery;
//import org.ual.querytype.knn.TopkKnnQuery;
import org.ual.spatialindex.parameters.DatasetParameters;
import org.ual.spatialindex.rtree.RTree;
import org.ual.spatialindex.rtreeenhanced.RTreeEnhanced;
import org.ual.spatialindex.spatialindex.ISpatioTextualIndex;
import org.ual.spatiotextualindex.dirtree.DIRTree;
import org.ual.spatiotextualindex.irtree.IRTree;
import org.ual.utils.ResultQueryTotal;
import org.ual.utils.io.QueryResultWriter;
import org.ual.utils.stats.QueryStats;
import org.ual.utils.stats.QueryStatsData;

import java.util.*;

public class QueryLogic {


    int[] groupSizes;; // Group Size
    int groupSizeDefault;
    int[] mPercentages;
    int mPercentageDefault;
    int[] numberOfKeywords;
    int numberOfKeywordsDefault;
    double[] querySpaceAreaPercentages;
    double querySpaceAreaPercentageDefault;
    int[] keywordSpaceSizePercentages;
    int keywordSpaceSizePercentageDefault;
    int[] topks;
    //int[] topks = {1, 10, 100, 200, 400, 600, 800, 1000};
    int topkDefault;
    double[] alphas;
    double alphaDefault;
    float[] radius;
    //float[] radius = {1f, 2f, 4f, 6f, 8f, 10f, 12f, 14f, 16f, 18f, 20f, 40f, 60f, 80f, 100f, 120f};
    float radiusDefault;

    int numberOfQueries;
    int ramdomSeed = 1; // Default seed
    boolean writeQueriesToDisk;
    String resultsDirectoryPath;
    DatasetParameters parameters;

    IndexLogic indexLogic;
    StatisticsLogic statisticsLogic;

    private static final Logger logger = LogManager.getLogger(QueryLogic.class);


    public QueryLogic(IndexLogic indexLogic, StatisticsLogic statisticsLogic, String resultsDirectoryPath, DatasetParameters parameters, boolean writeQueriesToDisk) {
        this.indexLogic = indexLogic;
        this.writeQueriesToDisk = writeQueriesToDisk;
        this.resultsDirectoryPath = resultsDirectoryPath;
        this.statisticsLogic = statisticsLogic;
        this.parameters = parameters;
    }

    public QueryLogic(IndexLogic indexLogic, StatisticsLogic statisticsLogic, String resultsDirectoryPath, DatasetParameters parameters, boolean writeQueriesToDisk, int[] groupSizes,
                      int groupSizeDefault, int[] mPercentages, int mPercentageDefault, int[] numberOfKeywords,
                      int numberOfKeywordsDefault, double[] querySpaceAreaPercentages, double querySpaceAreaPercentageDefault,
                      int[] keywordSpaceSizePercentages, int keywordSpaceSizePercentageDefault, int[] topks, int topkDefault,
                      double[] alphas, double alphaDefault, float[] radius, float radiusDefault, int numberOfQueries) {

        this.indexLogic = indexLogic;
        this.statisticsLogic = statisticsLogic;
        this.resultsDirectoryPath = resultsDirectoryPath;
        this.parameters = parameters;
        this.writeQueriesToDisk = writeQueriesToDisk;
        this.groupSizes = groupSizes;
        this.groupSizeDefault = groupSizeDefault;
        this.mPercentages = mPercentages;
        this.mPercentageDefault = mPercentageDefault;
        this.numberOfKeywords = numberOfKeywords;
        this.numberOfKeywordsDefault = numberOfKeywordsDefault;
        this.querySpaceAreaPercentages = querySpaceAreaPercentages;
        this.querySpaceAreaPercentageDefault = querySpaceAreaPercentageDefault;
        this.keywordSpaceSizePercentages = keywordSpaceSizePercentages;
        this.keywordSpaceSizePercentageDefault = keywordSpaceSizePercentageDefault;
        this.topks = topks;
        this.topkDefault = topkDefault;
        this.alphas = alphas;
        this.alphaDefault = alphaDefault;
        this.radius = radius;
        this.radiusDefault = radiusDefault;
        this.numberOfQueries = numberOfQueries;
    }

    public void printStats() {
        statisticsLogic.writeResults();
    }

    // Generate query
//    public List<GNNKQuery> generateGNNKQueries(int numberOfQueries, int groupSize, int numberOfKeywords, double querySpaceAreaPercentage, double keywordSpacePercentage, String aggregator) {
//        return GNNKQueryGenerator.generateGNNKQuery(numberOfQueries , groupSize, numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage, aggregator);
//    }
//
//    public List<SGNNKQuery> generateSGNNKQueries(int numberOfQueries, int groupSize, double subGroupSize, int numberOfKeywords, double querySpaceAreaPercentage, double keywordSpacePercentage, String aggregator) {
//        return SGNNKQueryGenerator.generateSGNNKQuery(numberOfQueries , groupSize, subGroupSize, numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage, aggregator);
//    }
//
//    public List<BooleanKnnQuery> generateBKSKQueries(int numberOfQueries, int numberOfKeywords, double querySpaceAreaPercentage, double keywordSpacePercentage) {
//        return BKQueryGenerator.generateBKQueries(numberOfQueries, numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage);
//    }
//
//    public List<TopkKnnQuery> generateTKSKQueries(int numberOfQueries, int numberOfKeywords, double querySpaceAreaPercentage, double keywordSpacePercentage) {
//        return TopKQueryGenerator.generateTKQueries(numberOfQueries, numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage);
//    }
//
//    // Process single query with parameters
//    public List<BRQuery> generateQBRSKQeuries(int numberOfQueries, int numberOfKeywords, double querySpaceAreaPercentage, double keywordSpacePercentage) {
//        return BRQueryGenerator.generateBRQueries(numberOfQueries, numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage);
//    }


    // Run queries
    public void initQueryVariables(int[] groupSizes, int groupSizeDefault, int[] mPercentages, int mPercentageDefault, int[] numberOfKeywords,
                                   int numberOfKeywordsDefault, double[] querySpaceAreaPercentages, double querySpaceAreaPercentageDefault,
                                   int[] keywordSpaceSizePercentages, int keywordSpaceSizePercentageDefault, int[] topks, int topkDefault,
                                   double[] alphas, double alphaDefault, float[] radius, float radiusDefault, int numberOfQueries) {
        this.groupSizes = groupSizes;
        this.groupSizeDefault = groupSizeDefault;
        this.mPercentages = mPercentages;
        this.mPercentageDefault = mPercentageDefault;
        this.numberOfKeywords = numberOfKeywords;
        this.numberOfKeywordsDefault = numberOfKeywordsDefault;
        this.querySpaceAreaPercentages = querySpaceAreaPercentages;
        this.querySpaceAreaPercentageDefault = querySpaceAreaPercentageDefault;
        this.keywordSpaceSizePercentages = keywordSpaceSizePercentages;
        this.keywordSpaceSizePercentageDefault = keywordSpaceSizePercentageDefault;
        this.topks = topks;
        this.topkDefault = topkDefault;
        this.alphas = alphas;
        this.alphaDefault = alphaDefault;
        this.radius = radius;
        this.radiusDefault = radiusDefault;
        this.numberOfQueries = numberOfQueries;
    }


    public void processAggregateQuery(AggregateQueryType[] aggregateQueryTypes, ArrayList<QueryType> queryTypes, IAggregator aggregator) {
        // Query evaluation
        logger.info("Processing and Evaluating queries Aggregate Queries:");
        //ArrayList<ResultQueryCost> results = new ArrayList<>();

        long startTime = System.currentTimeMillis();    // Start time

        // Loop through the aggregate query types
        for(AggregateQueryType aggregateQry : aggregateQueryTypes) {
            logger.info("Processing aggregate query: {}", aggregateQry.toString());
            QueryStats queryStats = new QueryStats(aggregateQry.toString());

            // Loop through the query types
            for(QueryType qryType : queryTypes) {
                logger.info("\t ...based on {}", qryType.toString());
                if(qryType == QueryType.GroupSize) {
                    for(int gs : groupSizes) {
                        QueryStatsData qryData = evaluateAggregateQuery(aggregateQry, qryType, gs, mPercentageDefault, numberOfKeywordsDefault,
                                querySpaceAreaPercentageDefault, keywordSpaceSizePercentageDefault, topkDefault, alphaDefault, aggregator);
                        //qryData.queryType = qryType.toString();
                        qryData.value = String.valueOf(gs);
                        //qryData.totalTime = res.totalTime;
                        queryStats.groupSizes.add(qryData);
                        //ResultQueryParameter param = new ResultQueryParameter("Group Size", Integer.toString(gs), results);
                        //statisticsLogic.globalQueryResults.groupSizes.add(param);
                    }
                } else if (qryType == QueryType.Percentage) {
                    for(int per : mPercentages) {
                        QueryStatsData qryData = evaluateAggregateQuery(aggregateQry, qryType, groupSizeDefault, per, numberOfKeywordsDefault,
                                querySpaceAreaPercentageDefault, keywordSpaceSizePercentageDefault, topkDefault, alphaDefault, aggregator);
                        qryData.value = String.valueOf(per);
                        queryStats.percentages.add(qryData);
                    }
                } else if (qryType == QueryType.NumberOfKeywords) {
                    for(int nkey : numberOfKeywords) {
                        QueryStatsData qryData = evaluateAggregateQuery(aggregateQry, qryType, groupSizeDefault, mPercentageDefault, nkey,
                                querySpaceAreaPercentageDefault, keywordSpaceSizePercentageDefault, topkDefault, alphaDefault, aggregator);
                        qryData.value = String.valueOf(nkey);
                        queryStats.numKeywords.add(qryData);
                    }
                } else if (qryType == QueryType.SpaceAreaPercentage) {
                    for(double area : querySpaceAreaPercentages) {
                        QueryStatsData qryData = evaluateAggregateQuery(aggregateQry, qryType, groupSizeDefault, mPercentageDefault, numberOfKeywordsDefault,
                                area, keywordSpaceSizePercentageDefault, topkDefault, alphaDefault, aggregator);
                        qryData.value = String.valueOf(area);
                        queryStats.querySpaceAreas.add(qryData);
                    }
                } else if (qryType == QueryType.KeywordSpaceSizePercentage) {
                    for(double space : querySpaceAreaPercentages) {
                        QueryStatsData qryData = evaluateAggregateQuery(aggregateQry, qryType, groupSizeDefault, mPercentageDefault, numberOfKeywordsDefault,
                                querySpaceAreaPercentageDefault, space, topkDefault, alphaDefault, aggregator);
                        qryData.value = String.valueOf(space);
                        queryStats.keyboardSpaceSizes.add(qryData);
                    }
                } else if (qryType == QueryType.TopK) {
                    for(int k : topks) {
                        QueryStatsData qryData = evaluateAggregateQuery(aggregateQry, qryType, groupSizeDefault, mPercentageDefault, numberOfKeywordsDefault,
                                querySpaceAreaPercentageDefault, keywordSpaceSizePercentageDefault, k, alphaDefault, aggregator);
                        qryData.value = String.valueOf(k);
                        queryStats.topks.add(qryData);
                    }
                } else if (qryType == QueryType.Alpha) {
                    for(double a : alphas) {
                        QueryStatsData qryData = evaluateAggregateQuery(aggregateQry, qryType, groupSizeDefault, mPercentageDefault, numberOfKeywordsDefault,
                                querySpaceAreaPercentageDefault, keywordSpaceSizePercentageDefault, topkDefault, a, aggregator);
                        qryData.value = String.valueOf(a);
                        queryStats.alphas.add(qryData);
                    }
                }
            }
            statisticsLogic.queriesStats.put(aggregateQry.toString(), queryStats);
        }
        long totalTime = System.currentTimeMillis() - startTime;    // Total time
        logger.info("All aggregate queries done in {} ms", totalTime);

//        memTimes.add(totalTime);
//
//        long startTime = System.currentTimeMillis();
//        for(int n : ns){
//            ArrayList<ResultQueryCost> results = evaluateAggregateQueries(n, mPercentageDefault, numberOfKeywordsDefault, querySpaceAreaPercentageDefault,
//                    keywordSpaceSizePercentageDefault, topkDefault, alphaDefault, aggregator);
//            ResultQueryParameter param = new ResultQueryParameter("Group Size", Integer.toString(n), results);
//            globalQueryResults.groupSizes.add(param);
//        }
//        long totalTime = System.currentTimeMillis() - startTime;
//        logger.info("Group Size done in {} ms", totalTime);
//        memTimes.add(totalTime);
    }

//    public void processAggregateQuery(AggregateQueryType[] aggregateQueryTypes, String aggregator) {
//        // Query evaluation
//        logger.info("Evaluating based on default paremeters...");
//
//        long startTime = System.currentTimeMillis();
//        for (AggregateQueryType aggregateQry : aggregateQueryTypes) {
//            logger.info("Processing aggregate query: {}", aggregateQry.toString());
//            evaluateAggregateQuery(aggregateQry, QueryType.Defaults, groupSizeDefault, mPercentageDefault, numberOfKeywordsDefault,
//                    querySpaceAreaPercentageDefault, keywordSpaceSizePercentageDefault, topkDefault, alphaDefault, aggregator);
//        }
//        long totalTime = System.currentTimeMillis() - startTime;
//        logger.info("Group Size done in {} ms", totalTime);
//
//    }



    private QueryStatsData evaluateAggregateQuery(AggregateQueryType aggregateQueryType, QueryType queryType, int groupSize, int mPercentage, int numberOfKeywords, double querySpaceAreaPercentage,
                                                  double keywordSpacePercentage, int topk, double alphaDistribution, IAggregator aggregator) {
        //ArrayList<ResultQueryCost> resultData = new ArrayList<>();
        QueryStatsData statsData = new QueryStatsData();
        QueryResultWriter resultWriter = new QueryResultWriter();

        statsData.queryType = queryType.toString();

        // Choose between Rtree (for IR and CIR) and RtreeEnhanced (for DIR and CDIR)
        ISpatioTextualIndex tree;
        if(IndexLogic.spatialIndex instanceof RTree) {
            RTree.alphaDistribution = alphaDistribution;
            tree = new IRTree((RTree) IndexLogic.spatialIndex);
        } else {
            RTreeEnhanced.alphaDistribution = alphaDistribution;
            tree = new DIRTree((RTreeEnhanced) IndexLogic.spatialIndex);
        }

        // 1. Generate queries (Keep outside to not measure creation time)
        //List<GNNKQuery> gnnkQueries = GNNKQueryGenerator.generateGNNKQuery(numberOfQueries , groupSize, numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage, aggregator);
        //List<SGNNKQuery> sgnnkQueries = SGNNKQueryGenerator.generateSGNNKQuery(numberOfQueries, groupSize, mPercentage, numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage, aggregator);

        AggregateSKNNQueryGenerator queryGenerator = new AggregateSKNNQueryGenerator(1, parameters);
        List<AggregateSKNNQuery> gnnkQueries = queryGenerator.generateGNNKQuery(numberOfQueries, groupSize, numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage, aggregator);
        List<AggregateSKNNQuery> sgnnkQueries = queryGenerator.generateSGNNKQuery(numberOfQueries, groupSize, mPercentage, numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage, aggregator);

        long startTime = System.currentTimeMillis();
        double totalCost = 0; // It doesn't keep track of MSGNNK cost - too much extra work
        double spatialCost = 0;
        double irCost = 0;

        // Check if the query type is GNNK or SGNNK
        if (aggregateQueryType == AggregateQueryType.GNNK || aggregateQueryType == AggregateQueryType.GNNK_BL) {
            // 1. Generate queries
            //List<GNNKQuery> gnnkQueries = GNNKQueryGenerator.generateGNNKQuery(numberOfQueries , groupSize, numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage, aggregator);

            // 2. Calculate Aggreagate Query
            //QueryResultWriter resultWriter = new QueryResultWriter();
            //ResultQueryCost resultCost = new ResultQueryCost();
            //statsData.queryName = aggregateQueryType.toString();

            for (AggregateSKNNQuery q : gnnkQueries) {
                List<AggregateSKNNQuery.Result> results;
                if(aggregateQueryType == AggregateQueryType.GNNK) {
                    results = tree.gnnkNEW(IndexLogic.invertedFile, q, topk);
                } else {
                    results = tree.gnnkBaselineNEW(IndexLogic.invertedFile, q, topk);
                }

                if(writeQueriesToDisk) {
                    resultWriter.writeAggregateSKNNResult(results);
                    resultWriter.writeLineSeparator();
                }

                totalCost += results.get(0).aggregateCost.totalCost;
                spatialCost += results.get(0).aggregateCost.spatialCost;
                irCost += results.get(0).aggregateCost.irCost;
            }

            //resultData.add(statsData);

        } else {
            // 1. Generate queries
            //List<SGNNKQuery> sgnnkQueries = SGNNKQueryGenerator.generateSGNNKQuery(numberOfQueries, groupSize, mPercentage, numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage, aggregator);

            // 2. Calculate Aggreagate Query
            //QueryResultWriter resultWriter = new QueryResultWriter();
            //ResultQueryCost resultCost = new ResultQueryCost();
            //statsData.queryName = aggregateQueryType.toString();

            for (AggregateSKNNQuery q : sgnnkQueries) {
                if (aggregateQueryType == AggregateQueryType.SGNNK) {
                    List<AggregateSKNNQuery.Result> results;
                    results = tree.sgnnkNEW(IndexLogic.invertedFile, q, topk);

                    totalCost += results.get(0).aggregateCost.totalCost;
                    spatialCost += results.get(0).aggregateCost.spatialCost;
                    irCost += results.get(0).aggregateCost.irCost;

                    if(writeQueriesToDisk)
                        resultWriter.writeAggregateSKNNResult(results);

                } else if (aggregateQueryType == AggregateQueryType.SGNNK_BL) {
                    List<AggregateSKNNQuery.Result> results;
                    results = tree.sgnnkBaselineNEW(IndexLogic.invertedFile, q, topk);

                    totalCost += results.get(0).aggregateCost.totalCost;
                    spatialCost += results.get(0).aggregateCost.spatialCost;
                    irCost += results.get(0).aggregateCost.irCost;

                    if(writeQueriesToDisk)
                        resultWriter.writeAggregateSKNNResult(results);
                } else if (aggregateQueryType == AggregateQueryType.SGNNK_EX) {
                    Map<Integer, List<AggregateSKNNQuery.Result>> results = tree.sgnnkExtendedNEW(IndexLogic.invertedFile, q, topk);
                    List<Integer> subroupSizes = new ArrayList<>(results.keySet());
                    Collections.sort(subroupSizes);

                    if(writeQueriesToDisk) {
                        for (Integer subgroupSize : subroupSizes) {
                            resultWriter.write("Size " + subgroupSize, true);
                            resultWriter.writeAggregateSKNNResult(results.get(subgroupSize));
                            //writer.write("Size " + subgroupSize);
                            //writer.writeSGNNKResult(results.get(subgroupSize));
                        }
                    }
                } else {
                    int holdSubGroupSize = q.subGroupSize;  // Fix for index out of bounds
                    while (q.subGroupSize <= q.groupSize) {
                        if(writeQueriesToDisk)
                            resultWriter.write("Size " + q.subGroupSize, true);
                        //writer.write("Size " + q.subGroupSize);
                        List<AggregateSKNNQuery.Result> results = tree.sgnnkNEW(IndexLogic.invertedFile, q, topk);
                        if(writeQueriesToDisk)
                            resultWriter.writeAggregateSKNNResult(results);
                        //writer.writeSGNNKResult(results);

                        totalCost += results.get(0).aggregateCost.totalCost;
                        spatialCost += results.get(0).aggregateCost.spatialCost;
                        irCost += results.get(0).aggregateCost.irCost;

                        q.subGroupSize++; // This will cause an index out of bound in type 3 query if not reset
                    }
                    q.subGroupSize = holdSubGroupSize; // Restore the original value

                }

                if(writeQueriesToDisk)
                    resultWriter.writeLineSeparator();
            }

        }

        //QueryResultWriter resultWriter = new QueryResultWriter();

        long totalTime = System.currentTimeMillis() - startTime;

        int averageTime = (int) (totalTime / numberOfQueries);
        int averageFileIO = (tree.getIO() + IndexLogic.invertedFile.getIO()) / numberOfQueries;
        double averageNodesVisited = tree.getVisitedNodes() * 1.0 / numberOfQueries; //numOfVisitedNodes
        double averageSpatialCost = spatialCost / numberOfQueries;
        double averageIRCost = irCost / numberOfQueries;    // TODO CHECK THIS


        if(writeQueriesToDisk) {
            resultWriter.write("[" + aggregateQueryType + "]" + " Average nodes visited: " + averageNodesVisited, true);
            resultWriter.write("[" + aggregateQueryType + "]" + " Total time millisecond: " + totalTime, true);
            resultWriter.writeLineSeparator();
            resultWriter.writeToDisk(resultsDirectoryPath, "[" + aggregateQueryType.toString() + "]" + queryType.toString());
            //writer.write(prefix + " Average nodes visited: " + averageNodesVisited);
            //writer.write(prefix + " Total time millisecond: " + totalTime);
        }
        //writer.close();


        logger.debug("Average time millisecond: {}", averageTime);
        logger.debug("Average total IO: {}", averageFileIO);
//			System.out.println("Average tree IO: " + tree.getIO() * 1.0 / count);
//			System.out.println("Average inverted index IO: " + ivIO * 1.0 / count);
        logger.printf(Level.INFO,"TotalTime= %dms avgT= %dms avgIO= %d avgSpatCost= %.6f avgIRCost= %.6f", totalTime, averageTime, averageFileIO, averageSpatialCost, averageIRCost);

        statsData.totalTime = totalTime;
        statsData.averageTime = averageTime;
        statsData.averageNodesVisited = averageNodesVisited;
        statsData.averageSpatialCost = averageSpatialCost;
        statsData.averageIRCost = averageIRCost;

//        resultCost[0] = spatialCost;
//        resultCost[1] = totalCost;

        return statsData;

    }


    public void processRangeQuery(RangeQueryType[] rangeQueryTypes, ArrayList<QueryType> queryTypes) {
        // Query evaluation
        logger.info("Evaluating queries with variable paremeters...");

        long startTime = System.currentTimeMillis();
        for(RangeQueryType rangeQry : rangeQueryTypes) {
            logger.info("Processing range query: {}", rangeQry.toString());
            QueryStats queryStats = new QueryStats(rangeQry.toString());
            for(QueryType qryType : queryTypes) {
                logger.info("Processing based on {}", qryType.toString());
                if(qryType == QueryType.Radius) {
                    for(float r : radius) {
                        QueryStatsData qryData = evaluateRangeQuery(rangeQry, qryType, numberOfKeywordsDefault, querySpaceAreaPercentageDefault, keywordSpaceSizePercentageDefault, alphaDefault, r);
                        qryData.value = String.valueOf(r);
                        queryStats.radii.add(qryData);
                    }
                } else if (qryType == QueryType.NumberOfKeywords) {
                    for(int nkey : numberOfKeywords) {
                        QueryStatsData qryData = evaluateRangeQuery(rangeQry, qryType, nkey, querySpaceAreaPercentageDefault, keywordSpaceSizePercentageDefault, alphaDefault, radiusDefault);
                        qryData.value = String.valueOf(nkey);
                        queryStats.numKeywords.add(qryData);
                    }
                } else if (qryType == QueryType.SpaceAreaPercentage) {
                    for(double area : querySpaceAreaPercentages) {
                        QueryStatsData qryData = evaluateRangeQuery(rangeQry, qryType, numberOfKeywordsDefault, area, keywordSpaceSizePercentageDefault, alphaDefault, radiusDefault);
                        qryData.value = String.valueOf(area);
                        queryStats.querySpaceAreas.add(qryData);
                    }
                } else if (qryType == QueryType.KeywordSpaceSizePercentage) {
                    for(double space : keywordSpaceSizePercentages) {
                        QueryStatsData qryData = evaluateRangeQuery(rangeQry, qryType, numberOfKeywordsDefault, querySpaceAreaPercentageDefault, space, alphaDefault, radiusDefault);
                        qryData.value = String.valueOf(space);
                        queryStats.keyboardSpaceSizes.add(qryData);
                    }
                } else if (qryType == QueryType.Alpha) {
                    for(double a : alphas) {
                        QueryStatsData qryData = evaluateRangeQuery(rangeQry, qryType, numberOfKeywordsDefault, querySpaceAreaPercentageDefault, keywordSpaceSizePercentageDefault, a, radiusDefault);
                        qryData.value = String.valueOf(a);
                        queryStats.alphas.add(qryData);
                    }
                }
            }
            statisticsLogic.queriesStats.put(rangeQry.toString(), queryStats);
        }
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("All range queries done in {} ms", totalTime);
    }


//    public void processRangeQuery(RangeQueryType[] rangeQueryTypes) {
//        // Query evaluation
//        logger.info("Evaluating based on default paremeters...");
//
//        long startTime = System.currentTimeMillis();
//        for (RangeQueryType rangeQry : rangeQueryTypes) {
//            logger.info("Processing aggregate query: {}", rangeQry.toString());
//            evaluateRangeQuery(rangeQry, QueryType.Defaults, numberOfKeywordsDefault, querySpaceAreaPercentageDefault, keywordSpaceSizePercentageDefault, alphaDefault, radiusDefault);
//        }
//        long totalTime = System.currentTimeMillis() - startTime;
//        logger.info("Query done in {} ms", totalTime);
//    }


    private QueryStatsData evaluateRangeQuery(RangeQueryType rangeQueryType, QueryType queryType, int numberOfKeywords, double querySpaceAreaPercentage,
                                              double keywordSpacePercentage, double alphaDistribution, float radius) {
        //ArrayList<ResultQueryCost> resultData = new ArrayList<>();
        QueryStatsData statsData = new QueryStatsData();
        QueryResultWriter resultWriter = new QueryResultWriter();

        statsData.queryType = queryType.toString();

        // Choose between Rtree (for IR and CIR) and RtreeEnhanced (for DIR and CDIR)
        ISpatioTextualIndex tree;
        if(IndexLogic.spatialIndex instanceof RTree) {
            RTree.alphaDistribution = alphaDistribution;
            tree = new IRTree((RTree) IndexLogic.spatialIndex);
        } else {
            RTreeEnhanced.alphaDistribution = alphaDistribution;
            tree = new DIRTree((RTreeEnhanced) IndexLogic.spatialIndex);
        }

        // 1. Generate queries (Keep outside to not measure creation time)
        //List<GNNKQuery> gnnkQueries = GNNKQueryGenerator.generateGNNKQuery(numberOfQueries , groupSize, numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage, aggregator);
        //List<SGNNKQuery> sgnnkQueries = SGNNKQueryGenerator.generateSGNNKQuery(numberOfQueries, groupSize, mPercentage, numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage, aggregator);

        SKNNQueryGenerator queryGenerator = new SKNNQueryGenerator(ramdomSeed, parameters);
        List<SKNNQuery> brskQueries = queryGenerator.generateBooleanRangeQueries(numberOfQueries, numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage);

        long startTime = System.currentTimeMillis();

        // Check if the query type is BRSK
        if (rangeQueryType == RangeQueryType.BRSK) {
            // 1. Generate queries
            //List<BRQuery> brskQueries = BRQueryGenerator.generateBRQueries(numberOfQueries, numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage);

            // 2. Calculate Aggreagate Query
            //QueryResultWriter resultWriter = new QueryResultWriter();
            //ResultQueryCost resultCost = new ResultQueryCost();
            //statsData.queryType = rangeQueryType.toString();

            for (SKNNQuery q : brskQueries) {
                List<SKNNQuery.Result> results;
                results = tree.booleanRangeQueryNEW(IndexLogic.invertedFile, q, radius);

                if(writeQueriesToDisk)
                    resultWriter.writeSKNNResult(results);
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;

        int averageTime = (int) (totalTime / numberOfQueries);
        int averageFileIO = (tree.getIO() + IndexLogic.invertedFile.getIO()) / numberOfQueries;
        double averageNodesVisited = tree.getVisitedNodes() * 1.0 / numberOfQueries; //numOfVisitedNodes


        if (writeQueriesToDisk) {
            resultWriter.write("[" + rangeQueryType + "]" + " Average nodes visited: " + averageNodesVisited, true);
            resultWriter.write("[" + rangeQueryType + "]" + " Total time millisecond: " + totalTime, true);
            resultWriter.writeLineSeparator();
            resultWriter.writeToDisk(resultsDirectoryPath, "[" + rangeQueryType.toString() + "]" + queryType.toString());
        }

        logger.debug("Average time millisecond: {}", averageTime);
        logger.debug("Average total IO: {}", averageFileIO);
        logger.printf(Level.INFO, "TotalTime= %dms avgT= %dms avgIO= %d", totalTime, averageTime, averageFileIO);

        statsData.totalTime = totalTime;
        statsData.averageTime = averageTime;
        statsData.averageNodesVisited = averageNodesVisited;

        return statsData;
    }


//    private QueryStatsData evaluateRangeQuery(RangeQueryType rangeQueryType, QueryType queryType, int numberOfKeywords, double querySpaceAreaPercentage,
//                                                   double keywordSpacePercentage, double alphaDistribution, float radius) {
//        //ArrayList<ResultQueryCost> resultData = new ArrayList<>();
//        QueryStatsData statsData = new QueryStatsData();
//        QueryResultWriter resultWriter = new QueryResultWriter();
//
//        statsData.queryType = queryType.toString();
//
//        // Choose between Rtree and DRtree
//        ISpatioTextualIndex tree;
//        if (IndexLogic.spatialIndex instanceof RTree) {
//            RTree.alphaDistribution = alphaDistribution;
//            tree = new IRTree((RTree) IndexLogic.spatialIndex);
//        } else {
//            RTreeEnhanced.alphaDistribution = alphaDistribution;
//            tree = new DIRTree((RTreeEnhanced) IndexLogic.spatialIndex);
//        }
//
//        // 1. Generate queries
//        List<BRQuery> brskQueries = BRQueryGenerator.generateBRQueries(numberOfQueries, numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage);
//
//        long startTime = System.currentTimeMillis();
//
//        // Check if the query type is BRSK
//        if (rangeQueryType == RangeQueryType.BRSK) {
//            // 1. Generate queries
//            //List<BRQuery> brskQueries = BRQueryGenerator.generateBRQueries(numberOfQueries, numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage);
//
//            // 2. Calculate Aggreagate Query
//            //QueryResultWriter resultWriter = new QueryResultWriter();
//            //ResultQueryCost resultCost = new ResultQueryCost();
//            //statsData.queryType = rangeQueryType.toString();
//
//            for (BRQuery q : brskQueries) {
//                List<BRQuery.Result> results;
//                results = tree.booleanRangeQuery(IndexLogic.invertedFile, q, radius);
//
//                if(writeQueriesToDisk)
//                    resultWriter.writeBRQResult(results);
//            }
//        }
//
//        long totalTime = System.currentTimeMillis() - startTime;
//
//        int averageTime = (int) (totalTime / numberOfQueries);
//        int averageFileIO = (tree.getIO() + IndexLogic.invertedFile.getIO()) / numberOfQueries;
//        double averageNodesVisited = tree.getVisitedNodes() * 1.0 / numberOfQueries; //numOfVisitedNodes
//
//
//        if (writeQueriesToDisk) {
//            resultWriter.write("[" + rangeQueryType + "]" + " Average nodes visited: " + averageNodesVisited, true);
//            resultWriter.write("[" + rangeQueryType + "]" + " Total time millisecond: " + totalTime, true);
//            resultWriter.writeLineSeparator();
//            resultWriter.writeToDisk(resultsDirectoryPath, "[" + rangeQueryType.toString() + "]" + queryType.toString());
//        }
//
//        logger.debug("Average time millisecond: {}", averageTime);
//        logger.debug("Average total IO: {}", averageFileIO);
//        logger.printf(Level.INFO, "TotalTime= %dms avgT= %dms avgIO= %d", totalTime, averageTime, averageFileIO);
//
//        statsData.totalTime = totalTime;
//        statsData.averageTime = averageTime;
//        statsData.averageNodesVisited = averageNodesVisited;
//
//        return statsData;
//    }


    public void processKnnQuery(KnnQueryType[] knnQueryTypes, ArrayList<QueryType> queryTypes) {
        // Query evaluation
        logger.info("Evaluating queries with variable paremeters...");

        long startTime = System.currentTimeMillis();
        for(KnnQueryType knnQry : knnQueryTypes) {
            logger.info("Processing aggregate query: {}", knnQry.toString());
            QueryStats queryStats = new QueryStats(knnQry.toString());
            for(QueryType qryType : queryTypes) {
                logger.info("Processing based on {}", qryType.toString());
                if(qryType == QueryType.TopK) {
                    for(int k : topks) {
                        QueryStatsData qryData = evaluateKnnQuery(knnQry, qryType, numberOfKeywordsDefault, querySpaceAreaPercentageDefault, keywordSpaceSizePercentageDefault, alphaDefault, k);
                        qryData.value = String.valueOf(k);
                        queryStats.topks.add(qryData);
                    }
                } else if (qryType == QueryType.NumberOfKeywords) {
                    for(int nkey : numberOfKeywords) {
                        QueryStatsData qryData = evaluateKnnQuery(knnQry, qryType, nkey, querySpaceAreaPercentageDefault, keywordSpaceSizePercentageDefault, alphaDefault, topkDefault);
                        qryData.value = String.valueOf(nkey);
                        queryStats.numKeywords.add(qryData);
                    }
                } else if (qryType == QueryType.SpaceAreaPercentage) {
                    for(double area : querySpaceAreaPercentages) {
                        QueryStatsData qryData = evaluateKnnQuery(knnQry, qryType, numberOfKeywordsDefault, area, keywordSpaceSizePercentageDefault, alphaDefault, topkDefault);
                        qryData.value = String.valueOf(area);
                        queryStats.querySpaceAreas.add(qryData);
                    }
                } else if (qryType == QueryType.KeywordSpaceSizePercentage) {
                    for(double space : keywordSpaceSizePercentages) {
                        QueryStatsData qryData = evaluateKnnQuery(knnQry, qryType, numberOfKeywordsDefault, querySpaceAreaPercentageDefault, space, alphaDefault, topkDefault);
                        qryData.value = String.valueOf(space);
                        queryStats.keyboardSpaceSizes.add(qryData);
                    }
                } else if (qryType == QueryType.Alpha) {
                    for(double a : alphas) {
                        QueryStatsData qryData = evaluateKnnQuery(knnQry, qryType, numberOfKeywordsDefault, querySpaceAreaPercentageDefault, keywordSpaceSizePercentageDefault, a, topkDefault);
                        qryData.value = String.valueOf(a);
                        queryStats.alphas.add(qryData);
                    }
                }
            }
            statisticsLogic.queriesStats.put(knnQry.toString(), queryStats);
        }
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("Group Size done in {} ms", totalTime);
    }

//    public void processKnnQuery(KnnQueryType[] knnQueryTypes) {
//        KnnQueryType knnQry = knnQueryTypes[0];
//        QueryType qryType = QueryType.TopK;
//        //QueryStatsData qryData = evaluateKnnQuery(knnQry, qryType, numberOfKeywordsDefault, querySpaceAreaPercentageDefault, keywordSpaceSizePercentageDefault, alphaDefault, 2);
//
//        // ArrayList<ResultQueryCost> resultData = new ArrayList<>();
//        QueryStatsData statsData = new QueryStatsData();
//        QueryResultWriter resultWriter = new QueryResultWriter();
//        statsData.queryType = qryType.toString();
//
//        RTree.alphaDistribution = alphaDefault;
//        // Choose between Rtree and DRtree
//        ISpatioTextualIndex tree = new IRTree((RTree) IndexLogic.spatialIndex);
//
//        double[] pCoords = {1.0, 1.0};
//        List<Integer> keywords = new ArrayList<>();
//        keywords.add(1);
//        Query qry = new Query(1, new Point(pCoords), keywords);
//        TopkKnnQuery tkskQueries = new TopkKnnQuery(qry);
//
//        List<TopkKnnQuery.Result> results;
//        results = tree.topkKnnQuery(IndexLogic.invertedFile, tkskQueries, 2);
//
//
//        //System.out.println(qryData);
//    }

    private QueryStatsData evaluateKnnQuery(KnnQueryType knnQueryType, QueryType queryType, int numberOfKeywords, double querySpaceAreaPercentage,
                                            double keywordSpacePercentage, double alphaDistribution, int topk) {
        //ArrayList<ResultQueryCost> resultData = new ArrayList<>();
        QueryStatsData statsData = new QueryStatsData();
        QueryResultWriter resultWriter = new QueryResultWriter();
        statsData.queryType = knnQueryType.toString();

        // Choose between Rtree (for IR and CIR) and RtreeEnhanced (for DIR and CDIR)
        ISpatioTextualIndex tree;
        if(IndexLogic.spatialIndex instanceof RTree) {
            RTree.alphaDistribution = alphaDistribution;
            tree = new IRTree((RTree) IndexLogic.spatialIndex);
        } else {
            RTreeEnhanced.alphaDistribution = alphaDistribution;
            tree = new DIRTree((RTreeEnhanced) IndexLogic.spatialIndex);
        }

        // 1. Generate queries (Keep outside to not measure creation time)
        //List<GNNKQuery> gnnkQueries = GNNKQueryGenerator.generateGNNKQuery(numberOfQueries , groupSize, numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage, aggregator);
        //List<SGNNKQuery> sgnnkQueries = SGNNKQueryGenerator.generateSGNNKQuery(numberOfQueries, groupSize, mPercentage, numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage, aggregator);

        SKNNQueryGenerator queryGenerator = new SKNNQueryGenerator(ramdomSeed, parameters);
        List<SKNNQuery> bkskQueries = queryGenerator.generateBooleanKNNQueries(numberOfQueries, numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage);
        List<SKNNQuery> tkskQueries = queryGenerator.generateTopKNNQueries(numberOfQueries, numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage);

        long startTime = System.currentTimeMillis();

        // Check if the query type is BRSK
        if (knnQueryType == KnnQueryType.BkSK) {
            // 1. Generate queries
            //List<BooleanKnnQuery> bkskQueries = BKQueryGenerator.generateBKQueries(numberOfQueries, numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage);

            // 2. Calculate Aggreagate Query
            //QueryResultWriter resultWriter = new QueryResultWriter();
            //ResultQueryCost resultCost = new ResultQueryCost();
            //statsData.queryName = knnQueryType.toString();

            for (SKNNQuery q : bkskQueries) {
                List<SKNNQuery.Result> results;
                results = tree.booleanKnnQueryNEW(IndexLogic.invertedFile, q, topk);

                if(writeQueriesToDisk)
                    resultWriter.writeSKNNResult(results);
            }
        } else if(knnQueryType == KnnQueryType.TkSK) {
            // 1. Generate queries
            //List<TopkKnnQuery> tkskQueries = TopKQueryGenerator.generateTKQueries(numberOfQueries, numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage);

            // 2. Calculate Aggreagate Query
            //QueryResultWriter resultWriter = new QueryResultWriter();
            //ResultQueryCost resultCost = new ResultQueryCost();
            //statsData.queryName = knnQueryType.toString();

            for (SKNNQuery q : tkskQueries) {
                List<SKNNQuery.Result> results;
                results = tree.topkKnnQueryNEW(IndexLogic.invertedFile, q, topk);

                if(writeQueriesToDisk)
                    resultWriter.writeSKNNResult(results);
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;

        int averageTime = (int) (totalTime / numberOfQueries);
        int averageFileIO = (tree.getIO() + IndexLogic.invertedFile.getIO()) / numberOfQueries;
        double averageNodesVisited = tree.getVisitedNodes() * 1.0 / numberOfQueries; //numOfVisitedNodes


        if (writeQueriesToDisk) {
            resultWriter.write("[" + knnQueryType + "]" + " Average nodes visited: " + averageNodesVisited, true);
            resultWriter.write("[" + knnQueryType + "]" + " Total time millisecond: " + totalTime, true);
            resultWriter.writeLineSeparator();
            resultWriter.writeToDisk(resultsDirectoryPath, "[" + knnQueryType.toString() + "]" + queryType.toString());
        }

        logger.debug("Average time millisecond: {}", averageTime);
        logger.debug("Average total IO: {}", averageFileIO);
        logger.printf(Level.INFO, "TotalTime= %dms avgT= %dms avgIO= %d", totalTime, averageTime, averageFileIO);

        statsData.totalTime = totalTime;
        statsData.averageTime = averageTime;
        statsData.averageNodesVisited = averageNodesVisited;

        return statsData;
    }

//    private QueryStatsData evaluateKnnQuery(KnnQueryType knnQueryType, QueryType queryType, int numberOfKeywords, double querySpaceAreaPercentage,
//                                               double keywordSpacePercentage, double alphaDistribution, int topk) {
//        //ArrayList<ResultQueryCost> resultData = new ArrayList<>();
//        QueryStatsData statsData = new QueryStatsData();
//        QueryResultWriter resultWriter = new QueryResultWriter();
//        statsData.queryType = knnQueryType.toString();
//
//        // Choose between Rtree and DRtree
//        ISpatioTextualIndex tree;
//        if (IndexLogic.spatialIndex instanceof RTree) {
//            RTree.alphaDistribution = alphaDistribution;
//            tree = new IRTree((RTree) IndexLogic.spatialIndex);
//        } else {
//            RTreeEnhanced.alphaDistribution = alphaDistribution;
//            tree = new DIRTree((RTreeEnhanced) IndexLogic.spatialIndex);
//        }
//
//        // 1. Generate queries
//        List<BooleanKnnQuery> bkskQueries = BKQueryGenerator.generateBKQueries(numberOfQueries, numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage);
//        List<TopkKnnQuery> tkskQueries = TopKQueryGenerator.generateTKQueries(numberOfQueries, numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage);
//
//        long startTime = System.currentTimeMillis();
//
//        // Check if the query type is BRSK
//        if (knnQueryType == KnnQueryType.BkSK) {
//            // 1. Generate queries
//            //List<BooleanKnnQuery> bkskQueries = BKQueryGenerator.generateBKQueries(numberOfQueries, numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage);
//
//            // 2. Calculate Aggreagate Query
//            //QueryResultWriter resultWriter = new QueryResultWriter();
//            //ResultQueryCost resultCost = new ResultQueryCost();
//            //statsData.queryName = knnQueryType.toString();
//
//            for (BooleanKnnQuery q : bkskQueries) {
//                List<BooleanKnnQuery.Result> results;
//                results = tree.booleanKnnQuery(IndexLogic.invertedFile, q, topk);
//
//                if(writeQueriesToDisk)
//                    resultWriter.writeBKQResult(results);
//            }
//        } else if(knnQueryType == KnnQueryType.TkSK) {
//            // 1. Generate queries
//            //List<TopkKnnQuery> tkskQueries = TopKQueryGenerator.generateTKQueries(numberOfQueries, numberOfKeywords, querySpaceAreaPercentage, keywordSpacePercentage);
//
//            // 2. Calculate Aggreagate Query
//            //QueryResultWriter resultWriter = new QueryResultWriter();
//            //ResultQueryCost resultCost = new ResultQueryCost();
//            //statsData.queryName = knnQueryType.toString();
//
//            for (TopkKnnQuery q : tkskQueries) {
//                List<TopkKnnQuery.Result> results;
//                results = tree.topkKnnQuery(IndexLogic.invertedFile, q, topk);
//
//                if(writeQueriesToDisk)
//                    resultWriter.writeTKQResult(results);
//            }
//        }
//
//        long totalTime = System.currentTimeMillis() - startTime;
//
//        int averageTime = (int) (totalTime / numberOfQueries);
//        int averageFileIO = (tree.getIO() + IndexLogic.invertedFile.getIO()) / numberOfQueries;
//        double averageNodesVisited = tree.getVisitedNodes() * 1.0 / numberOfQueries; //numOfVisitedNodes
//
//
//        if (writeQueriesToDisk) {
//            resultWriter.write("[" + knnQueryType + "]" + " Average nodes visited: " + averageNodesVisited, true);
//            resultWriter.write("[" + knnQueryType + "]" + " Total time millisecond: " + totalTime, true);
//            resultWriter.writeLineSeparator();
//            resultWriter.writeToDisk(resultsDirectoryPath, "[" + knnQueryType.toString() + "]" + queryType.toString());
//        }
//
//        logger.debug("Average time millisecond: {}", averageTime);
//        logger.debug("Average total IO: {}", averageFileIO);
//        logger.printf(Level.INFO, "TotalTime= %dms avgT= %dms avgIO= %d", totalTime, averageTime, averageFileIO);
//
//        statsData.totalTime = totalTime;
//        statsData.averageTime = averageTime;
//        statsData.averageNodesVisited = averageNodesVisited;
//
//        return statsData;
//    }


//    public int[] getGroupSizes() {
//        return groupSizes;
//    }
//
//    public void setGroupSizes(int[] groupSizes) {
//        this.groupSizes = groupSizes;
//    }
//
//    public int getGroupSizeDefault() {
//        return groupSizeDefault;
//    }
//
//    public void setGroupSizeDefault(int groupSizeDefault) {
//        this.groupSizeDefault = groupSizeDefault;
//    }
//
//    public int[] getmPercentages() {
//        return mPercentages;
//    }
//
//    public void setmPercentages(int[] mPercentages) {
//        this.mPercentages = mPercentages;
//    }
//
//    public int getmPercentageDefault() {
//        return mPercentageDefault;
//    }
//
//    public void setmPercentageDefault(int mPercentageDefault) {
//        this.mPercentageDefault = mPercentageDefault;
//    }
//
//    public int[] getNumberOfKeywords() {
//        return numberOfKeywords;
//    }
//
//    public void setNumberOfKeywords(int[] numberOfKeywords) {
//        this.numberOfKeywords = numberOfKeywords;
//    }
//
//    public int getNumberOfKeywordsDefault() {
//        return numberOfKeywordsDefault;
//    }
//
//    public void setNumberOfKeywordsDefault(int numberOfKeywordsDefault) {
//        this.numberOfKeywordsDefault = numberOfKeywordsDefault;
//    }
//
//    public double[] getQuerySpaceAreaPercentages() {
//        return querySpaceAreaPercentages;
//    }
//
//    public void setQuerySpaceAreaPercentages(double[] querySpaceAreaPercentages) {
//        this.querySpaceAreaPercentages = querySpaceAreaPercentages;
//    }
//
//    public double getQuerySpaceAreaPercentageDefault() {
//        return querySpaceAreaPercentageDefault;
//    }
//
//    public void setQuerySpaceAreaPercentageDefault(double querySpaceAreaPercentageDefault) {
//        this.querySpaceAreaPercentageDefault = querySpaceAreaPercentageDefault;
//    }
//
//    public int[] getKeywordSpaceSizePercentages() {
//        return keywordSpaceSizePercentages;
//    }
//
//    public void setKeywordSpaceSizePercentages(int[] keywordSpaceSizePercentages) {
//        this.keywordSpaceSizePercentages = keywordSpaceSizePercentages;
//    }
//
//    public int getKeywordSpaceSizePercentageDefault() {
//        return keywordSpaceSizePercentageDefault;
//    }
//
//    public void setKeywordSpaceSizePercentageDefault(int keywordSpaceSizePercentageDefault) {
//        this.keywordSpaceSizePercentageDefault = keywordSpaceSizePercentageDefault;
//    }
//
//    public int[] getTopks() {
//        return topks;
//    }
//
//    public void setTopks(int[] topks) {
//        this.topks = topks;
//    }
//
//    public int getTopkDefault() {
//        return topkDefault;
//    }
//
//    public void setTopkDefault(int topkDefault) {
//        this.topkDefault = topkDefault;
//    }
//
//    public double[] getAlphas() {
//        return alphas;
//    }
//
//    public void setAlphas(double[] alphas) {
//        this.alphas = alphas;
//    }
//
//    public double getAlphaDefault() {
//        return alphaDefault;
//    }
//
//    public void setAlphaDefault(double alphaDefault) {
//        this.alphaDefault = alphaDefault;
//    }
//
//    public float[] getRadius() {
//        return radius;
//    }
//
//    public void setRadius(float[] radius) {
//        this.radius = radius;
//    }
//
//    public float getRadiusDefault() {
//        return radiusDefault;
//    }
//
//    public void setRadiusDefault(float radiusDefault) {
//        this.radiusDefault = radiusDefault;
//    }
//
//    public int getNumberOfQueries() {
//        return numberOfQueries;
//    }
//
//    public void setNumberOfQueries(int numberOfQueries) {
//        this.numberOfQueries = numberOfQueries;
//    }

    public boolean isWriteQueriesToDisk() {
        return writeQueriesToDisk;
    }

    public void setWriteQueriesToDisk(boolean writeQueriesToDisk) {
        this.writeQueriesToDisk = writeQueriesToDisk;
    }

    public void setQueryResults(ResultQueryTotal globalQueryResults) {
        this.statisticsLogic.globalQueryResults = globalQueryResults;
    }


    public enum QueryType {
        GroupSize,
        Percentage,
        NumberOfKeywords,
        SpaceAreaPercentage,
        KeywordSpaceSizePercentage,
        TopK,
        Alpha,
        Radius,
        Defaults
   }

    public enum AggregateQueryType {
        GNNK,
        GNNK_BL,
        SGNNK,
        SGNNK_BL,
        SGNNK_EX,
        SGNNK_NM1
    }

    public enum KnnQueryType {
        BkSK,
        TkSK
    }

    public enum RangeQueryType {
        BRSK
    }

    public enum JoinQueryType {

    }









}
