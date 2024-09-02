package org.ual.querygeneration;

import org.ual.query.Query;
import org.ual.spatialindex.spatialindex.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class QueryGenerator {
    static final Random RANDOM = new Random(1);
    static int NUMBER_OF_QUERIES = 20;

    /**
     * Generate a query with keywords and weights
     */
    static Query createKWQuery(int queryId, double queryWeight, int numberOfKeywords, int keywordSpaceMiddle, int keywordSpaceSpan,
                             double centroidLatitude, double centroidLongtitude, double latitudeSpan, double longtitudeSpan) {
        List<Query> queries = new ArrayList<>();

        double x = (centroidLatitude - latitudeSpan / 2) + RANDOM.nextDouble() * latitudeSpan;
        double y = (centroidLongtitude - longtitudeSpan / 2) + RANDOM.nextDouble() * longtitudeSpan;

        List<Integer> keywords = new ArrayList<>();
        List<Double> keywordWeights = new ArrayList<>();

        double weightTotal = 0.0;

        for (int k = 0; k < numberOfKeywords; k++) {
            int keyword = keywordSpaceMiddle + RANDOM.nextInt(keywordSpaceSpan);
            double keywordWeight = 0.5 + RANDOM.nextDouble() / 2;
            weightTotal += keywordWeight;

            keywords.add(keyword);
            keywordWeights.add(keywordWeight);
        }

        for (int k = 0; k < keywordWeights.size(); k++) {
            double weight = (keywordWeights.get(k) / weightTotal);
            keywordWeights.set(k, weight);
        }

        Query query = new Query(queryId, queryWeight, new Point(new double[]{x, y}), keywords, keywordWeights);

        return query;
    }

    /**
     * Generate a keyword only query, without weights
     */
    static Query createTopKQuery(int queryId, int numberOfKeywords, int keywordSpaceMiddle, int keywordSpaceSpan,
                                double centroidLatitude, double centroidLongtitude, double latitudeSpan, double longtitudeSpan) {

        double x = (centroidLatitude - latitudeSpan / 2) + RANDOM.nextDouble() * latitudeSpan;
        double y = (centroidLongtitude - longtitudeSpan / 2) + RANDOM.nextDouble() * longtitudeSpan;

        List<Integer> keywords = new ArrayList<>();

        for (int k = 0; k < numberOfKeywords; k++) {
            int keyword = keywordSpaceMiddle + RANDOM.nextInt(keywordSpaceSpan);
            //System.out.println("Keyword GENERATED: " + keyword);
            keywords.add(keyword);
        }

        Query query = new Query(queryId,new Point(new double[]{x, y}), keywords);

        return query;
    }
}
