package org.ual.spatialindex.spatialindex;

import java.util.List;

public class NNEntry implements Comparable<NNEntry> {
    public IEntry node;
    public List<Integer> queryIndices;
    public List<Double> queryCosts;
    public double cost;
    public double minDistance;
    public double maxDistance;

    public NNEntry(IEntry node, double minDistance) {
        this.node = node;
        this.minDistance = minDistance;
    }

    public NNEntry(IEntry node, List<Integer> queryIndices, double cost) {
        this.node = node;
        this.queryIndices = queryIndices;
        this.cost = cost;
    }

    public NNEntry(IEntry node, double cost, List<Double> queryCosts) {
        this.node = node;
        this.queryCosts = queryCosts;
        this.cost = cost;
    }

    public int compareTo(NNEntry other) {
        if (this.cost < other.cost)
            return -1;
        if (this.cost > other.cost)
            return 1;
        return 0;
    }

    @Override
    public String toString() {
        return "NNEntry [node=" + node + ", cost=" + cost + "]";
    }
}
