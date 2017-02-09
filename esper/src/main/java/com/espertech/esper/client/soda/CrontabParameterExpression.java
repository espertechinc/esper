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

/**
 * Parameter expression such as last/lastweek/weekday/wildcard for use in crontab expressions.
 */
public class CrontabParameterExpression extends ExpressionBase {
    private ScheduleItemType type;
    private static final long serialVersionUID = -7679321191577855626L;

    /**
     * Ctor.
     */
    public CrontabParameterExpression() {
    }

    /**
     * Ctor.
     *
     * @param type of crontab parameter
     */
    public CrontabParameterExpression(ScheduleItemType type) {
        this.type = type;
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.UNARY;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        if (!this.getChildren().isEmpty()) {
            this.getChildren().get(0).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            writer.append(' ');
        }
        writer.write(type.getSyntax());
    }

    /**
     * Returns crontab parameter type.
     *
     * @return crontab parameter type
     */
    public ScheduleItemType getType() {
        return type;
    }

    /**
     * Sets the crontab parameter type.
     *
     * @param type crontab parameter type
     */
    public void setType(ScheduleItemType type) {
        this.type = type;
    }
}
