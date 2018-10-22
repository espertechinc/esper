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
package com.espertech.esper.common.internal.context.aifactory.createwindow;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.common.internal.context.activator.ViewableActivatorFilterForge;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowDeployTimeResolver;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.view.core.ViewFactoryForge;
import com.espertech.esper.common.internal.view.core.ViewFactoryForgeUtil;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class StatementAgentInstanceFactoryCreateNWForge {
    private final ViewableActivatorFilterForge activator;
    private final String namedWindowName;
    private final List<ViewFactoryForge> views;
    private final NamedWindowMetaData insertFromNamedWindow;
    private final ExprNode insertFromFilter;
    private final EventType asEventType;
    private final String resultSetProcessorProviderClassName;

    public StatementAgentInstanceFactoryCreateNWForge(ViewableActivatorFilterForge activator, String namedWindowName, List<ViewFactoryForge> views, NamedWindowMetaData insertFromNamedWindow, ExprNode insertFromFilter, EventType asEventType, String resultSetProcessorProviderClassName) {
        this.activator = activator;
        this.namedWindowName = namedWindowName;
        this.views = views;
        this.insertFromNamedWindow = insertFromNamedWindow;
        this.insertFromFilter = insertFromFilter;
        this.asEventType = asEventType;
        this.resultSetProcessorProviderClassName = resultSetProcessorProviderClassName;
    }

    public CodegenMethod initializeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(StatementAgentInstanceFactoryCreateNW.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(StatementAgentInstanceFactoryCreateNW.class, "saiff", newInstance(StatementAgentInstanceFactoryCreateNW.class));

        method.getBlock()
                .exprDotMethod(ref("saiff"), "setActivator", activator.makeCodegen(method, symbols, classScope))
                .exprDotMethod(ref("saiff"), "setNamedWindowName", constant(namedWindowName))
                .exprDotMethod(ref("saiff"), "setViewFactories", ViewFactoryForgeUtil.codegenForgesWInit(views, 0, null, method, symbols, classScope))
                .exprDotMethod(ref("saiff"), "setInsertFromNamedWindow", insertFromNamedWindow == null ? constantNull() : NamedWindowDeployTimeResolver.makeResolveNamedWindow(insertFromNamedWindow, symbols.getAddInitSvc(method)))
                .exprDotMethod(ref("saiff"), "setInsertFromFilter", insertFromFilter == null ? constantNull() : ExprNodeUtilityCodegen.codegenEvaluator(insertFromFilter.getForge(), method, this.getClass(), classScope))
                .exprDotMethod(ref("saiff"), "setAsEventType", asEventType == null ? constantNull() : EventTypeUtility.resolveTypeCodegen(asEventType, EPStatementInitServices.REF))
                .exprDotMethod(ref("saiff"), "setResultSetProcessorFactoryProvider", CodegenExpressionBuilder.newInstance(resultSetProcessorProviderClassName, symbols.getAddInitSvc(method)))
                .exprDotMethod(symbols.getAddInitSvc(method), "addReadyCallback", ref("saiff"));

        method.getBlock().methodReturn(ref("saiff"));
        return method;
    }
}
