package org.ual.querytype.knn;

import org.ual.query.Query;

public class BooleanKnnQuery extends KNNQuery {
    public BooleanKnnQuery(Query query) {
        super(query);
    }

    @Override
    public String toString() {
        return "BooleanKnnQuery{" +
                "query=" + query +
                '}';
    }

    public static class Result extends KNNQuery.Result {
        public Result(int id, Double cost, double minDistance) {
            super(id, cost, minDistance);
        }

        public Result(int id, double minDistance) {
            super(id, minDistance);
        }
    }
}
