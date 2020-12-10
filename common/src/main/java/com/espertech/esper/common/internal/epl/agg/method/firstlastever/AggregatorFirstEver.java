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

import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionMember;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorMethodWDistinctWFilterWValueBase;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.fabric.FabricTypeCollector;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.serde.compiletime.sharable.CodegenSharableSerdeClassTyped;

import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.agg.method.core.AggregatorCodegenUtil.*;
import static com.espertech.esper.common.internal.serde.compiletime.sharable.CodegenSharableSerdeClassTyped.CodegenSharableSerdeName.VALUE_NULLABLE;

/**
 * Aggregator for the very first value.
 */
public class AggregatorFirstEver extends AggregatorMethodWDistinctWFilterWValueBase {
    private final EPTypeClass childType;
    private final DataInputOutputSerdeForge serde;

    private CodegenExpressionMember isSet;
    private CodegenExpressionMember firstValue;
    private CodegenExpressionField serdeField;

    public AggregatorFirstEver(EPTypeClass optionalDistinctValueType, DataInputOutputSerdeForge optionalDistinctSerde, boolean hasFilter, ExprNode optionalFilter, EPTypeClass childType, DataInputOutputSerdeForge serde) {
        super(optionalDistinctValueType, optionalDistinctSerde, hasFilter, optionalFilter);
        this.childType = childType;
        this.serde = serde;
    }

    public void initForgeFiltered(int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope) {
        isSet = membersColumnized.addMember(col, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), "isSet");
        firstValue = membersColumnized.addMember(col, EPTypePremade.OBJECT.getEPType(), "firstValue");
        this.serdeField = classScope.addOrGetFieldSharable(new CodegenSharableSerdeClassTyped(VALUE_NULLABLE, childType, serde, classScope));
    }

    protected void applyEvalEnterNonNull(CodegenExpressionRef value, EPType valueType, CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        method.getBlock().apply(enterConsumer(forges[0].evaluateCodegen(EPTypePremade.OBJECT.getEPType(), method, symbols, classScope)));
    }

    protected void applyTableEnterNonNull(CodegenExpressionRef value, EPType[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().apply(enterConsumer(value));
    }

    protected void applyEvalLeaveNonNull(CodegenExpressionRef value, EPType valueType, CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        // no op
    }

    protected void applyTableLeaveNonNull(CodegenExpressionRef value, EPType[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
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
                .expression(writeNullable(rowDotMember(row, firstValue), serdeField, output, unitKey, writer, classScope));
    }

    protected void readWODistinct(CodegenExpressionRef row, int col, CodegenExpressionRef input, CodegenExpressionRef unitKey, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().apply(readBoolean(row, isSet, input))
                .assignRef(rowDotMember(row, firstValue), readNullable(serdeField, input, unitKey, classScope));
    }

    protected void appendFormatWODistinct(FabricTypeCollector collector) {
        collector.builtin(boolean.class);
        collector.serde(serde);
    }

    private Consumer<CodegenBlock> enterConsumer(CodegenExpression value) {
        return block -> block.ifCondition(not(isSet))
                .assignRef(isSet, constantTrue())
                .assignRef(firstValue, value);
    }
}