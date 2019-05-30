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
package com.espertech.esper.common.internal.epl.agg.method.core;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionMember;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.collection.RefCountedSet;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlanner;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.serde.compiletime.sharable.CodegenSharableSerdeClassTyped;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.agg.method.core.AggregatorCodegenUtil.rowDotMember;

public abstract class AggregatorMethodWDistinctWFilterBase implements AggregatorMethod {
    protected final CodegenExpressionMember distinct;
    protected final Class optionalDistinctValueType;
    protected final boolean hasFilter; // this flag can be true and "optionalFilter" can still be null when declaring a table column
    protected final ExprNode optionalFilter;
    private final CodegenExpressionField distinctSerde;

    protected abstract void applyEvalEnterFiltered(CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope);

    protected abstract void applyEvalLeaveFiltered(CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope);

    protected abstract void applyTableEnterFiltered(CodegenExpressionRef value, Class[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope);

    protected abstract void applyTableLeaveFiltered(CodegenExpressionRef value, Class[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope);

    protected abstract void clearWODistinct(CodegenMethod method, CodegenClassScope classScope);

    protected abstract void writeWODistinct(CodegenExpressionRef row, int col, CodegenExpressionRef output, CodegenExpressionRef unitKey, CodegenExpressionRef writer, CodegenMethod method, CodegenClassScope classScope);

    protected abstract void readWODistinct(CodegenExpressionRef row, int col, CodegenExpressionRef input, CodegenExpressionRef unitKey, CodegenMethod method, CodegenClassScope classScope);

    public AggregatorMethodWDistinctWFilterBase(AggregationForgeFactory factory,
                                                int col,
                                                CodegenCtor rowCtor,
                                                CodegenMemberCol membersColumnized,
                                                CodegenClassScope classScope,
                                                Class optionalDistinctValueType,
                                                DataInputOutputSerdeForge optionalDistinctSerde,
                                                boolean hasFilter,
                                                ExprNode optionalFilter) {
        this.optionalDistinctValueType = optionalDistinctValueType;
        this.optionalFilter = optionalFilter;
        this.hasFilter = hasFilter;

        if (optionalDistinctValueType != null) {
            distinct = membersColumnized.addMember(col, RefCountedSet.class, "distinctSet");
            rowCtor.getBlock().assignRef(distinct, newInstance(RefCountedSet.class));
            distinctSerde = classScope.addOrGetFieldSharable(new CodegenSharableSerdeClassTyped(CodegenSharableSerdeClassTyped.CodegenSharableSerdeName.REFCOUNTEDSET, optionalDistinctValueType, optionalDistinctSerde, classScope));
        } else {
            distinct = null;
            distinctSerde = null;
        }
    }

    public final void applyEvalEnterCodegen(CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        if (optionalFilter != null) {
            AggregatorCodegenUtil.prefixWithFilterCheck(optionalFilter.getForge(), method, symbols, classScope);
        }
        applyEvalEnterFiltered(method, symbols, forges, classScope);
    }

    public final void applyTableEnterCodegen(CodegenExpressionRef value, Class[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        if (hasFilter) {
            method.getBlock()
                .declareVar(Object[].class, "in", cast(Object[].class, value))
                .declareVar(boolean.class, "pass", cast(Boolean.class, arrayAtIndex(ref("in"), constant(1))))
                .ifCondition(not(ref("pass"))).blockReturnNoValue()
                .declareVar(Object.class, "filtered", arrayAtIndex(ref("in"), constant(0)));
            applyTableEnterFiltered(ref("filtered"), evaluationTypes, method, classScope);
        } else {
            applyTableEnterFiltered(value, evaluationTypes, method, classScope);
        }
    }

    public void applyEvalLeaveCodegen(CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        if (optionalFilter != null) {
            AggregatorCodegenUtil.prefixWithFilterCheck(optionalFilter.getForge(), method, symbols, classScope);
        }
        applyEvalLeaveFiltered(method, symbols, forges, classScope);
    }

    public void applyTableLeaveCodegen(CodegenExpressionRef value, Class[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        if (hasFilter) {
            method.getBlock()
                .declareVar(Object[].class, "in", cast(Object[].class, value))
                .declareVar(boolean.class, "pass", cast(Boolean.class, arrayAtIndex(ref("in"), constant(1))))
                .ifCondition(not(ref("pass"))).blockReturnNoValue()
                .declareVar(Object.class, "filtered", arrayAtIndex(ref("in"), constant(0)));
            applyTableLeaveFiltered(ref("filtered"), evaluationTypes, method, classScope);
        } else {
            applyTableLeaveFiltered(value, evaluationTypes, method, classScope);
        }
    }

    public final void clearCodegen(CodegenMethod method, CodegenClassScope classScope) {
        if (distinct != null) {
            method.getBlock().exprDotMethod(distinct, "clear");
        }
        clearWODistinct(method, classScope);
    }

    public final void writeCodegen(CodegenExpressionRef row, int col, CodegenExpressionRef output, CodegenExpressionRef unitKey, CodegenExpressionRef writer, CodegenMethod method, CodegenClassScope classScope) {
        if (distinct != null) {
            method.getBlock().exprDotMethod(distinctSerde, "write", rowDotMember(row, distinct), output, unitKey, writer);
        }
        writeWODistinct(row, col, output, unitKey, writer, method, classScope);
    }

    public final void readCodegen(CodegenExpressionRef row, int col, CodegenExpressionRef input, CodegenExpressionRef unitKey, CodegenMethod method, CodegenClassScope classScope) {
        if (distinct != null) {
            method.getBlock().assignRef(rowDotMember(row, distinct), cast(RefCountedSet.class, exprDotMethod(distinctSerde, "read", input, unitKey)));
        }
        readWODistinct(row, col, input, unitKey, method, classScope);
    }

    protected CodegenExpression toDistinctValueKey(CodegenExpression distinctValue) {
        if (!optionalDistinctValueType.isArray()) {
            return distinctValue;
        }
        Class mktype = MultiKeyPlanner.getMKClassForComponentType(optionalDistinctValueType.getComponentType());
        return newInstance(mktype, cast(optionalDistinctValueType, distinctValue));
    }
}
