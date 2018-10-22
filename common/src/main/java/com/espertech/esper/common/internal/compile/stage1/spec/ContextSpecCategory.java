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
import com.espertech.esper.common.internal.context.controller.category.ContextControllerDetailCategory;
import com.espertech.esper.common.internal.context.controller.category.ContextControllerDetailCategoryItem;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ContextSpecCategory implements ContextSpec {

    private final List<ContextSpecCategoryItem> items;
    private final FilterSpecRaw filterSpecRaw;

    private transient FilterSpecCompiled filterSpecCompiled;

    public ContextSpecCategory(List<ContextSpecCategoryItem> items, FilterSpecRaw filterSpecRaw) {
        this.items = items;
        this.filterSpecRaw = filterSpecRaw;
    }

    public FilterSpecRaw getFilterSpecRaw() {
        return filterSpecRaw;
    }

    public List<ContextSpecCategoryItem> getItems() {
        return items;
    }

    public void setFilterSpecCompiled(FilterSpecCompiled filterSpec) {
        this.filterSpecCompiled = filterSpec;
    }

    public FilterSpecCompiled getFilterSpecCompiled() {
        return filterSpecCompiled;
    }

    public CodegenExpression makeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(ContextControllerDetailCategory.class, this.getClass(), classScope);

        CodegenMethod makeFilter = filterSpecCompiled.makeCodegen(method, symbols, classScope);
        method.getBlock()
                .declareVar(FilterSpecActivatable.class, "filterSpec", localMethod(makeFilter))
                .declareVar(EventType.class, "eventType", exprDotMethod(ref("filterSpec"), "getFilterForEventType"));

        method.getBlock().declareVar(ContextControllerDetailCategoryItem[].class, "items", newArrayByLength(ContextControllerDetailCategoryItem.class, constant(items.size())));
        for (int i = 0; i < items.size(); i++) {
            method.getBlock().assignArrayElement("items", constant(i), localMethod(items.get(i).makeCodegen(classScope, method), ref("eventType"), symbols.getAddInitSvc(method)));
        }

        method.getBlock()
                .declareVar(ContextControllerDetailCategory.class, "detail", newInstance(ContextControllerDetailCategory.class))
                .exprDotMethod(ref("detail"), "setFilterSpecActivatable", ref("filterSpec"))
                .exprDotMethod(ref("detail"), "setItems", ref("items"))
                .methodReturn(ref("detail"));
        return localMethod(method);
    }
}
