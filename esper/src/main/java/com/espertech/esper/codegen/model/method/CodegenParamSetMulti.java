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
import com.espertech.esper.codegen.model.expression.CodegenExpression;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;

public class CodegenParamSetMulti extends CodegenParamSet {
    private final List<CodegenNamedParam> params;

    public CodegenParamSetMulti(List<CodegenNamedParam> params) {
        this.params = params;
    }

    public void mergeClasses(Set<Class> classes) {
        for (CodegenNamedParam param : params) {
            param.mergeClasses(classes);
        }
    }

    public void render(StringBuilder builder, Map<Class, String> imports, CodegenIndent codegenIndent, String optionalComment) {
        CodegenNamedParam.render(builder, imports, params);
    }

    public CodegenPassSet getPassAll() {
        CodegenExpression[] refs = new CodegenExpression[params.size()];
        for (int i = 0; i < params.size(); i++) {
            refs[i] = ref(params.get(i).getName());
        }
        return new CodegenPassSetMulti(refs);
    }
}
