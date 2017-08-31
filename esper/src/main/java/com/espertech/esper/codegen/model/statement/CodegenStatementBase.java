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

import com.espertech.esper.codegen.core.CodegenIndent;

import java.util.Map;

public abstract class CodegenStatementBase implements CodegenStatement {

    public abstract void renderStatement(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass);

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass, int level, CodegenIndent indent) {
        renderStatement(builder, imports, isInnerClass);
        builder.append(";\n");
    }
}
