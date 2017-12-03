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
package com.espertech.esper.filterspec;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class MatchedEventMapMeta {
    private final static int MIN_MAP_LOOKUP = 3;

    private final String[] tagsPerIndex;
    private final boolean hasArrayProperties;
    private final Map<String, Integer> tagsPerIndexMap;

    public MatchedEventMapMeta(String[] tagsPerIndex, boolean hasArrayProperties) {
        this.tagsPerIndex = tagsPerIndex;
        this.hasArrayProperties = hasArrayProperties;
        this.tagsPerIndexMap = getMap(tagsPerIndex);
    }

    public MatchedEventMapMeta(Set<String> allTags, boolean hasArrayProperties) {
        this.tagsPerIndex = allTags.toArray(new String[allTags.size()]);
        this.hasArrayProperties = hasArrayProperties;
        this.tagsPerIndexMap = getMap(tagsPerIndex);
    }

    public String[] getTagsPerIndex() {
        return tagsPerIndex;
    }

    public int getTagFor(String key) {
        if (tagsPerIndexMap != null) {
            Integer result = tagsPerIndexMap.get(key);
            return result == null ? -1 : result;
        }
        for (int i = 0; i < tagsPerIndex.length; i++) {
            if (tagsPerIndex[i].equals(key)) {
                return i;
            }
        }
        return -1;
    }

    private Map<String, Integer> getMap(String[] tagsPerIndex) {
        if (tagsPerIndex.length < MIN_MAP_LOOKUP) {
            return null;
        }

        Map<String, Integer> map = new HashMap<String, Integer>();
        for (int i = 0; i < tagsPerIndex.length; i++) {
            map.put(tagsPerIndex[i], i);
        }
        return map;
    }

    public boolean isHasArrayProperties() {
        return hasArrayProperties;
    }
}
