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

public class SupportCarInfoEvent implements Serializable {
    private final String name;
    private final String place;
    private final String refId;

    public SupportCarInfoEvent(String name, String place, String refId) {
        this.name = name;
        this.place = place;
        this.refId = refId;
    }

    public String getName() {
        return name;
    }

    public String getPlace() {
        return place;
    }

    public String getRefId() {
        return refId;
    }
}
