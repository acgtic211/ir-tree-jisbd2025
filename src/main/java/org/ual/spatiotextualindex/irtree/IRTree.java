package org.ual.spatiotextualindex.irtree;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ual.documentindex.InvertedFile;
import org.ual.query.Query;
import org.ual.querytype.*;
import org.ual.querytype.aggregate.AggregateQuery;
import org.ual.querytype.aggregate.GNNKQuery;
import org.ual.querytype.aggregate.SGNNKQuery;
import org.ual.querytype.knn.BooleanKnnQuery;
import org.ual.querytype.knn.TopkKnnQuery;
import org.ual.querytype.range.BRQuery;
import org.ual.spatialindex.rtree.Node;
import org.ual.spatialindex.rtree.RTree;
import org.ual.spatialindex.spatialindex.*;
import org.ual.spatialindex.storagemanager.IStorageManager;
import org.ual.spatialindex.storagemanager.PropertySet;

import java.util.*;

public class IRTree extends RTree implements ISpatioTextualIndex {

    private static final Logger log = LogManager.getLogger(IRTree.class);

    public IRTree(PropertySet propertySet, IStorageManager storageManager) {
        super(propertySet, storageManager);
    }

    // EXTENDED
    public IRTree(RTree rtree) {
        super(rtree);
    }


    public List<GNNKQuery.Result> gnnkBaseline(InvertedFile invertedFile, GNNKQuery gnnkQuery, int topk) {
        LinkedList<NNEntryExtended> list = new LinkedList<>();
        NNEntryExtended root = new NNEntryExtended(new RtreeEntry(rootID, false), new Cost(0, 0, 0));
        list.add(root);

        // Current (at most) k best objects, sorted according to their decreasing value of cost.
        // So the highest cost object will always be on top.
        PriorityQueue<GNNKQuery.Result> currentBestObjects =
                new PriorityQueue<>(topk, new WorstFirstNNEntryComparator());
        // Dummy objects
        for (int i = 0; i < topk; i++) {
            currentBestObjects.add(new GNNKQuery.Result(-1, new Cost(0, 0, Double.MAX_VALUE)));
        }

        // Cost of the highest valued node of current best objects
        double costBound = Double.MAX_VALUE;

        while (!list.isEmpty()) {
            NNEntryExtended first = list.poll();
            RtreeEntry rTreeEntry = (RtreeEntry) first.entry;

            if (first.cost.totalCost > costBound)
                continue;

            Node n = readNode(rTreeEntry.getIdentifier());
            numOfVisitedNodes++;

            HashMap<Integer, List<Cost>> costs = calculateQueryCosts(invertedFile, gnnkQuery.queries, n);

            // Individual query costs are calculated, now calculate aggregate query cost
            for (int child = 0; child < n.children; child++) {
                List<Cost> queryCosts = costs.get(child);
                Cost aggregateCost = gnnkQuery.aggregator.getAggregateValue(queryCosts, gnnkQuery.getWeights());

                int childId = n.identifiers[child];
                if (n.level == 0) {
                    currentBestObjects.add(new GNNKQuery.Result(childId, aggregateCost));
                    currentBestObjects.poll();
                    assert currentBestObjects.peek() != null;
                    costBound = currentBestObjects.peek().cost.totalCost;
                } else {
                    rTreeEntry = new RtreeEntry(childId, false);
                    NNEntryExtended entry = new NNEntryExtended(rTreeEntry, aggregateCost);
                    list.addFirst(entry);
                }
            }
        }

        List<GNNKQuery.Result> results = new ArrayList<>(currentBestObjects);
        Collections.sort(results);
        return results;
    }

    /**
     * @return A list of objects with size at most k,
     * where objects are sorted according to the decreasing value of their costs.
     */
    public List<SGNNKQuery.Result> sgnnkBaseline(InvertedFile invertedFile, SGNNKQuery sgnnkQuery, int topk) {
        LinkedList<NNEntryExtended> list = new LinkedList<>();
        NNEntryExtended root = new NNEntryExtended(new RtreeEntry(rootID, false), new Cost(0, 0, 0));
        list.add(root);

        // Current (at most) k best objects, sorted according to their decreasing value of cost.
        // So the highest cost object will always be on top.
        PriorityQueue<SGNNKQuery.Result> currentBestObjects =
                new PriorityQueue<>(topk, new WorstFirstNNEntryComparator());
        // Dummy objects
        for (int i = 0; i < topk; i++) {
            currentBestObjects.add(new SGNNKQuery.Result(-1, new Cost(0, 0, Double.MAX_VALUE), null));
        }
        double costBound = Double.MAX_VALUE;

        while (list.size() != 0) {
            NNEntryExtended first = list.poll();
            if (first.cost.totalCost > costBound)
                continue;

            RtreeEntry rTreeEntry = (RtreeEntry) first.entry;
            Node n = readNode(rTreeEntry.getIdentifier());

            numOfVisitedNodes++;

            HashMap<Integer, List<Cost>> costs = calculateQueryCosts(invertedFile, sgnnkQuery.queries, n);

            // Individual query costs are calculated, now calculate aggregate query cost
            for (int child = 0; child < n.children; child++) {
                final List<Cost> queryCosts = costs.get(child);

                List<Integer> minimumCostQueryIndices = new ArrayList<>();
                for (int queryIndex = 0; queryIndex < queryCosts.size(); queryIndex++) {
                    minimumCostQueryIndices.add(queryIndex);
                }
                // Sort query indices according to increasing order of query cost
                Collections.sort(minimumCostQueryIndices, (i1, i2) -> {
                    if (queryCosts.get(i1).totalCost < queryCosts.get(i2).totalCost) return -1;
                    else if (queryCosts.get(i1).totalCost > queryCosts.get(i2).totalCost) return 1;
                    return 0;
                });

                // Now choose first m queries with lowest cost
                List<Cost> minimumQueryCosts = new ArrayList<>();
                List<Integer> minimumCostQueryIds = new ArrayList<>();
                for (int i = 0; i < sgnnkQuery.subGroupSize; i++) {
                    int queryIndex = minimumCostQueryIndices.get(i);
                    minimumQueryCosts.add(queryCosts.get(queryIndex));
                    minimumCostQueryIds.add(sgnnkQuery.queries.get(queryIndex).id);
                }

                minimumQueryCosts = minimumQueryCosts.subList(0, sgnnkQuery.subGroupSize);
                minimumCostQueryIndices = minimumCostQueryIndices.subList(0, sgnnkQuery.subGroupSize);
                List<Double> minimumQueryWeights = new ArrayList<>();
                for (Integer queryIndex : minimumCostQueryIndices) {
                    minimumQueryWeights.add(sgnnkQuery.queries.get(queryIndex).weight);
                }

                Cost aggregateCost = sgnnkQuery.aggregator.getAggregateValue(minimumQueryCosts, minimumQueryWeights);
                int childId = n.identifiers[child];

                if (n.level == 0) {
                    currentBestObjects.add(new SGNNKQuery.Result(childId, aggregateCost, minimumCostQueryIds));
                    currentBestObjects.poll();
                    costBound = currentBestObjects.peek().cost.totalCost;
                } else {
                    rTreeEntry = new RtreeEntry(childId, false);
                    list.addFirst(new NNEntryExtended(rTreeEntry, minimumCostQueryIds, aggregateCost));
                }

            }
        }

        List<SGNNKQuery.Result> results = new ArrayList<>(currentBestObjects);
        Collections.sort(results);
        return results;
    }


    /**
     * @return A list of objects with size at most k,
     * where objects are sorted according to the decreasing value of their costs.
     */
    public List<GNNKQuery.Result> gnnk(InvertedFile invertedFile, GNNKQuery gnnkQuery, int topk) {
        PriorityQueue<NNEntryExtended> queue = new PriorityQueue<>();
        NNEntryExtended root = new NNEntryExtended(new RtreeEntry(rootID, false), new Cost(0, 0, 0));
        queue.add(root);

        List<GNNKQuery.Result> results = new ArrayList<>();

        while (!queue.isEmpty() && results.size() < topk) {
            NNEntryExtended first = queue.poll();
            RtreeEntry rTreeEntry = (RtreeEntry) first.entry;

            if (rTreeEntry.isLeafEntry) {
                results.add(new GNNKQuery.Result(first.entry.getIdentifier(), first.cost));
            } else {
                Node n = readNode(rTreeEntry.getIdentifier());

                numOfVisitedNodes++;

                HashMap<Integer, List<Cost>> costs = calculateQueryCosts(invertedFile, gnnkQuery.queries, n);

                // Individual query costs are calculated, now calculate aggregate query cost
                for (int child = 0; child < n.children; child++) {
                    List<Cost> queryCosts = costs.get(child);
                    Cost aggregateCost = gnnkQuery.aggregator.getAggregateValue(queryCosts, gnnkQuery.getWeights());

                    //log.debug("Node Type: {}, Level: {}", n.type, n.level);
                    if (n.level == 0) {
                        rTreeEntry = new RtreeEntry(n.identifiers[child], true);
                    } else {
                        rTreeEntry = new RtreeEntry(n.identifiers[child], false);
                    }

                    queue.add(new NNEntryExtended(rTreeEntry, aggregateCost));
                }
            }
        }

        Collections.sort(results);
        return results;
    }

    /**
     * @return A list of objects with size at most k,
     * where objects are sorted according to the decreasing value of their costs.
     */
    public List<SGNNKQuery.Result> sgnnk(InvertedFile invertedFile, SGNNKQuery sgnnkQuery, int topk) {
        PriorityQueue<NNEntryExtended> queue = new PriorityQueue<>();
        NNEntryExtended root = new NNEntryExtended(new RtreeEntry(rootID, false), new Cost(0, 0, 0));
        queue.add(root);

        List<SGNNKQuery.Result> results = new ArrayList<>();

        while (queue.size() != 0 && results.size() < topk) {
            NNEntryExtended first = queue.poll();
            RtreeEntry rTreeEntry = (RtreeEntry) first.entry;

            if (rTreeEntry.isLeafEntry) {
                results.add(new SGNNKQuery.Result(first.entry.getIdentifier(), first.cost, first.queryIndices));
            } else {
                Node n = readNode(rTreeEntry.getIdentifier());
                numOfVisitedNodes++;

//				if (!levelVsVisitedNodes.containsKey(n.level))
//					levelVsVisitedNodes.put(n.level, new ArrayList<AggregateQuery.Result>());
//				levelVsVisitedNodes.get(n.level).add(new AggregateQuery.Result(n.identifier, first.cost));

                HashMap<Integer, List<Cost>> costs = calculateQueryCosts(invertedFile, sgnnkQuery.queries, n);

                // Individual query costs are calculated, now calculate aggregate query cost
                for (int child = 0; child < n.children; child++) {
                    final List<Cost> queryCosts = costs.get(child);

                    List<Integer> minimumCostQueryIndices = new ArrayList<>();
                    for (int queryIndex = 0; queryIndex < queryCosts.size(); queryIndex++) {
                        minimumCostQueryIndices.add(queryIndex);
                    }
                    // Sort query indices according to increasing order of query cost
                    Collections.sort(minimumCostQueryIndices, (i1, i2) -> {
                        if (queryCosts.get(i1).totalCost < queryCosts.get(i2).totalCost) return -1;
                        else if (queryCosts.get(i1).totalCost > queryCosts.get(i2).totalCost) return 1;
                        return 0;
                    });

                    // Now choose first m queries with lowest cost
                    List<Cost> minimumQueryCosts = new ArrayList<>();
                    List<Integer> minimumCostQueryIds = new ArrayList<>();
                    for (int i = 0; i < sgnnkQuery.subGroupSize; i++) {
                        Integer queryIndex = minimumCostQueryIndices.get(i);
                        minimumQueryCosts.add(queryCosts.get(queryIndex));
                        minimumCostQueryIds.add(sgnnkQuery.queries.get(queryIndex).id);
                    }

                    minimumQueryCosts = minimumQueryCosts.subList(0, sgnnkQuery.subGroupSize);
                    minimumCostQueryIndices = minimumCostQueryIndices.subList(0, sgnnkQuery.subGroupSize);

                    List<Double> minimumQueryWeights = new ArrayList<>();
                    for (Integer queryIndex : minimumCostQueryIndices) {
                        minimumQueryWeights.add(sgnnkQuery.queries.get(queryIndex).weight);
                    }

                    Cost aggregateCost = sgnnkQuery.aggregator.getAggregateValue(minimumQueryCosts, minimumQueryWeights);

                    if (n.level == 0) {
                        rTreeEntry = new RtreeEntry(n.identifiers[child], true);
                    } else {
                        rTreeEntry = new RtreeEntry(n.identifiers[child], false);
                    }

                    NNEntryExtended entry = new NNEntryExtended(rTreeEntry, minimumCostQueryIds, aggregateCost);
//					System.out.println(entry);
                    queue.add(entry);
                }
            }
        }

        Collections.sort(results);
        return results;
    }

    /**
     * @return A list of objects with size at most k,
     * where objects are sorted according to the decreasing value of their costs.
     */
    public Map<Integer, List<SGNNKQuery.Result>> sgnnkExtended(InvertedFile invertedFile, SGNNKQuery sgnnkQuery, int topk) {
        PriorityQueue<NNEntryExtended> queue = new PriorityQueue<>();
        NNEntryExtended root = new NNEntryExtended(new RtreeEntry(rootID, false), new Cost(0, 0, 0), null);
        queue.add(root);

        Map<Integer, PriorityQueue<SGNNKQuery.Result>> topResults = new HashMap<>();
        for (int i = sgnnkQuery.subGroupSize; i <= sgnnkQuery.groupSize; i++) {
            PriorityQueue<SGNNKQuery.Result> bestResults = new PriorityQueue<>(topk, new WorstFirstNNEntryComparator());
            for (int j = 0; j < topk; j++) {
                bestResults.add(new SGNNKQuery.Result(-1, new Cost(0, 0, Double.MAX_VALUE), null));
            }
            topResults.put(i, bestResults);
        }

        while (queue.size() != 0) {
            NNEntryExtended first = queue.poll();
            RtreeEntry rTreeEntry = (RtreeEntry) first.entry;

            // For root, querycosts will be null
            if (first.queryCosts != null) {
                boolean pruneNode = true;
                for (int i = 0; i < first.queryCosts.size(); i++) {
                    double prunningBound = topResults.get(i + sgnnkQuery.subGroupSize).peek().cost.totalCost;
//					System.out.println(prunningBound);
                    if (first.queryCosts.get(i).totalCost < prunningBound)
                        pruneNode = false;
                }
                if (pruneNode) continue;
            }

            Node n = readNode(rTreeEntry.getIdentifier());

            numOfVisitedNodes++;

            HashMap<Integer, List<Cost>> costs = calculateQueryCosts(invertedFile, sgnnkQuery.queries, n);

            // Individual query costs are calculated, now calculate aggregate query cost
            for (int child = 0; child < n.children; child++) {
                final List<Cost> queryCosts = costs.get(child);

                List<Integer> minimumCostQueryIndices = new ArrayList<>();
                for (int queryIndex = 0; queryIndex < queryCosts.size(); queryIndex++) {
                    minimumCostQueryIndices.add(queryIndex);
                }
                // Sort query indices according to increasing order of query cost
                Collections.sort(minimumCostQueryIndices, (i1, i2) -> {
                    if (queryCosts.get(i1).totalCost < queryCosts.get(i2).totalCost) return -1;
                    else if (queryCosts.get(i1).totalCost > queryCosts.get(i2).totalCost) return 1;
                    return 0;
                });

                // Now choose first m queries with lowest cost
                List<Cost> minimumQueryCosts = new ArrayList<>();
                List<Integer> minimumCostQueryIds = new ArrayList<>();
                for (int i = 0; i < sgnnkQuery.groupSize; i++) {
                    Integer queryIndex = minimumCostQueryIndices.get(i);
                    minimumQueryCosts.add(queryCosts.get(queryIndex));
                    minimumCostQueryIds.add(sgnnkQuery.queries.get(queryIndex).id);
                }

                sgnnkQuery.aggregator.initializeAccumulator();
                for (int i = 0; i < sgnnkQuery.subGroupSize - 1; i++) {
                    int queryIndex = minimumCostQueryIndices.get(i);
                    sgnnkQuery.aggregator.accumulate(minimumQueryCosts.get(i),
                            sgnnkQuery.queries.get(queryIndex).weight);
                }

                Cost minQueryCost = null;
                boolean prune = true;
                List<Cost> aggregateQueryCosts = new ArrayList<>();

                for (int i = sgnnkQuery.subGroupSize - 1; i < sgnnkQuery.groupSize; i++) {
                    int queryIndex = minimumCostQueryIndices.get(i);
                    sgnnkQuery.aggregator.accumulate(minimumQueryCosts.get(i),
                            sgnnkQuery.queries.get(queryIndex).weight);
                    Cost queryCost = sgnnkQuery.aggregator.getAccumulatedValue();
                    aggregateQueryCosts.add(queryCost);

                    PriorityQueue<SGNNKQuery.Result> bestResults = topResults.get(i + 1);
                    List<Integer> queryIds = minimumCostQueryIds.subList(0, i + 1);
                    if (queryCost.totalCost < bestResults.peek().cost.totalCost) {
                        prune = false;
                        if (n.level == 0) {
                            bestResults.add(new SGNNKQuery.Result(n.getChildIdentifier(child), queryCost, queryIds));
                            bestResults.poll();
                        }

                        if (minQueryCost == null || minQueryCost.totalCost < queryCost.totalCost) {
                            minQueryCost = queryCost;
                        }
                    }
                }

                if (n.level > 0 && !prune) {
                    rTreeEntry = new RtreeEntry(n.identifiers[child], false);
                    queue.add(new NNEntryExtended(rTreeEntry, minQueryCost, aggregateQueryCosts));
                }
            }
        }

        Map<Integer, List<SGNNKQuery.Result>> results = new HashMap<>();
        for (Integer subgroupSize : topResults.keySet()) {
            List<SGNNKQuery.Result> result = new ArrayList<>(topResults.get(subgroupSize));
            Collections.sort(result);
            results.put(subgroupSize, result);
        }
        return results;
    }



    public List<BRQuery.Result> booleanRangeQuery(InvertedFile invertedFile, BRQuery query, float radius) {
        PriorityQueue<NNEntry> queue = new PriorityQueue<>(new NNEntryComparatorMinDistance());
        RtreeEntry rtreeEntry = new RtreeEntry(rootID, false);
        queue.add(new NNEntry(rtreeEntry, 0.0));

        List<BRQuery.Result> results = new ArrayList<>();

        // TODO (DEBUG) REMOVE topk limit (10)
        while (!queue.isEmpty() /*&& results.size() < 10*/) {
            NNEntry first = queue.poll();
            rtreeEntry = (RtreeEntry) first.node;    //TODO CHANGE NODE FOR ENTRY


            if(rtreeEntry.isLeafEntry) {
                if(radius < first.minDistance)
                    break;
                results.add(new BRQuery.Result(first.node.getIdentifier(), first.minDistance));
            } else {
                Node node = readNode(rtreeEntry.getIdentifier());

                numOfVisitedNodes++;
                HashMap<Integer, Integer> filter = invertedFile.booleanFilter(node.identifier, (ArrayList<Integer>) query.query.keywords);

                for (int child = 0; child < node.children; child++) {
                    Integer var = filter.get(node.identifiers[child]);

                    if(var == null) {
                        continue;
                    } else {
                        int hit = var;
                        if(hit < query.query.keywords.size())   //TODO Change to !=
                            continue;
                    }

                    if (node.level == 0) {
                        rtreeEntry = new RtreeEntry(node.identifiers[child], true);
                    } else {
                        rtreeEntry = new RtreeEntry(node.identifiers[child],false);
                    }

                    queue.add(new NNEntry(rtreeEntry, node.mbr[child].getMinimumDistance(query.query.location)));
                }
            }
        }

        Collections.sort(results);
        log.info("Number of BRQ results: Radius = {} - Number: {}", radius, results.size());
        return results;
    }


    public List<BooleanKnnQuery.Result> booleanKnnQuery(InvertedFile invertedFile, BooleanKnnQuery query, int topk) {
        //PriorityQueue<NNEntry> queue = new PriorityQueue(100, new NNEntryComparator());
        PriorityQueue<NNEntry> queue = new PriorityQueue<>(new NNEntryComparatorMinDistance());


        RtreeEntry rtreeEntry = new RtreeEntry(rootID, false);
        queue.add(new NNEntry(rtreeEntry, 0.0));
        int count = 0;

        List<BooleanKnnQuery.Result> results = new ArrayList<>();

        while (queue.size() != 0 && results.size() < topk) {
            NNEntry first = queue.poll();
            rtreeEntry = (RtreeEntry)first.node;

            if(rtreeEntry.isLeafEntry) {
                count++;
                results.add(new BooleanKnnQuery.Result(first.node.getIdentifier(), first.minDistance));

//                if(count >= topk)
//                    break;
            } else {
                Node node = readNode(rtreeEntry.getIdentifier());
                HashMap<Integer, Integer> filter = invertedFile.booleanFilter(node.identifier, (ArrayList<Integer>) query.query.keywords);

                numOfVisitedNodes++;

                for (int child = 0; child < node.children; child++) {
                    Integer var = filter.get(node.identifiers[child]);
                    if(var == null)
                        continue;
                    else {
                        int hit = var;
                        if(hit < query.query.keywords.size()) //TODO Change to !=
                            continue;
                    }

                    if (node.level == 0) {
                        rtreeEntry = new RtreeEntry(node.identifiers[child], true);
                    } else {
                        rtreeEntry = new RtreeEntry(node.identifiers[child],false);
                    }

                    queue.add(new NNEntry(rtreeEntry, node.mbr[child].getMinimumDistance(query.query.location)));
                }
            }
        }

        Collections.sort(results);
        return results;
    }


    public List<TopkKnnQuery.Result> topkKnnQuery(InvertedFile invertedFile, TopkKnnQuery query, int topk) {
        PriorityQueue<NNEntry> queue = new PriorityQueue<>(new NNEntryComparatorMinDistance());
        RtreeEntry rtreeEntry = new RtreeEntry(rootID, false);
        queue.add(new NNEntry(rtreeEntry, 0.0));

        int count = 0;
        double knearest = 0.0;

        List<TopkKnnQuery.Result> results = new ArrayList<>();

        while (!queue.isEmpty()) {
            NNEntry first = queue.poll();
            rtreeEntry = (RtreeEntry) first.node;

            numOfVisitedNodes++;

            if (rtreeEntry.isLeafEntry) {
                if (count >= topk || first.cost > knearest) //TODO CHANGED AND FOR OR
                    break;

                count++;
                results.add(new TopkKnnQuery.Result(rtreeEntry.getIdentifier(), first.cost, first.minDistance));
                knearest = first.cost;
            } else {
                Node node = readNode(rtreeEntry.getIdentifier());
                HashMap<Integer, Double> filter;

                invertedFile.load(node.identifier); //Important!!!

                if (numOfClusters != 0)
                    filter = invertedFile.rankingSumClusterEnhance(query.query.keywords, query.query.keywordWeights);
                else
                    filter = invertedFile.rankingSum((ArrayList<Integer>) query.query.keywords);

                for (int child = 0; child < node.children; child++) {
                    double irscore;
                    Double var = filter.get(node.identifiers[child]);
                    if (var == null)
                        continue;
                    else
                        irscore = var;

                    if (node.level == 0) {
                        rtreeEntry = new RtreeEntry(node.identifiers[child], true);
                    } else {
                        rtreeEntry = new RtreeEntry(node.identifiers[child], false);
                    }
                    double mind = combinedScore(node.mbr[child].getMinimumDistance(query.query.location), irscore);

                    queue.add(new NNEntry(rtreeEntry, mind));

                }
            }
        }
        Collections.sort(results);
        return results;
    }

    /**
     * For each child node, calculate the cost for all queries.
     * The first parameter of the result map is the index of the child node,
     * second parameter is the corresponding list of costs calculated for individual queries.
     */
    private HashMap<Integer, List<Cost>> calculateQueryCosts(InvertedFile invertedFile, List<Query> queries, Node n) {

        HashMap<Integer, List<Cost>> costs = new HashMap<>();
        HashMap<Integer, Double> similarities;

        for (int child = 0; child < n.children; child++) {
            costs.put(child, new ArrayList<Cost>());
        }

        invertedFile.load(n.identifier);
        for (Query q : queries) {
            if (numOfClusters != 0) {
                similarities = invertedFile.rankingSumClusterEnhance(q.keywords, q.keywordWeights);
            }
            else {
                similarities = invertedFile.rankingSum(q.keywords, q.keywordWeights);
            }

            for (int child = 0; child < n.children; child++) {
                int childId = n.identifiers[child];
                double irScore = 0;
                if (similarities.containsKey(childId))
                    irScore = similarities.get(childId);

                double spatialCost = n.mbr[child].getMinimumDistance(q.location);
                double queryCost = combinedScore(spatialCost, irScore);

                costs.get(child).add(new Cost(irScore, spatialCost, queryCost));
            }
        }
        return costs;
    }


    /**
     * Put the entry with highest cost first
     */
    private class WorstFirstNNEntryComparator implements Comparator<AggregateQuery.Result> {

        @Override
        public int compare(AggregateQuery.Result n1, AggregateQuery.Result n2) {
            if (n1.cost.totalCost > n2.cost.totalCost)
                return -1;
            if (n1.cost.totalCost < n2.cost.totalCost)
                return 1;
            return 0;
        }

    }

    public int getVisitedNodes() {
        return numOfVisitedNodes;
    }

}
