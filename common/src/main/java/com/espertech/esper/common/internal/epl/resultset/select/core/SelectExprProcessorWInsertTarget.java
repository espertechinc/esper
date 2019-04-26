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

public class SelectExprProcessorWInsertTarget {
    private final SelectExprProcessorForge forge;
    private final EventType insertIntoTargetType;

    public SelectExprProcessorWInsertTarget(SelectExprProcessorForge forge, EventType insertIntoTargetType) {
        this.forge = forge;
        this.insertIntoTargetType = insertIntoTargetType;
    }

    public SelectExprProcessorForge getForge() {
        return forge;
    }

    public EventType getInsertIntoTargetType() {
        return insertIntoTargetType;
    }
}
