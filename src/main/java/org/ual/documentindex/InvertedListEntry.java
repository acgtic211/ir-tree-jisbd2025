package org.ual.documentindex;

import java.util.ArrayList;

/**
 * Contains the term and an array of PlEntries
 */
public class InvertedListEntry {
    int term;
    ArrayList<PlEntry> pl; // Posting List

    public InvertedListEntry(int term) {
        this.term = term;
        this.pl = new ArrayList<>();
    }

    public void add(PlEntry plEntry) {
        pl.add(plEntry);
    }
}
