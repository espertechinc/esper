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
import java.util.Set;

/**
 * Test event; only serializable because it *may* go over the wire  when running remote tests and serialization is just convenient. Serialization generally not used for HA and HA testing.
 */
public class SupportContainerLevel2Event implements Serializable {
    private static final long serialVersionUID = 3555694815372613827L;
    private final Set<String> multivalues;
    private final String singlevalue;

    public SupportContainerLevel2Event(Set<String> multivalues, String singlevalue) {
        this.multivalues = multivalues;
        this.singlevalue = singlevalue;
    }

    public Set<String> getMultivalues() {
        return multivalues;
    }

    public String getSinglevalue() {
        return singlevalue;
    }
}
