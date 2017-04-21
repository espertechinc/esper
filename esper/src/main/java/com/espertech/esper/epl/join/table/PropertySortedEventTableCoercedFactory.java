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

public class PropertySortedEventTableCoercedFactory extends PropertySortedEventTableFactory {
    protected Class coercionType;

    /**
     * Ctor.
     *
     * @param streamNum    - the stream number that is indexed
     * @param eventType    - types of events indexed
     * @param propertyName - property names to use for indexing
     * @param coercionType - property types
     */
    public PropertySortedEventTableCoercedFactory(int streamNum, EventType eventType, String propertyName, Class coercionType) {
        super(streamNum, eventType, propertyName);
        this.coercionType = coercionType;
    }

    @Override
    public EventTable[] makeEventTables(EventTableFactoryTableIdent tableIdent, ExprEvaluatorContext exprEvaluatorContext) {
        EventTableOrganization organization = getOrganization();
        return new EventTable[]{new PropertySortedEventTableCoerced(propertyGetter, organization, coercionType)};
    }

    public String toString() {
        return "PropertySortedEventTableCoerced" +
                " streamNum=" + streamNum +
                " propertyName=" + propertyName +
                " coercionType=" + coercionType;
    }

    protected EventTableOrganization getOrganization() {
        return new EventTableOrganization(null, false, true, streamNum, new String[]{propertyName}, EventTableOrganizationType.BTREE);
    }
}
