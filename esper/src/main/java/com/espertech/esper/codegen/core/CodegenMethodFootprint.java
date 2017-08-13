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
package com.espertech.esper.codegen.core;

import com.espertech.esper.codegen.model.method.CodegenParamSet;

import java.util.List;
import java.util.Set;

public class CodegenMethodFootprint {
    private final Class returnType;
    private final CodegenMethodId methodId;
    private final List<CodegenParamSet> params;
    private final String optionalComment;

    public CodegenMethodFootprint(Class returnType, CodegenMethodId methodId, List<CodegenParamSet> params, String optionalComment) {
        if (returnType == null) {
            throw new IllegalArgumentException("Invalid null return type");
        }
        this.returnType = returnType;
        this.methodId = methodId;
        this.params = params;
        this.optionalComment = optionalComment;
    }

    public Class getReturnType() {
        return returnType;
    }

    public CodegenMethodId getMethodId() {
        return methodId;
    }

    public List<CodegenParamSet> getParams() {
        return params;
    }

    public String getOptionalComment() {
        return optionalComment;
    }

    public void mergeClasses(Set<Class> classes) {
        classes.add(returnType);
        for (CodegenParamSet param : params) {
            param.mergeClasses(classes);
        }
    }
}
