package org.ual.querytype.knn;

import org.ual.query.Query;
import java.util.List;

public class KNNQuery {
    public Query query;

    public KNNQuery(Query query) {
        this.query = query;
    }

    public static class Result implements Comparable<Result> {
        /**
         * ID of the data object
         */
        public int id;
        public Double cost;
        public double minDistance;

        public Result(int id, double minDistance) {
            this.id = id;
            this.minDistance = minDistance;
        }

        public Result(int id, Double cost, double minDistance) {
            this.id = id;
            this.cost = cost;
            this.minDistance = minDistance;
        }

        @Override
        public int compareTo(Result o) {
            if (this.minDistance < o.minDistance)
                return -1;
            else if (this.minDistance > o.minDistance)
                return 1;
            else {
                if (this.id < o.id)
                    return -1;
                else if (this.id > o.id)
                    return 1;
                return 0;
            }
        }

//        @Override
//        public int compareTo(Result other) {
//            if (this.cost < other.cost)
//                return -1;
//            else if (this.cost > other.cost)
//                return 1;
//            else {
//                if (this.id < other.id)
//                    return -1;
//                else if (this.id > other.id)
//                    return 1;
//                return 0;
//            }
//        }

        @Override
        public String toString() {
            return "Result [id=" + id + ", cost=" + cost + "]";
        }

    }
}
