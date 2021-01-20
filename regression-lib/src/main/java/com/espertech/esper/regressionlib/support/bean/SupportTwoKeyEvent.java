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
public class SupportTwoKeyEvent implements Serializable {
    private static final long serialVersionUID = -4877772826537106916L;
    private final String k1;
    private final int k2;
    private final int newValue;

    public SupportTwoKeyEvent(String k1, int k2, int newValue) {
        this.k1 = k1;
        this.k2 = k2;
        this.newValue = newValue;
    }

    public String getK1() {
        return k1;
    }

    public int getK2() {
        return k2;
    }

    public int getNewValue() {
        return newValue;
    }
}
