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
package com.espertech.esper.common.internal.context.controller.initterm;

import com.espertech.esper.common.internal.collection.IntSeqKey;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ContextControllerInitTermDistinctSvcNested implements ContextControllerInitTermDistinctSvc {
    private final Map<IntSeqKey, Set<Object>> distinctContexts = new HashMap<>();

    public ContextControllerInitTermDistinctSvcNested() {
    }

    public boolean addUnlessExists(IntSeqKey controllerPath, Object key) {
        Set<Object> keys = distinctContexts.get(controllerPath);
        if (keys == null) {
            keys = new HashSet<>();
            distinctContexts.put(controllerPath, keys);
        }
        return keys.add(key);
    }

    public void remove(IntSeqKey controllerPath, Object key) {
        Set<Object> keys = distinctContexts.get(controllerPath);
        if (keys == null) {
            return;
        }
        keys.remove(key);
    }

    public void clear(IntSeqKey path) {
        distinctContexts.remove(path);
    }

    public void destroy() {
        distinctContexts.clear();
    }
}
