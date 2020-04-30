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
import com.espertech.esper.common.internal.epl.expression.filter.ExprFilterReboolValueNode;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeIdentifierAndStreamRefVisitor;
import com.espertech.esper.common.internal.epl.pattern.core.MatchedEventConvertorForge;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.filterspec.FilterOperator;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamForge;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamValueLimitedExprForge;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamValueNullForge;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.espertech.esper.common.internal.compile.stage2.FilterSpecCompilerIndexPlannerHelper.getMatchEventConvertor;
import static com.espertech.esper.common.internal.compile.stage2.FilterSpecCompilerIndexPlannerHelper.hasLevelOrHint;

public class FilterSpecCompilerIndexPlannerBooleanLimited {
    protected static FilterSpecParamForge handleBooleanLimited(ExprNode constituent, LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, LinkedHashSet<String> allTagNamesOrdered, StreamTypeService streamTypeService, StatementRawInfo raw, StatementCompileTimeServices services)
        throws ExprValidationException {

        if (!hasLevelOrHint(FilterSpecCompilerIndexPlannerHint.BOOLCOMPOSITE, raw, services)) {
            return null;
        }

        // prequalify
        boolean prequalified = prequalify(constituent);
        if (!prequalified) {
            return null;
        }

        // determine rewrite
        RewriteDescriptor desc = findRewrite(constituent);
        if (desc == null) {
            return null;
        }

        // there is no value expression, i.e. "select * from SupportBean(theString = intPrimitive)"
        if (desc instanceof RewriteDescriptorNoValueExpr) {
            String reboolExpression = ExprNodeUtilityPrint.toExpressionStringMinPrecedence(constituent, new ExprNodeRenderableFlags(false));
            ExprFilterSpecLookupableForge lookupable = new ExprFilterSpecLookupableForge(reboolExpression, null, constituent.getForge(), null, true, null);
            return new FilterSpecParamValueNullForge(lookupable, FilterOperator.REBOOL);
        }

        // there is no value expression, i.e. "select * from SupportBean(theString regexp 'abc')"
        RewriteDescriptorWithValueExpr withValueExpr = (RewriteDescriptorWithValueExpr) desc;
        ExprNode valueExpression = withValueExpr.valueExpression;
        Class valueExpressionType = valueExpression.getForge().getEvaluationType();
        ExprFilterReboolValueNode replacement = new ExprFilterReboolValueNode(valueExpressionType);
        ExprNodeUtilityModify.replaceChildNode(withValueExpr.valueExpressionParent, valueExpression, replacement);
        ExprValidationContext validationContext = new ExprValidationContextBuilder(streamTypeService, raw, services).withIsFilterExpression(true).build();
        ExprNode rebool = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.FILTER, constituent, validationContext);
        DataInputOutputSerdeForge serde = services.getSerdeResolver().serdeForFilter(valueExpressionType, raw);
        MatchedEventConvertorForge convertor = getMatchEventConvertor(valueExpression, taggedEventTypes, arrayEventTypes, allTagNamesOrdered);

        String reboolExpression = ExprNodeUtilityPrint.toExpressionStringMinPrecedence(constituent, new ExprNodeRenderableFlags(false));
        ExprFilterSpecLookupableForge lookupable = new ExprFilterSpecLookupableForge(reboolExpression, null, rebool.getForge(), valueExpressionType, true, serde);
        return new FilterSpecParamValueLimitedExprForge(lookupable, FilterOperator.REBOOL, valueExpression, convertor, null);
    }

    private static boolean prequalify(ExprNode constituent) {
        FilterSpecExprNodeVisitorBooleanLimitedExprPrequalify prequalify = new FilterSpecExprNodeVisitorBooleanLimitedExprPrequalify();
        constituent.accept(prequalify);
        if (!prequalify.isLimited()) {
            return false;
        }
        ExprNodeIdentifierAndStreamRefVisitor streamRefVisitor = new ExprNodeIdentifierAndStreamRefVisitor(false);
        constituent.accept(streamRefVisitor);
        boolean hasStreamRefZero = false;
        for (ExprNodePropOrStreamDesc ref : streamRefVisitor.getRefs()) {
            if (ref.getStreamNum() == 0) {
                hasStreamRefZero = true;
                break;
            }
        }
        return hasStreamRefZero;
    }

    private static RewriteDescriptor findRewrite(ExprNode parent) {
        List<ExprNodeWithParentPair> valueExpressions = findValueExpressionsDeepMayNull(parent);
        if (valueExpressions == null) {
            return new RewriteDescriptorNoValueExpr();
        }

        if (valueExpressions.size() == 1) {
            ExprNodeWithParentPair pair = valueExpressions.get(0);
            return new RewriteDescriptorWithValueExpr(pair.getNode(), pair.getParent());
        }

        // find a single value expression that is non-deploy-time-constant
        List<ExprNodeWithParentPair> nonConstants = new ArrayList<>(valueExpressions.size());
        for (ExprNodeWithParentPair expr : valueExpressions) {
            if (!expr.getNode().getForge().getForgeConstantType().isConstant()) {
                nonConstants.add(expr);
            }
        }
        if (nonConstants.size() == 1) {
            ExprNodeWithParentPair pair = nonConstants.get(0);
            return new RewriteDescriptorWithValueExpr(pair.getNode(), pair.getParent());
        }

        // we are not handling multiple non-constant value expressions
        return null;
    }

    private static List<ExprNodeWithParentPair> findValueExpressionsDeepMayNull(ExprNode parent) {
        AtomicReference<List<ExprNodeWithParentPair>> pairs = new AtomicReference<>();
        findValueExpressionsDeepRecursive(parent, pairs);
        return pairs.get();
    }

    private static void findValueExpressionsDeepRecursive(ExprNode parent, AtomicReference<List<ExprNodeWithParentPair>> pairsRef) {
        for (ExprNode child : parent.getChildNodes()) {
            FilterSpecExprNodeVisitorValueLimitedExpr valueVisitor = new FilterSpecExprNodeVisitorValueLimitedExpr();
            child.accept(valueVisitor);

            // not by itself a value expression, but itself it may decompose into some value expressions
            if (!valueVisitor.isLimited()) {
                findValueExpressionsDeepRecursive(child, pairsRef);
                continue;
            }

            // add value expression, don't traverse child
            List<ExprNodeWithParentPair> pairs = pairsRef.get();
            if (pairs == null) {
                pairs = new ArrayList<>(2);
                pairsRef.set(pairs);
            }
            pairs.add(new ExprNodeWithParentPair(child, parent));
        }
    }

    private abstract static class RewriteDescriptor {
    }

    private static class RewriteDescriptorNoValueExpr extends RewriteDescriptor {
    }

    private static class RewriteDescriptorWithValueExpr extends RewriteDescriptor {
        private final ExprNode valueExpression;
        private final ExprNode valueExpressionParent;

        public RewriteDescriptorWithValueExpr(ExprNode valueExpression, ExprNode valueExpressionParent) {
            this.valueExpression = valueExpression;
            this.valueExpressionParent = valueExpressionParent;
        }

        public ExprNode getValueExpression() {
            return valueExpression;
        }

        public ExprNode getValueExpressionParent() {
            return valueExpressionParent;
        }
    }
}
