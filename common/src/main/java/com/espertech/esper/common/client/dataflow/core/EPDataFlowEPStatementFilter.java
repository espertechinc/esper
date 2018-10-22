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
package com.espertech.esper.common.client.dataflow.core;


/**
 * Filter for use with the EPStatementSource operator.
 */
public interface EPDataFlowEPStatementFilter {
    /**
     * Pass or skip the statement.
     *
     * @param statement to test
     * @return indicator whether to include (true) or exclude (false) the statement.
     */
    public boolean pass(EPDataFlowEPStatementFilterContext statement);
}
