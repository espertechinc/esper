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
 * Dot-expresson item representing a call that has an name and parameters.
 */
public class DotExpressionItemCall extends DotExpressionItem implements Serializable {

    private String name;
    private List<Expression> parameters;

    /**
     * Ctor.
     */
    public DotExpressionItemCall() {
    }

    /**
     * Ctor.
     *
     * @param name       the name
     * @param parameters parameters
     */
    public DotExpressionItemCall(String name, List<Expression> parameters) {
        this.name = name;
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

    public void renderItem(StringWriter writer) {
        writer.write(name);
        writer.write("(");
        String delimiter = "";
        for (Expression param : parameters) {
            writer.write(delimiter);
            delimiter = ",";
            param.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        }
        writer.write(")");
    }
}
