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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.Map;

public class SelectExprProcessorEvalAvroMapToAvro implements ExprEvaluator {

    private final ExprEvaluator eval;
    private final Schema inner;

    public SelectExprProcessorEvalAvroMapToAvro(ExprEvaluator eval, Schema schema, String columnName) {
        this.eval = eval;
        this.inner = schema.getField(columnName).schema();
        if (!(inner.getType() == Schema.Type.RECORD)) {
            throw new IllegalStateException("Column '" + columnName + "' is not a record but schema " + inner);
        }
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Map<String, Object> map = (Map<String, Object>) eval.evaluate(eventsPerStream, isNewData, context);
        if (map == null) {
            return null;
        }
        GenericData.Record record = new GenericData.Record(inner);
        for (Map.Entry<String, Object> row : map.entrySet()) {
            record.put(row.getKey(), row.getValue());
        }
        return record;
    }

    public Class getType() {
        return GenericData.Record.class;
    }
}
