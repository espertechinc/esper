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

import com.espertech.esper.collection.UniformPair;

import java.util.ArrayList;
import java.util.Map;

public class MySubscriberMultirowMap
{
    private ArrayList<UniformPair<Map[]>> indicateMap = new ArrayList<UniformPair<Map[]>>();

    public void update(Map[] newEvents, Map[] oldEvents)
    {
        indicateMap.add(new UniformPair<Map[]>(newEvents, oldEvents));
    }

    public ArrayList<UniformPair<Map[]>> getIndicateMap()
    {
        return indicateMap;
    }

    public ArrayList<UniformPair<Map[]>> getAndResetIndicateMap()
    {
        ArrayList<UniformPair<Map[]>> result = indicateMap;
        indicateMap = new ArrayList<UniformPair<Map[]>>();
        return result;
    }
}
