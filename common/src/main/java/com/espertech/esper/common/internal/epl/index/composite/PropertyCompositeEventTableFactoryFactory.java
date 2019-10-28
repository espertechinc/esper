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
package com.espertech.esper.common.internal.epl.index.composite;

import com.espertech.esper.common.client.EventPropertyValueGetter;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactory;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactoryFactory;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactoryFactoryContext;

public class PropertyCompositeEventTableFactoryFactory implements EventTableFactoryFactory {
    private final int indexedStreamNum;
    private final Integer subqueryNum;
    private final boolean isFireAndForget;
    private final String[] keyProps;
    private final Class[] keyTypes;
    private final EventPropertyValueGetter keyGetter;
    private final DataInputOutputSerde<Object> keySerde;
    private final String[] rangeProps;
    private final Class[] rangeTypes;
    private final EventPropertyValueGetter[] rangeGetters;
    private final DataInputOutputSerde<Object>[] rangeKeySerdes;

    public PropertyCompositeEventTableFactoryFactory(int indexedStreamNum, Integer subqueryNum, boolean isFireAndForget, String[] keyProps, Class[] keyTypes, EventPropertyValueGetter keyGetter, DataInputOutputSerde<Object> keySerde, String[] rangeProps, Class[] rangeTypes, EventPropertyValueGetter[] rangeGetters, DataInputOutputSerde<Object>[] rangeKeySerdes) {
        this.indexedStreamNum = indexedStreamNum;
        this.subqueryNum = subqueryNum;
        this.isFireAndForget = isFireAndForget;
        this.keyProps = keyProps;
        this.keyTypes = keyTypes;
        this.keyGetter = keyGetter;
        this.keySerde = keySerde;
        this.rangeProps = rangeProps;
        this.rangeTypes = rangeTypes;
        this.rangeGetters = rangeGetters;
        this.rangeKeySerdes = rangeKeySerdes;
    }

    public EventTableFactory create(EventType eventType, EventTableFactoryFactoryContext eventTableFactoryContext) {
        return eventTableFactoryContext.getEventTableIndexService().createComposite(indexedStreamNum, eventType,
                keyProps, keyTypes, keyGetter, null, keySerde,
                rangeProps, rangeTypes, rangeGetters, rangeKeySerdes,
                null, isFireAndForget);
    }
}
