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

import java.util.Set;

/**
 * Service for maintaining references between statement name and event type.
 */
public interface StatementEventTypeRef {
    /**
     * Returns true if the event type is listed as in-use by any statement, or false if not
     *
     * @param eventTypeName name
     * @return indicator whether type is in use
     */
    public boolean isInUse(String eventTypeName);

    /**
     * Returns the set of event types that are use by a given statement name.
     *
     * @param statementName name
     * @return set of event types or empty set if none found
     */
    public String[] getTypesForStatementName(String statementName);

    /**
     * Returns the set of statement names that use a given event type name.
     *
     * @param eventTypeName name
     * @return set of statements or null if none found
     */
    public Set<String> getStatementNamesForType(String eventTypeName);

    /**
     * Add a reference from a statement name to a set of event types.
     *
     * @param statementName        name of statement
     * @param eventTypesReferenced types
     */
    public void addReferences(String statementName, String[] eventTypesReferenced);

    /**
     * Remove all references for a given statement.
     *
     * @param statementName statement name
     */
    public void removeReferencesStatement(String statementName);

    /**
     * Remove all references for a given event type.
     *
     * @param eventTypeName event type name
     */
    public void removeReferencesType(String eventTypeName);
}
