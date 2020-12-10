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
package com.espertech.esper.common.internal.epl.agg.method.rate;

import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionMember;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorMethodWDistinctWFilterBase;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.fabric.FabricTypeCollector;
import com.espertech.esper.common.internal.schedule.TimeProviderField;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

import java.util.Deque;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.agg.method.core.AggregatorCodegenUtil.*;

/**
 * Aggregation computing an event arrival rate for with and without data window.
 */
public class AggregatorRateEver extends AggregatorMethodWDistinctWFilterBase {

    protected final AggregationForgeFactoryRate factory;
    protected CodegenExpressionMember points;
    protected CodegenExpressionMember hasLeave;

    public AggregatorRateEver(AggregationForgeFactoryRate factory, EPTypeClass optionalDistinctValueType, DataInputOutputSerdeForge optionalDistinctSerde, boolean hasFilter, ExprNode optionalFilter) {
        super(optionalDistinctValueType, optionalDistinctSerde, hasFilter, optionalFilter);
        this.factory = factory;
    }

    public void initForgeFiltered(int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope) {
        points = membersColumnized.addMember(col, EPTypePremade.DEQUE.getEPType(), "points");
        hasLeave = membersColumnized.addMember(col, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), "hasLeave");
        rowCtor.getBlock().assignRef(points, newInstance(EPTypePremade.ARRAYDEQUE.getEPType()));
    }

    protected void applyEvalEnterFiltered(CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        apply(method, classScope);
    }

    @Override
    public void applyEvalLeaveCodegen(CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        // This is an "ever" aggregator and is designed for use in non-window env
    }

    protected void applyEvalLeaveFiltered(CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
    }

    protected void applyTableEnterFiltered(CodegenExpressionRef value, EPType[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        apply(method, classScope);
    }

    protected void applyTableLeaveFiltered(CodegenExpressionRef value, EPType[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        // This is an "ever" aggregator and is designed for use in non-window env
    }

    protected void clearWODistinct(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(points, "clear");
    }

    public void getValueCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().ifCondition(not(exprDotMethod(points, "isEmpty")))
                .declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "newest", cast(EPTypePremade.LONGBOXED.getEPType(), exprDotMethod(points, "getLast")))
                .declareVar(EPTypePremade.BOOLEANBOXED.getEPType(), "leave", staticMethod(AggregatorRateEver.class, "removeFromHead", points, ref("newest"), constant(factory.getIntervalTime())))
                .assignCompound(hasLeave, "|", ref("leave"))
                .blockEnd()
                .ifCondition(not(hasLeave)).blockReturn(constantNull())
                .ifCondition(exprDotMethod(points, "isEmpty")).blockReturn(constant(0d))
                .methodReturn(op(op(op(exprDotMethod(points, "size"), "*", constant(factory.getTimeAbacus().getOneSecond())), "*", constant(1d)), "/", constant(factory.getIntervalTime())));
    }

    protected void writeWODistinct(CodegenExpressionRef row, int col, CodegenExpressionRef output, CodegenExpressionRef unitKey, CodegenExpressionRef writer, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock()
                .apply(writeBoolean(output, row, hasLeave))
                .staticMethod(AggregatorRateEverSerde.class, "writePoints", output, rowDotMember(row, points));
    }

    protected void readWODistinct(CodegenExpressionRef row, int col, CodegenExpressionRef input, CodegenExpressionRef unitKey, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock()
                .apply(readBoolean(row, hasLeave, input))
                .assignRef(rowDotMember(row, points), staticMethod(AggregatorRateEverSerde.class, "readPoints", input));
    }

    protected void appendFormatWODistinct(FabricTypeCollector collector) {
        collector.builtin(boolean.class);
        collector.aggregatorRateEver(AggregatorRateEverSerde.SERDE_VERSION);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param points    points
     * @param timestamp timestamp
     * @param interval  interval
     * @return hasLeave
     */
    public static boolean removeFromHead(Deque<Long> points, long timestamp, long interval) {
        boolean hasLeave = false;
        if (points.size() > 1) {
            while (true) {
                long first = points.getFirst();
                long delta = timestamp - first;
                if (delta >= interval) {
                    points.remove();
                    hasLeave = true;
                } else {
                    break;
                }
                if (points.isEmpty()) {
                    break;
                }
            }
        }
        return hasLeave;
    }

    protected void apply(CodegenMethod method, CodegenClassScope classScope) {
        CodegenExpression timeProvider = classScope.addOrGetFieldSharable(TimeProviderField.INSTANCE);
        method.getBlock().declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "timestamp", exprDotMethod(timeProvider, "getTime"))
            .exprDotMethod(points, "add", ref("timestamp"))
            .declareVar(EPTypePremade.BOOLEANBOXED.getEPType(), "leave", staticMethod(AggregatorRateEver.class, "removeFromHead", points, ref("timestamp"), constant(factory.getIntervalTime())))
            .assignCompound(hasLeave, "|", ref("leave"));
    }
}