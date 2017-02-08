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
package com.espertech.esper.util;

import java.util.HashMap;
import java.util.Map;

public class LazyAllocatedMap<K, V> {
    private Map<K, V> inner;

    public synchronized Map<K, V> getMap() {
        if (inner == null) {
            inner = new HashMap<K, V>();
        }
        return inner;
    }
}
