package org.ual.algorithm.kmean;

public class MedoidEntry {
	int id;
	double[] words;
	
	int cardinality = 0;
	double sumOfSquare = 0;
	
	MedoidEntry(int id, int size) {
		this.id = id;
		this.words = new double[size];
	}

}
