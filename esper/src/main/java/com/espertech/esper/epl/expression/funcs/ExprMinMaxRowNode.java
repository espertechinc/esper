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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.type.MinMaxTypeEnum;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.SimpleNumberBigDecimalCoercer;
import com.espertech.esper.util.SimpleNumberBigIntegerCoercer;
import com.espertech.esper.util.SimpleNumberCoercerFactory;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Represents the MAX(a,b) and MIN(a,b) functions is an expression tree.
 */
public class ExprMinMaxRowNode extends ExprNodeBase implements ExprEvaluator {
    private MinMaxTypeEnum minMaxTypeEnum;
    private Class resultType;
    private transient MinMaxTypeEnum.Computer computer;
    private transient ExprEvaluator[] evaluators;
    private static final long serialVersionUID = -5244192656164983580L;

    /**
     * Ctor.
     *
     * @param minMaxTypeEnum - type of compare
     */
    public ExprMinMaxRowNode(MinMaxTypeEnum minMaxTypeEnum) {
        this.minMaxTypeEnum = minMaxTypeEnum;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
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
        evaluators = ExprNodeUtility.getEvaluators(this.getChildNodes());

        for (ExprEvaluator child : evaluators) {
            Class childType = child.getType();
            if (!JavaClassHelper.isNumeric(childType)) {
                throw new ExprValidationException("Implicit conversion from datatype '" +
                        childType.getSimpleName() +
                        "' to numeric is not allowed");
            }
        }

        // Determine result type, set up compute function
        Class childTypeOne = evaluators[0].getType();
        Class childTypeTwo = evaluators[1].getType();
        resultType = JavaClassHelper.getArithmaticCoercionType(childTypeOne, childTypeTwo);

        for (int i = 2; i < this.getChildNodes().length; i++) {
            resultType = JavaClassHelper.getArithmaticCoercionType(resultType, evaluators[i].getType());
        }

        ExprNode[] childNodes = this.getChildNodes();
        if (resultType == BigInteger.class) {
            SimpleNumberBigIntegerCoercer[] convertors = new SimpleNumberBigIntegerCoercer[childNodes.length];
            for (int i = 0; i < childNodes.length; i++) {
                convertors[i] = SimpleNumberCoercerFactory.getCoercerBigInteger(evaluators[i].getType());
            }
            computer = new MinMaxTypeEnum.ComputerBigIntCoerce(evaluators, convertors, minMaxTypeEnum == MinMaxTypeEnum.MAX);
        } else if (resultType == BigDecimal.class) {
            SimpleNumberBigDecimalCoercer[] convertors = new SimpleNumberBigDecimalCoercer[childNodes.length];
            for (int i = 0; i < childNodes.length; i++) {
                convertors[i] = SimpleNumberCoercerFactory.getCoercerBigDecimal(evaluators[i].getType());
            }
            computer = new MinMaxTypeEnum.ComputerBigDecCoerce(evaluators, convertors, minMaxTypeEnum == MinMaxTypeEnum.MAX);
        } else {
            if (minMaxTypeEnum == MinMaxTypeEnum.MAX) {
                computer = new MinMaxTypeEnum.MaxComputerDoubleCoerce(evaluators);
            } else {
                computer = new MinMaxTypeEnum.MinComputerDoubleCoerce(evaluators);
            }
        }
        return null;
    }

    public Class getType() {
        return resultType;
    }

    public boolean isConstantResult() {
        return false;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprMinMaxRow(this);
        }
        Number result = computer.execute(eventsPerStream, isNewData, exprEvaluatorContext);

        if (InstrumentationHelper.ENABLED) {
            Number minmax = null;
            if (result != null) {
                minmax = JavaClassHelper.coerceBoxed(result, resultType);
            }
            InstrumentationHelper.get().aExprMinMaxRow(minmax);
            return minmax;
        }

        if (result == null) {
            return null;
        }
        return JavaClassHelper.coerceBoxed(result, resultType);
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

    public boolean equalsNode(ExprNode node) {
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
