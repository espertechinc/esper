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
package com.espertech.esper.common.internal.epl.agg.access.sorted;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedMethods;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedParam;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionMember;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenFieldSharableComparator;
import com.espertech.esper.common.internal.epl.agg.access.core.AggregatorAccessWFilterBase;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.serde.compiletime.sharable.CodegenSharableSerdeClassArrayTyped;
import com.espertech.esper.common.internal.serde.compiletime.sharable.CodegenSharableSerdeClassTyped;
import com.espertech.esper.common.internal.serde.compiletime.sharable.CodegenSharableSerdeEventTyped;

import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.GT;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.LT;
import static com.espertech.esper.common.internal.bytecodemodel.util.CodegenFieldSharableComparator.CodegenSharableSerdeName.COMPARATOROBJECTARRAYNONHASHABLE;
import static com.espertech.esper.common.internal.epl.agg.method.core.AggregatorCodegenUtil.rowDotMember;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.*;
import static com.espertech.esper.common.internal.serde.compiletime.sharable.CodegenSharableSerdeClassArrayTyped.CodegenSharableSerdeName.OBJECTARRAYMAYNULLNULL;
import static com.espertech.esper.common.internal.serde.compiletime.sharable.CodegenSharableSerdeClassTyped.CodegenSharableSerdeName.VALUE_NULLABLE;
import static com.espertech.esper.common.internal.serde.compiletime.sharable.CodegenSharableSerdeEventTyped.CodegenSharableSerdeName.NULLABLEEVENTMAYCOLLATE;

/**
 * Implementation of access function for single-stream (not joins).
 */
public class AggregatorAccessSortedMinMaxByEver extends AggregatorAccessWFilterBase implements AggregatorAccessSorted {
    private final AggregationStateMinMaxByEverForge forge;
    private final CodegenExpressionMember currentMinMaxBean;
    private final CodegenExpressionField currentMinMaxBeanSerde;
    private final CodegenExpressionMember currentMinMax;
    private final CodegenExpressionField currentMinMaxSerde;
    private final CodegenExpressionField comparator;

    public AggregatorAccessSortedMinMaxByEver(AggregationStateMinMaxByEverForge forge, int col, CodegenCtor ctor, CodegenMemberCol membersColumnized, CodegenClassScope classScope, ExprNode optionalFilter) {
        super(optionalFilter);
        this.forge = forge;
        currentMinMaxBean = membersColumnized.addMember(col, EventBean.class, "currentMinMaxBean");
        currentMinMaxBeanSerde = classScope.addOrGetFieldSharable(new CodegenSharableSerdeEventTyped(NULLABLEEVENTMAYCOLLATE, forge.getSpec().getStreamEventType()));
        currentMinMax = membersColumnized.addMember(col, Object.class, "currentMinMax");
        if (forge.getSpec().getCriteria().length == 1) {
            currentMinMaxSerde = classScope.addOrGetFieldSharable(new CodegenSharableSerdeClassTyped(VALUE_NULLABLE, forge.getSpec().getCriteriaTypes()[0], forge.getSpec().getCriteriaSerdes()[0], classScope));
        } else {
            currentMinMaxSerde = classScope.addOrGetFieldSharable(new CodegenSharableSerdeClassArrayTyped(OBJECTARRAYMAYNULLNULL, forge.getSpec().getCriteriaTypes(), forge.getSpec().getCriteriaSerdes(), classScope));
        }
        comparator = classScope.addOrGetFieldSharable(new CodegenFieldSharableComparator(COMPARATOROBJECTARRAYNONHASHABLE, forge.getSpec().getCriteriaTypes(), forge.getSpec().isSortUsingCollator(), forge.getSpec().getSortDescending()));
    }

    protected void applyEnterFiltered(CodegenMethod method, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        CodegenExpression eps = symbols.getAddEPS(method);
        CodegenExpression ctx = symbols.getAddExprEvalCtx(method);
        method.getBlock().declareVar(EventBean.class, "theEvent", arrayAtIndex(eps, constant(forge.getSpec().getStreamNum())))
            .ifCondition(equalsNull(ref("theEvent"))).blockReturnNoValue()
            .localMethod(addEventCodegen(method, namedMethods, classScope), ref("theEvent"), eps, ctx);
    }

    protected void applyLeaveFiltered(CodegenMethod method, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        // this is an ever-type aggregation
    }

    public void clearCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().assignRef(currentMinMaxBean, constantNull())
            .assignRef(currentMinMax, constantNull());
    }

    public void writeCodegen(CodegenExpressionRef row, int col, CodegenExpressionRef output, CodegenExpressionRef unitKey, CodegenExpressionRef writer, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock()
            .exprDotMethod(currentMinMaxSerde, "write", rowDotMember(row, currentMinMax), output, unitKey, writer)
            .exprDotMethod(currentMinMaxBeanSerde, "write", rowDotMember(row, currentMinMaxBean), output, unitKey, writer);
    }

    public void readCodegen(CodegenExpressionRef row, int col, CodegenExpressionRef input, CodegenMethod method, CodegenExpressionRef unitKey, CodegenClassScope classScope) {
        method.getBlock()
            .assignRef(rowDotMember(row, currentMinMax), cast(Object.class, exprDotMethod(currentMinMaxSerde, "read", input, unitKey)))
            .assignRef(rowDotMember(row, currentMinMaxBean), cast(EventBean.class, exprDotMethod(currentMinMaxBeanSerde, "read", input, unitKey)));
    }

    public CodegenExpression getFirstValueCodegen(CodegenClassScope classScope, CodegenMethod method) {
        if (forge.getSpec().isMax()) {
            method.getBlock().methodThrowUnsupported();
        }
        return currentMinMaxBean;
    }

    public CodegenExpression getLastValueCodegen(CodegenClassScope classScope, CodegenMethod method) {
        if (!forge.getSpec().isMax()) {
            method.getBlock().methodThrowUnsupported();
        }
        return currentMinMaxBean;
    }

    public CodegenExpression sizeCodegen() {
        throw new UnsupportedOperationException("Not supported for this state");
    }

    public CodegenExpression getReverseIteratorCodegen() {
        throw new UnsupportedOperationException("Not supported for this state");
    }

    public CodegenExpression iteratorCodegen() {
        throw new UnsupportedOperationException("Not supported for this state");
    }

    public CodegenExpression collectionReadOnlyCodegen() {
        throw new UnsupportedOperationException("Not supported for this state");
    }

    private CodegenMethod addEventCodegen(CodegenMethod parent, CodegenNamedMethods namedMethods, CodegenClassScope classScope) {
        CodegenMethod comparable = getComparableWObjectArrayKeyCodegen(forge.getSpec().getCriteria(), currentMinMaxBean, namedMethods, classScope);

        CodegenMethod methodNode = parent.makeChild(void.class, this.getClass(), classScope).addParam(EventBean.class, "theEvent").addParam(EventBean[].class, NAME_EPS).addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
        methodNode.getBlock().declareVar(Object.class, "comparable", localMethod(comparable, REF_EPS, constantTrue(), REF_EXPREVALCONTEXT))
            .ifCondition(equalsNull(currentMinMax))
            .assignRef(currentMinMax, ref("comparable"))
            .assignRef(currentMinMaxBean, ref("theEvent"))
            .ifElse()
            .declareVar(int.class, "compareResult", exprDotMethod(comparator, "compare", currentMinMax, ref("comparable")))
            .ifCondition(relational(ref("compareResult"), forge.getSpec().isMax() ? LT : GT, constant(0)))
            .assignRef(currentMinMax, ref("comparable"))
            .assignRef(currentMinMaxBean, ref("theEvent"));
        return methodNode;
    }

    private static CodegenMethod getComparableWObjectArrayKeyCodegen(ExprNode[] criteria, CodegenExpressionMember member, CodegenNamedMethods namedMethods, CodegenClassScope classScope) {
        String methodName = "getComparable_" + member.getRef();
        Consumer<CodegenMethod> code = method -> {
            if (criteria.length == 1) {
                method.getBlock().methodReturn(localMethod(CodegenLegoMethodExpression.codegenExpression(criteria[0].getForge(), method, classScope), REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
            } else {
                ExprForgeCodegenSymbol exprSymbol = new ExprForgeCodegenSymbol(true, null);
                CodegenExpression[] expressions = new CodegenExpression[criteria.length];
                for (int i = 0; i < criteria.length; i++) {
                    expressions[i] = criteria[i].getForge().evaluateCodegen(Object.class, method, exprSymbol, classScope);
                }
                exprSymbol.derivedSymbolsCodegen(method, method.getBlock(), classScope);

                method.getBlock().declareVar(Object[].class, "result", newArrayByLength(Object.class, constant(criteria.length)));
                for (int i = 0; i < criteria.length; i++) {
                    method.getBlock().assignArrayElement(ref("result"), constant(i), expressions[i]);
                }
                method.getBlock().methodReturn(ref("result"));
            }
        };
        return namedMethods.addMethod(Object.class, methodName, CodegenNamedParam.from(EventBean[].class, NAME_EPS, boolean.class, NAME_ISNEWDATA, ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT), AggregatorAccessSortedImpl.class, classScope, code);
    }

    public static CodegenExpression codegenGetAccessTableState(int column, CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(EventBean.class, AggregatorAccessSortedMinMaxByEver.class, classScope);
        method.getBlock().methodReturn(memberCol("currentMinMaxBean", column));
        return localMethod(method);
    }
}
