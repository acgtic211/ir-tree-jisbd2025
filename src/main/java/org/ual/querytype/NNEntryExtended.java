package org.ual.querytype;

import org.ual.spatialindex.spatialindex.IEntry;
import org.ual.spatialindex.spatialindex.RtreeEntry;

import java.util.List;

/**
 * A holder class for R-tree node and corresponding cost. Cost is an object.
 * Variant of the NNEntry class that use a Cost object, used in the Aggregated queries
 *
 */
// TODO MERGE with NNEntry?
public class NNEntryExtended implements Comparable<NNEntryExtended> {
    public IEntry entry;
    public List<Integer> queryIndices;
    public List<Cost> queryCosts;
    public Cost cost;

    public NNEntryExtended(IEntry entry, List<Integer> queryIndices, Cost cost) {
        this.entry = entry;
        this.queryIndices = queryIndices;
        this.cost = cost;
    }

    public NNEntryExtended(IEntry entry, Cost cost, List<Cost> queryCosts) {
        this.entry = entry;
        this.queryCosts = queryCosts;
        this.cost = cost;
    }

    public NNEntryExtended(IEntry entry, Cost cost) {
        this(entry, null, cost);
    }

    public int compareTo(NNEntryExtended other) {
        if (this.cost.totalCost < other.cost.totalCost)
            return -1;
        if (this.cost.totalCost > other.cost.totalCost)
            return 1;
        return 0;
    }

    @Override
    public String toString() {
        return "NNEntry [node=" + entry + ", cost=" + cost.totalCost + "]";
    }

}
