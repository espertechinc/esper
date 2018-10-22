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
package com.espertech.esper.common.internal.epl.output.core;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;

import java.util.List;

public interface OutputProcessViewFactoryForge {
    boolean isCodeGenerated();

    void provideCodegen(CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope);

    void updateCodegen(CodegenMethod method, CodegenClassScope classScope);

    void processCodegen(CodegenMethod method, CodegenClassScope classScope);

    void iteratorCodegen(CodegenMethod method, CodegenClassScope classScope);

    void collectSchedules(List<ScheduleHandleCallbackProvider> scheduleHandleCallbackProviders);
}
