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
package com.espertech.esper.common.internal.context.aifactory.createcontext;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableAIFactoryProviderBase;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerFactory;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerFactoryForge;
import com.espertech.esper.common.internal.context.controller.core.ContextDefinition;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol.REF_STMTINITSVC;

public class StmtClassForgeableAIFactoryProviderCreateContext extends StmtClassForgeableAIFactoryProviderBase {

    private final String contextName;
    private final ContextControllerFactoryForge[] forges;
    private final EventType eventTypeContextProperties;
    private final StatementAgentInstanceFactoryCreateContextForge forge;

    public StmtClassForgeableAIFactoryProviderCreateContext(String className, CodegenPackageScope packageScope, String contextName, ContextControllerFactoryForge[] forges, EventType eventTypeContextProperties, StatementAgentInstanceFactoryCreateContextForge forge) {
        super(className, packageScope);
        this.contextName = contextName;
        this.forges = forges;
        this.eventTypeContextProperties = eventTypeContextProperties;
        this.forge = forge;
    }

    protected Class typeOfFactory() {
        return StatementAgentInstanceFactoryCreateContext.class;
    }

    protected CodegenMethod codegenConstructorInit(CodegenMethodScope parent, CodegenClassScope classScope) {
        SAIFFInitializeSymbol saiffInitializeSymbol = new SAIFFInitializeSymbol();
        CodegenMethod method = parent.makeChildWithScope(typeOfFactory(), this.getClass(), saiffInitializeSymbol, classScope).addParam(EPStatementInitServices.class, REF_STMTINITSVC.getRef());
        method.getBlock()
                .exprDotMethod(REF_STMTINITSVC, "activateContext", constant(contextName), getDefinition(method, saiffInitializeSymbol, classScope))
                .methodReturn(localMethod(forge.initializeCodegen(classScope, method, saiffInitializeSymbol)));
        return method;
    }

    private CodegenExpression getDefinition(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(ContextDefinition.class, this.getClass(), classScope);

        // controllers
        method.getBlock().declareVar(ContextControllerFactory[].class, "controllers", newArrayByLength(ContextControllerFactory.class, constant(forges.length)));
        for (int i = 0; i < forges.length; i++) {
            method.getBlock().assignArrayElement("controllers", constant(i), localMethod(forges[i].makeCodegen(classScope, method, symbols)))
                    .exprDotMethod(arrayAtIndex(ref("controllers"), constant(i)), "setFactoryEnv", forges[i].getFactoryEnv().toExpression());
        }

        method.getBlock().declareVar(ContextDefinition.class, "def", newInstance(ContextDefinition.class))
                .exprDotMethod(ref("def"), "setContextName", constant(contextName))
                .exprDotMethod(ref("def"), "setControllerFactories", ref("controllers"))
                .exprDotMethod(ref("def"), "setEventTypeContextProperties", EventTypeUtility.resolveTypeCodegen(eventTypeContextProperties, EPStatementInitServices.REF))
                .methodReturn(ref("def"));
        return localMethod(method);
    }

}
