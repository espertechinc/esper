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
package com.espertech.esper.epl.expression.core;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;

import java.io.StringWriter;

/**
 * Represents a substitution value to be substituted in an expression tree, not valid for any purpose of use
 * as an expression, however can take a place in an expression tree.
 */
public class ExprSubstitutionNode extends ExprNodeBase {
    private static final String ERROR_MSG = "Invalid use of substitution parameters marked by '?' in statement, use the prepare method to prepare statements with substitution parameters";
    private Integer index;
    private String name;
    private static final long serialVersionUID = -4238446583735045135L;

    /**
     * Ctor.
     *
     * @param index is the index of the substitution parameter
     */
    public ExprSubstitutionNode(Integer index) {
        this.index = index;
    }

    /**
     * Ctor.
     *
     * @param name is the name of the substitution parameter
     */
    public ExprSubstitutionNode(String name) {
        this.name = name;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        throw new ExprValidationException(ERROR_MSG);
    }

    /**
     * Returns the substitution parameter index (or null if by-name).
     *
     * @return index
     */
    public Integer getIndex() {
        return index;
    }

    /**
     * Returns the substitution parameter name (or null if by-index).
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    public boolean isConstantResult() {
        return false;
    }

    public Class getType() {
        throw new IllegalStateException(ERROR_MSG);
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        throw new EPException(ERROR_MSG);
    }

    public ExprEvaluator getExprEvaluator() {
        throw new EPException(ERROR_MSG);
    }

    public ExprForge getForge() {
        throw new IllegalStateException(ERROR_MSG);
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append("?");
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprSubstitutionNode)) {
            return false;
        }

        return true;
    }
}
