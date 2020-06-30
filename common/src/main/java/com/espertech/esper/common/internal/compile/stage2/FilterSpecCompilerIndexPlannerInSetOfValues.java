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
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.ops.ExprInNode;
import com.espertech.esper.common.internal.epl.pattern.core.MatchedEventConvertorForge;
import com.espertech.esper.common.internal.filterspec.*;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;

import java.lang.reflect.Array;
import java.util.*;

import static com.espertech.esper.common.internal.compile.stage2.FilterSpecCompilerIndexPlannerHelper.*;

/**
 * Helper to compile (validate and optimize) filter expressions as used in pattern and filter-based streams.
 */
public class FilterSpecCompilerIndexPlannerInSetOfValues {

    protected static FilterSpecParamForge handleInSetNode(ExprInNode constituent, LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, LinkedHashSet<String> allTagNamesOrdered, StatementRawInfo raw, StatementCompileTimeServices services)
                throws ExprValidationException {
        ExprNode left = constituent.getChildNodes()[0];
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

        FilterOperator op = FilterOperator.IN_LIST_OF_VALUES;
        if (constituent.isNotIn()) {
            op = FilterOperator.NOT_IN_LIST_OF_VALUES;
        }

        int expectedNumberOfConstants = constituent.getChildNodes().length - 1;
        List<FilterSpecParamInValueForge> listofValues = new ArrayList<>();
        Iterator<ExprNode> it = Arrays.asList(constituent.getChildNodes()).iterator();
        it.next();  // ignore the first node as it's the identifier
        while (it.hasNext()) {
            ExprNode subNode = it.next();
            if (subNode.getForge().getForgeConstantType().isCompileTimeConstant()) {
                Object constant = subNode.getForge().getExprEvaluator().evaluate(null, true, null);
                if (constant instanceof Collection) {
                    return null;
                }
                if (constant instanceof Map) {
                    return null;
                }
                if ((constant != null) && (constant.getClass().isArray())) {
                    for (int i = 0; i < Array.getLength(constant); i++) {
                        Object arrayElement = Array.get(constant, i);
                        Object arrayElementCoerced = handleConstantsCoercion(lookupable, arrayElement);
                        listofValues.add(new FilterForEvalConstantAnyTypeForge(arrayElementCoerced));
                        if (i > 0) {
                            expectedNumberOfConstants++;
                        }
                    }
                } else {
                    constant = handleConstantsCoercion(lookupable, constant);
                    listofValues.add(new FilterForEvalConstantAnyTypeForge(constant));
                }
            } else if (subNode instanceof ExprContextPropertyNode) {
                ExprContextPropertyNode contextPropertyNode = (ExprContextPropertyNode) subNode;
                if (contextPropertyNode.getValueType() == EPTypeNull.INSTANCE) {
                    return null;
                }
                EPTypeClass returnType = (EPTypeClass) contextPropertyNode.getValueType();
                SimpleNumberCoercer coercer;
                if (JavaClassHelper.isCollectionMapOrArray(returnType)) {
                    checkArrayCoercion(returnType, lookupable.getReturnType().getType(), lookupable.getExpression());
                    coercer = null;
                } else {
                    coercer = getNumberCoercer(left.getForge().getEvaluationType(), contextPropertyNode.getValueType(), lookupable.getExpression());
                }
                EPTypeClass finalReturnType = coercer != null ? coercer.getReturnType() : returnType;
                listofValues.add(new FilterForEvalContextPropForge(contextPropertyNode.getPropertyName(), contextPropertyNode.getGetter(), coercer, finalReturnType));
            } else if (subNode.getForge().getForgeConstantType().isDeployTimeTimeConstant() && subNode instanceof ExprNodeDeployTimeConst) {
                ExprNodeDeployTimeConst deployTimeConst = (ExprNodeDeployTimeConst) subNode;
                EPType returnType = subNode.getForge().getEvaluationType();
                SimpleNumberCoercer coercer;
                if (JavaClassHelper.isCollectionMapOrArray(returnType)) {
                    checkArrayCoercion(returnType, lookupable.getReturnType().getType(), lookupable.getExpression());
                    coercer = null;
                } else {
                    coercer = getNumberCoercer(left.getForge().getEvaluationType(), returnType, lookupable.getExpression());
                }
                listofValues.add(new FilterForEvalDeployTimeConstForge(deployTimeConst, coercer, returnType));
            } else if (subNode instanceof ExprIdentNode) {
                ExprIdentNode identNodeInner = (ExprIdentNode) subNode;
                if (identNodeInner.getStreamId() == 0) {
                    break; // for same event evals use the boolean expression, via count compare failing below
                }

                boolean isMustCoerce = false;
                EPTypeClass coerceToType = JavaClassHelper.getBoxedType(lookupable.getReturnType());
                EPType identReturnType = identNodeInner.getForge().getEvaluationType();

                if (JavaClassHelper.isCollectionMapOrArray(identReturnType)) {
                    checkArrayCoercion(identReturnType, lookupable.getReturnType().getType(), lookupable.getExpression());
                    coerceToType = (EPTypeClass) identReturnType;
                    // no action
                } else if (identReturnType instanceof EPTypeClass && ((EPTypeClass) identReturnType).getType() != lookupable.getReturnType().getType()) {
                    EPTypeClass identTypeClass = (EPTypeClass) identReturnType;
                    if (JavaClassHelper.isNumeric(lookupable.getReturnType())) {
                        if (!JavaClassHelper.canCoerce(identTypeClass.getType(), lookupable.getReturnType().getType())) {
                            throwConversionError(identTypeClass.getType(), lookupable.getReturnType().getType(), lookupable.getExpression());
                        }
                        isMustCoerce = true;
                    } else {
                        break;  // assumed not compatible
                    }
                }

                FilterSpecParamInValueForge inValue;
                String streamName = identNodeInner.getResolvedStreamName();
                if (arrayEventTypes != null && !arrayEventTypes.isEmpty() && arrayEventTypes.containsKey(streamName)) {
                    Pair<Integer, String> indexAndProp = getStreamIndex(identNodeInner.getResolvedPropertyName());
                    EventType innerEventType = getArrayInnerEventType(arrayEventTypes, streamName);
                    inValue = new FilterForEvalEventPropIndexedForge(identNodeInner.getResolvedStreamName(), indexAndProp.getFirst(),
                        indexAndProp.getSecond(), innerEventType, isMustCoerce, coerceToType);
                } else {
                    inValue = new FilterForEvalEventPropForge(identNodeInner.getResolvedStreamName(), identNodeInner.getResolvedPropertyName(), identNodeInner.getExprEvaluatorIdent(), isMustCoerce, coerceToType);
                }

                listofValues.add(inValue);
            } else if (FilterSpecCompilerIndexPlannerHelper.hasLevelOrHint(FilterSpecCompilerIndexPlannerHint.VALUECOMPOSITE, raw, services) && isLimitedValueExpression(subNode)) {
                MatchedEventConvertorForge convertor = getMatchEventConvertor(subNode, taggedEventTypes, arrayEventTypes, allTagNamesOrdered);
                EPType valueType = subNode.getForge().getEvaluationType();
                EPTypeClass lookupableType = lookupable.getReturnType();
                SimpleNumberCoercer numberCoercer = getNumberCoercer(lookupableType, valueType, lookupable.getExpression());
                FilterForEvalLimitedExprForge forge = new FilterForEvalLimitedExprForge(subNode, convertor, numberCoercer);
                listofValues.add(forge);
            }
        }

        // Fallback if not all values in the in-node can be resolved to properties or constants
        if (listofValues.size() == expectedNumberOfConstants) {
            return new FilterSpecParamInForge(lookupable, op, listofValues);
        }
        return null;
    }

    private static void checkArrayCoercion(EPType returnTypeValue, Class returnTypeLookupable, String propertyName) throws ExprValidationException {
        if (returnTypeValue == null || returnTypeValue == EPTypeNull.INSTANCE) {
            return;
        }
        EPTypeClass returnTypeClass = (EPTypeClass) returnTypeValue;
        if (!returnTypeClass.getType().isArray()) {
            return;
        }
        if (!JavaClassHelper.isArrayTypeCompatible(returnTypeLookupable, returnTypeClass.getType().getComponentType())) {
            throwConversionError(returnTypeClass.getType().getComponentType(), returnTypeLookupable, propertyName);
        }
    }
}
