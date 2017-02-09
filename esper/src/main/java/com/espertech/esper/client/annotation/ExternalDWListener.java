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
package com.espertech.esper.client.annotation;

/**
 * Annotation for configuring external data window listeners.
 */
public @interface ExternalDWListener {
    /**
     * Returns indicator whether a listener thread is required or not.
     *
     * @return indicator
     */
    boolean threaded() default true;

    /**
     * Returns indicator the number of listener threads.
     *
     * @return number of threads
     */
    int numThreads() default 1;
}
