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
import com.espertech.esper.type.FrequencyParameter;
import com.espertech.esper.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;

/**
 * Expression for use within crontab to specify a frequency.
 */
public class ExprNumberSetFrequency extends ExprNodeBase implements ExprEvaluator {
    private static final Logger log = LoggerFactory.getLogger(ExprNumberSetFrequency.class);
    private transient ExprEvaluator evaluator;
    private static final long serialVersionUID = -5389069399403078192L;

    @Override
    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append("*/");
        this.getChildNodes()[0].toEPL(writer, ExprPrecedenceEnum.MINIMUM);
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.MINIMUM;
    }

    public boolean isConstantResult() {
        return this.getChildNodes()[0].isConstantResult();
    }

    public boolean equalsNode(ExprNode node) {
        if (!(node instanceof ExprNumberSetFrequency)) {
            return false;
        }
        return true;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        evaluator = this.getChildNodes()[0].getExprEvaluator();
        Class type = evaluator.getType();
        if (!(JavaClassHelper.isNumericNonFP(type))) {
            throw new ExprValidationException("Frequency operator requires an integer-type parameter");
        }
        return null;
    }

    public Class getType() {
        return FrequencyParameter.class;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object value = evaluator.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (value == null) {
            log.warn("Null value returned for frequency parameter");
            return new FrequencyParameter(Integer.MAX_VALUE);
        } else {
            int intValue = ((Number) value).intValue();
            return new FrequencyParameter(intValue);
        }
    }
}
