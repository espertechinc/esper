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
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.client.type.EPTypePremade;
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
    public static FilterSpecParamForge handleRangeNode(ExprBetweenNode betweenNode, LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, LinkedHashSet<String> allTagNamesOrdered, String statementName, StatementRawInfo raw, StatementCompileTimeServices services) throws ExprValidationException {
        ExprNode left = betweenNode.getChildNodes()[0];
        ExprFilterSpecLookupableForge lookupable = null;

        if (left instanceof ExprFilterOptimizableNode) {
            ExprFilterOptimizableNode filterOptimizableNode = (ExprFilterOptimizableNode) left;
            lookupable = filterOptimizableNode.getFilterLookupable();
        } else if (FilterSpecCompilerIndexPlannerHelper.hasLevelOrHint(FilterSpecCompilerIndexPlannerHint.LKUPCOMPOSITE, raw, services) && isLimitedLookupableExpression(left)) {
            lookupable = makeLimitedLookupableForgeMayNull(left, raw, services);
        }
        if (lookupable == null) {
            return null;
        }

        FilterOperator op = FilterOperator.parseRangeOperator(betweenNode.isLowEndpointIncluded(), betweenNode.isHighEndpointIncluded(), betweenNode.isNotBetween());

        FilterSpecParamFilterForEvalForge low = handleRangeNodeEndpoint(betweenNode.getChildNodes()[1], taggedEventTypes, arrayEventTypes, allTagNamesOrdered, statementName, raw, services);
        FilterSpecParamFilterForEvalForge high = handleRangeNodeEndpoint(betweenNode.getChildNodes()[2], taggedEventTypes, arrayEventTypes, allTagNamesOrdered, statementName, raw, services);
        return low == null || high == null ? null :  new FilterSpecParamRangeForge(lookupable, op, low, high);
    }

    private static FilterSpecParamFilterForEvalForge handleRangeNodeEndpoint(ExprNode endpoint, LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, LinkedHashSet<String> allTagNamesOrdered, String statementName, StatementRawInfo raw, StatementCompileTimeServices services) throws ExprValidationException {
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
            if (node.getValueType() == EPTypeNull.INSTANCE) {
                return null;
            }
            EPTypeClass type = (EPTypeClass) node.getValueType();
            if (JavaClassHelper.isImplementsCharSequence(type)) {
                return new FilterForEvalContextPropStringForge(node.getGetter(), node.getPropertyName());
            } else {
                return new FilterForEvalContextPropDoubleForge(node.getGetter(), node.getPropertyName());
            }
        }

        if (endpoint.getForge().getForgeConstantType().isDeployTimeTimeConstant() && endpoint instanceof ExprNodeDeployTimeConst) {
            ExprNodeDeployTimeConst node = (ExprNodeDeployTimeConst) endpoint;
            EPTypeClass type = (EPTypeClass) endpoint.getForge().getEvaluationType();
            if (JavaClassHelper.isImplementsCharSequence(type)) {
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
        if (FilterSpecCompilerIndexPlannerHelper.hasLevelOrHint(FilterSpecCompilerIndexPlannerHint.VALUECOMPOSITE, raw, services) && isLimitedValueExpression(endpoint)) {
            EPType returnType = endpoint.getForge().getEvaluationType();
            if (returnType == EPTypeNull.INSTANCE || returnType == null) {
                return null;
            }
            EPTypeClass returnClass = (EPTypeClass) returnType;
            MatchedEventConvertorForge convertor = getMatchEventConvertor(endpoint, taggedEventTypes, arrayEventTypes, allTagNamesOrdered);
            if (JavaClassHelper.isImplementsCharSequence(returnClass)) {
                return new FilterForEvalLimitedExprForge(endpoint, convertor, null);
            }
            SimpleNumberCoercer coercer = SimpleNumberCoercerFactory.getCoercer(returnClass, EPTypePremade.DOUBLEBOXED.getEPType());
            return new FilterForEvalLimitedExprForge(endpoint, convertor, coercer);
        }

        return null;
    }
}
