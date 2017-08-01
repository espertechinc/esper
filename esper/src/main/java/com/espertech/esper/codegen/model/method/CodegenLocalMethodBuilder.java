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

import com.espertech.esper.codegen.core.*;

import java.util.ArrayList;
import java.util.List;

public class CodegenLocalMethodBuilder {
    private final Class returnType;
    private final String generatorDetail;
    private final CodegenContext context;
    private final List<CodegenParamSet> paramSets = new ArrayList<>(2);

    public CodegenLocalMethodBuilder(Class returnType, String generatorDetail, CodegenContext context) {
        this.returnType = returnType;
        this.generatorDetail = generatorDetail;
        this.context = context;
    }

    public CodegenLocalMethodBuilder add(Class type, String name) {
        paramSets.add(new CodegenParamSetSingle(new CodegenNamedParam(type, name)));
        return this;
    }

    public CodegenLocalMethodBuilder add(CodegenNamedParam param) {
        paramSets.add(new CodegenParamSetSingle(param));
        return this;
    }

    public CodegenLocalMethodBuilder add(CodegenParamSet set) {
        paramSets.add(set);
        return this;
    }

    public CodegenBlock begin() {
        String methodName = CodeGenerationIDGenerator.generateMethod();
        CodegenMethod method = new CodegenMethod(new CodegenMethodFootprint(returnType, methodName, paramSets, generatorDetail));
        context.getMethods().add(method);
        return method.statements();
    }
}
