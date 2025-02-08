package org.ual.utils.analyze;

import org.ual.spatialindex.parameters.Dataset;
import org.ual.spatialindex.parameters.DatasetParameters;
import org.ual.spatialindex.parameters.ParametersFactory;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

public class LocationAnalyzer {

    public static void main(String[] args) throws IOException {
        DatasetParameters datasetParameters = ParametersFactory.getParameters(Dataset.POSTAL_CODES_SET);
        analyze(datasetParameters.locationFile);
    }

    private static void analyze(String locationsFilePath) throws IOException {
        LineNumberReader location_reader = new LineNumberReader(new FileReader(locationsFilePath));

        int id;
        double x1, y1;
        String line;
        String[] temp;

        double minLat = Double.POSITIVE_INFINITY;
        double maxLat = Double.NEGATIVE_INFINITY;
        double minLg = Double.POSITIVE_INFINITY;
        double maxLg = Double.NEGATIVE_INFINITY;


        while ((line = location_reader.readLine()) != null) {
            temp = line.split(",");
            id = Integer.parseInt(temp[0]);
            x1 = Double.parseDouble(temp[1]);
            y1 = Double.parseDouble(temp[2]);

            //System.out.println("line" + id + x1 + y1);


            maxLat = Math.max(x1, maxLat);
            minLat = Math.min(x1, minLat);

            maxLg = Math.max(y1, maxLg);
            minLg = Math.min(y1, minLg);

        }

        System.out.println("MaxLat: " + maxLat);
        System.out.println("MinLat: " + minLat);
        System.out.println("MaxLg: " + maxLg);
        System.out.println("MinLg: " + minLg);
    }
}
