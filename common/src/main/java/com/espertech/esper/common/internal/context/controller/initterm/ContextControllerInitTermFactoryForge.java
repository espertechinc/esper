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

import com.espertech.esper.common.client.annotation.AppliesTo;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.compile.stage1.spec.ContextSpecInitiatedTerminated;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerFactoryEnv;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerForgeBase;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerPortableInfo;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.context.util.ContextPropertyEventType;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.client.util.StateMgmtSetting;
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

    public void validateGetContextProps(LinkedHashMap<String, Object> props, String contextName, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) throws ExprValidationException {

        props.put(ContextPropertyEventType.PROP_CTX_STARTTIME, EPTypePremade.LONGBOXED.getEPType());
        props.put(ContextPropertyEventType.PROP_CTX_ENDTIME, EPTypePremade.LONGBOXED.getEPType());

        LinkedHashSet<String> allTags = new LinkedHashSet<String>();
        ContextPropertyEventType.addEndpointTypes(detail.getStartCondition(), props, allTags);
        ContextPropertyEventType.addEndpointTypes(detail.getEndCondition(), props, allTags);

        distinctStateMgmtSettings = StateMgmtSettingDefault.INSTANCE;
        if (detail.getDistinctExpressions() != null && detail.getDistinctExpressions().length > 0) {
            distinctStateMgmtSettings = services.getStateMgmtSettingsProvider().getContext(statementRawInfo, contextName, AppliesTo.CONTEXT_INITTERM_DISTINCT);
        }
        ctxStateMgmtSettings = services.getStateMgmtSettingsProvider().getContext(statementRawInfo, contextName, AppliesTo.CONTEXT_INITTERM);
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
}
