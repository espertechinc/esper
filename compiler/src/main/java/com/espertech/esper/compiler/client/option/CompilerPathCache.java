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
package com.espertech.esper.compiler.client.option;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.compiler.internal.util.CompilerPathCacheImpl;

/**
 * For optional use with the compiler, for speeding up compilation when the compiler path has one or more {@link EPCompiled} instances that provide visible EPL objects (named windows, tables etc.),
 * this cache retains and helps reuse the information in respect to each {@link EPCompiled} instance and the visible EPL objects it provides.
 * <p>
 *     The compiler is a stateless service and does not retain information between invocations.
 * </p>
 * <p>
 *     The compiler uses the cache, when provided, for any {@link EPCompiled} instances in the compiler path to determine the visible EPL objects for that {@link EPCompiled}.
 *     Thus the compiler does not need to perform any classloading or initialization of EPL objects for the {@link EPCompiled} thus reducing compilation time
 *     when there is a compiler path with {@link EPCompiled} instances in the path.
 * </p>
 * <p>
 *     The compiler, upon successful compilation of an EPL module (not a fire-and-forget query), populates the cache with the output {@link EPCompiled} and its EPL objects.
 * </p>
 * <p>
 *     The compiler, upon successful loading of an {@link EPCompiled} from the compiler path, populates the cache with the loaded {@link EPCompiled} and its EPL objects.
 * </p>
 * <p>
 *     Alternatively an application can deploy to a runtime and use the runtime path.
 * </p>
 */
public class CompilerPathCache {
    /**
     * Returns a cache that keeps a synchronized map of {@link EPCompiled} to EPL objects
     * @return cache
     */
    public static CompilerPathCache getInstance() {
        return new CompilerPathCacheImpl();
    }
}
