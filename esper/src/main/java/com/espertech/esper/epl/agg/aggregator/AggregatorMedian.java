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
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.codegen.model.expression.CodegenExpressionTypePair;
import com.espertech.esper.collection.RefCountedSet;
import com.espertech.esper.collection.SortedDoubleVector;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.util.SimpleNumberCoercerFactory;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Median aggregation.
 */
public class AggregatorMedian implements AggregationMethod {
    protected SortedDoubleVector vector;

    public AggregatorMedian() {
        this.vector = new SortedDoubleVector();
    }

    public static void rowMemberCodegen(boolean distinct, int column, CodegenCtor ctor, CodegenMembersColumnized membersColumnized) {
        membersColumnized.addMember(column, SortedDoubleVector.class, "vector");
        ctor.getBlock().assignRef(refCol("vector", column), newInstance(SortedDoubleVector.class));
        if (distinct) {
            membersColumnized.addMember(column, RefCountedSet.class, "distinctSet");
            ctor.getBlock().assignRef(refCol("distinctSet", column), newInstance(RefCountedSet.class));
        }
    }

    public void enter(Object object) {
        if (object == null) {
            return;
        }
        double value = ((Number) object).doubleValue();
        vector.add(value);
    }

    public static void applyEnterCodegen(boolean distinct, boolean hasFilter, int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        CodegenExpressionTypePair value = AggregatorCodegenUtil.prefixWithFilterNullDistinctChecks(true, distinct, hasFilter, forges, column, method, symbols, classScope);

        CodegenExpressionRef vector = refCol("vector", column);

        method.getBlock().exprDotMethod(vector, "add", SimpleNumberCoercerFactory.SimpleNumberCoercerDouble.codegenDouble(value.getExpression(), value.getType()));
    }

    public void leave(Object object) {
        if (object == null) {
            return;
        }
        double value = ((Number) object).doubleValue();
        vector.remove(value);
    }

    public static void applyLeaveCodegen(boolean distinct, boolean hasFilter, int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        CodegenExpressionTypePair value = AggregatorCodegenUtil.prefixWithFilterNullDistinctChecks(false, distinct, hasFilter, forges, column, method, symbols, classScope);

        CodegenExpressionRef vector = refCol("vector", column);

        method.getBlock().exprDotMethod(vector, "remove", SimpleNumberCoercerFactory.SimpleNumberCoercerDouble.codegenDouble(value.getExpression(), value.getType()));
    }

    public void clear() {
        vector.clear();
    }

    public static void clearCodegen(boolean distinct, int column, CodegenMethodNode method) {
        method.getBlock().exprDotMethod(refCol("vector", column), "clear")
                .applyConditional(distinct, block -> block.exprDotMethod(refCol("distinctSet", column), "clear"));
    }

    public Object getValue() {
        return medianCompute(vector);
    }

    public static void getValueCodegen(int column, CodegenMethodNode method) {
        method.getBlock().methodReturn(staticMethod(AggregatorMedian.class, "medianCompute", refCol("vector", column)));
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param vector vector
     * @return value
     */
    public static Object medianCompute(SortedDoubleVector vector) {
        if (vector.size() == 0) {
            return null;
        }
        if (vector.size() == 1) {
            return vector.getValue(0);
        }

        int middle = vector.size() >> 1;
        if (vector.size() % 2 == 0) {
            return (vector.getValue(middle - 1) + vector.getValue(middle)) / 2;
        } else {
            return vector.getValue(middle);
        }
    }
}
