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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPTypeClassParameterized;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.output.core.OutputProcessViewCodegenNames.*;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.REF_NEWDATA;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.REF_OLDDATA;
import static com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCode.instblock;

public class OutputProcessViewDirectSimpleForge implements OutputProcessViewFactoryForge {
    private final OutputStrategyPostProcessForge postProcess;

    public OutputProcessViewDirectSimpleForge(OutputStrategyPostProcessForge postProcess) {
        this.postProcess = postProcess;
    }

    public boolean isDirectAndSimple() {
        return postProcess == null;
    }

    public boolean isCodeGenerated() {
        return postProcess != null;
    }

    public void provideCodegen(CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        if (postProcess != null) {
            throw new IllegalStateException("Provide is not required");
        }
        method.getBlock().methodReturn(publicConstValue(OutputProcessViewDirectSimpleFactory.EPTYPE, "INSTANCE"));
    }

    public void updateCodegen(CodegenMethod method, CodegenClassScope classScope) {

        method.getBlock().apply(instblock(classScope, "qOutputProcessNonBuffered", REF_NEWDATA, REF_OLDDATA));

        generateRSPCall("processViewResult", method, classScope);

        CodegenExpression newOldIsNull = and(equalsNull(exprDotMethod(ref("newOldEvents"), "getFirst")), equalsNull(exprDotMethod(ref("newOldEvents"), "getSecond")));
        method.getBlock()
                .declareVar(EPTypePremade.BOOLEANBOXED.getEPType(), "forceOutput", constant(false))
                .ifCondition(and(equalsNull(REF_NEWDATA), equalsNull(REF_OLDDATA)))
                .ifCondition(or(equalsNull(ref("newOldEvents")), newOldIsNull))
                .assignRef("forceOutput", constantTrue());

        method.getBlock()
                .expression(localMethod(postProcess.postProcessCodegenMayNullMayForce(classScope, method), ref("forceOutput"), ref("newOldEvents")))
                .apply(instblock(classScope, "aOutputProcessNonBuffered"));
    }

    public void processCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().apply(instblock(classScope, "qOutputProcessNonBufferedJoin", REF_NEWDATA, REF_OLDDATA));

        generateRSPCall("processJoinResult", method, classScope);

        method.getBlock().ifRefNull("newOldEvents")
                .apply(instblock(classScope, "aOutputProcessNonBufferedJoin"))
                .blockReturnNoValue();

        method.getBlock().expression(localMethod(postProcess.postProcessCodegenMayNullMayForce(classScope, method), constantFalse(), ref("newOldEvents")));

        method.getBlock().apply(instblock(classScope, "aOutputProcessNonBufferedJoin"));
    }

    public void iteratorCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(staticMethod(OutputStrategyUtil.class, "getIterator", ref(NAME_JOINEXECSTRATEGY), ref(NAME_RESULTSETPROCESSOR), ref(NAME_PARENTVIEW), constant(false), constantNull()));
    }

    public void collectSchedules(List<ScheduleHandleCallbackProvider> scheduleHandleCallbackProviders) {
    }

    private void generateRSPCall(String rspMethod, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock()
                .declareVar(EPTypePremade.BOOLEANBOXED.getEPType(), "isGenerateSynthetic", exprDotMethod(member("o." + NAME_STATEMENTRESULTSVC), "isMakeSynthetic"))
                .declareVar(EPTypePremade.BOOLEANBOXED.getEPType(), "isGenerateNatural", exprDotMethod(member("o." + NAME_STATEMENTRESULTSVC), "isMakeNatural"))
                .declareVar(EPTypeClassParameterized.from(UniformPair.class, EventBean[].class), "newOldEvents", exprDotMethod(ref(NAME_RESULTSETPROCESSOR), rspMethod, REF_NEWDATA, REF_OLDDATA, ref("isGenerateSynthetic")))
                .ifCondition(and(not(ref("isGenerateSynthetic")), not(ref("isGenerateNatural")))).blockReturnNoValue();
    }
}
