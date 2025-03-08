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
    public static long rTreeJVMPeakMemUsed;
    public static long irTreeMemUsed;
    public static long irTreePeakMemUsed;
    public static long irTreeJVMPeakMemUsed;
    public static long weightIndexBuildTime;
    public static long rTreeBuildTime;
    public static long irTreeBuildTime;
    public ResultQueryTotal globalQueryResults;
    public HashMap<String, QueryStats> queriesStats = new HashMap<>();

    // Testing memory usage using threads
    public static long maxMemoryUsage = 0;
    private static boolean monitoring = false;
    private static Thread memoryMonitorThread;

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

    /**
     * Start monitoring the memory usage
     * WARNING!: This method should be called before the memory usage is expected to increase
     */
    public static void startMemoryMonitoring() {
        monitoring = true;
        maxMemoryUsage = 0; // Reset the max memory usage
        memoryMonitorThread = new Thread(() -> {
            while (monitoring) {
                long currentMemoryUsage = getMemUsed();
                if (currentMemoryUsage > maxMemoryUsage) {
                    maxMemoryUsage = currentMemoryUsage;
                }
                try {
                    Thread.sleep(100); // Adjust the interval as needed
                } catch (InterruptedException e) {
                    logger.error("Memory monitoring thread interrupted", e);
                }
            }
        });
        memoryMonitorThread.start();
    }

    /**
     * Stop monitoring the memory usage
     */
    public static void stopMemoryMonitoring() {
        monitoring = false;
        if (memoryMonitorThread != null) {
            try {
                memoryMonitorThread.join();
            } catch (InterruptedException e) {
                logger.error("Failed to stop memory monitoring thread", e);
            }
        }
    }

    /**
     * Get the maximum memory usage
     * WARNING!: This method should NOT be called while the memory monitor is running to prevent a race condition
     * @return the maximum memory usage by the JVM
     */
    public static long getMaxMemoryUsage() {
        return maxMemoryUsage;
    }


    /**
     * Write the stats of the queries results in the metrics directory, int txt and csv format
     */
    public void writeResults() {
        logger.info("Writing Results...");

        StatisticsLogic.resultWriter(queriesStats, metricsDirectoryPath, true);
        StatisticsLogic.resultWriter(queriesStats, metricsDirectoryPath, false);
        queriesStats.clear(); // Fix a "buffer leak" when changing the query type

        logger.info("Done");
    }


    private static void resultWriter(HashMap<String, QueryStats> queriesStats, String metricsDirectoryPath, boolean writeCSV) {
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