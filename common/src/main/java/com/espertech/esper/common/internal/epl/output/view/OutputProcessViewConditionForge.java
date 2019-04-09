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
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyCodegen;
import com.espertech.esper.common.internal.compile.stage1.spec.SelectClauseStreamSelectorEnum;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.time.node.ExprTimePeriod;
import com.espertech.esper.common.internal.epl.output.condition.OutputConditionFactoryForge;
import com.espertech.esper.common.internal.epl.output.core.OutputProcessViewFactoryForge;
import com.espertech.esper.common.internal.epl.output.core.OutputStrategyPostProcessForge;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorOutputConditionType;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class OutputProcessViewConditionForge implements OutputProcessViewFactoryForge {
    private final OutputStrategyPostProcessForge outputStrategyPostProcessForge;
    private final boolean isDistinct;
    private final MultiKeyClassRef distinctMultiKey;
    private final ExprTimePeriod afterTimePeriodExpr;
    private final Integer afterNumberOfEvents;
    private final OutputConditionFactoryForge outputConditionFactoryForge;
    private final int streamCount;
    private final ResultSetProcessorOutputConditionType conditionType;
    private final boolean terminable;
    private final boolean hasAfter;
    private final boolean unaggregatedUngrouped;
    private final SelectClauseStreamSelectorEnum selectClauseStreamSelector;
    private final EventType[] eventTypes;
    private final EventType resultEventType;

    public OutputProcessViewConditionForge(OutputStrategyPostProcessForge outputStrategyPostProcessForge, boolean isDistinct, MultiKeyClassRef distinctMultiKey, ExprTimePeriod afterTimePeriodExpr, Integer afterNumberOfEvents, OutputConditionFactoryForge outputConditionFactoryForge, int streamCount, ResultSetProcessorOutputConditionType conditionType, boolean terminable, boolean hasAfter, boolean unaggregatedUngrouped, SelectClauseStreamSelectorEnum selectClauseStreamSelector, EventType[] eventTypes, EventType resultEventType) {
        this.outputStrategyPostProcessForge = outputStrategyPostProcessForge;
        this.isDistinct = isDistinct;
        this.distinctMultiKey = distinctMultiKey;
        this.afterTimePeriodExpr = afterTimePeriodExpr;
        this.afterNumberOfEvents = afterNumberOfEvents;
        this.outputConditionFactoryForge = outputConditionFactoryForge;
        this.streamCount = streamCount;
        this.conditionType = conditionType;
        this.terminable = terminable;
        this.hasAfter = hasAfter;
        this.unaggregatedUngrouped = unaggregatedUngrouped;
        this.selectClauseStreamSelector = selectClauseStreamSelector;
        this.eventTypes = eventTypes;
        this.resultEventType = resultEventType;
    }

    public boolean isCodeGenerated() {
        return false;
    }

    public void provideCodegen(CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenExpressionRef spec = ref("spec");
        method.getBlock()
                .declareVar(OutputProcessViewConditionSpec.class, spec.getRef(), newInstance(OutputProcessViewConditionSpec.class))
                .exprDotMethod(spec, "setConditionType", enumValue(ResultSetProcessorOutputConditionType.class, conditionType.name()))
                .exprDotMethod(spec, "setOutputConditionFactory", outputConditionFactoryForge.make(method, symbols, classScope))
                .exprDotMethod(spec, "setStreamCount", constant(streamCount))
                .exprDotMethod(spec, "setTerminable", constant(terminable))
                .exprDotMethod(spec, "setSelectClauseStreamSelector", enumValue(SelectClauseStreamSelectorEnum.class, selectClauseStreamSelector.name()))
                .exprDotMethod(spec, "setPostProcessFactory", outputStrategyPostProcessForge == null ? constantNull() : outputStrategyPostProcessForge.make(method, symbols, classScope))
                .exprDotMethod(spec, "setHasAfter", constant(hasAfter))
                .exprDotMethod(spec, "setDistinct", constant(isDistinct))
                .exprDotMethod(spec, "setDistinctKeyGetter", MultiKeyCodegen.codegenGetterEventDistinct(isDistinct, resultEventType, distinctMultiKey, method, classScope))
                .exprDotMethod(spec, "setResultEventType", EventTypeUtility.resolveTypeCodegen(resultEventType, symbols.getAddInitSvc(method)))
                .exprDotMethod(spec, "setAfterTimePeriod", afterTimePeriodExpr == null ? constantNull() : afterTimePeriodExpr.getTimePeriodComputeForge().makeEvaluator(method, classScope))
                .exprDotMethod(spec, "setAfterConditionNumberOfEvents", constant(afterNumberOfEvents))
                .exprDotMethod(spec, "setUnaggregatedUngrouped", constant(unaggregatedUngrouped))
                .exprDotMethod(spec, "setEventTypes", EventTypeUtility.resolveTypeArrayCodegen(eventTypes, EPStatementInitServices.REF))
                .methodReturn(newInstance(OutputProcessViewConditionFactory.class, spec));
    }

    public void updateCodegen(CodegenMethod method, CodegenClassScope classScope) {
    }

    public void processCodegen(CodegenMethod method, CodegenClassScope classScope) {
    }

    public void iteratorCodegen(CodegenMethod method, CodegenClassScope classScope) {
    }

    public void collectSchedules(List<ScheduleHandleCallbackProvider> scheduleHandleCallbackProviders) {
        if (outputConditionFactoryForge != null) {
            outputConditionFactoryForge.collectSchedules(scheduleHandleCallbackProviders);
        }
    }
}
