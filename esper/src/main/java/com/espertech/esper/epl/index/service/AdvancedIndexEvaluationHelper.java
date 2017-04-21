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
package com.espertech.esper.epl.index.service;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

public class AdvancedIndexEvaluationHelper {

    public static double evalDoubleColumn(ExprEvaluator col, String indexName, String colName, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        Number number = (Number) col.evaluate(eventsPerStream, newData, exprEvaluatorContext);
        if (number == null) {
            throw invalidColumnValue(indexName, colName, null, "non-null");
        }
        return number.doubleValue();
    }

    public static double evalDoubleParameter(ExprEvaluator param, String indexName, String parameterName, ExprEvaluatorContext exprEvaluatorContext) {
        Number number = (Number) param.evaluate(null, true, exprEvaluatorContext);
        if (number == null) {
            throw invalidParameterValue(indexName, parameterName, null, "non-null");
        }
        return number.doubleValue();
    }

    public static int evalIntParameter(ExprEvaluator param, String indexName, String parameterName, ExprEvaluatorContext exprEvaluatorContext) {
        Integer number = (Integer) param.evaluate(null, true, exprEvaluatorContext);
        if (number == null) {
            throw invalidParameterValue(indexName, parameterName, null, "non-null");
        }
        return number;
    }

    public static EPException invalidParameterValue(String indexName, String parameterName, Object value, String expected) {
        return new EPException("Invalid value for index '" + indexName + "' parameter '" + parameterName + "' received " + (value == null ? "null" : value.toString()) + " and expected " + expected);
    }

    public static EPException invalidColumnValue(String indexName, String parameterName, Object value, String expected) {
        return new EPException("Invalid value for index '" + indexName + "' column '" + parameterName + "' received " + (value == null ? "null" : value.toString()) + " and expected " + expected);
    }
}
