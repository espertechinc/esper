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
package com.espertech.esper.common.internal.metrics.instrumentation;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenFieldSharable;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import java.lang.instrument.Instrumentation;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;

public class InstrumentationField implements CodegenFieldSharable {
    public final static InstrumentationField INSTANCE = new InstrumentationField();

    private InstrumentationField() {
    }

    public Class type() {
        return Instrumentation.class;
    }

    public CodegenExpression initCtorScoped() {
        return staticMethod(InstrumentationCommon.RUNTIME_HELPER_CLASS, "get");
    }
}
