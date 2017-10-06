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

import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.core.CodegenCtor;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.collection.RefCountedSet;
import com.espertech.esper.codegen.base.CodegenMembersColumnized;
import com.espertech.esper.epl.agg.factory.AggregationMethodFactoryNth;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprForge;

import java.util.function.Consumer;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionRelational.CodegenRelational.GT;

/**
 * Aggregator to return the Nth oldest element to enter, with N=1 the most recent
 * value is returned. If N is larger than the enter minus leave size, null is returned.
 * A maximum N historical values are stored, so it can be safely used to compare
 * recent values in large views without incurring excessive overhead.
 */
public class AggregatorNth implements AggregationMethod {

    protected final int sizeBuf;

    protected Object[] circularBuffer;
    protected int currentBufferElementPointer;
    protected long numDataPoints;

    /**
     * Ctor.
     *
     * @param sizeBuf size
     */
    public AggregatorNth(int sizeBuf) {
        this.sizeBuf = sizeBuf;
    }

    public static void rowMemberCodegen(AggregationMethodFactoryNth forge, int column, CodegenCtor ctor, CodegenMembersColumnized membersColumnized) {
        membersColumnized.addMember(column, Object[].class, "circularBuffer");
        membersColumnized.addMember(column, int.class, "currentBufferElementPointer");
        membersColumnized.addMember(column, long.class, "numDataPoints");
        if (forge.getParent().isDistinct()) {
            membersColumnized.addMember(column, RefCountedSet.class, "distinctSet");
            ctor.getBlock().assignRef(refCol("distinctSet", column), newInstance(RefCountedSet.class));
        }
    }

    public void enter(Object value) {
        Object[] arr = (Object[]) value;
        enterValues(arr);
    }

    public static void applyEnterCodegen(AggregationMethodFactoryNth forge, int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        if (forge.getParent().getOptionalFilter() != null) {
            AggregatorCodegenUtil.prefixWithFilterCheck(forge.getParent().getOptionalFilter().getForge(), method, symbols, classScope);
        }

        CodegenExpressionRef numDataPoints = refCol("numDataPoints", column);
        CodegenExpressionRef circularBuffer = refCol("circularBuffer", column);
        CodegenExpressionRef currentBufferElementPointer = refCol("currentBufferElementPointer", column);

        Class type = forges[0].getEvaluationType();
        CodegenExpression expr = forges[0].evaluateCodegen(long.class, method, symbols, classScope);
        method.getBlock().declareVar(type, "value", expr);
        if (forge.getParent().isDistinct()) {
            method.getBlock().ifCondition(not(exprDotMethod(refCol("distinctSet", column), "add", ref("value")))).blockReturnNoValue();
        }

        method.getBlock().increment(numDataPoints)
                .ifCondition(equalsNull(circularBuffer))
                .apply(clearCode(forge, column))
                .blockEnd()
                .assignArrayElement(circularBuffer, currentBufferElementPointer, ref("value"))
                .assignRef(currentBufferElementPointer, op(op(currentBufferElementPointer, "+", constant(1)), "%", constant(forge.getSizeOfBuf())));
    }

    public void leave(Object value) {
        if (sizeBuf > numDataPoints) {
            int diff = sizeBuf - (int) numDataPoints;
            int index = (currentBufferElementPointer + diff - 1) % sizeBuf;
            circularBuffer[index] = null;
        }
        numDataPoints--;
    }

    public static void applyLeaveCodegen(AggregationMethodFactoryNth forge, int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        if (forge.getParent().getOptionalFilter() != null) {
            AggregatorCodegenUtil.prefixWithFilterCheck(forge.getParent().getOptionalFilter().getForge(), method, symbols, classScope);
        }

        CodegenExpressionRef numDataPoints = refCol("numDataPoints", column);
        CodegenExpressionRef circularBuffer = refCol("circularBuffer", column);
        CodegenExpressionRef currentBufferElementPointer = refCol("currentBufferElementPointer", column);

        if (forge.getParent().isDistinct()) {
            Class type = forges[0].getEvaluationType();
            CodegenExpression expr = forges[0].evaluateCodegen(long.class, method, symbols, classScope);
            method.getBlock().declareVar(type, "value", expr);
            method.getBlock().ifCondition(not(exprDotMethod(refCol("distinctSet", column), "add", ref("value")))).blockReturnNoValue();
        }

        method.getBlock().ifCondition(relational(constant(forge.getSizeOfBuf()), GT, numDataPoints))
                .declareVar(int.class, "diff", op(constant(forge.getSizeOfBuf()), "-", cast(int.class, numDataPoints)))
                .declareVar(int.class, "index", op(op(op(currentBufferElementPointer, "+", ref("diff")), "-", constant(1)), "%", constant(forge.getSizeOfBuf())))
                .assignArrayElement(circularBuffer, ref("index"), constantNull())
                .blockEnd()
                .decrement(numDataPoints);
    }

    public void clear() {
        circularBuffer = new Object[sizeBuf];
        numDataPoints = 0;
        currentBufferElementPointer = 0;
    }

    public static void clearCodegen(AggregationMethodFactoryNth forge, int column, CodegenMethodNode method) {
        method.getBlock().apply(clearCode(forge, column))
            .applyConditional(forge.getParent().isDistinct(), block -> block.exprDotMethod(refCol("distinctSet", column), "clear"));
    }

    public Object getValue() {
        if (circularBuffer == null) {
            return null;
        }
        return circularBuffer[(currentBufferElementPointer + sizeBuf) % sizeBuf];
    }

    public static void getValueCodegen(AggregationMethodFactoryNth forge, int column, CodegenMethodNode method) {
        CodegenExpressionRef circularBuffer = refCol("circularBuffer", column);
        CodegenExpressionRef currentBufferElementPointer = refCol("currentBufferElementPointer", column);
        CodegenExpression sizeBuf = constant(forge.getSizeOfBuf());
        method.getBlock().ifRefNullReturnNull(circularBuffer)
                .declareVar(int.class, "index", op(op(currentBufferElementPointer, "+", sizeBuf), "%", sizeBuf))
                .methodReturn(arrayAtIndex(circularBuffer, ref("index")));
    }

    protected void enterValues(Object[] arr) {
        numDataPoints++;
        if (circularBuffer == null) {
            clear();
        }
        circularBuffer[currentBufferElementPointer] = arr[0];
        currentBufferElementPointer = (currentBufferElementPointer + 1) % sizeBuf;
    }

    public int getSizeBuf() {
        return sizeBuf;
    }

    public Object[] getCircularBuffer() {
        return circularBuffer;
    }

    public void setCircularBuffer(Object[] circularBuffer) {
        this.circularBuffer = circularBuffer;
    }

    public int getCurrentBufferElementPointer() {
        return currentBufferElementPointer;
    }

    public void setCurrentBufferElementPointer(int currentBufferElementPointer) {
        this.currentBufferElementPointer = currentBufferElementPointer;
    }

    public long getNumDataPoints() {
        return numDataPoints;
    }

    public void setNumDataPoints(long numDataPoints) {
        this.numDataPoints = numDataPoints;
    }

    private static Consumer<CodegenBlock> clearCode(AggregationMethodFactoryNth forge, int column) {
        return block -> {
            block.assignRef(refCol("circularBuffer", column), newArrayByLength(Object.class, constant(forge.getSizeOfBuf())))
                    .assignRef(refCol("numDataPoints", column), constant(0))
                    .assignRef(refCol("currentBufferElementPointer", column), constant(0));
        };
    }
}