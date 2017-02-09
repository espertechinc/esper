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
package com.espertech.esper.epl.datetime.eval;

import com.espertech.esper.epl.datetime.calop.CalendarOpFactory;
import com.espertech.esper.epl.datetime.interval.IntervalOpFactory;
import com.espertech.esper.epl.datetime.reformatop.ReformatOpFactory;

public class DatetimeMethodEnumStatics {

    public final static OpFactory CALENDAR_OP_FACTORY = new CalendarOpFactory();
    public final static OpFactory REFORMAT_OP_FACTORY = new ReformatOpFactory();
    public final static OpFactory INTERVAL_OP_FACTORY = new IntervalOpFactory();
}
