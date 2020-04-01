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
package com.espertech.esper.common.internal.epl.enummethod.eval;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenParams;

public class EnumToArrayScalarForge implements EnumForge {

    private final Class arrayComponentType;

    public EnumToArrayScalarForge(Class arrayComponentType) {
        this.arrayComponentType = arrayComponentType;
    }

    public Class getArrayComponentType() {
        return arrayComponentType;
    }

    public EnumEval getEnumEvaluator() {
        return new EnumToArrayScalarForgeEval(this);
    }

    public CodegenExpression codegen(EnumForgeCodegenParams premade, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return EnumToArrayScalarForgeEval.codegen(this, premade, codegenMethodScope, codegenClassScope);
    }

    public int getStreamNumSize() {
        return 0;
    }
}
