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
package com.espertech.esper.common.internal.compile.stage2;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.ops.ExprBetweenNode;
import com.espertech.esper.common.internal.epl.pattern.core.MatchedEventConvertorForge;
import com.espertech.esper.common.internal.filterspec.*;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;
import com.espertech.esper.common.internal.util.SimpleNumberCoercerFactory;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import static com.espertech.esper.common.internal.compile.stage2.FilterSpecCompilerIndexPlannerHelper.*;

public class FilterSpecCompilerIndexPlannerRange {
    public static FilterSpecParamForge handleRangeNode(ExprBetweenNode betweenNode, LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, LinkedHashSet<String> allTagNamesOrdered, String statementName, boolean advancedPlanning, StatementRawInfo raw, StatementCompileTimeServices services) throws ExprValidationException {
        ExprNode left = betweenNode.getChildNodes()[0];
        ExprFilterSpecLookupableFactoryForge lookupable = null;

        if (left instanceof ExprFilterOptimizableNode) {
            ExprFilterOptimizableNode filterOptimizableNode = (ExprFilterOptimizableNode) left;
            lookupable = filterOptimizableNode.getFilterLookupable();
        } else if (advancedPlanning && isLimitedLookupableExpression(left)) {
            lookupable = makeLimitedLookupableForgeMayNull(left, raw, services);
        }
        if (lookupable == null) {
            return null;
        }

        FilterOperator op = FilterOperator.parseRangeOperator(betweenNode.isLowEndpointIncluded(), betweenNode.isHighEndpointIncluded(), betweenNode.isNotBetween());

        FilterSpecParamFilterForEvalForge low = handleRangeNodeEndpoint(betweenNode.getChildNodes()[1], taggedEventTypes, arrayEventTypes, allTagNamesOrdered, statementName, advancedPlanning);
        FilterSpecParamFilterForEvalForge high = handleRangeNodeEndpoint(betweenNode.getChildNodes()[2], taggedEventTypes, arrayEventTypes, allTagNamesOrdered, statementName, advancedPlanning);
        return low == null || high == null ? null :  new FilterSpecParamRangeForge(lookupable, op, low, high);
    }

    private static FilterSpecParamFilterForEvalForge handleRangeNodeEndpoint(ExprNode endpoint, LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, LinkedHashSet<String> allTagNamesOrdered, String statementName, boolean advancedPlanning) throws ExprValidationException {
        // constant
        if (endpoint.getForge().getForgeConstantType().isCompileTimeConstant()) {
            Object value = endpoint.getForge().getExprEvaluator().evaluate(null, true, null);
            if (value == null) {
                return null;
            }
            if (value instanceof String) {
                return new FilterForEvalConstantStringForge((String) value);
            } else {
                return new FilterForEvalConstantDoubleForge(((Number) value).doubleValue());
            }
        }

        if (endpoint instanceof ExprContextPropertyNode) {
            ExprContextPropertyNode node = (ExprContextPropertyNode) endpoint;
            if (JavaClassHelper.isImplementsCharSequence(node.getType())) {
                return new FilterForEvalContextPropStringForge(node.getGetter(), node.getPropertyName());
            } else {
                return new FilterForEvalContextPropDoubleForge(node.getGetter(), node.getPropertyName());
            }
        }

        if (endpoint.getForge().getForgeConstantType().isDeployTimeTimeConstant() && endpoint instanceof ExprNodeDeployTimeConst) {
            ExprNodeDeployTimeConst node = (ExprNodeDeployTimeConst) endpoint;
            if (JavaClassHelper.isImplementsCharSequence(endpoint.getForge().getEvaluationType())) {
                return new FilterForEvalDeployTimeConstStringForge(node);
            } else {
                return new FilterForEvalDeployTimeConstDoubleForge(node);
            }
        }

        // or property
        if (endpoint instanceof ExprIdentNode) {
            return getIdentNodeDoubleEval((ExprIdentNode) endpoint, arrayEventTypes, statementName);
        }

        // or limited expression
        if (advancedPlanning && isLimitedValueExpression(endpoint)) {
            Class returnType = endpoint.getForge().getEvaluationType();
            MatchedEventConvertorForge convertor = getMatchEventConvertor(endpoint, taggedEventTypes, arrayEventTypes, allTagNamesOrdered);
            if (JavaClassHelper.isImplementsCharSequence(returnType)) {
                return new FilterForEvalLimitedExprForge(endpoint, convertor, null);
            }
            SimpleNumberCoercer coercer = SimpleNumberCoercerFactory.getCoercer(returnType, Double.class);
            return new FilterForEvalLimitedExprForge(endpoint, convertor, coercer);
        }

        return null;
    }
}
