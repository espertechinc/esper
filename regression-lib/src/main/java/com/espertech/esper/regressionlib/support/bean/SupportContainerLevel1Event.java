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
public class SupportContainerLevel1Event implements Serializable {
    private static final long serialVersionUID = -3363518150478206014L;
    private final Set<SupportContainerLevel2Event> level2s;

    public SupportContainerLevel1Event(Set<SupportContainerLevel2Event> level2s) {
        this.level2s = level2s;
    }

    public Set<SupportContainerLevel2Event> getLevel2s() {
        return level2s;
    }
}
