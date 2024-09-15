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
     *  Hotel set (hotel_doc - hotel_loc) objs: 20.676 keywords: 58821
     */
//     final double latitudeStart = 19;
//     final double latitudeEnd = 70;
//     final double longitudeStart = -159;
//     final double longitudeEnd = -68;
//     final int uniqueKeywords = 600;

    /**
     *  Postal codes set (postal_doc - postal_loc) objs: 171.227 keywords: 1.812.684
     */
     final double latitudeStart = -176;
     final double latitudeEnd = 180;
     final double longitudeStart = -159;
     final double longitudeEnd = 74;
     final int uniqueKeywords = 549405;

    /**
     *  Sports set (sports_doc - sports_loc) objs: 1.767.138 keywords: 4.510.539
     */
//     final double latitudeStart = -180;
//     final double latitudeEnd = 180;
//     final double longitudeStart = -90;
//     final double longitudeEnd = 79;
//     final int uniqueKeywords = 452950;

    /**
     *  ICDE 19 - Real set (icde19_real_doc - icde19_real_loc)
     */
//    final double latitudeStart = 41;
//    final double latitudeEnd = 49;
//    final double longitudeStart = 81;//72
//    final double longitudeEnd = 100;
//
//    final int uniqueKeywords = 36;

    /**
     *  GENERATED SET - RANDOM (generated_keywords.txt - generated_locs.txt) 250K objs
     */

//    final double latitudeStart = 20;
//    final double latitudeEnd = 48;
//    final double longitudeStart = -156;
//    final double longitudeEnd = -70;
//    final int uniqueKeywords = 600;

// 	Parameters for Yelp dataset

    //final int uniqueKeywords = 783;
    //final double maxWeight = 0.82478;

//	 Parameters for Flickr dataset

//	final int uniqueKeywords = 566432;
//	final double maxWeight = 8014422059718357;

    final double maxD = Math.sqrt((latitudeEnd - latitudeStart) * (latitudeEnd - latitudeStart)
            + (longitudeEnd - longitudeStart) * (longitudeEnd - longitudeStart));
}
