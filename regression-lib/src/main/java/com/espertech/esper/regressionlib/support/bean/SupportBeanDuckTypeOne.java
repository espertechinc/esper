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
public class SupportBeanDuckTypeOne implements SupportBeanDuckType, Serializable {
    private static final long serialVersionUID = -8866911270940523496L;
    private String stringValue;

    public SupportBeanDuckTypeOne(String stringValue) {
        this.stringValue = stringValue;
    }

    public String makeString() {
        return stringValue;
    }

    public Object makeCommon() {
        return new SupportBeanDuckTypeTwo(-1);
    }

    public double returnDouble() {
        return 12.9876d;
    }
}
