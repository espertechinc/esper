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
package com.espertech.esper.epl.expression.funcs;

import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;

/**
 * Represents the MAX(a,b) and MIN(a,b) functions is an expression tree.
 */
public class ExprMinMaxRowNode extends ExprNodeBase {
    private static final long serialVersionUID = -5244192656164983580L;

    private final MinMaxTypeEnum minMaxTypeEnum;

    private transient ExprMinMaxRowNodeForge forge;

    /**
     * Ctor.
     *
     * @param minMaxTypeEnum - type of compare
     */
    public ExprMinMaxRowNode(MinMaxTypeEnum minMaxTypeEnum) {
        this.minMaxTypeEnum = minMaxTypeEnum;
    }

    public ExprEvaluator getExprEvaluator() {
        checkValidated(forge);
        return forge.getExprEvaluator();
    }

    public ExprForge getForge() {
        checkValidated(forge);
        return forge;
    }

    /**
     * Returns the indicator for minimum or maximum.
     *
     * @return min/max indicator
     */
    public MinMaxTypeEnum getMinMaxTypeEnum() {
        return minMaxTypeEnum;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (this.getChildNodes().length < 2) {
            throw new ExprValidationException("MinMax node must have at least 2 parameters");
        }

        for (ExprNode child : getChildNodes()) {
            Class childType = child.getForge().getEvaluationType();
            if (!JavaClassHelper.isNumeric(childType)) {
                throw new ExprValidationException("Implicit conversion from datatype '" +
                        childType.getSimpleName() +
                        "' to numeric is not allowed");
            }
        }

        // Determine result type, set up compute function
        Class childTypeOne = getChildNodes()[0].getForge().getEvaluationType();
        Class childTypeTwo = getChildNodes()[1].getForge().getEvaluationType();
        Class resultType = JavaClassHelper.getArithmaticCoercionType(childTypeOne, childTypeTwo);

        for (int i = 2; i < this.getChildNodes().length; i++) {
            resultType = JavaClassHelper.getArithmaticCoercionType(resultType, getChildNodes()[i].getForge().getEvaluationType());
        }
        forge = new ExprMinMaxRowNodeForge(this, resultType);

        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append(minMaxTypeEnum.getExpressionText());
        writer.append('(');

        this.getChildNodes()[0].toEPL(writer, ExprPrecedenceEnum.MINIMUM);
        writer.append(',');
        this.getChildNodes()[1].toEPL(writer, ExprPrecedenceEnum.MINIMUM);

        for (int i = 2; i < this.getChildNodes().length; i++) {
            writer.append(',');
            this.getChildNodes()[i].toEPL(writer, ExprPrecedenceEnum.MINIMUM);
        }

        writer.append(')');
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprMinMaxRowNode)) {
            return false;
        }

        ExprMinMaxRowNode other = (ExprMinMaxRowNode) node;

        if (other.minMaxTypeEnum != this.minMaxTypeEnum) {
            return false;
        }

        return true;
    }
}
