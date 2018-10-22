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
package com.espertech.esper.common.internal.epl.variable.compiletime;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenFieldSharable;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.variable.core.VariableDeployTimeResolver;
import com.espertech.esper.common.internal.epl.variable.core.VariableReader;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;

public class VariableReaderCodegenFieldSharable implements CodegenFieldSharable {
    private final VariableMetaData metaWVisibility;

    public VariableReaderCodegenFieldSharable(VariableMetaData metaWVisibility) {
        this.metaWVisibility = metaWVisibility;
    }

    public Class type() {
        return VariableReader.class;
    }

    public CodegenExpression initCtorScoped() {
        return staticMethod(VariableDeployTimeResolver.class, "resolveVariableReader",
                constant(metaWVisibility.getVariableName()),
                constant(metaWVisibility.getVariableVisibility()),
                constant(metaWVisibility.getVariableModuleName()),
                constant(metaWVisibility.getOptionalContextName()),
                EPStatementInitServices.REF);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VariableReaderCodegenFieldSharable that = (VariableReaderCodegenFieldSharable) o;

        return metaWVisibility.getVariableName().equals(that.metaWVisibility.getVariableName());
    }

    public int hashCode() {
        return metaWVisibility.getVariableName().hashCode();
    }
}
