package org.ual.spatialindex.spatialindex;

import java.util.Arrays;

public class NodeData {
    public  int capacity;
    public NodeData[] data;

    public NodeData(int capacity) {
        this.capacity = capacity;
        this.data = new NodeData[capacity];
    }

    @Override
    public String toString() {
        return "NodeData{" +
                "capacity=" + capacity +
                ", data=" + Arrays.toString(data) +
                '}';
    }
}
