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
package com.espertech.esper.common.internal.serde;

import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenFieldSharable;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;

import java.util.Arrays;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.exprDotMethodChain;

public class CodegenSharableSerdeClassArrayTyped implements CodegenFieldSharable {
    private final CodegenSharableSerdeName name;
    private final Class[] valueTypes;

    public CodegenSharableSerdeClassArrayTyped(CodegenSharableSerdeName name, Class[] valueTypes) {
        this.name = name;
        this.valueTypes = valueTypes;
    }

    public Class type() {
        return DataInputOutputSerde.class;
    }

    public CodegenExpression initCtorScoped() {
        return exprDotMethodChain(EPStatementInitServices.REF).add(EPStatementInitServices.GETDATAINPUTOUTPUTSERDEPROVIDER).add(name.methodName, constant(valueTypes));
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
