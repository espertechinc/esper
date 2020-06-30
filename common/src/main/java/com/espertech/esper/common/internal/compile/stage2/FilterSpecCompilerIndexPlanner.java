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
import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerExecution;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.common.internal.compile.stage2.FilterSpecCompilerIndexPlannerHelper.*;
import static com.espertech.esper.common.internal.compile.stage2.FilterSpecCompilerIndexPlannerWidthBasic.planRemainingNodesBasic;
import static com.espertech.esper.common.internal.compile.stage2.FilterSpecCompilerIndexPlannerWidthWithConditions.planRemainingNodesWithConditions;
import static com.espertech.esper.common.internal.compile.stage2.FilterSpecPlanForge.makePlanFromTriplets;

public class FilterSpecCompilerIndexPlanner {
    /**
     * Assigned for filter parameters that are based on boolean expression and not on
     * any particular property name.
     * <p>
     * Keeping this artificial property name is a simplification as optimized filter parameters
     * generally keep a property name.
     */
    public final static String PROPERTY_NAME_BOOLEAN_EXPRESSION = ".boolean_expression";

    public static FilterSpecPlanForge planFilterParameters(List<ExprNode> validatedNodes, FilterSpecCompilerArgs args) throws ExprValidationException {
        FilterSpecPlanForge plan = planFilterParametersInternal(validatedNodes, args);
        promoteControlConfirmSinglePathSingleTriplet(plan);
        return plan;
    }

    private static FilterSpecPlanForge planFilterParametersInternal(List<ExprNode> validatedNodes, FilterSpecCompilerArgs args)
            throws ExprValidationException {

        if (validatedNodes.isEmpty()) {
            return FilterSpecPlanForge.EMPTY;
        }
        if (args.compileTimeServices.getConfiguration().getCompiler().getExecution().getFilterIndexPlanning() == ConfigurationCompilerExecution.FilterIndexPlanning.NONE) {
            decomposeCheckAggregation(validatedNodes);
            return buildNoPlan(validatedNodes, args);
        }

        boolean performConditionPlanning = hasLevelOrHint(FilterSpecCompilerIndexPlannerHint.CONDITIONS, args.statementRawInfo, args.compileTimeServices);
        FilterSpecParaForgeMap filterParamExprMap = new FilterSpecParaForgeMap();

        // Make filter parameter for each expression node, if it can be optimized.
        // Optionally receive a top-level control condition that negates
        ExprNode topLevelNegation = decomposePopulateConsolidate(filterParamExprMap, performConditionPlanning, validatedNodes, args);

        // Use all filter parameter and unassigned expressions
        int countUnassigned = filterParamExprMap.countUnassignedExpressions();

        // we are done if there are no remaining nodes
        if (countUnassigned == 0) {
            return makePlanFromTriplets(filterParamExprMap.getTriplets(), topLevelNegation, args);
        }

        // determine max-width
        int filterServiceMaxFilterWidth = args.compileTimeServices.getConfiguration().getCompiler().getExecution().getFilterServiceMaxFilterWidth();
        Hint hint = HintEnum.MAX_FILTER_WIDTH.getHint(args.statementRawInfo.getAnnotations());
        if (hint != null) {
            String hintValue = HintEnum.MAX_FILTER_WIDTH.getHintAssignedValue(hint);
            filterServiceMaxFilterWidth = Integer.parseInt(hintValue);
        }

        FilterSpecPlanForge plan = null;
        if (filterServiceMaxFilterWidth > 0) {
            if (performConditionPlanning) {
                plan = planRemainingNodesWithConditions(filterParamExprMap, args, filterServiceMaxFilterWidth, topLevelNegation);
            } else {
                plan = planRemainingNodesBasic(filterParamExprMap, args, filterServiceMaxFilterWidth);
            }
        }

        if (plan != null) {
            return plan;
        }

        // handle no-plan
        List<FilterSpecPlanPathTripletForge> triplets = new ArrayList<>(filterParamExprMap.getTriplets());
        List<ExprNode> unassignedExpressions = filterParamExprMap.getUnassignedExpressions();
        FilterSpecPlanPathTripletForge triplet = makeRemainingNode(unassignedExpressions, args);
        triplets.add(triplet);
        return makePlanFromTriplets(triplets, topLevelNegation, args);
    }

    private static FilterSpecPlanForge buildNoPlan(List<ExprNode> validatedNodes, FilterSpecCompilerArgs args)
            throws ExprValidationException {
        FilterSpecPlanPathTripletForge triplet = makeRemainingNode(validatedNodes, args);
        FilterSpecPlanPathTripletForge[] triplets = new FilterSpecPlanPathTripletForge[]{triplet};
        FilterSpecPlanPathForge path = new FilterSpecPlanPathForge(triplets, null);
        FilterSpecPlanPathForge[] paths = new FilterSpecPlanPathForge[]{path};
        return new FilterSpecPlanForge(paths, null, null, null);
    }

    private static void promoteControlConfirmSinglePathSingleTriplet(FilterSpecPlanForge plan) {
        if (plan.getPaths().length != 1) {
            return;
        }
        FilterSpecPlanPathForge path = plan.getPaths()[0];
        if (path.getTriplets().length != 1) {
            return;
        }
        ExprNode controlConfirm = path.getTriplets()[0].getTripletConfirm();
        if (controlConfirm == null) {
            return;
        }
        plan.setFilterConfirm(controlConfirm);
        path.getTriplets()[0].setTripletConfirm(null);
    }
}
