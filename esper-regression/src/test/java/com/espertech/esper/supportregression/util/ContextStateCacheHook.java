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
package com.espertech.esper.supportregression.util;

public interface ContextStateCacheHook {
    public final static String CONTEXT_CACHE_HOOK = "@Hook(type=HookType.CONTEXT_STATE_CACHE, hook='" + SupportContextStateCacheImpl.class.getName() + "')\n";
}
