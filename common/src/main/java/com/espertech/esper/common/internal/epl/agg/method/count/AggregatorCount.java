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
package com.espertech.esper.common.internal.epl.agg.method.count;

import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionMember;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.fabric.FabricTypeCollector;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorMethodWDistinctWFilterBase;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.GT;
import static com.espertech.esper.common.internal.epl.agg.method.core.AggregatorCodegenUtil.readLong;
import static com.espertech.esper.common.internal.epl.agg.method.core.AggregatorCodegenUtil.writeLong;

public class AggregatorCount extends AggregatorMethodWDistinctWFilterBase {
    private final boolean isEver;
    private CodegenExpressionMember cnt;

    public AggregatorCount(EPType optionalDistinctValueType, DataInputOutputSerdeForge optionalDistinctSerde, boolean hasFilter, ExprNode optionalFilter, boolean isEver) {
        super(optionalDistinctValueType, optionalDistinctSerde, hasFilter, optionalFilter);
        this.isEver = isEver;
    }

    public void initForgeFiltered(int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope) {
        this.cnt = membersColumnized.addMember(col, EPTypePremade.LONGPRIMITIVE.getEPType(), "cnt");
    }

    protected void applyEvalEnterFiltered(CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        Consumer<CodegenBlock> increment = block -> block.increment(cnt);

        // handle wildcard
        if (forges.length == 0 || (optionalFilter != null && forges.length == 1)) {
            method.getBlock().apply(increment);
            return;
        }

        EPType evalType = forges[0].getEvaluationType();
        if (evalType == EPTypeNull.INSTANCE || evalType == null) {
            method.getBlock().declareVar(EPTypePremade.OBJECT.getEPType(), "value", constantNull());
        } else {
            EPTypeClass evalClass = (EPTypeClass) evalType;
            method.getBlock().declareVar(evalClass, "value", forges[0].evaluateCodegen((EPTypeClass) evalType, method, symbols, classScope));
            if (!evalClass.getType().isPrimitive()) {
                method.getBlock().ifRefNull("value").blockReturnNoValue();
            }
        }
        if (distinct != null) {
            method.getBlock().ifCondition(not(exprDotMethod(distinct, "add", toDistinctValueKey(ref("value"))))).blockReturnNoValue();
        }
        method.getBlock().apply(increment);
    }

    protected void applyTableEnterFiltered(CodegenExpressionRef value, EPType[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        if (distinct != null) {
            method.getBlock().ifCondition(not(exprDotMethod(distinct, "add", toDistinctValueKey(ref("value"))))).blockReturnNoValue();
        }
        method.getBlock().increment(cnt);
    }

    @Override
    public void applyEvalLeaveCodegen(CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        if (!isEver) {
            super.applyEvalLeaveCodegen(method, symbols, forges, classScope);
        }
    }

    public void applyEvalLeaveFiltered(CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        Consumer<CodegenBlock> decrement = block -> block.ifCondition(relational(cnt, GT, constant(0))).decrement(cnt);

        // handle wildcard
        if (forges.length == 0 || (optionalFilter != null && forges.length == 1)) {
            method.getBlock().apply(decrement);
            return;
        }

        EPType evalType = forges[0].getEvaluationType();
        if (evalType == EPTypeNull.INSTANCE || evalType == null) {
            method.getBlock().declareVar(EPTypePremade.OBJECT.getEPType(), "value", constantNull());
        } else {
            EPTypeClass evalClass = (EPTypeClass) evalType;
            method.getBlock().declareVar(evalClass, "value", forges[0].evaluateCodegen((EPTypeClass) evalType, method, symbols, classScope));
            if (!evalClass.getType().isPrimitive()) {
                method.getBlock().ifRefNull("value").blockReturnNoValue();
            }
        }
        if (distinct != null) {
            method.getBlock().ifCondition(not(exprDotMethod(distinct, "remove", toDistinctValueKey(ref("value"))))).blockReturnNoValue();
        }
        method.getBlock().apply(decrement);
    }

    protected void applyTableLeaveFiltered(CodegenExpressionRef value, EPType[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        if (distinct != null) {
            method.getBlock().ifCondition(not(exprDotMethod(distinct, "remove", toDistinctValueKey(ref("value"))))).blockReturnNoValue();
        }
        method.getBlock().decrement(cnt);
    }

    protected void clearWODistinct(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().assignRef(cnt, constant(0));
    }

    public void getValueCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(cnt);
    }

    protected void writeWODistinct(CodegenExpressionRef row, int col, CodegenExpressionRef output, CodegenExpressionRef unitKey, CodegenExpressionRef writer, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().apply(writeLong(output, row, cnt));
    }

    protected void readWODistinct(CodegenExpressionRef row, int col, CodegenExpressionRef input, CodegenExpressionRef unitKey, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().apply(readLong(row, cnt, input));
    }

    protected void appendFormatWODistinct(FabricTypeCollector collector) {
        collector.builtin(long.class);
    }
}