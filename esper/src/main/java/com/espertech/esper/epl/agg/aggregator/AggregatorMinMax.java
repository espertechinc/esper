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
package com.espertech.esper.epl.agg.aggregator;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMembersColumnized;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.core.CodegenCtor;
import com.espertech.esper.codegen.model.expression.CodegenExpressionTypePair;
import com.espertech.esper.collection.RefCountedSet;
import com.espertech.esper.collection.SortedRefCountedSet;
import com.espertech.esper.epl.agg.factory.AggregationMethodFactoryMinMax;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.MinMaxTypeEnum;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Min/max aggregator for all values.
 */
public class AggregatorMinMax implements AggregationMethod {
    protected final MinMaxTypeEnum minMaxTypeEnum;

    protected SortedRefCountedSet<Object> refSet;

    /**
     * Ctor.
     *
     * @param minMaxTypeEnum - enum indicating to return minimum or maximum values
     */
    public AggregatorMinMax(MinMaxTypeEnum minMaxTypeEnum) {
        this.minMaxTypeEnum = minMaxTypeEnum;
        this.refSet = new SortedRefCountedSet<Object>();
    }

    public static void rowMemberCodegen(boolean distinct, int column, CodegenCtor ctor, CodegenMembersColumnized membersColumnized) {
        membersColumnized.addMember(column, SortedRefCountedSet.class, "refSet");
        ctor.getBlock().assignRef(refCol("refSet", column), newInstance(SortedRefCountedSet.class));
        if (distinct) {
            membersColumnized.addMember(column, RefCountedSet.class, "distinctSet");
            ctor.getBlock().assignRef(refCol("distinctSet", column), newInstance(RefCountedSet.class));
        }
    }

    public void enter(Object object) {
        if (object == null) {
            return;
        }
        refSet.add(object);
    }

    public static void applyEnterCodegen(AggregationMethodFactoryMinMax forge, int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        CodegenExpressionTypePair value = AggregatorCodegenUtil.prefixWithFilterNullDistinctChecks(true, forge.getParent().isDistinct(), forge.getParent().isHasFilter(), forges, column, method, symbols, classScope);
        method.getBlock().exprDotMethod(refCol("refSet", column), "add", value.getExpression());
    }

    public void leave(Object object) {
        if (object == null) {
            return;
        }
        refSet.remove(object);
    }

    public static void applyLeaveCodegen(AggregationMethodFactoryMinMax forge, int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        CodegenExpressionTypePair value = AggregatorCodegenUtil.prefixWithFilterNullDistinctChecks(false, forge.getParent().isDistinct(), forge.getParent().isHasFilter(), forges, column, method, symbols, classScope);
        method.getBlock().exprDotMethod(refCol("refSet", column), "remove", value.getExpression());
    }

    public void clear() {
        refSet.clear();
    }

    public static void clearCodegen(boolean distinct, int column, CodegenMethodNode method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(refCol("refSet", column), "clear");
        if (distinct) {
            method.getBlock().applyConditional(distinct, block -> block.exprDotMethod(refCol("distinctSet", column), "clear"));
        }
    }

    public Object getValue() {
        if (minMaxTypeEnum == MinMaxTypeEnum.MAX) {
            return refSet.maxValue();
        } else {
            return refSet.minValue();
        }
    }

    public static void getValueCodegen(AggregationMethodFactoryMinMax forge, int column, CodegenMethodNode method) {
        method.getBlock().methodReturn(exprDotMethod(refCol("refSet", column), forge.getParent().getMinMaxTypeEnum() == MinMaxTypeEnum.MAX ? "maxValue" : "minValue"));
    }

    public SortedRefCountedSet<Object> getRefSet() {
        return refSet;
    }
}
