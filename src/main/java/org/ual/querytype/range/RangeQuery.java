package org.ual.querytype.range;

import org.ual.query.Query;

public class RangeQuery {
    public Query query;
    public double minDistance;

    public RangeQuery(Query query) {
        this.query = query;
    }

    public static class Result implements Comparable<Result> {
        /**
         * ID of the data object
         */
        public int id;
        public double minDistance;

        public Result(int id, double minDistance) {
            this.id = id;
            this.minDistance = minDistance;
        }

        @Override
        public int compareTo(Result other) {
            if (this.minDistance < other.minDistance)
                return -1;
            else if (this.minDistance > other.minDistance)
                return 1;
            else {
                if (this.id < other.id)
                    return -1;
                else if (this.id > other.id)
                    return 1;
                return 0;
            }
        }

        @Override
        public String toString() {
            return "Result [id=" + id + ", minDist=" + minDistance + "]";
        }

    }
}
