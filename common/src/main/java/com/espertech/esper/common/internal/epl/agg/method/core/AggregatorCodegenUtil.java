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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionMember;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;

import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class AggregatorCodegenUtil {

    public static CodegenExpression rowDotMember(CodegenExpressionRef row, CodegenExpressionMember member) {
        return member(row.getRef() + "." + member.getRef());
    }

    public static CodegenExpression writeNullable(CodegenExpression value, CodegenExpressionField serde, CodegenExpressionRef output, CodegenExpressionRef unitKey, CodegenExpressionRef writer, CodegenClassScope classScope) {
        return exprDotMethod(serde, "write", value, output, unitKey, writer);
    }

    public static CodegenExpression readNullable(CodegenExpressionField serde, CodegenExpressionRef input, CodegenExpressionRef unitKey, CodegenClassScope classScope) {
        return exprDotMethod(serde, "read", input, unitKey);
    }

    public static void prefixWithFilterCheck(ExprForge filterForge, CodegenMethod method, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        Class filterType = filterForge.getEvaluationType();
        method.getBlock().declareVar(filterType, "pass", filterForge.evaluateCodegen(filterType, method, symbols, classScope));
        if (!filterType.isPrimitive()) {
            method.getBlock().ifRefNull("pass").blockReturnNoValue();
        }
        method.getBlock().ifCondition(not(ref("pass"))).blockReturnNoValue();
    }

    public static Consumer<CodegenBlock> writeBoolean(CodegenExpressionRef output, CodegenExpressionRef row, CodegenExpressionMember member) {
        return block -> block.exprDotMethod(output, "writeBoolean", rowDotMember(row, member));
    }

    public static Consumer<CodegenBlock> readBoolean(CodegenExpressionRef row, CodegenExpressionMember member, CodegenExpression input) {
        return block -> block.assignRef(rowDotMember(row, member), exprDotMethod(input, "readBoolean"));
    }

    public static Consumer<CodegenBlock> writeLong(CodegenExpressionRef output, CodegenExpressionRef row, CodegenExpressionMember member) {
        return block -> block.exprDotMethod(output, "writeLong", rowDotMember(row, member));
    }

    public static Consumer<CodegenBlock> readLong(CodegenExpressionRef row, CodegenExpressionMember member, CodegenExpression input) {
        return block -> block.assignRef(rowDotMember(row, member), exprDotMethod(input, "readLong"));
    }

    public static Consumer<CodegenBlock> writeDouble(CodegenExpressionRef output, CodegenExpressionRef row, CodegenExpressionMember member) {
        return block -> block.exprDotMethod(output, "writeDouble", rowDotMember(row, member));
    }

    public static Consumer<CodegenBlock> readDouble(CodegenExpressionRef row, CodegenExpressionMember member, CodegenExpression input) {
        return block -> block.assignRef(rowDotMember(row, member), exprDotMethod(input, "readDouble"));
    }

    public static Consumer<CodegenBlock> writeInt(CodegenExpressionRef output, CodegenExpressionRef row, CodegenExpressionMember member) {
        return block -> block.exprDotMethod(output, "writeInt", rowDotMember(row, member));
    }

    public static Consumer<CodegenBlock> readInt(CodegenExpressionRef row, CodegenExpressionMember member, CodegenExpression input) {
        return block -> block.assignRef(rowDotMember(row, member), exprDotMethod(input, "readInt"));
    }

    public static Consumer<CodegenBlock> writeFloat(CodegenExpressionRef output, CodegenExpressionRef row, CodegenExpressionMember member) {
        return block -> block.exprDotMethod(output, "writeFloat", rowDotMember(row, member));
    }

    public static Consumer<CodegenBlock> readFloat(CodegenExpressionRef row, CodegenExpressionMember member, CodegenExpression input) {
        return block -> block.assignRef(rowDotMember(row, member), exprDotMethod(input, "readFloat"));
    }
}
