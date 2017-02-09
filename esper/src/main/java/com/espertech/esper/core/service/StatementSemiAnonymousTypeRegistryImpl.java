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

import com.espertech.esper.client.EventType;

public class StatementSemiAnonymousTypeRegistryImpl implements StatementSemiAnonymousTypeRegistry {
    public final static StatementSemiAnonymousTypeRegistryImpl INSTANCE = new StatementSemiAnonymousTypeRegistryImpl();

    public void register(EventType anonymouseType) {
    }
}
