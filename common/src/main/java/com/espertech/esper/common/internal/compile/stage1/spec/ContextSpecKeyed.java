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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyCodegen;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.controller.condition.ContextConditionDescriptorFilter;
import com.espertech.esper.common.internal.context.controller.keyed.ContextControllerDetailKeyed;
import com.espertech.esper.common.internal.context.controller.keyed.ContextControllerDetailKeyedItem;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ContextSpecKeyed implements ContextSpec {

    private final List<ContextSpecKeyedItem> items;
    private List<ContextSpecConditionFilter> optionalInit;
    private ContextSpecCondition optionalTermination;
    private MultiKeyClassRef multiKeyClassRef;

    public ContextSpecKeyed(List<ContextSpecKeyedItem> items, List<ContextSpecConditionFilter> optionalInit, ContextSpecCondition optionalTermination) {
        this.items = items;
        this.optionalInit = optionalInit;
        this.optionalTermination = optionalTermination;
    }

    public List<ContextSpecKeyedItem> getItems() {
        return items;
    }

    public ContextSpecCondition getOptionalTermination() {
        return optionalTermination;
    }

    public void setOptionalTermination(ContextSpecCondition optionalTermination) {
        this.optionalTermination = optionalTermination;
    }

    public void setMultiKeyClassRef(MultiKeyClassRef multiKeyClassRef) {
        this.multiKeyClassRef = multiKeyClassRef;
    }

    public List<ContextSpecConditionFilter> getOptionalInit() {
        return optionalInit;
    }

    public CodegenExpression makeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(ContextControllerDetailKeyed.class, this.getClass(), classScope);

        method.getBlock().declareVar(ContextControllerDetailKeyedItem[].class, "items", newArrayByLength(ContextControllerDetailKeyedItem.class, constant(items.size())));
        for (int i = 0; i < items.size(); i++) {
            method.getBlock().assignArrayElement("items", constant(i), items.get(i).makeCodegen(method, symbols, classScope));
        }

        method.getBlock()
                .declareVar(ContextControllerDetailKeyed.class, "detail", newInstance(ContextControllerDetailKeyed.class))
                .exprDotMethod(ref("detail"), "setItems", ref("items"))
                .exprDotMethod(ref("detail"), "setMultiKeyFromObjectArray", MultiKeyCodegen.codegenMultiKeyFromArrayTransform(multiKeyClassRef, method, classScope));

        if (optionalInit != null && !optionalInit.isEmpty()) {
            method.getBlock().declareVar(ContextConditionDescriptorFilter[].class, "init", newArrayByLength(ContextConditionDescriptorFilter.class, constant(optionalInit.size())));
            for (int i = 0; i < optionalInit.size(); i++) {
                method.getBlock().assignArrayElement("init", constant(i), cast(ContextConditionDescriptorFilter.class, optionalInit.get(i).make(method, symbols, classScope)));
            }
            method.getBlock().exprDotMethod(ref("detail"), "setOptionalInit", ref("init"));
        }

        if (optionalTermination != null) {
            method.getBlock().exprDotMethod(ref("detail"), "setOptionalTermination", optionalTermination.make(method, symbols, classScope));
        }

        method.getBlock().expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add("addReadyCallback", ref("detail")))
                .methodReturn(ref("detail"));
        return localMethod(method);
    }
}
