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

import java.util.Map;
import java.util.Set;

import static com.espertech.esper.codegen.core.CodeGenerationHelper.appendClassName;

public class CodegenStatementDeclareVarEventPerStreamUnd extends CodegenStatementBase {
    private final Class clazz;
    private final int streamNum;

    public CodegenStatementDeclareVarEventPerStreamUnd(Class clazz, int streamNum) {
        this.clazz = clazz;
        this.streamNum = streamNum;
    }

    public void renderStatement(StringBuilder builder, Map<Class, String> imports) {
        appendClassName(builder, clazz, null, imports);
        builder.append(" s").append(streamNum).append("=(");
        appendClassName(builder, clazz, null, imports);
        builder.append(")eventsPerStream[").append(streamNum).append("].getUnderlying()");
    }

    public void mergeClasses(Set<Class> classes) {
        classes.add(clazz);
    }
}
