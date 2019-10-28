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
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactory;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactoryFactory;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactoryFactoryContext;

public class PropertyHashedArrayFactoryFactory implements EventTableFactoryFactory {
    protected final int streamNum;
    protected final String[] propertyNames;
    protected final Class[] propertyTypes;
    protected final DataInputOutputSerde<Object>[] propertySerdes;
    protected final boolean unique;
    protected final EventPropertyValueGetter[] propertyGetters;
    protected final boolean isFireAndForget;

    public PropertyHashedArrayFactoryFactory(int streamNum, String[] propertyNames, Class[] propertyTypes, DataInputOutputSerde<Object>[] propertySerdes, boolean unique, EventPropertyValueGetter[] propertyGetters, boolean isFireAndForget) {
        this.streamNum = streamNum;
        this.propertyNames = propertyNames;
        this.propertyTypes = propertyTypes;
        this.propertySerdes = propertySerdes;
        this.unique = unique;
        this.propertyGetters = propertyGetters;
        this.isFireAndForget = isFireAndForget;
    }

    public EventTableFactory create(EventType eventType, EventTableFactoryFactoryContext eventTableFactoryContext) {
        return eventTableFactoryContext.getEventTableIndexService().createInArray(streamNum, eventType, propertyNames, propertyTypes, propertySerdes, unique, propertyGetters, isFireAndForget, eventTableFactoryContext);
    }
}
