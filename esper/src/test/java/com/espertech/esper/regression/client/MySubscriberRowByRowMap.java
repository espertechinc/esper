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

package com.espertech.esper.regression.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MySubscriberRowByRowMap
{
    private ArrayList<Map> indicateIStream = new ArrayList<Map>();
    private ArrayList<Map> indicateRStream = new ArrayList<Map>();

    public void update(Map row)
    {
        indicateIStream.add(row);
    }

    public void updateRStream(Map row)
    {
        indicateRStream.add(row);
    }

    public List<Map> getAndResetIndicateIStream()
    {
        List<Map> result = indicateIStream;
        indicateIStream = new ArrayList<Map>();
        return result;
    }

    public List<Map> getAndResetIndicateRStream()
    {
        List<Map> result = indicateRStream;
        indicateRStream = new ArrayList<Map>();
        return result;
    }
}
