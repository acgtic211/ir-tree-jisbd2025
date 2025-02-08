package org.ual.querytype;

import org.ual.algorithm.aggregator.IAggregator;
import org.ual.query.Query;
import org.ual.query.QueryResult;
import java.util.ArrayList;
import java.util.List;

public class AggregateSKNNQuery extends Query {
    public List<Query> queries;
    public IAggregator aggregator;
    public int groupSize;
    public int subGroupSize;



    public AggregateSKNNQuery(int id) {
        super(id);
    }

    public AggregateSKNNQuery(int id, List<Query> queries, int groupSize, IAggregator aggregator) {
        super(id);
        this.queries = queries;
        this.aggregator = aggregator;
        this.groupSize = groupSize;
    }

    public AggregateSKNNQuery(int id, int groupSize, IAggregator aggregator) {
        super(id);
        this.groupSize = groupSize;
        this.aggregator = aggregator;
    }

    // SGNNKQuery
    public void setSGNNKQuery(List<Query> queries, int groupSize, int subGroupSize, IAggregator aggregator) {
        this.queries = queries;
        this.aggregator = aggregator;
        this.groupSize = groupSize;
        this.subGroupSize = subGroupSize;

        assert subGroupSize <= queries.size() :
                "Sub-group size must be less then the number of queries";
    }

    // GNNKQuery
    public void setGNNKQuery(List<Query> queries, int groupSize, IAggregator aggregator) {
        this.queries = queries;
        this.aggregator = aggregator;
        this.groupSize = groupSize;
    }



    public List<Double> getWeights() {
        List<Double> weights = new ArrayList<>();
        for (Query query : queries) {
            weights.add(query.weight);
        }

        return weights;
    }

    @Override
    public String toString() {//TODO ADD Query toString
        return "AggregateSKNNQueryNew{" +
                "id=" + id +
                ", weight=" + weight +
                ", location=" + location +
                ", keywords=" + keywords +
                ", keywordWeights=" + keywordWeights +
                '}';
    }



    public static class Result extends QueryResult implements Comparable<Result>{
        public List<Integer> queryIds;
        public Cost aggregateCost;

//        public Result(int id, int minDistance) {
//            super(id, minDistance);
//        }
//
//        public Result(int id, Cost cost, int minDistance) {
//            super(id, cost, minDistance);
//        }

//        public Result(int id, Cost cost) {
//            super(id, cost);
//        }

        public Result(int id, Cost aggregateCost) {
            super(id);
            this.aggregateCost = aggregateCost;
        }

        public Result(int id, Cost aggregateCost, List<Integer> minimumCostQueryIds) {
            super(id);
            this.aggregateCost = aggregateCost;
            this.queryIds = minimumCostQueryIds;
        }

        @Override
        public int compareTo(Result other) {
            if (this.aggregateCost.totalCost < other.aggregateCost.totalCost)
                return -1;
            else if (this.aggregateCost.totalCost > other.aggregateCost.totalCost)
                return 1;
            else {
                if (this.id < other.id)
                    return -1;
                else if (this.id > other.id)
                    return 1;
                return 0;
            }
        }
    }

}
