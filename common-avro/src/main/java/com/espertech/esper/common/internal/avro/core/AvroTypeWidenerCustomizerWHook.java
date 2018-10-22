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
package com.espertech.esper.common.internal.avro.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.hook.type.ObjectValueTypeWidenerFactory;
import com.espertech.esper.common.client.hook.type.ObjectValueTypeWidenerFactoryContext;
import com.espertech.esper.common.internal.util.TypeWidenerCustomizer;
import com.espertech.esper.common.internal.util.TypeWidenerException;
import com.espertech.esper.common.internal.util.TypeWidenerSPI;

public class AvroTypeWidenerCustomizerWHook implements TypeWidenerCustomizer {
    private final ObjectValueTypeWidenerFactory factory;
    private final EventType eventType;

    public AvroTypeWidenerCustomizerWHook(ObjectValueTypeWidenerFactory factory, EventType eventType) {
        this.factory = factory;
        this.eventType = eventType;
    }

    public TypeWidenerSPI widenerFor(String columnName, Class columnType, Class writeablePropertyType, String writeablePropertyName, String statementName) throws TypeWidenerException {

        TypeWidenerSPI widener;
        try {
            ObjectValueTypeWidenerFactoryContext context = new ObjectValueTypeWidenerFactoryContext(columnType, writeablePropertyName, eventType, statementName);
            widener = factory.make(context);
        } catch (Throwable t) {
            throw new TypeWidenerException("Widener not available: " + t.getMessage(), t);
        }

        if (widener != null) {
            return widener;
        }
        return AvroTypeWidenerCustomizerDefault.INSTANCE.widenerFor(columnName, columnType, writeablePropertyType, writeablePropertyName, statementName); // default behavior applies otherwise
    }
}
