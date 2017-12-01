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
package com.espertech.esper.epl.expression.methodagg;

import com.espertech.esper.client.hook.AggregationFunctionFactory;
import com.espertech.esper.epl.agg.service.common.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.common.AggregationValidationContext;
import com.espertech.esper.epl.expression.accessagg.ExprAggMultiFunctionUtil;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.baseagg.ExprAggregationPlugInNodeMarker;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Represents a custom aggregation function in an expresson tree.
 */
public class ExprPlugInAggNode extends ExprAggregateNodeBase implements ExprAggregationPlugInNodeMarker {
    private static final long serialVersionUID = 65459875362787079L;
    private transient AggregationFunctionFactory aggregationFunctionFactory;
    private final String functionName;

    /**
     * Ctor.
     *
     * @param distinct                   - flag indicating unique or non-unique value aggregation
     * @param aggregationFunctionFactory - is the base class for plug-in aggregation functions
     * @param functionName               is the aggregation function name
     */
    public ExprPlugInAggNode(boolean distinct, AggregationFunctionFactory aggregationFunctionFactory, String functionName) {
        super(distinct);
        this.aggregationFunctionFactory = aggregationFunctionFactory;
        this.functionName = functionName;
        aggregationFunctionFactory.setFunctionName(functionName);
    }

    public AggregationMethodFactory validateAggregationChild(ExprValidationContext validationContext) throws ExprValidationException {
        Class[] parameterTypes = new Class[positionalParams.length];
        Object[] constant = new Object[positionalParams.length];
        boolean[] isConstant = new boolean[positionalParams.length];
        ExprNode[] expressions = new ExprNode[positionalParams.length];

        int count = 0;
        boolean hasDataWindows = true;
        for (ExprNode child : positionalParams) {
            if (child.isConstantResult()) {
                isConstant[count] = true;
                constant[count] = child.getForge().getExprEvaluator().evaluate(null, true, validationContext.getExprEvaluatorContext());
            }
            parameterTypes[count] = child.getForge().getEvaluationType();
            expressions[count] = child;

            if (!ExprNodeUtilityRich.hasRemoveStreamForAggregations(child, validationContext.getStreamTypeService(), validationContext.isResettingAggregations())) {
                hasDataWindows = false;
            }

            if (child instanceof ExprWildcard) {
                ExprAggMultiFunctionUtil.checkWildcardNotJoinOrSubquery(validationContext.getStreamTypeService(), functionName);
                parameterTypes[count] = validationContext.getStreamTypeService().getEventTypes()[0].getUnderlyingType();
                isConstant[count] = false;
                constant[count] = null;
            }

            count++;
        }

        LinkedHashMap<String, List<ExprNode>> namedParameters = null;
        if (optionalFilter != null) {
            namedParameters = new LinkedHashMap<>();
            namedParameters.put("filter", Collections.singletonList(optionalFilter));
            positionalParams = ExprNodeUtilityCore.addExpression(positionalParams, optionalFilter);
        }

        AggregationValidationContext context = new AggregationValidationContext(parameterTypes, isConstant, constant, super.isDistinct(), hasDataWindows, expressions, namedParameters);
        try {
            // the aggregation function factory is transient, obtain if not provided
            if (aggregationFunctionFactory == null) {
                aggregationFunctionFactory = validationContext.getEngineImportService().resolveAggregationFactory(functionName);
            }

            aggregationFunctionFactory.validate(context);
        } catch (Exception ex) {
            throw new ExprValidationException("Plug-in aggregation function '" + functionName + "' failed validation: " + ex.getMessage(), ex);
        }

        Class childType = null;
        if (positionalParams.length > 0) {
            childType = positionalParams[0].getForge().getEvaluationType();
        }

        return validationContext.getEngineImportService().getAggregationFactoryFactory().makePlugInMethod(validationContext.getStatementExtensionSvcContext(), this, aggregationFunctionFactory, childType);
    }

    public String getAggregationFunctionName() {
        return functionName;
    }

    public final boolean equalsNodeAggregateMethodOnly(ExprAggregateNode node) {
        if (!(node instanceof ExprPlugInAggNode)) {
            return false;
        }

        ExprPlugInAggNode other = (ExprPlugInAggNode) node;
        return other.getAggregationFunctionName().equals(this.getAggregationFunctionName());
    }

    protected boolean isFilterExpressionAsLastParameter() {
        return false;
    }
}
