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
package com.espertech.esper.avro.core;

import com.espertech.esper.client.EventType;
import com.espertech.esper.client.hook.ObjectValueTypeWidenerFactory;
import com.espertech.esper.client.hook.ObjectValueTypeWidenerFactoryContext;
import com.espertech.esper.util.TypeWidener;
import com.espertech.esper.util.TypeWidenerCustomizer;
import com.espertech.esper.util.TypeWidenerException;

public class AvroTypeWidenerCustomizerWHook implements TypeWidenerCustomizer {
    private final ObjectValueTypeWidenerFactory factory;
    private final EventType eventType;

    public AvroTypeWidenerCustomizerWHook(ObjectValueTypeWidenerFactory factory, EventType eventType) {
        this.factory = factory;
        this.eventType = eventType;
    }

    public TypeWidener widenerFor(String columnName, Class columnType, Class writeablePropertyType, String writeablePropertyName, String statementName, String engineURI) throws TypeWidenerException {

        TypeWidener widener;
        try {
            ObjectValueTypeWidenerFactoryContext context = new ObjectValueTypeWidenerFactoryContext(columnType, writeablePropertyName, eventType, statementName, engineURI);
            widener = factory.make(context);
        } catch (Throwable t) {
            throw new TypeWidenerException("Widener not available: " + t.getMessage(), t);
        }

        if (widener != null) {
            return widener;
        }
        return AvroTypeWidenerCustomizerDefault.INSTANCE.widenerFor(columnName, columnType, writeablePropertyType, writeablePropertyName, statementName, engineURI); // default behavior applies otherwise
    }
}
