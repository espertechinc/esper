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

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.core.CodegenCtor;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.codegen.base.CodegenMembersColumnized;
import com.espertech.esper.epl.agg.factory.AggregationMethodFactoryMinMax;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.MinMaxTypeEnum;
import com.espertech.esper.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionRelational.CodegenRelational.GT;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionRelational.CodegenRelational.LT;
import static com.espertech.esper.epl.expression.core.MinMaxTypeEnum.MAX;

/**
 * Min/max aggregator for all values, not considering events leaving the aggregation (i.e. ever).
 */
public class AggregatorMinMaxEver implements AggregationMethod {
    private static final Logger log = LoggerFactory.getLogger(AggregatorMinMaxEver.class);

    protected final MinMaxTypeEnum minMaxTypeEnum;

    protected Comparable currentMinMax;

    /**
     * Ctor.
     *
     * @param minMaxTypeEnum - enum indicating to return minimum or maximum values
     */
    public AggregatorMinMaxEver(MinMaxTypeEnum minMaxTypeEnum) {
        this.minMaxTypeEnum = minMaxTypeEnum;
    }

    public static void rowMemberCodegen(int column, CodegenCtor ctor, CodegenMembersColumnized membersColumnized) {
        membersColumnized.addMember(column, Comparable.class, "currentMinMax");
    }

    public void enter(Object object) {
        if (object == null) {
            return;
        }
        if (currentMinMax == null) {
            currentMinMax = (Comparable) object;
            return;
        }
        if (minMaxTypeEnum == MAX) {
            if (currentMinMax.compareTo(object) < 0) {
                currentMinMax = (Comparable) object;
            }
        } else {
            if (currentMinMax.compareTo(object) > 0) {
                currentMinMax = (Comparable) object;
            }
        }
    }

    public static void applyEnterCodegen(AggregationMethodFactoryMinMax forge, int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        if (forge.getParent().isHasFilter()) {
            AggregatorCodegenUtil.prefixWithFilterCheck(forges[forges.length - 1], method, symbols, classScope);
        }

        Class type = forges[0].getEvaluationType();
        CodegenExpressionRef currentMinMax = refCol("currentMinMax", column);
        method.getBlock().declareVar(JavaClassHelper.getBoxedType(type), "object", forges[0].evaluateCodegen(type, method, symbols, classScope));
        if (!type.isPrimitive()) {
            method.getBlock().ifRefNull("object").blockReturnNoValue();
        }
        method.getBlock().ifCondition(equalsNull(currentMinMax))
                .assignRef(currentMinMax, ref("object"))
                .blockReturnNoValue()
                .ifCondition(relational(exprDotMethod(currentMinMax, "compareTo", ref("object")), forge.getParent().getMinMaxTypeEnum() == MAX ? LT : GT, constant(0)))
                .assignRef(currentMinMax, ref("object"));
    }

    public void leave(Object object) {
        // no-op, this is designed to handle min-max ever
    }

    public void clear() {
        currentMinMax = null;
    }

    public static void clearCodegen(int column, CodegenMethodNode method, CodegenClassScope classScope) {
        method.getBlock().assignRef(refCol("currentMinMax", column), constantNull());
    }

    public Object getValue() {
        return currentMinMax;
    }

    public static void getValueCodegen(int column, CodegenMethodNode method) {
        method.getBlock().methodReturn(refCol("currentMinMax", column));
    }

    public void setCurrentMinMax(Comparable currentMinMax) {
        this.currentMinMax = currentMinMax;
    }

    public Comparable getCurrentMinMax() {
        return currentMinMax;
    }
}
