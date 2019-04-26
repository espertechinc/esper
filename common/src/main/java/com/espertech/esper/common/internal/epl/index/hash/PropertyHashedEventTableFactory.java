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
package com.espertech.esper.common.internal.epl.index.hash;

import com.espertech.esper.common.client.EventPropertyValueGetter;
import com.espertech.esper.common.internal.collection.MultiKeyFromObjectArray;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactory;
import com.espertech.esper.common.internal.epl.index.base.EventTableOrganization;
import com.espertech.esper.common.internal.epl.index.base.EventTableOrganizationType;

import java.util.Arrays;

public class PropertyHashedEventTableFactory implements EventTableFactory {
    protected final int streamNum;
    protected final String[] propertyNames;
    protected final boolean unique;
    protected final String optionalIndexName;
    protected final EventPropertyValueGetter propertyGetter;
    protected final MultiKeyFromObjectArray multiKeyTransform;

    public PropertyHashedEventTableFactory(int streamNum, String[] propertyNames, boolean unique, String optionalIndexName, EventPropertyValueGetter propertyGetter, MultiKeyFromObjectArray multiKeyTransform) {
        this.streamNum = streamNum;
        this.propertyNames = propertyNames;
        this.unique = unique;
        this.optionalIndexName = optionalIndexName;
        this.propertyGetter = propertyGetter;
        this.multiKeyTransform = multiKeyTransform;

        if (propertyGetter == null) {
            throw new IllegalArgumentException("Property-getter is null");
        }
    }

    public EventTable[] makeEventTables(AgentInstanceContext agentInstanceContext, Integer subqueryNumber) {
        if (unique) {
            return new EventTable[]{new PropertyHashedEventTableUnique(this)};
        } else {
            return new EventTable[]{new PropertyHashedEventTableUnadorned(this)};
        }
    }

    public Class getEventTableClass() {
        if (unique) {
            return PropertyHashedEventTableUnique.class;
        } else {
            return PropertyHashedEventTableUnadorned.class;
        }
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() +
                (unique ? " unique" : " non-unique") +
                " streamNum=" + streamNum +
                " propertyNames=" + Arrays.asList(propertyNames);
    }

    public EventTableOrganization getOrganization() {
        return new EventTableOrganization(optionalIndexName, unique, false, streamNum, propertyNames, EventTableOrganizationType.HASH);
    }
}
