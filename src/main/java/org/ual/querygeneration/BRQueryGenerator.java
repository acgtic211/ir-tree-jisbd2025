package org.ual.querygeneration;

import org.ual.query.Query;
import org.ual.querytype.aggregate.GNNKQuery;
import org.ual.querytype.range.BRQuery;
import org.ual.spatialindex.parameters.Parameters;

import java.util.ArrayList;
import java.util.List;

public class BRQueryGenerator extends QueryGenerator {

    public static List<BRQuery> generateBRQueries(int numberOfQueries, int numberOfKeywords, double querySpaceAreaPercentage,
                                                  double keywordSpaceSizePercentage) {
        resetRandom();
        List<BRQuery> brQueries = new ArrayList<>();

        // Generate QueryID, Point(X, Y) and List<Int> Keywords
        for (int queryId = 0; queryId < numberOfQueries; queryId++) {
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

            Query query = createTopKQuery(queryId, numberOfKeywords, keywordSpaceMiddle, keywordSpaceSpan,
                    centroidLatitude, centroidLongtitude, latitudeSpan, longtitudeSpan);
            //TODO DELETE
            query.keywords.clear();
            query.keywords.add(1);

            brQueries.add(new BRQuery(query));
        }
        for(BRQuery q : brQueries) System.out.println(q.query.location);
        return brQueries;
    }
}
