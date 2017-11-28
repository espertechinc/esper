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
package com.espertech.esper.view.stat;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.SingleEventIterator;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.view.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * View for computing statistics, which the view exposes via fields representing the sum, count, standard deviation
 * for sample and for population and variance.
 */
public class UnivariateStatisticsView extends ViewSupport implements DerivedValueView {
    private final UnivariateStatisticsViewFactory viewFactory;
    protected final AgentInstanceViewFactoryChainContext agentInstanceContext;
    protected final BaseStatisticsBean baseStatisticsBean = new BaseStatisticsBean();

    private EventBean lastNewEvent;
    private EventBean[] eventsPerStream = new EventBean[1];
    protected Object[] lastValuesEventNew;

    public UnivariateStatisticsView(UnivariateStatisticsViewFactory viewFactory, AgentInstanceViewFactoryChainContext agentInstanceContext) {
        this.viewFactory = viewFactory;
        this.agentInstanceContext = agentInstanceContext;
    }

    /**
     * Returns field name of the field to report statistics on.
     *
     * @return field name
     */
    public final ExprNode getFieldExpression() {
        return viewFactory.fieldExpression;
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qViewProcessIRStream(this, UnivariateStatisticsViewFactory.NAME, newData, oldData);
        }

        // If we have child views, keep a reference to the old values, so we can update them as old data event.
        EventBean oldDataMap = null;
        if (lastNewEvent == null) {
            if (this.hasViews()) {
                oldDataMap = populateMap(baseStatisticsBean, agentInstanceContext.getStatementContext().getEventAdapterService(), viewFactory.eventType, viewFactory.additionalProps, lastValuesEventNew);
            }
        }

        // add data points to the bean
        if (newData != null) {
            for (int i = 0; i < newData.length; i++) {
                eventsPerStream[0] = newData[i];
                Number pointnum = (Number) viewFactory.fieldExpressionEvaluator.evaluate(eventsPerStream, true, agentInstanceContext);
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
                Number pointnum = (Number) viewFactory.fieldExpressionEvaluator.evaluate(eventsPerStream, true, agentInstanceContext);
                if (pointnum != null) {
                    double point = pointnum.doubleValue();
                    baseStatisticsBean.removePoint(point, 0);
                }
            }
        }

        // If there are child view, call update method
        if (this.hasViews()) {
            EventBean newDataMap = populateMap(baseStatisticsBean, agentInstanceContext.getStatementContext().getEventAdapterService(), viewFactory.eventType, viewFactory.additionalProps, lastValuesEventNew);

            EventBean[] oldEvents;
            EventBean[] newEvents = new EventBean[]{newDataMap};
            if (lastNewEvent == null) {
                oldEvents = new EventBean[]{oldDataMap};
            } else {
                oldEvents = new EventBean[]{lastNewEvent};
            }

            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qViewIndicate(this, UnivariateStatisticsViewFactory.NAME, newEvents, oldEvents);
            }
            updateChildren(newEvents, oldEvents);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aViewIndicate();
            }

            lastNewEvent = newDataMap;
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aViewProcessIRStream();
        }
    }

    public final EventType getEventType() {
        return viewFactory.eventType;
    }

    public final Iterator<EventBean> iterator() {
        return new SingleEventIterator(populateMap(baseStatisticsBean,
                agentInstanceContext.getStatementContext().getEventAdapterService(),
                viewFactory.eventType,
                viewFactory.additionalProps, lastValuesEventNew));
    }

    public final String toString() {
        return this.getClass().getName() + " fieldExpression=" + viewFactory.fieldExpression;
    }

    public static EventBean populateMap(BaseStatisticsBean baseStatisticsBean,
                                        EventAdapterService eventAdapterService,
                                        EventType eventType,
                                        StatViewAdditionalProps additionalProps,
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

    public static EventType createEventType(StatementContext statementContext, StatViewAdditionalProps additionalProps, int streamNum) {
        Map<String, Object> eventTypeMap = new HashMap<String, Object>();
        eventTypeMap.put(ViewFieldEnum.UNIVARIATE_STATISTICS__DATAPOINTS.getName(), Long.class);
        eventTypeMap.put(ViewFieldEnum.UNIVARIATE_STATISTICS__TOTAL.getName(), Double.class);
        eventTypeMap.put(ViewFieldEnum.UNIVARIATE_STATISTICS__STDDEV.getName(), Double.class);
        eventTypeMap.put(ViewFieldEnum.UNIVARIATE_STATISTICS__STDDEVPA.getName(), Double.class);
        eventTypeMap.put(ViewFieldEnum.UNIVARIATE_STATISTICS__VARIANCE.getName(), Double.class);
        eventTypeMap.put(ViewFieldEnum.UNIVARIATE_STATISTICS__AVERAGE.getName(), Double.class);
        StatViewAdditionalProps.addCheckDupProperties(eventTypeMap, additionalProps,
                ViewFieldEnum.UNIVARIATE_STATISTICS__DATAPOINTS,
                ViewFieldEnum.UNIVARIATE_STATISTICS__TOTAL,
                ViewFieldEnum.UNIVARIATE_STATISTICS__STDDEV,
                ViewFieldEnum.UNIVARIATE_STATISTICS__STDDEVPA,
                ViewFieldEnum.UNIVARIATE_STATISTICS__VARIANCE,
                ViewFieldEnum.UNIVARIATE_STATISTICS__AVERAGE
        );
        String outputEventTypeName = statementContext.getStatementId() + "_statview_" + streamNum;
        return statementContext.getEventAdapterService().createAnonymousMapType(outputEventTypeName, eventTypeMap, false);
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
