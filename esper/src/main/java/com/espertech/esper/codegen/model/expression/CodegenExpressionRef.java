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
package com.espertech.esper.codegen.model.expression;

import java.util.Map;
import java.util.Set;

public class CodegenExpressionRef implements CodegenExpression {
    private final String ref;

    public CodegenExpressionRef(String ref) {
        this.ref = ref;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        builder.append(ref);
    }

    public void mergeClasses(Set<Class> classes) {
    }

    public String getRef() {
        return ref;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CodegenExpressionRef that = (CodegenExpressionRef) o;

        return ref.equals(that.ref);
    }

    public int hashCode() {
        return ref.hashCode();
    }
}
