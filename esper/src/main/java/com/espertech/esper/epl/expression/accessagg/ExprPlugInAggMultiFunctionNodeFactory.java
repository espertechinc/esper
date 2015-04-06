/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.expression.accessagg;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.agg.access.AggregationAccessor;
import com.espertech.esper.epl.agg.access.AggregationAgent;
import com.espertech.esper.epl.agg.access.AggregationStateKey;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.AggregationMethodFactoryUtil;
import com.espertech.esper.epl.agg.service.AggregationStateFactory;
import com.espertech.esper.epl.agg.service.AggregationStateFactoryPlugin;
import com.espertech.esper.epl.core.MethodResolutionService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionAgentContext;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionHandler;

public class ExprPlugInAggMultiFunctionNodeFactory implements AggregationMethodFactory
{
    private final ExprPlugInAggMultiFunctionNode parent;
    private final PlugInAggregationMultiFunctionHandler handlerPlugin;
    private EPType returnType;

    public ExprPlugInAggMultiFunctionNodeFactory(ExprPlugInAggMultiFunctionNode parent, PlugInAggregationMultiFunctionHandler handlerPlugin) {
        this.handlerPlugin = handlerPlugin;
        this.parent = parent;
    }

    public boolean isAccessAggregation() {
        return true;
    }

    public AggregationMethod make(MethodResolutionService methodResolutionService, int agentInstanceId, int groupId, int aggregationId) {
        return null;
    }

    public AggregationStateKey getAggregationStateKey(boolean isMatchRecognize) {
        return handlerPlugin.getAggregationStateUniqueKey();
    }

    public AggregationStateFactory getAggregationStateFactory(boolean isMatchRecognize) {
        return new AggregationStateFactoryPlugin(this);
    }

    public AggregationAccessor getAccessor() {
        return handlerPlugin.getAccessor();
    }

    public Class getResultType() {
        obtainReturnType();
        return EPTypeHelper.getNormalizedClass(returnType);
    }

    public PlugInAggregationMultiFunctionHandler getHandlerPlugin() {
        return handlerPlugin;
    }

    public Class getComponentTypeCollection() {
        obtainReturnType();
        return EPTypeHelper.getClassMultiValued(returnType);
    }

    public EventType getEventTypeSingle() {
        obtainReturnType();
        return EPTypeHelper.getEventTypeSingleValued(returnType);
    }

    public EventType getEventTypeCollection() {
        obtainReturnType();
        return EPTypeHelper.getEventTypeMultiValued(returnType);
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    private void obtainReturnType() {
        if (returnType == null) {
            returnType = handlerPlugin.getReturnType();
        }
    }

    public void validateIntoTableCompatible(AggregationMethodFactory intoTableAgg) throws ExprValidationException {
        AggregationMethodFactoryUtil.validateAggregationType(this, intoTableAgg);
        ExprPlugInAggMultiFunctionNodeFactory that = (ExprPlugInAggMultiFunctionNodeFactory) intoTableAgg;
        if (!getAggregationStateKey(false).equals(that.getAggregationStateKey(false))) {
            throw new ExprValidationException("Mismatched state key");
        }
    }

    public AggregationAgent getAggregationStateAgent() {
        PlugInAggregationMultiFunctionAgentContext ctx = new PlugInAggregationMultiFunctionAgentContext(parent.getChildNodes());
        return handlerPlugin.getAggregationAgent(ctx);
    }

    public ExprEvaluator getMethodAggregationEvaluator(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return null;
    }
}
