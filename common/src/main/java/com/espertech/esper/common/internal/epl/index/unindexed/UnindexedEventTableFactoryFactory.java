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
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactory;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactoryFactoryBase;

public class UnindexedEventTableFactoryFactory extends EventTableFactoryFactoryBase {

    public UnindexedEventTableFactoryFactory(int indexedStreamNum, Integer subqueryNum, Object optionalSerde, boolean isFireAndForget) {
        super(indexedStreamNum, subqueryNum, optionalSerde, isFireAndForget);
    }

    public EventTableFactory create(EventType eventType, StatementContext statementContext) {
        return statementContext.getEventTableIndexService().createUnindexed(indexedStreamNum, eventType, optionalSerde, isFireAndForget, statementContext);
    }
}
