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
package com.espertech.esper.regressionlib.support.multistmtassert;

public class EPLWithInvokedFlags {
    private final String epl;
    private final boolean[] received;

    public EPLWithInvokedFlags(String epl, boolean[] received) {
        this.epl = epl;
        this.received = received;
    }

    public String epl() {
        return epl;
    }

    public boolean[] getReceived() {
        return received;
    }
}
