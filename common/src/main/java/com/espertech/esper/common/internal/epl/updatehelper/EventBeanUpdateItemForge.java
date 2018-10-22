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
package com.espertech.esper.common.internal.epl.updatehelper;

import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.event.core.EventPropertyWriterSPI;
import com.espertech.esper.common.internal.util.TypeWidenerSPI;

public class EventBeanUpdateItemForge {
    private final ExprForge expression;
    private final String optionalPropertyName;
    private final EventPropertyWriterSPI optionalWriter;
    private final boolean notNullableField;
    private final TypeWidenerSPI optionalWidener;

    public EventBeanUpdateItemForge(ExprForge expression, String optinalPropertyName, EventPropertyWriterSPI optionalWriter, boolean notNullableField, TypeWidenerSPI optionalWidener) {
        this.expression = expression;
        this.optionalPropertyName = optinalPropertyName;
        this.optionalWriter = optionalWriter;
        this.notNullableField = notNullableField;
        this.optionalWidener = optionalWidener;
    }

    public ExprForge getExpression() {
        return expression;
    }

    public String getOptionalPropertyName() {
        return optionalPropertyName;
    }

    public EventPropertyWriterSPI getOptionalWriter() {
        return optionalWriter;
    }

    public boolean isNotNullableField() {
        return notNullableField;
    }

    public TypeWidenerSPI getOptionalWidener() {
        return optionalWidener;
    }
}