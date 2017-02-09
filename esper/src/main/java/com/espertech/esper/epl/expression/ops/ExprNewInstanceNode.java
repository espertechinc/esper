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
import com.espertech.esper.epl.core.EngineImportException;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.event.bean.InstanceManufacturer;
import com.espertech.esper.event.bean.InstanceManufacturerFactory;

import java.io.StringWriter;

/**
 * Represents the "new Class(...)" operator in an expression tree.
 */
public class ExprNewInstanceNode extends ExprNodeBase implements ExprEvaluator {

    private static final long serialVersionUID = 1354077020397807076L;
    private final String classIdent;
    private transient Class targetClass;
    private transient InstanceManufacturer manufacturer;

    public ExprNewInstanceNode(String classIdent) {
        this.classIdent = classIdent;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        try {
            targetClass = validationContext.getEngineImportService().resolveClass(classIdent, false);
        } catch (EngineImportException e) {
            throw new ExprValidationException("Failed to resolve new-operator class name '" + classIdent + "'");
        }
        manufacturer = InstanceManufacturerFactory.getManufacturer(targetClass, validationContext.getEngineImportService(), this.getChildNodes());
        return null;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return manufacturer.make(eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public Class getType() {
        return targetClass;
    }

    public boolean isConstantResult() {
        return false;
    }

    public String getClassIdent() {
        return classIdent;
    }

    public boolean equalsNode(ExprNode node) {
        if (!(node instanceof ExprNewInstanceNode)) {
            return false;
        }

        ExprNewInstanceNode other = (ExprNewInstanceNode) node;
        return other.classIdent.equals(this.classIdent);
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.write("new ");
        writer.write(classIdent);
        ExprNodeUtility.toExpressionStringParams(writer, this.getChildNodes());
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }
}
