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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Index factory that organizes events by the event property values into hash buckets. Based on a HashMap
 * with {@link com.espertech.esper.collection.MultiKeyUntyped} keys that store the property values.
 * <p>
 * Takes a list of property names as parameter. Doesn't care which event type the events have as long as the properties
 * exist. If the same event is added twice, the class throws an exception on add.
 */
public class PropertyIndexedEventTableFactory implements EventTableFactory {
    protected final int streamNum;
    protected final String[] propertyNames;
    protected final boolean unique;
    protected final String optionalIndexName;

    /**
     * Getters for properties.
     */
    protected final EventPropertyGetter[] propertyGetters;

    /**
     * Ctor.
     *
     * @param streamNum         - the stream number that is indexed
     * @param eventType         - types of events indexed
     * @param propertyNames     - property names to use for indexing
     * @param unique            unique flag
     * @param optionalIndexName index name
     */
    public PropertyIndexedEventTableFactory(int streamNum, EventType eventType, String[] propertyNames, boolean unique, String optionalIndexName) {
        this.streamNum = streamNum;
        this.propertyNames = propertyNames;
        this.unique = unique;
        this.optionalIndexName = optionalIndexName;

        // Init getters
        propertyGetters = new EventPropertyGetter[propertyNames.length];
        for (int i = 0; i < propertyNames.length; i++) {
            propertyGetters[i] = eventType.getGetter(propertyNames[i]);
        }
    }

    public EventTable[] makeEventTables(EventTableFactoryTableIdent tableIdent, ExprEvaluatorContext exprEvaluatorContext) {
        EventTableOrganization organization = getOrganization();
        if (unique) {
            return new EventTable[]{new PropertyIndexedEventTableUnique(propertyGetters, organization)};
        } else {
            return new EventTable[]{new PropertyIndexedEventTableUnadorned(propertyGetters, organization)};
        }
    }

    public Class getEventTableClass() {
        if (unique) {
            return PropertyIndexedEventTableUnique.class;
        } else {
            return PropertyIndexedEventTableUnadorned.class;
        }
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() +
                (unique ? " unique" : " non-unique") +
                " streamNum=" + streamNum +
                " propertyNames=" + Arrays.toString(propertyNames);
    }

    public int getStreamNum() {
        return streamNum;
    }

    public String[] getPropertyNames() {
        return propertyNames;
    }

    public boolean isUnique() {
        return unique;
    }

    public String getOptionalIndexName() {
        return optionalIndexName;
    }

    public EventPropertyGetter[] getPropertyGetters() {
        return propertyGetters;
    }

    protected EventTableOrganization getOrganization() {
        return new EventTableOrganization(optionalIndexName, unique, false,
                streamNum, propertyNames, EventTableOrganizationType.HASH);
    }

    private final static Logger log = LoggerFactory.getLogger(PropertyIndexedEventTableFactory.class);
}
