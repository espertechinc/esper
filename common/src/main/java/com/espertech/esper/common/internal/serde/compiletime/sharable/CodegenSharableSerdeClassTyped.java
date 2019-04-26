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
import com.espertech.esper.common.internal.serde.serdeset.additional.DIORefCountedSet;
import com.espertech.esper.common.internal.serde.serdeset.additional.DIOSortedRefCountedSet;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class CodegenSharableSerdeClassTyped implements CodegenFieldSharable {
    private final CodegenSharableSerdeName name;
    private final Class valueType;
    private final DataInputOutputSerdeForge forge;
    private final CodegenClassScope classScope;

    public CodegenSharableSerdeClassTyped(CodegenSharableSerdeName name, Class valueType, DataInputOutputSerdeForge forge, CodegenClassScope classScope) {
        this.name = name;
        this.valueType = valueType;
        this.forge = forge;
        this.classScope = classScope;
    }

    public Class type() {
        return DataInputOutputSerde.class;
    }

    public CodegenExpression initCtorScoped() {
        CodegenExpression serde = forge.codegen(classScope.getPackageScope().getInitMethod(), classScope, null);
        if (name == CodegenSharableSerdeName.VALUE_NULLABLE) {
            return serde;
        } else if (name == CodegenSharableSerdeName.REFCOUNTEDSET) {
            return newInstance(DIORefCountedSet.class, serde);
        } else if (name == CodegenSharableSerdeName.SORTEDREFCOUNTEDSET) {
            return newInstance(DIOSortedRefCountedSet.class, serde);
        } else {
            throw new IllegalArgumentException("Unrecognized name " + name);
        }
    }

    public enum CodegenSharableSerdeName {
        VALUE_NULLABLE("valueNullable"),
        REFCOUNTEDSET("refCountedSet"),
        SORTEDREFCOUNTEDSET("sortedRefCountedSet");

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

        CodegenSharableSerdeClassTyped that = (CodegenSharableSerdeClassTyped) o;

        if (name != that.name) return false;
        return valueType.equals(that.valueType);
    }

    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + valueType.hashCode();
        return result;
    }
}
