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
import com.espertech.esper.common.client.serde.MultiKeyGeneratedSerde;
import com.espertech.esper.common.internal.collection.MultiKeyGeneratedFromObjectArray;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactory;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactoryFactoryBase;

public class PropertyHashedFactoryFactory extends EventTableFactoryFactoryBase {

    private final String[] indexProps;
    private final Class[] indexTypes;
    private final boolean unique;
    private final EventPropertyValueGetter valueGetter;
    private final MultiKeyGeneratedFromObjectArray transformFireAndForget;
    private final MultiKeyGeneratedSerde optionalMultiKeySerde;

    public PropertyHashedFactoryFactory(int indexedStreamNum, Integer subqueryNum, Object optionalSerde, boolean isFireAndForget,
                                        String[] indexProps, Class[] indexTypes, boolean unique, EventPropertyValueGetter valueGetter,
                                        MultiKeyGeneratedFromObjectArray transformFireAndForget, MultiKeyGeneratedSerde optionalMultiKeySerde) {
        super(indexedStreamNum, subqueryNum, optionalSerde, isFireAndForget);
        this.indexProps = indexProps;
        this.indexTypes = indexTypes;
        this.unique = unique;
        this.valueGetter = valueGetter;
        this.transformFireAndForget = transformFireAndForget;
        this.optionalMultiKeySerde = optionalMultiKeySerde;
    }

    public EventTableFactory create(EventType eventType, StatementContext statementContext) {
        return statementContext.getEventTableIndexService().createHashedOnly(indexedStreamNum, eventType, indexProps,
                indexTypes, transformFireAndForget, optionalMultiKeySerde, unique, null, valueGetter, optionalSerde, isFireAndForget, statementContext);
    }
}
