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
package com.espertech.esper.epl.parse;

import com.espertech.esper.epl.generated.EsperEPL2GrammarParser;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.Locale;

public class ASTTypeExpressionAnnoHelper {
    public static String expectMayTypeAnno(EsperEPL2GrammarParser.TypeExpressionAnnotationContext ctx, CommonTokenStream tokenStream) {
        if (ctx == null) {
            return null;
        }
        String annoName = ctx.n.getText();
        if (!annoName.toLowerCase(Locale.ENGLISH).equals("type")) {
            throw ASTWalkException.from("Invalid annotation for property selection, expected 'type' but found '" + annoName + "'", tokenStream, ctx);
        }
        return ctx.v.getText();
    }
}
