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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public abstract class ViewFactoryForgeBase implements ViewFactoryForge {
    protected EventType eventType;

    protected abstract Class typeOfFactory();

    protected abstract String factoryMethod();

    protected abstract void assign(CodegenMethod method, CodegenExpressionRef factory, SAIFFInitializeSymbol symbols, CodegenClassScope classScope);

    public final EventType getEventType() {
        return eventType;
    }

    public final CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        if (eventType == null) {
            throw new IllegalStateException("Event type is unassigned");
        }

        CodegenMethod method = parent.makeChild(ViewFactory.class, this.getClass(), classScope);
        CodegenExpressionRef factory = ref("factory");
        method.getBlock()
                .declareVar(typeOfFactory(), factory.getRef(), exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETVIEWFACTORYSERVICE).add(factoryMethod()))
                .exprDotMethod(factory, "setEventType", EventTypeUtility.resolveTypeCodegen(eventType, EPStatementInitServices.REF));

        assign(method, factory, symbols, classScope);

        method.getBlock().methodReturn(ref("factory"));
        return localMethod(method);
    }
}
