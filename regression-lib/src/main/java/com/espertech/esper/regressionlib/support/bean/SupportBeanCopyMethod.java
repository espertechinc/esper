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
public class SupportBeanCopyMethod implements Serializable {
    private static final long serialVersionUID = -5033276410791065014L;
    private String valOne;
    private String valTwo;

    public SupportBeanCopyMethod(String valOne, String valTwo) {
        this.valOne = valOne;
        this.valTwo = valTwo;
    }

    public String getValOne() {
        return valOne;
    }

    public void setValOne(String valOne) {
        this.valOne = valOne;
    }

    public String getValTwo() {
        return valTwo;
    }

    public void setValTwo(String valTwo) {
        this.valTwo = valTwo;
    }

    public SupportBeanCopyMethod myCopyMethod() {
        return new SupportBeanCopyMethod(valOne, valTwo);
    }
}
