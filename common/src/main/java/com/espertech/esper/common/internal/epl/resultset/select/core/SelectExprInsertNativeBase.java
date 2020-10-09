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
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.event.core.EventBeanManufacturerForge;
import com.espertech.esper.common.internal.util.TypeWidenerSPI;

public abstract class SelectExprInsertNativeBase implements SelectExprProcessorForge {

    private final EventType eventType;
    protected final EventBeanManufacturerForge eventManufacturer;
    protected final ExprForge[] exprForges;

    protected SelectExprInsertNativeBase(EventType eventType, EventBeanManufacturerForge eventManufacturer, ExprForge[] exprForges) {
        this.eventType = eventType;
        this.eventManufacturer = eventManufacturer;
        this.exprForges = exprForges;
    }

    public EventType getResultEventType() {
        return eventType;
    }

    public static SelectExprInsertNativeBase makeInsertNative(EventType eventType, EventBeanManufacturerForge eventManufacturer, ExprForge[] exprForges, TypeWidenerSPI[] wideners) {
        boolean hasWidener = false;
        for (TypeWidenerSPI widener : wideners) {
            if (widener != null) {
                hasWidener = true;
                break;
            }
        }
        if (!hasWidener) {
            return new SelectExprInsertNativeNoWiden(eventType, eventManufacturer, exprForges);
        }
        return new SelectExprInsertNativeWidening(eventType, eventManufacturer, exprForges, wideners);
    }
}
