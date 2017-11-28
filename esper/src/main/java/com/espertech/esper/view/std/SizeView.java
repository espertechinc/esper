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
package com.espertech.esper.view.std;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.SingleEventIterator;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.view.ViewFieldEnum;
import com.espertech.esper.view.ViewSupport;
import com.espertech.esper.view.stat.StatViewAdditionalProps;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This view is a very simple view presenting the number of elements in a stream or view.
 * The view computes a single long-typed count of the number of events passed through it similar
 * to the base statistics COUNT column.
 */
public class SizeView extends ViewSupport {
    private final AgentInstanceContext agentInstanceContext;
    private final EventType eventType;
    private final StatViewAdditionalProps additionalProps;

    protected long size = 0;
    private EventBean lastSizeEvent;
    protected Object[] lastValuesEventNew;

    public SizeView(AgentInstanceContext agentInstanceContext, EventType eventType, StatViewAdditionalProps additionalProps) {
        this.agentInstanceContext = agentInstanceContext;
        this.eventType = eventType;
        this.additionalProps = additionalProps;
    }

    public final EventType getEventType() {
        return eventType;
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qViewProcessIRStream(this, SizeViewFactory.NAME, newData, oldData);
        }
        long priorSize = size;

        // If we have child views, keep a reference to the old values, so we can update them as old data event.
        EventBean oldDataMap = null;
        if (lastSizeEvent == null) {
            if (this.hasViews()) {
                Map<String, Object> postOldData = new HashMap<String, Object>();
                postOldData.put(ViewFieldEnum.SIZE_VIEW__SIZE.getName(), priorSize);
                addProperties(postOldData);
                oldDataMap = agentInstanceContext.getStatementContext().getEventAdapterService().adapterForTypedMap(postOldData, eventType);
            }
        }

        // add data points to the window
        if (newData != null) {
            size += newData.length;

            if ((additionalProps != null) && (newData.length != 0)) {
                if (lastValuesEventNew == null) {
                    lastValuesEventNew = new Object[additionalProps.getAdditionalEvals().length];
                }
                for (int val = 0; val < additionalProps.getAdditionalEvals().length; val++) {
                    lastValuesEventNew[val] = additionalProps.getAdditionalEvals()[val].evaluate(new EventBean[]{newData[newData.length - 1]}, true, agentInstanceContext);
                }
            }
        }

        if (oldData != null) {
            size -= oldData.length;
        }

        // If there are child views, fireStatementStopped update method
        if ((this.hasViews()) && (priorSize != size)) {
            Map<String, Object> postNewData = new HashMap<String, Object>();
            postNewData.put(ViewFieldEnum.SIZE_VIEW__SIZE.getName(), size);
            addProperties(postNewData);
            EventBean newEvent = agentInstanceContext.getStatementContext().getEventAdapterService().adapterForTypedMap(postNewData, eventType);

            EventBean[] oldEvents;
            if (lastSizeEvent != null) {
                oldEvents = new EventBean[]{lastSizeEvent};
            } else {
                oldEvents = new EventBean[]{oldDataMap};
            }
            EventBean[] newEvents = new EventBean[]{newEvent};

            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qViewIndicate(this, SizeViewFactory.NAME, newEvents, oldEvents);
            }
            updateChildren(newEvents, oldEvents);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aViewIndicate();
            }

            lastSizeEvent = newEvent;
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aViewProcessIRStream();
        }
    }

    public final Iterator<EventBean> iterator() {
        HashMap<String, Object> current = new HashMap<String, Object>();
        current.put(ViewFieldEnum.SIZE_VIEW__SIZE.getName(), size);
        addProperties(current);
        return new SingleEventIterator(agentInstanceContext.getStatementContext().getEventAdapterService().adapterForTypedMap(current, eventType));
    }

    public final String toString() {
        return this.getClass().getName();
    }

    public static EventType createEventType(StatementContext statementContext, StatViewAdditionalProps additionalProps, int streamNum) {
        Map<String, Object> schemaMap = new HashMap<String, Object>();
        schemaMap.put(ViewFieldEnum.SIZE_VIEW__SIZE.getName(), long.class);
        StatViewAdditionalProps.addCheckDupProperties(schemaMap, additionalProps, ViewFieldEnum.SIZE_VIEW__SIZE);
        String outputEventTypeName = statementContext.getStatementId() + "_sizeview_" + streamNum;
        return statementContext.getEventAdapterService().createAnonymousMapType(outputEventTypeName, schemaMap, false);
    }

    private void addProperties(Map<String, Object> newDataMap) {
        if (additionalProps == null) {
            return;
        }
        additionalProps.addProperties(newDataMap, lastValuesEventNew);
    }
}
