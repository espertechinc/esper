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
package com.espertech.esper.common.internal.epl.agg.method.firstlastever;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorMethodWDistinctWFilterWValueBase;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.serde.CodegenSharableSerdeClassTyped;

import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.agg.method.core.AggregatorCodegenUtil.*;
import static com.espertech.esper.common.internal.serde.CodegenSharableSerdeClassTyped.CodegenSharableSerdeName.VALUE_NULLABLE;

/**
 * Aggregator for the very first value.
 */
public class AggregatorFirstEver extends AggregatorMethodWDistinctWFilterWValueBase {
    private final CodegenExpressionRef isSet;
    private final CodegenExpressionRef firstValue;
    private final CodegenExpressionField serde;

    public AggregatorFirstEver(AggregationForgeFactory factory, int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope, Class optionalDistinctValueType, boolean hasFilter, ExprNode optionalFilter, Class childType) {
        super(factory, col, rowCtor, membersColumnized, classScope, optionalDistinctValueType, hasFilter, optionalFilter);
        isSet = membersColumnized.addMember(col, boolean.class, "isSet");
        firstValue = membersColumnized.addMember(col, Object.class, "firstValue");
        serde = classScope.addOrGetFieldSharable(new CodegenSharableSerdeClassTyped(VALUE_NULLABLE, childType));
    }

    protected void applyEvalEnterNonNull(CodegenExpressionRef value, Class valueType, CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        method.getBlock().apply(enterConsumer(forges[0].evaluateCodegen(Object.class, method, symbols, classScope)));
    }

    protected void applyTableEnterNonNull(CodegenExpressionRef value, Class[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().apply(enterConsumer(value));
    }

    protected void applyEvalLeaveNonNull(CodegenExpressionRef value, Class valueType, CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        // no op
    }

    protected void applyTableLeaveNonNull(CodegenExpressionRef value, Class[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        // no op
    }

    protected void clearWODistinct(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().assignRef(firstValue, constantNull())
                .assignRef(isSet, constantFalse());
    }

    public void getValueCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(firstValue);
    }

    protected void writeWODistinct(CodegenExpressionRef row, int col, CodegenExpressionRef output, CodegenExpressionRef unitKey, CodegenExpressionRef writer, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().apply(writeBoolean(output, row, isSet))
                .expression(writeNullable(rowDotRef(row, firstValue), serde, output, unitKey, writer, classScope));
    }

    protected void readWODistinct(CodegenExpressionRef row, int col, CodegenExpressionRef input, CodegenExpressionRef unitKey, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().apply(readBoolean(row, isSet, input))
                .assignRef(rowDotRef(row, firstValue), readNullable(serde, input, unitKey, classScope));
    }

    private Consumer<CodegenBlock> enterConsumer(CodegenExpression value) {
        return block -> block.ifCondition(not(isSet))
                .assignRef(isSet, constantTrue())
                .assignRef(firstValue, value);
    }
}