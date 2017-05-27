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
package com.espertech.esper.supportregression.multithread;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

import java.util.LinkedList;
import java.util.List;

public class MTListener implements UpdateListener {
    private final String fieldName;
    private List values;

    public MTListener(String fieldName) {
        this.fieldName = fieldName;
        values = new LinkedList();
    }

    public void update(EventBean[] newEvents, EventBean[] oldEvents) {
        Object value = newEvents[0].get(fieldName);

        synchronized (values) {
            values.add(value);
        }
    }

    public List getValues() {
        return values;
    }
}
