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
package com.espertech.esper.common.internal.context.controller.initterm;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.LRUCache;
import com.espertech.esper.common.internal.context.mgr.ContextManagerRealization;

public interface ContextControllerInitTermWDistinct {
    Object getDistinctKey(EventBean eventBean);

    LRUCache<Object, EventBean> getDistinctLastTriggerEvents();

    ContextManagerRealization getRealization();
}
