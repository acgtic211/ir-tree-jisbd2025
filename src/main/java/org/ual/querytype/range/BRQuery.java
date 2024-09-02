package org.ual.querytype.range;

import org.ual.query.Query;

public class BRQuery extends RangeQuery {
    public BRQuery(Query query) {
        super(query);
    }

    @Override
    public String toString() {
        return "BRQuery{" +
                "query=" + query +
                ", minDistance=" + minDistance +
                '}';
    }

    public static class Result extends RangeQuery.Result {
        public Result(int id, Double minDistance) {
            super(id, minDistance);
        }

        @Override
        public int compareTo(RangeQuery.Result other) {
            return super.compareTo(other);
        }
    }
}
