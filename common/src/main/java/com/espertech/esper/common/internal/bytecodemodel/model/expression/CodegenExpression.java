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
package com.espertech.esper.common.internal.bytecodemodel.model.expression;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public interface CodegenExpression {
    void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass);

    void mergeClasses(Set<Class> classes);

    void traverseExpressions(Consumer<CodegenExpression> consumer);

    static void assertNonNullArgs(CodegenExpression[] params) {
        for (int i = 0; i < params.length; i++) {
            if (params[i] == null) {
                throw new IllegalArgumentException("Parameter " + i + " is null");
            }
        }
    }
}
