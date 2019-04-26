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
package com.espertech.esper.common.internal.epl.rowrecog.core;

import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;

import java.util.List;

public class RowRecogPlan {
    private final RowRecogDescForge forge;
    private final List<StmtClassForgeableFactory> additionalForgeables;

    public RowRecogPlan(RowRecogDescForge forge, List<StmtClassForgeableFactory> additionalForgeables) {
        this.forge = forge;
        this.additionalForgeables = additionalForgeables;
    }

    public RowRecogDescForge getForge() {
        return forge;
    }

    public List<StmtClassForgeableFactory> getAdditionalForgeables() {
        return additionalForgeables;
    }
}
