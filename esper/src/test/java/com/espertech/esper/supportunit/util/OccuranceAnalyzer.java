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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.Pair;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OccuranceAnalyzer {
    public final static long RESOLUTION = 1000 * 1000 * 1000L;
    public final static long MSEC_DIVISIOR = 1000 * 1000L;

    public static OccuranceResult analyze(List<Pair<Long, EventBean[]>> occurances, long[] granularities) {
        Long low = Long.MAX_VALUE;
        Long high = Long.MIN_VALUE;
        int countTotal = 0;

        for (Pair<Long, EventBean[]> entry : occurances) {
            long time = entry.getFirst();
            if (time < low) {
                low = time;
            }
            if (time > high) {
                high = time;
            }
            countTotal += entry.getSecond().length;
        }

        List<OccuranceBucket> buckets = recursiveAnalyze(occurances, granularities, 0, low, high);
        return new OccuranceResult(occurances.size(), countTotal, low, high, RESOLUTION, buckets);
    }

    public static List<OccuranceBucket> recursiveAnalyze(List<Pair<Long, EventBean[]>> occurances, long[] granularities, int level, long start, long end) {
        // form buckets
        long granularity = granularities[level];
        Map<Integer, OccuranceIntermediate> intermediates = new LinkedHashMap<Integer, OccuranceIntermediate>();
        int countBucket = 0;
        for (long offset = start; offset < end; offset += granularity) {
            OccuranceIntermediate intermediate = new OccuranceIntermediate(offset, offset + granularity - 1);
            intermediates.put(countBucket, intermediate);
            countBucket++;
        }

        // sort into bucket
        for (Pair<Long, EventBean[]> entry : occurances) {
            long time = entry.getFirst();
            long delta = time - start;
            int bucket = (int) (delta / granularity);
            OccuranceIntermediate intermediate = intermediates.get(bucket);
            intermediate.getItems().add(entry);
        }

        // report each bucket
        List<OccuranceBucket> buckets = new ArrayList<OccuranceBucket>();
        for (Map.Entry<Integer, OccuranceIntermediate> pair : intermediates.entrySet()) {
            OccuranceIntermediate inter = pair.getValue();
            OccuranceBucket bucket = getBucket(inter);
            buckets.add(bucket);

            // for buckets within buckets
            if ((level < (granularities.length - 1) && (!inter.getItems().isEmpty()))) {
                bucket.setInnerBuckets(recursiveAnalyze(inter.getItems(), granularities, level + 1, inter.getLow(), inter.getHigh()));
            }
        }

        return buckets;
    }

    private static OccuranceBucket getBucket(OccuranceIntermediate inter) {
        int countTotal = 0;
        for (Pair<Long, EventBean[]> entry : inter.getItems()) {
            countTotal += entry.getSecond().length;
        }

        return new OccuranceBucket(inter.getLow(), inter.getHigh(), inter.getItems().size(), countTotal);
    }

}
