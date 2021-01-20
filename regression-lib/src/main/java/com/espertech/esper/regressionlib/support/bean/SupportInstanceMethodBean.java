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

import com.espertech.esper.common.client.EventBean;

import java.io.Serializable;

/**
 * Test event; only serializable because it *may* go over the wire  when running remote tests and serialization is just convenient. Serialization generally not used for HA and HA testing.
 */
public class SupportInstanceMethodBean implements Serializable {
    private static final long serialVersionUID = -5055134445011825292L;
    private final int x;

    public SupportInstanceMethodBean(int x) {
        this.x = x;
    }

    public int getX() {
        return x;
    }

    public boolean myInstanceMethodAlwaysTrue() {
        return true;
    }

    public boolean myInstanceMethodEventBean(EventBean event, String propertyName, int expected) {
        Object value = event.get(propertyName);
        return value.equals(expected);
    }
}
