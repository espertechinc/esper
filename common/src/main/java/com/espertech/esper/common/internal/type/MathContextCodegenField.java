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
package com.espertech.esper.common.internal.type;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenFieldSharable;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import java.math.MathContext;
import java.math.RoundingMode;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Math context member
 */
public class MathContextCodegenField implements CodegenFieldSharable {

    private final MathContext mathContext;

    public MathContextCodegenField(MathContext mathContext) {
        this.mathContext = mathContext;
    }

    public Class type() {
        return MathContext.class;
    }

    public CodegenExpression initCtorScoped() {
        if (mathContext == null) {
            return constantNull();
        }
        return newInstance(MathContext.class, constant(mathContext.getPrecision()),
                enumValue(RoundingMode.class, mathContext.getRoundingMode().name()));
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MathContextCodegenField that = (MathContextCodegenField) o;

        return mathContext != null ? mathContext.equals(that.mathContext) : that.mathContext == null;
    }

    public int hashCode() {
        return mathContext != null ? mathContext.hashCode() : 0;
    }
}
