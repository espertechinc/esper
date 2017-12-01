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
package com.espertech.esper.epl.expression.codegen;

import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.util.JavaClassHelper;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class CodegenLegoCompareEquals {
    public static CodegenExpression codegenEqualsNonNullNoCoerce(CodegenExpression lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
        if (lhsType.isPrimitive() && rhsType.isPrimitive() && !JavaClassHelper.isFloatingPointClass(lhsType) && !JavaClassHelper.isFloatingPointClass(rhsType)) {
            return equalsIdentity(lhs, rhs);
        }
        if (lhsType.isPrimitive() && rhsType.isPrimitive()) {
            return staticMethod(JavaClassHelper.getBoxedType(lhsType), "compare", lhs, rhs);
        }
        if (lhsType.isPrimitive()) {
            return exprDotMethod(rhs, "equals", lhs);
        }
        return exprDotMethod(lhs, "equals", rhs);
    }
}
