package org.ual.querygeneration;

import org.ual.query.Query;
import org.ual.querytype.SKNNQuery;
import org.ual.spatialindex.parameters.DatasetParameters;
import org.ual.spatialindex.spatialindex.Point;

import java.util.ArrayList;
import java.util.List;

public class SKNNQueryGenerator extends QueryGenerator {

    public SKNNQueryGenerator(int seed, DatasetParameters parameters) {
        super(seed, parameters);
    }

    public List<SKNNQuery> generateBooleanKNNQueries(int numQueries, int numKeywords, double querySpaceAreaPercentage, double keywordSpaceSizePercentage) {
        resetRandomSeed();
        List<SKNNQuery> bkQueries = new ArrayList<>();

        for (int queryId = 0; queryId < numQueries; queryId++) {
            double latitudeSpan = (parameters.latitudeEnd - parameters.latitudeStart) * Math.sqrt(querySpaceAreaPercentage / 100);
            double longtitudeSpan = (parameters.longitudeEnd - parameters.longitudeStart) * Math.sqrt(querySpaceAreaPercentage / 100);

            double centroidLatitude = parameters.latitudeStart + RANDOM.nextDouble() * (parameters.latitudeEnd - parameters.latitudeStart);
            double centroidLongtitude = parameters.longitudeStart + RANDOM.nextDouble() * (parameters.longitudeEnd - parameters.longitudeStart);

            int keywordSpaceSpan = (int) Math.ceil(parameters.uniqueKeywords * keywordSpaceSizePercentage / 100.0);
            int keywordSpaceMiddle = RANDOM.nextInt(parameters.uniqueKeywords - keywordSpaceSpan + 1);

            // TODO: Change creation method
            Query query = createTopKQuery(queryId, numKeywords, keywordSpaceMiddle, keywordSpaceSpan, centroidLatitude,
                    centroidLongtitude, latitudeSpan, longtitudeSpan, parameters.topkWords);
            // TESTING
//            if(queryId < 5) {
//                query.keywords.clear();
//                query.keywords.add(1);
//                //query.location = new Point(new double[]{106, -6});
//            }
//            query.keywords.clear();
//            query.keywords.add(1);
            //System.out.println("Query: " + query.toString());

            SKNNQuery bkQuery = new SKNNQuery(queryId);
            bkQuery.setKNNKQuery(query);
            bkQueries.add(bkQuery);
        }

        return bkQueries;
    }

    public List<SKNNQuery> generateTopKNNQueries(int numQueries, int numberOfKeywords, double querySpaceAreaPercentage, double keywordSpaceSizePercentage) {
        resetRandomSeed();
        List<SKNNQuery> tkQueries = new ArrayList<>();

        // Generate QueryID, Point(X, Y) and List<Int> Keywords
        for (int queryId = 0; queryId < numQueries; queryId++) {
            double latitudeSpan = (parameters.latitudeEnd - parameters.latitudeStart) * Math.sqrt(querySpaceAreaPercentage / 100);
            double longitudeSpan = (parameters.longitudeEnd - parameters.longitudeStart) * Math.sqrt(querySpaceAreaPercentage / 100);

            double centroidLatitude = parameters.latitudeStart + RANDOM.nextDouble() * (parameters.latitudeEnd - parameters.latitudeStart);
            double centroidLongitude = parameters.longitudeStart + RANDOM.nextDouble() * (parameters.longitudeEnd - parameters.longitudeStart);

            int keywordSpaceSpan = (int) Math.ceil(parameters.uniqueKeywords * keywordSpaceSizePercentage / 100.0);   // Added Math.ceil to fix rounding issue to 0
            int keywordSpaceMiddle = RANDOM.nextInt(parameters.uniqueKeywords - keywordSpaceSpan + 1);

            //double queryWeight = 1.0;
            // TODO: Check creation method
            //Query query = createKWQuery(queryId, queryWeight, numberOfKeywords, keywordSpaceMiddle, keywordSpaceSpan, centroidLatitude, centroidLongitude, latitudeSpan, longitudeSpan);
            Query query = createTopKQuery(queryId, numberOfKeywords, keywordSpaceMiddle, keywordSpaceSpan, centroidLatitude,
                    centroidLongitude, latitudeSpan, longitudeSpan, parameters.topkWords);

            // Test
//            query.keywords.clear();
//            query.keywords.add(1);

            SKNNQuery tkQuery = new SKNNQuery(queryId);
            tkQuery.setKNNKQuery(query);
            tkQueries.add(tkQuery);
        }

        return tkQueries;
    }

    public List<SKNNQuery> generateBooleanRangeQueries(int numQueries, int numberOfKeywords, double querySpaceAreaPercentage, double keywordSpaceSizePercentage) {
        resetRandomSeed();
        List<SKNNQuery> brQueries = new ArrayList<>();

        // Generate QueryID, Point(X, Y) and List<Int> Keywords
        for (int queryId = 0; queryId < numQueries; queryId++) {
            double latitudeSpan = (parameters.latitudeEnd - parameters.latitudeStart) * Math.sqrt(querySpaceAreaPercentage / 100.0);
            double longitudeSpan = (parameters.longitudeEnd - parameters.longitudeStart) * Math.sqrt(querySpaceAreaPercentage / 100.0);

            double centroidLatitude = parameters.latitudeStart + RANDOM.nextDouble() * (parameters.latitudeEnd - parameters.latitudeStart);
            double centroidLongitude = parameters.longitudeStart + RANDOM.nextDouble() * (parameters.longitudeEnd - parameters.longitudeStart);

            int keywordSpaceSpan = (int) Math.ceil(parameters.uniqueKeywords * keywordSpaceSizePercentage / 100.0);
            int keywordSpaceMiddle = RANDOM.nextInt(parameters.uniqueKeywords - keywordSpaceSpan + 1);

            //double queryWeight = 1.0;
            // TODO: Check creation method
            //Query query = createKWQuery(queryId, queryWeight, numberOfKeywords, keywordSpaceMiddle, keywordSpaceSpan, centroidLatitude, centroidLongitude, latitudeSpan, longitudeSpan);
            Query query = createTopKQuery(queryId, numberOfKeywords, keywordSpaceMiddle, keywordSpaceSpan, centroidLatitude,
                    centroidLongitude, latitudeSpan, longitudeSpan, parameters.topkWords);

            SKNNQuery brQuery = new SKNNQuery(queryId);
            brQuery.setKNNKQuery(query);
            brQueries.add(brQuery);
        }

        return brQueries;
    }
}
