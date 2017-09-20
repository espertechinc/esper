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
package com.espertech.esper.epl.core.resultset.grouped;

import com.espertech.esper.client.EventBean;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class ResultSetProcessorGroupedOutputAllGroupRepsImpl implements ResultSetProcessorGroupedOutputAllGroupReps {

    private final Map<Object, EventBean[]> groupRepsView = new LinkedHashMap<>();

    public Object put(Object mk, EventBean[] array) {
        return groupRepsView.put(mk, array);
    }

    public void remove(Object key) {
        groupRepsView.remove(key);
    }

    public Iterator<Map.Entry<Object, EventBean[]>> entryIterator() {
        return groupRepsView.entrySet().iterator();
    }

    public void destroy() {
        // no action required
    }
}
