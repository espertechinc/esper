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
package com.espertech.esper.common.internal.context.controller.initterm;

import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.client.util.StateMgmtSetting;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.compile.stage1.spec.ContextSpecInitiatedTerminated;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.compile.ContextMetaData;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerFactoryEnv;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerForgeBase;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerPortableInfo;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.context.util.ContextPropertyEventType;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.fabric.FabricCharge;
import com.espertech.esper.common.internal.statemgmtsettings.StateMgmtSettingDefault;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.exprDotMethodChain;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

public class ContextControllerInitTermFactoryForge extends ContextControllerForgeBase {

    private final ContextSpecInitiatedTerminated detail;
    private StateMgmtSetting distinctStateMgmtSettings;
    private StateMgmtSetting ctxStateMgmtSettings;

    public ContextControllerInitTermFactoryForge(ContextControllerFactoryEnv ctx, ContextSpecInitiatedTerminated detail) {
        super(ctx);
        this.detail = detail;
    }

    public void validateGetContextProps(LinkedHashMap<String, Object> props, String contextName, int controllerLevel, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) throws ExprValidationException {
        props.put(ContextPropertyEventType.PROP_CTX_STARTTIME, EPTypePremade.LONGBOXED.getEPType());
        props.put(ContextPropertyEventType.PROP_CTX_ENDTIME, EPTypePremade.LONGBOXED.getEPType());

        LinkedHashSet<String> allTags = new LinkedHashSet<String>();
        ContextPropertyEventType.addEndpointTypes(detail.getStartCondition(), props, allTags);
        ContextPropertyEventType.addEndpointTypes(detail.getEndCondition(), props, allTags);
    }

    public void planStateSettings(ContextMetaData detail, FabricCharge fabricCharge, int controllerLevel, String nestedContextName, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) {
        distinctStateMgmtSettings = StateMgmtSettingDefault.INSTANCE;
        if (this.detail.getDistinctExpressions() != null && this.detail.getDistinctExpressions().length > 0) {
            distinctStateMgmtSettings = services.getStateMgmtSettingsProvider().context().contextInitTermDistinct(fabricCharge, detail, this, statementRawInfo, controllerLevel);
        }
        ctxStateMgmtSettings = services.getStateMgmtSettingsProvider().context().contextInitTerm(fabricCharge, detail, this, statementRawInfo, controllerLevel);
    }

    public CodegenMethod makeCodegen(CodegenClassScope classScope, CodegenMethodScope parent, SAIFFInitializeSymbol symbols) {
        CodegenMethod method = parent.makeChild(ContextControllerInitTermFactory.EPTYPE, this.getClass(), classScope);
        method.getBlock()
                .declareVar(ContextControllerInitTermFactory.EPTYPE, "factory", exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETCONTEXTSERVICEFACTORY).add("initTermFactory", distinctStateMgmtSettings.toExpression(), ctxStateMgmtSettings.toExpression()))
                .exprDotMethod(ref("factory"), "setInitTermSpec", detail.makeCodegen(method, symbols, classScope))
                .methodReturn(ref("factory"));
        return method;
    }

    public ContextControllerPortableInfo getValidationInfo() {
        return ContextControllerInitTermValidation.INSTANCE;
    }

    public ContextSpecInitiatedTerminated getDetail() {
        return detail;
    }
}
