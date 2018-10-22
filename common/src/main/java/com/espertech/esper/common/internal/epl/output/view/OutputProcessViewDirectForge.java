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
package com.espertech.esper.common.internal.epl.output.view;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.output.core.OutputProcessViewFactoryForge;
import com.espertech.esper.common.internal.epl.output.core.OutputStrategyPostProcessForge;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class OutputProcessViewDirectForge implements OutputProcessViewFactoryForge {
    private OutputStrategyPostProcessForge outputStrategyPostProcessForge;

    public OutputProcessViewDirectForge(OutputStrategyPostProcessForge outputStrategyPostProcessForge) {
        this.outputStrategyPostProcessForge = outputStrategyPostProcessForge;
    }

    public boolean isCodeGenerated() {
        return false;
    }

    public void provideCodegen(CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenExpressionRef factory = ref("factory");
        method.getBlock()
                .declareVar(OutputProcessViewDirectFactory.class, factory.getRef(), newInstance(OutputProcessViewDirectFactory.class))
                .exprDotMethod(factory, "setPostProcessFactory", outputStrategyPostProcessForge == null ? constantNull() : outputStrategyPostProcessForge.make(method, symbols, classScope))
                .methodReturn(factory);
    }

    public void updateCodegen(CodegenMethod method, CodegenClassScope classScope) {
    }

    public void processCodegen(CodegenMethod method, CodegenClassScope classScope) {
    }

    public void iteratorCodegen(CodegenMethod method, CodegenClassScope classScope) {
    }

    public void collectSchedules(List<ScheduleHandleCallbackProvider> scheduleHandleCallbackProviders) {
    }
}
