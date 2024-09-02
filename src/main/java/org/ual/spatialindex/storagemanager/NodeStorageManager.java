package org.ual.spatialindex.storagemanager;

import org.ual.spatialindex.spatialindex.INode;

import java.util.LinkedHashMap;
import java.util.Stack;

//TODO CLEAN PAGES
public class NodeStorageManager implements IStorageManager {
    private final LinkedHashMap<Integer, INode> treeStorage = new LinkedHashMap<>();
    private final Stack<Integer> emptyPages = new Stack<>();

    @Override
    public INode loadNode(int id) {
        return treeStorage.get(id);
    }

    @Override
    public int storeNode(int id, INode node) {
        int ret = id;

        if(id == NewPage) {
            if (emptyPages.empty()) {
                ret = (treeStorage.isEmpty())? 0 : treeStorage.size();
                treeStorage.put(ret, node);
            } else {
                ret = emptyPages.pop();
                treeStorage.put(ret, node);
            }
        } else {
            if (id < 0 || id >= treeStorage.size()) throw new InvalidPageException(id);
            treeStorage.put(id, node);
        }

        return ret;
    }

    @Override
    public void deleteNode(int id) {
        if (treeStorage.containsKey(id)) {
            treeStorage.replace(id, null);
            emptyPages.push(id);
        } else {
            throw new InvalidPageException(id);
        }
    }

    @Override
    public int getIO() {
        return 0;
    }


}
