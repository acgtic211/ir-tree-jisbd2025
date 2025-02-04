package org.ual.spatialindex.parameters;

public class ParametersFactory {
    public static DatasetParameters getParameters(Dataset dataset) {
        switch (dataset) {
            case TESTING_SET:
                return new DatasetParameters("src/main/resources/data/key_test.txt", "src/main/resources/data/loc_test.txt", -5, 5, -4, 8, 7);
            case ORIGINAL_SET:
                return new DatasetParameters("src/main/resources/data/keywords.txt", "src/main/resources/data/locations.txt", 31, 49, -118, -81, 6);
            case HOTEL_SET:
                return new DatasetParameters("src/main/resources/data/hotel_doc", "src/main/resources/data/hotel_loc", 19, 70, -159, -68, 600);
            case POSTAL_CODES_SET:
                return new DatasetParameters("src/main/resources/data/postal_doc.txt", "src/main/resources/data/postal_loc.txt", -176, 180, -159, 74, 549405);
            case SPORTS_SET:
                return new DatasetParameters("src/main/resources/data/sports_doc.txt", "src/main/resources/data/sports_loc.txt", -180, 180, -90, 79, 452950);
            default:
                throw new IllegalArgumentException("Unknown dataset: " + dataset);
        }
    }
}
