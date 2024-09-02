package org.ual.querytype.aggregate;

import org.ual.algorithm.aggregator.IAggregator;
import org.ual.query.Query;
import org.ual.querytype.Cost;

import java.util.List;

public class GNNKQuery extends AggregateQuery {

    public GNNKQuery(List<Query> queries, IAggregator aggregator) {
        super(queries, aggregator);
    }

    @Override
    public String toString() {
        return "GNNKQuery [" + aggregator.getName()
                + ", queries=" + queries + "]";
    }

    public static class Result extends AggregateQuery.Result {

        public Result(int id, Cost cost) {
            super(id, cost);
        }
    }

}
