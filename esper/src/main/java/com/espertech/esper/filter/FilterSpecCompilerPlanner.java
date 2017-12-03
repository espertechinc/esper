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
package com.espertech.esper.filter;

import com.espertech.esper.client.annotation.Hint;
import com.espertech.esper.client.annotation.HintEnum;
import com.espertech.esper.collection.CombinationEnumeration;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeUtil;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.ops.ExprAndNode;
import com.espertech.esper.epl.expression.ops.ExprOrNode;
import com.espertech.esper.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.epl.expression.visitor.ExprNodeSubselectDeclaredDotVisitor;
import com.espertech.esper.epl.expression.visitor.ExprNodeTableAccessFinderVisitor;
import com.espertech.esper.epl.expression.visitor.ExprNodeVariableVisitor;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.filterspec.FilterOperator;
import com.espertech.esper.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.filterspec.FilterSpecParam;
import com.espertech.esper.filterspec.FilterSpecParamExprNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class FilterSpecCompilerPlanner {
    protected static List<FilterSpecParam>[] planFilterParameters(List<ExprNode> validatedNodes, FilterSpecCompilerArgs args)
            throws ExprValidationException {

        if (validatedNodes.isEmpty()) {
            return allocateListArray(0);
        }

        FilterParamExprMap filterParamExprMap = new FilterParamExprMap();

        // Make filter parameter for each expression node, if it can be optimized
        decomposePopulateConsolidate(filterParamExprMap, validatedNodes, args);

        // Use all filter parameter and unassigned expressions
        List<FilterSpecParam> filterParams = new ArrayList<FilterSpecParam>();
        filterParams.addAll(filterParamExprMap.getFilterParams());
        int countUnassigned = filterParamExprMap.countUnassignedExpressions();

        // we are done if there are no remaining nodes
        if (countUnassigned == 0) {
            return allocateListArraySizeOne(filterParams);
        }

        // determine max-width
        int filterServiceMaxFilterWidth = args.configurationInformation.getEngineDefaults().getExecution().getFilterServiceMaxFilterWidth();
        Hint hint = HintEnum.MAX_FILTER_WIDTH.getHint(args.annotations);
        if (hint != null) {
            String hintValue = HintEnum.MAX_FILTER_WIDTH.getHintAssignedValue(hint);
            filterServiceMaxFilterWidth = Integer.parseInt(hintValue);
        }

        List<FilterSpecParam>[] plan = null;
        if (filterServiceMaxFilterWidth > 0) {
            plan = planRemainingNodesIfFeasible(filterParamExprMap, args, filterServiceMaxFilterWidth);
        }

        if (plan != null) {
            return plan;
        }

        // handle no-plan
        FilterSpecParamExprNode node = makeRemainingNode(filterParamExprMap.getUnassignedExpressions(), args);
        filterParams.add(node);
        return allocateListArraySizeOne(filterParams);
    }

    private static List<FilterSpecParam>[] planRemainingNodesIfFeasible(FilterParamExprMap overallExpressions, FilterSpecCompilerArgs args, int filterServiceMaxFilterWidth)
            throws ExprValidationException {
        List<ExprNode> unassigned = overallExpressions.getUnassignedExpressions();
        List<ExprOrNode> orNodes = new ArrayList<ExprOrNode>(unassigned.size());

        for (ExprNode node : unassigned) {
            if (node instanceof ExprOrNode) {
                orNodes.add((ExprOrNode) node);
            }
        }

        FilterParamExprMap expressionsWithoutOr = new FilterParamExprMap();
        expressionsWithoutOr.add(overallExpressions);

        // first dimension: or-node index
        // second dimension: or child node index
        FilterParamExprMap[][] orNodesMaps = new FilterParamExprMap[orNodes.size()][];
        int countOr = 0;
        int sizeFactorized = 1;
        int[] sizePerOr = new int[orNodes.size()];
        for (ExprOrNode orNode : orNodes) {
            expressionsWithoutOr.removeNode(orNode);
            orNodesMaps[countOr] = new FilterParamExprMap[orNode.getChildNodes().length];
            int len = orNode.getChildNodes().length;

            for (int i = 0; i < len; i++) {
                FilterParamExprMap map = new FilterParamExprMap();
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
        List<FilterSpecParam>[] result = (List<FilterSpecParam>[]) new List[sizeFactorized];
        CombinationEnumeration permutations = CombinationEnumeration.fromZeroBasedRanges(sizePerOr);
        int count = 0;
        for (; permutations.hasMoreElements(); ) {
            Object[] permutation = permutations.nextElement();
            result[count] = computePermutation(expressionsWithoutOr, permutation, orNodesMaps, args);
            count++;
        }
        return result;
    }

    private static List<FilterSpecParam> computePermutation(FilterParamExprMap filterParamExprMap, Object[] permutation, FilterParamExprMap[][] orNodesMaps, FilterSpecCompilerArgs args)
            throws ExprValidationException {
        FilterParamExprMap mapAll = new FilterParamExprMap();
        mapAll.add(filterParamExprMap);

        // combine
        for (int orNodeNum = 0; orNodeNum < permutation.length; orNodeNum++) {
            int orChildNodeNum = (Integer) permutation[orNodeNum];
            FilterParamExprMap mapOrSub = orNodesMaps[orNodeNum][orChildNodeNum];
            mapAll.add(mapOrSub);
        }

        // consolidate across
        FilterSpecCompilerConsolidateUtil.consolidate(mapAll, args.statementName);

        List<FilterSpecParam> filterParams = new ArrayList<FilterSpecParam>();
        filterParams.addAll(mapAll.getFilterParams());
        int countUnassigned = mapAll.countUnassignedExpressions();

        if (countUnassigned == 0) {
            return filterParams;
        }

        FilterSpecParamExprNode node = makeRemainingNode(mapAll.getUnassignedExpressions(), args);
        filterParams.add(node);
        return filterParams;
    }

    private static void decomposePopulateConsolidate(FilterParamExprMap filterParamExprMap, List<ExprNode> validatedNodes, FilterSpecCompilerArgs args)
            throws ExprValidationException {
        List<ExprNode> constituents = decomposeCheckAggregation(validatedNodes);

        // Make filter parameter for each expression node, if it can be optimized
        for (ExprNode constituent : constituents) {
            FilterSpecParam param = FilterSpecCompilerMakeParamUtil.makeFilterParam(constituent, args.arrayEventTypes, args.exprEvaluatorContext, args.statementName);
            filterParamExprMap.put(constituent, param); // accepts null values as the expression may not be optimized
        }

        // Consolidate entries as possible, i.e. (a != 5 and a != 6) is (a not in (5,6))
        // Removes duplicates for same property and same filter operator for filter service index optimizations
        FilterSpecCompilerConsolidateUtil.consolidate(filterParamExprMap, args.statementName);
    }

    private static FilterSpecParamExprNode makeRemainingNode(List<ExprNode> unassignedExpressions, FilterSpecCompilerArgs args)
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

    private static List<FilterSpecParam>[] allocateListArraySizeOne(List<FilterSpecParam> params) {
        List<FilterSpecParam>[] arr = allocateListArray(1);
        arr[0] = params;
        return arr;
    }

    private static List<FilterSpecParam>[] allocateListArray(int i) {
        return (List<FilterSpecParam>[]) new List[i];
    }

    private static FilterSpecParamExprNode makeBooleanExprParam(ExprNode exprNode, FilterSpecCompilerArgs args) {
        boolean hasSubselectFilterStream = determineSubselectFilterStream(exprNode);
        boolean hasTableAccess = determineTableAccessFilterStream(exprNode);

        ExprNodeVariableVisitor visitor = new ExprNodeVariableVisitor(args.variableService);
        exprNode.accept(visitor);
        boolean hasVariable = visitor.isHasVariables();

        ExprFilterSpecLookupable lookupable = new ExprFilterSpecLookupable(FilterSpecCompiler.PROPERTY_NAME_BOOLEAN_EXPRESSION, null, exprNode.getForge().getEvaluationType(), false);

        return new FilterSpecParamExprNode(lookupable, FilterOperator.BOOLEAN_EXPRESSION, exprNode, args.taggedEventTypes, args.arrayEventTypes, args.variableService, args.tableService, args.eventAdapterService, args.filterBooleanExpressionFactory, args.configurationInformation.getEngineDefaults().getExecution().getThreadingProfile(), hasSubselectFilterStream, hasTableAccess, hasVariable);
    }

    private static ExprAndNode makeValidateAndNode(List<ExprNode> remainingExprNodes, FilterSpecCompilerArgs args)
            throws ExprValidationException {
        ExprAndNode andNode = ExprNodeUtilityRich.connectExpressionsByLogicalAnd(remainingExprNodes);
        ExprValidationContext validationContext = new ExprValidationContext(args.streamTypeService, args.engineImportService, args.statementExtensionSvcContext, null, args.timeProvider, args.variableService, args.tableService, args.exprEvaluatorContext, args.eventAdapterService, args.statementName, args.statementId, args.annotations, args.contextDescriptor, false, false, true, false, null, false);
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
