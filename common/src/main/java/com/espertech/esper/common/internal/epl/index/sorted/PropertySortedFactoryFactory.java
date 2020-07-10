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
package com.espertech.esper.common.internal.epl.index.sorted;

import com.espertech.esper.common.client.EventPropertyValueGetter;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactory;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactoryFactoryBase;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactoryFactoryContext;
import com.espertech.esper.common.client.util.StateMgmtSetting;

public class PropertySortedFactoryFactory extends EventTableFactoryFactoryBase {
    public final static EPTypeClass EPTYPE = new EPTypeClass(PropertySortedFactoryFactory.class);

    private final String indexProp;
    private final EPTypeClass indexType;
    private final EventPropertyValueGetter valueGetter;
    private final DataInputOutputSerde<Object> indexSerde;
    private final StateMgmtSetting stateMgmtSettings;

    public PropertySortedFactoryFactory(int indexedStreamNum, Integer subqueryNum, boolean isFireAndForget, String indexProp, EPTypeClass indexType, EventPropertyValueGetter valueGetter, DataInputOutputSerde<Object> indexSerde, StateMgmtSetting stateMgmtSettings) {
        super(indexedStreamNum, subqueryNum, isFireAndForget);
        this.indexProp = indexProp;
        this.indexType = indexType;
        this.valueGetter = valueGetter;
        this.indexSerde = indexSerde;
        this.stateMgmtSettings = stateMgmtSettings;
    }

    public EventTableFactory create(EventType eventType, EventTableFactoryFactoryContext eventTableFactoryContext) {
        return eventTableFactoryContext.getEventTableIndexService().createSorted(indexedStreamNum, eventType, indexProp, indexType,
                valueGetter, indexSerde, null, isFireAndForget, eventTableFactoryContext, stateMgmtSettings);
    }
}
