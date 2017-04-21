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

import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.EventBeanUtility;

/**
 * Index that organizes events by the event property values into a single TreeMap sortable non-nested index
 * with Object keys that store the property values.
 */
public class PropertySortedEventTableFactory implements EventTableFactory {
    protected final int streamNum;
    protected final String propertyName;

    /**
     * Getters for properties.
     */
    protected final EventPropertyGetter propertyGetter;

    /**
     * Ctor.
     *
     * @param streamNum    - the stream number that is indexed
     * @param eventType    - types of events indexed
     * @param propertyName - property name
     */
    public PropertySortedEventTableFactory(int streamNum, EventType eventType, String propertyName) {
        this.streamNum = streamNum;
        this.propertyName = propertyName;
        propertyGetter = EventBeanUtility.getAssertPropertyGetter(eventType, propertyName);
    }

    public EventTable[] makeEventTables(EventTableFactoryTableIdent tableIdent, ExprEvaluatorContext exprEvaluatorContext) {
        EventTableOrganization organization = getOrganization();
        return new EventTable[]{new PropertySortedEventTableImpl(propertyGetter, organization)};
    }

    public Class getEventTableClass() {
        return PropertySortedEventTable.class;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() +
                " streamNum=" + streamNum +
                " propertyName=" + propertyName;
    }

    public int getStreamNum() {
        return streamNum;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public EventPropertyGetter getPropertyGetter() {
        return propertyGetter;
    }

    protected EventTableOrganization getOrganization() {
        return new EventTableOrganization(null, false, false, streamNum, new String[]{propertyName}, EventTableOrganizationType.BTREE);
    }
}
