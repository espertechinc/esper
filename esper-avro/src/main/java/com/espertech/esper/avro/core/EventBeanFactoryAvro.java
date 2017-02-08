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
package com.espertech.esper.avro.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventBeanFactory;
import com.espertech.esper.client.EventType;
import com.espertech.esper.event.EventAdapterService;
import org.apache.avro.generic.GenericData;

public class EventBeanFactoryAvro implements EventBeanFactory {
    private final EventType type;
    private final EventAdapterService eventAdapterService;

    public EventBeanFactoryAvro(EventType type, EventAdapterService eventAdapterService) {
        this.type = type;
        this.eventAdapterService = eventAdapterService;
    }

    public EventBean wrap(Object underlying) {
        return eventAdapterService.adapterForTypedAvro(underlying, type);
    }

    public Class getUnderlyingType() {
        return GenericData.Record[].class;
    }
}
