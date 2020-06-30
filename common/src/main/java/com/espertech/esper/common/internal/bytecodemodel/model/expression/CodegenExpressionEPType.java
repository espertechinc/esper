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
package com.espertech.esper.common.internal.bytecodemodel.model.expression;

import com.espertech.esper.common.client.type.*;

import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class CodegenExpressionEPType {
    public static CodegenExpression toExpression(EPType epType) {
        if (epType instanceof EPTypeNull) {
            return enumValue(EPTypeNull.class, "INSTANCE");
        }

        EPTypeClass epTypeClass = (EPTypeClass) epType;
        if (!(epType instanceof EPTypeClassParameterized)) {
            EPTypePremade existing = EPTypePremade.getExisting(epTypeClass.getType());
            if (existing != null) {
                return enumValue(EPTypePremade.class, existing.name() + ".getEPType()");
            }
            return newInstance(EPTypeClass.EPTYPE, constant(epTypeClass.getType()));
        }

        EPTypeClassParameterized parameterized = (EPTypeClassParameterized) epType;
        EPTypeClass[] parameters = parameterized.getParameters();
        CodegenExpression[] params = new CodegenExpression[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            params[i] = toExpression(parameters[i]);
        }
        return newInstance(EPTypeClassParameterized.EPTYPE, constant(epTypeClass.getType()), newArrayWithInit(EPTypeClass.EPTYPE, params));
    }

    public static void render(EPType constant, StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        CodegenExpression expression = toExpression(constant);
        expression.render(builder, imports, isInnerClass);
    }
}
