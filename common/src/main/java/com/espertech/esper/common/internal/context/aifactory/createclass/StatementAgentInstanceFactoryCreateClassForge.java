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
package com.espertech.esper.common.internal.context.aifactory.createclass;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeBuilder;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;

public class StatementAgentInstanceFactoryCreateClassForge {

    private final EventType statementEventType;
    private final String className;

    public StatementAgentInstanceFactoryCreateClassForge(EventType statementEventType, String className) {
        this.statementEventType = statementEventType;
        this.className = className;
    }

    public CodegenMethod initializeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        return new SAIFFInitializeBuilder(StatementAgentInstanceFactoryCreateClass.EPTYPE, this.getClass(), "saiff", parent, symbols, classScope)
            .eventtype("statementEventType", statementEventType)
            .constant("className", className)
            .buildMethod();
    }
}
