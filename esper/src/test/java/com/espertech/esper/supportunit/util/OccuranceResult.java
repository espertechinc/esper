/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.supportunit.util;

import java.util.List;

public class OccuranceResult {
    private int countEntry;
    private int countTotal;
    private Long low;
    private Long high;
    private long resolution;
    private List<OccuranceBucket> buckets;

    public OccuranceResult(int countEntry, int countTotal, Long low, Long high, long resolution, List<OccuranceBucket> buckets) {
        this.countEntry = countEntry;
        this.countTotal = countTotal;
        this.low = low;
        this.high = high;
        this.resolution = resolution;
        this.buckets = buckets;
    }

    public int getCountEntry() {
        return countEntry;
    }

    public int getCountTotal() {
        return countTotal;
    }

    public Long getLow() {
        return low;
    }

    public Long getHigh() {
        return high;
    }

    public List<OccuranceBucket> getBuckets() {
        return buckets;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("Total " + countTotal + " entries " + countEntry);
        buf.append("\n");

        int count = 0;
        for (OccuranceBucket bucket : buckets) {
            render(buf, 0, Integer.toString(count), low, bucket);
        }

        return buf.toString();
    }

    private static void render(StringBuilder buf, int indent, String identifier, long start, OccuranceBucket bucket) {
        double lowRelative = (bucket.getLow() - start) / 1d / OccuranceAnalyzer.MSEC_DIVISIOR;
        double highRelative = (bucket.getHigh() - start) / 1d / OccuranceAnalyzer.MSEC_DIVISIOR;

        addIndent(buf, indent);
        buf.append(identifier);
        buf.append(" ");
        buf.append("[").append(lowRelative).append(", ").append(highRelative).append("]");
        buf.append(" ");
        buf.append(bucket.getCountTotal()).append(" entries ").append(bucket.getCountEntry());
        buf.append("\n");

        int count = 0;
        for (OccuranceBucket inner : bucket.getInnerBuckets()) {
            render(buf, indent + 1, identifier + "." + count, bucket.getLow(), inner);
            count++;
        }
    }

    private static void addIndent(StringBuilder buf, int indent) {
        for (int i = 0; i < indent; i++) {
            buf.append(" ");
            buf.append(" ");
        }
    }
}
