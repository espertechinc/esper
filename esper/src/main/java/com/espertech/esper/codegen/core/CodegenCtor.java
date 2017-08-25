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

import com.espertech.esper.codegen.base.CodegenBlock;

import java.util.List;
import java.util.Set;

public class CodegenCtor {
    private final CodegenBlock block;
    private final List<CodegenCtorParam> params;

    public CodegenCtor(List<CodegenCtorParam> params) {
        this.block = new CodegenBlock(this);
        this.params = params;
    }

    public CodegenBlock getBlock() {
        return block;
    }

    public List<CodegenCtorParam> getParams() {
        return params;
    }

    public void mergeClasses(Set<Class> classes) {
        block.mergeClasses(classes);
        for (CodegenCtorParam param : params) {
            param.mergeClasses(classes);
        }
    }
}
