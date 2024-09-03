package org.ual.build;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ual.algorithm.kmean.KMean;
import org.ual.spatialindex.rtree.Node;
import org.ual.spatialindex.rtree.RTree;
import org.ual.spatialindex.spatialindex.Region;
import org.ual.spatialindex.storagemanager.*;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

public class BuildRTree {
    private static final Logger logger = LogManager.getLogger(BuildRTree.class);

    /**
     * Process the location file and store it in a RTree.
     *
     * location_file format: one object per line; each line: id,x,y (integer,double,double)
     *
     * @param locationsFilePath
     * @param fanout
     * @return RTree
     * @throws IOException
     */
    public static RTree buildRTree(String locationsFilePath, int fanout) throws IOException {

        LineNumberReader location_reader = new LineNumberReader(new FileReader(locationsFilePath));

        // Create a memory based storage manager.
        IStorageManager storageManager = new NodeStorageManager();

        // Create a new, empty, RTree with dimensionality 2, minimum load 70%
        PropertySet ps2 = new PropertySet();

        Double fillFactor = 0.7;
        ps2.setProperty("FillFactor", fillFactor);

        int capacity = fanout;
        ps2.setProperty("IndexCapacity", capacity);
        ps2.setProperty("LeafCapacity", capacity);
        // Index capacity and leaf capacity may be different.

        capacity = 2;
        ps2.setProperty("Dimension", capacity);

        RTree tree = new RTree(ps2, storageManager);

        int count = 0;
        int id;
        double x1, y1;
        double[] f1 = new double[2];
        double[] f2 = new double[2];
        String line;
        String[] temp;

        long start = System.currentTimeMillis();

        while ((line = location_reader.readLine()) != null) {
            temp = line.split(",");
            id = Integer.parseInt(temp[0]);
            x1 = Double.parseDouble(temp[1]);
            y1 = Double.parseDouble(temp[2]);

            f1[0] = x1;
            f1[1] = y1;
            f2[0] = x1;
            f2[1] = y1;
            Region r = new Region(f1, f2);

            tree.insertData(null, r, id);

//            if ((count % 1000) == 0)
//                logger.debug("Count: {}", count);

            count++;
        }

        long end = System.currentTimeMillis();
        logger.info("Operations: {}", count);
        logger.info("Tree: {}", tree);
        //logger.info("Time: {} minutes", ((end - start) / 1000.0f) / 60.0f);
        logger.info("Rtree build time: {} ms", (end - start));

        // since we created a new RTree, the PropertySet that was used to initialize the structure
        // now contains the IndexIdentifier property, which can be used later to reuse the index.
        // (Remember that multiple indices may reside in the same storage manager at the same time
        // and every one is accessed using its unique IndexIdentifier).
        Integer indexID = (Integer) ps2.getProperty("IndexIdentifier");
        logger.debug("Index ID: {}", indexID);

        boolean ret = tree.isIndexValid();
        if (!ret)
            logger.error("Structure is INVALID!");

        location_reader.close();

        return tree;
    }
}
