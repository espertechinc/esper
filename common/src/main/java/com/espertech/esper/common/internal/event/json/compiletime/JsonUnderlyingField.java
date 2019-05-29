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
package com.espertech.esper.common.internal.event.json.compiletime;

import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class JsonUnderlyingField {
    private final String fieldName;
    private final int propertyNumber;
    private final Class propertyType;

    public JsonUnderlyingField(String fieldName, int propertyNumber, Class propertyType) {
        this.fieldName = fieldName;
        this.propertyNumber = propertyNumber;
        this.propertyType = propertyType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public int getPropertyNumber() {
        return propertyNumber;
    }

    public Class getPropertyType() {
        return propertyType;
    }

    public CodegenExpression toExpression() {
        return newInstance(JsonUnderlyingField.class, constant(fieldName), constant(propertyNumber), constant(propertyType));
    }
}
