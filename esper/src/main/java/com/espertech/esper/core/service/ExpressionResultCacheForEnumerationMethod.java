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
package com.espertech.esper.core.service;

import java.util.Deque;

/*
 * On the level of enumeration method:
 *     If a enumeration method expression is invoked within another enumeration method expression (not counting expression declarations),
 *     for example "source.where(a => source.minBy(b => b.x))" the "source.minBy(b => b.x)" is not dependent on any other lambda so the result gets cached.
 *     The cache is keyed by the enumeration-method-node as an IdentityHashMap and verified by a context stack (Long[]) that is built in nested evaluation calls.
 *
 * NOTE: ExpressionResultCacheForEnumerationMethod should not be held onto since the instance returned can be reused.
 */
public interface ExpressionResultCacheForEnumerationMethod {

    void pushStack(ExpressionResultCacheStackEntry lambda);

    boolean popLambda();

    Deque<ExpressionResultCacheStackEntry> getStack();

    ExpressionResultCacheEntryLongArrayAndObj getEnumerationMethodLastValue(Object node);

    void saveEnumerationMethodLastValue(Object node, Object result);

    void pushContext(long contextNumber);

    void popContext();
}
