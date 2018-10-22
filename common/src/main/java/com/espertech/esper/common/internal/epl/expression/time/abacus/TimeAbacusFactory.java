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
package com.espertech.esper.common.internal.epl.expression.time.abacus;

import com.espertech.esper.common.client.configuration.ConfigurationException;

import java.util.concurrent.TimeUnit;

public class TimeAbacusFactory {
    public static TimeAbacus make(TimeUnit timeUnit) {
        if (timeUnit == TimeUnit.MILLISECONDS) {
            return TimeAbacusMilliseconds.INSTANCE;
        } else if (timeUnit == TimeUnit.MICROSECONDS) {
            return TimeAbacusMicroseconds.INSTANCE;
        } else {
            throw new ConfigurationException("Invalid time-source time unit of " + timeUnit + ", expected millis or micros");
        }
    }
}
