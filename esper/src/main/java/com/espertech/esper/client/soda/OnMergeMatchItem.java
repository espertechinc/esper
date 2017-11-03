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
import java.util.ArrayList;
import java.util.List;

/**
 * As part of on-merge, this represents a single "matched" or "not matched" entry.
 */
public class OnMergeMatchItem extends OnClause {
    private static final long serialVersionUID = 0L;

    private boolean matched;
    private Expression optionalCondition;
    private List<OnMergeMatchedAction> actions;

    /**
     * Ctor.
     */
    public OnMergeMatchItem() {
        actions = new ArrayList<OnMergeMatchedAction>();
    }

    /**
     * Ctor.
     *
     * @param matched           true for matched, false for not-matched
     * @param optionalCondition an optional additional filter
     * @param actions           one or more actions to take
     */
    public OnMergeMatchItem(boolean matched, Expression optionalCondition, List<OnMergeMatchedAction> actions) {
        this.matched = matched;
        this.optionalCondition = optionalCondition;
        this.actions = actions;
    }

    /**
     * Renders the clause in textual representation.
     *
     * @param writer    to output to
     * @param formatter for newline-whitespace formatting
     */
    public void toEPL(StringWriter writer, EPStatementFormatter formatter) {
        formatter.beginMergeWhenMatched(writer);
        if (matched) {
            writer.write("when matched");
        } else {
            writer.write("when not matched");
        }
        if (optionalCondition != null) {
            writer.write(" and ");
            optionalCondition.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        }
        for (OnMergeMatchedAction action : actions) {
            formatter.beginMergeAction(writer);
            writer.append("then ");
            action.toEPL(writer);
        }
    }

    /**
     * Returns true for matched, and false for not-matched.
     *
     * @return matched or not-matched indicator
     */
    public boolean isMatched() {
        return matched;
    }

    /**
     * Set to true for matched, and false for not-matched.
     *
     * @param matched matched or not-matched indicator
     */
    public void setMatched(boolean matched) {
        this.matched = matched;
    }

    /**
     * Returns the condition to apply or null if none is provided.
     *
     * @return condition
     */
    public Expression getOptionalCondition() {
        return optionalCondition;
    }

    /**
     * Sets the condition to apply or null if none is provided.
     *
     * @param optionalCondition condition to apply or null to have no condition
     */
    public void setOptionalCondition(Expression optionalCondition) {
        this.optionalCondition = optionalCondition;
    }

    /**
     * Returns all actions.
     *
     * @return actions
     */
    public List<OnMergeMatchedAction> getActions() {
        return actions;
    }

    /**
     * Sets all actions.
     *
     * @param actions to set
     */
    public void setActions(List<OnMergeMatchedAction> actions) {
        this.actions = actions;
    }
}