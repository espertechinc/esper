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
package com.espertech.esper.runtime.internal.kernel.faf;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.internal.collection.ArrayEventIterator;
import com.espertech.esper.common.internal.epl.fafquery.querymethod.EPPreparedQueryResult;

import java.util.Iterator;

/**
 * Query result.
 */
public class EPQueryResultImpl implements EPFireAndForgetQueryResult {
    private EPPreparedQueryResult queryResult;

    /**
     * Ctor.
     *
     * @param queryResult is the prepared query
     */
    public EPQueryResultImpl(EPPreparedQueryResult queryResult) {
        this.queryResult = queryResult;
    }

    public Iterator<EventBean> iterator() {
        return new ArrayEventIterator(queryResult.getResult());
    }

    public EventBean[] getArray() {
        return queryResult.getResult();
    }

    public EventType getEventType() {
        return queryResult.getEventType();
    }
}
