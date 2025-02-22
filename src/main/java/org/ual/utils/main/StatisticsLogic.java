package org.ual.utils.main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ual.utils.ResultQueryTotal;
import org.ual.utils.io.StatisticsResultWriter;
import org.ual.utils.stats.QueryStats;
import org.ual.utils.stats.QueryStatsData;

import java.io.*;
import java.util.*;

public class StatisticsLogic {
    private final String metricsDirectoryPath;
    public static long weightIndexMemUsed;
    public static long weightIndexPeakMemUsed;
    public static long rTreeMemUsed;
    public static long rTreePeakMemUsed;
    public static long irTreeMemUsed;
    public static long irTreePeakMemUsed;
    public static long weightIndexBuildTime;
    public static long rTreeBuildTime;
    public static long irTreeBuildTime;
    public ResultQueryTotal globalQueryResults;
    public HashMap<String, QueryStats> queriesStats = new HashMap<>();

    private static final Logger logger = LogManager.getLogger(StatisticsLogic.class);


    public StatisticsLogic(String metricsDirectoryPath) {
        this.metricsDirectoryPath = metricsDirectoryPath;
    }

    /**
     * Get the memory used by the JVM without garbage collection
     * @return the memory used by the JVM
     */
    public static long getMemUsed() {
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long usedMemory = totalMemory - freeMemory;

        return usedMemory;
    }

    /**
     * WARNING!: DO NOT USE inbetween time measures. In introduce a 500ms delay.
     * Get the memory used by the JVM after garbage collection
     * @return the memory used by the JVM after garbage collection
     */
    public static long getClearedMem() {
        for (int i = 0; i < 5; i++) {
            System.gc();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                logger.error("Fail to clean memory", e);
            }
        }
        return getMemUsed();
    }


    public void writeResults(/*ResultQueryTotal globalQueryResults, String metricsDirectoryPath */) {
        logger.info("Writing Results...");

//        StatisticsResultWriter.writeCSV(globalQueryResults.groupSizes, "group-size", globalQueryResults.queryType, metricsDirectoryPath);
//        StatisticsResultWriter.writeCSV(globalQueryResults.percentages, "subgroup-size", globalQueryResults.queryType, metricsDirectoryPath);
//        StatisticsResultWriter.writeCSV(globalQueryResults.numKeywords, "number-of-keyword", globalQueryResults.queryType, metricsDirectoryPath);
//        StatisticsResultWriter.writeCSV(globalQueryResults.querySpaceAreas, "query-space-area", globalQueryResults.queryType, metricsDirectoryPath);
//        StatisticsResultWriter.writeCSV(globalQueryResults.keyboardSpaceSizes, "keyword-space-size", globalQueryResults.queryType, metricsDirectoryPath);
//        StatisticsResultWriter.writeCSV(globalQueryResults.topks, "topk", globalQueryResults.queryType, metricsDirectoryPath);
//        StatisticsResultWriter.writeCSV(globalQueryResults.radii, "range", globalQueryResults.queryType, metricsDirectoryPath);
//        StatisticsResultWriter.writeCSV(globalQueryResults.alphas, "alpha", globalQueryResults.queryType, metricsDirectoryPath);
//
//        StatisticsResultWriter.resultWriter(globalQueryResults.groupSizes, "group-size", globalQueryResults.queryType, metricsDirectoryPath);
//        StatisticsResultWriter.resultWriter(globalQueryResults.percentages,"subgroup-size", globalQueryResults.queryType, metricsDirectoryPath);
//        StatisticsResultWriter.resultWriter(globalQueryResults.numKeywords, "number-of-keyword", globalQueryResults.queryType, metricsDirectoryPath);
//        StatisticsResultWriter.resultWriter(globalQueryResults.querySpaceAreas, "query-space-area", globalQueryResults.queryType, metricsDirectoryPath);
//        StatisticsResultWriter.resultWriter(globalQueryResults.keyboardSpaceSizes, "keyword-space-size", globalQueryResults.queryType, metricsDirectoryPath);
//        StatisticsResultWriter.resultWriter(globalQueryResults.topks, "topk", globalQueryResults.queryType, metricsDirectoryPath);
//        StatisticsResultWriter.resultWriter(globalQueryResults.radii, "range", globalQueryResults.queryType, metricsDirectoryPath);
//        StatisticsResultWriter.resultWriter(globalQueryResults.alphas, "alpha", globalQueryResults.queryType, metricsDirectoryPath);

        //StatisticsResultWriter.resultWriter(queriesStats, metricsDirectoryPath, true);

        StatisticsLogic.resultWriter(queriesStats, metricsDirectoryPath, true);
        StatisticsLogic.resultWriter(queriesStats, metricsDirectoryPath, false);

        queriesStats.clear(); // Fix a "buffer leak" when changing the query type

        logger.info("Done");
    }


    // Write human-readable file
//    public static void resultWriter(HashMap<String, QueryStats> queriesStats, String metricsDirectoryPath, boolean writeCSV) {
//        for (Map.Entry<String, QueryStats> qryType : queriesStats.entrySet()) {
//            if (!qryType.getValue().groupSizes.isEmpty()) {
//                String fileName = "[" + qryType.getKey() + "]" + "GroupSize"; // [Aggregate] alpha
//                if (writeCSV)
//                    writeCSV(metricsDirectoryPath, fileName, qryType.getValue().groupSizes);
//                else
//                    writeTXT(metricsDirectoryPath, fileName, qryType.getValue().groupSizes);
//            }
//            if (!qryType.getValue().alphas.isEmpty()) {
//                String fileName = "[" + qryType.getKey() + "]" + "Alpha"; // [Aggregate] alpha
//                if (writeCSV)
//                    writeCSV(metricsDirectoryPath, fileName, qryType.getValue().alphas);
//                else
//                    writeTXT(metricsDirectoryPath, fileName, qryType.getValue().alphas);
//            }
//            if (!qryType.getValue().numKeywords.isEmpty()) {
//                String fileName = "[" + qryType.getKey() + "]" + "NumberKeywords"; // [Aggregate] alpha
//                if (writeCSV)
//                    writeCSV(metricsDirectoryPath, fileName, qryType.getValue().numKeywords);
//                else
//                    writeTXT(metricsDirectoryPath, fileName, qryType.getValue().numKeywords);
//            }
//            if (!qryType.getValue().percentages.isEmpty()) {
//                String fileName = "[" + qryType.getKey() + "]" + "Percentages"; // [Aggregate] alpha
//                if (writeCSV)
//                    writeCSV(metricsDirectoryPath, fileName, qryType.getValue().percentages);
//                else
//                    writeTXT(metricsDirectoryPath, fileName, qryType.getValue().percentages);
//            }
//            if (!qryType.getValue().keyboardSpaceSizes.isEmpty()) {
//                String fileName = "[" + qryType.getKey() + "]" + "KeywordSpaceSize"; // [Aggregate] alpha
//                if (writeCSV)
//                    writeCSV(metricsDirectoryPath, fileName, qryType.getValue().keyboardSpaceSizes);
//                else
//                    writeTXT(metricsDirectoryPath, fileName, qryType.getValue().keyboardSpaceSizes);
//            }
//            if (!qryType.getValue().querySpaceAreas.isEmpty()) {
//                String fileName = "[" + qryType.getKey() + "]" + "SpaceArea"; // [Aggregate] alpha
//                if (writeCSV)
//                    writeCSV(metricsDirectoryPath, fileName, qryType.getValue().querySpaceAreas);
//                else
//                    writeTXT(metricsDirectoryPath, fileName, qryType.getValue().querySpaceAreas);
//            }
//            if (!qryType.getValue().radii.isEmpty()) {
//                String fileName = "[" + qryType.getKey() + "]" + "Radius"; // [Aggregate] alpha
//                if (writeCSV)
//                    writeCSV(metricsDirectoryPath, fileName, qryType.getValue().radii);
//                else
//                    writeTXT(metricsDirectoryPath, fileName, qryType.getValue().radii);
//            }
//            if (!qryType.getValue().topks.isEmpty()) {
//                String fileName = "[" + qryType.getKey() + "]" + "TopK"; // [Aggregate] alpha
//                if (writeCSV)
//                    writeCSV(metricsDirectoryPath, fileName, qryType.getValue().topks);
//                else
//                    writeTXT(metricsDirectoryPath, fileName, qryType.getValue().topks);
//            }
//        }
//    }

    public static void resultWriter(HashMap<String, QueryStats> queriesStats, String metricsDirectoryPath, boolean writeCSV) {
        for (Map.Entry<String, QueryStats> qryType : queriesStats.entrySet()) {
            writeData(metricsDirectoryPath, qryType.getKey(), "GroupSize", qryType.getValue().groupSizes, writeCSV);
            writeData(metricsDirectoryPath, qryType.getKey(), "Alpha", qryType.getValue().alphas, writeCSV);
            writeData(metricsDirectoryPath, qryType.getKey(), "NumberKeywords", qryType.getValue().numKeywords, writeCSV);
            writeData(metricsDirectoryPath, qryType.getKey(), "Percentages", qryType.getValue().percentages, writeCSV);
            writeData(metricsDirectoryPath, qryType.getKey(), "KeywordSpaceSize", qryType.getValue().keyboardSpaceSizes, writeCSV);
            writeData(metricsDirectoryPath, qryType.getKey(), "SpaceArea", qryType.getValue().querySpaceAreas, writeCSV);
            writeData(metricsDirectoryPath, qryType.getKey(), "Radius", qryType.getValue().radii, writeCSV);
            writeData(metricsDirectoryPath, qryType.getKey(), "TopK", qryType.getValue().topks, writeCSV);
        }
    }

    private static void writeData(String metricsDirectoryPath, String queryKey, String dataType, List<QueryStatsData> data, boolean writeCSV) {
        if (!data.isEmpty()) {
            String fileName = "[" + queryKey + "]" + dataType;
            if (writeCSV)
                writeCSV(metricsDirectoryPath, fileName, data);
            else
                writeTXT(metricsDirectoryPath, fileName, data);
        }
    }


    private static void writeTXT(String metricsDirectoryPath, String fileName, List<QueryStatsData> qryData) {
        try (FileWriter fw = new FileWriter(metricsDirectoryPath + fileName + ".txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println("==================================================");
            out.println("");
            for (QueryStatsData resultData : qryData) {
                out.println("Parameter: " + resultData.queryType + " - Value: " + resultData.value);
                out.printf("[%s] totalTime= %dms | avgTime= %dms | avgNodesVisited= %f | avgSpatCost= %.6f | avgIRCost= %.6f \n",
                        resultData.queryType, resultData.totalTime, resultData.averageTime, resultData.averageNodesVisited, resultData.averageSpatialCost,
                        resultData.averageIRCost);
                out.println("");
            }
            out.println("");
            out.println("==================================================");
        } catch (IOException e) {
            logger.error("Fail to write results", e);
        }
    }

    private static void writeCSV(String metricsDirectoryPath, String fileName, List<QueryStatsData> qryData) {
        ArrayList<String> headers = new ArrayList<>();
        ArrayList<String> row = new ArrayList<>();
        boolean writeHeaders = true;

        for (QueryStatsData resultData : qryData) {
            headers.add(resultData.value);
            row.add(String.valueOf(resultData.totalTime));
        }

        // Check if file exist to skip writing headers
        File file = new File(metricsDirectoryPath + fileName + ".csv");
        if (file.exists())
            writeHeaders = false;

        try (FileWriter fw = new FileWriter(metricsDirectoryPath + fileName + ".csv", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            // Write headers
            if (writeHeaders)
                out.println(String.join(",", headers));

            // Write rows
            out.println(String.join(",", row));

        } catch (IOException e) {
            logger.error("Fail to write results in csv: ", e);
        }


    }


//    private static void writeCSV(String metricsDirectoryPath, String fileName, List<QueryStatsData> qryData) {
//        ArrayList<String> headers = new ArrayList<>();
//        ArrayList<String> row = new ArrayList<>();
//        boolean writeHeaders = true;
//
//        //HashMap<String, HashMap<String, String>> values = new HashMap<>();
//        HashMap<String, ArrayList<String>> values = new HashMap<>();
//        // paramName, paramVal, paramVal, ...
//        // queryName, val, val, ...
//
//        for (QueryStatsData resultData : qryData) {
//            headers.add(resultData.value);
//            row.add(String.valueOf(resultData.totalTime));
//
//            // Check if file exist to skip writing headers
//            File file = new File(metricsDirectoryPath + fileName + ".csv");
//            if (file.exists())
//                writeHeaders = false;
//
//            try (FileWriter fw = new FileWriter(metricsDirectoryPath + fileName + ".csv", true);
//                 BufferedWriter bw = new BufferedWriter(fw);
//                 PrintWriter out = new PrintWriter(bw)) {
//
//                // Write headers
//                if (writeHeaders)
//                    out.println(String.join(",", headers));
//
//                // Write rows
//                out.println(String.join(",", row));
//
//            } catch (IOException e) {
//                logger.error("Fail to write results", e);
//            }
//
//        }
//
//    }
}