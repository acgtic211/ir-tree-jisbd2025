package org.ual.querygeneration;

import org.ual.algorithm.aggregator.AggregatorFactory;
import org.ual.algorithm.aggregator.IAggregator;
import org.ual.query.Query;
import org.ual.querytype.aggregate.GNNKQuery;
import org.ual.spatialindex.parameters.Parameters;

import java.util.ArrayList;
import java.util.List;

public class GNNKQueryGenerator extends QueryGenerator {

    public static List<GNNKQuery> generateGNNKQuery(int numberOfGNNKQueries,
                                                    int groupSize, int numberOfKeywords, double querySpaceAreaPercentage,
                                                    double keywordSpaceSizePercentage, String aggregatorName) {
        resetRandom();
        List<GNNKQuery> gnnkQueries = new ArrayList<>();

        // Calculate QueryID, Weight, X, Y, List<Int> Keywords, List<Double> KeyWeights
        for (int gnnk = 0; gnnk < numberOfGNNKQueries; gnnk++) {
            // Aggregator name
            // Group size

            double latitudeSpan = (Parameters.latitudeEnd - Parameters.latitudeStart)
                    * Math.sqrt(querySpaceAreaPercentage / 100);
            double longtitudeSpan = (Parameters.longitudeEnd - Parameters.longitudeStart)
                    * Math.sqrt(querySpaceAreaPercentage / 100);

            double centroidLatitude = Parameters.latitudeStart +
                    RANDOM.nextDouble() * (Parameters.latitudeEnd - Parameters.latitudeStart);
            double centroidLongtitude = Parameters.longitudeStart +
                    RANDOM.nextDouble() * (Parameters.longitudeEnd - Parameters.longitudeStart);

            int keywordSpaceSpan = (int) (Parameters.uniqueKeywords * keywordSpaceSizePercentage / 100);
            int keywordSpaceMiddle = RANDOM.nextInt(Parameters.uniqueKeywords - keywordSpaceSpan + 1);

            List<Query> queries = new ArrayList<>();

            int[] queryWeights = new int[groupSize];
            double queryWeightSum = 0;

            for (int i = 0; i < queryWeights.length; i++) {
                queryWeights[i] = 1; //+ RANDOM.nextInt(Integer.MAX_VALUE / numberOfQueries);
                queryWeightSum += queryWeights[i];
            }

            for (int i = 0; i < queryWeights.length; i++) {
                double queryWeight = queryWeights[i] / queryWeightSum * groupSize;

                // TODO FIX RANDOMIZER DISCREPANCY
                queries.add(createKWQuery(i, queryWeight, numberOfKeywords, keywordSpaceMiddle, keywordSpaceSpan,
                        centroidLatitude, centroidLongtitude, latitudeSpan, longtitudeSpan));
            }

            IAggregator aggregator = AggregatorFactory.getAggregator(aggregatorName);

            GNNKQuery gnnkQuery = new GNNKQuery(queries, aggregator);
            gnnkQueries.add(gnnkQuery);
        }
        for(GNNKQuery q : gnnkQueries) System.out.println(q.queries.get(0).location);
        return gnnkQueries;

    }


}
