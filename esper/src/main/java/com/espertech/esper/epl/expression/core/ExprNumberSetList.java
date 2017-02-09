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
import com.espertech.esper.type.*;
import com.espertech.esper.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Expression for use within crontab to specify a list of values.
 */
public class ExprNumberSetList extends ExprNodeBase implements ExprEvaluator {
    private static final Logger log = LoggerFactory.getLogger(ExprNumberSetList.class);
    private transient ExprEvaluator[] evaluators;
    private static final long serialVersionUID = 4941618470342360450L;

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        String delimiter = "";

        writer.append('[');
        Iterator<ExprNode> it = Arrays.asList(this.getChildNodes()).iterator();
        do {
            ExprNode expr = it.next();
            writer.append(delimiter);
            expr.toEPL(writer, ExprPrecedenceEnum.MINIMUM);
            delimiter = ",";
        }
        while (it.hasNext());
        writer.append(']');
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean isConstantResult() {
        for (ExprNode child : this.getChildNodes()) {
            if (!child.isConstantResult()) {
                return false;
            }
        }
        return true;
    }

    public boolean equalsNode(ExprNode node) {
        return node instanceof ExprNumberSetList;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        // all nodes must either be int, frequency or range
        evaluators = ExprNodeUtility.getEvaluators(this.getChildNodes());
        for (ExprEvaluator child : evaluators) {
            Class type = child.getType();
            if ((type == FrequencyParameter.class) || (type == RangeParameter.class)) {
                continue;
            }
            if (!(JavaClassHelper.isNumericNonFP(type))) {
                throw new ExprValidationException("Frequency operator requires an integer-type parameter");
            }
        }
        return null;
    }

    public Class getType() {
        return ListParameter.class;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        List<NumberSetParameter> parameters = new ArrayList<NumberSetParameter>();
        for (ExprEvaluator child : evaluators) {
            Object value = child.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            if (value == null) {
                log.info("Null value returned for lower bounds value in list parameter, skipping parameter");
                continue;
            }
            if ((value instanceof FrequencyParameter) || (value instanceof RangeParameter)) {
                parameters.add((NumberSetParameter) value);
                continue;
            }

            int intValue = ((Number) value).intValue();
            parameters.add(new IntParameter(intValue));
        }
        if (parameters.isEmpty()) {
            log.warn("Empty list of values in list parameter, using upper bounds");
            parameters.add(new IntParameter(Integer.MAX_VALUE));
        }
        return new ListParameter(parameters);
    }
}
