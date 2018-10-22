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
import com.espertech.esper.common.internal.event.avro.EventTypeAvroHandler;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;

public class SelectExprForgeContext {
    private final ExprForge[] exprForges;
    private final String[] columnNames;
    private final EventBeanTypedEventFactory eventBeanTypedEventFactory;
    private final EventType[] eventTypes;
    private final EventTypeAvroHandler eventTypeAvroHandler;

    public SelectExprForgeContext(ExprForge[] exprForges, String[] columnNames, EventBeanTypedEventFactory eventBeanTypedEventFactory, EventType[] eventTypes, EventTypeAvroHandler eventTypeAvroHandler) {
        this.exprForges = exprForges;
        this.columnNames = columnNames;
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
        this.eventTypes = eventTypes;
        this.eventTypeAvroHandler = eventTypeAvroHandler;
    }

    public ExprForge[] getExprForges() {
        return exprForges;
    }

    public String[] getColumnNames() {
        return columnNames;
    }

    public EventBeanTypedEventFactory getEventBeanTypedEventFactory() {
        return eventBeanTypedEventFactory;
    }

    public int getNumStreams() {
        return eventTypes.length;
    }

    public EventType[] getEventTypes() {
        return eventTypes;
    }

    public EventTypeAvroHandler getEventTypeAvroHandler() {
        return eventTypeAvroHandler;
    }
}
