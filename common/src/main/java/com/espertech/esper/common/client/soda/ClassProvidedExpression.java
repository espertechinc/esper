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
import java.util.List;

/**
 * Script-expression is external scripting language expression such as JavaScript, Groovy or MVEL, for example.
 */
public class ClassProvidedExpression implements Serializable {
    private static final long serialVersionUID = 6617568880253045414L;
    private String classText;

    /**
     * Ctor.
     */
    public ClassProvidedExpression() {
    }

    /**
     * Ctor.
     *
     * @param classText             class text
     */
    public ClassProvidedExpression(String classText) {
        this.classText = classText;
    }

    /**
     * Returns the class text
     * @return class text
     */
    public String getClassText() {
        return classText;
    }

    /**
     * Sets the class text
     * @param classText class text
     */
    public void setClassText(String classText) {
        this.classText = classText;
    }

    /**
     * Print part.
     *
     * @param writer to write to
     */
    public void toEPL(StringWriter writer) {
        writer.append("inlined_class ");
        writer.append("\"\"\"");
        writer.append(classText);
        writer.append("\"\"\"");
    }

    /**
     * Print.
     *
     * @param writer    to print to
     * @param classProvideds   classes
     * @param formatter for newline-whitespace formatting
     */
    public static void toEPL(StringWriter writer, List<ClassProvidedExpression> classProvideds, EPStatementFormatter formatter) {
        if ((classProvideds == null) || (classProvideds.isEmpty())) {
            return;
        }

        for (ClassProvidedExpression part : classProvideds) {
            if (part.getClassText() == null) {
                continue;
            }
            formatter.beginExpressionDecl(writer);
            part.toEPL(writer);
        }
    }
}
