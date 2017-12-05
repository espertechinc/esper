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
package com.espertech.esper.epl.util;

import com.espertech.esper.epl.spec.PatternStreamSpecCompiled;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;
import com.espertech.esper.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.epl.spec.*;
import com.espertech.esper.filterspec.FilterSpecCompiled;
import com.espertech.esper.pattern.EvalFilterFactoryNode;
import com.espertech.esper.pattern.EvalNodeAnalysisResult;
import com.espertech.esper.pattern.EvalNodeUtil;

import java.util.ArrayList;
import java.util.List;

public class StatementSpecCompiledAnalyzer {

    public static StatementSpecCompiledAnalyzerResult analyzeFilters(StatementSpecCompiled spec) {
        List<FilterSpecCompiled> filters = new ArrayList<FilterSpecCompiled>();
        List<NamedWindowConsumerStreamSpec> namedWindows = new ArrayList<NamedWindowConsumerStreamSpec>();

        addFilters(spec.getStreamSpecs(), filters, namedWindows);

        for (ExprSubselectNode subselect : spec.getSubSelectExpressions()) {
            addFilters(subselect.getStatementSpecCompiled().getStreamSpecs(), filters, namedWindows);
        }

        return new StatementSpecCompiledAnalyzerResult(filters, namedWindows);
    }

    private static void addFilters(StreamSpecCompiled[] streams, List<FilterSpecCompiled> filters, List<NamedWindowConsumerStreamSpec> namedWindows) {
        for (StreamSpecCompiled compiled : streams) {
            if (compiled instanceof FilterStreamSpecCompiled) {
                FilterStreamSpecCompiled c = (FilterStreamSpecCompiled) compiled;
                filters.add(c.getFilterSpec());
            }
            if (compiled instanceof PatternStreamSpecCompiled) {
                PatternStreamSpecCompiled r = (PatternStreamSpecCompiled) compiled;
                EvalNodeAnalysisResult evalNodeAnalysisResult = EvalNodeUtil.recursiveAnalyzeChildNodes(r.getEvalFactoryNode());
                List<EvalFilterFactoryNode> filterNodes = evalNodeAnalysisResult.getFilterNodes();
                for (EvalFilterFactoryNode filterNode : filterNodes) {
                    filters.add(filterNode.getFilterSpec());
                }
            }
            if (compiled instanceof NamedWindowConsumerStreamSpec) {
                namedWindows.add((NamedWindowConsumerStreamSpec) compiled);
            }
        }
    }
}
