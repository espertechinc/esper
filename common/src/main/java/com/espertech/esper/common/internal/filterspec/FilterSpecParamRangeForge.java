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

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbolWEventType;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupableForge;

import java.util.Arrays;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * This class represents a range filter parameter in an {@link FilterSpecActivatable} filter specification.
 */
public final class FilterSpecParamRangeForge extends FilterSpecParamForge {
    private final FilterSpecParamFilterForEvalForge min;
    private final FilterSpecParamFilterForEvalForge max;

    /**
     * Constructor.
     *
     * @param lookupable     is the lookupable
     * @param filterOperator is the type of range operator
     * @param min            is the begin point of the range
     * @param max            is the end point of the range
     * @throws IllegalArgumentException if an operator was supplied that does not take a double range value
     */
    public FilterSpecParamRangeForge(ExprFilterSpecLookupableForge lookupable, FilterOperator filterOperator, FilterSpecParamFilterForEvalForge min, FilterSpecParamFilterForEvalForge max)
            throws IllegalArgumentException {
        super(lookupable, filterOperator);
        this.min = min;
        this.max = max;

        if (!(filterOperator.isRangeOperator()) && (!(filterOperator.isInvertedRangeOperator()))) {
            throw new IllegalArgumentException("Illegal filter operator " + filterOperator + " supplied to " +
                    "range filter parameter");
        }
    }

    public final String toString() {
        return super.toString() + "  range=(min=" + min.toString() + ",max=" + max.toString() + ')';
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof FilterSpecParamRangeForge)) {
            return false;
        }

        FilterSpecParamRangeForge other = (FilterSpecParamRangeForge) obj;
        if (!super.equals(other)) {
            return false;
        }

        return this.min.equals(other.min) &&
                (this.max.equals(other.max));
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (min != null ? min.hashCode() : 0);
        result = 31 * result + (max != null ? max.hashCode() : 0);
        return result;
    }

    public CodegenMethod makeCodegen(CodegenClassScope classScope, CodegenMethodScope parent, SAIFFInitializeSymbolWEventType symbols) {
        CodegenMethod method = parent.makeChild(FilterSpecParam.EPTYPE, FilterSpecParamConstantForge.class, classScope);
        method.getBlock()
                .declareVar(ExprFilterSpecLookupable.EPTYPE, "lookupable", localMethod(lookupable.makeCodegen(method, symbols, classScope)))
                .declareVar(ExprFilterSpecLookupable.EPTYPE_FILTEROPERATOR, "op", enumValue(FilterOperator.class, filterOperator.name()));

        CodegenExpressionNewAnonymousClass param = newAnonymousClass(method.getBlock(), FilterSpecParam.EPTYPE, Arrays.asList(ref("lookupable"), ref("op")));
        CodegenMethod getFilterValue = CodegenMethod.makeParentNode(FilterValueSetParam.EPTYPE, this.getClass(), classScope).addParam(FilterSpecParam.GET_FILTER_VALUE_FP);
        param.addMethod("getFilterValue", getFilterValue);

        EPTypeClass returnType = DoubleRange.EPTYPE;
        EPTypeClass castType = EPTypePremade.DOUBLEBOXED.getEPType();
        if (lookupable.getReturnType().getType() == String.class) {
            castType = EPTypePremade.STRING.getEPType();
            returnType = StringRange.EPTYPE;
        }
        getFilterValue.getBlock()
                .declareVar(EPTypePremade.OBJECT.getEPType(), "min", min.makeCodegen(classScope, method))
                .declareVar(EPTypePremade.OBJECT.getEPType(), "max", max.makeCodegen(classScope, method))
                .declareVar(EPTypePremade.OBJECT.getEPType(), "value", newInstance(returnType, cast(castType, ref("min")), cast(castType, ref("max"))))
                .methodReturn(FilterValueSetParamImpl.codegenNew(ref("value")));

        method.getBlock().methodReturn(param);
        return method;
    }

    public void valueExprToString(StringBuilder out, int i) {
        out.append("lower ");
        min.valueToString(out);
        out.append(" upper ");
        max.valueToString(out);
    }
}
