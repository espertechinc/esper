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
package com.espertech.esper.regressionlib.support.patternassert;

import com.espertech.esper.common.internal.epl.pattern.core.EvalRootForgeNode;
import com.espertech.esper.common.internal.epl.pattern.core.PatternCompileHook;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SupportPatternCompileHook implements PatternCompileHook {
    private static List<EvalRootForgeNode> roots = new ArrayList<>();

    public void pattern(EvalRootForgeNode root) {
        roots.add(root);
    }

    public static EvalRootForgeNode getOneAndReset() {
        assertEquals(1, roots.size());
        return roots.remove(0);
    }

    public static void reset() {
        roots.clear();
    }
}
