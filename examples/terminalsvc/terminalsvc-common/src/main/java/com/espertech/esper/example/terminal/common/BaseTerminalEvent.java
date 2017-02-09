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
package com.espertech.esper.example.terminal.common;

import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;

public abstract class BaseTerminalEvent implements Serializable {
    private final TerminalInfo term;

    public BaseTerminalEvent(TerminalInfo term) {
        this.term = term;
    }

    public TerminalInfo getTerm() {
        return term;
    }

    public long getTimeMinute() {
        Calendar calendar = GregorianCalendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int min = calendar.get(Calendar.MINUTE);

        return hour * 100 + min;
    }

    public String getType() {
        return this.getClass().getSimpleName();
    }
}
