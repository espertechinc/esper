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
package com.espertech.esper.common.internal.epl.expression.core;

import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.core.EventTypeSPI;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.io.StringWriter;

public class ExprArrayElement extends ExprNodeBase {
    private final String arrayPropName;
    private ExprArrayElementForge forge;

    public ExprArrayElement(String arrayPropName) {
        this.arrayPropName = arrayPropName;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        // verify array index child expression
        if (getChildNodes().length != 1) {
            throw new ExprValidationException("Array-element node expected a single index expression");
        }
        ExprNode index = getChildNodes()[0];
        Class indexExprType = index.getForge().getEvaluationType();
        if (JavaClassHelper.getBoxedType(indexExprType) != Integer.class) {
            String message = "Array expression requires an Integer-typed dimension but received type '" + JavaClassHelper.getClassNameFullyQualPretty(indexExprType) + "'";
            throw new ExprValidationException(message);
        }

        // may be a variable
        VariableMetaData meta = validationContext.getVariableCompileTimeResolver().resolve(arrayPropName);
        if (meta != null) {
            if (!meta.getType().isArray()) {
                throw new ExprValidationException("Variable '" + arrayPropName + "' is not an array");
            }
            Class type = meta.getType();
            forge = new ExprArrayElementForgeVariable(this, type.getComponentType(), type, meta);
            return null;
        }

        // resolve property from stream
        int streamNum = -1;
        Class componentType = null;
        EventPropertyGetterSPI getter = null;
        for (int i = 0; i < validationContext.getStreamTypeService().getEventTypes().length; i++) {
            EventTypeSPI type = (EventTypeSPI) validationContext.getStreamTypeService().getEventTypes()[i];
            EventPropertyDescriptor desc = type.getPropertyDescriptor(arrayPropName);
            if (desc == null) {
                continue;
            }
            if (!desc.isIndexed() && !desc.getPropertyType().isArray()) {
                throw new ExprValidationException("Property '" + arrayPropName + "' is not an array since its type is '" + JavaClassHelper.getClassNameFullyQualPretty(desc.getPropertyType()) + "'");
            }
            componentType = desc.getPropertyType().getComponentType();
            getter = type.getGetterSPI(desc.getPropertyName());
            if (getter == null) {
                throw new ExprValidationException("Property '" + arrayPropName + "' does not have a getter");
            }
            if (i == 0 && validationContext.getStreamTypeService().isStreamZeroUnambigous()) {
                streamNum = 0;
                break;
            }
            if (streamNum != -1) {
                throw new ExprValidationException("Property '" + arrayPropName + "' occurs in multiple streams");
            }
            streamNum = i;
        }
        if (streamNum == -1) {
            throw new ExprValidationException("Failed to resolve property '" + arrayPropName + "' to any stream");
        }
        forge = new ExprArrayElementForgeProperty(this, componentType, JavaClassHelper.getArrayType(componentType), streamNum, getter);
        return null;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append(arrayPropName).append("[");
        getChildNodes()[0].toEPL(writer, ExprPrecedenceEnum.UNARY);
        writer.append("]");
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprArrayElement)) {
            return false;
        }
        ExprArrayElement other = (ExprArrayElement) node;
        return other.arrayPropName.equals(this.arrayPropName);
    }

    public ExprArrayElementForge getForge() {
        return forge;
    }

    public String getArrayPropName() {
        return arrayPropName;
    }
}
