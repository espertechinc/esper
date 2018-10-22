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

public class SupportContainerLevel1Event implements Serializable {
    private final Set<SupportContainerLevel2Event> level2s;

    public SupportContainerLevel1Event(Set<SupportContainerLevel2Event> level2s) {
        this.level2s = level2s;
    }

    public Set<SupportContainerLevel2Event> getLevel2s() {
        return level2s;
    }
}
