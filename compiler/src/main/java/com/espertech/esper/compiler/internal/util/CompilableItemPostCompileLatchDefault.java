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
package com.espertech.esper.compiler.internal.util;

import java.util.Map;

public class CompilableItemPostCompileLatchDefault implements CompilableItemPostCompileLatch {
    public final static CompilableItemPostCompileLatchDefault INSTANCE = new CompilableItemPostCompileLatchDefault();

    private CompilableItemPostCompileLatchDefault() {
    }

    public void awaitAndRun() {
    }

    public void completed(Map<String, byte[]> moduleBytes) {
    }
}
