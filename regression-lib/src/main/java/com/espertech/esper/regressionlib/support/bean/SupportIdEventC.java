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
public class SupportIdEventC implements Serializable {
    private static final long serialVersionUID = 348251293095218023L;
    private final String id;
    private final String pc;

    public SupportIdEventC(String id, String pc) {
        this.id = id;
        this.pc = pc;
    }

    public String getId() {
        return id;
    }

    public String getPc() {
        return pc;
    }
}
