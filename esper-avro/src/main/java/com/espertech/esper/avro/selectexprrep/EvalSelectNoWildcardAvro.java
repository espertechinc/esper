/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.avro.selectexprrep;

import com.espertech.esper.avro.core.AvroEventBean;
import com.espertech.esper.avro.core.AvroEventType;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.core.SelectExprProcessor;
import com.espertech.esper.epl.core.eval.SelectExprContext;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import org.apache.avro.generic.GenericData;

public class EvalSelectNoWildcardAvro implements SelectExprProcessor {

    private final SelectExprContext selectExprContext;
    private final AvroEventType resultEventType;

    public EvalSelectNoWildcardAvro(SelectExprContext selectExprContext, EventType resultEventType) {
        this.selectExprContext = selectExprContext;
        this.resultEventType = (AvroEventType) resultEventType;
    }

    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext)
    {
        ExprEvaluator[] expressionNodes = selectExprContext.getExpressionNodes();
        String[] columnNames = selectExprContext.getColumnNames();

        GenericData.Record record = new GenericData.Record(resultEventType.getSchemaAvro());

        // Evaluate all expressions and build a map of name-value pairs
        for (int i = 0; i < expressionNodes.length; i++)
        {
            Object evalResult = expressionNodes[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            record.put(columnNames[i], evalResult);
        }

        return new AvroEventBean(record, resultEventType);
    }

    public EventType getResultEventType()
    {
        return resultEventType;
    }
}