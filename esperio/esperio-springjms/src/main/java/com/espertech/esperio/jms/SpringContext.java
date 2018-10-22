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
package com.espertech.esperio.jms;

/**
 * Supplies properties for use in configuration files to configure Spring application context.
 */
public class SpringContext {
    /**
     * Use to configure a classpath context.
     */
    public final static String CLASSPATH_CONTEXT = "classpath-app-context";

    /**
     * Use to configure a file context.
     */
    public final static String FILE_APP_CONTEXT = "file-app-context";
}
