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
package com.espertech.esper.supportregression.rowrecog;

public class SupportTestCaseItem {
    private String testdata;
    private String[] expected;

    public SupportTestCaseItem(String testdata, String[] expected) {
        this.testdata = testdata;
        this.expected = expected;
    }

    public String getTestdata() {
        return testdata;
    }

    public String[] getExpected() {
        return expected;
    }
}
