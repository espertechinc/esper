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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionMember;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorMethodWDistinctWFilterWValueBase;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.serde.compiletime.sharable.CodegenSharableSerdeClassTyped;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;
import static com.espertech.esper.common.internal.epl.agg.method.core.AggregatorCodegenUtil.*;

/**
 * Aggregator for the very last value.
 */
public class AggregatorLastEver extends AggregatorMethodWDistinctWFilterWValueBase {
    private final EPTypeClass childType;
    private final DataInputOutputSerdeForge serde;
    private CodegenExpressionMember lastValue;
    private CodegenExpressionField serdeField;

    public AggregatorLastEver(EPTypeClass optionalDistinctValueType, DataInputOutputSerdeForge optionalDistinctSerde, boolean hasFilter, ExprNode optionalFilter, EPTypeClass childType, DataInputOutputSerdeForge serde) {
        super(optionalDistinctValueType, optionalDistinctSerde, hasFilter, optionalFilter);
        this.childType = childType;
        this.serde = serde;
    }

    public void initForgeFiltered(int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope) {
        lastValue = membersColumnized.addMember(col, EPTypePremade.OBJECT.getEPType(), "lastValue");
        serdeField = classScope.addOrGetFieldSharable(new CodegenSharableSerdeClassTyped(CodegenSharableSerdeClassTyped.CodegenSharableSerdeName.VALUE_NULLABLE, childType, serde, classScope));
    }

    protected void applyEvalEnterNonNull(CodegenExpressionRef value, EPType valueType, CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        method.getBlock().assignRef(lastValue, forges[0].evaluateCodegen(EPTypePremade.OBJECT.getEPType(), method, symbols, classScope));
    }

    protected void applyTableEnterNonNull(CodegenExpressionRef value, EPType[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().assignRef(lastValue, value);
    }

    @Override
    public void applyEvalLeaveCodegen(CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
    }

    @Override
    public void applyTableLeaveCodegen(CodegenExpressionRef value, EPType[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        super.applyTableLeaveCodegen(value, evaluationTypes, method, classScope);
    }

    protected void applyEvalLeaveNonNull(CodegenExpressionRef value, EPType valueType, CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
    }

    protected void applyTableLeaveNonNull(CodegenExpressionRef value, EPType[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
    }

    protected void clearWODistinct(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().assignRef(lastValue, constantNull());
    }

    public void getValueCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(lastValue);
    }

    protected void writeWODistinct(CodegenExpressionRef row, int col, CodegenExpressionRef output, CodegenExpressionRef unitKey, CodegenExpressionRef writer, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().expression(writeNullable(rowDotMember(row, lastValue), serdeField, output, unitKey, writer, classScope));
    }

    protected void readWODistinct(CodegenExpressionRef row, int col, CodegenExpressionRef input, CodegenExpressionRef unitKey, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().assignRef(rowDotMember(row, lastValue), readNullable(serdeField, input, unitKey, classScope));
    }

}