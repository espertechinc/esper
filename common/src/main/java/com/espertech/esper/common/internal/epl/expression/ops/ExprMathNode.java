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

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.type.MathArithTypeEnum;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.io.StringWriter;
import java.math.BigDecimal;

/**
 * Represents a simple Math (+/-/divide/*) in a filter expression tree.
 */
public class ExprMathNode extends ExprNodeBase {
    private final MathArithTypeEnum mathArithTypeEnum;
    private final boolean isIntegerDivision;
    private final boolean isDivisionByZeroReturnsNull;

    private transient ExprMathNodeForge forge;

    /**
     * Ctor.
     *
     * @param mathArithTypeEnum           - type of math
     * @param isIntegerDivision           - false for division returns double, true for using Java-standard integer division
     * @param isDivisionByZeroReturnsNull - false for division-by-zero returns infinity, true for null
     */
    public ExprMathNode(MathArithTypeEnum mathArithTypeEnum, boolean isIntegerDivision, boolean isDivisionByZeroReturnsNull) {
        this.mathArithTypeEnum = mathArithTypeEnum;
        this.isIntegerDivision = isIntegerDivision;
        this.isDivisionByZeroReturnsNull = isDivisionByZeroReturnsNull;
    }

    public ExprEvaluator getExprEvaluator() {
        checkValidated(forge);
        return forge.getExprEvaluator();
    }

    public ExprForge getForge() {
        checkValidated(forge);
        return forge;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (this.getChildNodes().length != 2) {
            throw new ExprValidationException("Arithmatic node must have 2 parameters");
        }

        for (ExprNode child : this.getChildNodes()) {
            ExprNodeUtilityValidate.validateReturnsNumeric(child.getForge());
        }

        // Determine result type, set up compute function
        ExprNode lhs = this.getChildNodes()[0];
        ExprNode rhs = this.getChildNodes()[1];
        EPTypeClass lhsType = (EPTypeClass) lhs.getForge().getEvaluationType();
        EPTypeClass rhsType = (EPTypeClass) rhs.getForge().getEvaluationType();

        EPTypeClass resultType;
        if ((lhsType.getType() == short.class || lhsType.getType() == Short.class) &&
            (rhsType.getType() == short.class || rhsType.getType() == Short.class)) {
            resultType = EPTypePremade.INTEGERBOXED.getEPType();
        } else if ((lhsType.getType() == byte.class || lhsType.getType() == Byte.class) &&
            (rhsType.getType() == byte.class || rhsType.getType() == Byte.class)) {
            resultType = EPTypePremade.INTEGERBOXED.getEPType();
        } else if (lhsType.equals(rhsType)) {
            resultType = JavaClassHelper.getBoxedType(rhsType);
        } else {
            resultType = JavaClassHelper.getArithmaticCoercionType(lhsType, rhsType);
        }

        if ((mathArithTypeEnum == MathArithTypeEnum.DIVIDE) && (!isIntegerDivision)) {
            if (resultType.getType() != BigDecimal.class) {
                resultType = EPTypePremade.DOUBLEBOXED.getEPType();
            }
        }

        MathArithTypeEnum.Computer arithTypeEnumComputer = mathArithTypeEnum.getComputer(resultType, lhsType, rhsType, isIntegerDivision, isDivisionByZeroReturnsNull, validationContext.getClasspathImportService().getDefaultMathContext());
        forge = new ExprMathNodeForge(this, arithTypeEnumComputer, resultType);
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public void toPrecedenceFreeEPL(StringWriter writer, ExprNodeRenderableFlags flags) {
        this.getChildNodes()[0].toEPL(writer, getPrecedence(), flags);
        writer.append(mathArithTypeEnum.getExpressionText());
        this.getChildNodes()[1].toEPL(writer, getPrecedence(), flags);
    }

    public ExprPrecedenceEnum getPrecedence() {
        if (mathArithTypeEnum == MathArithTypeEnum.MULTIPLY ||
            mathArithTypeEnum == MathArithTypeEnum.DIVIDE ||
            mathArithTypeEnum == MathArithTypeEnum.MODULO) {
            return ExprPrecedenceEnum.MULTIPLY;
        } else {
            return ExprPrecedenceEnum.ADDITIVE;
        }
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprMathNode)) {
            return false;
        }

        ExprMathNode other = (ExprMathNode) node;

        if (other.mathArithTypeEnum != this.mathArithTypeEnum) {
            return false;
        }

        return true;
    }

    /**
     * Returns the type of math.
     *
     * @return math type
     */
    public MathArithTypeEnum getMathArithTypeEnum() {
        return mathArithTypeEnum;
    }
}
