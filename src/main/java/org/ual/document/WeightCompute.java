package org.ual.document;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ual.spatialindex.storage.IStore;
import org.ual.spatialindex.storage.Weight;
import org.ual.spatialindex.storage.WeightEntry;

import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.*;

public class WeightCompute {
    private static final Logger logger = LogManager.getLogger(WeightCompute.class);


    /**
     * Generates a Weighted Keyword list from an input file (ex. words.txt).
     * This will populate {@code weightList} with {@code Weight} objects, which contains a {@code docID} and a collection of terms and their weights.
     *
     * The source file utils.Parameters contains some variables specific to the dataset. Edit these parameters if necessary.
     *
     * @param wordsFile
     * @param weightList
     */
    public static void ComputeTermWeights(String wordsFile, IStore weightList) {
        double lmd = 0.2; // smoothing factor

        // Contains <term, frequency> pairs for each term in the input file
        ArrayList<String[]> lines = new ArrayList<>();
        // Term Frequency dictionary
        HashMap<String, Integer> wordsFreq = new HashMap<>();
        // Weights per term
        ArrayList<WeightEntry> wordWeights = new ArrayList<>();

        try {
            LineNumberReader lr = new LineNumberReader(new FileReader(wordsFile));
            int totalLength = 0;
            double maxWeight = 0;

            String line = lr.readLine();    // 0,1,2,3,4,5
            while (line != null) {
                String[] cols = line.split(",");    // [0,1,2,3,4,5]
                lines.add(cols);
                for (int i = 1; i < cols.length; i++) {
                    totalLength++;
                    if (wordsFreq.containsKey(cols[i])) {
                        int count = wordsFreq.get(cols[i]);
                        wordsFreq.put(cols[i], count + 1);
                    } else {
                        wordsFreq.put(cols[i], 1);
                    }
                }
                line = lr.readLine();
            }
            lr.close();

            // Calculate term weight based on total term frequency
            for (String[] cols : lines) {
                // Contains <term, frequency> pairs for each term in individual document
                HashMap<String, Integer> sent = new HashMap<>();
                String wordID = cols[0];

                for (int i = 1; i < cols.length; i++) {
                    if (sent.containsKey(cols[i])) {
                        int count = sent.get(cols[i]);
                        sent.put(cols[i], count + 1);
                    } else {
                        sent.put(cols[i], 1);
                    }
                }

                Iterator<Map.Entry<String, Integer>> iter = sent.entrySet().iterator();
                String buf = "";
                while (iter.hasNext()) {
                    Map.Entry<String, Integer> entry = iter.next();
                    String word = entry.getKey();
                    double termFreqInRow = entry.getValue();	// Term frequency in this row
                    double termTotalFreq = wordsFreq.get(word);			// Term frequency in all documents

                    double weight = (1 - lmd) * termFreqInRow / (cols.length - 1)
                            + lmd * termTotalFreq / totalLength;
                    weight = Math.pow(weight, termFreqInRow);
                    wordWeights.add(new WeightEntry(Integer.parseInt(word), weight));
                    buf += word + " " + weight + ",";
                    maxWeight = Math.max(maxWeight, weight);
                }
                buf = buf.substring(0, buf.length() - 1);
                logger.debug("WordID: {}, Weight: {}", wordID, buf);

                weightList.write(new Weight(Integer.parseInt(wordID), new ArrayList<>(wordWeights)));
                //weightList.write(Integer.parseInt(wordID), wordWeights);
                wordWeights.clear();
            }
        } catch (Exception e) {
            logger.error("Error while operating with weight file.", e);
        }
    }


    /**
     * Generates a Weighted Keyword list from an input file (ex. words.txt).
     * This will populate {@code weightList} with {@code Weight} objects, which contains a {@code docID} and a collection of terms and their weights.
     *
     * The source file utils.Parameters contains some variables specific to the dataset. Edit these parameters if necessary.
     *
     * @param wordsFile filepath
     * @param weightList list of weights
     */
    public static void ComputeTermWeights(String wordsFile, List<Weight> weightList) {
        double lmd = 0.2; // smoothing factor

        // Contains <term, frequency> pairs for each term in the input file
        ArrayList<String[]> lines = new ArrayList<>();
        // Term Frequency dictionary
        HashMap<String, Integer> wordsFreq = new HashMap<>();
        // Weights per term
        ArrayList<WeightEntry> wordWeights = new ArrayList<>();

        try {
            LineNumberReader lr = new LineNumberReader(new FileReader(wordsFile));
            int totalLength = 0;
            double maxWeight = 0;

            String line = lr.readLine();    // 0,1,2,3,4,5
            while (line != null) {
                String[] cols = line.split(",");    // [0,1,2,3,4,5]
                lines.add(cols);
                for (int i = 1; i < cols.length; i++) {
                    totalLength++;
                    if (wordsFreq.containsKey(cols[i])) {
                        int count = wordsFreq.get(cols[i]);
                        wordsFreq.put(cols[i], count + 1);
                    } else {
                        wordsFreq.put(cols[i], 1);
                    }
                }
                line = lr.readLine();
            }
            lr.close();

            // Calculate term weight based on total term frequency
            for (String[] cols : lines) {
                // Contains <term, frequency> pairs for each term in individual document
                HashMap<String, Integer> sent = new HashMap<>();
                String wordID = cols[0];

                for (int i = 1; i < cols.length; i++) {
                    if (sent.containsKey(cols[i])) {
                        int count = sent.get(cols[i]);
                        sent.put(cols[i], count + 1);
                    } else {
                        sent.put(cols[i], 1);
                    }
                }

                Iterator<Map.Entry<String, Integer>> iter = sent.entrySet().iterator();
                String buf = "";
                while (iter.hasNext()) {
                    Map.Entry<String, Integer> entry = iter.next();
                    String word = entry.getKey();
                    double termFreqInRow = entry.getValue();	// Term frequency in this row
                    double termTotalFreq = wordsFreq.get(word);			// Term frequency in all documents

                    double weight = (1 - lmd) * termFreqInRow / (cols.length - 1)
                            + lmd * termTotalFreq / totalLength;
                    weight = Math.pow(weight, termFreqInRow);
                    wordWeights.add(new WeightEntry(Integer.parseInt(word), weight));
                    buf += word + " " + weight + ",";
                    maxWeight = Math.max(maxWeight, weight);
                }
                buf = buf.substring(0, buf.length() - 1);
                logger.info("WordID: {}, Weight: {}", wordID, buf);
                //System.out.println(wordID + "," + buf);

                weightList.add(new Weight(Integer.parseInt(wordID), wordWeights));
                wordWeights.clear();
            }
        } catch (Exception e) {
            logger.error("Error while operating with weight file.", e);
        }
    }

}
