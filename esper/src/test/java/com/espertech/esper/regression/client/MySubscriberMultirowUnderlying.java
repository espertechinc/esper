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
import com.espertech.esper.support.bean.SupportBean;

import java.util.ArrayList;

public class MySubscriberMultirowUnderlying
{
    private ArrayList<UniformPair<SupportBean[]>> indicateArr = new ArrayList<UniformPair<SupportBean[]>>();

    public void update(SupportBean[] newEvents, SupportBean[] oldEvents)
    {
        indicateArr.add(new UniformPair<SupportBean[]>(newEvents, oldEvents));
    }

    public ArrayList<UniformPair<SupportBean[]>> getIndicateArr()
    {
        return indicateArr;
    }

    public ArrayList<UniformPair<SupportBean[]>> getAndResetIndicateArr()
    {
        ArrayList<UniformPair<SupportBean[]>> result = indicateArr;
        indicateArr = new ArrayList<UniformPair<SupportBean[]>>();
        return result;
    }
}
