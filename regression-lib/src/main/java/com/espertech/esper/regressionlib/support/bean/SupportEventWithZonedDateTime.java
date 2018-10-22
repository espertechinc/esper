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

import java.io.Serializable;
import java.time.ZonedDateTime;

public class SupportEventWithZonedDateTime implements Serializable {
    private final ZonedDateTime zdt;

    public SupportEventWithZonedDateTime(ZonedDateTime zdt) {
        this.zdt = zdt;
    }

    public ZonedDateTime getZdt() {
        return zdt;
    }
}
