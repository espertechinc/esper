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
package com.espertech.esper.example.benchmark.server;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A Stats instance gathers percentile based on a given histogram
 * This class is thread unsafe.
 *
 * @author Alexandre Vasseur http://avasseur.blogspot.com
 * @see com.espertech.esper.example.benchmark.server.StatsHolder for thread safe access
 * Use createAndMergeFrom(proto) for best effort merge of this instance into the proto instance
 * (no read / write lock is performed so the actual counts are a best effort)
 */
public class Stats {

    private AtomicBoolean mustReset = new AtomicBoolean(false);

    final public String name;
    final public String unit;
    private long count;
    private double avg;

    private int[] histogram;
    private long[] counts;

    public Stats(String name, String unit, int... hists) { //10, 20, (20+ implicit)
        this.name = name;
        this.unit = unit;
        histogram = new int[hists.length + 1]; //we add one slot for the implicit 20+
        System.arraycopy(hists, 0, histogram, 0, hists.length);
        histogram[histogram.length - 1] = hists[hists.length - 1] + 1;
        counts = new long[histogram.length];
        for (int i = 0; i < counts.length; i++)
            counts[i] = 0;
    }

    /**
     * Use this method to merge this stat instance into a prototype one (for thread safe read only snapshoting)
     */
    public static Stats createAndMergeFrom(Stats model) {
        Stats r = new Stats(model.name, model.unit, 0);
        r.histogram = new int[model.histogram.length];
        System.arraycopy(model.histogram, 0, r.histogram, 0, model.histogram.length);
        r.counts = new long[model.histogram.length];

        r.merge(model);
        return r;
    }

    public void update(long ns) {
        if (mustReset.compareAndSet(true, false))
            internal_reset();

        count++;
        avg = (avg * (count - 1) + ns) / count;
        if (ns >= histogram[histogram.length - 1]) {
            counts[counts.length - 1]++;
        } else {
            int index = 0;
            for (int level : histogram) {
                if (ns < level) {
                    counts[index]++;
                    break;
                }
                index++;
            }
        }
    }

    public void dump() {
        System.out.println("---Stats - " + name + " (unit: " + unit + ")");
        System.out.printf("  Avg: %.0f #%d\n", avg, count);
        int index = 0;
        long lastLevel = 0;
        long occurCumul = 0;
        for (long occur : counts) {
            occurCumul += occur;
            if (index != counts.length - 1) {
                System.out.printf("  %7d < %7d: %6.2f%% %6.2f%% #%d\n",
                        lastLevel, histogram[index], (float) occur / count * 100,
                        (float) occurCumul / count * 100, occur);
                lastLevel = histogram[index];
            } else {
                System.out.printf("  %7d <    more: %6.2f%% %6.2f%% #%d\n", lastLevel, (float) occur / count * 100, 100f, occur);
            }
            index++;
        }
    }

    public void merge(Stats stats) {
        // we assume same histogram - no check done here
        count += stats.count;
        avg = ((avg * count) + (stats.avg * stats.count)) / (count + stats.count);
        for (int i = 0; i < counts.length; i++) {
            counts[i] += stats.counts[i];
        }
    }

    private void internal_reset() {
        count = 0;
        avg = 0;
        for (int i = 0; i < counts.length; i++)
            counts[i] = 0;
    }

    public void reset() {
        mustReset.set(true);
    }

    public static void main(String[] args) {
        Stats stats = new Stats("a", "any", 10, 20);
        stats.update(1);
        stats.update(2);
        stats.update(10);
        stats.update(15);
        stats.update(25);
        //stats.dump();

        Stats stats2 = new Stats("b", "any", 10, 20);
        stats2.update(1);
        stats.merge(stats2);
        stats.dump();

        long l = 100;
        long l2 = 3;
        System.out.println("" + (float) l / l2);
        System.out.printf("%15.4f", (float) l / l2);
    }
}
