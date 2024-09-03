package org.ual.algorithm.kmean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ual.spatialindex.storage.IStore;
import org.ual.spatialindex.storage.Weight;
import org.ual.spatialindex.storage.WeightEntry;

import java.util.*;



public class KMean {
	public static int numOfClusters;	//number of clusters, lable begins with 0
	//public static int numDocs = 162033;		//number of documents
	public static int dimension;	//number of words in all documents
	public static int MOVES;		    //stopping condition
	public static MedoidEntry[] medoids; 
	public static int[] assign;	//clustering result: each element in the array represents a document. The value of the element is the cluster label.
	public static ArrayList<DocEntry> docs = new ArrayList<>();
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
		MOVES = numMoves;

		int id, count = 0;
		int maxid = -1;
		int maxword = -1;

		Iterator<Weight> iter = weights.iterator();
		while (iter.hasNext()){
			Weight we = iter.next();
			id = we.wordId;
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

//			if(count % 10000 == 0)
//				logger.debug("Count: {}", count);
			count++;
		}

		dimension = maxword + 1;
		assign = new int[maxid + 1];
		medoids = new MedoidEntry[numOfClusters];

		int moves = Integer.MAX_VALUE;

		Arrays.fill(assign, -1);

		long start = System.currentTimeMillis();

		logger.info("Initial Medoids");

		HashSet<Integer> hs = new HashSet<>();
		for(int i = 0; i < numOfClusters; i++){
			int pos;
			do {
				pos = random.nextInt(docs.size());
			} while(hs.contains(pos));

			hs.add(pos);
			DocEntry de = docs.get(pos);
			id = de.id;

			medoids[i] = new MedoidEntry(id, dimension);
			//medoids[i].cardinality = 1;

			for(int j = 0; j < de.words.length; j++){
				medoids[i].words[de.words[j].id] = de.words[j].weight;
				medoids[i].sumOfSquare += Math.pow(de.words[j].weight, 2);;
			}
			medoids[i].sumOfSquare = Math.sqrt(medoids[i].sumOfSquare);
			//medoids[i].show();
			//assign[id] = i;
		}

		logger.info("Initial Clustering...");

		for(int k = 0; k < docs.size(); k++){
			DocEntry de = docs.get(k);

			double dist = -1;
			int cg = -1;
			for(int i = 0; i < medoids.length; i++){
				double d = cosDist(de, medoids[i]);

				if(dist <= d){
					dist = d;
					cg = i;
				}
			}
			assign[de.id] = cg;
		}
		updateMedoid();
		printResults();

		long end = System.currentTimeMillis();
		//logger.info("Time: {} minutes", (end - start)/1000.0/60);
		logger.info("Medoids and clustering done in: {} ms", (end - start));

		//iteration
		while(moves > MOVES) {
			//logger.info("moves: {} ", moves);
			moves = 0;

			start = System.currentTimeMillis();
			//re-clustering
            for (DocEntry de : docs) {
                double dist = -1;
                int cg = -1;
                for (int i = 0; i < medoids.length; i++) {
                    double d = cosDist(de, medoids[i]);

                    if (dist <= d) {
                        dist = d;
                        cg = i;
                    }
                }
                if (cg != assign[de.id])
                    moves++;

                assign[de.id] = cg;
            }
			updateMedoid();
			printResults();

			end = System.currentTimeMillis();
			//logger.info("Moves: {} Time: {} minutes", moves, (end - start)/1000.0/60);
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


	public static double cosDist(DocEntry dd, MedoidEntry dm){
		double dist = 0;
		double fz = 0;
		
		for(int j = 0; j < dd.words.length; j++) {
			fz += dd.words[j].weight * dm.words[dd.words[j].id];
		}
		
		dist = fz / (dd.sumOfSquare * dm.sumOfSquare);
		return dist;
	}
	
	public static void updateMedoid(){
		
		for(int i = 0; i < medoids.length; i++){
			medoids[i].cardinality = 0;
			medoids[i].sumOfSquare = 0;
            Arrays.fill(medoids[i].words, 0.0);
		}
						
		for(int i = 0; i < docs.size(); i++){
			DocEntry de = docs.get(i);
			int clusterLabel = assign[de.id];
			medoids[clusterLabel].cardinality++;
			for(int j = 0; j < de.words.length; j++){
				medoids[clusterLabel].words[de.words[j].id] += de.words[j].weight;
			}
		}
		
		for(int i = 0; i < medoids.length; i++){
			for(int j = 0; j < medoids[i].words.length; j++){
				medoids[i].words[j] /= medoids[i].cardinality;
				medoids[i].sumOfSquare += Math.pow(medoids[i].words[j], 2);
			}
			medoids[i].sumOfSquare = Math.sqrt(medoids[i].sumOfSquare);
		}
				
	}
	
	public static void printResults() {
		HashMap<Integer, Integer> counter = new HashMap<>();
        for (int j : assign) {
            if (j == -1)
                continue;
            if (counter.containsKey(j)) {
                int c = counter.get(j);
                c++;
                counter.put(j, c);
            } else {
                counter.put(j, 1);
            }
        }

        for (int m : counter.keySet()) {
            int c = counter.get(m);
			logger.info("Medoid: {} - Members: {}", m, c);
        }
	}
	
}
