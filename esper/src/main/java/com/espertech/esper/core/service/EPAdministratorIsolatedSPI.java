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

import com.espertech.esper.client.EPAdministratorIsolated;
import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPStatement;

/**
 * Implementation for the admin interface.
 */
public interface EPAdministratorIsolatedSPI extends EPAdministratorIsolated {
    /**
     * Add a statement name to the list of statements held by the isolated service provider.
     *
     * @param name to add
     */
    public void addStatement(String name);

    public EPStatement createEPLStatementId(String eplStatement, String statementName, Object userObject, Integer optionalStatementId) throws EPException;
}