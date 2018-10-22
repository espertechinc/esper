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
package com.espertech.esper.common.internal.epl.enummethod.compile;

import com.espertech.esper.common.internal.epl.enummethod.cache.ExpressionResultCacheStackEntry;

import java.util.ArrayDeque;
import java.util.Deque;

public class EnumMethodCallStackHelperImpl implements EnumMethodCallStackHelper {

    private Deque<ExpressionResultCacheStackEntry> callStack;

    public void pushStack(ExpressionResultCacheStackEntry lambda) {
        if (callStack == null) {
            callStack = new ArrayDeque<>();
        }
        callStack.push(lambda);
    }

    public boolean popLambda() {
        callStack.remove();
        return callStack.isEmpty();
    }

    public Deque<ExpressionResultCacheStackEntry> getStack() {
        return callStack;
    }
}
