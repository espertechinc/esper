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
package com.espertech.esper.supportunit.filter;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.filter.*;

import java.util.Collection;
import java.util.concurrent.locks.ReadWriteLock;

public class SupportFilterParamIndex extends FilterParamIndexLookupableBase
{
    public SupportFilterParamIndex(FilterSpecLookupable lookupable)
    {
        super(FilterOperator.EQUAL, lookupable);
    }

    public EventEvaluator get(Object expressionValue)
    {
        return null;
    }

    public void put(Object expressionValue, EventEvaluator evaluator)
    {
    }

    public boolean remove(Object expressionValue)
    {
        return true;
    }

    public int size()
    {
        return 0;
    }

    public ReadWriteLock getReadWriteLock()
    {
        return null;
    }

    public void matchEvent(EventBean theEvent, Collection<FilterHandle> matches)
    {
    }
}
