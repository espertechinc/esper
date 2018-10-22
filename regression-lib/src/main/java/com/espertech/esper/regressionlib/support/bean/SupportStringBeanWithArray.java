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

public class SupportStringBeanWithArray implements Serializable {
    private final String topId;
    private final String[] containedIds;

    public SupportStringBeanWithArray(String topId, String[] containedIds) {
        this.topId = topId;
        this.containedIds = containedIds;
    }

    public String getTopId() {
        return topId;
    }

    public String[] getContainedIds() {
        return containedIds;
    }
}
