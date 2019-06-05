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
import com.espertech.esper.common.internal.compile.stage2.FilterSpecCompiled;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.controller.keyed.ContextControllerDetailKeyedItem;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ContextSpecKeyedItem {

    private final FilterSpecRaw filterSpecRaw;
    private final List<String> propertyNames;
    private final String aliasName;

    private FilterSpecCompiled filterSpecCompiled;
    private EventPropertyGetterSPI[] getters;
    private MultiKeyClassRef keyMultiKey;
    private DataInputOutputSerdeForge[] lookupableSerdes;

    public ContextSpecKeyedItem(FilterSpecRaw filterSpecRaw, List<String> propertyNames, String aliasName) {
        this.filterSpecRaw = filterSpecRaw;
        this.propertyNames = propertyNames;
        this.aliasName = aliasName;
    }

    public FilterSpecRaw getFilterSpecRaw() {
        return filterSpecRaw;
    }

    public List<String> getPropertyNames() {
        return propertyNames;
    }

    public FilterSpecCompiled getFilterSpecCompiled() {
        return filterSpecCompiled;
    }

    public void setFilterSpecCompiled(FilterSpecCompiled filterSpecCompiled) {
        this.filterSpecCompiled = filterSpecCompiled;
    }

    public void setGetters(EventPropertyGetterSPI[] getters) {
        this.getters = getters;
    }

    public void setLookupableSerdes(DataInputOutputSerdeForge[] lookupableSerdes) {
        this.lookupableSerdes = lookupableSerdes;
    }

    public EventPropertyGetterSPI[] getGetters() {
        return getters;
    }

    public MultiKeyClassRef getKeyMultiKey() {
        return keyMultiKey;
    }

    public void setKeyMultiKey(MultiKeyClassRef keyMultiKey) {
        this.keyMultiKey = keyMultiKey;
    }

    public String getAliasName() {
        return aliasName;
    }

    public CodegenExpression makeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(ContextControllerDetailKeyedItem.class, this.getClass(), classScope);
        Class[] types = EventTypeUtility.getPropertyTypes(filterSpecCompiled.getFilterForEventType(), propertyNames.toArray(new String[0]));

        method.getBlock()
            .declareVar(FilterSpecActivatable.class, "activatable", localMethod(filterSpecCompiled.makeCodegen(method, symbols, classScope)))
            .declareVar(ExprFilterSpecLookupable[].class, "lookupables", newArrayByLength(ExprFilterSpecLookupable.class, constant(getters.length)));
        for (int i = 0; i < getters.length; i++) {
            CodegenExpression getter = EventTypeUtility.codegenGetterWCoerceWArray(getters[i], types[i], types[i], method, this.getClass(), classScope);
            CodegenExpression lookupable = newInstance(ExprFilterSpecLookupable.class, constant(propertyNames.get(i)), getter,
                constant(types[i]), constantFalse(), lookupableSerdes[i].codegen(method, classScope, null));
            CodegenExpression eventType = exprDotMethod(ref("activatable"), "getFilterForEventType");
            method.getBlock()
                .assignArrayElement(ref("lookupables"), constant(i), lookupable)
                .expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETFILTERSHAREDLOOKUPABLEREGISTERY).add("registerLookupable", eventType, arrayAtIndex(ref("lookupables"), constant(i))));
        }

        CodegenExpression getter = MultiKeyCodegen.codegenGetterMayMultiKey(filterSpecCompiled.getFilterForEventType(), getters, types, null, keyMultiKey, method, classScope);

        method.getBlock()
            .declareVar(ContextControllerDetailKeyedItem.class, "item", newInstance(ContextControllerDetailKeyedItem.class))
            .exprDotMethod(ref("item"), "setGetter", getter)
            .exprDotMethod(ref("item"), "setLookupables", ref("lookupables"))
            .exprDotMethod(ref("item"), "setPropertyTypes", constant(types))
            .exprDotMethod(ref("item"), "setKeySerde", keyMultiKey.getExprMKSerde(method, classScope))
            .exprDotMethod(ref("item"), "setFilterSpecActivatable", ref("activatable"))
            .exprDotMethod(ref("item"), "setAliasName", constant(aliasName))
            .methodReturn(ref("item"));
        return localMethod(method);
    }
}
