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
package com.espertech.esperio.support.util;

import java.io.Serializable;

public class SupportSerializableBean implements Serializable {
    private String theString;

    public SupportSerializableBean(String theString) {
        this.theString = theString;
    }

    public String getTheString() {
        return theString;
    }
}
