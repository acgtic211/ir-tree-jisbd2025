package org.ual.spatialindex.parameters;

public class DatasetParameters {
    public final String keywordFile;
    public final String locationFile;
    public final double latitudeStart;
    public final double latitudeEnd;
    public final double longitudeStart;
    public final double longitudeEnd;
    public final int uniqueKeywords;
    public final double maxD;
    public final int[] topkWords; // Top-k words for each dataset. Calculated in KeywordsAnalyzer.java

    public DatasetParameters(String keywordFile, String locationFile, double latitudeStart, double latitudeEnd, double longitudeStart, double longitudeEnd, int uniqueKeywords, int[] topkWords) {
        this.keywordFile = keywordFile;
        this.locationFile = locationFile;
        this.latitudeStart = latitudeStart;
        this.latitudeEnd = latitudeEnd;
        this.longitudeStart = longitudeStart;
        this.longitudeEnd = longitudeEnd;
        this.uniqueKeywords = uniqueKeywords;
        this.topkWords = topkWords;
        this.maxD = Math.sqrt((latitudeEnd - latitudeStart) * (latitudeEnd - latitudeStart)
                + (longitudeEnd - longitudeStart) * (longitudeEnd - longitudeStart));
    }
}
