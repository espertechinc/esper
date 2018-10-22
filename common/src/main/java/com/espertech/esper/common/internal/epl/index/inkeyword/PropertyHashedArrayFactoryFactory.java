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
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactory;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactoryFactory;

public class PropertyHashedArrayFactoryFactory implements EventTableFactoryFactory {
    protected final int streamNum;
    protected final String[] propertyNames;
    protected final Class[] propertyTypes;
    protected final boolean unique;
    protected final EventPropertyValueGetter[] propertyGetters;
    protected final boolean isFireAndForget;

    public PropertyHashedArrayFactoryFactory(int streamNum, String[] propertyNames, Class[] propertyTypes, boolean unique, EventPropertyValueGetter[] propertyGetters, boolean isFireAndForget) {
        this.streamNum = streamNum;
        this.propertyNames = propertyNames;
        this.propertyTypes = propertyTypes;
        this.unique = unique;
        this.propertyGetters = propertyGetters;
        this.isFireAndForget = isFireAndForget;
    }

    public EventTableFactory create(EventType eventType, StatementContext statementContext) {
        return statementContext.getEventTableIndexService().createInArray(streamNum, eventType, propertyNames, propertyTypes, unique, propertyGetters, isFireAndForget, statementContext);
    }
}
