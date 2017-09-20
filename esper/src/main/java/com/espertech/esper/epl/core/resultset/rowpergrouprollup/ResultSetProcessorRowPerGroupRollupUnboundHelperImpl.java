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
package com.espertech.esper.epl.core.resultset.rowpergrouprollup;

import com.espertech.esper.client.EventBean;

import java.util.LinkedHashMap;
import java.util.Map;

public class ResultSetProcessorRowPerGroupRollupUnboundHelperImpl implements ResultSetProcessorRowPerGroupRollupUnboundHelper {

    private LinkedHashMap<Object, EventBean>[] eventPerGroupBuf;

    public ResultSetProcessorRowPerGroupRollupUnboundHelperImpl(int levelCount) {
        eventPerGroupBuf = (LinkedHashMap<Object, EventBean>[]) new LinkedHashMap[levelCount];
        for (int i = 0; i < levelCount; i++) {
            eventPerGroupBuf[i] = new LinkedHashMap<>();
        }
    }

    public Map<Object, EventBean>[] getBuffer() {
        return eventPerGroupBuf;
    }

    public void destroy() {
        // no action required
    }
}
