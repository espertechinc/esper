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
package com.espertech.esper.core.service;

/**
 * Interface for statement-level dispatch.
 * <p>
 * Relevant when a statements callbacks have completed and the join processing must take place.
 */
public interface EPStatementDispatch {
    /**
     * Execute dispatch.
     */
    public void execute();
}
