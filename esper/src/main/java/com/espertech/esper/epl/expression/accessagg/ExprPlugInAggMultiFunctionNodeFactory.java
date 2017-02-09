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
package com.espertech.esper.epl.expression.accessagg;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.service.StatementExtensionSvcContext;
import com.espertech.esper.epl.agg.access.AggregationAccessor;
import com.espertech.esper.epl.agg.access.AggregationAgent;
import com.espertech.esper.epl.agg.access.AggregationStateKey;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.factory.AggregationFactoryFactory;
import com.espertech.esper.epl.agg.service.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.AggregationMethodFactoryUtil;
import com.espertech.esper.epl.agg.service.AggregationStateFactory;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionAgentContext;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionHandler;

public class ExprPlugInAggMultiFunctionNodeFactory implements AggregationMethodFactory {
    private final ExprPlugInAggMultiFunctionNode parent;
    private final PlugInAggregationMultiFunctionHandler handlerPlugin;
    private final AggregationFactoryFactory aggregationFactoryFactory;
    private final StatementExtensionSvcContext statementExtensionSvcContext;
    private EPType returnType;

    public ExprPlugInAggMultiFunctionNodeFactory(ExprPlugInAggMultiFunctionNode parent, PlugInAggregationMultiFunctionHandler handlerPlugin, AggregationFactoryFactory aggregationFactoryFactory, StatementExtensionSvcContext statementExtensionSvcContext) {
        this.handlerPlugin = handlerPlugin;
        this.parent = parent;
        this.aggregationFactoryFactory = aggregationFactoryFactory;
        this.statementExtensionSvcContext = statementExtensionSvcContext;
    }

    public boolean isAccessAggregation() {
        return true;
    }

    public AggregationMethod make() {
        return null;
    }

    public AggregationStateKey getAggregationStateKey(boolean isMatchRecognize) {
        return handlerPlugin.getAggregationStateUniqueKey();
    }

    public AggregationStateFactory getAggregationStateFactory(boolean isMatchRecognize) {
        return aggregationFactoryFactory.makePlugInAccess(statementExtensionSvcContext, this);
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
