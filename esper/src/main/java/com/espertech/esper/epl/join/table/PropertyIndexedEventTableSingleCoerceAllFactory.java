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
package com.espertech.esper.epl.join.table;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

public class PropertyIndexedEventTableSingleCoerceAllFactory extends PropertyIndexedEventTableSingleCoerceAddFactory {
    /**
     * Ctor.
     *
     * @param streamNum    is the stream number of the indexed stream
     * @param eventType    is the event type of the indexed stream
     * @param coercionType are the classes to coerce indexed values to
     * @param propertyName property name
     */
    public PropertyIndexedEventTableSingleCoerceAllFactory(int streamNum, EventType eventType, String propertyName, Class coercionType) {
        super(streamNum, eventType, propertyName, coercionType);
    }

    public EventTable[] makeEventTables(EventTableFactoryTableIdent tableIdent, ExprEvaluatorContext exprEvaluatorContext) {
        EventTableOrganization organization = getOrganization();
        return new EventTable[]{new PropertyIndexedEventTableSingleCoerceAll(propertyGetter, organization, coercer, coercionType)};
    }

    protected EventTableOrganization getOrganization() {
        return new EventTableOrganization(optionalIndexName, unique, true, streamNum, new String[]{propertyName}, EventTableOrganizationType.HASH);
    }
}
