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
public class SupportIdEventA implements Serializable {
    private static final long serialVersionUID = -5724991545710291778L;
    private final String id;
    private final String pa;
    private final Integer mysec;

    public SupportIdEventA(String id, String pa, Integer mysec) {
        this.id = id;
        this.pa = pa;
        this.mysec = mysec;
    }

    public String getId() {
        return id;
    }

    public String getPa() {
        return pa;
    }

    public Integer getMysec() {
        return mysec;
    }
}
