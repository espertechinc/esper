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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.type.BitWiseOpEnum;
import com.espertech.esper.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;

/**
 * Represents the bit-wise operators in an expression tree.
 */
public class ExprBitWiseNode extends ExprNodeBase implements ExprEvaluator {

    private final BitWiseOpEnum bitWiseOpEnum;
    private transient BitWiseOpEnum.Computer bitWiseOpEnumComputer;
    private Class resultType;

    private transient ExprEvaluator[] evaluators;

    private static final long serialVersionUID = 9035943176810365437L;

    /**
     * Ctor.
     *
     * @param bitWiseOpEnum - type of math
     */
    public ExprBitWiseNode(BitWiseOpEnum bitWiseOpEnum) {
        this.bitWiseOpEnum = bitWiseOpEnum;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
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

        evaluators = ExprNodeUtility.getEvaluators(this.getChildNodes());
        for (ExprEvaluator child : evaluators) {
            Class childType = child.getType();
            if ((!JavaClassHelper.isBoolean(childType)) && (!JavaClassHelper.isNumeric(childType))) {
                throw new ExprValidationException("Invalid datatype for bitwise " +
                        childType.getName() + " is not allowed");
            }
        }

        // Determine result type, set up compute function
        Class childTypeOne = evaluators[0].getType();
        Class childTypeTwo = evaluators[1].getType();
        if ((JavaClassHelper.isFloatingPointClass(childTypeOne)) || (JavaClassHelper.isFloatingPointClass(childTypeTwo))) {
            throw new ExprValidationException("Invalid type for bitwise " + bitWiseOpEnum.getComputeDescription() + " operator");
        } else {
            Class childBoxedTypeOne = JavaClassHelper.getBoxedType(childTypeOne);
            Class childBoxedTypeTwo = JavaClassHelper.getBoxedType(childTypeTwo);
            if (childBoxedTypeOne == childBoxedTypeTwo) {
                resultType = childBoxedTypeOne;
                bitWiseOpEnumComputer = bitWiseOpEnum.getComputer(resultType);
            } else {
                throw new ExprValidationException("Bitwise expressions must be of the same type for bitwise " + bitWiseOpEnum.getComputeDescription() + " operator");
            }
        }
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public Class getType() {
        return resultType;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprBitwise(this, bitWiseOpEnum);
        }
        Object valueChildOne = evaluators[0].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        Object valueChildTwo = evaluators[1].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);

        if ((valueChildOne == null) || (valueChildTwo == null)) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprBitwise(null);
            }
            return null;
        }

        // bitWiseOpEnumComputer is initialized by validation
        if (InstrumentationHelper.ENABLED) {
            Object result = bitWiseOpEnumComputer.compute(valueChildOne, valueChildTwo);
            InstrumentationHelper.get().aExprBitwise(result);
            return result;
        }
        return bitWiseOpEnumComputer.compute(valueChildOne, valueChildTwo);
    }

    public boolean equalsNode(ExprNode node) {
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

    private static final Logger log = LoggerFactory.getLogger(ExprBitWiseNode.class);
}
