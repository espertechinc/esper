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
package com.espertech.esper.common.internal.context.util;

/**
 * Records minimal statement filter version required for processing.
 */
public class StatementAgentInstanceFilterVersion {

    private volatile long stmtFilterVersion;

    /**
     * Ctor.
     */
    public StatementAgentInstanceFilterVersion() {
        stmtFilterVersion = Long.MIN_VALUE;
    }

    /**
     * Set filter version.
     *
     * @param stmtFilterVersion to set
     */
    public void setStmtFilterVersion(long stmtFilterVersion) {
        this.stmtFilterVersion = stmtFilterVersion;
    }

    public long getStmtFilterVersion() {
        return stmtFilterVersion;
    }

    /**
     * Check current filter.
     *
     * @param filterVersion to check
     * @return false if not current
     */
    public boolean isCurrentFilter(long filterVersion) {
        if (filterVersion < stmtFilterVersion) {
            // catch-up in case of roll
            if (filterVersion + 100000 < stmtFilterVersion && stmtFilterVersion != Long.MAX_VALUE) {
                stmtFilterVersion = filterVersion;
            }
            return false;
        }
        return true;
    }
}
