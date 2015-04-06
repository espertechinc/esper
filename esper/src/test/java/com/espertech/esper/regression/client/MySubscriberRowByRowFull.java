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
import java.util.List;

public class MySubscriberRowByRowFull
{
    private ArrayList<UniformPair<Integer>> indicateStart = new ArrayList<UniformPair<Integer>>();
    private ArrayList<Object> indicateEnd = new ArrayList<Object>();
    private ArrayList<Object[]> indicateIStream = new ArrayList<Object[]>();
    private ArrayList<Object[]> indicateRStream = new ArrayList<Object[]>();

    public void updateStart(int lengthIStream, int lengthRStream)
    {
        indicateStart.add(new UniformPair<Integer>(lengthIStream, lengthRStream));
    }

    public void update(String theString, int intPrimitive)
    {
        indicateIStream.add(new Object[] {theString, intPrimitive});
    }

    public void updateRStream(String theString, int intPrimitive)
    {
        indicateRStream.add(new Object[] {theString, intPrimitive});
    }

    public void updateEnd()
    {
        indicateEnd.add(this);
    }

    public List<UniformPair<Integer>> getAndResetIndicateStart()
    {
        List<UniformPair<Integer>> result = indicateStart;
        indicateStart = new ArrayList<UniformPair<Integer>>();
        return result;
    }

    public List<Object[]> getAndResetIndicateIStream()
    {
        List<Object[]> result = indicateIStream;
        indicateIStream = new ArrayList<Object[]>();
        return result;
    }

    public List<Object[]> getAndResetIndicateRStream()
    {
        List<Object[]> result = indicateRStream;
        indicateRStream = new ArrayList<Object[]>();
        return result;
    }

    public List<Object> getAndResetIndicateEnd()
    {
        List<Object> result = indicateEnd;
        indicateEnd = new ArrayList<Object>();
        return result;
    }

    public ArrayList<UniformPair<Integer>> getIndicateStart() {
        return indicateStart;
    }

    public ArrayList<Object> getIndicateEnd() {
        return indicateEnd;
    }

    public ArrayList<Object[]> getIndicateIStream() {
        return indicateIStream;
    }

    public ArrayList<Object[]> getIndicateRStream() {
        return indicateRStream;
    }
}
