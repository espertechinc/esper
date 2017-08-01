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
package com.espertech.esper.codegen.model.method;

import com.espertech.esper.codegen.core.CodegenIndent;
import com.espertech.esper.codegen.core.CodegenNamedParam;

import java.util.Map;
import java.util.Set;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;

public class CodegenParamSetSingle extends CodegenParamSet {
    private final CodegenNamedParam param;

    public CodegenParamSetSingle(CodegenNamedParam param) {
        this.param = param;
    }

    public void mergeClasses(Set<Class> classes) {
        param.mergeClasses(classes);
    }

    public void render(StringBuilder builder, Map<Class, String> imports, CodegenIndent codegenIndent, String optionalComment) {
        param.render(builder, imports);
    }

    public CodegenPassSet getPassAll() {
        return new CodegenPassSetSingle(ref(param.getName()));
    }
}
