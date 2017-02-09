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
package com.espertech.esper.dispatch;

/**
 * Provider of implementations for the dispatch service.
 */
public class DispatchServiceProvider {
    /**
     * Returns new service.
     *
     * @return new dispatch service implementation.
     */
    public static DispatchService newService() {
        return new DispatchServiceImpl();
    }
}
