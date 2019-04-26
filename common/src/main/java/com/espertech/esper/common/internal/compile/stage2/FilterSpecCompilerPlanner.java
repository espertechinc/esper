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

import com.espertech.esper.common.client.annotation.Hint;
import com.espertech.esper.common.client.annotation.HintEnum;
import com.espertech.esper.common.internal.collection.CombinationEnumeration;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeUtil;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.ops.ExprAndNode;
import com.espertech.esper.common.internal.epl.expression.ops.ExprOrNode;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeSubselectDeclaredDotVisitor;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeTableAccessFinderVisitor;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeVariableVisitor;
import com.espertech.esper.common.internal.filterspec.FilterOperator;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamExprNodeForge;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamForge;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class FilterSpecCompilerPlanner {
    /**
     * Assigned for filter parameters that are based on boolean expression and not on
     * any particular property name.
     * <p>
     * Keeping this artificial property name is a simplification as optimized filter parameters
     * generally keep a property name.
     */
    public final static String PROPERTY_NAME_BOOLEAN_EXPRESSION = ".boolean_expression";

    public static List<FilterSpecParamForge>[] planFilterParameters(List<ExprNode> validatedNodes, FilterSpecCompilerArgs args)
            throws ExprValidationException {

        if (validatedNodes.isEmpty()) {
            return allocateListArray(0);
        }

        FilterSpecParaForgeMap filterParamExprMap = new FilterSpecParaForgeMap();

        // Make filter parameter for each expression node, if it can be optimized
        decomposePopulateConsolidate(filterParamExprMap, validatedNodes, args);

        // Use all filter parameter and unassigned expressions
        List<FilterSpecParamForge> filterParams = new ArrayList<>();
        filterParams.addAll(filterParamExprMap.getFilterParams());
        int countUnassigned = filterParamExprMap.countUnassignedExpressions();

        // we are done if there are no remaining nodes
        if (countUnassigned == 0) {
            return allocateListArraySizeOne(filterParams);
        }

        // determine max-width
        int filterServiceMaxFilterWidth = args.compileTimeServices.getConfiguration().getCompiler().getExecution().getFilterServiceMaxFilterWidth();
        Hint hint = HintEnum.MAX_FILTER_WIDTH.getHint(args.statementRawInfo.getAnnotations());
        if (hint != null) {
            String hintValue = HintEnum.MAX_FILTER_WIDTH.getHintAssignedValue(hint);
            filterServiceMaxFilterWidth = Integer.parseInt(hintValue);
        }

        List<FilterSpecParamForge>[] plan = null;
        if (filterServiceMaxFilterWidth > 0) {
            plan = planRemainingNodesIfFeasible(filterParamExprMap, args, filterServiceMaxFilterWidth);
        }

        if (plan != null) {
            return plan;
        }

        // handle no-plan
        FilterSpecParamForge node = makeRemainingNode(filterParamExprMap.getUnassignedExpressions(), args);
        filterParams.add(node);
        return allocateListArraySizeOne(filterParams);
    }

    private static List<FilterSpecParamForge>[] planRemainingNodesIfFeasible(FilterSpecParaForgeMap overallExpressions, FilterSpecCompilerArgs args, int filterServiceMaxFilterWidth)
            throws ExprValidationException {
        List<ExprNode> unassigned = overallExpressions.getUnassignedExpressions();
        List<ExprOrNode> orNodes = new ArrayList<ExprOrNode>(unassigned.size());

        for (ExprNode node : unassigned) {
            if (node instanceof ExprOrNode) {
                orNodes.add((ExprOrNode) node);
            }
        }

        FilterSpecParaForgeMap expressionsWithoutOr = new FilterSpecParaForgeMap();
        expressionsWithoutOr.add(overallExpressions);

        // first dimension: or-node index
        // second dimension: or child node index
        FilterSpecParaForgeMap[][] orNodesMaps = new FilterSpecParaForgeMap[orNodes.size()][];
        int countOr = 0;
        int sizeFactorized = 1;
        int[] sizePerOr = new int[orNodes.size()];
        for (ExprOrNode orNode : orNodes) {
            expressionsWithoutOr.removeNode(orNode);
            orNodesMaps[countOr] = new FilterSpecParaForgeMap[orNode.getChildNodes().length];
            int len = orNode.getChildNodes().length;

            for (int i = 0; i < len; i++) {
                FilterSpecParaForgeMap map = new FilterSpecParaForgeMap();
                orNodesMaps[countOr][i] = map;
                List<ExprNode> nodes = Collections.singletonList(orNode.getChildNodes()[i]);
                decomposePopulateConsolidate(map, nodes, args);
            }

            sizePerOr[countOr] = len;
            sizeFactorized = sizeFactorized * len;
            countOr++;
        }

        // we become too large
        if (sizeFactorized > filterServiceMaxFilterWidth) {
            return null;
        }

        // combine
        List<FilterSpecParamForge>[] result = (List<FilterSpecParamForge>[]) new List[sizeFactorized];
        CombinationEnumeration permutations = CombinationEnumeration.fromZeroBasedRanges(sizePerOr);
        int count = 0;
        for (; permutations.hasMoreElements(); ) {
            Object[] permutation = permutations.nextElement();
            result[count] = computePermutation(expressionsWithoutOr, permutation, orNodesMaps, args);
            count++;
        }
        return result;
    }

    private static List<FilterSpecParamForge> computePermutation(FilterSpecParaForgeMap filterParamExprMap, Object[] permutation, FilterSpecParaForgeMap[][] orNodesMaps, FilterSpecCompilerArgs args)
            throws ExprValidationException {
        FilterSpecParaForgeMap mapAll = new FilterSpecParaForgeMap();
        mapAll.add(filterParamExprMap);

        // combine
        for (int orNodeNum = 0; orNodeNum < permutation.length; orNodeNum++) {
            int orChildNodeNum = (Integer) permutation[orNodeNum];
            FilterSpecParaForgeMap mapOrSub = orNodesMaps[orNodeNum][orChildNodeNum];
            mapAll.add(mapOrSub);
        }

        // consolidate across
        FilterSpecCompilerConsolidateUtil.consolidate(mapAll, args.statementRawInfo.getStatementName());

        List<FilterSpecParamForge> filterParams = new ArrayList<>();
        filterParams.addAll(mapAll.getFilterParams());
        int countUnassigned = mapAll.countUnassignedExpressions();

        if (countUnassigned == 0) {
            return filterParams;
        }

        FilterSpecParamForge node = makeRemainingNode(mapAll.getUnassignedExpressions(), args);
        filterParams.add(node);
        return filterParams;
    }

    private static void decomposePopulateConsolidate(FilterSpecParaForgeMap filterParamExprMap, List<ExprNode> validatedNodes, FilterSpecCompilerArgs args)
            throws ExprValidationException {
        List<ExprNode> constituents = decomposeCheckAggregation(validatedNodes);

        // Make filter parameter for each expression node, if it can be optimized
        for (ExprNode constituent : constituents) {
            FilterSpecParamForge param = FilterSpecCompilerMakeParamUtil.makeFilterParam(constituent, args.arrayEventTypes, args.statementRawInfo.getStatementName());
            filterParamExprMap.put(constituent, param); // accepts null values as the expression may not be optimized
        }

        // Consolidate entries as possible, i.e. (a != 5 and a != 6) is (a not in (5,6))
        // Removes duplicates for same property and same filter operator for filter service index optimizations
        FilterSpecCompilerConsolidateUtil.consolidate(filterParamExprMap, args.statementRawInfo.getStatementName());
    }

    private static FilterSpecParamForge makeRemainingNode(List<ExprNode> unassignedExpressions, FilterSpecCompilerArgs args)
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
        return makeBooleanExprParam(exprNode, args);
    }

    private static List<FilterSpecParamForge>[] allocateListArraySizeOne(List<FilterSpecParamForge> params) {
        List<FilterSpecParamForge>[] arr = allocateListArray(1);
        arr[0] = params;
        return arr;
    }

    private static List<FilterSpecParamForge>[] allocateListArray(int i) {
        return (List<FilterSpecParamForge>[]) new List[i];
    }

    private static FilterSpecParamForge makeBooleanExprParam(ExprNode exprNode, FilterSpecCompilerArgs args) {
        boolean hasSubselectFilterStream = determineSubselectFilterStream(exprNode);
        boolean hasTableAccess = determineTableAccessFilterStream(exprNode);

        ExprNodeVariableVisitor visitor = new ExprNodeVariableVisitor(args.compileTimeServices.getVariableCompileTimeResolver());
        exprNode.accept(visitor);
        boolean hasVariable = visitor.isHasVariables();

        Class evalType = exprNode.getForge().getEvaluationType();
        DataInputOutputSerdeForge serdeForge = args.compileTimeServices.getSerdeResolver().serdeForFilter(evalType, args.statementRawInfo);
        ExprFilterSpecLookupableForge lookupable = new ExprFilterSpecLookupableForge(PROPERTY_NAME_BOOLEAN_EXPRESSION, null, evalType, false, serdeForge);

        return new FilterSpecParamExprNodeForge(lookupable, FilterOperator.BOOLEAN_EXPRESSION, exprNode, args.taggedEventTypes, args.arrayEventTypes, args.streamTypeService, hasSubselectFilterStream, hasTableAccess, hasVariable, args.compileTimeServices);
    }

    private static ExprAndNode makeValidateAndNode(List<ExprNode> remainingExprNodes, FilterSpecCompilerArgs args)
            throws ExprValidationException {
        ExprAndNode andNode = ExprNodeUtilityMake.connectExpressionsByLogicalAnd(remainingExprNodes);
        ExprValidationContext validationContext = new ExprValidationContextBuilder(args.streamTypeService, args.statementRawInfo, args.compileTimeServices)
                .withAllowBindingConsumption(true).withContextDescriptor(args.contextDescriptor).build();
        andNode.validate(validationContext);
        return andNode;
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

    private static List<ExprNode> decomposeCheckAggregation(List<ExprNode> validatedNodes) throws ExprValidationException {
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
}
