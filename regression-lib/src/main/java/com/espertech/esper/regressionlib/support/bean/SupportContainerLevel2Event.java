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

public class SupportContainerLevel2Event implements Serializable {
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
