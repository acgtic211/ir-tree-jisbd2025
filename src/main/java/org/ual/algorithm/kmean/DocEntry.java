package org.ual.algorithm.kmean;

public class DocEntry {
	int id;
	WordEntry[] words;
	double sumOfSquare = 0;
	
	DocEntry(int id, int size) {
		this.id = id;
		this.words = new WordEntry[size];
	}
	
	public void show(){
		System.out.println("Doc: " + id);
        for (WordEntry word : words) {
            System.out.print(word.id + " " + word.weight + ",");
        }
		System.out.println(" ");
	}

}
