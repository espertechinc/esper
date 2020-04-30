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
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.funcs.ExprPlugInSingleRowNode;
import com.espertech.esper.common.internal.epl.expression.ops.*;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.filterspec.FilterSpecCompilerAdvIndexDescProvider;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamForge;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

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
public class FilterSpecCompilerIndexPlanner {
    /**
     * For a given expression determine if this is optimizable and create the filter parameter
     * representing the expression, or null if not optimizable.
     *
     * @param constituent      is the expression to look at
     * @param taggedEventTypes event types that provide non-array values
     * @param arrayEventTypes  event types that provide array values
     * @param statementName    statement name
     * @param streamTypeService
     * @return filter parameter representing the expression, or null
     * @throws ExprValidationException if the expression is invalid
     */
    protected static FilterSpecParamForge makeFilterParam(ExprNode constituent,
                                                          LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes,
                                                          LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes,
                                                          LinkedHashSet<String> allTagNamesOrdered,
                                                          String statementName,
                                                          StreamTypeService streamTypeService, StatementRawInfo raw,
                                                          StatementCompileTimeServices services)
        throws ExprValidationException {

        // Is this expression node a simple compare, i.e. a=5 or b<4; these can be indexed
        if ((constituent instanceof ExprEqualsNode) || (constituent instanceof ExprRelationalOpNode)) {
            FilterSpecParamForge param = handleEqualsAndRelOp(constituent, taggedEventTypes, arrayEventTypes, allTagNamesOrdered, statementName, raw, services);
            if (param != null) {
                return param;
            }
        }

        constituent = rewriteOrToInIfApplicable(constituent);

        // Is this expression node a simple compare, i.e. a=5 or b<4; these can be indexed
        if (constituent instanceof ExprInNode) {
            FilterSpecParamForge param = handleInSetNode((ExprInNode) constituent, taggedEventTypes, arrayEventTypes, allTagNamesOrdered, raw, services);
            if (param != null) {
                return param;
            }
        }

        if (constituent instanceof ExprBetweenNode) {
            FilterSpecParamForge param = handleRangeNode((ExprBetweenNode) constituent, taggedEventTypes, arrayEventTypes, allTagNamesOrdered, statementName, raw, services);
            if (param != null) {
                return param;
            }
        }

        if (constituent instanceof ExprPlugInSingleRowNode) {
            FilterSpecParamForge param = handlePlugInSingleRow((ExprPlugInSingleRowNode) constituent);
            if (param != null) {
                return param;
            }
        }

        if (constituent instanceof FilterSpecCompilerAdvIndexDescProvider) {
            FilterSpecParamForge param = handleAdvancedIndexDescProvider((FilterSpecCompilerAdvIndexDescProvider) constituent, arrayEventTypes, statementName);
            if (param != null) {
                return param;
            }
        }

        return handleBooleanLimited(constituent, taggedEventTypes, arrayEventTypes, allTagNamesOrdered, streamTypeService, raw, services);
    }
}
