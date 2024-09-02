package org.ual.documentindex;

import java.io.Serializable;

/**
 * Posting List class. Contains the documentId, weight and cluster information
 */
public class PlEntry implements Serializable {
    public int documentId;
    public double weight;
    public int cluster;

    public PlEntry(int id, double weight) {
        this.documentId = id;
        this.weight = weight;
    }

    public PlEntry(int id, double weight, int cluster) {
        this.documentId = id;
        this.weight = weight;
        this.cluster = cluster;
    }

}
