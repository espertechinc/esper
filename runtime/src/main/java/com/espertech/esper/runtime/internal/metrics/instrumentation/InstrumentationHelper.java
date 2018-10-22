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
package com.espertech.esper.runtime.internal.metrics.instrumentation;

public class InstrumentationHelper {
    public final static boolean ENABLED = false;
    public final static boolean ASSERTIONENABLED = false;

    public final static Instrumentation DEFAULT_INSTRUMENTATION = InstrumentationDefault.INSTANCE;
    public static Instrumentation instrumentation = DEFAULT_INSTRUMENTATION;

    public static Instrumentation get() {
        return instrumentation;
    }
}
