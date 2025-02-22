package org.ual.spatialindex.parameters;

public class ParametersFactory {
    public static DatasetParameters getParameters(Dataset dataset) {
        switch (dataset) {
            case PAPER_SET:
                return new DatasetParameters("src/main/resources/data/paper_keywords.txt", "src/main/resources/data/paper_locations.txt", 0, 30, 0, 27, 5, new int[]{2, 1, 3, 5, 4});
            case TESTING_SET:
                return new DatasetParameters("src/main/resources/data/key_test.txt", "src/main/resources/data/loc_test.txt", -5, 5, -4, 8, 7, new int[]{5, 2, 1, 3, 4, 6, 10});
            case ORIGINAL_SET:
                return new DatasetParameters("src/main/resources/data/keywords.txt", "src/main/resources/data/locations.txt", 31, 49, -118, -81, 6, new int[]{5, 1, 2, 4, 3, 6});
            case HOTEL_SET:
                return new DatasetParameters("src/main/resources/data/hotel_doc", "src/main/resources/data/hotel_loc", 19, 70, -159, -68, 600, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
            case POSTAL_CODES_SET:
                return new DatasetParameters("src/main/resources/data/postal_doc.txt", "src/main/resources/data/postal_loc.txt", -176, 180, -159, 74, 549405, new int[]{1, 8, 6, 872, 892, 890, 878, 882, 881, 898});
            case SPORTS_SET:
                return new DatasetParameters("src/main/resources/data/sports_doc.txt", "src/main/resources/data/sports_loc.txt", -180, 180, -90, 79, 452950, new int[]{82, 138, 1, 164, 202, 118, 3462, 203, 17, 907});
            case PARKS_SET:
                return new DatasetParameters("src/main/resources/data/parks_doc.txt", "src/main/resources/data/parks_loc.txt", -180, 180, -90, 81, 1002722, new int[]{15, 3, 4, 384, 39, 156, 0, 1, 25, 1162});
            default:
                throw new IllegalArgumentException("Unknown dataset: " + dataset);
        }
    }
}
