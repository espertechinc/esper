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
package com.espertech.esper.epl.expression.ops;

import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.type.RelationalOpEnum;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;

/**
 * Represents a lesser or greater then (&lt;/&lt;=/&gt;/&gt;=) expression in a filter expression tree.
 */
public class ExprRelationalOpNodeImpl extends ExprNodeBase implements ExprRelationalOpNode {
    private static final long serialVersionUID = -6170161542681634598L;

    private final RelationalOpEnum relationalOpEnum;

    private transient ExprRelationalOpNodeForge forge;

    /**
     * Ctor.
     *
     * @param relationalOpEnum - type of compare, ie. lt, gt, le, ge
     */
    public ExprRelationalOpNodeImpl(RelationalOpEnum relationalOpEnum) {
        this.relationalOpEnum = relationalOpEnum;
    }

    public ExprEvaluator getExprEvaluator() {
        checkValidated(forge);
        return forge.getExprEvaluator();
    }

    public ExprForge getForge() {
        checkValidated(forge);
        return forge;
    }

    public boolean isConstantResult() {
        return false;
    }

    /**
     * Returns the type of relational op used.
     *
     * @return enum with relational op type
     */
    public RelationalOpEnum getRelationalOpEnum() {
        return relationalOpEnum;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        // Must have 2 child nodes
        if (this.getChildNodes().length != 2) {
            throw new IllegalStateException("Relational op node does not have exactly 2 parameters");
        }

        // Must be either numeric or string
        Class typeOne = JavaClassHelper.getBoxedType(getChildNodes()[0].getForge().getEvaluationType());
        Class typeTwo = JavaClassHelper.getBoxedType(getChildNodes()[1].getForge().getEvaluationType());

        if ((typeOne != String.class) || (typeTwo != String.class)) {
            if (!JavaClassHelper.isNumeric(typeOne)) {
                throw new ExprValidationException("Implicit conversion from datatype '" +
                        (typeOne == null ? "null" : typeOne.getSimpleName()) +
                        "' to numeric is not allowed");
            }
            if (!JavaClassHelper.isNumeric(typeTwo)) {
                throw new ExprValidationException("Implicit conversion from datatype '" +
                        (typeTwo == null ? "null" : typeTwo.getSimpleName()) +
                        "' to numeric is not allowed");
            }
        }

        Class compareType = JavaClassHelper.getCompareToCoercionType(typeOne, typeTwo);
        RelationalOpEnum.Computer computer = relationalOpEnum.getComputer(compareType, typeOne, typeTwo);
        forge = new ExprRelationalOpNodeForge(this, computer);
        return null;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        this.getChildNodes()[0].toEPL(writer, getPrecedence());
        writer.append(relationalOpEnum.getExpressionText());
        this.getChildNodes()[1].toEPL(writer, getPrecedence());
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.RELATIONAL_BETWEEN_IN;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprRelationalOpNodeImpl)) {
            return false;
        }

        ExprRelationalOpNodeImpl other = (ExprRelationalOpNodeImpl) node;

        if (other.relationalOpEnum != this.relationalOpEnum) {
            return false;
        }

        return true;
    }
}
