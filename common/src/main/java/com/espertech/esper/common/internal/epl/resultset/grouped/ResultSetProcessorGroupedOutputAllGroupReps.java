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
package com.espertech.esper.common.internal.epl.resultset.grouped;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorOutputHelper;

import java.util.Iterator;
import java.util.Map;

public interface ResultSetProcessorGroupedOutputAllGroupReps extends ResultSetProcessorOutputHelper {

    Object put(Object mk, EventBean[] array);

    void remove(Object key);

    Iterator<Map.Entry<Object, EventBean[]>> entryIterator();

    void destroy();
}
