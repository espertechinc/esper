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
package com.espertech.esper.common.internal.compile.multikey;

import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;

import java.util.List;

public class MultiKeyPlan {
    private final List<StmtClassForgeableFactory> multiKeyForgeables;
    private final MultiKeyClassRef classRef;

    public MultiKeyPlan(List<StmtClassForgeableFactory> multiKeyForgeables, MultiKeyClassRef classRef) {
        this.multiKeyForgeables = multiKeyForgeables;
        this.classRef = classRef;
    }

    public List<StmtClassForgeableFactory> getMultiKeyForgeables() {
        return multiKeyForgeables;
    }

    public MultiKeyClassRef getClassRef() {
        return classRef;
    }
}
