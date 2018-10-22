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
package com.espertech.esper.common.internal.view.derived;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.SingleEventIterator;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.view.core.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * View for computing a weighted average. The view uses 2 fields within the parent view to compute the weighted average.
 * The X field and weight field. In a price-volume example it calculates the volume-weighted average price
 * as   (sum(price * volume) / sum(volume)).
 * Example: weighted_avg("price", "volume")
 */
public class WeightedAverageView extends ViewSupport implements DerivedValueView {
    private final WeightedAverageViewFactory viewFactory;
    private final AgentInstanceContext agentInstanceContext;

    private EventBean[] eventsPerStream = new EventBean[1];

    protected double sumXtimesW = Double.NaN;
    protected double sumW = Double.NaN;
    protected double currentValue = Double.NaN;
    protected Object[] lastValuesEventNew;

    private EventBean lastNewEvent;

    public WeightedAverageView(WeightedAverageViewFactory viewFactory, AgentInstanceViewFactoryChainContext agentInstanceContext) {
        this.viewFactory = viewFactory;
        this.agentInstanceContext = agentInstanceContext.getAgentInstanceContext();
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        agentInstanceContext.getAuditProvider().view(newData, oldData, agentInstanceContext, viewFactory);
        agentInstanceContext.getInstrumentationProvider().qViewProcessIRStream(viewFactory, newData, oldData);

        double oldValue = currentValue;

        // If we have child views, keep a reference to the old values, so we can update them as old data event.
        EventBean oldDataMap = null;
        if (lastNewEvent == null) {
            if (child != null) {
                Map<String, Object> oldDataValues = new HashMap<String, Object>();
                oldDataValues.put(ViewFieldEnum.WEIGHTED_AVERAGE__AVERAGE.getName(), oldValue);
                addProperties(oldDataValues);
                oldDataMap = agentInstanceContext.getEventBeanTypedEventFactory().adapterForTypedMap(oldDataValues, viewFactory.eventType);
            }
        }

        // add data points to the bean
        if (newData != null) {
            for (int i = 0; i < newData.length; i++) {
                eventsPerStream[0] = newData[i];
                Number pointnum = (Number) viewFactory.fieldNameXEvaluator.evaluate(eventsPerStream, true, agentInstanceContext);
                Number weightnum = (Number) viewFactory.fieldNameWeightEvaluator.evaluate(eventsPerStream, true, agentInstanceContext);
                if (pointnum != null && weightnum != null) {
                    double point = pointnum.doubleValue();
                    double weight = weightnum.doubleValue();

                    if (Double.valueOf(sumXtimesW).isNaN()) {
                        sumXtimesW = point * weight;
                        sumW = weight;
                    } else {
                        sumXtimesW += point * weight;
                        sumW += weight;
                    }
                }
            }

            if ((viewFactory.additionalProps != null) && (newData.length != 0)) {
                if (lastValuesEventNew == null) {
                    lastValuesEventNew = new Object[viewFactory.additionalProps.getAdditionalEvals().length];
                }
                for (int val = 0; val < viewFactory.additionalProps.getAdditionalEvals().length; val++) {
                    lastValuesEventNew[val] = viewFactory.additionalProps.getAdditionalEvals()[val].evaluate(eventsPerStream, true, agentInstanceContext);
                }
            }
        }

        // remove data points from the bean
        if (oldData != null) {
            for (int i = 0; i < oldData.length; i++) {
                eventsPerStream[0] = oldData[i];
                Number pointnum = (Number) viewFactory.fieldNameXEvaluator.evaluate(eventsPerStream, true, agentInstanceContext);
                Number weightnum = (Number) viewFactory.fieldNameWeightEvaluator.evaluate(eventsPerStream, true, agentInstanceContext);

                if (pointnum != null && weightnum != null) {
                    double point = pointnum.doubleValue();
                    double weight = weightnum.doubleValue();
                    sumXtimesW -= point * weight;
                    sumW -= weight;
                }
            }
        }

        if (sumW != 0) {
            currentValue = sumXtimesW / sumW;
        } else {
            currentValue = Double.NaN;
        }

        // If there are child view, fireStatementStopped update method
        if (child != null) {
            Map<String, Object> newDataMap = new HashMap<String, Object>();
            newDataMap.put(ViewFieldEnum.WEIGHTED_AVERAGE__AVERAGE.getName(), currentValue);
            addProperties(newDataMap);
            EventBean newDataEvent = agentInstanceContext.getEventBeanTypedEventFactory().adapterForTypedMap(newDataMap, viewFactory.eventType);

            EventBean[] newEvents = new EventBean[]{newDataEvent};
            EventBean[] oldEvents;
            if (lastNewEvent == null) {
                oldEvents = new EventBean[]{oldDataMap};
            } else {
                oldEvents = new EventBean[]{lastNewEvent};
            }

            agentInstanceContext.getInstrumentationProvider().qViewIndicate(viewFactory, newEvents, oldEvents);
            child.update(newEvents, oldEvents);
            agentInstanceContext.getInstrumentationProvider().aViewIndicate();

            lastNewEvent = newDataEvent;
        }

        agentInstanceContext.getInstrumentationProvider().aViewProcessIRStream();
    }

    private void addProperties(Map<String, Object> newDataMap) {
        if (viewFactory.additionalProps == null) {
            return;
        }
        viewFactory.additionalProps.addProperties(newDataMap, lastValuesEventNew);
    }

    public final EventType getEventType() {
        return viewFactory.eventType;
    }

    public final Iterator<EventBean> iterator() {
        Map<String, Object> newDataMap = new HashMap<String, Object>();
        newDataMap.put(ViewFieldEnum.WEIGHTED_AVERAGE__AVERAGE.getName(), currentValue);
        addProperties(newDataMap);
        return new SingleEventIterator(agentInstanceContext.getEventBeanTypedEventFactory().adapterForTypedMap(newDataMap, viewFactory.eventType));
    }

    public static EventType createEventType(StatViewAdditionalPropsForge additionalProps, ViewForgeEnv env, int streamNum) {
        LinkedHashMap<String, Object> schemaMap = new LinkedHashMap<String, Object>();
        schemaMap.put(ViewFieldEnum.WEIGHTED_AVERAGE__AVERAGE.getName(), Double.class);
        StatViewAdditionalPropsForge.addCheckDupProperties(schemaMap, additionalProps, ViewFieldEnum.WEIGHTED_AVERAGE__AVERAGE);
        return DerivedViewTypeUtil.newType("wavgview", schemaMap, env, streamNum);
    }

    public double getSumXtimesW() {
        return sumXtimesW;
    }

    public void setSumXtimesW(double sumXtimesW) {
        this.sumXtimesW = sumXtimesW;
    }

    public double getSumW() {
        return sumW;
    }

    public void setSumW(double sumW) {
        this.sumW = sumW;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }

    public Object[] getLastValuesEventNew() {
        return lastValuesEventNew;
    }

    public void setLastValuesEventNew(Object[] lastValuesEventNew) {
        this.lastValuesEventNew = lastValuesEventNew;
    }

    public ViewFactory getViewFactory() {
        return viewFactory;
    }
}
