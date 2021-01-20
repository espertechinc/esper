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

import com.espertech.esper.common.internal.support.SupportEnum;

import java.io.Serializable;

/**
 * Test event; only serializable because it *may* go over the wire  when running remote tests and serialization is just convenient. Serialization generally not used for HA and HA testing.
 */
public class SupportBeanWithEnum implements Serializable {
    private static final long serialVersionUID = 1652982399263886479L;
    private String theString;
    private SupportEnum supportEnum;

    public SupportBeanWithEnum(String theString, SupportEnum supportEnum) {
        this.theString = theString;
        this.supportEnum = supportEnum;
    }

    public String getTheString() {
        return theString;
    }

    public SupportEnum getSupportEnum() {
        return supportEnum;
    }
}
