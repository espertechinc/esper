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
package com.espertech.esper.compiler.internal.parse;

import com.espertech.esper.compiler.internal.generated.EsperEPL2GrammarParser;

public class PropertyParserANTLR {
    public static boolean isNestedPropertyWithNonSimpleLead(EsperEPL2GrammarParser.EventPropertyContext ctx) {
        if (ctx.eventPropertyAtomic().size() == 1) {
            return false;
        }
        EsperEPL2GrammarParser.EventPropertyAtomicContext atomic = ctx.eventPropertyAtomic().get(0);
        return atomic.lb != null || atomic.lp != null || atomic.q1 != null;
    }
}
