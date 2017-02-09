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
 * Static method call consists of a class name and method name.
 */
public class StaticMethodExpression extends ExpressionBase {
    private static final long serialVersionUID = -8876482797010708113L;

    private String className;
    private List<DotExpressionItem> chain = new ArrayList<DotExpressionItem>();

    /**
     * Ctor.
     *
     * @param className  class name providing the static method
     * @param method     method name
     * @param parameters an optiona array of parameters
     */
    public StaticMethodExpression(String className, String method, Object[] parameters) {
        this.className = className;

        List<Expression> parameterList = new ArrayList<Expression>();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i] instanceof Expression) {
                parameterList.add((Expression) parameters[i]);
            } else {
                parameterList.add(new ConstantExpression(parameters[i]));
            }
        }
        chain.add(new DotExpressionItem(method, parameterList, false));
    }

    /**
     * Returns the chain of method invocations, each pair a method name and list of parameter expressions
     *
     * @return method chain
     */
    public List<DotExpressionItem> getChain() {
        return chain;
    }

    /**
     * Sets the chain of method invocations, each pair a method name and list of parameter expressions
     *
     * @param chain method chain
     */
    public void setChain(List<DotExpressionItem> chain) {
        this.chain = chain;
    }

    /**
     * Ctor.
     *
     * @param className class name providing the static method
     * @param chain     method chain
     */
    public StaticMethodExpression(String className, List<DotExpressionItem> chain) {
        this.className = className;
        this.chain = chain;
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.UNARY;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.write(className);
        DotExpressionItem.render(chain, writer, true);
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
     * @param className class name
     */
    public void setClassName(String className) {
        this.className = className;
    }
}
