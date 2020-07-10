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
package com.espertech.esper.common.internal.view.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.annotation.AppliesTo;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.client.util.StateMgmtSetting;
import com.espertech.esper.common.internal.statemgmtsettings.StateMgmtSettingDefault;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public abstract class ViewFactoryForgeBase implements ViewFactoryForge {
    protected EventType eventType;
    protected StateMgmtSetting stateMgmtSettings = StateMgmtSettingDefault.INSTANCE;

    protected abstract EPTypeClass typeOfFactory();

    protected abstract String factoryMethod();

    protected abstract AppliesTo appliesTo();

    protected abstract void attachValidate(EventType parentEventType, int streamNumber, ViewForgeEnv viewForgeEnv, boolean grouped) throws ViewParameterException;

    protected abstract void assign(CodegenMethod method, CodegenExpressionRef factory, SAIFFInitializeSymbol symbols, CodegenClassScope classScope);

    public final EventType getEventType() {
        return eventType;
    }

    public final void attach(EventType parentEventType, int streamNumber, ViewForgeEnv viewForgeEnv, boolean grouped) throws ViewParameterException {
        attachValidate(parentEventType, streamNumber, viewForgeEnv, grouped);
        stateMgmtSettings = viewForgeEnv.getStateMgmtSettingsProvider().getView(viewForgeEnv.getStatementRawInfo(), streamNumber, viewForgeEnv.isSubquery(), grouped, appliesTo());
    }

    public final CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        if (eventType == null) {
            throw new IllegalStateException("Event type is unassigned");
        }

        CodegenMethod method = parent.makeChild(ViewFactory.EPTYPE, this.getClass(), classScope);
        CodegenExpressionRef factory = ref("factory");
        method.getBlock()
                .declareVar(typeOfFactory(), factory.getRef(), exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETVIEWFACTORYSERVICE).add(factoryMethod(), stateMgmtSettings.toExpression()))
                .exprDotMethod(factory, "setEventType", EventTypeUtility.resolveTypeCodegen(eventType, EPStatementInitServices.REF));

        assign(method, factory, symbols, classScope);

        method.getBlock().methodReturn(ref("factory"));
        return localMethod(method);
    }
}
