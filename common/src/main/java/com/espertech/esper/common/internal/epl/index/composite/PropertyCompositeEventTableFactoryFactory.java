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
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactory;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactoryFactory;

public class PropertyCompositeEventTableFactoryFactory implements EventTableFactoryFactory {
    private final int indexedStreamNum;
    private final Integer subqueryNum;
    private final boolean isFireAndForget;
    private final String[] keyProps;
    private final Class[] keyTypes;
    private final EventPropertyValueGetter keyGetter;
    private final String[] rangeProps;
    private final Class[] rangeTypes;
    private final EventPropertyValueGetter[] rangeGetters;

    public PropertyCompositeEventTableFactoryFactory(int indexedStreamNum, Integer subqueryNum, boolean isFireAndForget, String[] keyProps, Class[] keyTypes, EventPropertyValueGetter keyGetter, String[] rangeProps, Class[] rangeTypes, EventPropertyValueGetter[] rangeGetters) {
        this.indexedStreamNum = indexedStreamNum;
        this.subqueryNum = subqueryNum;
        this.isFireAndForget = isFireAndForget;
        this.keyProps = keyProps;
        this.keyTypes = keyTypes;
        this.keyGetter = keyGetter;
        this.rangeProps = rangeProps;
        this.rangeTypes = rangeTypes;
        this.rangeGetters = rangeGetters;
    }

    public EventTableFactory create(EventType eventType, StatementContext statementContext) {
        return statementContext.getEventTableIndexService().createComposite(indexedStreamNum, eventType,
                keyProps, keyTypes, keyGetter,
                rangeProps, rangeTypes, rangeGetters,
                null, isFireAndForget);
    }
}
