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
import com.espertech.esper.type.BitWiseOpEnum;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;

/**
 * Represents the bit-wise operators in an expression tree.
 */
public class ExprBitWiseNode extends ExprNodeBase {
    private static final long serialVersionUID = 9035943176810365437L;

    private final BitWiseOpEnum bitWiseOpEnum;

    private transient ExprBitWiseNodeForge forge;

    /**
     * Ctor.
     *
     * @param bitWiseOpEnum - type of math
     */
    public ExprBitWiseNode(BitWiseOpEnum bitWiseOpEnum) {
        this.bitWiseOpEnum = bitWiseOpEnum;
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
     * Returns the bitwise operator.
     *
     * @return operator
     */
    public BitWiseOpEnum getBitWiseOpEnum() {
        return bitWiseOpEnum;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (this.getChildNodes().length != 2) {
            throw new ExprValidationException("BitWise node must have 2 parameters");
        }

        Class typeOne = JavaClassHelper.getBoxedType(getChildNodes()[0].getForge().getEvaluationType());
        Class typeTwo = JavaClassHelper.getBoxedType(getChildNodes()[0].getForge().getEvaluationType());
        checkNumericOrBoolean(typeOne);
        checkNumericOrBoolean(typeTwo);

        if ((JavaClassHelper.isFloatingPointClass(typeOne)) || (JavaClassHelper.isFloatingPointClass(typeTwo))) {
            throw new ExprValidationException("Invalid type for bitwise " + bitWiseOpEnum.getComputeDescription() + " operator");
        }
        if (typeOne != typeTwo) {
            throw new ExprValidationException("Bitwise expressions must be of the same type for bitwise " + bitWiseOpEnum.getComputeDescription() + " operator");
        }
        BitWiseOpEnum.Computer computer = bitWiseOpEnum.getComputer(typeOne);
        forge = new ExprBitWiseNodeForge(this, typeOne, computer);
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprBitWiseNode)) {
            return false;
        }

        ExprBitWiseNode other = (ExprBitWiseNode) node;

        if (other.bitWiseOpEnum != bitWiseOpEnum) {
            return false;
        }

        return true;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        getChildNodes()[0].toEPL(writer, getPrecedence());
        writer.append(bitWiseOpEnum.getComputeDescription());
        getChildNodes()[1].toEPL(writer, getPrecedence());
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.BITWISE;
    }

    private void checkNumericOrBoolean(Class childType) throws ExprValidationException {
        if ((!JavaClassHelper.isBoolean(childType)) && (!JavaClassHelper.isNumeric(childType))) {
            throw new ExprValidationException("Invalid datatype for bitwise " +
                    childType.getName() + " is not allowed");
        }
    }
}
