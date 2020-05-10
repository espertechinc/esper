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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbolWEventType;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupableForge;

import static com.espertech.esper.common.internal.compile.stage2.FilterSpecCompiler.NEWLINE;

public abstract class FilterSpecParamForge {
    public final static FilterSpecParamForge[] EMPTY_PARAM_ARRAY = new FilterSpecParamForge[0];

    public abstract CodegenMethod makeCodegen(CodegenClassScope classScope, CodegenMethodScope parent, SAIFFInitializeSymbolWEventType symbols);
    public abstract void valueExprToString(StringBuilder out, int indent);

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

    public void appendFilterPlanParam(StringBuilder buf) {
        buf.append("      -lookupable: ").append(lookupable.getExpression()).append(NEWLINE);
        buf.append("      -operator: ").append(filterOperator.getTextualOp()).append(NEWLINE);
        buf.append("      -value-expression: ");
        valueExprToString(buf, 8);
        buf.append(NEWLINE);
    }
}
