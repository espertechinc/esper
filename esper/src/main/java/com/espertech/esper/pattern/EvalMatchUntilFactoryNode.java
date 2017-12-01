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

import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;

/**
 * This class represents a match-until observer in the evaluation tree representing any event expressions.
 */
public class EvalMatchUntilFactoryNode extends EvalNodeFactoryBase {
    private static final long serialVersionUID = 5697835058233579562L;
    private ExprNode lowerBounds;
    private ExprNode upperBounds;
    private ExprNode singleBound;
    private transient MatchedEventConvertor convertor;
    private int[] tagsArrayed;

    protected EvalMatchUntilFactoryNode(ExprNode lowerBounds, ExprNode upperBounds, ExprNode singleBound) {
        if (singleBound != null && (lowerBounds != null || upperBounds != null)) {
            throw new IllegalArgumentException("Invalid bounds, specify either single bound or range bounds");
        }
        this.lowerBounds = lowerBounds;
        this.upperBounds = upperBounds;
        this.singleBound = singleBound;
    }

    public EvalNode makeEvalNode(PatternAgentInstanceContext agentInstanceContext, EvalNode parentNode) {
        EvalNode[] children = EvalNodeUtil.makeEvalNodeChildren(this.getChildNodes(), agentInstanceContext, parentNode);
        return new EvalMatchUntilNode(agentInstanceContext, this, children[0], children.length == 1 ? null : children[1]);
    }

    /**
     * Returns an array of tags for events, which is all tags used within the repeat-operator.
     *
     * @return array of tags
     */
    public int[] getTagsArrayed() {
        return tagsArrayed;
    }

    /**
     * Sets the convertor for matching events to events-per-stream.
     *
     * @param convertor convertor
     */
    public void setConvertor(MatchedEventConvertor convertor) {
        this.convertor = convertor;
    }

    public ExprNode getLowerBounds() {
        return lowerBounds;
    }

    public ExprNode getUpperBounds() {
        return upperBounds;
    }

    public ExprNode getSingleBound() {
        return singleBound;
    }

    public void setLowerBounds(ExprNode lowerBounds) {
        this.lowerBounds = lowerBounds;
    }

    public void setUpperBounds(ExprNode upperBounds) {
        this.upperBounds = upperBounds;
    }

    public void setSingleBound(ExprNode singleBound) {
        this.singleBound = singleBound;
    }

    /**
     * Sets the tags used within the repeat operator.
     *
     * @param tagsArrayedSet tags used within the repeat operator
     */
    public void setTagsArrayedSet(int[] tagsArrayedSet) {
        tagsArrayed = tagsArrayedSet;
    }

    public MatchedEventConvertor getConvertor() {
        return convertor;
    }

    public final String toString() {
        return "EvalMatchUntilNode children=" + this.getChildNodes().size();
    }

    public boolean isFilterChildNonQuitting() {
        return true;
    }

    public boolean isStateful() {
        return true;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        if (singleBound != null) {
            writer.append("[");
            writer.append(ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(singleBound));
            writer.append("] ");
        } else {
            if (lowerBounds != null || upperBounds != null) {
                writer.append("[");
                if (lowerBounds != null) {
                    writer.append(ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(lowerBounds));
                }
                writer.append(":");
                if (upperBounds != null) {
                    writer.append(ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(upperBounds));
                }
                writer.append("] ");
            }
        }
        getChildNodes().get(0).toEPL(writer, getPrecedence());
        if (getChildNodes().size() > 1) {
            writer.append(" until ");
            getChildNodes().get(1).toEPL(writer, getPrecedence());
        }
    }

    public PatternExpressionPrecedenceEnum getPrecedence() {
        return PatternExpressionPrecedenceEnum.REPEAT_UNTIL;
    }

    private static final Logger log = LoggerFactory.getLogger(EvalMatchUntilFactoryNode.class);
}
