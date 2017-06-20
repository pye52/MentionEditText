package com.kanade.mentionedittext;

import android.support.annotation.NonNull;

import java.util.Comparator;

class Range implements Comparable<Range>{
    int id;
    String name;
    int from;
    int to;

    public Range(int id, String name, int from, int to) {
        this.id = id;
        this.name = name;
        this.from = from;
        this.to = to;
    }

    public boolean isWrapped(int start, int end) {
        return from >= start && to <= end;
    }

    public boolean isWrappedBy(int start, int end) {
        return (start > from && start < to) || (end > from && end < to);
    }

    public boolean contains(int start, int end) {
        return from <= start && to >= end;
    }

    public boolean isEqual(int start, int end) {
        return (from == start && to == end) || (from == end && to == start);
    }

    public int getAnchorPosition(int value) {
        if ((value - from) - (to - value) >= 0) {
            return to;
        } else {
            return from;
        }
    }

    public void setOffset(int offset) {
        from += offset;
        to += offset;
    }

    @Override
    public int compareTo(@NonNull Range o) {
        return from - o.from;
    }
}