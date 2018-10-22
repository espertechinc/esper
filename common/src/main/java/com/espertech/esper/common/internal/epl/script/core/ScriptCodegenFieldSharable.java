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
package com.espertech.esper.common.internal.epl.script.core;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenFieldSharable;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;

public class ScriptCodegenFieldSharable implements CodegenFieldSharable {
    private final String scriptName;
    private final int parameterNumber;
    private final ScriptDescriptorCompileTime scriptDescriptor;
    private final CodegenClassScope classScope;

    public ScriptCodegenFieldSharable(ScriptDescriptorCompileTime scriptDescriptor, CodegenClassScope classScope) {
        this.scriptName = scriptDescriptor.getScriptName();
        this.parameterNumber = scriptDescriptor.getParameterNames().length;
        this.scriptDescriptor = scriptDescriptor;
        this.classScope = classScope;
    }

    public Class type() {
        return ScriptEvaluator.class;
    }

    public CodegenExpression initCtorScoped() {
        return staticMethod(ScriptEvaluatorCompilerRuntime.class, "compileScriptEval", scriptDescriptor.make(classScope.getPackageScope().getInitMethod(), classScope));
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScriptCodegenFieldSharable that = (ScriptCodegenFieldSharable) o;

        if (parameterNumber != that.parameterNumber) return false;
        return scriptName.equals(that.scriptName);
    }

    public int hashCode() {
        int result = scriptName.hashCode();
        result = 31 * result + parameterNumber;
        return result;
    }
}
