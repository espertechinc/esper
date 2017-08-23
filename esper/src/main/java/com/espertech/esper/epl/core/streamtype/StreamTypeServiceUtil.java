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
package com.espertech.esper.epl.core.streamtype;

import com.espertech.esper.client.EventPropertyDescriptor;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.util.LevenshteinDistance;

public class StreamTypeServiceUtil {
    protected static Pair<Integer, String> findLevMatch(EventType[] eventTypes, String propertyName) {
        String bestMatch = null;
        int bestMatchDiff = Integer.MAX_VALUE;
        for (int i = 0; i < eventTypes.length; i++) {
            if (eventTypes[i] == null) {
                continue;
            }
            EventPropertyDescriptor[] props = eventTypes[i].getPropertyDescriptors();
            for (int j = 0; j < props.length; j++) {
                int diff = LevenshteinDistance.computeLevenshteinDistance(propertyName, props[j].getPropertyName());
                if (diff < bestMatchDiff) {
                    bestMatchDiff = diff;
                    bestMatch = props[j].getPropertyName();
                }
            }
        }

        if (bestMatchDiff < Integer.MAX_VALUE) {
            return new Pair<Integer, String>(bestMatchDiff, bestMatch);
        }
        return null;
    }

    protected static Pair<Integer, String> findLevMatch(String propertyName, EventType eventType) {
        String bestMatch = null;
        int bestMatchDiff = Integer.MAX_VALUE;
        EventPropertyDescriptor[] props = eventType.getPropertyDescriptors();
        for (int j = 0; j < props.length; j++) {
            int diff = LevenshteinDistance.computeLevenshteinDistance(propertyName, props[j].getPropertyName());
            if (diff < bestMatchDiff) {
                bestMatchDiff = diff;
                bestMatch = props[j].getPropertyName();
            }
        }

        if (bestMatchDiff < Integer.MAX_VALUE) {
            return new Pair<Integer, String>(bestMatchDiff, bestMatch);
        }
        return null;
    }
}
