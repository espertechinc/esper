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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyCodegen;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.time.node.ExprTimePeriod;
import com.espertech.esper.common.internal.epl.output.core.OutputProcessViewFactoryForge;
import com.espertech.esper.common.internal.epl.output.core.OutputStrategyPostProcessForge;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Output process view that does not enforce any output policies and may simply
 * hand over events to child views, but works with distinct and after-output policies
 */
public class OutputProcessViewDirectDistinctOrAfterFactoryForge implements OutputProcessViewFactoryForge {
    private final OutputStrategyPostProcessForge outputStrategyPostProcessForge;
    private final boolean isDistinct;
    private final MultiKeyClassRef distinctMultiKey;
    protected final ExprTimePeriod afterTimePeriod;
    protected final Integer afterConditionNumberOfEvents;
    protected final EventType resultEventType;

    public OutputProcessViewDirectDistinctOrAfterFactoryForge(OutputStrategyPostProcessForge outputStrategyPostProcessForge, boolean isDistinct, MultiKeyClassRef distinctMultiKey, ExprTimePeriod afterTimePeriod, Integer afterConditionNumberOfEvents, EventType resultEventType) {
        this.outputStrategyPostProcessForge = outputStrategyPostProcessForge;
        this.isDistinct = isDistinct;
        this.distinctMultiKey = distinctMultiKey;
        this.afterTimePeriod = afterTimePeriod;
        this.afterConditionNumberOfEvents = afterConditionNumberOfEvents;
        this.resultEventType = resultEventType;
    }

    public boolean isCodeGenerated() {
        return false;
    }

    public void provideCodegen(CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        method.getBlock().methodReturn(
                newInstance(OutputProcessViewDirectDistinctOrAfterFactory.class,
                        outputStrategyPostProcessForge == null ? constantNull() : outputStrategyPostProcessForge.make(method, symbols, classScope),
                        constant(isDistinct), MultiKeyCodegen.codegenGetterEventDistinct(isDistinct, resultEventType, distinctMultiKey, method, classScope),
                        afterTimePeriod == null ? constantNull() : afterTimePeriod.getTimePeriodComputeForge().makeEvaluator(method, classScope),
                        constant(afterConditionNumberOfEvents),
                        EventTypeUtility.resolveTypeCodegen(resultEventType, symbols.getAddInitSvc(method))));
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