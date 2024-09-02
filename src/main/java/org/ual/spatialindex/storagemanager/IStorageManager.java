package org.ual.spatialindex.storagemanager;

import org.ual.spatialindex.spatialindex.INode;

public interface IStorageManager {
    static final int NewPage = -1;

    INode loadNode(final int id);
    int storeNode(final int id, final INode node);
    void deleteNode(final int id);
    int getIO();
}
