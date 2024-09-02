package org.ual.documentindex;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ual.spatialindex.rtreeenhanced.RTreeEnhanced;
import org.ual.spatialindex.rtree.RTree;
import org.ual.spatialindex.storage.WeightEntry;

import java.util.*;

public class InvertedFile {
    private HashMap<Integer, ArrayList<InvertedListEntry>> nodeInvListStorage; // Inverted lists stored by node ID
    private ArrayList<InvertedListEntry> invertedList; // Inverted list content

    private static final Logger logger = LogManager.getLogger(InvertedFile.class);

    public InvertedFile() {
        nodeInvListStorage = new HashMap<>();
    }

    public void create(int nodeId) {
        invertedList = new ArrayList<>();
        nodeInvListStorage.put(nodeId, invertedList);
    }

    public void load(int nodeId) {
        invertedList = nodeInvListStorage.get(nodeId);
    }

    public void addDocument(int nodeId, int docId, ArrayList<WeightEntry> document) {
        load(nodeId);

        for (int i = 0; i < document.size(); i++) {
            WeightEntry docEntry = document.get(i);
            InvertedListEntry invListEntry = new InvertedListEntry(docEntry.word);
            int loc = Collections.binarySearch(invertedList, invListEntry, new InvertedListEntryComparator());

            if (loc >= 0) {
                invListEntry = invertedList.get(loc);
                PlEntry plEntry = new PlEntry(docId, docEntry.weight);
                invListEntry.add(plEntry);
            } else {
                PlEntry plEntry = new PlEntry(docId, docEntry.weight);
                invListEntry.add(plEntry);
                invertedList.add((-loc - 1), invListEntry);
            }
        }
    }

    public void addDocument(int nodeId, int docId, ArrayList<WeightEntry> document, int cluster) {
        //invertedList = nodeInvListStorage.get(nodeId);
        load(nodeId);

        for (int i = 0; i < document.size(); i++) {
            WeightEntry docEntry = document.get(i);
            InvertedListEntry invListEntry = new InvertedListEntry(docEntry.word);
            int loc = Collections.binarySearch(invertedList, invListEntry, new InvertedListEntryComparator());

            if (loc >= 0) {
                invListEntry = invertedList.get(loc);
                PlEntry plEntry = new PlEntry(docId, docEntry.weight, cluster);
                invListEntry.add(plEntry);
            } else {
                PlEntry plEntry = new PlEntry(docId, docEntry.weight, cluster);
                invListEntry.add(plEntry);
                invertedList.add((-loc - 1), invListEntry);
            }
        }
    }

    public ArrayList<WeightEntry> store(int nodeId) {
        ArrayList<WeightEntry> pseudoDoc = new ArrayList<>();
        //invertedList = nodeInvListStorage.get(treeId);
        load(nodeId);

        for (int i = 0; i < invertedList.size(); i++) {
            InvertedListEntry invListEntry = invertedList.get(i);

            double maxWeight = Double.NEGATIVE_INFINITY;
            for(int j = 0; j < invListEntry.pl.size(); j++) {
                PlEntry plEntry = invListEntry.pl.get(j);
                maxWeight = Math.max(maxWeight, plEntry.weight);
            }

            WeightEntry weightEntry = new WeightEntry(invListEntry.term, maxWeight);
            pseudoDoc.add(weightEntry);
        }

        nodeInvListStorage.put(nodeId, new ArrayList<>(invertedList));
        invertedList.clear();

        return  pseudoDoc;
    }

    public ArrayList<ArrayList<WeightEntry>> storeClusterEnhance(int nodeId) {
        ArrayList<ArrayList<WeightEntry>> pseudoDoc = new ArrayList<>(Collections.nCopies(RTree.numOfClusters, null));

        for (int i = 0; i < RTree.numOfClusters; i++) {
            pseudoDoc.set(i, new ArrayList<>());
        }

        load(nodeId);

        for (int i = 0; i < invertedList.size(); i++) {
            InvertedListEntry invListEntry = invertedList.get(i);

            double[] maxWeight = new double[RTree.numOfClusters];
            Arrays.fill(maxWeight, Double.NEGATIVE_INFINITY);

            for (int j = 0; j < invListEntry.pl.size(); j++) {
                PlEntry plEntry = invListEntry.pl.get(j);
                maxWeight[plEntry.cluster] = Math.max(maxWeight[plEntry.cluster], plEntry.weight);

                logger.debug("Term: {} - DocID: {} - Cluster: {} - Weight: {}", invListEntry.term, plEntry.documentId, plEntry.cluster, plEntry.weight);
            }

            for (int j = 0; j < maxWeight.length; j++) {
                if (maxWeight[j] != Double.NEGATIVE_INFINITY) {
                    WeightEntry weightEntry = new WeightEntry(invListEntry.term, maxWeight[j]);
                    pseudoDoc.get(j).add(weightEntry);
                }
            }
        }

        nodeInvListStorage.put(nodeId, new ArrayList<>(invertedList));
        invertedList.clear();

        return pseudoDoc;
    }



    //TODO TEMP FIX
    public ArrayList<ArrayList<WeightEntry>> storeClusterEnhanceDIRTree(int nodeId) {
        ArrayList<ArrayList<WeightEntry>> pseudoDoc = new ArrayList<>(Collections.nCopies(RTreeEnhanced.numOfClusters, null));

        for (int i = 0; i < RTreeEnhanced.numOfClusters; i++) {
            pseudoDoc.set(i, new ArrayList<>());
        }

        load(nodeId);

        logger.debug("DIR-tree nodeId: {}", nodeId);

        for (int i = 0; i < invertedList.size(); i++) {
            InvertedListEntry invListEntry = invertedList.get(i);

            double[] maxWeight = new double[RTreeEnhanced.numOfClusters];
            Arrays.fill(maxWeight, Double.NEGATIVE_INFINITY);

            for (int j = 0; j < invListEntry.pl.size(); j++) {
                PlEntry plEntry = invListEntry.pl.get(j);
                maxWeight[plEntry.cluster] = Math.max(maxWeight[plEntry.cluster], plEntry.weight);

                logger.debug("ClusterDIR Term: {} - DocID: {} - Cluster: {} - Weight: {}", invListEntry.term, plEntry.documentId, plEntry.cluster, plEntry.weight);
            }

            for (int j = 0; j < maxWeight.length; j++) {
                if (maxWeight[j] != Double.NEGATIVE_INFINITY) {
                    WeightEntry weightEntry = new WeightEntry(invListEntry.term, maxWeight[j]);
                    pseudoDoc.get(j).add(weightEntry);
                }
            }
        }

        nodeInvListStorage.put(nodeId, new ArrayList<>(invertedList));
        invertedList.clear();

        return pseudoDoc;
    }


    /**
     * Read the posting list of this keyword i.e. the documents which contains this keyword
     */
    public ArrayList<PlEntry> read(int keyword) {
        InvertedListEntry invListEntry = new InvertedListEntry(keyword);
        int loc = Collections.binarySearch(invertedList, invListEntry, new InvertedListEntryComparator());

        if (loc >= 0) {
            return new ArrayList<>(invertedList.get(loc).pl);
        } else {
            return null;
        }
    }


    /**
     * Calculates the total similarity of each document (node), where weights are calculated summing over
     * the weight of the terms that matches the query keywords
     *
     * Corresponding tree must be loaded by using load(treeId) before calling this method.
     *
     * @return A map of (document ID, similarity) pairs
     */
    @Deprecated
    public HashMap<Integer, Double> rankingSum(ArrayList<Integer> keywords) {
        HashMap<Integer, Double> filter = new HashMap<>();

        //TODO TEST LOAD root node to search
        //load(0);
        for (Integer keyword : keywords) {
            ArrayList<PlEntry> docList = read(keyword);
            if (docList == null)
                continue;
            for (int k = 0; k < docList.size(); k++) {
                PlEntry ple = docList.get(k);

                if (filter.containsKey(ple.documentId)) {
                    double similarity = filter.get(ple.documentId);
                    similarity = similarity + ple.weight;
                    filter.put(ple.documentId, similarity);
                } else
                    filter.put(ple.documentId, ple.weight);
            }
        }

        return filter;
    }


    /**
     * Calculates the total similarity of each document (node). The 'keywords' parameter contains a keyword -> weight
     * mapping.
     *
     * Corresponding tree must be loaded by using load(treeId) before calling this method.
     *
     * @return A map of (document ID, similarity) pairs
     */
    public HashMap<Integer, Double> rankingSum(List<Integer> keywords, List<Double> keywordWeights) {
        HashMap<Integer, Double> filter = new HashMap<>();

        for (int i = 0; i < keywords.size(); i++) {
            int keyword = keywords.get(i);
            double weight = keywordWeights.get(i);

            ArrayList<PlEntry> docList = read(keyword);
            if (docList == null)
                continue;

            for (PlEntry ple : docList) {
                if (filter.containsKey(ple.documentId)) {
                    double similarity = filter.get(ple.documentId);
                    similarity = similarity + weight;
                    filter.put(ple.documentId, similarity);
                } else {
                    filter.put(ple.documentId, weight);
                }
            }
        }

        return filter;
    }


    public HashMap<Integer, Double> rankingSumClusterEnhance(List<Integer> keywords, List<Double> keywordWeights) {
        HashMap<String, Double> filter = new HashMap<>();
        HashMap<Integer, Double> filterFinal = new HashMap<>();

        for (int j = 0; j < keywords.size(); j++) {
            int word = keywords.get(j);
            double weight = keywordWeights.get(j); // Test

            ArrayList<PlEntry> doclist = read(word);
            if (doclist == null)
                continue;

            for (PlEntry ple : doclist) {
                //for (int k = 0; k < doclist.size(); k++) {
                //PlEntry ple = (PlEntry) doclist.get(k);
                String key = ple.documentId + "," + ple.cluster;
                if (filter.containsKey(key)) {
                    double similarity = filter.get(key);
                    similarity += weight;
                    filter.put(key, similarity);
                } else
                    filter.put(key, weight);
            }
        }

        for (String key : filter.keySet()) {
            double value = filter.get(key);
            String[] temp = key.split(",");
            int id = Integer.parseInt(temp[0]);

            if (filterFinal.containsKey(id)) {
                double w = filterFinal.get(id);
                if (w < value)
                    filterFinal.put(id, value);
            } else
                filterFinal.put(id, value);
        }

        return filterFinal;
    }

    // TODO TEST
    public HashMap<Integer, Double> rankingSumClusterEnhance(List<Integer> keywords) {
        HashMap<String, Double> filter = new HashMap<>();
        HashMap<Integer, Double> filterFinal = new HashMap<>();

        for (int j = 0; j < keywords.size(); j++) {
            int word = keywords.get(j);
            ArrayList<PlEntry> doclist = read(word);
            if (doclist == null)
                continue;

            for (PlEntry ple : doclist) {
                //for (int k = 0; k < doclist.size(); k++) {
                //PlEntry ple = (PlEntry) doclist.get(k);
                String key = ple.documentId + "," + ple.cluster;
                if (filter.containsKey(key)) {
                    double similarity = filter.get(key);
                    similarity += ple.weight;
                    filter.put(key, similarity);
                } else
                    filter.put(key, ple.weight);
            }
        }

        for (String key : filter.keySet()) {
            double value = filter.get(key);
            String[] temp = key.split(",");
            int id = Integer.parseInt(temp[0]);

            if (filterFinal.containsKey(id)) {
                double w = filterFinal.get(id);
                if (w < value)
                    filterFinal.put(id, value);
            } else {
                filterFinal.put(id, value);
            }
        }

        return filterFinal;
    }


    // TODO TEST
    public HashMap<Integer, Integer> booleanFilter(int treeId, ArrayList<Integer> keywords) {
        HashMap<Integer, Integer> filter = new HashMap<>();

        load(treeId);

        for (int j = 0; j < keywords.size(); j++){
            int word = keywords.get(j);

            ArrayList<PlEntry> doclist = read(word);
            if(doclist == null)
                continue;

            for (PlEntry ple : doclist) {
                if(!filter.containsKey(ple.documentId)) {
                    filter.put(ple.documentId, 1);
                } else {
                    int count = ple.documentId;
                    count++;
                    filter.put(ple.documentId, count);
                }
            }
        }

        return filter;
    }

    @Deprecated
    // TODO REMOVE
    public int getIO() {
        return 0;
    }
}
