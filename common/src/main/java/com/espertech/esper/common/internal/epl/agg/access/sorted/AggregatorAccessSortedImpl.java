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
import com.espertech.esper.common.internal.bytecodemodel.base.*;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedMethods;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedParam;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenFieldSharableComparator;
import com.espertech.esper.common.internal.collection.HashableMultiKey;
import com.espertech.esper.common.internal.collection.RefCountedSetAtomicInteger;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.agg.access.core.AggregatorAccessWFilterBase;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.serde.CodegenSharableSerdeEventTyped;
import com.espertech.esper.common.internal.serde.DIOSerdeTreeMapEventsMayDeque;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.util.CodegenFieldSharableComparator.CodegenSharableSerdeName.COMPARATORHASHABLEMULTIKEYS;
import static com.espertech.esper.common.internal.epl.agg.method.core.AggregatorCodegenUtil.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.*;
import static com.espertech.esper.common.internal.serde.CodegenSharableSerdeEventTyped.CodegenSharableSerdeName.REFCOUNTEDSETATOMICINTEGER;

public class AggregatorAccessSortedImpl extends AggregatorAccessWFilterBase implements AggregatorAccessSorted {

    protected final AggregationStateSortedForge forge;
    protected final CodegenExpressionRef sorted;
    protected final CodegenExpressionField sortedSerde;
    protected final CodegenExpressionRef size;
    protected final CodegenExpressionField comparator;
    protected final CodegenExpressionRef joinRefs;
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
                return exprDotMethodChain(EPStatementInitServices.REF).add(EPStatementInitServices.GETDATAINPUTOUTPUTSERDEPROVIDER).add("treeMapEventsMayDeque", constant(forge.getSpec().getCriteriaTypes()), type);
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
                .exprDotMethod(sortedSerde, "write", rowDotRef(row, sorted), output, unitKey, writer);
        if (joinRefs != null) {
            method.getBlock().exprDotMethod(joinRefsSerde, "write", rowDotRef(row, joinRefs), output, unitKey, writer);
        }
    }

    public void readCodegen(CodegenExpressionRef row, int col, CodegenExpressionRef input, CodegenMethod method, CodegenExpressionRef unitKey, CodegenClassScope classScope) {
        method.getBlock()
                .apply(readInt(row, size, input))
                .assignRef(rowDotRef(row, sorted), newInstance(TreeMap.class, comparator))
                .exprDotMethod(sortedSerde, "read", rowDotRef(row, sorted), input, unitKey);
        if (joinRefs != null) {
            method.getBlock().assignRef(rowDotRef(row, joinRefs), cast(RefCountedSetAtomicInteger.class, exprDotMethod(joinRefsSerde, "read", input, unitKey)));
        }
    }

    private static CodegenMethod getComparableWMultiKeyCodegen(ExprNode[] criteria, CodegenExpressionRef ref, CodegenNamedMethods namedMethods, CodegenClassScope classScope) {
        String methodName = "getComparable_" + ref.getRef();
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
                .exprDotMethod(ref("state"), "setSize", refCol("size", column))
                .exprDotMethod(ref("state"), "setSorted", refCol("sorted", column))
                .methodReturn(ref("state"));
        return localMethod(method);
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
