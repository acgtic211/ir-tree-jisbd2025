package org.ual.spatialindex.spatialindex;

public interface IStatistics {
    long getReads();
    long getWrites();
    long getNumberOfNodes();
    long getNumberOfData();
}
