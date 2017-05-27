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
package com.espertech.esper.supportregression.patternassert;

import java.util.LinkedList;
import java.util.List;

public class CaseList {
    private LinkedList<EventExpressionCase> results;

    public CaseList() {
        results = new LinkedList<EventExpressionCase>();
    }

    public void addTest(EventExpressionCase desc) {
        results.add(desc);
    }

    public int getNumTests() {
        return results.size();
    }

    public List<EventExpressionCase> getResults() {
        return results;
    }
}
