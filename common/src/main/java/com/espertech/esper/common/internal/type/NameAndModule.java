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

import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class NameAndModule {
    public final static NameAndModule[] EMPTY_ARRAY = new NameAndModule[0];

    private final String name;
    private final String moduleName;

    public NameAndModule(String name, String moduleName) {
        this.name = name;
        this.moduleName = moduleName;
    }

    public String getName() {
        return name;
    }

    public String getModuleName() {
        return moduleName;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NameAndModule that = (NameAndModule) o;

        if (!name.equals(that.name)) return false;
        return moduleName != null ? moduleName.equals(that.moduleName) : that.moduleName == null;
    }

    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (moduleName != null ? moduleName.hashCode() : 0);
        return result;
    }

    public static CodegenExpression makeArray(Collection<NameAndModule> names) {
        if (names.isEmpty()) {
            return enumValue(NameAndModule.class, "EMPTY_ARRAY");
        }
        CodegenExpression[] expressions = new CodegenExpression[names.size()];
        int count = 0;
        for (NameAndModule entry : names) {
            expressions[count++] = entry.make();
        }
        return newArrayWithInit(NameAndModule.class, expressions);
    }

    private CodegenExpression make() {
        return newInstance(NameAndModule.class, constant(name), constant(moduleName));
    }

    public static NameAndModule findName(String searchForName, NameAndModule[] names) {
        NameAndModule found = null;
        for (NameAndModule item : names) {
            if (item.getName().equals(searchForName)) {
                if (found != null) {
                    throw new IllegalStateException("Found multiple entries for name '" + searchForName + "'");
                }
                found = item;
            }
        }
        if (found == null) {
            throw new IllegalStateException("Failed to find name '" + searchForName + "'");
        }
        return found;
    }
}
