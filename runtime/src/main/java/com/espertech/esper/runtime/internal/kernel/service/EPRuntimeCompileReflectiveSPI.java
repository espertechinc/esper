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
package com.espertech.esper.runtime.internal.kernel.service;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.module.Module;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.common.internal.compile.util.CompileExpressionSPI;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.runtime.client.EPRuntime;

public class EPRuntimeCompileReflectiveSPI implements CompileExpressionSPI {
    private final EPRuntimeCompileReflectiveService provider;
    private final EPRuntime runtime;

    public EPRuntimeCompileReflectiveSPI(EPRuntimeCompileReflectiveService provider, EPRuntime runtime) {
        this.provider = provider;
        this.runtime = runtime;
    }

    public boolean isCompilerAvailable() {
        return provider.isCompilerAvailable();
    }

    public EPCompiled reflectiveCompile(String epl) {
        return provider.reflectiveCompile(epl, runtime.getConfigurationDeepCopy(), runtime.getRuntimePath());
    }

    public EPCompiled reflectiveCompile(Module module) {
        return provider.reflectiveCompile(module, runtime.getConfigurationDeepCopy(), runtime.getRuntimePath());
    }

    public EPCompiled reflectiveCompileFireAndForget(String epl) throws EPException {
        return provider.reflectiveCompileFireAndForget(epl, runtime.getConfigurationDeepCopy(), runtime.getRuntimePath());
    }

    public ExprNode reflectiveCompileExpression(String epl, EventType[] eventTypes, String[] streamNames) throws EPException {
        return provider.reflectiveCompileExpression(epl, eventTypes, streamNames, runtime.getConfigurationDeepCopy());
    }

    public EPStatementObjectModel reflectiveEPLToModel(String epl) {
        return provider.reflectiveEPLToModel(epl, runtime.getConfigurationDeepCopy());
    }

    public ExprNode compileExpression(String epl, EventType[] eventTypes, String[] streamNames) throws EPException {
        return reflectiveCompileExpression(epl, eventTypes, streamNames);
    }

    public Module reflectiveParseModule(String epl) {
        return provider.reflectiveParseModule(epl);
    }
}
