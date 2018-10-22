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
package com.espertech.esper.common.internal.rettype;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenFieldSharable;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;

public class EPTypeCodegenSharable implements CodegenFieldSharable {
    private final EPType epType;
    private final CodegenClassScope classScope;

    public EPTypeCodegenSharable(EPType epType, CodegenClassScope classScope) {
        this.epType = epType;
        this.classScope = classScope;
    }

    public Class type() {
        return EPType.class;
    }

    public CodegenExpression initCtorScoped() {
        return epType.codegen(classScope.getPackageScope().getInitMethod(), classScope, EPStatementInitServices.REF);
    }

    public enum CodegenSharableSerdeName {
        OBJECTARRAYMAYNULLNULL("objectArrayMayNullNull");

        private final String methodName;

        CodegenSharableSerdeName(String methodName) {
            this.methodName = methodName;
        }

        public String getMethodName() {
            return methodName;
        }
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EPTypeCodegenSharable that = (EPTypeCodegenSharable) o;

        return epType != null ? epType.equals(that.epType) : that.epType == null;
    }

    public int hashCode() {
        return epType != null ? epType.hashCode() : 0;
    }
}
