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

public class MySubscriberMultirowObjectArr
{
    private ArrayList<UniformPair<Object[][]>> indicateArr = new ArrayList<UniformPair<Object[][]>>();

    public void update(Object[][] newEvents, Object[][] oldEvents)
    {
        indicateArr.add(new UniformPair<Object[][]>(newEvents, oldEvents));
    }

    public ArrayList<UniformPair<Object[][]>> getIndicateArr()
    {
        return indicateArr;
    }

    public ArrayList<UniformPair<Object[][]>> getAndResetIndicateArr()
    {
        ArrayList<UniformPair<Object[][]>> result = indicateArr;
        indicateArr = new ArrayList<UniformPair<Object[][]>>();
        return result;
    }
}
