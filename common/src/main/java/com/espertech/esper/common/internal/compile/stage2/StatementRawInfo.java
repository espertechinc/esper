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
package com.espertech.esper.common.internal.compile.stage2;

import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.compile.stage1.Compilable;
import com.espertech.esper.common.internal.context.compile.ContextCompileTimeDescriptor;

import java.io.StringWriter;
import java.lang.annotation.Annotation;

public class StatementRawInfo {
    private final int statementNumber;
    private final String statementName;
    private final Annotation[] annotations;
    private final StatementType statementType;
    private final ContextCompileTimeDescriptor optionalContextDescriptor;
    private final String intoTableName;
    private final Compilable compilable;
    private final String moduleName;

    public StatementRawInfo(int statementNumber, String statementName, Annotation[] annotations, StatementType statementType, ContextCompileTimeDescriptor optionalContextDescriptor, String intoTableName, Compilable compilable, String moduleName) {
        this.statementNumber = statementNumber;
        this.statementName = statementName;
        this.annotations = annotations;
        this.statementType = statementType;
        this.optionalContextDescriptor = optionalContextDescriptor;
        this.intoTableName = intoTableName;
        this.compilable = compilable;
        this.moduleName = moduleName;
    }

    public int getStatementNumber() {
        return statementNumber;
    }

    public String getStatementName() {
        return statementName;
    }

    public Annotation[] getAnnotations() {
        return annotations;
    }

    public StatementType getStatementType() {
        return statementType;
    }

    public ContextCompileTimeDescriptor getOptionalContextDescriptor() {
        return optionalContextDescriptor;
    }

    public String getContextName() {
        return optionalContextDescriptor == null ? null : optionalContextDescriptor.getContextName();
    }

    public String getIntoTableName() {
        return intoTableName;
    }

    public Compilable getCompilable() {
        return compilable;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void appendCodeDebugInfo(StringWriter writer) {
        writer.append("statement ")
                .append(Integer.toString(statementNumber))
                .append(" name ")
                .append(statementName.replace("\\", ""));
    }
}
