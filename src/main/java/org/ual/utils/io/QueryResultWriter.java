package org.ual.utils.io;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ual.querytype.aggregate.GNNKQuery;
import org.ual.querytype.aggregate.SGNNKQuery;
import org.ual.querytype.knn.BooleanKnnQuery;
import org.ual.querytype.knn.TopkKnnQuery;
import org.ual.querytype.range.BRQuery;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class QueryResultWriter {
    private static StringBuilder stringBuilder = new StringBuilder();
    private int queryCount = 0;

    private static final Logger logger = LogManager.getLogger(QueryResultWriter.class);

    public void writeGNNKResult(List<GNNKQuery.Result> results) {
        stringBuilder.append("Query ").append(queryCount).append("\n");
        for (GNNKQuery.Result result : results) {
            stringBuilder.append(String.format("ID: %d COST: %.3f\n", result.id, result.cost.totalCost));
        }
        queryCount++;
        stringBuilder.append("\n");
    }

    public void writeSGNNKResult(List<SGNNKQuery.Result> results) {
        stringBuilder.append("Query ").append(queryCount).append("\n");
        for (SGNNKQuery.Result result : results) {
            // Fix for NullPointer Reference
            if (result.queryIds != null) {
                stringBuilder.append(String.format("ID: %d COST: %.3f QRY_IDs: %s\n", result.id, result.cost.totalCost, result.queryIds));
            }
            else {
                stringBuilder.append(String.format("ID: %d COST: %.3f\n", result.id, result.cost.totalCost));
            }
        }
        queryCount++;
        stringBuilder.append("\n");
    }

    public void writeBRQResult(List<BRQuery.Result> results) {
        stringBuilder.append("Query ").append(queryCount).append("\n");
        for (BRQuery.Result result : results) {
            stringBuilder.append(String.format("ID: %d - MinDist: %.3f\n", result.id, result.minDistance));//.totalCost
        }
        queryCount++;
        stringBuilder.append("\n");
    }

    public void writeBKQResult(List<BooleanKnnQuery.Result> results) {
        stringBuilder.append("Query ").append(queryCount).append("\n");
        for (BooleanKnnQuery.Result result : results) {
            stringBuilder.append(String.format("ID: %d - MinDist: %.3f\n", result.id, result.minDistance));//.totalCost
        }
        queryCount++;
        stringBuilder.append("\n");
    }

    public void writeTKQResult(List<TopkKnnQuery.Result> results) {
        stringBuilder.append("Query ").append(queryCount).append("\n");
        for (TopkKnnQuery.Result result : results) {
            stringBuilder.append(String.format("ID: %d - MinDist: %.3f - Cost: %.3f\n", result.id, result.minDistance, result.cost));//.totalCost
        }
        queryCount++;
        stringBuilder.append("\n");
    }

    public void writeLineSeparator() {
        stringBuilder.append("====================================================\n");
    }

    public void write(String str, boolean printNewline) {
        stringBuilder.append(str);
        if (printNewline) {
            stringBuilder.append("\n");
        }
    }


    // Writer to disk when done
    public void writeToDisk(String filePath, String fileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath + fileName + ".txt", true))) {
            writer.append(stringBuilder);
        } catch (IOException e) {
            logger.error("Fail to write queries", e);
        }
//        try (FileWriter fw = new FileWriter(filePath + fileName + ".txt", true);
//             BufferedWriter bw = new BufferedWriter(fw);
//             bw.write(stringBuilder);
//             PrintWriter out = new PrintWriter(bw)) {
//            out.println("==================================================");
//            out.println("Number of Queries: " + queryCount);
//            out.println("==================================================");
//            out.append(stringBuilder);
//        } catch (IOException e) {
//            logger.error("Fail to write queries", e);
//        }
    }
}
