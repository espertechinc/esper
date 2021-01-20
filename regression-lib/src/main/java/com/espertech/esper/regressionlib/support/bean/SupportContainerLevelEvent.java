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
public class SupportContainerLevelEvent implements Serializable {
    private static final long serialVersionUID = -9019953099221709600L;
    private final Set<SupportContainerLevel1Event> level1s;

    public SupportContainerLevelEvent(Set<SupportContainerLevel1Event> level1s) {
        this.level1s = level1s;
    }

    public Set<SupportContainerLevel1Event> getLevel1s() {
        return level1s;
    }
}
