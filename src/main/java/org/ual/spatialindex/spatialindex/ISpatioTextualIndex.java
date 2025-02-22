package org.ual.spatialindex.spatialindex;

import org.ual.documentindex.InvertedFile;
import org.ual.querytype.AggregateSKNNQuery;
import org.ual.querytype.SKNNQuery;
//import org.ual.querytype.knn.BooleanKnnQuery;
//import org.ual.querytype.knn.TopkKnnQuery;
//import org.ual.querytype.range.BRQuery;
//import org.ual.querytype.aggregate.GNNKQuery;
//import org.ual.querytype.aggregate.SGNNKQuery;

import java.util.List;
import java.util.Map;

public interface ISpatioTextualIndex {
//    List<GNNKQuery.Result> gnnkBaseline(InvertedFile invertedFile, GNNKQuery gnnkQuery, int topk);
//    List<SGNNKQuery.Result> sgnnkBaseline(InvertedFile invertedFile, SGNNKQuery sgnnkQuery, int topk);
//    List<GNNKQuery.Result> gnnk(InvertedFile invertedFile, GNNKQuery gnnkQuery, int topk);
//    List<SGNNKQuery.Result> sgnnk(InvertedFile invertedFile, SGNNKQuery sgnnkQuery, int topk);
//    Map<Integer, List<SGNNKQuery.Result>> sgnnkExtended(InvertedFile invertedFile, SGNNKQuery sgnnkQuery, int topk);

    List<AggregateSKNNQuery.Result> gnnkBaseline(InvertedFile invertedFile, AggregateSKNNQuery gnnkQuery, int topk);
    List<AggregateSKNNQuery.Result> sgnnkBaseline(InvertedFile invertedFile, AggregateSKNNQuery sgnnkQuery, int topk);
    List<AggregateSKNNQuery.Result> gnnk(InvertedFile invertedFile, AggregateSKNNQuery gnnkQuery, int topk);
    List<AggregateSKNNQuery.Result> sgnnk(InvertedFile invertedFile, AggregateSKNNQuery sgnnkQuery, int topk);
    Map<Integer, List<AggregateSKNNQuery.Result>> sgnnkExtended(InvertedFile invertedFile, AggregateSKNNQuery sgnnkQuery, int topk);

//    List<BRQuery.Result> booleanRangeQuery(InvertedFile invertedFile, BRQuery q, float radius);
//    List<BooleanKnnQuery.Result> booleanKnnQuery(InvertedFile invertedFile, BooleanKnnQuery query, int topk);
//    List<TopkKnnQuery.Result> topkKnnQuery(InvertedFile invertedFile, TopkKnnQuery q, int topk);

    List<SKNNQuery.Result> booleanRangeQuery(InvertedFile invertedFile, SKNNQuery query, float radius);
    List<SKNNQuery.Result> booleanKnnQuery(InvertedFile invertedFile, SKNNQuery query, int topk);
    List<SKNNQuery.Result> topkKnnQuery(InvertedFile invertedFile, SKNNQuery query, int topk);

    int getIO();
    int getVisitedNodes();
}
