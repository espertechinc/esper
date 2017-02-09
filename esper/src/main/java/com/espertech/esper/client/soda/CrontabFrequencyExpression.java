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
 * Frequency expression for use in crontab expressions.
 */
public class CrontabFrequencyExpression extends ExpressionBase {
    private static final long serialVersionUID = -5781607347729616944L;

    /**
     * Ctor.
     */
    public CrontabFrequencyExpression() {
    }

    /**
     * Ctor.
     *
     * @param numericParameter the frequency value
     */
    public CrontabFrequencyExpression(Expression numericParameter) {
        this.getChildren().add(numericParameter);
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.UNARY;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append("*/");
        this.getChildren().get(0).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
    }
}
