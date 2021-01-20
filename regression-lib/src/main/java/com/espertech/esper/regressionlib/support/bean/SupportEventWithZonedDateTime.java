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

/**
 * Test event; only serializable because it *may* go over the wire  when running remote tests and serialization is just convenient. Serialization generally not used for HA and HA testing.
 */
public class SupportEventWithZonedDateTime implements Serializable {
    private static final long serialVersionUID = -8506573792738188220L;
    private final ZonedDateTime zdt;

    public SupportEventWithZonedDateTime(ZonedDateTime zdt) {
        this.zdt = zdt;
    }

    public ZonedDateTime getZdt() {
        return zdt;
    }
}
