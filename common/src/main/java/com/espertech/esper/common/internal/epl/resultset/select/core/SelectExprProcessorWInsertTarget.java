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
package com.espertech.esper.common.internal.epl.resultset.select.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;

import java.util.List;

public class SelectExprProcessorWInsertTarget {
    private final SelectExprProcessorForge forge;
    private final EventType insertIntoTargetType;
    private final List<StmtClassForgeableFactory> additionalForgeables;

    public SelectExprProcessorWInsertTarget(SelectExprProcessorForge forge, EventType insertIntoTargetType, List<StmtClassForgeableFactory> additionalForgeables) {
        this.forge = forge;
        this.insertIntoTargetType = insertIntoTargetType;
        this.additionalForgeables = additionalForgeables;
    }

    public SelectExprProcessorForge getForge() {
        return forge;
    }

    public EventType getInsertIntoTargetType() {
        return insertIntoTargetType;
    }

    public List<StmtClassForgeableFactory> getAdditionalForgeables() {
        return additionalForgeables;
    }
}
