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
package com.espertech.esper.client.soda;

import java.io.StringWriter;
import java.util.List;

/**
 * Context dimension descriptor for a start-and-end temporal (single instance) or initiated-terminated (overlapping) context
 */
public class ContextDescriptorInitiatedTerminated implements ContextDescriptor {

    private static final long serialVersionUID = 8185386941253467559L;
    private ContextDescriptorCondition startCondition;
    private ContextDescriptorCondition endCondition;
    private boolean overlapping;
    private List<Expression> optionalDistinctExpressions;

    /**
     * Ctor.
     */
    public ContextDescriptorInitiatedTerminated() {
    }

    /**
     * Ctor.
     *
     * @param startCondition              the condition that starts/initiates a context partition
     * @param endCondition                the condition that ends/terminates a context partition
     * @param overlapping                 true for overlapping contexts
     * @param optionalDistinctExpressions list of distinct-value expressions, can be null
     */
    public ContextDescriptorInitiatedTerminated(ContextDescriptorCondition startCondition, ContextDescriptorCondition endCondition, boolean overlapping, List<Expression> optionalDistinctExpressions) {
        this.startCondition = startCondition;
        this.endCondition = endCondition;
        this.overlapping = overlapping;
        this.optionalDistinctExpressions = optionalDistinctExpressions;
    }

    /**
     * Ctor.
     *
     * @param startCondition the condition that starts/initiates a context partition
     * @param endCondition   the condition that ends/terminates a context partition
     * @param overlapping    true for overlapping contexts
     */
    public ContextDescriptorInitiatedTerminated(ContextDescriptorCondition startCondition, ContextDescriptorCondition endCondition, boolean overlapping) {
        this.startCondition = startCondition;
        this.endCondition = endCondition;
        this.overlapping = overlapping;
    }

    /**
     * Returns the condition that starts/initiates a context partition
     *
     * @return start condition
     */
    public ContextDescriptorCondition getStartCondition() {
        return startCondition;
    }

    /**
     * Sets the condition that starts/initiates a context partition
     *
     * @param startCondition start condition
     */
    public void setStartCondition(ContextDescriptorCondition startCondition) {
        this.startCondition = startCondition;
    }

    /**
     * Returns the condition that ends/terminates a context partition
     *
     * @return end condition
     */
    public ContextDescriptorCondition getEndCondition() {
        return endCondition;
    }

    /**
     * Sets the condition that ends/terminates a context partition
     *
     * @param endCondition end condition
     */
    public void setEndCondition(ContextDescriptorCondition endCondition) {
        this.endCondition = endCondition;
    }

    /**
     * Returns true for overlapping context, false for non-overlapping.
     *
     * @return overlap indicator
     */
    public boolean isOverlapping() {
        return overlapping;
    }

    /**
     * Set to true for overlapping context, false for non-overlapping.
     *
     * @param overlapping overlap indicator
     */
    public void setOverlapping(boolean overlapping) {
        this.overlapping = overlapping;
    }

    /**
     * Returns the list of expressions providing distinct keys, if any
     *
     * @return distinct expressions
     */
    public List<Expression> getOptionalDistinctExpressions() {
        return optionalDistinctExpressions;
    }

    /**
     * Sets the list of expressions providing distinct keys, if any
     *
     * @param optionalDistinctExpressions distinct expressions
     */
    public void setOptionalDistinctExpressions(List<Expression> optionalDistinctExpressions) {
        this.optionalDistinctExpressions = optionalDistinctExpressions;
    }

    public void toEPL(StringWriter writer, EPStatementFormatter formatter) {
        writer.append(overlapping ? "initiated by " : "start ");
        if (optionalDistinctExpressions != null && optionalDistinctExpressions.size() > 0) {
            writer.append("distinct(");
            String delimiter = "";
            for (Expression expression : optionalDistinctExpressions) {
                writer.write(delimiter);
                expression.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
                delimiter = ", ";
            }
            writer.append(") ");
        }
        startCondition.toEPL(writer, formatter);
        if (!(endCondition instanceof ContextDescriptorConditionNever)) {
            writer.append(" ");
            writer.append(overlapping ? "terminated " : "end ");
            endCondition.toEPL(writer, formatter);
        }
    }
}
