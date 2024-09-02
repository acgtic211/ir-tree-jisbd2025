package org.ual.documentindex;

import java.util.Comparator;

public class InvertedListEntryComparator implements Comparator<InvertedListEntry> {
    @Override
    public int compare(InvertedListEntry n1, InvertedListEntry n2) {
        if (n1.term < n2.term) return -1;
        if (n1.term > n2.term) return 1;
        return 0;
    }
}
