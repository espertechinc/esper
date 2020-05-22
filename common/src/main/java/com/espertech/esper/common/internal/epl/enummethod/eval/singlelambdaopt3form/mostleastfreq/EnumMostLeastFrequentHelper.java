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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.mostleastfreq;

import java.util.Map;

public class EnumMostLeastFrequentHelper {

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param items        items
     * @param mostFrequent flag
     * @return value
     */
    public static Object getEnumMostLeastFrequentResult(Map<Object, Integer> items, boolean mostFrequent) {
        if (mostFrequent) {
            Object maxKey = null;
            int max = Integer.MIN_VALUE;
            for (Map.Entry<Object, Integer> entry : items.entrySet()) {
                if (entry.getValue() > max) {
                    maxKey = entry.getKey();
                    max = entry.getValue();
                }
            }
            return maxKey;
        }

        int min = Integer.MAX_VALUE;
        Object minKey = null;
        for (Map.Entry<Object, Integer> entry : items.entrySet()) {
            if (entry.getValue() < min) {
                minKey = entry.getKey();
                min = entry.getValue();
            }
        }
        return minKey;
    }
}
