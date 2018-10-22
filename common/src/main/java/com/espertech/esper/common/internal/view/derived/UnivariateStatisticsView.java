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
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.view.core.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * View for computing statistics, which the view exposes via fields representing the sum, count, standard deviation
 * for sample and for population and variance.
 */
public class UnivariateStatisticsView extends ViewSupport implements DerivedValueView {
    private final UnivariateStatisticsViewFactory viewFactory;
    protected final AgentInstanceContext agentInstanceContext;
    protected final BaseStatisticsBean baseStatisticsBean = new BaseStatisticsBean();

    private EventBean lastNewEvent;
    private EventBean[] eventsPerStream = new EventBean[1];
    protected Object[] lastValuesEventNew;

    public UnivariateStatisticsView(UnivariateStatisticsViewFactory viewFactory, AgentInstanceViewFactoryChainContext agentInstanceContext) {
        this.viewFactory = viewFactory;
        this.agentInstanceContext = agentInstanceContext.getAgentInstanceContext();
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        agentInstanceContext.getAuditProvider().view(newData, oldData, agentInstanceContext, viewFactory);
        agentInstanceContext.getInstrumentationProvider().qViewProcessIRStream(viewFactory, newData, oldData);

        // If we have child views, keep a reference to the old values, so we can update them as old data event.
        EventBean oldDataMap = null;
        if (lastNewEvent == null) {
            if (child != null) {
                oldDataMap = populateMap(baseStatisticsBean, agentInstanceContext.getEventBeanTypedEventFactory(), viewFactory.eventType, viewFactory.additionalProps, lastValuesEventNew);
            }
        }

        // add data points to the bean
        if (newData != null) {
            for (int i = 0; i < newData.length; i++) {
                eventsPerStream[0] = newData[i];
                Number pointnum = (Number) viewFactory.fieldEval.evaluate(eventsPerStream, true, agentInstanceContext);
                if (pointnum != null) {
                    double point = pointnum.doubleValue();
                    baseStatisticsBean.addPoint(point, 0);
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
                Number pointnum = (Number) viewFactory.fieldEval.evaluate(eventsPerStream, true, agentInstanceContext);
                if (pointnum != null) {
                    double point = pointnum.doubleValue();
                    baseStatisticsBean.removePoint(point, 0);
                }
            }
        }

        // If there are child view, call update method
        if (child != null) {
            EventBean newDataMap = populateMap(baseStatisticsBean, agentInstanceContext.getEventBeanTypedEventFactory(), viewFactory.eventType, viewFactory.additionalProps, lastValuesEventNew);

            EventBean[] oldEvents;
            EventBean[] newEvents = new EventBean[]{newDataMap};
            if (lastNewEvent == null) {
                oldEvents = new EventBean[]{oldDataMap};
            } else {
                oldEvents = new EventBean[]{lastNewEvent};
            }

            agentInstanceContext.getInstrumentationProvider().qViewIndicate(viewFactory, newEvents, oldEvents);
            child.update(newEvents, oldEvents);
            agentInstanceContext.getInstrumentationProvider().aViewIndicate();

            lastNewEvent = newDataMap;
        }

        agentInstanceContext.getInstrumentationProvider().aViewProcessIRStream();
    }

    public final EventType getEventType() {
        return viewFactory.eventType;
    }

    public final Iterator<EventBean> iterator() {
        return new SingleEventIterator(populateMap(baseStatisticsBean,
                agentInstanceContext.getEventBeanTypedEventFactory(),
                viewFactory.eventType,
                viewFactory.additionalProps, lastValuesEventNew));
    }

    public final String toString() {
        return this.getClass().getName();
    }

    public static EventBean populateMap(BaseStatisticsBean baseStatisticsBean,
                                        EventBeanTypedEventFactory eventAdapterService,
                                        EventType eventType,
                                        StatViewAdditionalPropsEval additionalProps,
                                        Object[] lastNewValues) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put(ViewFieldEnum.UNIVARIATE_STATISTICS__DATAPOINTS.getName(), baseStatisticsBean.getN());
        result.put(ViewFieldEnum.UNIVARIATE_STATISTICS__TOTAL.getName(), baseStatisticsBean.getXSum());
        result.put(ViewFieldEnum.UNIVARIATE_STATISTICS__STDDEV.getName(), baseStatisticsBean.getXStandardDeviationSample());
        result.put(ViewFieldEnum.UNIVARIATE_STATISTICS__STDDEVPA.getName(), baseStatisticsBean.getXStandardDeviationPop());
        result.put(ViewFieldEnum.UNIVARIATE_STATISTICS__VARIANCE.getName(), baseStatisticsBean.getXVariance());
        result.put(ViewFieldEnum.UNIVARIATE_STATISTICS__AVERAGE.getName(), baseStatisticsBean.getXAverage());
        if (additionalProps != null) {
            additionalProps.addProperties(result, lastNewValues);
        }
        return eventAdapterService.adapterForTypedMap(result, eventType);
    }

    public static EventType createEventType(StatViewAdditionalPropsForge additionalProps, ViewForgeEnv env, int streamNum) {
        LinkedHashMap<String, Object> eventTypeMap = new LinkedHashMap<String, Object>();
        eventTypeMap.put(ViewFieldEnum.UNIVARIATE_STATISTICS__DATAPOINTS.getName(), Long.class);
        eventTypeMap.put(ViewFieldEnum.UNIVARIATE_STATISTICS__TOTAL.getName(), Double.class);
        eventTypeMap.put(ViewFieldEnum.UNIVARIATE_STATISTICS__STDDEV.getName(), Double.class);
        eventTypeMap.put(ViewFieldEnum.UNIVARIATE_STATISTICS__STDDEVPA.getName(), Double.class);
        eventTypeMap.put(ViewFieldEnum.UNIVARIATE_STATISTICS__VARIANCE.getName(), Double.class);
        eventTypeMap.put(ViewFieldEnum.UNIVARIATE_STATISTICS__AVERAGE.getName(), Double.class);
        StatViewAdditionalPropsForge.addCheckDupProperties(eventTypeMap, additionalProps,
                ViewFieldEnum.UNIVARIATE_STATISTICS__DATAPOINTS,
                ViewFieldEnum.UNIVARIATE_STATISTICS__TOTAL,
                ViewFieldEnum.UNIVARIATE_STATISTICS__STDDEV,
                ViewFieldEnum.UNIVARIATE_STATISTICS__STDDEVPA,
                ViewFieldEnum.UNIVARIATE_STATISTICS__VARIANCE,
                ViewFieldEnum.UNIVARIATE_STATISTICS__AVERAGE
        );
        return DerivedViewTypeUtil.newType("statview", eventTypeMap, env, streamNum);
    }

    public BaseStatisticsBean getBaseStatisticsBean() {
        return baseStatisticsBean;
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
