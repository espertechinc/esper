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
 * Context condition that start/initiated or ends/terminates context partitions based on a crontab expression.
 */
public class ContextDescriptorConditionCrontab implements ContextDescriptorCondition {

    private static final long serialVersionUID = 5676956299459269157L;
    private List<Expression> crontabExpressions;
    private boolean now;

    /**
     * Ctor.
     */
    public ContextDescriptorConditionCrontab() {
    }

    /**
     * Ctor.
     *
     * @param crontabExpressions crontab expressions returning number sets for each crontab position
     * @param now                indicator whethet to include "now"
     */
    public ContextDescriptorConditionCrontab(List<Expression> crontabExpressions, boolean now) {
        this.crontabExpressions = crontabExpressions;
        this.now = now;
    }

    /**
     * Returns the crontab expressions.
     *
     * @return crontab
     */
    public List<Expression> getCrontabExpressions() {
        return crontabExpressions;
    }

    /**
     * Sets the crontab expressions.
     *
     * @param crontabExpressions to set
     */
    public void setCrontabExpressions(List<Expression> crontabExpressions) {
        this.crontabExpressions = crontabExpressions;
    }

    public void toEPL(StringWriter writer, EPStatementFormatter formatter) {
        write(writer, crontabExpressions, now);
    }

    /**
     * Returns "now" indicator
     *
     * @return "now" indicator
     */
    public boolean isNow() {
        return now;
    }

    /**
     * Sets "now" indicator
     *
     * @param now "now" indicator
     */
    public void setNow(boolean now) {
        this.now = now;
    }

    private static void write(StringWriter writer, List<Expression> expressions, boolean now) {
        if (now) {
            writer.append("@now and ");
        }
        writer.append("(");
        String delimiter = "";
        for (Expression e : expressions) {
            writer.append(delimiter);
            e.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            delimiter = ", ";
        }
        writer.append(")");
    }
}
