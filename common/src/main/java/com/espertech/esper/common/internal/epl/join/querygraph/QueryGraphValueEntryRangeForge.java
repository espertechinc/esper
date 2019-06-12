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
package com.espertech.esper.common.internal.epl.join.querygraph;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;

import java.io.StringWriter;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newArrayWithInit;

public abstract class QueryGraphValueEntryRangeForge implements QueryGraphValueEntryForge {

    protected final QueryGraphRangeEnum type;

    protected QueryGraphValueEntryRangeForge(QueryGraphRangeEnum type) {
        this.type = type;
    }

    public QueryGraphRangeEnum getType() {
        return type;
    }

    public abstract String toQueryPlan();

    public abstract ExprNode[] getExpressions();

    protected abstract Class getResultType();

    public abstract CodegenExpression make(Class optionalCoercionType, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope);

    public static String toQueryPlan(List<QueryGraphValueEntryRangeForge> rangeKeyPairs) {
        StringWriter writer = new StringWriter();
        String delimiter = "";
        for (QueryGraphValueEntryRangeForge item : rangeKeyPairs) {
            writer.write(delimiter);
            writer.write(item.toQueryPlan());
            delimiter = ", ";
        }
        return writer.toString();
    }

    public static CodegenExpression makeArray(QueryGraphValueEntryRangeForge[] ranges, CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenExpression[] expressions = new CodegenExpression[ranges.length];
        for (int i = 0; i < ranges.length; i++) {
            expressions[i] = ranges[i].make(method, symbols, classScope);
        }
        return newArrayWithInit(QueryGraphValueEntryRange.class, expressions);
    }

    public static Class[] getRangeResultTypes(QueryGraphValueEntryRangeForge[] ranges) {
        Class[] types = new Class[ranges.length];
        for (int i = 0; i < ranges.length; i++) {
            types[i] = ranges[i].getResultType();
        }
        return types;
    }
}
