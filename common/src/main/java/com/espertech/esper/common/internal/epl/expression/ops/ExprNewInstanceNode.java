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
package com.espertech.esper.common.internal.epl.expression.ops;

import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.event.bean.manufacturer.InstanceManufacturerFactory;
import com.espertech.esper.common.internal.event.bean.manufacturer.InstanceManufacturerFactoryFactory;
import com.espertech.esper.common.internal.settings.ClasspathImportException;

import java.io.StringWriter;

/**
 * Represents the "new Class(...)" operator in an expression tree.
 */
public class ExprNewInstanceNode extends ExprNodeBase {

    private final String classIdent;

    private transient ExprNewInstanceNodeForge forge;

    public ExprNewInstanceNode(String classIdent) {
        this.classIdent = classIdent;
    }

    public ExprEvaluator getExprEvaluator() {
        checkValidated(forge);
        return forge.getExprEvaluator();
    }

    public ExprForge getForge() {
        checkValidated(forge);
        return forge;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        Class targetClass;
        try {
            targetClass = validationContext.getClasspathImportService().resolveClass(classIdent, false);
        } catch (ClasspathImportException e) {
            throw new ExprValidationException("Failed to resolve new-operator class name '" + classIdent + "'");
        }
        InstanceManufacturerFactory manufacturerFactory = InstanceManufacturerFactoryFactory.getManufacturer(targetClass, validationContext.getClasspathImportService(), this.getChildNodes());
        forge = new ExprNewInstanceNodeForge(this, targetClass, manufacturerFactory);
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public String getClassIdent() {
        return classIdent;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprNewInstanceNode)) {
            return false;
        }

        ExprNewInstanceNode other = (ExprNewInstanceNode) node;
        return other.classIdent.equals(this.classIdent);
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.write("new ");
        writer.write(classIdent);
        ExprNodeUtilityPrint.toExpressionStringParams(writer, this.getChildNodes());
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }
}
