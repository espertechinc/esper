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
    private final int numArrayDimensions;
    private boolean arrayInitializedByExpr;

    private transient ExprForge forge;

    public ExprNewInstanceNode(String classIdent, int numArrayDimensions) {
        this.classIdent = classIdent;
        this.numArrayDimensions = numArrayDimensions;
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
        // Resolve target class
        Class targetClass = null;
        if (numArrayDimensions != 0) {
            targetClass = JavaClassHelper.getPrimitiveClassForName(classIdent);
        }
        if (targetClass == null) {
            try {
                targetClass = validationContext.getClasspathImportService().resolveClass(classIdent, false, validationContext.getClassProvidedClasspathExtension());
            } catch (ClasspathImportException e) {
                throw new ExprValidationException("Failed to resolve new-operator class name '" + classIdent + "'");
            }
        }

        // handle non-array
        if (numArrayDimensions == 0) {
            InstanceManufacturerFactory manufacturerFactory = InstanceManufacturerFactoryFactory.getManufacturer(targetClass, validationContext.getClasspathImportService(), this.getChildNodes());
            forge = new ExprNewInstanceNodeNonArrayForge(this, targetClass, manufacturerFactory);
            return null;
        }

        // determine array initialized or not
        Class targetClassArray = JavaClassHelper.getArrayType(targetClass, numArrayDimensions);
        if (getChildNodes().length == 1 && getChildNodes()[0] instanceof ExprArrayNode) {
            arrayInitializedByExpr = true;
        } else {
            for (ExprNode child : getChildNodes()) {
                Class evalType = child.getForge().getEvaluationType();
                if (JavaClassHelper.getBoxedType(evalType) != Integer.class) {
                    String message = "New-keyword with an array-type result requires an Integer-typed dimension but received type '" + JavaClassHelper.getClassNameFullyQualPretty(evalType) + "'";
                    throw new ExprValidationException(message);
                }
            }
        }

        // handle array initialized by dimension only
        if (!arrayInitializedByExpr) {
            forge = new ExprNewInstanceNodeArrayForge(this, targetClass, targetClassArray);
            return null;
        }

        // handle array initialized by array expression
        if (numArrayDimensions < 1 || numArrayDimensions > 2) {
            throw new IllegalStateException("Num-array-dimensions unexpected at " + numArrayDimensions);
        }
        ExprArrayNode arrayNode = (ExprArrayNode) getChildNodes()[0];

        // handle 2-dimensional array validation
        if (numArrayDimensions == 2) {
            for (ExprNode inner : arrayNode.getChildNodes()) {
                if (!(inner instanceof ExprArrayNode)) {
                    throw new ExprValidationException("Two-dimensional array element does not allow element expression '" + ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(inner) + "'");
                }
                ExprArrayNode innerArray = (ExprArrayNode) inner;
                innerArray.setOptionalRequiredType(targetClass);
                innerArray.validate(validationContext);
            }
            arrayNode.setOptionalRequiredType(targetClassArray.getComponentType());
        } else {
            arrayNode.setOptionalRequiredType(targetClass);
        }
        arrayNode.validate(validationContext);

        forge = new ExprNewInstanceNodeArrayForge(this, targetClass, targetClassArray);
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
        return other.classIdent.equals(this.classIdent) && other.numArrayDimensions == this.numArrayDimensions;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.write("new ");
        writer.write(classIdent);
        if (numArrayDimensions == 0) {
            ExprNodeUtilityPrint.toExpressionStringParams(writer, this.getChildNodes());
        } else {
            if (arrayInitializedByExpr) {
                writer.write("[] ");
                this.getChildNodes()[0].toEPL(writer, ExprPrecedenceEnum.UNARY);
            } else {
                for (ExprNode child : this.getChildNodes()) {
                    writer.write("[");
                    child.toEPL(writer, ExprPrecedenceEnum.UNARY);
                    writer.write("]");
                }
            }
        }
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public int getNumArrayDimensions() {
        return numArrayDimensions;
    }

    public boolean isArrayInitializedByExpr() {
        return arrayInitializedByExpr;
    }
}
