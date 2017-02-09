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
 * Dot-expresson item is for use in "(inner_expression).dot_expression".
 * <p>
 * Each item represent an individual chain item and may either be a method name with method parameters,
 * or a (nested) property name typically with an empty list of parameters or for mapped properties a non-empty list of parameters.
 */
public class DotExpressionItem implements Serializable {
    private static final long serialVersionUID = 4189610785425631920L;

    private String name;
    private List<Expression> parameters;
    private boolean property; // relevant if there are no parameters

    /**
     * Ctor.
     */
    public DotExpressionItem() {
    }

    /**
     * Ctor.
     *
     * @param name       the property (or nested property) or method name
     * @param parameters are optional and should only be provided if this chain item is a method;
     *                   Parameters are expressions for parameters to the method (use only for methods and not for properties unless mapped property).
     * @param isProperty true if this is a nested property name
     */
    public DotExpressionItem(String name, List<Expression> parameters, boolean isProperty) {
        this.name = name;
        this.property = isProperty;
        this.parameters = parameters;
    }

    /**
     * Returns method name or nested property name.
     *
     * @return method name or nested property name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the method name or nested property name.
     *
     * @param name method name or nested property name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns method parameters or parameters for mapped properties or empty list if this item represents a simple nested property.
     *
     * @return parameter expressions
     */
    public List<Expression> getParameters() {
        return parameters;
    }

    /**
     * Sets method parameters or parameters for mapped properties or empty list if this item represents a simple nested property.
     *
     * @param parameters expressions to set
     */
    public void setParameters(List<Expression> parameters) {
        this.parameters = parameters;
    }

    /**
     * Returns true if this dot-item is a property name.
     *
     * @return true for property, false for method
     */
    public boolean isProperty() {
        return property;
    }

    /**
     * Set to true if this dot-item is a property name.
     *
     * @param property true for property, false for method
     */
    public void setProperty(boolean property) {
        this.property = property;
    }

    /**
     * Render to EPL.
     *
     * @param chain     chain to render
     * @param writer    writer to output to
     * @param prefixDot indicator whether to prefix with "."
     */
    protected static void render(List<DotExpressionItem> chain, StringWriter writer, boolean prefixDot) {
        String delimiterOuter = prefixDot ? "." : "";
        for (DotExpressionItem item : chain) {
            writer.write(delimiterOuter);
            writer.write(item.name);

            if (!item.isProperty() || !item.parameters.isEmpty()) {
                writer.write("(");
                if (!item.parameters.isEmpty()) {
                    String delimiter = "";
                    for (Expression param : item.parameters) {
                        writer.write(delimiter);
                        delimiter = ",";
                        param.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
                    }
                }
                writer.write(")");
            }
            delimiterOuter = ".";
        }
    }
}
