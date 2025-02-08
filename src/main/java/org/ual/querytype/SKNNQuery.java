package org.ual.querytype;

import org.ual.query.Query;
import org.ual.query.QueryResult;
import org.ual.spatialindex.spatialindex.Point;

import java.util.List;

public class SKNNQuery extends Query {
    public Query query;

    public SKNNQuery(int id) {
        super(id);
    }

    public SKNNQuery(int id, double weight, Point location, List<Integer> keywords, List<Double> keywordWeights) {
        super(id, weight, location, keywords, keywordWeights);
    }

    public SKNNQuery(int id, Point location, List<Integer> keywords) {
        super(id, location, keywords);
    }

    // GNNKQuery
    public void setKNNKQuery(Query query) {
        this.query = query;
    }


    @Override
    public String toString() {
        return "SKNNQueryNew{" +
                "id=" + id +
                ", weight=" + weight +
                ", location=" + location +
                ", keywords=" + keywords +
                ", keywordWeights=" + keywordWeights +
                '}';
    }

    public static class Result extends QueryResult implements Comparable<Result> {
        public double minDistance;
        public double cost;

        public Result(int id, double minDistance) {
            super(id);
            this.minDistance = minDistance;
        }

        public Result(int id, double cost, double minDistance) {
            super(id);
            this.cost = cost;
            this.minDistance = minDistance;
        }

//        public Result(int id, Cost cost, int minDistance) {
//            super(id, cost, minDistance);
//        }
//
//        public Result(int id, double cost, double minDistance) {
//            super(id, cost, minDistance);
//        }

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

        @Override
        public String toString() {
            return "Result{" +
                    "id=" + id +
                    ", minDistance=" + minDistance +
                    ", cost=" + cost +
                    '}';
        }
    }
}
