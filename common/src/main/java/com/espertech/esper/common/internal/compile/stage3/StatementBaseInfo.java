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
package com.espertech.esper.common.internal.compile.stage3;

import com.espertech.esper.common.internal.compile.stage1.Compilable;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompiled;
import com.espertech.esper.common.internal.context.util.ContextPropertyRegistry;

import java.io.Serializable;

public class StatementBaseInfo {
    private final Compilable compilable;
    private StatementSpecCompiled statementSpec;
    private final Serializable userObjectCompileTime;
    private final StatementRawInfo statementRawInfo;
    private final String optionalModuleName;

    public StatementBaseInfo(Compilable compilable, StatementSpecCompiled statementSpec, Serializable userObjectCompileTime, StatementRawInfo statementRawInfo, String optionalModuleName) {
        this.compilable = compilable;
        this.statementSpec = statementSpec;
        this.userObjectCompileTime = userObjectCompileTime;
        this.statementRawInfo = statementRawInfo;
        this.optionalModuleName = optionalModuleName;
    }

    public Compilable getCompilable() {
        return compilable;
    }

    public StatementSpecCompiled getStatementSpec() {
        return statementSpec;
    }

    public String getStatementName() {
        return statementRawInfo.getStatementName();
    }

    public Serializable getUserObjectCompileTime() {
        return userObjectCompileTime;
    }

    public int getStatementNumber() {
        return statementRawInfo.getStatementNumber();
    }

    public StatementRawInfo getStatementRawInfo() {
        return statementRawInfo;
    }

    public String getModuleName() {
        return optionalModuleName;
    }

    public ContextPropertyRegistry getContextPropertyRegistry() {
        if (statementRawInfo.getOptionalContextDescriptor() == null) {
            return null;
        }
        return statementRawInfo.getOptionalContextDescriptor().getContextPropertyRegistry();
    }

    public String getContextName() {
        if (statementRawInfo.getOptionalContextDescriptor() == null) {
            return null;
        }
        return statementRawInfo.getOptionalContextDescriptor().getContextName();
    }

    public void setStatementSpec(StatementSpecCompiled statementSpec) {
        this.statementSpec = statementSpec;
    }
}
