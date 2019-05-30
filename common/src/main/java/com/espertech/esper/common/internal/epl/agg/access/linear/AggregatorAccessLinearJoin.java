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
package com.espertech.esper.common.internal.epl.agg.access.linear;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenSymbolProviderEmpty;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedMethods;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionMember;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.collection.ArrayEventIterator;
import com.espertech.esper.common.internal.epl.agg.access.core.AggregatorAccessWFilterBase;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.serde.compiletime.sharable.CodegenSharableSerdeEventTyped;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.*;
import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.GE;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.LT;
import static com.espertech.esper.common.internal.epl.agg.method.core.AggregatorCodegenUtil.rowDotMember;
import static com.espertech.esper.common.internal.serde.compiletime.sharable.CodegenSharableSerdeEventTyped.CodegenSharableSerdeName.LINKEDHASHMAPEVENTSANDINT;
import static com.espertech.esper.common.internal.util.CollectionUtil.METHOD_TOARRAYEVENTS;

/**
 * Implementation of access function for joins.
 */
public class AggregatorAccessLinearJoin extends AggregatorAccessWFilterBase implements AggregatorAccessLinear {

    private final AggregationStateLinearForge forge;
    private final CodegenExpressionMember refSet;
    private final CodegenExpressionMember array;

    public AggregatorAccessLinearJoin(AggregationStateLinearForge forge, int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope, ExprNode optionalFilter) {
        super(optionalFilter);
        this.forge = forge;
        refSet = membersColumnized.addMember(col, LinkedHashMap.class, "refSet");
        array = membersColumnized.addMember(col, EventBean[].class, "array");
        rowCtor.getBlock().assignRef(refSet, newInstance(LinkedHashMap.class));
    }

    protected void applyEnterFiltered(CodegenMethod method, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        CodegenExpression eps = symbols.getAddEPS(method);
        CodegenMethod addEvent = addEventCodegen(method, classScope);
        method.getBlock().declareVar(EventBean.class, "theEvent", arrayAtIndex(eps, constant(forge.getStreamNum())))
                .ifRefNull("theEvent").blockReturnNoValue()
                .localMethod(addEvent, ref("theEvent"));
    }

    protected void applyLeaveFiltered(CodegenMethod method, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        CodegenExpression eps = symbols.getAddEPS(method);
        CodegenMethod removeEvent = removeEventCodegen(method, classScope);
        method.getBlock().declareVar(EventBean.class, "theEvent", arrayAtIndex(eps, constant(forge.getStreamNum())))
                .ifRefNull("theEvent").blockReturnNoValue()
                .localMethod(removeEvent, ref("theEvent"));
    }

    public void clearCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(refSet, "clear")
                .assignRef(array, constantNull());
    }

    public void writeCodegen(CodegenExpressionRef row, int col, CodegenExpressionRef output, CodegenExpressionRef unitKey, CodegenExpressionRef writer, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(getSerde(classScope), "write", rowDotMember(row, refSet), output, unitKey, writer);
    }

    public void readCodegen(CodegenExpressionRef row, int col, CodegenExpressionRef input, CodegenMethod method, CodegenExpressionRef unitKey, CodegenClassScope classScope) {
        method.getBlock().assignRef(rowDotMember(row, refSet), cast(LinkedHashMap.class, exprDotMethod(getSerde(classScope), "read", input, unitKey)));
    }

    public CodegenExpression getFirstNthValueCodegen(CodegenExpressionRef index, CodegenMethod parentMethod, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        CodegenMethod initArray = initArrayCodegen(namedMethods, classScope);
        CodegenMethod method = parentMethod.makeChildWithScope(EventBean.class, AggregatorAccessLinearJoin.class, CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(int.class, "index");
        method.getBlock().ifCondition(relational(ref("index"), LT, constant(0))).blockReturn(constantNull())
                .ifCondition(exprDotMethod(refSet, "isEmpty")).blockReturn(constantNull())
                .ifCondition(relational(ref("index"), GE, exprDotMethod(refSet, "size"))).blockReturn(constantNull())
                .ifCondition(equalsNull(array)).localMethod(initArray).blockEnd()
                .methodReturn(arrayAtIndex(array, ref("index")));
        return localMethod(method, index);
    }

    public CodegenExpression getLastNthValueCodegen(CodegenExpressionRef index, CodegenMethod parentMethod, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        CodegenMethod initArray = initArrayCodegen(namedMethods, classScope);
        CodegenMethod method = parentMethod.makeChildWithScope(EventBean.class, AggregatorAccessLinearJoin.class, CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(int.class, "index");
        method.getBlock().ifCondition(relational(ref("index"), LT, constant(0))).blockReturn(constantNull())
                .ifCondition(exprDotMethod(refSet, "isEmpty")).blockReturn(constantNull())
                .ifCondition(relational(ref("index"), GE, exprDotMethod(refSet, "size"))).blockReturn(constantNull())
                .ifCondition(equalsNull(array)).localMethod(initArray).blockEnd()
                .methodReturn(arrayAtIndex(array, op(op(arrayLength(array), "-", ref("index")), "-", constant(1))));
        return localMethod(method, index);
    }

    public CodegenExpression getFirstValueCodegen(CodegenClassScope classScope, CodegenMethod parentMethod) {
        CodegenMethod method = parentMethod.makeChildWithScope(EventBean.class, AggregatorAccessLinearJoin.class, CodegenSymbolProviderEmpty.INSTANCE, classScope);
        method.getBlock().ifCondition(exprDotMethod(refSet, "isEmpty")).blockReturn(constantNull())
                .declareVar(Map.Entry.class, "entry", cast(Map.Entry.class, exprDotMethodChain(refSet).add("entrySet").add("iterator").add("next")))
                .methodReturn(cast(EventBean.class, exprDotMethod(ref("entry"), "getKey")));
        return localMethod(method);
    }

    public CodegenExpression getLastValueCodegen(CodegenClassScope classScope, CodegenMethod parentMethod, CodegenNamedMethods namedMethods) {
        CodegenMethod initArray = initArrayCodegen(namedMethods, classScope);
        CodegenMethod method = parentMethod.makeChildWithScope(EventBean.class, AggregatorAccessLinearJoin.class, CodegenSymbolProviderEmpty.INSTANCE, classScope);
        method.getBlock().ifCondition(exprDotMethod(refSet, "isEmpty")).blockReturn(constantNull())
                .ifCondition(equalsNull(array)).localMethod(initArray).blockEnd()
                .methodReturn(arrayAtIndex(array, op(arrayLength(array), "-", constant(1))));
        return localMethod(method);
    }

    public CodegenExpression iteratorCodegen(CodegenClassScope classScope, CodegenMethod parentMethod, CodegenNamedMethods namedMethods) {
        CodegenMethod initArray = initArrayCodegen(namedMethods, classScope);
        CodegenMethod method = parentMethod.makeChildWithScope(Iterator.class, AggregatorAccessLinearJoin.class, CodegenSymbolProviderEmpty.INSTANCE, classScope);
        method.getBlock().ifNull(array)
                .localMethod(initArray)
                .blockEnd()
                .methodReturn(newInstance(ArrayEventIterator.class, array));
        return localMethod(method);
    }

    public CodegenExpression collectionReadOnlyCodegen(CodegenMethod parentMethod, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        CodegenMethod initArray = initArrayCodegen(namedMethods, classScope);
        CodegenMethod method = parentMethod.makeChildWithScope(Collection.class, AggregatorAccessLinearJoin.class, CodegenSymbolProviderEmpty.INSTANCE, classScope);
        method.getBlock().ifNull(array)
                .localMethod(initArray)
                .blockEnd()
                .methodReturn(staticMethod(Arrays.class, "asList", array));
        return localMethod(method);
    }

    public CodegenExpression sizeCodegen() {
        return exprDotMethod(refSet, "size");
    }

    private CodegenMethod addEventCodegen(CodegenMethod parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChildWithScope(void.class, AggregatorAccessLinearJoin.class, CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(EventBean.class, "theEvent");
        method.getBlock().assignRef(array, constantNull())
                .declareVar(Integer.class, "value", cast(Integer.class, exprDotMethod(refSet, "get", ref("theEvent"))))
                .ifRefNull("value")
                .exprDotMethod(refSet, "put", ref("theEvent"), constant(1))
                .blockReturnNoValue()
                .incrementRef("value")
                .exprDotMethod(refSet, "put", ref("theEvent"), ref("value"));
        return method;
    }

    private CodegenMethod removeEventCodegen(CodegenMethod parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChildWithScope(void.class, AggregatorAccessLinearJoin.class, CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(EventBean.class, "theEvent");
        method.getBlock().assignRef(array, constantNull())
                .declareVar(Integer.class, "value", cast(Integer.class, exprDotMethod(refSet, "get", ref("theEvent"))))
                .ifRefNull("value").blockReturnNoValue()
                .ifCondition(equalsIdentity(ref("value"), constant(1)))
                .exprDotMethod(refSet, "remove", ref("theEvent"))
                .blockReturnNoValue()
                .decrementRef("value")
                .exprDotMethod(refSet, "put", ref("theEvent"), ref("value"));
        return method;
    }

    private CodegenMethod initArrayCodegen(CodegenNamedMethods namedMethods, CodegenClassScope classScope) {
        Consumer<CodegenMethod> code = method -> {
            method.getBlock().declareVar(Set.class, EventBean.class, "events", exprDotMethod(refSet, "keySet"))
                    .assignRef(array, staticMethod(CollectionUtil.class, METHOD_TOARRAYEVENTS, ref("events")));
        };
        return namedMethods.addMethod(void.class, "initArray_" + array.getRef(), Collections.emptyList(), AggregatorAccessLinearJoin.class, classScope, code);
    }

    private CodegenExpressionField getSerde(CodegenClassScope classScope) {
        return classScope.addOrGetFieldSharable(new CodegenSharableSerdeEventTyped(LINKEDHASHMAPEVENTSANDINT, forge.getEventType()));
    }
}