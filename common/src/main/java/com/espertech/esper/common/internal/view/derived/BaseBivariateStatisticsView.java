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
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.view.core.DerivedValueView;
import com.espertech.esper.common.internal.view.core.ViewFactory;
import com.espertech.esper.common.internal.view.core.ViewSupport;

import java.util.Iterator;

/**
 * View for computing statistics that require 2 input variable arrays containing X and Y datapoints.
 * Subclasses compute correlation or regression values, for instance.
 */
public abstract class BaseBivariateStatisticsView extends ViewSupport implements DerivedValueView {
    protected final ViewFactory viewFactory;
    /**
     * This bean can be overridden by subclasses providing extra values such as correlation, regression.
     */
    protected BaseStatisticsBean statisticsBean = new BaseStatisticsBean();

    private final ExprEvaluator expressionXEval;
    private final ExprEvaluator expressionYEval;
    private final EventBean[] eventsPerStream = new EventBean[1];

    /**
     * Services required by implementing classes.
     */
    protected final AgentInstanceContext agentInstanceContext;

    /**
     * Additional properties.
     */
    protected final StatViewAdditionalPropsEval additionalProps;

    /**
     * Event type.
     */
    protected final EventType eventType;

    protected Object[] lastValuesEventNew;
    private EventBean lastNewEvent;

    /**
     * Populate bean.
     *
     * @param baseStatisticsBean  results
     * @param eventAdapterService event adapters
     * @param eventType           type
     * @param additionalProps     additional props
     * @param decoration          decoration values
     * @return bean
     */
    protected abstract EventBean populateMap(BaseStatisticsBean baseStatisticsBean, EventBeanTypedEventFactory eventAdapterService,
                                             EventType eventType, StatViewAdditionalPropsEval additionalProps, Object[] decoration);

    /**
     * Constructor requires the name of the two fields to use in the parent view to compute the statistics.
     *
     * @param expressionXEval      is the expression to get the X values from
     * @param expressionYEval      is the expression to get the Y values from
     * @param agentInstanceContext contains required view services
     * @param eventType            type of event
     * @param additionalProps      additional props
     * @param viewFactory          view factory
     */
    public BaseBivariateStatisticsView(ViewFactory viewFactory,
                                       AgentInstanceContext agentInstanceContext,
                                       ExprEvaluator expressionXEval,
                                       ExprEvaluator expressionYEval,
                                       EventType eventType,
                                       StatViewAdditionalPropsEval additionalProps
    ) {
        this.viewFactory = viewFactory;
        this.agentInstanceContext = agentInstanceContext;
        this.expressionXEval = expressionXEval;
        this.expressionYEval = expressionYEval;
        this.eventType = eventType;
        this.additionalProps = additionalProps;
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        agentInstanceContext.getAuditProvider().view(newData, oldData, agentInstanceContext, viewFactory);
        agentInstanceContext.getInstrumentationProvider().qViewProcessIRStream(viewFactory, newData, oldData);

        // If we have child views, keep a reference to the old values, so we can fireStatementStopped them as old data event.
        EventBean oldValues = null;
        if (lastNewEvent == null) {
            if (child != null) {
                oldValues = populateMap(statisticsBean, agentInstanceContext.getEventBeanTypedEventFactory(), eventType, additionalProps, lastValuesEventNew);
            }
        }

        // add data points to the bean
        if (newData != null) {
            for (int i = 0; i < newData.length; i++) {
                eventsPerStream[0] = newData[i];
                Number xnum = (Number) expressionXEval.evaluate(eventsPerStream, true, agentInstanceContext);
                Number ynum = (Number) expressionYEval.evaluate(eventsPerStream, true, agentInstanceContext);
                if (xnum != null && ynum != null) {
                    double x = xnum.doubleValue();
                    double y = ynum.doubleValue();
                    statisticsBean.addPoint(x, y);
                }
            }

            if ((additionalProps != null) && (newData.length != 0)) {
                if (lastValuesEventNew == null) {
                    lastValuesEventNew = new Object[additionalProps.getAdditionalEvals().length];
                }
                for (int val = 0; val < additionalProps.getAdditionalEvals().length; val++) {
                    lastValuesEventNew[val] = additionalProps.getAdditionalEvals()[val].evaluate(eventsPerStream, true, agentInstanceContext);
                }
            }
        }

        // remove data points from the bean
        if (oldData != null) {
            for (int i = 0; i < oldData.length; i++) {
                eventsPerStream[0] = oldData[i];
                Number xnum = (Number) expressionXEval.evaluate(eventsPerStream, true, agentInstanceContext);
                Number ynum = (Number) expressionYEval.evaluate(eventsPerStream, true, agentInstanceContext);
                if (xnum != null && ynum != null) {
                    double x = xnum.doubleValue();
                    double y = ynum.doubleValue();
                    statisticsBean.removePoint(x, y);
                }
            }
        }

        // If there are child view, fireStatementStopped update method
        if (child != null) {
            EventBean newDataMap = populateMap(statisticsBean, agentInstanceContext.getEventBeanTypedEventFactory(), eventType, additionalProps, lastValuesEventNew);
            EventBean[] newEvents = new EventBean[]{newDataMap};
            EventBean[] oldEvents;
            if (lastNewEvent == null) {
                oldEvents = new EventBean[]{oldValues};
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

    public final Iterator<EventBean> iterator() {
        return new SingleEventIterator(populateMap(statisticsBean,
                agentInstanceContext.getEventBeanTypedEventFactory(),
                eventType, additionalProps, lastValuesEventNew));
    }

    public BaseStatisticsBean getStatisticsBean() {
        return statisticsBean;
    }

    public Object[] getLastValuesEventNew() {
        return lastValuesEventNew;
    }

    public void setLastValuesEventNew(Object[] lastValuesEventNew) {
        this.lastValuesEventNew = lastValuesEventNew;
    }

    public StatViewAdditionalPropsEval getAdditionalProps() {
        return additionalProps;
    }

    public ViewFactory getViewFactory() {
        return viewFactory;
    }

    public ExprEvaluator getExpressionXEval() {
        return expressionXEval;
    }

    public ExprEvaluator getExpressionYEval() {
        return expressionYEval;
    }
}
