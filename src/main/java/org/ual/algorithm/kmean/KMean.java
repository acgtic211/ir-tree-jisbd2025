package org.ual.algorithm.kmean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ual.spatialindex.storage.IStore;
import org.ual.spatialindex.storage.Weight;
import org.ual.spatialindex.storage.WeightEntry;

import java.util.*;



public class KMean {
	private static int numOfClusters;	//number of clusters, lable begins with 0
	//public static int numDocs = 162033;		//number of documents
	private static int dimension;	//number of words in all documents
	private static MedoidEntry[] medoids;
	private static int[] assign;	//clustering result: each element in the array represents a document. The value of the element is the cluster label.
	private static ArrayList<DocEntry> docs = new ArrayList<>();
	private static final Logger logger = LogManager.getLogger(KMean.class);
	private static final Random random = new Random(1);


	/**
	 * Calculate Kmeans based on medoids
	 *
	 * @param weights
	 * @param numClusters
	 * @param numMoves
	 * @return
	 */
	public static HashMap<Integer, Integer> calculateKMean(IStore weights, int numClusters, int numMoves) {
		numOfClusters = numClusters;

		int maxid = -1;
		int maxword = -1;

		Iterator<Weight> iter = weights.iterator();
		while (iter.hasNext()){
			Weight we = iter.next();
			int id = we.wordId;
			maxid = Math.max(maxid, id);
			DocEntry de = new DocEntry(id, we.weights.size());

			int j = 0;
			for(WeightEntry entry : we.weights) {
				int word = entry.word;
				double weight = entry.weight;
				de.words[j] = new WordEntry(word, weight);
				maxword = Math.max(maxword, word);
				de.sumOfSquare += Math.pow(weight, 2);
				j++;
			}
			de.sumOfSquare = Math.sqrt(de.sumOfSquare);
			docs.add(de);
		}

		dimension = maxword + 1;
		assign = new int[maxid + 1];
		medoids = new MedoidEntry[numOfClusters];

		int moves = Integer.MAX_VALUE;

		Arrays.fill(assign, -1);

		long start = System.currentTimeMillis();

		initialMedoids();
		initialClustering();
		updateMedoid();
		printResults();

		long end = System.currentTimeMillis();
		logger.info("Medoids and clustering done in: {} ms", (end - start));

		//iteration
		while(moves > numMoves) {
			moves = 0;

			start = System.currentTimeMillis();
			//re-clustering
            for (DocEntry de : docs) {
				int cg = findMedoid(de);
				if (cg != assign[de.id])
                    moves++;

                assign[de.id] = cg;
            }
			updateMedoid();
			printResults();

			end = System.currentTimeMillis();
			logger.info("Moves: {} Time: {} ms", moves, (end - start));
		}

		logger.info("Final Results:");
		printResults();

		HashMap<Integer, Integer> tree = new HashMap<>();

		for(int c = 0; c < assign.length; c++) {
			logger.debug("assign = {}", assign[c]);
			if(assign[c] == -1)
				continue;
			Integer res = tree.put(c, assign[c]);
			if(res != null) {
				logger.error("Duplicated entry");
				System.exit(-1);
			}
		}
		logger.info("Total time: {} ms", (end - start));

		return tree;
	}

	private static void initialClustering() {
		logger.info("Initial Clustering...");

		for(DocEntry de : docs){
			assign[de.id] = findMedoid(de);
		}
	}

	private static int findMedoid(DocEntry de) {
		double dist = -1;
		int cg = -1;
		for(int i = 0; i < medoids.length; i++){
			double d = cosDist(de, medoids[i]);

			if(dist <= d){
				dist = d;
				cg = i;
			}
		}
		return cg;
	}

	private static void initialMedoids() {
		logger.info("Initial Medoids");
		HashSet<Integer> hs = new HashSet<>();
		for(int i = 0; i < numOfClusters; i++){
			int pos;
			do {
				pos = random.nextInt(docs.size());
			} while(hs.contains(pos));

			hs.add(pos);
			DocEntry de = docs.get(pos);

			medoids[i] = new MedoidEntry(de.id, dimension);
			for(WordEntry word : de.words){
				medoids[i].words[word.id] = word.weight;
				medoids[i].sumOfSquare += Math.pow(word.weight, 2);
			}
			medoids[i].sumOfSquare = Math.sqrt(medoids[i].sumOfSquare);
		}
	}


	public static double cosDist(DocEntry dd, MedoidEntry dm){
		double dist;
		double fz = 0;

		for(WordEntry word : dd.words) {
			fz += word.weight * dm.words[word.id];
		}
		
		dist = fz / (dd.sumOfSquare * dm.sumOfSquare);
		return dist;
	}
	
	public static void updateMedoid(){

        for (MedoidEntry medoid : medoids) {
            medoid.cardinality = 0;
            medoid.sumOfSquare = 0;
            Arrays.fill(medoid.words, 0.0);
        }

        for (DocEntry de : docs) {
            int clusterLabel = assign[de.id];
            medoids[clusterLabel].cardinality++;
			for(WordEntry word : de.words) {
				medoids[clusterLabel].words[word.id] += word.weight;
            }
        }

        for (MedoidEntry medoid : medoids) {
            for (int j = 0; j < medoid.words.length; j++) {
                medoid.words[j] /= medoid.cardinality;
                medoid.sumOfSquare += Math.pow(medoid.words[j], 2);
            }
            medoid.sumOfSquare = Math.sqrt(medoid.sumOfSquare);
        }
				
	}
	
	public static void printResults() {
		HashMap<Integer, Integer> counter = new HashMap<>();
        for (int j : assign) {
			if (j != -1) {
				counter.merge(j, 1, Integer::sum);
			}
        }

        for (Map.Entry<Integer, Integer> entry : counter.entrySet()) {
			int m = entry.getKey();
            int c = entry.getValue();
			logger.info("Medoid: {} - Members: {}", m, c);
        }
	}

	private KMean() {
		throw new IllegalStateException("Utility class");
	}
	
}
