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
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.ops.ExprEqualsNode;
import com.espertech.esper.common.internal.epl.expression.ops.ExprRelationalOpNode;
import com.espertech.esper.common.internal.epl.pattern.core.MatchedEventConvertorForge;
import com.espertech.esper.common.internal.filterspec.*;
import com.espertech.esper.common.internal.type.RelationalOpEnum;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import static com.espertech.esper.common.internal.compile.stage2.FilterSpecCompilerIndexPlannerHelper.*;

public class FilterSpecCompilerIndexPlannerEquals {

    protected static FilterSpecParamForge handleEqualsAndRelOp(ExprNode constituent,
                                                               LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes,
                                                               LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes,
                                                               LinkedHashSet<String> allTagNamesOrdered,
                                                               String statementName,
                                                               StatementRawInfo raw,
                                                               StatementCompileTimeServices services)
        throws ExprValidationException {
        FilterOperator op;
        if (constituent instanceof ExprEqualsNode) {
            ExprEqualsNode equalsNode = (ExprEqualsNode) constituent;
            if (!equalsNode.isIs()) {
                op = FilterOperator.EQUAL;
                if (equalsNode.isNotEquals()) {
                    op = FilterOperator.NOT_EQUAL;
                }
            } else {
                op = FilterOperator.IS;
                if (equalsNode.isNotEquals()) {
                    op = FilterOperator.IS_NOT;
                }
            }
        } else {
            ExprRelationalOpNode relNode = (ExprRelationalOpNode) constituent;
            if (relNode.getRelationalOpEnum() == RelationalOpEnum.GT) {
                op = FilterOperator.GREATER;
            } else if (relNode.getRelationalOpEnum() == RelationalOpEnum.LT) {
                op = FilterOperator.LESS;
            } else if (relNode.getRelationalOpEnum() == RelationalOpEnum.LE) {
                op = FilterOperator.LESS_OR_EQUAL;
            } else if (relNode.getRelationalOpEnum() == RelationalOpEnum.GE) {
                op = FilterOperator.GREATER_OR_EQUAL;
            } else {
                throw new IllegalStateException("Opertor '" + relNode.getRelationalOpEnum() + "' not mapped");
            }
        }

        ExprNode left = constituent.getChildNodes()[0];
        ExprNode right = constituent.getChildNodes()[1];

        // check identifier and constant combination
        if ((right.getForge().getForgeConstantType().isCompileTimeConstant()) && (left instanceof ExprFilterOptimizableNode)) {
            ExprFilterOptimizableNode filterOptimizableNode = (ExprFilterOptimizableNode) left;
            if (filterOptimizableNode.getFilterLookupEligible()) {
                ExprFilterSpecLookupableForge lookupable = filterOptimizableNode.getFilterLookupable();
                Object constant = right.getForge().getExprEvaluator().evaluate(null, true, null);
                constant = handleConstantsCoercion(lookupable, constant);
                return new FilterSpecParamConstantForge(lookupable, op, constant);
            }
        }
        if ((left.getForge().getForgeConstantType().isCompileTimeConstant()) && (right instanceof ExprFilterOptimizableNode)) {
            ExprFilterOptimizableNode filterOptimizableNode = (ExprFilterOptimizableNode) right;
            if (filterOptimizableNode.getFilterLookupEligible()) {
                ExprFilterSpecLookupableForge lookupable = filterOptimizableNode.getFilterLookupable();
                Object constant = left.getForge().getExprEvaluator().evaluate(null, true, null);
                constant = handleConstantsCoercion(lookupable, constant);
                FilterOperator opReversed = op.isComparisonOperator() ? op.reversedRelationalOp() : op;
                return new FilterSpecParamConstantForge(lookupable, opReversed, constant);
            }
        }
        // check identifier and expression containing other streams
        if (left instanceof ExprIdentNode && right instanceof ExprIdentNode) {
            ExprIdentNode identNodeLeft = (ExprIdentNode) left;
            ExprIdentNode identNodeRight = (ExprIdentNode) right;

            if ((identNodeLeft.getStreamId() == 0) && (identNodeLeft.getFilterLookupEligible()) && (identNodeRight.getStreamId() != 0)) {
                return handleProperty(op, identNodeLeft, identNodeRight, arrayEventTypes, statementName);
            }
            if ((identNodeRight.getStreamId() == 0) && (identNodeRight.getFilterLookupEligible()) && (identNodeLeft.getStreamId() != 0)) {
                op = getReversedOperator(constituent, op); // reverse operators, as the expression is "stream1.prop xyz stream0.prop"
                return handleProperty(op, identNodeRight, identNodeLeft, arrayEventTypes, statementName);
            }
        }

        if ((left instanceof ExprFilterOptimizableNode) && (right instanceof ExprContextPropertyNode)) {
            ExprFilterOptimizableNode filterOptimizableNode = (ExprFilterOptimizableNode) left;
            ExprContextPropertyNode ctxNode = (ExprContextPropertyNode) right;
            ExprFilterSpecLookupableForge lookupable = filterOptimizableNode.getFilterLookupable();
            if (filterOptimizableNode.getFilterLookupEligible()) {
                SimpleNumberCoercer numberCoercer = getNumberCoercer(lookupable.getReturnType(), ctxNode.getValueType(), lookupable.getExpression());
                return new FilterSpecParamContextPropForge(lookupable, op, ctxNode.getPropertyName(), ctxNode.getGetter(), numberCoercer);
            }
        }
        if ((left instanceof ExprContextPropertyNode) && (right instanceof ExprFilterOptimizableNode)) {
            ExprFilterOptimizableNode filterOptimizableNode = (ExprFilterOptimizableNode) right;
            ExprContextPropertyNode ctxNode = (ExprContextPropertyNode) left;
            ExprFilterSpecLookupableForge lookupable = filterOptimizableNode.getFilterLookupable();
            if (filterOptimizableNode.getFilterLookupEligible()) {
                op = getReversedOperator(constituent, op); // reverse operators, as the expression is "stream1.prop xyz stream0.prop"
                SimpleNumberCoercer numberCoercer = getNumberCoercer(lookupable.getReturnType(), ctxNode.getValueType(), lookupable.getExpression());
                return new FilterSpecParamContextPropForge(lookupable, op, ctxNode.getPropertyName(), ctxNode.getGetter(), numberCoercer);
            }
        }

        if ((left instanceof ExprFilterOptimizableNode) && (right.getForge().getForgeConstantType().isDeployTimeTimeConstant() && right instanceof ExprNodeDeployTimeConst)) {
            ExprFilterOptimizableNode filterOptimizableNode = (ExprFilterOptimizableNode) left;
            ExprNodeDeployTimeConst deployTimeConst = (ExprNodeDeployTimeConst) right;
            ExprFilterSpecLookupableForge lookupable = filterOptimizableNode.getFilterLookupable();
            if (filterOptimizableNode.getFilterLookupEligible()) {
                EPType returnType = right.getForge().getEvaluationType();
                SimpleNumberCoercer numberCoercer = getNumberCoercer(lookupable.getReturnType(), returnType, lookupable.getExpression());
                return new FilterSpecParamDeployTimeConstParamForge(lookupable, op, deployTimeConst, returnType, numberCoercer);
            }
        }
        if ((left.getForge().getForgeConstantType().isDeployTimeTimeConstant() && left instanceof ExprNodeDeployTimeConst) && (right instanceof ExprFilterOptimizableNode)) {
            ExprFilterOptimizableNode filterOptimizableNode = (ExprFilterOptimizableNode) right;
            ExprNodeDeployTimeConst deployTimeConst = (ExprNodeDeployTimeConst) left;
            ExprFilterSpecLookupableForge lookupable = filterOptimizableNode.getFilterLookupable();
            if (filterOptimizableNode.getFilterLookupEligible()) {
                EPType returnType = left.getForge().getEvaluationType();
                op = getReversedOperator(constituent, op); // reverse operators, as the expression is "stream1.prop xyz stream0.prop"
                SimpleNumberCoercer numberCoercer = getNumberCoercer(lookupable.getReturnType(), returnType, lookupable.getExpression());
                return new FilterSpecParamDeployTimeConstParamForge(lookupable, op, deployTimeConst, returnType, numberCoercer);
            }
        }

        // check lookable-limited and value-limited expression
        ExprNode lookupable = null;
        ExprNode value = null;
        FilterOperator opWReverse = op;
        if (isLimitedLookupableExpression(left) && isLimitedValueExpression(right)) {
            lookupable = left;
            value = right;
        } else if (isLimitedLookupableExpression(right) && isLimitedValueExpression(left)) {
            lookupable = right;
            value = left;
            opWReverse = getReversedOperator(constituent, op);
        }
        if (lookupable != null) {
            return handleLimitedExpr(opWReverse, lookupable, value, taggedEventTypes, arrayEventTypes, allTagNamesOrdered, raw, services);
        }

        return null;
    }

    private static FilterSpecParamForge handleLimitedExpr(FilterOperator op, ExprNode lookupable, ExprNode value, LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, LinkedHashSet<String> allTagNamesOrdered, StatementRawInfo raw,
                                                          StatementCompileTimeServices services) throws ExprValidationException {
        ExprFilterSpecLookupableForge lookupableForge;
        EPType lookupableType = lookupable.getForge().getEvaluationType();
        EPType valueType = value.getForge().getEvaluationType();
        if (lookupable instanceof ExprIdentNode) {
            if (!FilterSpecCompilerIndexPlannerHelper.hasLevelOrHint(FilterSpecCompilerIndexPlannerHint.VALUECOMPOSITE, raw, services)) {
                return null;
            }
            ExprIdentNode identNode = (ExprIdentNode) lookupable;
            if (!identNode.getFilterLookupEligible()) {
                return null;
            }
            lookupableForge = identNode.getFilterLookupable();
        } else {
            if (!FilterSpecCompilerIndexPlannerHelper.hasLevelOrHint(FilterSpecCompilerIndexPlannerHint.LKUPCOMPOSITE, raw, services)) {
                return null;
            }
            lookupableForge = makeLimitedLookupableForgeMayNull(lookupable, raw, services);
            if (lookupableForge == null) {
                return null;
            }
        }
        MatchedEventConvertorForge convertor = getMatchEventConvertor(value, taggedEventTypes, arrayEventTypes, allTagNamesOrdered);
        SimpleNumberCoercer numberCoercer = getNumberCoercer(lookupableType, valueType, lookupableForge.getExpression());
        return new FilterSpecParamValueLimitedExprForge(lookupableForge, op, value, convertor, numberCoercer);
    }

    private static FilterOperator getReversedOperator(ExprNode constituent, FilterOperator op) {
        if (!(constituent instanceof ExprRelationalOpNode)) {
            return op;
        }

        ExprRelationalOpNode relNode = (ExprRelationalOpNode) constituent;
        RelationalOpEnum relationalOpEnum = relNode.getRelationalOpEnum();

        if (relationalOpEnum == RelationalOpEnum.GT) {
            return FilterOperator.LESS;
        } else if (relationalOpEnum == RelationalOpEnum.LT) {
            return FilterOperator.GREATER;
        } else if (relationalOpEnum == RelationalOpEnum.LE) {
            return FilterOperator.GREATER_OR_EQUAL;
        } else if (relationalOpEnum == RelationalOpEnum.GE) {
            return FilterOperator.LESS_OR_EQUAL;
        }
        return op;
    }

    private static FilterSpecParamForge handleProperty(FilterOperator op, ExprIdentNode identNodeLeft, ExprIdentNode identNodeRight, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, String statementName)
        throws ExprValidationException {
        String propertyName = identNodeLeft.getResolvedPropertyName();

        EPType leftType = identNodeLeft.getForge().getEvaluationType();
        EPType rightType = identNodeRight.getForge().getEvaluationType();

        SimpleNumberCoercer numberCoercer = getNumberCoercer(leftType, rightType, propertyName);
        boolean isMustCoerce = numberCoercer != null;
        EPTypeClass numericCoercionType = JavaClassHelper.getBoxedType((EPTypeClass) leftType);

        String streamName = identNodeRight.getResolvedStreamName();
        if (arrayEventTypes != null && !arrayEventTypes.isEmpty() && arrayEventTypes.containsKey(streamName)) {
            EventType innerEventType = getArrayInnerEventType(arrayEventTypes, streamName);
            Pair<Integer, String> indexAndProp = getStreamIndex(identNodeRight.getResolvedPropertyName());
            return new FilterSpecParamEventPropIndexedForge(identNodeLeft.getFilterLookupable(), op, identNodeRight.getResolvedStreamName(), indexAndProp.getFirst(),
                indexAndProp.getSecond(), innerEventType, isMustCoerce, numberCoercer, numericCoercionType, statementName);
        }
        return new FilterSpecParamEventPropForge(identNodeLeft.getFilterLookupable(), op, identNodeRight.getResolvedStreamName(), identNodeRight.getResolvedPropertyName(), identNodeRight.getExprEvaluatorIdent(),
            isMustCoerce, numberCoercer, numericCoercionType, statementName);
    }
}
