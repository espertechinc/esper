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
package com.espertech.esper.common.internal.epl.namedwindow.consume;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraph;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Iterator;

public interface NamedWindowConsumerCallback {
    public Iterator<EventBean> getIterator();

    public void stopped(NamedWindowConsumerView namedWindowConsumerView);

    public boolean isParentBatchWindow();

    Collection<EventBean> snapshot(QueryGraph queryGraph, Annotation[] annotations);
}
