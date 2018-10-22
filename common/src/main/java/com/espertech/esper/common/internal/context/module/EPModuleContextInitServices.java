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
package com.espertech.esper.common.internal.context.module;

import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleContextInitializeSymbol;
import com.espertech.esper.common.internal.context.compile.ContextCollector;
import com.espertech.esper.common.internal.event.path.EventTypeResolver;

public interface EPModuleContextInitServices {
    CodegenExpressionRef REF = ModuleContextInitializeSymbol.REF_INITSVC;

    String GETCONTEXTCOLLECTOR = "getContextCollector";
    String GETEVENTTYPERESOLVER = EPStatementInitServices.GETEVENTTYPERESOLVER;

    ContextCollector getContextCollector();

    EventTypeResolver getEventTypeResolver();
}
