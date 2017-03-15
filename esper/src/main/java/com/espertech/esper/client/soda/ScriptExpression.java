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

import java.io.Serializable;
import java.io.StringWriter;
import java.util.List;

/**
 * Script-expression is external scripting language expression such as JavaScript, Groovy or MVEL, for example.
 */
public class ScriptExpression implements Serializable {
    private static final long serialVersionUID = 3264067514571908258L;
    private String name;
    private List<String> parameterNames;
    private String expressionText;
    private String optionalReturnType;
    private String optionalEventTypeName;
    private String optionalDialect;

    /**
     * Ctor.
     */
    public ScriptExpression() {
    }

    /**
     * Ctor.
     *
     * @param name               script name
     * @param parameterNames     parameter list
     * @param expressionText     script text
     * @param optionalReturnType return type
     * @param optionalDialect    dialect
     * @param optionalEventTypeName optional event type name
     */
    public ScriptExpression(String name, List<String> parameterNames, String expressionText, String optionalReturnType, String optionalDialect, String optionalEventTypeName) {
        this.name = name;
        this.parameterNames = parameterNames;
        this.expressionText = expressionText;
        this.optionalReturnType = optionalReturnType;
        this.optionalDialect = optionalDialect;
        this.optionalEventTypeName = optionalEventTypeName;
    }

    /**
     * Ctor.
     *
     * @param name               script name
     * @param parameterNames     parameter list
     * @param expressionText     script text
     * @param optionalReturnType return type
     * @param optionalDialect    dialect
     */
    public ScriptExpression(String name, List<String> parameterNames, String expressionText, String optionalReturnType, String optionalDialect) {
        this(name, parameterNames, expressionText, optionalReturnType, optionalDialect, null);
    }

    /**
     * Returns the script name.
     *
     * @return script name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the script name.
     *
     * @param name script name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the return type, if any is specified.
     *
     * @return return type
     */
    public String getOptionalReturnType() {
        return optionalReturnType;
    }

    /**
     * Sets the return type, if any is specified.
     *
     * @param optionalReturnType return type
     */
    public void setOptionalReturnType(String optionalReturnType) {
        this.optionalReturnType = optionalReturnType;
    }

    /**
     * Returns a dialect name, or null if none is defined and the configured default applies
     *
     * @return dialect name
     */
    public String getOptionalDialect() {
        return optionalDialect;
    }

    /**
     * Sets a dialect name, or null if none is defined and the configured default applies
     *
     * @param optionalDialect dialect name
     */
    public void setOptionalDialect(String optionalDialect) {
        this.optionalDialect = optionalDialect;
    }

    /**
     * Returns the script body.
     *
     * @return script body
     */
    public String getExpressionText() {
        return expressionText;
    }

    /**
     * Sets the script body.
     *
     * @param expressionText script body
     */
    public void setExpressionText(String expressionText) {
        this.expressionText = expressionText;
    }

    /**
     * Returns the lambda expression parameters.
     *
     * @return lambda expression parameters
     */
    public List<String> getParameterNames() {
        return parameterNames;
    }

    /**
     * Sets the lambda expression parameters.
     *
     * @param parameterNames lambda expression parameters
     */
    public void setParameterNames(List<String> parameterNames) {
        this.parameterNames = parameterNames;
    }

    /**
     * Returns the optional event type name.
     * @return type name
     */
    public String getOptionalEventTypeName() {
        return optionalEventTypeName;
    }

    /**
     * Sets the optional event type name.
     * @param optionalEventTypeName name
     */
    public void setOptionalEventTypeName(String optionalEventTypeName) {
        this.optionalEventTypeName = optionalEventTypeName;
    }

    /**
     * Print.
     *
     * @param writer    to print to
     * @param scripts   scripts
     * @param formatter for newline-whitespace formatting
     */
    public static void toEPL(StringWriter writer, List<ScriptExpression> scripts, EPStatementFormatter formatter) {
        if ((scripts == null) || (scripts.isEmpty())) {
            return;
        }

        for (ScriptExpression part : scripts) {
            if (part.getName() == null) {
                continue;
            }
            formatter.beginExpressionDecl(writer);
            part.toEPL(writer);
        }
    }

    /**
     * Print part.
     *
     * @param writer to write to
     */
    public void toEPL(StringWriter writer) {
        writer.append("expression ");
        if (optionalReturnType != null) {
            writer.append(optionalReturnType);
            writer.append(" ");
        }
        if (optionalEventTypeName != null) {
            writer.append("@type(");
            writer.append(optionalEventTypeName);
            writer.append(") ");
        }
        if (optionalDialect != null && optionalDialect.trim().length() != 0) {
            writer.append(optionalDialect);
            writer.append(":");
        }
        writer.append(name);
        writer.append("(");
        if (parameterNames != null && !parameterNames.isEmpty()) {
            String delimiter = "";
            for (String name : parameterNames) {
                writer.append(delimiter);
                writer.append(name);
                delimiter = ",";
            }
        }
        writer.append(")");
        writer.append(" [");
        writer.append(expressionText);
        writer.append("]");
    }
}
