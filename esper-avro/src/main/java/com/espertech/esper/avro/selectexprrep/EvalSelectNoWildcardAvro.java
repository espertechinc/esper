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
package com.espertech.esper.avro.selectexprrep;

import com.espertech.esper.avro.core.AvroEventType;
import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.core.*;
import com.espertech.esper.epl.core.eval.SelectExprContext;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.util.TypeWidener;
import com.espertech.esper.util.TypeWidenerCustomizer;
import com.espertech.esper.util.TypeWidenerFactory;
import org.apache.avro.generic.GenericData;

import java.util.Collection;

public class EvalSelectNoWildcardAvro implements SelectExprProcessor {

    private final SelectExprContext selectExprContext;
    private final AvroEventType resultEventType;
    private final ExprEvaluator[] evaluator;

    public EvalSelectNoWildcardAvro(SelectExprContext selectExprContext, EventType resultEventType, String statementName, String engineURI) throws ExprValidationException {
        this.selectExprContext = selectExprContext;
        this.resultEventType = (AvroEventType) resultEventType;

        this.evaluator = new ExprEvaluator[selectExprContext.getExpressionNodes().length];
        TypeWidenerCustomizer typeWidenerCustomizer = selectExprContext.getEventAdapterService().getTypeWidenerCustomizer(resultEventType);
        for (int i = 0; i < evaluator.length; i++) {
            ExprEvaluator eval = selectExprContext.getExpressionNodes()[i];
            evaluator[i] = eval;

            if (eval instanceof SelectExprProcessorEvalByGetterFragment) {
                evaluator[i] = handleFragment((SelectExprProcessorEvalByGetterFragment) eval);
            } else if (eval instanceof SelectExprProcessorEvalStreamInsertUnd) {
                SelectExprProcessorEvalStreamInsertUnd und = (SelectExprProcessorEvalStreamInsertUnd) eval;
                evaluator[i] = new ExprEvaluator() {
                    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
                        EventBean event = eventsPerStream[und.getStreamNum()];
                        if (event == null) {
                            return null;
                        }
                        return event.getUnderlying();
                    }

                    public Class getType() {
                        return GenericData.Record.class;
                    }
                };
            } else if (eval instanceof SelectExprProcessorEvalTypableMap) {
                SelectExprProcessorEvalTypableMap typableMap = (SelectExprProcessorEvalTypableMap) eval;
                evaluator[i] = new SelectExprProcessorEvalAvroMapToAvro(typableMap.getInnerEvaluator(), ((AvroEventType) resultEventType).getSchemaAvro(), selectExprContext.getColumnNames()[i]);
            } else if (eval instanceof SelectExprProcessorEvalStreamInsertNamedWindow) {
                SelectExprProcessorEvalStreamInsertNamedWindow nw = (SelectExprProcessorEvalStreamInsertNamedWindow) eval;
                evaluator[i] = new ExprEvaluator() {
                    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
                        EventBean event = eventsPerStream[nw.getStreamNum()];
                        if (event == null) {
                            return null;
                        }
                        return event.getUnderlying();
                    }

                    public Class getType() {
                        return GenericData.Record.class;
                    }
                };

            } else if (eval.getType() != null && eval.getType().isArray()) {
                TypeWidener widener = TypeWidenerFactory.getArrayToCollectionCoercer(eval.getType().getComponentType());
                if (eval.getType() == byte[].class) {
                    widener = TypeWidenerFactory.BYTE_ARRAY_TO_BYTE_BUFFER_COERCER;
                }
                evaluator[i] = new SelectExprProcessorEvalAvroArrayCoercer(eval, widener);
            } else {
                String propertyName = selectExprContext.getColumnNames()[i];
                Class propertyType = resultEventType.getPropertyType(propertyName);
                TypeWidener widener = TypeWidenerFactory.getCheckPropertyAssignType(propertyName, eval.getType(), propertyType, propertyName, true, typeWidenerCustomizer, statementName, engineURI);
                if (widener != null) {
                    evaluator[i] = new SelectExprProcessorEvalAvroArrayCoercer(eval, widener);
                }
            }
        }
    }

    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        String[] columnNames = selectExprContext.getColumnNames();

        GenericData.Record record = new GenericData.Record(resultEventType.getSchemaAvro());

        // Evaluate all expressions and build a map of name-value pairs
        for (int i = 0; i < evaluator.length; i++) {
            Object evalResult = evaluator[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            record.put(columnNames[i], evalResult);
        }

        return selectExprContext.getEventAdapterService().adapterForTypedAvro(record, resultEventType);
    }

    public EventType getResultEventType() {
        return resultEventType;
    }

    private ExprEvaluator handleFragment(SelectExprProcessorEvalByGetterFragment eval) {
        if (eval.getType() == GenericData.Record[].class) {
            return new SelectExprProcessorEvalByGetterFragmentAvroArray(eval.getStreamNum(), eval.getGetter(), Collection.class);
        }
        if (eval.getType() == GenericData.Record.class) {
            return new SelectExprProcessorEvalByGetterFragmentAvro(eval.getStreamNum(), eval.getGetter(), GenericData.Record.class);
        }
        throw new EPException("Unrecognized return type " + eval.getType() + " for use with Avro");
    }
}