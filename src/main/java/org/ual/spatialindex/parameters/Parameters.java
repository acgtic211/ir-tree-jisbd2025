package org.ual.spatialindex.parameters;

/**
 * Hold the keyword and location parameters. Change it based on the dataset used.
 */
public interface Parameters {
    /**
     *  Original set (keywords.txt - locations.txt)
     */
    // final double latitudeStart = 31;
    // final double latitudeEnd = 49;
    // final double longitudeStart = -118;
    // final double longitudeEnd = -81;

    /**
     *  Hotel set (hotel_doc - hotel_loc) objs: 20676 keywords: 58821
     */
//     final double latitudeStart = 19;
//     final double latitudeEnd = 70;
//     final double longitudeStart = -100;
//     final double longitudeEnd = -99;
//     final int uniqueKeywords = 600;

    /**
     *  ICDE 19 - Real set (icde19_real_doc - icde19_real_loc)
     */
    final double latitudeStart = 41;
    final double latitudeEnd = 49;
    final double longitudeStart = 81;//72
    final double longitudeEnd = 100;

    final int uniqueKeywords = 36;

// 	Parameters for Yelp dataset

    //final int uniqueKeywords = 783;
    //final double maxWeight = 0.82478;

//	 Parameters for Flickr dataset

//	final int uniqueKeywords = 566432;
//	final double maxWeight = 8014422059718357;

    final double maxD = Math.sqrt((latitudeEnd - latitudeStart) * (latitudeEnd - latitudeStart)
            + (longitudeEnd - longitudeStart) * (longitudeEnd - longitudeStart));
}
