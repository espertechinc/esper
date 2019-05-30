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
import com.espertech.esper.common.client.util.HashableMultiKey;
import com.espertech.esper.common.internal.bytecodemodel.base.*;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedMethods;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedParam;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionMember;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenFieldSharableComparator;
import com.espertech.esper.common.internal.collection.RefCountedSetAtomicInteger;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.agg.access.core.AggregatorAccessWFilterBase;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.serde.compiletime.sharable.CodegenSharableSerdeEventTyped;
import com.espertech.esper.common.internal.serde.serdeset.additional.DIOSerdeTreeMapEventsMayDeque;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.util.CodegenFieldSharableComparator.CodegenSharableSerdeName.COMPARATORHASHABLEMULTIKEYS;
import static com.espertech.esper.common.internal.epl.agg.method.core.AggregatorCodegenUtil.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.*;
import static com.espertech.esper.common.internal.event.path.EventTypeResolver.GETEVENTSERDEFACTORY;
import static com.espertech.esper.common.internal.serde.compiletime.sharable.CodegenSharableSerdeEventTyped.CodegenSharableSerdeName.REFCOUNTEDSETATOMICINTEGER;

public class AggregatorAccessSortedImpl extends AggregatorAccessWFilterBase implements AggregatorAccessSorted {

    protected final AggregationStateSortedForge forge;
    protected final CodegenExpressionMember sorted;
    protected final CodegenExpressionField sortedSerde;
    protected final CodegenExpressionMember size;
    protected final CodegenExpressionField comparator;
    protected final CodegenExpressionMember joinRefs;
    protected final CodegenExpressionField joinRefsSerde;

    public AggregatorAccessSortedImpl(boolean join, AggregationStateSortedForge forge, int col, CodegenCtor ctor, CodegenMemberCol membersColumnized, CodegenClassScope classScope, ExprNode optionalFilter) {
        super(optionalFilter);
        this.forge = forge;
        sorted = membersColumnized.addMember(col, TreeMap.class, "sorted");
        size = membersColumnized.addMember(col, int.class, "size");
        Class[] types = ExprNodeUtilityQuery.getExprResultTypes(forge.getSpec().getCriteria());
        comparator = classScope.addOrGetFieldSharable(new CodegenFieldSharableComparator(COMPARATORHASHABLEMULTIKEYS, types, forge.getSpec().isSortUsingCollator(), forge.getSpec().getSortDescending()));
        ctor.getBlock().assignRef(sorted, newInstance(TreeMap.class, comparator));

        sortedSerde = classScope.addOrGetFieldSharable(new CodegenFieldSharable() {
            public Class type() {
                return DIOSerdeTreeMapEventsMayDeque.class;
            }

            public CodegenExpression initCtorScoped() {
                CodegenExpression type = EventTypeUtility.resolveTypeCodegen(forge.getSpec().getStreamEventType(), EPStatementInitServices.REF);
                CodegenExpression criteriaSerdes = DataInputOutputSerdeForge.codegenArray(forge.getSpec().getCriteriaSerdes(), classScope.getPackageScope().getInitMethod(), classScope, null);
                return exprDotMethodChain(EPStatementInitServices.REF).add(EPStatementInitServices.GETEVENTTYPERESOLVER).add(GETEVENTSERDEFACTORY).add("treeMapEventsMayDeque", criteriaSerdes, type);
            }
        });

        if (join) {
            joinRefs = membersColumnized.addMember(col, RefCountedSetAtomicInteger.class, "refs");
            ctor.getBlock().assignRef(joinRefs, newInstance(RefCountedSetAtomicInteger.class));
            joinRefsSerde = classScope.addOrGetFieldSharable(new CodegenSharableSerdeEventTyped(REFCOUNTEDSETATOMICINTEGER, forge.getSpec().getStreamEventType()));
        } else {
            joinRefs = null;
            joinRefsSerde = null;
        }
    }

    protected void applyEnterFiltered(CodegenMethod method, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        CodegenExpressionRef eps = symbols.getAddEPS(method);
        CodegenExpressionRef ctx = symbols.getAddExprEvalCtx(method);
        CodegenMethod referenceAddToColl = referenceAddToCollCodegen(method, namedMethods, classScope);
        method.getBlock().declareVar(EventBean.class, "theEvent", arrayAtIndex(eps, constant(forge.getSpec().getStreamNum())))
            .ifRefNull("theEvent").blockReturnNoValue();

        if (joinRefs == null) {
            method.getBlock().localMethod(referenceAddToColl, ref("theEvent"), eps, ctx);
        } else {
            method.getBlock().ifCondition(exprDotMethod(joinRefs, "add", ref("theEvent")))
                .localMethod(referenceAddToColl, ref("theEvent"), eps, ctx);
        }
    }

    protected void applyLeaveFiltered(CodegenMethod method, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        CodegenExpressionRef eps = symbols.getAddEPS(method);
        CodegenExpressionRef ctx = symbols.getAddExprEvalCtx(method);
        CodegenMethod dereferenceRemove = dereferenceRemoveFromCollCodegen(method, namedMethods, classScope);
        method.getBlock().declareVar(EventBean.class, "theEvent", arrayAtIndex(eps, constant(forge.getSpec().getStreamNum())))
            .ifRefNull("theEvent").blockReturnNoValue();

        if (joinRefs == null) {
            method.getBlock().localMethod(dereferenceRemove, ref("theEvent"), eps, ctx);
        } else {
            method.getBlock().ifCondition(exprDotMethod(joinRefs, "remove", ref("theEvent")))
                .localMethod(dereferenceRemove, ref("theEvent"), eps, ctx);
        }
    }

    public void clearCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(sorted, "clear")
            .assignRef(size, constant(0));
        if (joinRefs != null) {
            method.getBlock().exprDotMethod(joinRefs, "clear");
        }
    }

    public CodegenExpression getFirstValueCodegen(CodegenClassScope classScope, CodegenMethod parent) {
        CodegenMethod method = parent.makeChildWithScope(EventBean.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
        method.getBlock().ifCondition(exprDotMethod(sorted, "isEmpty"))
            .blockReturn(constantNull())
            .declareVar(Map.Entry.class, "max", exprDotMethod(sorted, "firstEntry"))
            .methodReturn(staticMethod(AggregatorAccessSortedImpl.class, "checkedPayloadMayDeque", exprDotMethod(ref("max"), "getValue")));
        return localMethod(method);
    }

    public CodegenExpression getLastValueCodegen(CodegenClassScope classScope, CodegenMethod parent) {
        CodegenMethod method = parent.makeChildWithScope(EventBean.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
        method.getBlock().ifCondition(exprDotMethod(sorted, "isEmpty"))
            .blockReturn(constantNull())
            .declareVar(Map.Entry.class, "min", exprDotMethod(sorted, "lastEntry"))
            .methodReturn(staticMethod(AggregatorAccessSortedImpl.class, "checkedPayloadMayDeque", exprDotMethod(ref("min"), "getValue")));
        return localMethod(method);
    }

    public CodegenExpression iteratorCodegen() {
        return newInstance(AggregationStateSortedIterator.class, sorted, constantFalse());
    }

    public CodegenExpression getReverseIteratorCodegen() {
        return newInstance(AggregationStateSortedIterator.class, sorted, constantTrue());
    }

    public CodegenExpression collectionReadOnlyCodegen() {
        return newInstance(AggregationStateSortedWrappingCollection.class, sorted, size);
    }

    public CodegenExpression sizeCodegen() {
        return size;
    }

    public void writeCodegen(CodegenExpressionRef row, int col, CodegenExpressionRef output, CodegenExpressionRef unitKey, CodegenExpressionRef writer, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock()
            .apply(writeInt(output, row, size))
            .exprDotMethod(sortedSerde, "write", rowDotMember(row, sorted), output, unitKey, writer);
        if (joinRefs != null) {
            method.getBlock().exprDotMethod(joinRefsSerde, "write", rowDotMember(row, joinRefs), output, unitKey, writer);
        }
    }

    public void readCodegen(CodegenExpressionRef row, int col, CodegenExpressionRef input, CodegenMethod method, CodegenExpressionRef unitKey, CodegenClassScope classScope) {
        method.getBlock()
            .apply(readInt(row, size, input))
            .assignRef(rowDotMember(row, sorted), newInstance(TreeMap.class, comparator))
            .exprDotMethod(sortedSerde, "read", rowDotMember(row, sorted), input, unitKey);
        if (joinRefs != null) {
            method.getBlock().assignRef(rowDotMember(row, joinRefs), cast(RefCountedSetAtomicInteger.class, exprDotMethod(joinRefsSerde, "read", input, unitKey)));
        }
    }

    private static CodegenMethod getComparableWMultiKeyCodegen(ExprNode[] criteria, CodegenExpressionMember member, CodegenNamedMethods namedMethods, CodegenClassScope classScope) {
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
                method.getBlock().methodReturn(newInstance(HashableMultiKey.class, ref("result")));
            }
        };
        return namedMethods.addMethod(Object.class, methodName, CodegenNamedParam.from(EventBean[].class, NAME_EPS, boolean.class, NAME_ISNEWDATA, ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT), AggregatorAccessSortedImpl.class, classScope, code);
    }

    private CodegenMethod referenceAddToCollCodegen(CodegenMethod parent, CodegenNamedMethods namedMethods, CodegenClassScope classScope) {
        CodegenMethod getComparable = getComparableWMultiKeyCodegen(forge.getSpec().getCriteria(), sorted, namedMethods, classScope);

        CodegenMethod method = parent.makeChildWithScope(void.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(EventBean.class, "theEvent").addParam(EventBean[].class, NAME_EPS).addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
        method.getBlock().declareVar(Object.class, "comparable", localMethod(getComparable, REF_EPS, constantTrue(), REF_EXPREVALCONTEXT))
            .declareVar(Object.class, "existing", exprDotMethod(sorted, "get", ref("comparable")))
            .ifRefNull("existing")
            .exprDotMethod(sorted, "put", ref("comparable"), ref("theEvent"))
            .ifElseIf(instanceOf(ref("existing"), EventBean.class))
            .declareVar(ArrayDeque.class, "coll", newInstance(ArrayDeque.class, constant(2)))
            .exprDotMethod(ref("coll"), "add", ref("existing"))
            .exprDotMethod(ref("coll"), "add", ref("theEvent"))
            .exprDotMethod(sorted, "put", ref("comparable"), ref("coll"))
            .ifElse()
            .declareVar(ArrayDeque.class, "q", cast(ArrayDeque.class, ref("existing")))
            .exprDotMethod(ref("q"), "add", ref("theEvent"))
            .blockEnd()
            .increment(size);

        return method;
    }

    private CodegenMethod dereferenceRemoveFromCollCodegen(CodegenMethod parent, CodegenNamedMethods namedMethods, CodegenClassScope classScope) {
        CodegenMethod getComparable = getComparableWMultiKeyCodegen(forge.getSpec().getCriteria(), sorted, namedMethods, classScope);

        CodegenMethod method = parent.makeChildWithScope(void.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(EventBean.class, "theEvent").addParam(EventBean[].class, NAME_EPS).addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
        method.getBlock().declareVar(Object.class, "comparable", localMethod(getComparable, REF_EPS, constantTrue(), REF_EXPREVALCONTEXT))
            .declareVar(Object.class, "existing", exprDotMethod(sorted, "get", ref("comparable")))
            .ifRefNull("existing").blockReturnNoValue()
            .ifCondition(exprDotMethod(ref("existing"), "equals", ref("theEvent")))
            .exprDotMethod(sorted, "remove", ref("comparable"))
            .decrement(size)
            .ifElseIf(instanceOf(ref("existing"), ArrayDeque.class))
            .declareVar(ArrayDeque.class, "q", cast(ArrayDeque.class, ref("existing")))
            .exprDotMethod(ref("q"), "remove", ref("theEvent"))
            .ifCondition(exprDotMethod(ref("q"), "isEmpty"))
            .exprDotMethod(sorted, "remove", ref("comparable"))
            .blockEnd()
            .decrement(size);

        return method;
    }

    public static CodegenExpression codegenGetAccessTableState(int column, CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(AggregationStateSorted.class, AggregatorAccessSortedImpl.class, classScope);
        method.getBlock()
            .declareVar(AggregationStateSorted.class, "state", newInstance(AggregationStateSorted.class))
            .exprDotMethod(ref("state"), "setSize", memberCol("size", column))
            .exprDotMethod(ref("state"), "setSorted", memberCol("sorted", column))
            .methodReturn(ref("state"));
        return localMethod(method);
    }

    public static void checkedPayloadAddAll(ArrayDeque<EventBean> events, Object value) {
        if (value instanceof EventBean) {
            events.add((EventBean) value);
            return;
        }
        ArrayDeque<EventBean> q = (ArrayDeque<EventBean>) value;
        events.addAll(q);
    }

    public static Object checkedPayloadGetUnderlyingArray(Object value, Class underlyingClass) {
        if (value instanceof EventBean) {
            Object array = Array.newInstance(underlyingClass, 1);
            Array.set(array, 0, ((EventBean) value).getUnderlying());
            return array;
        }
        ArrayDeque<EventBean> q = (ArrayDeque<EventBean>) value;
        Object array = Array.newInstance(underlyingClass, q.size());
        int index = 0;
        for (EventBean event : q) {
            Array.set(array, index++, event.getUnderlying());
        }
        return array;
    }

    public static Collection<EventBean> checkedPayloadGetCollEvents(Object value) {
        if (value instanceof EventBean) {
            return Collections.singletonList((EventBean) value);
        }
        return (Collection<EventBean>) value;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param value payload to check
     * @return bean
     */
    public final static EventBean checkedPayloadMayDeque(Object value) {
        if (value instanceof EventBean) {
            return (EventBean) value;
        }
        ArrayDeque<EventBean> q = (ArrayDeque<EventBean>) value;
        return q.getFirst();
    }
}
