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
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents the "new {...}" operator in an expression tree.
 */
public class ExprNewStructNode extends ExprNodeBase implements ExprEvaluatorTypableReturn {

    private static final long serialVersionUID = -210293632565665600L;

    private final String[] columnNames;
    private transient LinkedHashMap<String, Object> eventType;
    private transient ExprEvaluator[] evaluators;
    private boolean isAllConstants;

    public ExprNewStructNode(String[] columnNames) {
        this.columnNames = columnNames;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        eventType = new LinkedHashMap<String, Object>();
        evaluators = ExprNodeUtility.getEvaluators(this.getChildNodes());

        for (int i = 0; i < columnNames.length; i++) {
            isAllConstants = isAllConstants && this.getChildNodes()[i].isConstantResult();
            if (eventType.containsKey(columnNames[i])) {
                throw new ExprValidationException("Failed to validate new-keyword property names, property '" + columnNames[i] + "' has already been declared");
            }

            Map<String, Object> eventTypeResult = null;
            if (evaluators[i] instanceof ExprEvaluatorTypableReturn) {
                eventTypeResult = ((ExprEvaluatorTypableReturn) evaluators[i]).getRowProperties();
            }
            if (eventTypeResult != null) {
                eventType.put(columnNames[i], eventTypeResult);
            } else {
                Class classResult = JavaClassHelper.getBoxedType(evaluators[i].getType());
                eventType.put(columnNames[i], classResult);
            }
        }
        return null;
    }

    public String[] getColumnNames() {
        return columnNames;
    }

    public boolean isConstantResult() {
        return isAllConstants;
    }

    public Class getType() {
        return Map.class;
    }

    public LinkedHashMap<String, Object> getRowProperties() throws ExprValidationException {
        return eventType;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprNew(this);
        }
        Map<String, Object> props = new HashMap<String, Object>();
        for (int i = 0; i < evaluators.length; i++) {
            props.put(columnNames[i], evaluators[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext));
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprNew(props);
        }
        return props;
    }

    public Boolean isMultirow() {
        return false;   // New itself can only return a single row
    }

    public Object[] evaluateTypableSingle(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object[] rows = new Object[columnNames.length];
        for (int i = 0; i < columnNames.length; i++) {
            rows[i] = evaluators[i].evaluate(eventsPerStream, isNewData, context);
        }
        return rows;
    }

    public Object[][] evaluateTypableMulti(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public boolean equalsNode(ExprNode node) {
        if (!(node instanceof ExprNewStructNode)) {
            return false;
        }

        ExprNewStructNode other = (ExprNewStructNode) node;
        return Arrays.deepEquals(other.columnNames, columnNames);
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.write("new{");
        String delimiter = "";
        for (int i = 0; i < this.getChildNodes().length; i++) {
            writer.append(delimiter);
            writer.append(columnNames[i]);
            ExprNode expr = this.getChildNodes()[i];

            boolean outputexpr = true;
            if (expr instanceof ExprIdentNode) {
                ExprIdentNode prop = (ExprIdentNode) expr;
                if (prop.getResolvedPropertyName().equals(columnNames[i])) {
                    outputexpr = false;
                }
            }

            if (outputexpr) {
                writer.append("=");
                expr.toEPL(writer, ExprPrecedenceEnum.MINIMUM);
            }
            delimiter = ",";
        }
        writer.write("}");
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }
}
