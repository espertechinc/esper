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
package com.espertech.esper.common.internal.epl.expression.ops;

import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.type.BitWiseOpEnum;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.io.StringWriter;

/**
 * Represents the bit-wise operators in an expression tree.
 */
public class ExprBitWiseNode extends ExprNodeBase {
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
        EPType lhsType = getChildNodes()[0].getForge().getEvaluationType();
        EPType rhsType = getChildNodes()[1].getForge().getEvaluationType();
        checkNumericOrBoolean(lhsType);
        checkNumericOrBoolean(rhsType);

        EPTypeClass lhsTypeClass = JavaClassHelper.getBoxedType((EPTypeClass) lhsType);
        EPTypeClass rhsTypeClass = JavaClassHelper.getBoxedType((EPTypeClass) rhsType);

        if ((JavaClassHelper.isFloatingPointClass(lhsTypeClass)) || (JavaClassHelper.isFloatingPointClass(rhsTypeClass))) {
            throw new ExprValidationException("Invalid type for bitwise " + bitWiseOpEnum.getComputeDescription() + " operator");
        }
        if (lhsTypeClass != rhsTypeClass) {
            throw new ExprValidationException("Bitwise expressions must be of the same type for bitwise " + bitWiseOpEnum.getComputeDescription() + " operator");
        }
        BitWiseOpEnum.Computer computer = bitWiseOpEnum.getComputer(lhsTypeClass.getType());
        forge = new ExprBitWiseNodeForge(this, lhsTypeClass, computer);
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

    public void toPrecedenceFreeEPL(StringWriter writer, ExprNodeRenderableFlags flags) {
        getChildNodes()[0].toEPL(writer, getPrecedence(), flags);
        writer.append(bitWiseOpEnum.getComputeDescription());
        getChildNodes()[1].toEPL(writer, getPrecedence(), flags);
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.BITWISE;
    }

    private void checkNumericOrBoolean(EPType childType) throws ExprValidationException {
        if (childType == null || childType == EPTypeNull.INSTANCE || (!JavaClassHelper.isTypeBoolean(childType)) && (!JavaClassHelper.isNumeric(childType))) {
            throw new ExprValidationException("Invalid datatype for binary operator, " +
                (childType == null ? "null" : childType.getTypeName()) + " is not allowed");
        }
    }
}
