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
package com.espertech.esper.common.internal.filterspec;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbolWEventType;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupableForge;

import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public abstract class FilterSpecParamForge {
    public final static FilterSpecParamForge[] EMPTY_PARAM_ARRAY = new FilterSpecParamForge[0];

    public abstract CodegenMethod makeCodegen(CodegenClassScope classScope, CodegenMethodScope parent, SAIFFInitializeSymbolWEventType symbols);

    /**
     * The property name of the filter parameter.
     */
    protected final ExprFilterSpecLookupableForge lookupable;

    protected final FilterOperator filterOperator;

    FilterSpecParamForge(ExprFilterSpecLookupableForge lookupable, FilterOperator filterOperator) {
        this.lookupable = lookupable;
        this.filterOperator = filterOperator;
    }

    public ExprFilterSpecLookupableForge getLookupable() {
        return lookupable;
    }

    public FilterOperator getFilterOperator() {
        return filterOperator;
    }

    public static FilterSpecParamForge[] toArray(Collection<FilterSpecParamForge> coll) {
        if (coll.isEmpty()) {
            return EMPTY_PARAM_ARRAY;
        }
        return coll.toArray(new FilterSpecParamForge[coll.size()]);
    }

    public static CodegenMethod makeParamArrayArrayCodegen(FilterSpecParamForge[][] forges, CodegenClassScope classScope, CodegenMethod parent) {
        SAIFFInitializeSymbolWEventType symbolsWithType = new SAIFFInitializeSymbolWEventType();
        CodegenMethod method = parent.makeChildWithScope(FilterSpecParam[][].class, FilterSpecParamForge.class, symbolsWithType, classScope).addParam(EventType.class, SAIFFInitializeSymbolWEventType.REF_EVENTTYPE.getRef()).addParam(EPStatementInitServices.class, SAIFFInitializeSymbolWEventType.REF_STMTINITSVC.getRef());
        method.getBlock().declareVar(FilterSpecParam[][].class, "params", newArrayByLength(FilterSpecParam[].class, constant(forges.length)));

        for (int i = 0; i < forges.length; i++) {
            method.getBlock().assignArrayElement("params", constant(i), localMethod(FilterSpecParamForge.makeParamArrayCodegen(forges[i], classScope, method, symbolsWithType)));
        }
        method.getBlock().methodReturn(ref("params"));
        return method;
    }

    private static CodegenMethod makeParamArrayCodegen(FilterSpecParamForge[] forges, CodegenClassScope classScope, CodegenMethod parent, SAIFFInitializeSymbolWEventType symbolsWithType) {
        CodegenMethod method = parent.makeChild(FilterSpecParam[].class, FilterSpecParamForge.class, classScope);
        method.getBlock().declareVar(FilterSpecParam[].class, "items", newArrayByLength(FilterSpecParam.class, constant(forges.length)));
        for (int i = 0; i < forges.length; i++) {
            CodegenMethod makeParam = forges[i].makeCodegen(classScope, method, symbolsWithType);
            method.getBlock().assignArrayElement("items", constant(i), localMethod(makeParam));
        }
        method.getBlock().methodReturn(ref("items"));
        return method;
    }
}
