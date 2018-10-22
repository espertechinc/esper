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

import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class NameParamNumAndModule {
    public final static NameParamNumAndModule[] EMPTY_ARRAY = new NameParamNumAndModule[0];

    private final String name;
    private final int paramNum;
    private final String moduleName;

    public NameParamNumAndModule(String name, int paramNum, String moduleName) {
        this.name = name;
        this.paramNum = paramNum;
        this.moduleName = moduleName;
    }

    public String getName() {
        return name;
    }

    public int getParamNum() {
        return paramNum;
    }

    public String getModuleName() {
        return moduleName;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NameParamNumAndModule that = (NameParamNumAndModule) o;

        if (paramNum != that.paramNum) return false;
        if (!name.equals(that.name)) return false;
        return moduleName != null ? moduleName.equals(that.moduleName) : that.moduleName == null;
    }

    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + paramNum;
        result = 31 * result + (moduleName != null ? moduleName.hashCode() : 0);
        return result;
    }

    public static CodegenExpression makeArray(Collection<NameParamNumAndModule> names) {
        if (names.isEmpty()) {
            return enumValue(NameParamNumAndModule.class, "EMPTY_ARRAY");
        }
        CodegenExpression[] expressions = new CodegenExpression[names.size()];
        int count = 0;
        for (NameParamNumAndModule entry : names) {
            expressions[count++] = entry.make();
        }
        return newArrayWithInit(NameParamNumAndModule.class, expressions);
    }

    private CodegenExpression make() {
        return newInstance(NameParamNumAndModule.class, constant(name), constant(paramNum), constant(moduleName));
    }
}
