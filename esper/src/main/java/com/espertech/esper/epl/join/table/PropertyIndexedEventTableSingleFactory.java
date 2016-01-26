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
 * Index factory that organizes events by the event property values into hash buckets. Based on a HashMap
 * with {@link com.espertech.esper.collection.MultiKeyUntyped} keys that store the property values.
 */
public class PropertyIndexedEventTableSingleFactory implements EventTableFactory
{
    protected final int streamNum;
    protected final String propertyName;
    protected final boolean unique;
    protected final String optionalIndexName;

    protected final EventPropertyGetter propertyGetter;

    /**
     * Ctor.
     * @param streamNum - the stream number that is indexed
     * @param eventType - types of events indexed
     */
    public PropertyIndexedEventTableSingleFactory(int streamNum, EventType eventType, String propertyName, boolean unique, String optionalIndexName)
    {
        this.streamNum = streamNum;
        this.propertyName = propertyName;
        this.unique = unique;
        this.optionalIndexName = optionalIndexName;

        // Init getters
        propertyGetter = EventBeanUtility.getAssertPropertyGetter(eventType, propertyName);
    }

    public EventTable[] makeEventTables(EventTableFactoryTableIdent tableIdent) {
        EventTableOrganization organization = new EventTableOrganization(optionalIndexName, unique, false, streamNum, new String[] {propertyName}, EventTableOrganizationType.HASH);
        if (unique) {
            return new EventTable[] {new PropertyIndexedEventTableSingleUnique(propertyGetter, organization)};
        }
        else {
            return new EventTable[] {new PropertyIndexedEventTableSingleUnadorned(propertyGetter, organization)};
        }
    }

    public Class getEventTableClass() {
        if (unique) {
            return PropertyIndexedEventTableSingleUnique.class;
        }
        else {
            return PropertyIndexedEventTableSingle.class;
        }
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() +
                (unique ? " unique" : " non-unique") +
                " streamNum=" + streamNum +
                " propertyName=" + propertyName;
    }

    private static Log log = LogFactory.getLog(PropertyIndexedEventTableSingleFactory.class);
}
