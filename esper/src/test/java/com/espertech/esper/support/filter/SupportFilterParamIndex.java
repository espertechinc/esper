/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.support.filter;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.filter.*;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.event.SupportEventTypeFactory;

import java.util.Collection;
import java.util.concurrent.locks.ReadWriteLock;

public class SupportFilterParamIndex extends FilterParamIndexLookupableBase
{
    public SupportFilterParamIndex(FilterSpecLookupable lookupable)
    {
        super(FilterOperator.EQUAL, lookupable);
    }

    protected EventEvaluator get(Object expressionValue)
    {
        return null;
    }

    protected void put(Object expressionValue, EventEvaluator evaluator)
    {
    }

    protected boolean remove(Object expressionValue)
    {
        return true;
    }

    protected int size()
    {
        return 0;
    }

    protected ReadWriteLock getReadWriteLock()
    {
        return null;
    }

    public void matchEvent(EventBean theEvent, Collection<FilterHandle> matches)
    {
    }
}
