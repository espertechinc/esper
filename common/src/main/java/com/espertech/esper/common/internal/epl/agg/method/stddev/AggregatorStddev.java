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
package com.espertech.esper.common.internal.epl.agg.method.stddev;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionMember;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorMethodWDistinctWFilterWValueBase;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.util.SimpleNumberCoercerFactory;

import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.LE;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.LT;
import static com.espertech.esper.common.internal.epl.agg.method.core.AggregatorCodegenUtil.*;

/**
 * Standard deviation always generates double-typed numbers.
 */
public class AggregatorStddev extends AggregatorMethodWDistinctWFilterWValueBase {
    private CodegenExpressionMember mean;
    private CodegenExpressionMember qn;
    private CodegenExpressionMember cnt;

    public AggregatorStddev(AggregationForgeFactory factory, int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope, Class optionalDistinctValueType, DataInputOutputSerdeForge optionalDistinctSerde, boolean hasFilter, ExprNode optionalFilter) {
        super(factory, col, rowCtor, membersColumnized, classScope, optionalDistinctValueType, optionalDistinctSerde, hasFilter, optionalFilter);
        mean = membersColumnized.addMember(col, double.class, "mean");
        qn = membersColumnized.addMember(col, double.class, "qn");
        cnt = membersColumnized.addMember(col, long.class, "cnt");
    }

    protected void applyEvalEnterNonNull(CodegenExpressionRef value, Class valueType, CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        applyEvalEnterNonNull(method, SimpleNumberCoercerFactory.SimpleNumberCoercerDouble.codegenDouble(value, valueType));
    }

    protected void applyEvalLeaveNonNull(CodegenExpressionRef value, Class valueType, CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        applyEvalLeaveNonNull(method, SimpleNumberCoercerFactory.SimpleNumberCoercerDouble.codegenDouble(value, valueType));
    }

    protected void applyTableEnterNonNull(CodegenExpressionRef value, Class[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        applyEvalEnterNonNull(method, exprDotMethod(cast(Number.class, value), "doubleValue"));
    }

    protected void applyTableLeaveNonNull(CodegenExpressionRef value, Class[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        applyEvalLeaveNonNull(method, exprDotMethod(cast(Number.class, value), "doubleValue"));
    }

    protected void clearWODistinct(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().apply(getClear());
    }

    public void getValueCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().ifCondition(relational(cnt, LT, constant(2)))
                .blockReturn(constantNull())
                .methodReturn(staticMethod(Math.class, "sqrt", op(qn, "/", op(cnt, "-", constant(1)))));
    }

    protected void writeWODistinct(CodegenExpressionRef row, int col, CodegenExpressionRef output, CodegenExpressionRef unitKey, CodegenExpressionRef writer, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().apply(writeDouble(output, row, mean))
                .apply(writeDouble(output, row, qn))
                .apply(writeLong(output, row, cnt));
    }

    protected void readWODistinct(CodegenExpressionRef row, int col, CodegenExpressionRef input, CodegenExpressionRef unitKey, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().apply(readDouble(row, mean, input))
                .apply(readDouble(row, qn, input))
                .apply(readLong(row, cnt, input));
    }

    private void applyEvalEnterNonNull(CodegenMethod method, CodegenExpression doubleExpression) {
        method.getBlock().declareVar(double.class, "p", doubleExpression)
                .ifCondition(equalsIdentity(cnt, constant(0)))
                .assignRef(mean, ref("p"))
                .assignRef(qn, constant(0))
                .assignRef(cnt, constant(1))
                .ifElse()
                .increment(cnt)
                .declareVar(double.class, "oldmean", mean)
                .assignCompound(mean, "+", op(op(ref("p"), "-", mean), "/", cnt))
                .assignCompound(qn, "+", op(op(ref("p"), "-", ref("oldmean")), "*", op(ref("p"), "-", mean)));
    }

    private void applyEvalLeaveNonNull(CodegenMethod method, CodegenExpression doubleExpression) {
        method.getBlock().declareVar(double.class, "p", doubleExpression)
                .ifCondition(relational(cnt, LE, constant(1)))
                .apply(getClear())
                .ifElse()
                .decrement(cnt)
                .declareVar(double.class, "oldmean", mean)
                .assignCompound(mean, "-", op(op(ref("p"), "-", mean), "/", cnt))
                .assignCompound(qn, "-", op(op(ref("p"), "-", ref("oldmean")), "*", op(ref("p"), "-", mean)));
    }

    private Consumer<CodegenBlock> getClear() {
        return block -> {
            block.assignRef(mean, constant(0))
                    .assignRef(qn, constant(0))
                    .assignRef(cnt, constant(0));
        };
    }
}
