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
package com.espertech.esper.common.internal.context.aifactory.update;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.subselect.SubSelectFactoryForge;

import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class StatementAgentInstanceFactoryUpdateForge {

    private final InternalEventRouterDescForge forge;
    private final Map<ExprSubselectNode, SubSelectFactoryForge> subselects;

    public StatementAgentInstanceFactoryUpdateForge(InternalEventRouterDescForge forge, Map<ExprSubselectNode, SubSelectFactoryForge> subselects) {
        this.forge = forge;
        this.subselects = subselects;
    }

    public CodegenMethod initializeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(StatementAgentInstanceFactoryUpdate.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(StatementAgentInstanceFactoryUpdate.class, "saiff", newInstance(StatementAgentInstanceFactoryUpdate.class))
                .exprDotMethod(ref("saiff"), "setDesc", forge.make(method, symbols, classScope))
                .exprDotMethod(ref("saiff"), "setSubselects", SubSelectFactoryForge.codegenInitMap(subselects, this.getClass(), method, symbols, classScope))
                .expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add("addReadyCallback", ref("saiff")))
                .methodReturn(ref("saiff"));
        return method;
    }
}
