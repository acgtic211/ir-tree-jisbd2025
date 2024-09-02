package org.ual.spatialindex.spatialindex;

import org.ual.spatialindex.storagemanager.PropertySet;

public interface ISpatialIndex {
    //void flush() throws IllegalStateException;
    //void insertData(NodeData data, final IShape shape, int id);
    void insertData(NodeData data, final IShape shape, int id);
    boolean deleteData(final IShape shape, int id);
    void containmentQuery(final IShape query, final IVisitor v);
    void intersectionQuery(final IShape query, final IVisitor v);
    void pointLocationQuery(final IShape query, final IVisitor v);
    void nearestNeighborQuery(int k, final IShape query, final IVisitor v, INearestNeighborComparator nnc);
    void nearestNeighborQuery(int k, final IShape query, final IVisitor v);
    void queryStrategy(final IQueryStrategy qs);
    PropertySet getIndexProperties();
    void addWriteNodeCommand(INodeCommand nc);
    void addReadNodeCommand(INodeCommand nc);
    void addDeleteNodeCommand(INodeCommand nc);
    boolean isIndexValid();
    IStatistics getStatistics();
}
