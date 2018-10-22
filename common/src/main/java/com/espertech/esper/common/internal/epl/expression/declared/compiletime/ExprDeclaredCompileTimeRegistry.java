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
package com.espertech.esper.common.internal.epl.expression.declared.compiletime;

import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionDeclItem;
import com.espertech.esper.common.internal.epl.util.CompileTimeRegistry;

import java.util.HashMap;
import java.util.Map;

public class ExprDeclaredCompileTimeRegistry implements CompileTimeRegistry {
    private final Map<String, ExpressionDeclItem> expressions = new HashMap<>();

    public void newExprDeclared(ExpressionDeclItem detail) {
        if (!detail.getVisibility().isModuleProvidedAccessModifier()) {
            throw new IllegalStateException("Invalid visibility for contexts");
        }
        String name = detail.getName();
        ExpressionDeclItem existing = expressions.get(name);
        if (existing != null) {
            throw new IllegalStateException("Duplicate declared expression encountered for name '" + name + "'");
        }
        expressions.put(name, detail);
    }

    public Map<String, ExpressionDeclItem> getExpressions() {
        return expressions;
    }
}
