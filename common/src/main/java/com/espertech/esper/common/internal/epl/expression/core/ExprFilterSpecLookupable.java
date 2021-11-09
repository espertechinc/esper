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
package com.espertech.esper.common.internal.epl.expression.core;

import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.filterspec.FilterOperator;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.io.StringWriter;

public class ExprFilterSpecLookupable {
    public final static EPTypeClass EPTYPE = new EPTypeClass(ExprFilterSpecLookupable.class);
    public final static EPTypeClass EPTYPEARRAY = new EPTypeClass(ExprFilterSpecLookupable[].class);
    public final static EPTypeClass EPTYPE_FILTEROPERATOR = new EPTypeClass(FilterOperator.class);

    private final String expression;
    private transient final ExprEventEvaluator eval;
    private final EPTypeClass returnType;
    private final boolean isNonPropertyEval;
    private final DataInputOutputSerde valueSerde;
    private transient final ExprEvaluator expr;

    public ExprFilterSpecLookupable(String expression, ExprEventEvaluator eval, ExprEvaluator expr, EPTypeClass returnType, boolean isNonPropertyEval, DataInputOutputSerde valueSerde) {
        this.expression = expression;
        this.eval = eval;
        this.expr = expr;
        this.returnType = JavaClassHelper.getBoxedType(returnType); // For type consistency for recovery and serde define as boxed type
        this.isNonPropertyEval = isNonPropertyEval;
        this.valueSerde = valueSerde;
    }

    public String getExpression() {
        return expression;
    }

    public ExprEventEvaluator getEval() {
        return eval;
    }

    public EPTypeClass getReturnType() {
        return returnType;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExprFilterSpecLookupable that = (ExprFilterSpecLookupable) o;

        if (!expression.equals(that.expression)) return false;

        return true;
    }

    public int hashCode() {
        return expression.hashCode();
    }

    public void appendTo(StringWriter writer) {
        writer.append(expression);
    }

    public String toString() {
        return "ExprFilterSpecLookupable{" +
                "expression='" + expression + '\'' +
                '}';
    }

    public boolean isNonPropertyEval() {
        return isNonPropertyEval;
    }

    public DataInputOutputSerde getValueSerde() {
        return valueSerde;
    }

    public ExprFilterSpecLookupable make(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext) {
        // this lookupable does not depend on matched-events or evaluation-context
        // we allow it to be a factory of itself
        return this;
    }

    public ExprEvaluator getExpr() {
        return expr;
    }
}

