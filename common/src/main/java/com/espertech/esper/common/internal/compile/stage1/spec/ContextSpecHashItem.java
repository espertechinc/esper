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
package com.espertech.esper.common.internal.compile.stage1.spec;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecCompiled;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbolWEventType;
import com.espertech.esper.common.internal.context.controller.hash.ContextControllerDetailHashItem;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprChainedSpec;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupableForge;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ContextSpecHashItem {

    private final ExprChainedSpec function;
    private final FilterSpecRaw filterSpecRaw;

    private FilterSpecCompiled filterSpecCompiled;
    private ExprFilterSpecLookupableForge lookupable;

    public ContextSpecHashItem(ExprChainedSpec function, FilterSpecRaw filterSpecRaw) {
        this.function = function;
        this.filterSpecRaw = filterSpecRaw;
    }

    public ExprChainedSpec getFunction() {
        return function;
    }

    public FilterSpecRaw getFilterSpecRaw() {
        return filterSpecRaw;
    }

    public FilterSpecCompiled getFilterSpecCompiled() {
        return filterSpecCompiled;
    }

    public void setFilterSpecCompiled(FilterSpecCompiled filterSpecCompiled) {
        this.filterSpecCompiled = filterSpecCompiled;
    }

    public ExprFilterSpecLookupableForge getLookupable() {
        return lookupable;
    }

    public void setLookupable(ExprFilterSpecLookupableForge lookupable) {
        this.lookupable = lookupable;
    }

    public CodegenExpression makeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(ContextControllerDetailHashItem.class, this.getClass(), classScope);
        method.getBlock().declareVar(EventType.class, "eventType", EventTypeUtility.resolveTypeCodegen(filterSpecCompiled.getFilterForEventType(), symbols.getAddInitSvc(method)));

        SAIFFInitializeSymbolWEventType symbolsWithType = new SAIFFInitializeSymbolWEventType();
        CodegenMethod methodLookupableMake = parent.makeChildWithScope(ExprFilterSpecLookupable.class, this.getClass(), symbolsWithType, classScope).addParam(EventType.class, "eventType").addParam(EPStatementInitServices.class, SAIFFInitializeSymbolWEventType.REF_STMTINITSVC.getRef());
        CodegenMethod methodLookupable = lookupable.makeCodegen(methodLookupableMake, symbolsWithType, classScope);
        methodLookupableMake.getBlock().methodReturn(localMethod(methodLookupable));

        method.getBlock()
                .declareVar(ContextControllerDetailHashItem.class, "item", newInstance(ContextControllerDetailHashItem.class))
                .declareVar(ExprFilterSpecLookupable.class, "lookupable", localMethod(methodLookupableMake, ref("eventType"), symbols.getAddInitSvc(method)))
                .exprDotMethod(ref("item"), "setFilterSpecActivatable", localMethod(filterSpecCompiled.makeCodegen(method, symbols, classScope)))
                .exprDotMethod(ref("item"), "setLookupable", ref("lookupable"))
                .expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETFILTERSHAREDLOOKUPABLEREGISTERY).add("registerLookupable", ref("eventType"), ref("lookupable")))
                .methodReturn(ref("item"));

        return localMethod(method);
    }
}
