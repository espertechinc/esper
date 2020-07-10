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
package com.espertech.esper.common.internal.epl.index.unindexed;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactory;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactoryFactoryBase;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactoryFactoryContext;
import com.espertech.esper.common.client.util.StateMgmtSetting;

public class UnindexedEventTableFactoryFactory extends EventTableFactoryFactoryBase {
    public final static EPTypeClass EPTYPE = new EPTypeClass(UnindexedEventTableFactoryFactory.class);

    private final StateMgmtSetting stateMgmtSettings;

    public UnindexedEventTableFactoryFactory(int indexedStreamNum, Integer subqueryNum, boolean isFireAndForget, StateMgmtSetting stateMgmtSettings) {
        super(indexedStreamNum, subqueryNum, isFireAndForget);
        this.stateMgmtSettings = stateMgmtSettings;
    }

    public EventTableFactory create(EventType eventType, EventTableFactoryFactoryContext eventTableFactoryContext) {
        return eventTableFactoryContext.getEventTableIndexService().createUnindexed(indexedStreamNum, eventType, null, isFireAndForget, stateMgmtSettings);
    }
}
