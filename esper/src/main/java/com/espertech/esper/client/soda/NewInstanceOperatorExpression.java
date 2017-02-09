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
 * The "new instance" operator instantiates a host language object.
 */
public class NewInstanceOperatorExpression extends ExpressionBase {

    private static final long serialVersionUID = 4725168176516142366L;
    private String className;

    /**
     * Ctor.
     */
    public NewInstanceOperatorExpression() {
    }

    /**
     * Ctor.
     * <p>
     *
     * @param className the class name
     */
    public NewInstanceOperatorExpression(String className) {
        this.className = className;
    }

    /**
     * Returns the class name.
     *
     * @return class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Sets the class name.
     *
     * @param className class name to set
     */
    public void setClassName(String className) {
        this.className = className;
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.UNARY;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.write("new ");
        writer.write(className);
        writer.write("(");
        ExpressionBase.toPrecedenceFreeEPL(this.getChildren(), writer);
        writer.write(")");
    }
}
