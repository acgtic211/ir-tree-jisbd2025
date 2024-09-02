package org.ual.querytype.knn;

import org.ual.query.Query;
import org.ual.querytype.Cost;

import java.util.List;

public class TopkKnnQuery extends KNNQuery {
    public TopkKnnQuery(Query query) {
        super(query);
    }

    @Override
    public String toString() {
        return "TopkKnnQuery{" +
                "query=" + query +
                '}';
    }

    public static class Result extends KNNQuery.Result {
        public Result(int id, double minDistance) {
            super(id, minDistance);
        }

        public Result(int id, Double cost, double minDistance) {
            super(id, cost, minDistance);
        }
    }
}
