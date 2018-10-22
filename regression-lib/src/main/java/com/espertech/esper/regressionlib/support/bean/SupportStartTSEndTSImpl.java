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

public class SupportStartTSEndTSImpl implements SupportStartTSEndTSInterface {
    private final long start;
    private final long end;

    public SupportStartTSEndTSImpl(String datestr, long duration) {
        start = DateTime.parseDefaultMSec(datestr);
        end = start + duration;
    }

    public long getStartTS() {
        return start;
    }

    public long getEndTS() {
        return end;
    }
}
