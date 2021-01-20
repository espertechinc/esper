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

/**
 * Test event; only serializable because it *may* go over the wire  when running remote tests and serialization is just convenient. Serialization generally not used for HA and HA testing.
 */
public class SupportCarEvent implements Serializable {
    private static final long serialVersionUID = 4462687158141409866L;
    private final String name;
    private final String place;
    private final int count;

    public SupportCarEvent(String name, String place, int count) {
        this.name = name;
        this.place = place;
        this.count = count;
    }

    public String getName() {
        return name;
    }

    public String getPlace() {
        return place;
    }

    public int getCount() {
        return count;
    }
}
