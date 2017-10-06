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
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprForge;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Aggregator for the very first value.
 */
public class AggregatorFirstEver implements AggregationMethod {
    protected boolean isSet;
    protected Object firstValue;

    public static void rowMemberCodegen(int column, CodegenCtor ctor, CodegenMembersColumnized membersColumnized) {
        membersColumnized.addMember(column, boolean.class, "isSet");
        membersColumnized.addMember(column, Object.class, "firstValue");
    }

    public void enter(Object object) {
        if (!isSet) {
            isSet = true;
            firstValue = object;
        }
    }

    public static void applyEnterCodegen(boolean hasFilter, int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        if (hasFilter) {
            AggregatorCodegenUtil.prefixWithFilterCheck(forges[forges.length - 1], method, symbols, classScope);
        }
        CodegenExpressionRef isSet = refCol("isSet", column);
        method.getBlock().ifCondition(not(isSet))
                .assignRef(isSet, constantTrue())
                .assignRef(refCol("firstValue", column), forges[0].evaluateCodegen(Object.class, method, symbols, classScope));
    }

    public void leave(Object object) {
    }

    public void clear() {
        firstValue = null;
        isSet = false;
    }

    public static void clearCodegen(int column, CodegenMethodNode method) {
        method.getBlock().assignRef(refCol("firstValue", column), constantNull())
                .assignRef(refCol("isSet", column), constantFalse());
    }

    public Object getValue() {
        return firstValue;
    }

    public static void getValueCodegen(int column, CodegenMethodNode method) {
        method.getBlock().methodReturn(refCol("firstValue", column));
    }

    public boolean isSet() {
        return isSet;
    }

    public void setSet(boolean set) {
        isSet = set;
    }

    public Object getFirstValue() {
        return firstValue;
    }

    public void setFirstValue(Object firstValue) {
        this.firstValue = firstValue;
    }
}