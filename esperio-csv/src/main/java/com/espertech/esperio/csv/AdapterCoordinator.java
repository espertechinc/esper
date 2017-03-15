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
package com.espertech.esperio.csv;

import com.espertech.esper.adapter.InputAdapter;

/**
 * A AdapterCoordinator coordinates several Adapters so that the events they
 * send into the runtime engine arrive in some well-defined order, in
 * effect making the several Adapters into one large sending Adapter.
 */
public interface AdapterCoordinator extends InputAdapter {
    /**
     * Coordinate an InputAdapter.
     *
     * @param adapter - the InputAdapter to coordinate
     */
    public void coordinate(InputAdapter adapter);
}
