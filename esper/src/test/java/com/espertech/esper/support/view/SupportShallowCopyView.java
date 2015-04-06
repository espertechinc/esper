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

package com.espertech.esper.support.view;

import java.util.Iterator;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.view.ViewSupport;

public class SupportShallowCopyView extends ViewSupport
{
    private String someReadWriteValue;
    private String someReadOnlyValue;
    private String someWriteOnlyValue;

    public SupportShallowCopyView(String someValue)
    {
        this.someReadWriteValue = someValue;
        this.someReadOnlyValue = someValue;
        this.someWriteOnlyValue = someValue;
    }

    public SupportShallowCopyView()
    {
    }

    public boolean isNullWriteOnlyValue()
    {
        return someWriteOnlyValue == null;
    }
    
    public String getSomeReadWriteValue()
    {
        return someReadWriteValue;
    }

    public void setSomeReadWriteValue(String someReadWriteValue)
    {
        this.someReadWriteValue = someReadWriteValue;
    }

    public String getSomeReadOnlyValue()
    {
        return someReadOnlyValue;
    }

    public void setSomeWriteOnlyValue(String someWriteOnlyValue)
    {
        this.someWriteOnlyValue = someWriteOnlyValue;
    }

    public void setParent()
    {
        throw new UnsupportedOperationException();
    }

    public void update(EventBean[] newData, EventBean[] oldData)
    {
    }

    public EventType getEventType()
    {
        return null;
    }

    public Iterator<EventBean> iterator()
    {
        return null;
    }
}
