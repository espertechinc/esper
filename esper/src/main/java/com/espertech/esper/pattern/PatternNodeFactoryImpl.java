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
package com.espertech.esper.pattern;

import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.spec.FilterSpecRaw;
import com.espertech.esper.epl.spec.PatternGuardSpec;
import com.espertech.esper.epl.spec.PatternObserverSpec;

import java.util.List;

public class PatternNodeFactoryImpl implements PatternNodeFactory {

    public EvalFactoryNode makeAndNode() {
        return new EvalAndFactoryNode();
    }

    public EvalFactoryNode makeEveryDistinctNode(List<ExprNode> expressions) {
        return new EvalEveryDistinctFactoryNode(expressions);
    }

    public EvalFactoryNode makeEveryNode() {
        return new EvalEveryFactoryNode();
    }

    public EvalFactoryNode makeFilterNode(FilterSpecRaw filterSpecification, String eventAsName, Integer consumptionLevel) {
        return new EvalFilterFactoryNode(filterSpecification, eventAsName, consumptionLevel);
    }

    public EvalFactoryNode makeFollowedByNode(List<ExprNode> maxExpressions, boolean hasEngineWideMax) {
        return new EvalFollowedByFactoryNode(maxExpressions, hasEngineWideMax);
    }

    public EvalFactoryNode makeGuardNode(PatternGuardSpec patternGuardSpec) {
        return new EvalGuardFactoryNode(patternGuardSpec);
    }

    public EvalFactoryNode makeMatchUntilNode(ExprNode lowerBounds, ExprNode upperBounds, ExprNode singleBounds) {
        return new EvalMatchUntilFactoryNode(lowerBounds, upperBounds, singleBounds);
    }

    public EvalFactoryNode makeNotNode() {
        return new EvalNotFactoryNode();
    }

    public EvalFactoryNode makeObserverNode(PatternObserverSpec patternObserverSpec) {
        return new EvalObserverFactoryNode(patternObserverSpec);
    }

    public EvalFactoryNode makeOrNode() {
        return new EvalOrFactoryNode();
    }

    public EvalRootFactoryNode makeRootNode(EvalFactoryNode childNode) {
        return new EvalRootFactoryNode(childNode);
    }

    public EvalFactoryNode makeAuditNode(boolean auditPattern, boolean auditPatternInstance, String expressionText, EvalAuditInstanceCount instanceCount, boolean filterChildNonQuitting) {
        return new EvalAuditFactoryNode(auditPattern, auditPatternInstance, expressionText, instanceCount, filterChildNonQuitting);
    }

    public boolean isAuditSupported() {
        return true;
    }
}
