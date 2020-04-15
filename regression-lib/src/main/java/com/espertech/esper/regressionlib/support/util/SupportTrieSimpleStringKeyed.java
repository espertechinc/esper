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
package com.espertech.esper.regressionlib.support.util;

import java.util.*;

public class SupportTrieSimpleStringKeyed<V> implements SupportTrie<String, V> {
    private final Map<String, V> simple = new HashMap<>();

    public V get(String key) {
        return simple.get(key);
    }

    public void put(String key, V value) {
        simple.put(key, value);
    }

    public void remove(String key) {
        simple.remove(key);
    }

    public void clear() {
        simple.clear();
    }

    public SortedMap<String, V> prefixMap(String key) {
        TreeMap<String, V> result = new TreeMap<>();
        for (Map.Entry<String, V> entry : simple.entrySet()) {
            if (entry.getKey().startsWith(key)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }
}

