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
package com.espertech.esper.regressionlib.support.bean;

import com.espertech.esper.common.client.util.DateTime;

import java.io.Serializable;

public class SupportEventIdWithTimestamp implements Serializable {
    private String id;
    private long mytimestamp;

    public SupportEventIdWithTimestamp(String id, long mytimestamp) {
        this.id = id;
        this.mytimestamp = mytimestamp;
    }

    public static SupportEventIdWithTimestamp makeTime(String id, String mytime) {
        long msec = DateTime.parseDefaultMSec("2002-05-1T" + mytime);
        return new SupportEventIdWithTimestamp(id, msec);
    }

    public String getId() {
        return id;
    }

    public long getMytimestamp() {
        return mytimestamp;
    }
}
