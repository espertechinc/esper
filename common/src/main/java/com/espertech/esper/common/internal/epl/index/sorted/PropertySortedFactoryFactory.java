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
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactory;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactoryFactoryBase;

public class PropertySortedFactoryFactory extends EventTableFactoryFactoryBase {

    private final String indexProp;
    private final Class indexType;
    private final EventPropertyValueGetter valueGetter;

    public PropertySortedFactoryFactory(int indexedStreamNum, Integer subqueryNum, Object optionalSerde, boolean isFireAndForget, String indexProp, Class indexType, EventPropertyValueGetter valueGetter) {
        super(indexedStreamNum, subqueryNum, optionalSerde, isFireAndForget);
        this.indexProp = indexProp;
        this.indexType = indexType;
        this.valueGetter = valueGetter;
    }

    public EventTableFactory create(EventType eventType, StatementContext statementContext) {
        return statementContext.getEventTableIndexService().createSorted(indexedStreamNum, eventType, indexProp, indexType,
                valueGetter, optionalSerde, isFireAndForget, statementContext);
    }
}
