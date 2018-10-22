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
package com.espertech.esper.common.internal.epl.pattern.matchuntil;

import com.espertech.esper.common.internal.compile.stage2.EvalNodeUtil;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.pattern.core.*;

/**
 * This class represents a match-until observer in the evaluation tree representing any event expressions.
 */
public class EvalMatchUntilFactoryNode extends EvalFactoryNodeBase {
    private ExprEvaluator lowerBounds;
    private ExprEvaluator upperBounds;
    private ExprEvaluator singleBound;
    private int[] tagsArrayed;
    protected EvalFactoryNode[] children;
    private MatchedEventConvertor optionalConvertor;

    public EvalNode makeEvalNode(PatternAgentInstanceContext agentInstanceContext, EvalNode parentNode) {
        EvalNode[] nodes = EvalNodeUtil.makeEvalNodeChildren(children, agentInstanceContext, parentNode);
        return new EvalMatchUntilNode(agentInstanceContext, this, nodes[0], nodes.length == 1 ? null : nodes[1]);
    }

    public void setLowerBounds(ExprEvaluator lowerBounds) {
        this.lowerBounds = lowerBounds;
    }

    public void setUpperBounds(ExprEvaluator upperBounds) {
        this.upperBounds = upperBounds;
    }

    public void setSingleBound(ExprEvaluator singleBound) {
        this.singleBound = singleBound;
    }

    public void setTagsArrayed(int[] tagsArrayed) {
        this.tagsArrayed = tagsArrayed;
    }

    public void setChildren(EvalFactoryNode[] children) {
        this.children = children;
    }

    public void setOptionalConvertor(MatchedEventConvertor optionalConvertor) {
        this.optionalConvertor = optionalConvertor;
    }

    public ExprEvaluator getLowerBounds() {
        return lowerBounds;
    }

    public ExprEvaluator getUpperBounds() {
        return upperBounds;
    }

    public ExprEvaluator getSingleBound() {
        return singleBound;
    }

    public EvalFactoryNode[] getChildren() {
        return children;
    }

    public MatchedEventConvertor getOptionalConvertor() {
        return optionalConvertor;
    }

    public int[] getTagsArrayed() {
        return tagsArrayed;
    }

    public boolean isFilterChildNonQuitting() {
        return true;
    }

    public boolean isStateful() {
        return true;
    }

    public void accept(EvalFactoryNodeVisitor visitor) {
        visitor.visit(this);
        for (EvalFactoryNode child : children) {
            child.accept(visitor);
        }
    }
}
