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
package com.espertech.esper.codegen.model.statement;

import com.espertech.esper.codegen.base.CodegenBlock;

import java.util.Set;

public class CodegenStatementTryCatchCatchBlock {
    private final Class ex;
    private final String name;
    private final CodegenBlock block;

    public CodegenStatementTryCatchCatchBlock(Class ex, String name, CodegenBlock block) {
        this.ex = ex;
        this.name = name;
        this.block = block;
    }

    public Class getEx() {
        return ex;
    }

    public String getName() {
        return name;
    }

    public CodegenBlock getBlock() {
        return block;
    }

    void mergeClasses(Set<Class> classes) {
        classes.add(ex);
        block.mergeClasses(classes);
    }
}
