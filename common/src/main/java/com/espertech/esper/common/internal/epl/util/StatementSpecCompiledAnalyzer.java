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
package com.espertech.esper.common.internal.epl.util;


import com.espertech.esper.common.internal.compile.stage1.spec.NamedWindowConsumerStreamSpec;
import com.espertech.esper.common.internal.compile.stage1.spec.PatternStreamSpecCompiled;
import com.espertech.esper.common.internal.compile.stage1.spec.StreamSpecCompiled;
import com.espertech.esper.common.internal.compile.stage2.*;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.pattern.filter.EvalFilterForgeNode;

import java.util.ArrayList;
import java.util.List;

public class StatementSpecCompiledAnalyzer {

    public static StatementSpecCompiledAnalyzerResult analyzeFilters(StatementSpecCompiled spec) {
        List<FilterSpecCompiled> filters = new ArrayList<>();
        List<NamedWindowConsumerStreamSpec> namedWindows = new ArrayList<>();

        addFilters(spec.getStreamSpecs(), filters, namedWindows);

        for (ExprSubselectNode subselect : spec.getSubselectNodes()) {
            addFilters(subselect.getStatementSpecCompiled().getStreamSpecs(), filters, namedWindows);
        }

        return new StatementSpecCompiledAnalyzerResult(filters, namedWindows);
    }

    private static void addFilters(StreamSpecCompiled[] streams, List<FilterSpecCompiled> filters, List<NamedWindowConsumerStreamSpec> namedWindows) {
        for (StreamSpecCompiled compiled : streams) {
            if (compiled instanceof FilterStreamSpecCompiled) {
                FilterStreamSpecCompiled c = (FilterStreamSpecCompiled) compiled;
                filters.add(c.getFilterSpecCompiled());
            }
            if (compiled instanceof PatternStreamSpecCompiled) {
                PatternStreamSpecCompiled r = (PatternStreamSpecCompiled) compiled;
                EvalNodeAnalysisResult evalNodeAnalysisResult = EvalNodeUtil.recursiveAnalyzeChildNodes(r.getRoot());
                List<EvalFilterForgeNode> filterNodes = evalNodeAnalysisResult.getFilterNodes();
                for (EvalFilterForgeNode filterNode : filterNodes) {
                    filters.add(filterNode.getFilterSpecCompiled());
                }
            }
            if (compiled instanceof NamedWindowConsumerStreamSpec) {
                namedWindows.add((NamedWindowConsumerStreamSpec) compiled);
            }
        }
    }
}
