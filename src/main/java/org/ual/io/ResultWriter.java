package org.ual.io;

import org.ual.querytype.aggregate.GNNKQuery;
import org.ual.querytype.aggregate.SGNNKQuery;
import org.ual.querytype.knn.BooleanKnnQuery;
import org.ual.querytype.knn.TopkKnnQuery;
import org.ual.querytype.range.BRQuery;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Class to write query results to disk
 */
public class ResultWriter {
    private BufferedWriter writer;
    private int queryCount = 0;
    private boolean printInConsole;

    public ResultWriter(int noOfQueries, String outputDirectory, boolean printInConsole) throws IOException {
        this.printInConsole = printInConsole;

        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("MM-dd-HH:mm:ss:SS");
        String outputFileName = outputDirectory + "result-(" + noOfQueries + ")-" + format.format(date) + ".txt"; // "src/main/resources/data/output/result-("
        writer = new BufferedWriter(new FileWriter(outputFileName));
    }

    public ResultWriter(int noOfQueries, String outputDirectory, boolean printInConsole, String prefix) throws IOException {
        this.printInConsole = printInConsole;

        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("MM-dd-HH:mm:ss:SS");
        String outputFileName = outputDirectory + prefix + "result-(" + noOfQueries + ")-" + format.format(date) + ".txt"; // "src/main/resources/data/output/result-("
        writer = new BufferedWriter(new FileWriter(outputFileName));
    }

    public ResultWriter(int noOfQueries, String outputDirectory) throws IOException {
        this(noOfQueries, outputDirectory, false);
    }

    public void writeGNNKResult(List<GNNKQuery.Result> results) throws IOException {
        write("Query " + queryCount);
        for (GNNKQuery.Result result : results) {
            write(String.format("%d %.3f", result.id, result.cost.totalCost));
        }
        queryCount++;
        write("");
    }

    public void writeSGNNKResult(List<SGNNKQuery.Result> results) throws IOException {
        write("Query " + queryCount);
        for (SGNNKQuery.Result result : results) {
            // Fix for NullPointer Reference
            if (result.queryIds != null) {
                write(String.format("%d %.3f %s", result.id, result.cost.totalCost, result.queryIds));
            }
            else {
                write(String.format("%d %.3f, ", result.id, result.cost.totalCost));
            }
        }
        queryCount++;
        write("");
    }

    public void writeBRQResult(List<BRQuery.Result> results) throws IOException {
        write("Query " + queryCount);
        for (BRQuery.Result result : results) {
            write(String.format("ID: %d - MinDist: %.3f", result.id, result.minDistance));//.totalCost
        }
        queryCount++;
        write("");
    }

    public void writeBKQResult(List<BooleanKnnQuery.Result> results) throws IOException {
        write("Query " + queryCount);
        for (BooleanKnnQuery.Result result : results) {
            write(String.format("ID: %d - MinDist: %.3f", result.id, result.minDistance));//.totalCost
        }
        queryCount++;
        write("");
    }

    public void writeTKQResult(List<TopkKnnQuery.Result> results) throws IOException {
        write("Query " + queryCount);
        for (TopkKnnQuery.Result result : results) {
            write(String.format("ID: %d - MinDist: %.3f - Cost: %.3f", result.id, result.minDistance, result.cost));//.totalCost
        }
        queryCount++;
        write("");
    }


    public void write(String str) throws IOException {
        write(str, true);
    }

    public void write(String str, boolean printNewline) throws IOException {
        if (printInConsole)
            System.out.print(str);
        writer.write(str);
        if (printNewline) {
            if (printInConsole)
                System.out.println();
            writer.write("\n");
        }
    }

    public void close() throws IOException {
        writer.close();
    }



}
