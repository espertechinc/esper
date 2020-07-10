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
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.collection.MultiKeyFromObjectArray;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactory;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactoryFactoryBase;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactoryFactoryContext;
import com.espertech.esper.common.client.util.StateMgmtSetting;

public class PropertyHashedFactoryFactory extends EventTableFactoryFactoryBase {
    public final static EPTypeClass EPTYPE = new EPTypeClass(PropertyHashedFactoryFactory.class);

    private final String[] indexProps;
    private final boolean unique;
    private final EventPropertyValueGetter valueGetter;
    private final MultiKeyFromObjectArray transformFireAndForget;
    private final DataInputOutputSerde<Object> keySerde;
    private final StateMgmtSetting stateMgmtSettings;

    public PropertyHashedFactoryFactory(int indexedStreamNum, Integer subqueryNum, boolean isFireAndForget,
                                        String[] indexProps, boolean unique, EventPropertyValueGetter valueGetter,
                                        MultiKeyFromObjectArray transformFireAndForget, DataInputOutputSerde<Object> keySerde,
                                        StateMgmtSetting stateMgmtSettings) {
        super(indexedStreamNum, subqueryNum, isFireAndForget);
        this.indexProps = indexProps;
        this.unique = unique;
        this.valueGetter = valueGetter;
        this.transformFireAndForget = transformFireAndForget;
        this.keySerde = keySerde;
        this.stateMgmtSettings = stateMgmtSettings;
    }

    public EventTableFactory create(EventType eventType, EventTableFactoryFactoryContext eventTableFactoryContext) {
        return eventTableFactoryContext.getEventTableIndexService().createHashedOnly(indexedStreamNum, eventType, indexProps,
                transformFireAndForget, keySerde, unique, null, valueGetter, null, isFireAndForget, stateMgmtSettings);
    }
}
