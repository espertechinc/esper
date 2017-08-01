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
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.view.DerivedValueView;
import com.espertech.esper.view.ViewFactory;
import com.espertech.esper.view.ViewSupport;

import java.util.Iterator;

/**
 * View for computing statistics that require 2 input variable arrays containing X and Y datapoints.
 * Subclasses compute correlation or regression values, for instance.
 */
public abstract class BaseBivariateStatisticsView extends ViewSupport implements DerivedValueView {
    private final static String NAME = "Statistics";

    protected final ViewFactory viewFactory;
    /**
     * This bean can be overridden by subclasses providing extra values such as correlation, regression.
     */
    protected BaseStatisticsBean statisticsBean = new BaseStatisticsBean();

    private final ExprNode expressionX;
    private final ExprNode expressionY;
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
    protected final StatViewAdditionalProps additionalProps;

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
    protected abstract EventBean populateMap(BaseStatisticsBean baseStatisticsBean, EventAdapterService eventAdapterService,
                                             EventType eventType, StatViewAdditionalProps additionalProps, Object[] decoration);

    /**
     * Constructor requires the name of the two fields to use in the parent view to compute the statistics.
     *
     * @param expressionX          is the expression to get the X values from
     * @param expressionXEval      is the expression to get the X values from
     * @param expressionY          is the expression to get the Y values from
     * @param expressionYEval      is the expression to get the Y values from
     * @param agentInstanceContext contains required view services
     * @param eventType            type of event
     * @param additionalProps      additional props
     * @param viewFactory          view factory
     */
    public BaseBivariateStatisticsView(ViewFactory viewFactory,
                                       AgentInstanceContext agentInstanceContext,
                                       ExprNode expressionX,
                                       ExprEvaluator expressionXEval,
                                       ExprNode expressionY,
                                       ExprEvaluator expressionYEval,
                                       EventType eventType,
                                       StatViewAdditionalProps additionalProps
    ) {
        this.viewFactory = viewFactory;
        this.agentInstanceContext = agentInstanceContext;
        this.expressionX = expressionX;
        this.expressionXEval = expressionXEval;
        this.expressionY = expressionY;
        this.expressionYEval = expressionYEval;
        this.eventType = eventType;
        this.additionalProps = additionalProps;
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qViewProcessIRStream(this, NAME, newData, oldData);
        }

        // If we have child views, keep a reference to the old values, so we can fireStatementStopped them as old data event.
        EventBean oldValues = null;
        if (lastNewEvent == null) {
            if (this.hasViews()) {
                oldValues = populateMap(statisticsBean, agentInstanceContext.getStatementContext().getEventAdapterService(), eventType, additionalProps, lastValuesEventNew);
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
        if (this.hasViews()) {
            EventBean newDataMap = populateMap(statisticsBean, agentInstanceContext.getStatementContext().getEventAdapterService(), eventType, additionalProps, lastValuesEventNew);
            EventBean[] newEvents = new EventBean[]{newDataMap};
            EventBean[] oldEvents;
            if (lastNewEvent == null) {
                oldEvents = new EventBean[]{oldValues};
            } else {
                oldEvents = new EventBean[]{lastNewEvent};
            }

            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qViewIndicate(this, NAME, newEvents, oldEvents);
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

    public final Iterator<EventBean> iterator() {
        return new SingleEventIterator(populateMap(statisticsBean,
                agentInstanceContext.getStatementContext().getEventAdapterService(),
                eventType, additionalProps, lastValuesEventNew));
    }

    /**
     * Returns the expression supplying X data points.
     *
     * @return X expression
     */
    public final ExprNode getExpressionX() {
        return expressionX;
    }

    /**
     * Returns the expression supplying Y data points.
     *
     * @return Y expression
     */
    public final ExprNode getExpressionY() {
        return expressionY;
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

    public StatViewAdditionalProps getAdditionalProps() {
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
