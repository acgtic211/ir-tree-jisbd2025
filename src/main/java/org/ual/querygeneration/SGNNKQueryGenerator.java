package org.ual.querygeneration;

import org.ual.algorithm.aggregator.AggregatorFactory;
import org.ual.algorithm.aggregator.IAggregator;
import org.ual.query.Query;
import org.ual.querytype.aggregate.SGNNKQuery;
import org.ual.spatialindex.parameters.Parameters;

import java.util.ArrayList;
import java.util.List;

public class SGNNKQueryGenerator extends QueryGenerator {

    public static List<SGNNKQuery> generateSGNNKQuery(int numberOfSGNNKQueries, int groupSize, double subgroupSize,
                                                      int numberOfKeywords, double querySpaceAreaPercentage,
                                                      double keywordSpaceSizePercentage, String aggregatorName) {
        resetRandom();
        List<SGNNKQuery> sgnnkQueries = new ArrayList<>();
        int subGroup = (int) (groupSize * subgroupSize / 100);

        // Calculate QueryID, Weight, X, Y, List<Int> Keywords, List<Double> KeyWeights
        for (int sgnnk = 0; sgnnk < numberOfSGNNKQueries; sgnnk++) {

            double latitudeSpan = (Parameters.latitudeEnd - Parameters.latitudeStart)
                    * Math.sqrt(querySpaceAreaPercentage / 100);
            double longitudeSpan = (Parameters.longitudeEnd - Parameters.longitudeStart)
                    * Math.sqrt(querySpaceAreaPercentage / 100);

            double centroidLatitude = Parameters.latitudeStart +
                    RANDOM.nextDouble() * (Parameters.latitudeEnd - Parameters.latitudeStart);
            double centroidLongitude = Parameters.longitudeStart +
                    RANDOM.nextDouble() * (Parameters.longitudeEnd - Parameters.longitudeStart);

            int keywordSpaceSpan = (int) (Parameters.uniqueKeywords * keywordSpaceSizePercentage / 100);
            int keywordSpaceMiddle = RANDOM.nextInt(Parameters.uniqueKeywords - keywordSpaceSpan + 1);

            // Query writer
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
                        centroidLatitude, centroidLongitude, latitudeSpan, longitudeSpan));
            }

            IAggregator aggregator = AggregatorFactory.getAggregator(aggregatorName);
            SGNNKQuery sgnnkQuery = new SGNNKQuery(queries, subGroup, aggregator);

            sgnnkQueries.add(sgnnkQuery);
        }

        return sgnnkQueries;
    }
}
