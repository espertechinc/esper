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

import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBeanComplexProps;
import com.espertech.esper.support.bean.SupportEnum;
import com.espertech.esper.support.bean.SupportMarketDataBean;

import java.util.ArrayList;
import java.util.List;

public class MySubscriberRowByRowSpecific
{
    private ArrayList<Object[]> indicate = new ArrayList<Object[]>();

    public void update(String theString, int intPrimitive)
    {
        indicate.add(new Object[] {theString, intPrimitive});
    }

    public void update(int wideByte, long wideInt, double wideLong, double wideFloat)
    {
        indicate.add(new Object[] {wideByte, wideInt, wideLong, wideFloat});
    }

    public void update(SupportBean supportBean)
    {
        indicate.add(new Object[] {supportBean});
    }

    public void update(SupportBean supportBean, int value1, String value2)
    {
        indicate.add(new Object[] {supportBean, value1, value2});
    }

    public void update(SupportBeanComplexProps.SupportBeanSpecialGetterNested n,
                               SupportBeanComplexProps.SupportBeanSpecialGetterNestedNested nn)
    {
        indicate.add(new Object[] {n, nn});
    }

    public void update(String theString, SupportEnum supportEnum)
    {
        indicate.add(new Object[] {theString, supportEnum});
    }

    public void update(String nullableValue, Long longBoxed)
    {
        indicate.add(new Object[] {nullableValue, longBoxed});
    }

    public void update(String value, SupportMarketDataBean s1, SupportBean s0)
    {
        indicate.add(new Object[] {value, s1, s0});
    }

    public void update(SupportBean s0, SupportMarketDataBean s1)
    {
        indicate.add(new Object[] {s0, s1});
    }

    public List<Object[]> getAndResetIndicate()
    {
        List<Object[]> result = indicate;
        indicate = new ArrayList<Object[]>();
        return result;
    }
}
