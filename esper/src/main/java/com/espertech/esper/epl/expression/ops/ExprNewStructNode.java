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
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents the "new {...}" operator in an expression tree.
 */
public class ExprNewStructNode extends ExprNodeBase {

    private static final long serialVersionUID = -210293632565665600L;

    private final String[] columnNames;

    private transient ExprNewStructNodeForge forge;

    public ExprNewStructNode(String[] columnNames) {
        this.columnNames = columnNames;
    }

    public ExprEvaluator getExprEvaluator() {
        checkValidated(forge);
        return forge.getExprEvaluator();
    }

    public ExprForge getForge() {
        checkValidated(forge);
        return forge;
    }

    public Class getEvaluationType() {
        return Map.class;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        LinkedHashMap eventType = new LinkedHashMap<String, Object>();

        boolean isAllConstants = false;
        for (int i = 0; i < columnNames.length; i++) {
            isAllConstants = isAllConstants && this.getChildNodes()[i].isConstantResult();
            if (eventType.containsKey(columnNames[i])) {
                throw new ExprValidationException("Failed to validate new-keyword property names, property '" + columnNames[i] + "' has already been declared");
            }

            Map<String, Object> eventTypeResult = null;
            if (getChildNodes()[i].getForge() instanceof ExprTypableReturnForge) {
                eventTypeResult = ((ExprTypableReturnForge) getChildNodes()[i].getForge()).getRowProperties();
            }
            if (eventTypeResult != null) {
                eventType.put(columnNames[i], eventTypeResult);
            } else {
                Class classResult = JavaClassHelper.getBoxedType(getChildNodes()[i].getForge().getEvaluationType());
                eventType.put(columnNames[i], classResult);
            }
        }
        forge = new ExprNewStructNodeForge(this, isAllConstants, eventType);
        return null;
    }

    public String[] getColumnNames() {
        return columnNames;
    }

    public boolean isConstantResult() {
        checkValidated(forge);
        return forge.isAllConstants();
    }

    public Object[][] evaluateTypableMulti(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
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
