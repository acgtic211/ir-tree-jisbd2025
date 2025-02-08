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

    public DatasetParameters(String keywordFile, String locationFile, double latitudeStart, double latitudeEnd, double longitudeStart, double longitudeEnd, int uniqueKeywords) {
        this.keywordFile = keywordFile;
        this.locationFile = locationFile;
        this.latitudeStart = latitudeStart;
        this.latitudeEnd = latitudeEnd;
        this.longitudeStart = longitudeStart;
        this.longitudeEnd = longitudeEnd;
        this.uniqueKeywords = uniqueKeywords;
        this.maxD = Math.sqrt((latitudeEnd - latitudeStart) * (latitudeEnd - latitudeStart)
                + (longitudeEnd - longitudeStart) * (longitudeEnd - longitudeStart));
    }
}
