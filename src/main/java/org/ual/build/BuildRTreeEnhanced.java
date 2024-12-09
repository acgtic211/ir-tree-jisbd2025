package org.ual.build;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ual.spatialindex.rtree.RTree;
import org.ual.spatialindex.rtreeenhanced.RTreeEnhanced;
import org.ual.spatialindex.spatialindex.Region;
import org.ual.spatialindex.storage.AbstractDocumentStore;
import org.ual.spatialindex.storagemanager.*;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashSet;

/**
 * Build a R-Tree like structure that takes document similarity into account for the DIR-tree
 */
public class BuildRTreeEnhanced {
    private static final Logger logger = LogManager.getLogger(BuildRTreeEnhanced.class);

    /**
     * Build a R-Tree like structure that takes document similarity into account
     * @param locationsFilePath
     * @param weightIndex
     * @param fanout
     * @param betaArea
     * @param maxWord
     * @return
     * @throws Exception
     */
    public static RTreeEnhanced buildRTreeEnhanced(String locationsFilePath, AbstractDocumentStore weightIndex, int fanout, double betaArea, int maxWord) throws Exception {
        //maxWord is used to control the number of words involved in tree building.
        //Large maxWord may incur high construction cost.
        AbstractDocumentStore.maxWord = maxWord;

        LineNumberReader location_reader = new LineNumberReader(new FileReader(locationsFilePath));

        // Create a memory based storage manager for the nodes
        IStorageManager storageManager = new NodeStorageManager();

        // Create a new, empty, RTree with dimensionality 2, minimum load 70%, using "file" as
        // the StorageManager and the RSTAR splitting policy.
        PropertySet ps2 = new PropertySet();

        Double f = 0.7;
        ps2.setProperty("FillFactor", f);

        Integer i = fanout;
        ps2.setProperty("IndexCapacity", i);
        ps2.setProperty("LeafCapacity", i);
        // Index capacity and leaf capacity may be different.

        i = 2;
        ps2.setProperty("Dimension", i);

        // Create DRTree
        RTreeEnhanced tree = new RTreeEnhanced(ps2, storageManager, weightIndex, betaArea);


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

            HashSet<Integer> doc = RTreeEnhanced.objstore.readSet(id);

            tree.insertData(null, r, id, doc);

//            if ((count % 1000) == 0)
//                System.err.println(count);

            count++;
        }

        long end = System.currentTimeMillis();
        logger.info("Operations: {}", count);
        logger.info("Tree: {}", tree);
        //logger.info("Time: {} minutes", ((end - start) / 1000.0f) / 60.0f);
        logger.info("RTree Enhanced build in: {} ms", (end - start));

        // since we created a new RTree, the PropertySet that was used to initialize the structure
        // now contains the IndexIdentifier property, which can be used later to reuse the index.
        // (Remember that multiple indices may reside in the same storage manager at the same time
        //  and every one is accessed using its unique IndexIdentifier).
        Integer indexID = (Integer) ps2.getProperty("IndexIdentifier");
        logger.info("Index ID: {}", indexID);

        boolean ret = tree.isIndexValid();
        if (!ret)
            logger.error("Structure is INVALID!");

        location_reader.close();

        return tree;
    }

    public static RTreeEnhanced buildEnhancedRTree(String locationsFilePath, int fanout, double fillFactor, int dimension, int maxWord,
                                   AbstractDocumentStore weightIndex, double betaArea) {
        try (LineNumberReader locationReader = new LineNumberReader((new FileReader(locationsFilePath)))) {
            //maxWord is used to control the number of words involved in tree building.
            //Large maxWord may incur high construction cost.
            AbstractDocumentStore.maxWord = maxWord;

            // Create a memory based storage manager for the nodes
            IStorageManager storageManager = new NodeStorageManager();

            // Create a new, empty, RTree with dimensionality 2, minimum load 70%, using "file" as
            // the StorageManager and the RSTAR splitting policy.
            PropertySet propertySet = new PropertySet();

            propertySet.setProperty("FillFactor", fillFactor);

            Integer capacity = fanout;
            propertySet.setProperty("IndexCapacity", capacity);
            propertySet.setProperty("LeafCapacity", capacity);
            // Index capacity and leaf capacity may be different.

            propertySet.setProperty("Dimension", dimension);

            // Create EnhancedRTree
            RTreeEnhanced tree = new RTreeEnhanced(propertySet, storageManager, weightIndex, betaArea);

            int count = 0;
            int id;
            double x1, y1;
            double[] f1 = new double[2];
            double[] f2 = new double[2];
            String line;
            String[] temp;

            long start = System.currentTimeMillis();

            while ((line = locationReader.readLine()) != null) {
                temp = line.split(",");
                id = Integer.parseInt(temp[0]);
                x1 = Double.parseDouble(temp[1]);
                y1 = Double.parseDouble(temp[2]);

                f1[0] = x1;
                f1[1] = y1;
                f2[0] = x1;
                f2[1] = y1;
                Region r = new Region(f1, f2);

                HashSet<Integer> doc = RTreeEnhanced.objstore.readSet(id);

                tree.insertData(null, r, id, doc);

//            if ((count % 1000) == 0)
//                System.err.println(count);

                count++;
            }

            long end = System.currentTimeMillis();
            logger.info("Operations: {}", count);
            logger.info("Tree: {}", tree);
            //logger.info("Time: {} minutes", ((end - start) / 1000.0f) / 60.0f);
            logger.info("EnhancedRTree Enhanced build in: {} ms", (end - start));

            // since we created a new RTree, the PropertySet that was used to initialize the structure
            // now contains the IndexIdentifier property, which can be used later to reuse the index.
            // (Remember that multiple indices may reside in the same storage manager at the same time
            //  and every one is accessed using its unique IndexIdentifier).
            Integer indexID = (Integer) propertySet.getProperty("IndexIdentifier");
            logger.info("Index ID: {}", indexID);

            boolean ret = tree.isIndexValid();
            if (!ret)
                logger.error("Structure is INVALID!");

            return tree;
        } catch (IOException e) {
            logger.error("Fail to operate with file: ", e);
            throw new RuntimeException(e);
        }

    }

}
