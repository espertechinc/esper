/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.filter;

import com.espertech.esper.client.EventType;

/**
 * Service to provide engine-wide access to filter expressions that do not originate from
 * event property values, i.e. expressions that cannot be reproduced by obtaining a getter from the event type.
 */
public interface FilterNonPropertyRegisteryService {
    /**
     * Register expression.
     * @param statementName statement name
     * @param eventType event type
     * @param lookupable filter expression
     */
    void registerNonPropertyExpression(String statementName, EventType eventType, FilterSpecLookupable lookupable);

    /**
     * Obtain expression
     * @param eventType event type
     * @param expression expression text
     * @return lookupable
     */
    FilterSpecLookupable getNonPropertyExpression(EventType eventType, String expression);

    /**
     * Remove references to expression
     * @param statementName statement name
     */
    void removeReferencesStatement(String statementName);
}
