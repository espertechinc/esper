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
package com.espertech.esper.common.client.soda;

import java.io.Serializable;
import java.io.StringWriter;

/**
 * Clause for creating an application-provided class for use across one or more statements.
 */
public class CreateClassClause implements Serializable {
    private ClassProvidedExpression classProvidedExpression;

    /**
     * Ctor.
     */
    public CreateClassClause() {
    }

    /**
     * Ctor.
     *
     * @param classProvidedExpression class
     */
    public CreateClassClause(ClassProvidedExpression classProvidedExpression) {
        this.classProvidedExpression = classProvidedExpression;
    }

    /**
     * Ctor.
     *
     * @param classtext class text
     */
    public CreateClassClause(String classtext) {
        this.classProvidedExpression = new ClassProvidedExpression(classtext);
    }

    /**
     * Returns class-provided that contains the class text.
     * @return class-provided
     */
    public ClassProvidedExpression getClassProvidedExpression() {
        return classProvidedExpression;
    }

    /**
     * Sets class-provided that contains the class text.
     * @param classProvidedExpression class-provided
     */
    public void setClassProvidedExpression(ClassProvidedExpression classProvidedExpression) {
        this.classProvidedExpression = classProvidedExpression;
    }

    /**
     * EPL output
     *
     * @param writer to write to
     */
    public void toEPL(StringWriter writer) {
        writer.append("create ");
        classProvidedExpression.toEPL(writer);
    }
}
