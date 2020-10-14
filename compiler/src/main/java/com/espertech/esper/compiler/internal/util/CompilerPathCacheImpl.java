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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.internal.epl.util.EPCompilerPathableImpl;
import com.espertech.esper.compiler.client.option.CompilerPathCache;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CompilerPathCacheImpl extends CompilerPathCache {
    private final Map<EPCompiled, EPCompilerPathableImpl> pathables = Collections.synchronizedMap(new HashMap<>());

    public void put(EPCompiled compiled, EPCompilerPathableImpl pathable) {
        pathables.put(compiled, pathable);
    }

    public EPCompilerPathableImpl get(EPCompiled unit) {
        return pathables.get(unit);
    }
}
