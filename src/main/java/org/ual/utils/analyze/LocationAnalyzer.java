package org.ual.utils.analyze;

import org.ual.spatialindex.rtree.RTree;
import org.ual.spatialindex.spatialindex.Region;
import org.ual.spatialindex.storagemanager.IStorageManager;
import org.ual.spatialindex.storagemanager.NodeStorageManager;
import org.ual.spatialindex.storagemanager.PropertySet;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

public class LocationAnalyzer {
    static String locationsFilePath = "src/main/resources/data/icde19_real_loc.txt";

    public static void main(String[] args) throws IOException {
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
        System.out.println("MaxLg: " + minLg);
    }
}
