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

import java.util.ArrayList;
import java.util.List;

public class SupportTestCaseHolder {

    private String measures;
    private String pattern;
    private List<SupportTestCaseItem> testcases;

    public SupportTestCaseHolder(String measures, String pattern) {
        this.measures = measures;
        this.pattern = pattern;
        this.testcases = new ArrayList<SupportTestCaseItem>();
    }

    public String getMeasures() {
        return measures;
    }

    public String getPattern() {
        return pattern;
    }

    public List<SupportTestCaseItem> getTestCases() {
        return testcases;
    }

    public SupportTestCaseHolder add(String testdataString, String[] expected) {
        testcases.add(new SupportTestCaseItem(testdataString, expected));
        return this;
    }
}
