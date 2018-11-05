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
package com.espertech.esper.common.internal.epl.namedwindow.core;

import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;

import java.util.function.BiConsumer;

/**
 * Service to manage named windows on an runtime level.
 */
public interface NamedWindowManagementService {
    /**
     * Error message for data windows required.
     */
    String ERROR_MSG_DATAWINDOWS = "Named windows require one or more child views that are data window views";

    /**
     * Error message for no data window allowed.
     */
    public final static String ERROR_MSG_NO_DATAWINDOW_ALLOWED = "Consuming statements to a named window cannot declare a data window view onto the named window";

    void addNamedWindow(String namedWindowName, NamedWindowMetaData desc, EPStatementInitServices services);

    NamedWindow getNamedWindow(String deploymentId, String namedWindowName);

    int getDeploymentCount();

    void destroyNamedWindow(String deploymentId, String namedWindowName);

    void traverseNamedWindows(BiConsumer<String, NamedWindow> consumer);
}
