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

import java.util.HashSet;
import java.util.Set;

public class ContextControllerInitTermDistinctSvcNonNested implements ContextControllerInitTermDistinctSvc {
    private final Set<Object> distinctContexts = new HashSet<>();

    public ContextControllerInitTermDistinctSvcNonNested() {
    }

    public boolean addUnlessExists(IntSeqKey controllerPath, Object key) {
        return distinctContexts.add(key);
    }

    public void remove(IntSeqKey controllerPath, Object key) {
        distinctContexts.remove(key);
    }

    public void clear(IntSeqKey path) {
        distinctContexts.clear();
    }

    public void destroy() {
        distinctContexts.clear();
    }
}
