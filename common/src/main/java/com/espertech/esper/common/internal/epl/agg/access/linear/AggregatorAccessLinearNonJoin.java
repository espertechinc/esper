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
import com.espertech.esper.common.internal.epl.agg.access.core.AggregatorAccessWFilterBase;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.serde.compiletime.sharable.CodegenSharableSerdeEventTyped;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.GE;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.LT;
import static com.espertech.esper.common.internal.epl.agg.method.core.AggregatorCodegenUtil.rowDotMember;
import static com.espertech.esper.common.internal.serde.compiletime.sharable.CodegenSharableSerdeEventTyped.CodegenSharableSerdeName.LISTEVENTS;

/**
 * Implementation of access function for single-stream (not joins).
 */
public class AggregatorAccessLinearNonJoin extends AggregatorAccessWFilterBase implements AggregatorAccessLinear {
    private final AggregationStateLinearForge forge;
    private final CodegenExpressionMember events;

    public AggregatorAccessLinearNonJoin(AggregationStateLinearForge forge, int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope, ExprNode optionalFilter) {
        super(optionalFilter);
        this.forge = forge;
        events = membersColumnized.addMember(col, List.class, "events");
        rowCtor.getBlock().assignRef(events, newInstance(ArrayList.class));
    }

    protected void applyEnterFiltered(CodegenMethod method, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        CodegenExpressionRef eps = symbols.getAddEPS(method);
        method.getBlock()
                .declareVar(EventBean.class, "theEvent", arrayAtIndex(eps, constant(forge.getStreamNum())))
                .ifRefNull("theEvent").blockReturnNoValue()
                .exprDotMethod(events, "add", ref("theEvent"));
    }

    protected void applyLeaveFiltered(CodegenMethod method, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        CodegenExpressionRef eps = symbols.getAddEPS(method);
        method.getBlock()
                .declareVar(EventBean.class, "theEvent", arrayAtIndex(eps, constant(forge.getStreamNum())))
                .ifRefNull("theEvent").blockReturnNoValue()
                .exprDotMethod(events, "remove", ref("theEvent"));
    }

    public void clearCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(events, "clear");
    }

    public CodegenExpression getFirstNthValueCodegen(CodegenExpressionRef index, CodegenMethod parentMethod, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        CodegenMethod method = parentMethod.makeChildWithScope(EventBean.class, AggregatorAccessLinearNonJoin.class, CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(int.class, "index");
        method.getBlock().ifCondition(relational(ref("index"), LT, constant(0))).blockReturn(constantNull())
                .ifCondition(relational(ref("index"), GE, exprDotMethod(events, "size"))).blockReturn(constantNull())
                .methodReturn(cast(EventBean.class, exprDotMethod(events, "get", ref("index"))));
        return localMethod(method, index);
    }

    public CodegenExpression getLastNthValueCodegen(CodegenExpressionRef index, CodegenMethod parentMethod, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        CodegenMethod method = parentMethod.makeChildWithScope(EventBean.class, AggregatorAccessLinearNonJoin.class, CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(int.class, "index");
        method.getBlock().ifCondition(relational(ref("index"), LT, constant(0))).blockReturn(constantNull())
                .ifCondition(relational(ref("index"), GE, exprDotMethod(events, "size"))).blockReturn(constantNull())
                .methodReturn(cast(EventBean.class, exprDotMethod(events, "get", op(op(exprDotMethod(events, "size"), "-", ref("index")), "-", constant(1)))));
        return localMethod(method, index);
    }

    public CodegenExpression getFirstValueCodegen(CodegenClassScope classScope, CodegenMethod parentMethod) {
        CodegenMethod method = parentMethod.makeChildWithScope(EventBean.class, AggregatorAccessLinearNonJoin.class, CodegenSymbolProviderEmpty.INSTANCE, classScope);
        method.getBlock().ifCondition(exprDotMethod(events, "isEmpty")).blockReturn(constantNull())
                .methodReturn(cast(EventBean.class, exprDotMethod(events, "get", constant(0))));
        return localMethod(method);
    }

    public CodegenExpression getLastValueCodegen(CodegenClassScope classScope, CodegenMethod parentMethod, CodegenNamedMethods namedMethods) {
        CodegenMethod method = parentMethod.makeChildWithScope(EventBean.class, AggregatorAccessLinearNonJoin.class, CodegenSymbolProviderEmpty.INSTANCE, classScope);
        method.getBlock().ifCondition(exprDotMethod(events, "isEmpty")).blockReturn(constantNull())
                .methodReturn(cast(EventBean.class, exprDotMethod(events, "get", op(exprDotMethod(events, "size"), "-", constant(1)))));
        return localMethod(method);
    }

    public CodegenExpression iteratorCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenNamedMethods namedMethods) {
        return exprDotMethod(events, "iterator");
    }

    public CodegenExpression collectionReadOnlyCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        return events;
    }

    public CodegenExpression sizeCodegen() {
        return exprDotMethod(events, "size");
    }

    public void writeCodegen(CodegenExpressionRef row, int col, CodegenExpressionRef output, CodegenExpressionRef unitKey, CodegenExpressionRef writer, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(getSerde(classScope), "write", rowDotMember(row, events), output, unitKey, writer);
    }

    public void readCodegen(CodegenExpressionRef row, int col, CodegenExpressionRef input, CodegenMethod method, CodegenExpressionRef unitKey, CodegenClassScope classScope) {
        method.getBlock().assignRef(rowDotMember(row, events), cast(List.class, exprDotMethod(getSerde(classScope), "read", input, unitKey)));
    }

    private CodegenExpressionField getSerde(CodegenClassScope classScope) {
        return classScope.addOrGetFieldSharable(new CodegenSharableSerdeEventTyped(LISTEVENTS, forge.getEventType()));
    }
}
