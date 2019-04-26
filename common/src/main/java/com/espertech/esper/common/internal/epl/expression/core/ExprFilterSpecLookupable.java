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

import com.espertech.esper.common.client.EventPropertyValueGetter;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.io.StringWriter;

public class ExprFilterSpecLookupable {
    private final String expression;
    private transient final EventPropertyValueGetter getter;
    private final Class returnType;
    private final boolean isNonPropertyGetter;
    private final DataInputOutputSerde<Object> valueSerde;

    public ExprFilterSpecLookupable(String expression, EventPropertyValueGetter getter, Class returnType, boolean isNonPropertyGetter, DataInputOutputSerde<Object> valueSerde) {
        this.expression = expression;
        this.getter = getter;
        this.returnType = JavaClassHelper.getBoxedType(returnType); // For type consistency for recovery and serde define as boxed type
        this.isNonPropertyGetter = isNonPropertyGetter;
        this.valueSerde = valueSerde;
    }

    public String getExpression() {
        return expression;
    }

    public EventPropertyValueGetter getGetter() {
        return getter;
    }

    public Class getReturnType() {
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

    public boolean isNonPropertyGetter() {
        return isNonPropertyGetter;
    }

    public DataInputOutputSerde<Object> getValueSerde() {
        return valueSerde;
    }
}

