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
package com.espertech.esper.common.internal.epl.index.inkeyword;

import com.espertech.esper.common.client.EventPropertyValueGetter;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactory;
import com.espertech.esper.common.internal.epl.index.hash.PropertyHashedEventTable;
import com.espertech.esper.common.internal.epl.index.hash.PropertyHashedEventTableFactory;
import com.espertech.esper.common.internal.epl.index.hash.PropertyHashedEventTableUnadorned;
import com.espertech.esper.common.internal.epl.index.hash.PropertyHashedEventTableUnique;

import java.util.Arrays;

public class PropertyHashedArrayFactory implements EventTableFactory {
    protected final int streamNum;
    protected final String[] propertyNames;
    protected final boolean unique;
    protected final String optionalIndexName;
    protected final EventPropertyValueGetter[] propertyGetters;
    protected final PropertyHashedEventTableFactory[] factories;

    public PropertyHashedArrayFactory(int streamNum, String[] propertyNames, boolean unique, String optionalIndexName, EventPropertyValueGetter[] propertyGetters) {
        this.streamNum = streamNum;
        this.propertyNames = propertyNames;
        this.unique = unique;
        this.optionalIndexName = optionalIndexName;
        this.propertyGetters = propertyGetters;
        this.factories = new PropertyHashedEventTableFactory[propertyGetters.length];
        for (int i = 0; i < factories.length; i++) {
            factories[i] = new PropertyHashedEventTableFactory(streamNum, new String[]{propertyNames[i]}, unique, null, propertyGetters[i], null);
        }
    }

    public EventTable[] makeEventTables(AgentInstanceContext agentInstanceContext, Integer subqueryNumber) {
        EventTable[] tables = new EventTable[propertyGetters.length];
        if (unique) {
            for (int i = 0; i < tables.length; i++) {
                tables[i] = new PropertyHashedEventTableUnique(factories[i]);
            }
        } else {
            for (int i = 0; i < tables.length; i++) {
                tables[i] = new PropertyHashedEventTableUnadorned(factories[i]);
            }
        }
        return tables;
    }

    public Class getEventTableClass() {
        if (unique) {
            return PropertyHashedEventTableUnique.class;
        } else {
            return PropertyHashedEventTable.class;
        }
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() +
                (unique ? " unique" : " non-unique") +
                " streamNum=" + streamNum +
                " propertyNames=" + Arrays.toString(propertyNames);
    }
}
