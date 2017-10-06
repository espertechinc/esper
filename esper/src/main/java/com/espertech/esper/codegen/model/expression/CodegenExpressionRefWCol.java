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

public class CodegenExpressionRefWCol extends CodegenExpressionRef {
    private final int col;

    public CodegenExpressionRefWCol(String ref, int col) {
        super(ref);
        this.col = col;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        super.render(builder, imports, isInnerClass);
        builder.append(col);
    }

    public void mergeClasses(Set<Class> classes) {
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        CodegenExpressionRefWCol that = (CodegenExpressionRefWCol) o;

        return col == that.col;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + col;
        return result;
    }

    public String toName() {
        return getRef() + col;
    }
}
