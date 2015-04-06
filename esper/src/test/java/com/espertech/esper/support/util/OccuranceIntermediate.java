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

package com.espertech.esper.support.util;

import com.espertech.esper.collection.Pair;
import com.espertech.esper.client.EventBean;

import java.util.List;
import java.util.ArrayList;

public class OccuranceIntermediate
{
    private Long low;
    private Long high;
    private List<Pair<Long, EventBean[]>> items;

    public OccuranceIntermediate(Long low, Long high)
    {
        this.low = low;
        this.high = high;
        this.items = new ArrayList<Pair<Long, EventBean[]>>();
    }

    public List<Pair<Long, EventBean[]>> getItems()
    {
        return items;
    }

    public Long getLow()
    {
        return low;
    }

    public Long getHigh()
    {
        return high;
    }
}
