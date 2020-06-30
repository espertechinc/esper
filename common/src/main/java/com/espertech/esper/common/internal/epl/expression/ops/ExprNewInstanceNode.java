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

import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.event.bean.manufacturer.InstanceManufacturerFactory;
import com.espertech.esper.common.internal.event.bean.manufacturer.InstanceManufacturerFactoryFactory;
import com.espertech.esper.common.internal.settings.ClasspathImportEPTypeUtil;
import com.espertech.esper.common.internal.type.ClassDescriptor;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.io.StringWriter;

/**
 * Represents the "new Class(...)" operator in an expression tree.
 */
public class ExprNewInstanceNode extends ExprNodeBase {

    private final ClassDescriptor classIdentNoDimensions;
    private final int numArrayDimensions;
    private boolean arrayInitializedByExpr;

    private transient ExprForge forge;

    public ExprNewInstanceNode(ClassDescriptor classIdentNoDimensions, int numArrayDimensions) {
        this.classIdentNoDimensions = classIdentNoDimensions;
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
        EPTypeClass targetClass = null;
        if (numArrayDimensions > 0 && classIdentNoDimensions.getTypeParameters().isEmpty()) {
            // the "double[]" does become "double[]" and not "Double[]"
            Class primitive = JavaClassHelper.getPrimitiveClassForName(classIdentNoDimensions.getClassIdentifier());
            if (primitive != null) {
                targetClass = EPTypePremade.getOrCreate(primitive);
            }
        }
        if (targetClass == null) {
            targetClass = ClasspathImportEPTypeUtil.resolveClassIdentifierToEPType(classIdentNoDimensions, false, validationContext.getClasspathImportService(), validationContext.getClassProvidedClasspathExtension());
        }
        if (targetClass == null) {
            throw new ExprValidationException("Failed to resolve type parameter '" + classIdentNoDimensions.toEPL() + "'");
        }

        // handle non-array
        if (numArrayDimensions == 0) {
            InstanceManufacturerFactory manufacturerFactory = InstanceManufacturerFactoryFactory.getManufacturer(targetClass, validationContext.getClasspathImportService(), this.getChildNodes());
            forge = new ExprNewInstanceNodeNonArrayForge(this, targetClass, manufacturerFactory);
            return null;
        }

        // determine array initialized or not
        EPTypeClass targetClassArray = JavaClassHelper.getArrayType(targetClass, numArrayDimensions);
        if (getChildNodes().length == 1 && getChildNodes()[0] instanceof ExprArrayNode) {
            arrayInitializedByExpr = true;
        } else {
            for (ExprNode child : getChildNodes()) {
                EPType evalType = child.getForge().getEvaluationType();
                if (!JavaClassHelper.isTypeInteger(evalType)) {
                    String message = "New-keyword with an array-type result requires an Integer-typed dimension but received type '" + (evalType == null ? "null" : evalType.getTypeName()) + "'";
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
            EPTypeClass component = JavaClassHelper.getArrayComponentType(targetClassArray);
            arrayNode.setOptionalRequiredType(component);
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

    public ClassDescriptor getClassIdentNoDimensions() {
        return classIdentNoDimensions;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprNewInstanceNode)) {
            return false;
        }

        ExprNewInstanceNode other = (ExprNewInstanceNode) node;
        return other.classIdentNoDimensions.equals(this.classIdentNoDimensions) && other.numArrayDimensions == this.numArrayDimensions;
    }

    public void toPrecedenceFreeEPL(StringWriter writer, ExprNodeRenderableFlags flags) {
        writer.write("new ");
        writer.write(classIdentNoDimensions.toEPL());
        if (numArrayDimensions == 0) {
            ExprNodeUtilityPrint.toExpressionStringParams(writer, this.getChildNodes());
        } else {
            if (arrayInitializedByExpr) {
                writer.write("[] ");
                this.getChildNodes()[0].toEPL(writer, ExprPrecedenceEnum.UNARY, flags);
            } else {
                for (ExprNode child : this.getChildNodes()) {
                    writer.write("[");
                    child.toEPL(writer, ExprPrecedenceEnum.UNARY, flags);
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
