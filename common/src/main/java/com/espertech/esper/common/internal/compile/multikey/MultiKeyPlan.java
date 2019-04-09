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

import com.espertech.esper.common.internal.compile.stage3.StmtClassForgableFactory;

import java.util.List;

public class MultiKeyPlan {
    private final List<StmtClassForgableFactory> multiKeyForgables;
    private final MultiKeyClassRef optionalClassRef;

    public MultiKeyPlan(List<StmtClassForgableFactory> multiKeyForgables, MultiKeyClassRef optionalClassRef) {
        this.multiKeyForgables = multiKeyForgables;
        this.optionalClassRef = optionalClassRef;
    }

    public List<StmtClassForgableFactory> getMultiKeyForgables() {
        return multiKeyForgables;
    }

    public MultiKeyClassRef getOptionalClassRef() {
        return optionalClassRef;
    }
}
