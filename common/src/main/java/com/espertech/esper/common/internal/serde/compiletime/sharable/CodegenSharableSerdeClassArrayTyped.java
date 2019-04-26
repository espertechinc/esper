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
package com.espertech.esper.common.internal.serde.compiletime.sharable;

import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenFieldSharable;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

import java.util.Arrays;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.exprDotMethodChain;
import static com.espertech.esper.common.internal.event.path.EventTypeResolver.GETEVENTSERDEFACTORY;

public class CodegenSharableSerdeClassArrayTyped implements CodegenFieldSharable {
    private final CodegenSharableSerdeName name;
    private final Class[] valueTypes;
    private final DataInputOutputSerdeForge[] serdes;
    private final CodegenClassScope classScope;

    public CodegenSharableSerdeClassArrayTyped(CodegenSharableSerdeName name, Class[] valueTypes, DataInputOutputSerdeForge[] serdes, CodegenClassScope classScope) {
        this.name = name;
        this.valueTypes = valueTypes;
        this.serdes = serdes;
        this.classScope = classScope;
    }

    public Class type() {
        return DataInputOutputSerde.class;
    }

    public CodegenExpression initCtorScoped() {
        return exprDotMethodChain(EPStatementInitServices.REF).add(EPStatementInitServices.GETEVENTTYPERESOLVER).add(GETEVENTSERDEFACTORY).add(name.methodName, DataInputOutputSerdeForge.codegenArray(serdes, classScope.getPackageScope().getInitMethod(), classScope, null));
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

        CodegenSharableSerdeClassArrayTyped that = (CodegenSharableSerdeClassArrayTyped) o;

        if (name != that.name) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(valueTypes, that.valueTypes);
    }

    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + Arrays.hashCode(valueTypes);
        return result;
    }
}
