package org.ual.spatialindex.spatialindex;

public interface IVisitor {
    void visitNode(final INode n);
    void visitData(final IData d);
}
