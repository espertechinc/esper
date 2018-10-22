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
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.controller.hash.ContextControllerDetailHash;
import com.espertech.esper.common.internal.context.controller.hash.ContextControllerDetailHashItem;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ContextSpecHash implements ContextSpec {

    private final List<ContextSpecHashItem> items;
    private final int granularity;
    private final boolean preallocate;

    public ContextSpecHash(List<ContextSpecHashItem> items, int granularity, boolean preallocate) {
        this.items = items;
        this.preallocate = preallocate;
        this.granularity = granularity;
    }

    public List<ContextSpecHashItem> getItems() {
        return items;
    }

    public boolean isPreallocate() {
        return preallocate;
    }

    public int getGranularity() {
        return granularity;
    }

    public CodegenExpression makeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(ContextControllerDetailHash.class, this.getClass(), classScope);

        method.getBlock().declareVar(ContextControllerDetailHashItem[].class, "items", newArrayByLength(ContextControllerDetailHashItem.class, constant(items.size())));
        for (int i = 0; i < items.size(); i++) {
            method.getBlock().assignArrayElement("items", constant(i), items.get(i).makeCodegen(method, symbols, classScope));
        }

        method.getBlock()
                .declareVar(ContextControllerDetailHash.class, "detail", newInstance(ContextControllerDetailHash.class))
                .exprDotMethod(ref("detail"), "setItems", ref("items"))
                .exprDotMethod(ref("detail"), "setGranularity", constant(granularity))
                .exprDotMethod(ref("detail"), "setPreallocate", constant(preallocate))
                .methodReturn(ref("detail"));
        return localMethod(method);
    }
}
