/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.join.table;

import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.event.EventBeanUtility;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Index that organizes events by the event property values into a single TreeMap sortable non-nested index
 * with Object keys that store the property values.
 */
public class PropertySortedEventTableFactory implements EventTableFactory
{
    protected final int streamNum;
    protected final String propertyName;

    /**
     * Getters for properties.
     */
    protected final EventPropertyGetter propertyGetter;

    /**
     * Ctor.
     * @param streamNum - the stream number that is indexed
     * @param eventType - types of events indexed
     */
    public PropertySortedEventTableFactory(int streamNum, EventType eventType, String propertyName)
    {
        this.streamNum = streamNum;
        this.propertyName = propertyName;
        propertyGetter = EventBeanUtility.getAssertPropertyGetter(eventType, propertyName);
    }

    public EventTable[] makeEventTables() {
        EventTableOrganization organization = new EventTableOrganization(null, false, false, streamNum, new String[] {propertyName}, EventTableOrganization.EventTableOrganizationType.BTREE);
        return new EventTable[] {new PropertySortedEventTable(propertyGetter, organization)};
    }

    public Class getEventTableClass() {
        return PropertySortedEventTable.class;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() +
                " streamNum=" + streamNum +
                " propertyName=" + propertyName;
    }

    private static Log log = LogFactory.getLog(PropertySortedEventTableFactory.class);
}
