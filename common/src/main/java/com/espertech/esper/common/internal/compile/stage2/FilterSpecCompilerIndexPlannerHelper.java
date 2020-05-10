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
import com.espertech.esper.common.client.annotation.HintEnum;
import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerExecution;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeUtil;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.ops.ExprAndNode;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeStreamUseCollectVisitor;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeSubselectDeclaredDotVisitor;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeTableAccessFinderVisitor;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeVariableVisitor;
import com.espertech.esper.common.internal.epl.pattern.core.MatchedEventConvertorForge;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.event.property.IndexedProperty;
import com.espertech.esper.common.internal.event.property.NestedProperty;
import com.espertech.esper.common.internal.event.property.Property;
import com.espertech.esper.common.internal.event.property.PropertyParser;
import com.espertech.esper.common.internal.filterspec.*;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;
import com.espertech.esper.common.internal.util.SimpleNumberCoercerFactory;

import java.io.StringWriter;
import java.util.*;

import static com.espertech.esper.common.internal.compile.stage2.FilterSpecCompilerIndexPlanner.PROPERTY_NAME_BOOLEAN_EXPRESSION;

public class FilterSpecCompilerIndexPlannerHelper {
    protected static ExprNode decomposePopulateConsolidate(FilterSpecParaForgeMap filterParamExprMap, boolean performConditionPlanning, List<ExprNode> validatedNodes, FilterSpecCompilerArgs args)
        throws ExprValidationException {
        List<ExprNode> constituents = decomposeCheckAggregation(validatedNodes);

        // Remove constituents that are value-expressions
        ExprNode topLevelControl = null;
        if (performConditionPlanning) {
            List<ExprNode> valueOnlyConstituents = null;
            for (ExprNode node : constituents) {
                FilterSpecExprNodeVisitorValueLimitedExpr visitor = new FilterSpecExprNodeVisitorValueLimitedExpr();
                node.accept(visitor);
                if (visitor.isLimited()) {
                    if (valueOnlyConstituents == null) {
                        valueOnlyConstituents = new ArrayList<>();
                    }
                    valueOnlyConstituents.add(node);
                }
            }

            if (valueOnlyConstituents != null) {
                constituents.removeAll(valueOnlyConstituents);
                topLevelControl = ExprNodeUtilityMake.connectExpressionsByLogicalAndWhenNeeded(valueOnlyConstituents);
            }
        }

        // Make filter parameter for each expression node, if it can be optimized
        for (ExprNode constituent : constituents) {
            FilterSpecPlanPathTripletForge triplet = FilterSpecCompilerIndexPlannerConstituent.makeFilterParam(constituent, performConditionPlanning, args.taggedEventTypes, args.arrayEventTypes, args.allTagNamesOrdered, args.statementRawInfo.getStatementName(), args.streamTypeService, args.statementRawInfo, args.compileTimeServices);
            filterParamExprMap.put(constituent, triplet); // accepts null values as the expression may not be optimized
        }

        // Consolidate entries as possible, i.e. (a != 5 and a != 6) is (a not in (5,6))
        // Removes duplicates for same property and same filter operator for filter service index optimizations
        FilterSpecCompilerConsolidateUtil.consolidate(filterParamExprMap, args.statementRawInfo.getStatementName());
        return topLevelControl;
    }

    protected static SimpleNumberCoercer getNumberCoercer(Class leftType, Class rightType, String expression) throws ExprValidationException {
        Class numericCoercionType = JavaClassHelper.getBoxedType(leftType);
        if (rightType != leftType) {
            if (JavaClassHelper.isNumeric(rightType)) {
                if (!JavaClassHelper.canCoerce(rightType, leftType)) {
                    throwConversionError(rightType, leftType, expression);
                }
                return SimpleNumberCoercerFactory.getCoercer(rightType, numericCoercionType);
            }
        }
        return null;
    }

    protected static void throwConversionError(Class fromType, Class toType, String propertyName)
        throws ExprValidationException {
        String text = "Implicit conversion from datatype '" +
            fromType.getSimpleName() +
            "' to '" +
            toType.getSimpleName() +
            "' for property '" +
            propertyName +
            "' is not allowed (strict filter type coercion)";
        throw new ExprValidationException(text);
    }

    protected static MatchedEventConvertorForge getMatchEventConvertor(ExprNode value, LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, LinkedHashSet<String> allTagNamesOrdered) throws ExprValidationException {
        ExprNodeStreamUseCollectVisitor streamUseCollectVisitor = new ExprNodeStreamUseCollectVisitor();
        value.accept(streamUseCollectVisitor);

        Set<Integer> streams = new HashSet<>(streamUseCollectVisitor.getReferenced().size());
        for (ExprStreamRefNode streamRefNode : streamUseCollectVisitor.getReferenced()) {
            if (streamRefNode.getStreamReferencedIfAny() == null) {
                continue;
            }
            streams.add(streamRefNode.getStreamReferencedIfAny());
        }

        return new MatchedEventConvertorForge(taggedEventTypes, arrayEventTypes, allTagNamesOrdered, streams, true);
    }

    protected static Pair<Integer, String> getStreamIndex(String resolvedPropertyName) {
        Property property = PropertyParser.parseAndWalkLaxToSimple(resolvedPropertyName);
        if (!(property instanceof NestedProperty)) {
            throw new IllegalStateException("Expected a nested property providing an index for array match '" + resolvedPropertyName + "'");
        }
        NestedProperty nested = (NestedProperty) property;
        if (nested.getProperties().size() < 2) {
            throw new IllegalStateException("Expected a nested property name for array match '" + resolvedPropertyName + "', none found");
        }
        if (!(nested.getProperties().get(0) instanceof IndexedProperty)) {
            throw new IllegalStateException("Expected an indexed property for array match '" + resolvedPropertyName + "', please provide an index");
        }
        int index = ((IndexedProperty) nested.getProperties().get(0)).getIndex();
        nested.getProperties().remove(0);
        StringWriter writer = new StringWriter();
        nested.toPropertyEPL(writer);
        return new Pair<>(index, writer.toString());
    }

    protected static List<ExprNode> decomposeCheckAggregation(List<ExprNode> validatedNodes) throws ExprValidationException {
        // Break a top-level AND into constituent expression nodes
        List<ExprNode> constituents = new ArrayList<ExprNode>();
        for (ExprNode validated : validatedNodes) {
            if (validated instanceof ExprAndNode) {
                recursiveAndConstituents(constituents, validated);
            } else {
                constituents.add(validated);
            }

            // Ensure there is no aggregation nodes
            List<ExprAggregateNode> aggregateExprNodes = new LinkedList<ExprAggregateNode>();
            ExprAggregateNodeUtil.getAggregatesBottomUp(validated, aggregateExprNodes);
            if (!aggregateExprNodes.isEmpty()) {
                throw new ExprValidationException("Aggregation functions not allowed within filters");
            }
        }

        return constituents;
    }

    private static void recursiveAndConstituents(List<ExprNode> constituents, ExprNode exprNode) {
        for (ExprNode inner : exprNode.getChildNodes()) {
            if (inner instanceof ExprAndNode) {
                recursiveAndConstituents(constituents, inner);
            } else {
                constituents.add(inner);
            }
        }
    }

    protected static boolean isLimitedValueExpression(ExprNode node) {
        FilterSpecExprNodeVisitorValueLimitedExpr visitor = new FilterSpecExprNodeVisitorValueLimitedExpr();
        node.accept(visitor);
        return visitor.isLimited();
    }

    protected static EventType getArrayInnerEventType(LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, String streamName) {
        Pair<EventType, String> arrayEventType = arrayEventTypes.get(streamName);
        Object prop = ((MapEventType) arrayEventType.getFirst()).getTypes().get(streamName);
        return ((EventType[]) prop)[0];
    }

    // expressions automatically coerce to the most upwards type
    // filters require the same type
    protected static Object handleConstantsCoercion(ExprFilterSpecLookupableForge lookupable, Object constant)
        throws ExprValidationException {
        Class identNodeType = lookupable.getReturnType();
        if (!JavaClassHelper.isNumeric(identNodeType)) {
            return constant;    // no coercion required, other type checking performed by expression this comes from
        }

        if (constant == null) {
            // null constant type
            return null;
        }

        if (!JavaClassHelper.canCoerce(constant.getClass(), identNodeType)) {
            throwConversionError(constant.getClass(), identNodeType, lookupable.getExpression());
        }

        Class identNodeTypeBoxed = JavaClassHelper.getBoxedType(identNodeType);
        return JavaClassHelper.coerceBoxed((Number) constant, identNodeTypeBoxed);
    }

    protected static FilterSpecParamFilterForEvalDoubleForge getIdentNodeDoubleEval(ExprIdentNode node, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, String statementName) {
        if (node.getStreamId() == 0) {
            return null;
        }

        if (arrayEventTypes != null && !arrayEventTypes.isEmpty() && arrayEventTypes.containsKey(node.getResolvedStreamName())) {
            Pair<Integer, String> indexAndProp = getStreamIndex(node.getResolvedPropertyName());
            EventType eventType = getArrayInnerEventType(arrayEventTypes, node.getResolvedStreamName());
            return new FilterForEvalEventPropIndexedDoubleForge(node.getResolvedStreamName(), indexAndProp.getFirst(), indexAndProp.getSecond(), eventType);
        } else {
            return new FilterForEvalEventPropDoubleForge(node.getResolvedStreamName(), node.getResolvedPropertyName(), node.getExprEvaluatorIdent());
        }
    }

    protected static boolean isLimitedLookupableExpression(ExprNode node) {
        FilterSpecExprNodeVisitorLookupableLimitedExpr visitor = new FilterSpecExprNodeVisitorLookupableLimitedExpr();
        node.accept(visitor);
        return visitor.isLimited() && visitor.isHasStreamZeroReference();
    }

    protected static ExprFilterSpecLookupableForge makeLimitedLookupableForgeMayNull(ExprNode lookupable, StatementRawInfo raw, StatementCompileTimeServices services) throws ExprValidationException {
        if (!FilterSpecCompilerIndexPlannerHelper.hasLevelOrHint(FilterSpecCompilerIndexPlannerHint.LKUPCOMPOSITE, raw, services)) {
            return null;
        }
        Class lookupableType = lookupable.getForge().getEvaluationType();
        String expression = ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(lookupable);
        FilterSpecCompilerIndexLimitedLookupableGetterForge getterForge = new FilterSpecCompilerIndexLimitedLookupableGetterForge(lookupable);
        DataInputOutputSerdeForge serde = services.getSerdeResolver().serdeForFilter(lookupableType, raw);
        return new ExprFilterSpecLookupableForge(expression, getterForge, null, lookupableType, true, serde);
    }

    protected static FilterSpecPlanPathTripletForge makeRemainingNode(List<ExprNode> unassignedExpressions, FilterSpecCompilerArgs args)
        throws ExprValidationException {
        if (unassignedExpressions.isEmpty()) {
            throw new IllegalArgumentException();
        }

        // any unoptimized expression nodes are put under one AND
        ExprNode exprNode;
        if (unassignedExpressions.size() == 1) {
            exprNode = unassignedExpressions.get(0);
        } else {
            exprNode = makeValidateAndNode(unassignedExpressions, args);
        }
        FilterSpecParamForge param = makeBooleanExprParam(exprNode, args);
        return new FilterSpecPlanPathTripletForge(param, null);
    }

    private static ExprAndNode makeValidateAndNode(List<ExprNode> remainingExprNodes, FilterSpecCompilerArgs args)
        throws ExprValidationException {
        ExprAndNode andNode = ExprNodeUtilityMake.connectExpressionsByLogicalAnd(remainingExprNodes);
        ExprValidationContext validationContext = new ExprValidationContextBuilder(args.streamTypeService, args.statementRawInfo, args.compileTimeServices)
            .withAllowBindingConsumption(true).withContextDescriptor(args.contextDescriptor).build();
        andNode.validate(validationContext);
        return andNode;
    }

    protected static boolean hasLevelOrHint(FilterSpecCompilerIndexPlannerHint requiredHint, StatementRawInfo raw, StatementCompileTimeServices services) throws ExprValidationException {
        ConfigurationCompilerExecution.FilterIndexPlanning config = services.getConfiguration().getCompiler().getExecution().getFilterIndexPlanning();
        if (config == ConfigurationCompilerExecution.FilterIndexPlanning.ADVANCED) {
            return true;
        }
        List<String> hints = HintEnum.FILTERINDEX.getHintAssignedValues(raw.getAnnotations());
        if (hints == null) {
            return false;
        }
        for (String hint : hints) {
            String[] hintAtoms = HintEnum.splitCommaUnlessInParen(hint);
            for (int i = 0; i < hintAtoms.length; i++) {
                String hintAtom = hintAtoms[i];
                String hintLowercase = hintAtom.toLowerCase(Locale.ENGLISH).trim();
                FilterSpecCompilerIndexPlannerHint found = null;
                for (FilterSpecCompilerIndexPlannerHint available : FilterSpecCompilerIndexPlannerHint.values()) {
                    if (hintLowercase.equals(available.getNameLowercase())) {
                        found = available;
                        if (requiredHint == available) {
                            return true;
                        }
                    }
                }
                if (found == null) {
                    throw new ExprValidationException("Unrecognized filterindex hint value '" + hintAtom + "'");
                }
            }
        }
        return false;
    }

    private static FilterSpecParamForge makeBooleanExprParam(ExprNode exprNode, FilterSpecCompilerArgs args) {
        boolean hasSubselectFilterStream = determineSubselectFilterStream(exprNode);
        boolean hasTableAccess = determineTableAccessFilterStream(exprNode);

        ExprNodeVariableVisitor visitor = new ExprNodeVariableVisitor(args.compileTimeServices.getVariableCompileTimeResolver());
        exprNode.accept(visitor);
        boolean hasVariable = visitor.isHasVariables();

        Class evalType = exprNode.getForge().getEvaluationType();
        DataInputOutputSerdeForge serdeForge = args.compileTimeServices.getSerdeResolver().serdeForFilter(evalType, args.statementRawInfo);
        ExprFilterSpecLookupableForge lookupable = new ExprFilterSpecLookupableForge(PROPERTY_NAME_BOOLEAN_EXPRESSION, null, null, evalType, false, serdeForge);

        return new FilterSpecParamExprNodeForge(lookupable, FilterOperator.BOOLEAN_EXPRESSION, exprNode, args.taggedEventTypes, args.arrayEventTypes, args.streamTypeService, hasSubselectFilterStream, hasTableAccess, hasVariable, args.compileTimeServices);
    }

    private static boolean determineTableAccessFilterStream(ExprNode exprNode) {
        ExprNodeTableAccessFinderVisitor visitor = new ExprNodeTableAccessFinderVisitor();
        exprNode.accept(visitor);
        return visitor.isHasTableAccess();
    }

    private static boolean determineSubselectFilterStream(ExprNode exprNode) {
        ExprNodeSubselectDeclaredDotVisitor visitor = new ExprNodeSubselectDeclaredDotVisitor();
        exprNode.accept(visitor);
        if (visitor.getSubselects().isEmpty()) {
            return false;
        }
        for (ExprSubselectNode subselectNode : visitor.getSubselects()) {
            if (subselectNode.isFilterStreamSubselect()) {
                return true;
            }
        }
        return false;
    }
}
