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
import com.espertech.esper.type.RelationalOpEnum;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;

/**
 * Represents a lesser or greater then (&lt;/&lt;=/&gt;/&gt;=) expression in a filter expression tree.
 */
public class ExprRelationalOpNodeImpl extends ExprNodeBase implements ExprEvaluator, ExprRelationalOpNode {
    private final RelationalOpEnum relationalOpEnum;
    private transient RelationalOpEnum.Computer computer;
    private transient ExprEvaluator[] evaluators;
    private static final long serialVersionUID = -6170161542681634598L;

    /**
     * Ctor.
     *
     * @param relationalOpEnum - type of compare, ie. lt, gt, le, ge
     */
    public ExprRelationalOpNodeImpl(RelationalOpEnum relationalOpEnum) {
        this.relationalOpEnum = relationalOpEnum;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
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
        evaluators = ExprNodeUtility.getEvaluators(this.getChildNodes());

        // Must be either numeric or string
        Class typeOne = JavaClassHelper.getBoxedType(evaluators[0].getType());
        Class typeTwo = JavaClassHelper.getBoxedType(evaluators[1].getType());

        if ((typeOne != String.class) || (typeTwo != String.class)) {
            if (!JavaClassHelper.isNumeric(typeOne)) {
                throw new ExprValidationException("Implicit conversion from datatype '" +
                        typeOne.getSimpleName() +
                        "' to numeric is not allowed");
            }
            if (!JavaClassHelper.isNumeric(typeTwo)) {
                throw new ExprValidationException("Implicit conversion from datatype '" +
                        typeTwo.getSimpleName() +
                        "' to numeric is not allowed");
            }
        }

        Class compareType = JavaClassHelper.getCompareToCoercionType(typeOne, typeTwo);

        computer = relationalOpEnum.getComputer(compareType, typeOne, typeTwo);
        return null;
    }

    public Class getType() {
        return Boolean.class;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprRelOp(this, relationalOpEnum.getExpressionText());
        }
        Object valueLeft = evaluators[0].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (valueLeft == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprRelOp(null);
            }
            return null;
        }

        Object valueRight = evaluators[1].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (valueRight == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprRelOp(null);
            }
            return null;
        }

        if (InstrumentationHelper.ENABLED) {
            Boolean result = computer.compare(valueLeft, valueRight);
            InstrumentationHelper.get().aExprRelOp(result);
            return result;
        }
        return computer.compare(valueLeft, valueRight);
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        this.getChildNodes()[0].toEPL(writer, getPrecedence());
        writer.append(relationalOpEnum.getExpressionText());
        this.getChildNodes()[1].toEPL(writer, getPrecedence());
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.RELATIONAL_BETWEEN_IN;
    }

    public boolean equalsNode(ExprNode node) {
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
