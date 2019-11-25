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
package com.espertech.esper.common.internal.epl.expression.agg.method;

import com.espertech.esper.common.client.hook.aggfunc.*;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.agg.method.plugin.AggregationForgeFactoryPlugin;
import com.espertech.esper.common.internal.epl.expression.agg.accessagg.ExprAggMultiFunctionUtil;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprPlugInAggNodeMarker;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Represents a custom aggregation function in an expresson tree.
 */
public class ExprPlugInAggNode extends ExprAggregateNodeBase implements ExprPlugInAggNodeMarker {
    private AggregationFunctionForge aggregationFunctionForge;
    private final String functionName;

    /**
     * Ctor.
     *
     * @param distinct                 - flag indicating unique or non-unique value aggregation
     * @param aggregationFunctionForge - is the base class for plug-in aggregation functions
     * @param functionName             is the aggregation function name
     */
    public ExprPlugInAggNode(boolean distinct, AggregationFunctionForge aggregationFunctionForge, String functionName) {
        super(distinct);
        this.aggregationFunctionForge = aggregationFunctionForge;
        this.functionName = functionName;
        aggregationFunctionForge.setFunctionName(functionName);
    }

    public AggregationForgeFactory validateAggregationChild(ExprValidationContext validationContext) throws ExprValidationException {
        Class[] parameterTypes = new Class[positionalParams.length];
        Object[] constant = new Object[positionalParams.length];
        boolean[] isConstant = new boolean[positionalParams.length];
        ExprNode[] expressions = new ExprNode[positionalParams.length];

        int count = 0;
        boolean hasDataWindows = true;
        for (ExprNode child : positionalParams) {
            if (child.getForge().getForgeConstantType() == ExprForgeConstantType.COMPILETIMECONST) {
                isConstant[count] = true;
                constant[count] = child.getForge().getExprEvaluator().evaluate(null, true, null);
            }
            parameterTypes[count] = child.getForge().getEvaluationType();
            expressions[count] = child;

            if (!ExprNodeUtilityAggregation.hasRemoveStreamForAggregations(child, validationContext.getStreamTypeService(), validationContext.isResettingAggregations())) {
                hasDataWindows = false;
            }

            if (child instanceof ExprWildcard && validationContext.getStreamTypeService().getEventTypes().length > 0) {
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
            positionalParams = ExprNodeUtilityMake.addExpression(positionalParams, optionalFilter);
        }

        AggregationFunctionValidationContext context = new AggregationFunctionValidationContext(parameterTypes, isConstant, constant, super.isDistinct(), hasDataWindows, expressions, namedParameters);
        try {
            // the aggregation function factory is transient, obtain if not provided
            if (aggregationFunctionForge == null) {
                aggregationFunctionForge = validationContext.getClasspathImportService().resolveAggregationFunction(functionName);
            }

            aggregationFunctionForge.validate(context);
        } catch (Exception ex) {
            throw new ExprValidationException("Plug-in aggregation function '" + functionName + "' failed validation: " + ex.getMessage(), ex);
        }

        AggregationFunctionMode mode = aggregationFunctionForge.getAggregationFunctionMode();
        if (mode == null) {
            throw new ExprValidationException("Aggregation function forge returned a null value for mode");
        }

        if (mode instanceof AggregationFunctionModeManaged) {
            if (positionalParams.length > 2) {
                throw new ExprValidationException("Aggregation function forge single-value mode requires zero, one or two parameters");
            }
        } else if (mode instanceof AggregationFunctionModeMultiParam || mode instanceof AggregationFunctionModeCodeGenerated) {
        } else {
            throw new ExprValidationException("Aggregation function forge returned an unrecognized mode " + mode);
        }

        Class aggregatedValueType = getPositionalParams().length == 0 ? null : getPositionalParams()[0].getForge().getEvaluationType();
        DataInputOutputSerdeForge distinctForge = isDistinct ? validationContext.getSerdeResolver().serdeForAggregationDistinct(aggregatedValueType, validationContext.getStatementRawInfo()) : null;
        return new AggregationForgeFactoryPlugin(this, aggregationFunctionForge, mode, aggregatedValueType, distinctForge);
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
