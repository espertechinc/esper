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
package com.espertech.esper.epl.spec;

import com.espertech.esper.filter.FilterSpecCompiled;
import com.espertech.esper.pattern.EvalFactoryNode;
import com.espertech.esper.pattern.EvalFilterFactoryNode;
import com.espertech.esper.pattern.EvalNodeAnalysisResult;
import com.espertech.esper.pattern.EvalNodeUtil;

import java.util.ArrayList;
import java.util.List;

public class ContextDetailConditionPattern implements ContextDetailCondition {

    private static final long serialVersionUID = -5855240089039407834L;
    private final EvalFactoryNode patternRaw;
    private final boolean inclusive;
    private final boolean immediate;

    private transient PatternStreamSpecCompiled patternCompiled;

    public ContextDetailConditionPattern(EvalFactoryNode patternRaw, boolean inclusive, boolean immediate) {
        this.patternRaw = patternRaw;
        this.inclusive = inclusive;
        this.immediate = immediate;
    }

    public EvalFactoryNode getPatternRaw() {
        return patternRaw;
    }

    public PatternStreamSpecCompiled getPatternCompiled() {
        return patternCompiled;
    }

    public void setPatternCompiled(PatternStreamSpecCompiled patternCompiled) {
        this.patternCompiled = patternCompiled;
    }

    public boolean isInclusive() {
        return inclusive;
    }

    public boolean isImmediate() {
        return immediate;
    }

    public List<FilterSpecCompiled> getFilterSpecIfAny() {
        List<FilterSpecCompiled> filters = new ArrayList<FilterSpecCompiled>();
        EvalNodeAnalysisResult evalNodeAnalysisResult = EvalNodeUtil.recursiveAnalyzeChildNodes(patternCompiled.getEvalFactoryNode());
        List<EvalFilterFactoryNode> filterNodes = evalNodeAnalysisResult.getFilterNodes();
        for (EvalFilterFactoryNode filterNode : filterNodes) {
            filters.add(filterNode.getFilterSpec());
        }
        return filters.isEmpty() ? null : filters;
    }
}
