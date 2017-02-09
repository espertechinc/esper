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
 * Generic single-row method call consists of a method name and parameters, possibly chained.
 */
public class SingleRowMethodExpression extends ExpressionBase {
    private static final long serialVersionUID = -8698785052124988195L;
    private List<DotExpressionItem> chain = new ArrayList<DotExpressionItem>();

    /**
     * Ctor.
     *
     * @param method     method name
     * @param parameters an optiona array of parameters
     */
    public SingleRowMethodExpression(String method, Object[] parameters) {
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
     * Returns the optional method invocation chain for the single-row method consisting of
     * pairs of method name and list of parameters.
     *
     * @return chain of method invocations
     */
    public List<DotExpressionItem> getChain() {
        return chain;
    }

    /**
     * Ctor.
     *
     * @param chain of method invocations with at least one element, each pair a method name and list of parameter expressions
     */
    public SingleRowMethodExpression(List<DotExpressionItem> chain) {
        this.chain = chain;
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.UNARY;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        DotExpressionItem.render(chain, writer, false);
    }
}
