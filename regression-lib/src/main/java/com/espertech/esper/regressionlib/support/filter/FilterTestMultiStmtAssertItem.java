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
package com.espertech.esper.regressionlib.support.filter;

import com.espertech.esper.common.internal.support.SupportBean;

public class FilterTestMultiStmtAssertItem {
    private final SupportBean bean;
    private final boolean[] expectedPerStmt;

    public FilterTestMultiStmtAssertItem(SupportBean bean, boolean... expectedPerStmt) {
        this.bean = bean;
        this.expectedPerStmt = expectedPerStmt;
    }

    public SupportBean getBean() {
        return bean;
    }

    public boolean[] getExpectedPerStmt() {
        return expectedPerStmt;
    }

    public static FilterTestMultiStmtAssertItem makeItem(SupportBean bean, boolean... expectedPerStmt) {
        return new FilterTestMultiStmtAssertItem(bean, expectedPerStmt);
    }
}
