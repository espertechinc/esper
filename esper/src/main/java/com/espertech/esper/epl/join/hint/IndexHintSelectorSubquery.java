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
package com.espertech.esper.epl.join.hint;

public class IndexHintSelectorSubquery implements IndexHintSelector {
    private final int subqueryNum;

    public IndexHintSelectorSubquery(int subqueryNum) {
        this.subqueryNum = subqueryNum;
    }

    public int getSubqueryNum() {
        return subqueryNum;
    }

    public boolean matchesSubquery(int subqueryNumber) {
        return subqueryNum == subqueryNumber;
    }
}
