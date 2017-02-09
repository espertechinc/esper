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
package com.espertech.esper.epl.updatehelper;

import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.event.EventPropertyWriter;
import com.espertech.esper.util.TypeWidener;

public class EventBeanUpdateItem {
    private final ExprEvaluator expression;
    private final String optionalPropertyName;
    private final EventPropertyWriter optionalWriter;
    private final boolean notNullableField;
    private final TypeWidener optionalWidener;

    public EventBeanUpdateItem(ExprEvaluator expression, String optinalPropertyName, EventPropertyWriter optionalWriter, boolean notNullableField, TypeWidener optionalWidener) {
        this.expression = expression;
        this.optionalPropertyName = optinalPropertyName;
        this.optionalWriter = optionalWriter;
        this.notNullableField = notNullableField;
        this.optionalWidener = optionalWidener;
    }

    public ExprEvaluator getExpression() {
        return expression;
    }

    public String getOptionalPropertyName() {
        return optionalPropertyName;
    }

    public EventPropertyWriter getOptionalWriter() {
        return optionalWriter;
    }

    public boolean isNotNullableField() {
        return notNullableField;
    }

    public TypeWidener getOptionalWidener() {
        return optionalWidener;
    }
}