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

public class SupportCountAccessEvent implements Serializable {
    private static int countGetterCalled;

    private final int id;
    private final String p00;

    public SupportCountAccessEvent(int id, String p00) {
        this.id = id;
        this.p00 = p00;
    }

    public static int getAndResetCountGetterCalled() {
        int value = countGetterCalled;
        countGetterCalled = 0;
        return value;
    }

    public int getId() {
        return id;
    }

    public String getP00() {
        countGetterCalled++;
        return p00;
    }
}
