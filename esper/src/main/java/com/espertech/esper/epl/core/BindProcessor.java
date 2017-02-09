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
package com.espertech.esper.epl.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.SelectClauseElementCompiled;
import com.espertech.esper.epl.spec.SelectClauseElementWildcard;
import com.espertech.esper.epl.spec.SelectClauseExprCompiledSpec;
import com.espertech.esper.epl.spec.SelectClauseStreamCompiledSpec;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableService;

import java.util.ArrayList;

/**
 * Works in conjunction with {@link SelectExprResultProcessor} to present
 * a result as an object array for 'natural' delivery.
 */
public class BindProcessor {
    private ExprEvaluator[] expressionNodes;
    private Class[] expressionTypes;
    private String[] columnNamesAssigned;

    /**
     * Ctor.
     *
     * @param selectionList  the select clause
     * @param typesPerStream the event types per stream
     * @param streamNames    the stream names
     * @param tableService   table service
     * @throws ExprValidationException when the validation of the select clause failed
     */
    public BindProcessor(SelectClauseElementCompiled[] selectionList,
                         EventType[] typesPerStream,
                         String[] streamNames,
                         TableService tableService)
            throws ExprValidationException {
        ArrayList<ExprEvaluator> expressions = new ArrayList<ExprEvaluator>();
        ArrayList<Class> types = new ArrayList<Class>();
        ArrayList<String> columnNames = new ArrayList<String>();

        for (SelectClauseElementCompiled element : selectionList) {
            // handle wildcards by outputting each stream's underlying event
            if (element instanceof SelectClauseElementWildcard) {
                for (int i = 0; i < typesPerStream.length; i++) {
                    Class returnType = typesPerStream[i].getUnderlyingType();
                    TableMetadata tableMetadata = tableService.getTableMetadataFromEventType(typesPerStream[i]);
                    ExprEvaluator evaluator;
                    if (tableMetadata != null) {
                        evaluator = new BindProcessorEvaluatorStreamTable(i, returnType, tableMetadata);
                    } else {
                        evaluator = new BindProcessorEvaluatorStream(i, returnType);
                    }
                    expressions.add(evaluator);
                    types.add(returnType);
                    columnNames.add(streamNames[i]);
                }
            } else if (element instanceof SelectClauseStreamCompiledSpec) {
                // handle stream wildcards by outputting the stream underlying event
                final SelectClauseStreamCompiledSpec streamSpec = (SelectClauseStreamCompiledSpec) element;
                EventType type = typesPerStream[streamSpec.getStreamNumber()];
                final Class returnType = type.getUnderlyingType();

                final TableMetadata tableMetadata = tableService.getTableMetadataFromEventType(type);
                ExprEvaluator evaluator;
                if (tableMetadata != null) {
                    evaluator = new BindProcessorEvaluatorStreamTable(streamSpec.getStreamNumber(), returnType, tableMetadata);
                } else {
                    evaluator = new BindProcessorEvaluatorStream(streamSpec.getStreamNumber(), returnType);
                }
                expressions.add(evaluator);
                types.add(returnType);
                columnNames.add(streamNames[streamSpec.getStreamNumber()]);
            } else if (element instanceof SelectClauseExprCompiledSpec) {
                // handle expressions
                SelectClauseExprCompiledSpec expr = (SelectClauseExprCompiledSpec) element;
                ExprEvaluator evaluator = expr.getSelectExpression().getExprEvaluator();
                expressions.add(evaluator);
                types.add(evaluator.getType());
                if (expr.getAssignedName() != null) {
                    columnNames.add(expr.getAssignedName());
                } else {
                    columnNames.add(ExprNodeUtility.toExpressionStringMinPrecedenceSafe(expr.getSelectExpression()));
                }
            } else {
                throw new IllegalStateException("Unrecognized select expression element of type " + element.getClass());
            }
        }

        expressionNodes = expressions.toArray(new ExprEvaluator[expressions.size()]);
        expressionTypes = types.toArray(new Class[types.size()]);
        columnNamesAssigned = columnNames.toArray(new String[columnNames.size()]);
    }

    /**
     * Process select expressions into columns for native dispatch.
     *
     * @param eventsPerStream      each stream's events
     * @param isNewData            true for new events
     * @param exprEvaluatorContext context for expression evaluatiom
     * @return object array with select-clause results
     */
    public Object[] process(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object[] parameters = new Object[expressionNodes.length];

        for (int i = 0; i < parameters.length; i++) {
            Object result = expressionNodes[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            parameters[i] = result;
        }

        return parameters;
    }

    /**
     * Returns the expression types generated by the select-clause expressions.
     *
     * @return types
     */
    public Class[] getExpressionTypes() {
        return expressionTypes;
    }

    /**
     * Returns the column names of select-clause expressions.
     *
     * @return column names
     */
    public String[] getColumnNamesAssigned() {
        return columnNamesAssigned;
    }
}
