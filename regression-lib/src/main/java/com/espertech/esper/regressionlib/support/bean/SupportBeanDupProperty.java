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
public class SupportBeanDupProperty implements Serializable {
    private static final long serialVersionUID = 2192605371545498346L;
    private String myProperty;
    private String MyProperty;
    private String MYPROPERTY;
    private String myproperty;

    public SupportBeanDupProperty(String myProperty, String MyProperty, String MYPROPERTY, String myproperty) {
        this.myProperty = myProperty;
        this.MyProperty = MyProperty;
        this.MYPROPERTY = MYPROPERTY;
        this.myproperty = myproperty;
    }

    public String getmyProperty() {
        return myProperty;
    }

    public String getMyProperty() {
        return MyProperty;
    }

    public String getMYPROPERTY() {
        return MYPROPERTY;
    }

    public String getMyproperty() {
        return myproperty;
    }
}
