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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Index factory that organizes events by the event property values into hash buckets. Based on a HashMap
 * with {@link com.espertech.esper.collection.MultiKeyUntyped} keys that store the property values.
 */
public class PropertyIndexedEventTableSingleArrayFactory implements EventTableFactory {
    protected final int streamNum;
    protected final String[] propertyNames;
    protected final boolean unique;
    protected final String optionalIndexName;

    protected final EventPropertyGetter[] propertyGetters;

    public PropertyIndexedEventTableSingleArrayFactory(int streamNum, EventType eventType, String[] propertyNames, boolean unique, String optionalIndexName) {
        this.streamNum = streamNum;
        this.propertyNames = propertyNames;
        this.unique = unique;
        this.optionalIndexName = optionalIndexName;

        // Init getters
        propertyGetters = new EventPropertyGetter[propertyNames.length];
        for (int i = 0; i < propertyNames.length; i++) {
            propertyGetters[i] = EventBeanUtility.getAssertPropertyGetter(eventType, propertyNames[i]);
        }
    }

    public EventTable[] makeEventTables(EventTableFactoryTableIdent tableIdent, ExprEvaluatorContext exprEvaluatorContext) {
        EventTable[] tables = new EventTable[propertyGetters.length];
        if (unique) {
            for (int i = 0; i < tables.length; i++) {
                EventTableOrganization organization = new EventTableOrganization(optionalIndexName, unique, false, streamNum, new String[]{propertyNames[i]}, EventTableOrganizationType.HASH);
                tables[i] = new PropertyIndexedEventTableSingleUnique(propertyGetters[i], organization);
            }
        } else {
            for (int i = 0; i < tables.length; i++) {
                EventTableOrganization organization = new EventTableOrganization(optionalIndexName, unique, false, streamNum, new String[]{propertyNames[i]}, EventTableOrganizationType.HASH);
                tables[i] = new PropertyIndexedEventTableSingleUnadorned(propertyGetters[i], organization);
            }
        }
        return tables;
    }

    public Class getEventTableClass() {
        if (unique) {
            return PropertyIndexedEventTableSingleUnique.class;
        } else {
            return PropertyIndexedEventTableSingle.class;
        }
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() +
                (unique ? " unique" : " non-unique") +
                " streamNum=" + streamNum +
                " propertyNames=" + Arrays.toString(propertyNames);
    }

    private final static Logger log = LoggerFactory.getLogger(PropertyIndexedEventTableSingleArrayFactory.class);
}
