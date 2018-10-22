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
package com.espertech.esper.common.internal.epl.index.advanced.index.quadtree;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.index.base.EventTable;

import java.util.Collection;

public interface EventTableQuadTree extends EventTable {
    Collection<EventBean> queryRange(double x, double y, double width, double height);
}
