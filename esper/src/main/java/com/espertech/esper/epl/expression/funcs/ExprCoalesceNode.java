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
import com.espertech.esper.util.CoercionException;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;

/**
 * Represents the COALESCE(a,b,...) function is an expression tree.
 */
public class ExprCoalesceNode extends ExprNodeBase {
    private static final long serialVersionUID = -8276568753875819730L;

    private transient ExprCoalesceNodeForge forge;

    public ExprEvaluator getExprEvaluator() {
        checkValidated(forge);
        return forge.getExprEvaluator();
    }

    public ExprForge getForge() {
        checkValidated(forge);
        return forge;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (this.getChildNodes().length < 2) {
            throw new ExprValidationException("Coalesce node must have at least 2 parameters");
        }

        // get child expression types
        Class[] childTypes = new Class[getChildNodes().length];
        for (int i = 0; i < this.getChildNodes().length; i++) {
            childTypes[i] = this.getChildNodes()[i].getForge().getEvaluationType();
        }

        // determine coercion type
        Class resultType;
        try {
            resultType = JavaClassHelper.getCommonCoercionType(childTypes);
        } catch (CoercionException ex) {
            throw new ExprValidationException("Implicit conversion not allowed: " + ex.getMessage());
        }

        // determine which child nodes need numeric coercion
        boolean[] isNumericCoercion = new boolean[getChildNodes().length];
        for (int i = 0; i < this.getChildNodes().length; i++) {
            ExprNode node = this.getChildNodes()[i];
            if ((JavaClassHelper.getBoxedType(node.getForge().getEvaluationType()) != resultType) &&
                    (node.getForge().getEvaluationType() != null) && (resultType != null)) {
                if (!JavaClassHelper.isNumeric(resultType)) {
                    throw new ExprValidationException("Implicit conversion from datatype '" +
                            resultType.getSimpleName() +
                            "' to " + node.getForge().getEvaluationType() + " is not allowed");
                }
                isNumericCoercion[i] = true;
            }
        }

        forge = new ExprCoalesceNodeForge(this, resultType, isNumericCoercion);
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        ExprNodeUtilityCore.toExpressionStringWFunctionName("coalesce", this.getChildNodes(), writer);
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprCoalesceNode)) {
            return false;
        }
        return true;
    }
}
