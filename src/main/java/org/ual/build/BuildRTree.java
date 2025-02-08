package org.ual.build;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ual.spatialindex.parameters.DatasetParameters;
import org.ual.spatialindex.rtree.RTree;
import org.ual.spatialindex.rtree.BulkLoader;
import org.ual.spatialindex.spatialindex.NodeData;
import org.ual.spatialindex.spatialindex.Region;
import org.ual.spatialindex.storagemanager.*;
import org.ual.utils.main.StatisticsLogic;

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
     * @param datasetParameters
     * @param fanout
     * @return RTree
     * @throws IOException
     */
    public static RTree buildRTree(DatasetParameters datasetParameters, int fanout ) throws IOException {

        LineNumberReader location_reader = new LineNumberReader(new FileReader(datasetParameters.locationFile));

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

        RTree tree = new RTree(ps2, storageManager, datasetParameters);

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


    public static RTree buildRTree(DatasetParameters datasetParameters, int fanout, double fillFactor, int dimension) {
        try(LineNumberReader locationReader = new LineNumberReader((new FileReader(datasetParameters.locationFile)))) {
            // Create a memory based storage manager.
            IStorageManager storageManager = new NodeStorageManager();

            // Create a new, empty, RTree with dimensionality 2, minimum load 70%
            PropertySet propertySet = new PropertySet();

            propertySet.setProperty("FillFactor", fillFactor);

            int capacity = fanout;
            propertySet.setProperty("IndexCapacity", capacity);
            propertySet.setProperty("LeafCapacity", capacity);
            // Index capacity and leaf capacity may be different.

            propertySet.setProperty("Dimension", dimension);

            RTree tree = new RTree(propertySet, storageManager, datasetParameters);

            int count = 0;
            int id;
            double x1, y1;
            double[] f1 = new double[2];
            double[] f2 = new double[2];
            String line;
            String[] temp;

            long start = System.currentTimeMillis();
            long initMem = StatisticsLogic.getMemUsed();

            while ((line = locationReader.readLine()) != null) {
                temp = line.split(",");
                id = Integer.parseInt(temp[0]);
                x1 = Double.parseDouble(temp[1]);
                y1 = Double.parseDouble(temp[2]);

                f1[0] = x1;
                f1[1] = y1;
                f2[0] = x1;
                f2[1] = y1;
                Region region = new Region(f1, f2);

                tree.insertData(null, region, id);

    //            if ((count % 1000) == 0)
    //                logger.debug("Count: {}", count);

                count++;
            }

            long end = System.currentTimeMillis();
            long endMem = StatisticsLogic.getMemUsed();
            StatisticsLogic.rTreePeakMemUsed = endMem - initMem;
            StatisticsLogic.rTreeMemUsed = StatisticsLogic.cleanMem((int) tree.getStatistics().getNumberOfNodes(), initMem); //Call gc()
            StatisticsLogic.rTreeBuildTime = (end - start);

            logger.info("Operations: {}", count);
            logger.info("Tree: {}", tree);
            //logger.info("Time: {} minutes", ((end - start) / 1000.0f) / 60.0f);
            logger.info("Rtree build time: {} ms", StatisticsLogic.rTreeBuildTime);
            logger.info("Rtree peak memory usage: {} Megabytes", (StatisticsLogic.rTreePeakMemUsed/1024)/1024);
            logger.info("Rtree memory usage: {} Megabytes", (StatisticsLogic.rTreeMemUsed/1024)/1024);

            // since we created a new RTree, the PropertySet that was used to initialize the structure
            // now contains the IndexIdentifier property, which can be used later to reuse the index.
            // (Remember that multiple indices may reside in the same storage manager at the same time
            // and every one is accessed using its unique IndexIdentifier).
            Integer indexID = (Integer) propertySet.getProperty("IndexIdentifier");
            logger.debug("Index ID: {}", indexID);

            boolean ret = tree.isIndexValid();
            if (!ret)
                logger.error("Structure is INVALID!");

            return tree;
        } catch (IOException e) {
            logger.error("Fail to operate with file: ", e);
            throw new RuntimeException(e);
        }
    }


    public static RTree buildRTreeSTR(DatasetParameters datasetParameters, int fanout, double fillFactor, int dimension) {
        try(LineNumberReader locationReader = new LineNumberReader((new FileReader(datasetParameters.locationFile)))) {
            // Create a memory based storage manager.
            IStorageManager storageManager = new NodeStorageManager();

            // Create a new, empty, RTree with dimensionality 2, minimum load 70%
            PropertySet propertySet = new PropertySet();

            propertySet.setProperty("FillFactor", fillFactor);

            int capacity = fanout;
            propertySet.setProperty("IndexCapacity", capacity);
            propertySet.setProperty("LeafCapacity", capacity);
            // Index capacity and leaf capacity may be different.

            propertySet.setProperty("Dimension", dimension);

            RTree tree = new RTree(propertySet, storageManager, datasetParameters);


            int count = 0;
            int id;
            double x1, y1;
            double[] f1 = new double[2];
            double[] f2 = new double[2];
            String line;
            String[] temp;

            long start = System.currentTimeMillis();
            long initMem = StatisticsLogic.getMemUsed();

            while ((line = locationReader.readLine()) != null) {
                temp = line.split(",");
                id = Integer.parseInt(temp[0]);
                x1 = Double.parseDouble(temp[1]);
                y1 = Double.parseDouble(temp[2]);

                f1[0] = x1;
                f1[1] = y1;
                f2[0] = x1;
                f2[1] = y1;
                Region region = new Region(f1, f2);

                //tree.storePseudoNodes(null, region, id);
                tree.storePseudoNodes(new NodeData(0), region, id);


                //            if ((count % 1000) == 0)
                //                logger.debug("Count: {}", count);

                count++;
            }

            System.out.println("Processing pseudo nodes");

            //BulkLoader bulkLoader = new BulkLoader();
            //bulkLoader.bulkLoadUsingSTR(tree, tree.pseudoNodes, capacity, dimension);

            BulkLoader bulkLoader = new BulkLoader();
            bulkLoader.bulkLoadUsingSTR(tree, tree.pseudoNodes, capacity, dimension, 10000, 100);

            //BulkLoader.bulkLoadUsingSTR(tree, tree.pseudoNodes, capacity, dimension);
            System.out.println("Done processing pseudo nodes");

            long end = System.currentTimeMillis();
            long endMem = StatisticsLogic.getMemUsed();
            StatisticsLogic.rTreePeakMemUsed = endMem - initMem;
            StatisticsLogic.rTreeMemUsed = StatisticsLogic.cleanMem((int) tree.getStatistics().getNumberOfNodes(), initMem); //Call gc()
            StatisticsLogic.rTreeBuildTime = (end - start);

            logger.info("Operations: {}", count);
            logger.info("Tree: {}", tree);
            //logger.info("Time: {} minutes", ((end - start) / 1000.0f) / 60.0f);
            logger.info("Rtree build time with BulkLoading: {} ms", StatisticsLogic.rTreeBuildTime);
            logger.info("Rtree peak memory usage: {} Megabytes", (StatisticsLogic.rTreePeakMemUsed/1024)/1024);
            logger.info("Rtree memory usage: {} Megabytes", (StatisticsLogic.rTreeMemUsed/1024)/1024);

            // since we created a new RTree, the PropertySet that was used to initialize the structure
            // now contains the IndexIdentifier property, which can be used later to reuse the index.
            // (Remember that multiple indices may reside in the same storage manager at the same time
            // and every one is accessed using its unique IndexIdentifier).
            Integer indexID = (Integer) propertySet.getProperty("IndexIdentifier");
            logger.debug("Index ID: {}", indexID);

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
