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
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.io.StringWriter;

/**
 * Represents the "new Class(...)" operator in an expression tree.
 */
public class ExprNewInstanceNode extends ExprNodeBase {

    private final String classIdent;
    private final boolean array;

    private transient ExprForge forge;

    public ExprNewInstanceNode(String classIdent, boolean array) {
        this.classIdent = classIdent;
        this.array = array;
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
        Class targetClass = null;
        InstanceManufacturerFactory manufacturerFactory = null;
        if (array) {
            targetClass = JavaClassHelper.getPrimitiveClassForName(classIdent);
            for (ExprNode child : getChildNodes()) {
                Class evalType = child.getForge().getEvaluationType();
                if (JavaClassHelper.getBoxedType(evalType) != Integer.class) {
                    String message = "New-keyword with an array-type result requires an Integer-typed dimension but received type '" + JavaClassHelper.getClassNameFullyQualPretty(evalType) + "'";
                    throw new ExprValidationException(message);
                }
            }
        }
        if (targetClass == null) {
            try {
                targetClass = validationContext.getClasspathImportService().resolveClass(classIdent, false, validationContext.getClassProvidedClasspathExtension());
            } catch (ClasspathImportException e) {
                throw new ExprValidationException("Failed to resolve new-operator class name '" + classIdent + "'");
            }
        }

        if (!array) {
            manufacturerFactory = InstanceManufacturerFactoryFactory.getManufacturer(targetClass, validationContext.getClasspathImportService(), this.getChildNodes());
            forge = new ExprNewInstanceNodeNonArrayForge(this, targetClass, manufacturerFactory);
        } else {
            forge = new ExprNewInstanceNodeArrayForge(this, targetClass, JavaClassHelper.getArrayType(targetClass, getChildNodes().length));
        }
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
        return other.classIdent.equals(this.classIdent) && other.array == this.array;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.write("new ");
        writer.write(classIdent);
        if (!array) {
            ExprNodeUtilityPrint.toExpressionStringParams(writer, this.getChildNodes());
        } else {
            for (ExprNode child : this.getChildNodes()) {
                writer.write("[");
                child.toEPL(writer, ExprPrecedenceEnum.UNARY);
                writer.write("]");
            }
        }
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean isArray() {
        return array;
    }
}
