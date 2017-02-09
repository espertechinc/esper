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

import com.espertech.esper.client.EPServiceProviderIsolated;
import com.espertech.esper.client.EPStatement;

/**
 * Service for managing statement isolation.
 */
public interface StatementIsolationService {
    /**
     * Returns an isolated service by names, or allocates a new one if none found.
     *
     * @param name           isolated service
     * @param optionalUnitId the unique id assigned to the isolation unit
     * @return isolated service provider
     */
    public EPServiceProviderIsolated getIsolationUnit(String name, Integer optionalUnitId);

    /**
     * Destroys the servce.
     */
    public void destroy();

    /**
     * Returns all names or currently known isolation services.
     *
     * @return names
     */
    public String[] getIsolationUnitNames();

    /**
     * Indicates statements are moved to isolation.
     *
     * @param name   isolated service provider name.
     * @param unitId isolated service provider number.
     * @param stmt   statements moved.
     */
    public void beginIsolatingStatements(String name, int unitId, EPStatement[] stmt);

    /**
     * Indicates statements are have moved to isolation.
     *
     * @param name   isolated service provider name.
     * @param unitId isolated service provider number.
     * @param stmt   statements moved.
     */
    public void commitIsolatingStatements(String name, int unitId, EPStatement[] stmt);

    /**
     * Indicates statements are have not moved to isolation.
     *
     * @param name   isolated service provider name.
     * @param unitId isolated service provider number.
     * @param stmt   statements moved.
     */
    public void rollbackIsolatingStatements(String name, int unitId, EPStatement[] stmt);

    /**
     * Indicates statements are moved out of isolation.
     *
     * @param name   isolated service provider name.
     * @param unitId isolated service provider number.
     * @param stmt   statements moved.
     */
    public void beginUnisolatingStatements(String name, int unitId, EPStatement[] stmt);

    /**
     * Indicates statements have been moved out of isolation.
     *
     * @param name   isolated service provider name.
     * @param unitId isolated service provider number.
     * @param stmt   statements moved.
     */
    public void commitUnisolatingStatements(String name, int unitId, EPStatement[] stmt);

    /**
     * Indicates statements are not moved out of isolation.
     *
     * @param name   isolated service provider name.
     * @param unitId isolated service provider number.
     * @param stmt   statements moved.
     */
    public void rollbackUnisolatingStatements(String name, int unitId, EPStatement[] stmt);

    /**
     * Indicates a new statement created in an isolated service.
     *
     * @param stmtId           statement id
     * @param stmtName         statement name
     * @param isolatedServices isolated services
     */
    public void newStatement(int stmtId, String stmtName, EPIsolationUnitServices isolatedServices);
}
