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

package com.espertech.esper.epl.datetime.calop;

import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.client.util.TimePeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;

public class CalendarOpPlusFastAddHelper {

    private static boolean DEBUG = false;
    private static Logger log = LoggerFactory.getLogger(CalendarOpPlusFastAddHelper.class);

    public static CalendarOpPlusFastAddResult computeNextDue(long currentTime, TimePeriod timePeriod, Calendar reference) {
        if (reference.getTimeInMillis() > currentTime) {
            return new CalendarOpPlusFastAddResult(0, reference);
        }

        // add one time period
        Calendar work = (Calendar) reference.clone();
        if (DEBUG && log.isDebugEnabled()) {
            log.debug("Work date is " + DateTime.print(work));
        }

        CalendarOpPlusMinus.actionSafeOverflow(work, 1, timePeriod);
        long inMillis = work.getTimeInMillis();
        if (inMillis > currentTime) {
            return new CalendarOpPlusFastAddResult(1, work);
        }
        if (DEBUG && log.isDebugEnabled()) {
            log.debug("Work date is " + DateTime.print(work));
        }

        long factor = 1;

        // determine multiplier
        long deltaCurrentToStart = currentTime - reference.getTimeInMillis();
        long deltaAddedOne = work.getTimeInMillis() - reference.getTimeInMillis();
        double multiplierDbl = (deltaCurrentToStart / deltaAddedOne) - 1;
        long multiplierRoundedLong = (long) multiplierDbl;

        // handle integer max
        while(multiplierRoundedLong > Integer.MAX_VALUE) {
            CalendarOpPlusMinus.actionSafeOverflow(work, Integer.MAX_VALUE, timePeriod);
            factor += Integer.MAX_VALUE;
            multiplierRoundedLong -= Integer.MAX_VALUE;
            if (DEBUG && log.isDebugEnabled()) {
                log.debug("Work date is " + DateTime.print(work) + " factor " + factor);
            }
        }

        // add
        int multiplierRoundedInt = (int) multiplierRoundedLong;
        CalendarOpPlusMinus.actionSafeOverflow(work, multiplierRoundedInt, timePeriod);
        factor += multiplierRoundedInt;

        // if below, add more
        if (work.getTimeInMillis() <= currentTime) {
            while(work.getTimeInMillis() <= currentTime) {
                CalendarOpPlusMinus.actionSafeOverflow(work, 1, timePeriod);
                factor += 1;
                if (DEBUG && log.isDebugEnabled()) {
                    log.debug("Work date is " + DateTime.print(work) + " factor " + factor);
                }
            }
            return new CalendarOpPlusFastAddResult(factor, work);
        }

        // we are over
        while(work.getTimeInMillis() > currentTime) {
            CalendarOpPlusMinus.actionSafeOverflow(work, -1, timePeriod);
            factor -= 1;
            if (DEBUG && log.isDebugEnabled()) {
                log.debug("Work date is " + DateTime.print(work) + " factor " + factor);
            }
        }
        CalendarOpPlusMinus.actionSafeOverflow(work, 1, timePeriod);
        if (DEBUG && log.isDebugEnabled()) {
            log.debug("Work date is " + DateTime.print(work) + " factor " + factor);
        }
        return new CalendarOpPlusFastAddResult(factor+1, work);
    }
}
