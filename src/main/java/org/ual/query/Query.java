package org.ual.query;

import org.ual.spatialindex.spatialindex.Point;

import java.util.ArrayList;
import java.util.List;

public class Query {
    public int id;
    public double weight;
    public Point location;
    public List<Integer> keywords;
    public List<Double> keywordWeights;

    //TEST
    QueryResult result;

    public Query(int id) {
        this.id = id;
        this.keywords = new ArrayList<>();
        this.keywordWeights = new ArrayList<>();
    }

    public Query(int id, double weight, Point location, List<Integer> keywords, List<Double> keywordWeights) {
        this.id = id;
        this.weight = weight;
        this.location = location;
        this.keywords = keywords;
        this.keywordWeights = keywordWeights;
    }

    public Query(int id, Point location, List<Integer> keywords) {
        this.id = id;
        this.location = location;
        this.keywords = keywords;
    }

    @Override
    public String toString() {
        return "Query{" +
                "id=" + id +
                ", weight=" + weight +
                ", location=" + location +
                ", keywords=" + keywords +
                ", keywordWeights=" + keywordWeights +
                '}';
    }
}
