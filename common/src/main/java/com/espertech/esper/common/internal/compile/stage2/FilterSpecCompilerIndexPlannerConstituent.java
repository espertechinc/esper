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
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityMake;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.funcs.ExprPlugInSingleRowNode;
import com.espertech.esper.common.internal.epl.expression.ops.*;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.filterspec.FilterSpecCompilerAdvIndexDescProvider;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamForge;

import java.util.*;

import static com.espertech.esper.common.internal.compile.stage2.FilterSpecCompilerIndexPlannerAdvancedIndex.handleAdvancedIndexDescProvider;
import static com.espertech.esper.common.internal.compile.stage2.FilterSpecCompilerIndexPlannerBooleanLimited.handleBooleanLimited;
import static com.espertech.esper.common.internal.compile.stage2.FilterSpecCompilerIndexPlannerEquals.handleEqualsAndRelOp;
import static com.espertech.esper.common.internal.compile.stage2.FilterSpecCompilerIndexPlannerInSetOfValues.handleInSetNode;
import static com.espertech.esper.common.internal.compile.stage2.FilterSpecCompilerIndexPlannerOrToInRewrite.rewriteOrToInIfApplicable;
import static com.espertech.esper.common.internal.compile.stage2.FilterSpecCompilerIndexPlannerPlugInSingleRow.handlePlugInSingleRow;
import static com.espertech.esper.common.internal.compile.stage2.FilterSpecCompilerIndexPlannerRange.handleRangeNode;

/**
 * Helper to compile (validate and optimize) filter expressions as used in pattern and filter-based streams.
 */
public class FilterSpecCompilerIndexPlannerConstituent {
    /**
     * For a given expression determine if this is optimizable and create the filter parameter
     * representing the expression, or null if not optimizable.
     *
     * @param constituent      is the expression to look at
     * @param performConditionPlanning indicator whether condition planning should occur
     * @param taggedEventTypes event types that provide non-array values
     * @param arrayEventTypes  event types that provide array values
     * @param allTagNamesOrdered tag names
     * @param statementName    statement name
     * @param streamTypeService stream type service
     * @param raw statement info
     * @param services compile services
     * @return filter parameter representing the expression, or null
     * @throws ExprValidationException if the expression is invalid
     */
    protected static FilterSpecPlanPathTripletForge makeFilterParam(ExprNode constituent,
                                                                    boolean performConditionPlanning,
                                                                    LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes,
                                                                    LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes,
                                                                    LinkedHashSet<String> allTagNamesOrdered,
                                                                    String statementName,
                                                                    StreamTypeService streamTypeService,
                                                                    StatementRawInfo raw,
                                                                    StatementCompileTimeServices services)
        throws ExprValidationException {

        // Is this expression node a simple compare, i.e. a=5 or b<4; these can be indexed
        if ((constituent instanceof ExprEqualsNode) || (constituent instanceof ExprRelationalOpNode)) {
            FilterSpecParamForge param = handleEqualsAndRelOp(constituent, taggedEventTypes, arrayEventTypes, allTagNamesOrdered, statementName, raw, services);
            if (param != null) {
                return new FilterSpecPlanPathTripletForge(param, null);
            }
        }

        constituent = rewriteOrToInIfApplicable(constituent, false);

        // Is this expression node a simple compare, i.e. a=5 or b<4; these can be indexed
        if (constituent instanceof ExprInNode) {
            FilterSpecParamForge param = handleInSetNode((ExprInNode) constituent, taggedEventTypes, arrayEventTypes, allTagNamesOrdered, raw, services);
            if (param != null) {
                return new FilterSpecPlanPathTripletForge(param, null);
            }
        }

        if (constituent instanceof ExprBetweenNode) {
            FilterSpecParamForge param = handleRangeNode((ExprBetweenNode) constituent, taggedEventTypes, arrayEventTypes, allTagNamesOrdered, statementName, raw, services);
            if (param != null) {
                return new FilterSpecPlanPathTripletForge(param, null);
            }
        }

        if (constituent instanceof ExprPlugInSingleRowNode) {
            FilterSpecParamForge param = handlePlugInSingleRow((ExprPlugInSingleRowNode) constituent);
            if (param != null) {
                return new FilterSpecPlanPathTripletForge(param, null);
            }
        }

        if (constituent instanceof FilterSpecCompilerAdvIndexDescProvider) {
            FilterSpecParamForge param = handleAdvancedIndexDescProvider((FilterSpecCompilerAdvIndexDescProvider) constituent, arrayEventTypes, statementName);
            if (param != null) {
                return new FilterSpecPlanPathTripletForge(param, null);
            }
        }

        if (constituent instanceof ExprOrNode && performConditionPlanning) {
            return handleOrAlternateExpression((ExprOrNode) constituent, performConditionPlanning, taggedEventTypes, arrayEventTypes, allTagNamesOrdered, statementName, streamTypeService, raw, services);
        }

        FilterSpecParamForge param = handleBooleanLimited(constituent, taggedEventTypes, arrayEventTypes, allTagNamesOrdered, streamTypeService, raw, services);
        if (param != null) {
            return new FilterSpecPlanPathTripletForge(param, null);
        }
        return null;
    }

    private static FilterSpecPlanPathTripletForge handleOrAlternateExpression(ExprOrNode orNode, boolean performConditionPlanning, LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, LinkedHashSet<String> allTagNamesOrdered, String statementName, StreamTypeService streamTypeService, StatementRawInfo raw, StatementCompileTimeServices services) throws ExprValidationException {
        List<ExprNode> valueExpressions = new ArrayList<>(orNode.getChildNodes().length);
        for (ExprNode child : orNode.getChildNodes()) {
            FilterSpecExprNodeVisitorValueLimitedExpr visitor = new FilterSpecExprNodeVisitorValueLimitedExpr();
            child.accept(visitor);
            if (visitor.isLimited()) {
                valueExpressions.add(child);
            }
        }

        // The or-node must have a single constituent and one or more value expressions
        if (orNode.getChildNodes().length != valueExpressions.size() + 1) {
            return null;
        }
        List<ExprNode> constituents = new ArrayList<>(Arrays.asList(orNode.getChildNodes()));
        constituents.removeAll(valueExpressions);
        if (constituents.size() != 1) {
            throw new IllegalStateException("Found multiple constituents");
        }
        ExprNode constituent = constituents.get(0);

        FilterSpecPlanPathTripletForge triplet = makeFilterParam(constituent, performConditionPlanning, taggedEventTypes, arrayEventTypes, allTagNamesOrdered, statementName, streamTypeService, raw, services);
        if (triplet == null) {
            return null;
        }

        ExprNode controlConfirm = ExprNodeUtilityMake.connectExpressionsByLogicalOrWhenNeeded(valueExpressions);
        return new FilterSpecPlanPathTripletForge(triplet.getParam(), controlConfirm);
    }
}
