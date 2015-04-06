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

package com.espertech.esper.support.bean;

public class SupportPositionReport
{
    private int VID;
    private int timestamp;
    private int spd;
    private int seg;

    public SupportPositionReport(int VID, int timestamp, int spd, int seg)
    {
        this.VID = VID;
        this.timestamp = timestamp;
        this.spd = spd;
        this.seg = seg;
    }

    public int getVID()
    {
        return VID;
    }

    public int getTimestamp()
    {
        return timestamp;
    }

    public int getSpd()
    {
        return spd;
    }

    public int getSeg()
    {
        return seg;
    }
}
