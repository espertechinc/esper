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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.type.RangeParameter;
import com.espertech.esper.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;

/**
 * Expression for use within crontab to specify a range.
 * <p>
 * Differs from the between-expression since the value returned by evaluating is a cron-value object.
 */
public class ExprNumberSetRange extends ExprNodeBase implements ExprEvaluator {
    private static final Logger log = LoggerFactory.getLogger(ExprNumberSetRange.class);
    private transient ExprEvaluator[] evaluators;
    private static final long serialVersionUID = -3777415170380735662L;

    public void toPrecedenceFreeEPL(StringWriter writer) {
        this.getChildNodes()[0].toEPL(writer, ExprPrecedenceEnum.MINIMUM);
        writer.append(":");
        this.getChildNodes()[1].toEPL(writer, ExprPrecedenceEnum.MINIMUM);
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    @Override
    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public boolean isConstantResult() {
        return this.getChildNodes()[0].isConstantResult() && this.getChildNodes()[1].isConstantResult();
    }

    public boolean equalsNode(ExprNode node) {
        return node instanceof ExprNumberSetRange;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        evaluators = ExprNodeUtility.getEvaluators(this.getChildNodes());
        Class typeOne = evaluators[0].getType();
        Class typeTwo = evaluators[1].getType();
        if ((!(JavaClassHelper.isNumericNonFP(typeOne))) || (!(JavaClassHelper.isNumericNonFP(typeTwo)))) {
            throw new ExprValidationException("Range operator requires integer-type parameters");
        }
        return null;
    }

    public Class getType() {
        return RangeParameter.class;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object valueLower = evaluators[0].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        Object valueUpper = evaluators[1].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (valueLower == null) {
            log.warn("Null value returned for lower bounds value in range parameter, using zero as lower bounds");
            valueLower = 0;
        }
        if (valueUpper == null) {
            log.warn("Null value returned for upper bounds value in range parameter, using max as upper bounds");
            valueUpper = Integer.MAX_VALUE;
        }
        int intValueLower = ((Number) valueLower).intValue();
        int intValueUpper = ((Number) valueUpper).intValue();
        return new RangeParameter(intValueLower, intValueUpper);
    }
}
