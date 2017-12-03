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
package com.espertech.esper.epl.expression.core;

import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.util.JavaClassHelper;

import java.io.Serializable;
import java.io.StringWriter;

public class ExprFilterSpecLookupable implements Serializable {
    private static final long serialVersionUID = 3576828533611557509L;
    private final String expression;
    private transient final EventPropertyGetter getter;
    private final Class returnType;
    private final boolean isNonPropertyGetter;

    public ExprFilterSpecLookupable(String expression, EventPropertyGetter getter, Class returnType, boolean isNonPropertyGetter) {
        this.expression = expression;
        this.getter = getter;
        this.returnType = JavaClassHelper.getBoxedType(returnType); // For type consistency for recovery and serde define as boxed type
        this.isNonPropertyGetter = isNonPropertyGetter;
    }

    public String getExpression() {
        return expression;
    }

    public EventPropertyGetter getGetter() {
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
        return "expression='" + expression + '\'';
    }

    public boolean isNonPropertyGetter() {
        return isNonPropertyGetter;
    }
}

