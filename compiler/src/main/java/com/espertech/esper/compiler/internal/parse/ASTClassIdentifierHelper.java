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

import com.espertech.esper.common.internal.type.ClassIdentifierWArray;
import com.espertech.esper.compiler.internal.generated.EsperEPL2GrammarParser;

import java.util.List;
import java.util.Locale;

public class ASTClassIdentifierHelper {
    public static ClassIdentifierWArray walk(EsperEPL2GrammarParser.ClassIdentifierWithDimensionsContext ctx) throws ASTWalkException {
        if (ctx == null) {
            return null;
        }

        String name = ASTUtil.unescapeClassIdent(ctx.classIdentifier());
        List<EsperEPL2GrammarParser.DimensionsContext> dimensions = ctx.dimensions();

        if (dimensions.isEmpty()) {
            return new ClassIdentifierWArray(name);
        }

        EsperEPL2GrammarParser.DimensionsContext first = dimensions.get(0);
        String keyword = first.IDENT() != null ? first.IDENT().toString().trim().toLowerCase(Locale.ENGLISH) : null;
        if (keyword != null && !keyword.equals(ClassIdentifierWArray.PRIMITIVE_KEYWORD)) {
            throw ASTWalkException.from("Invalid array keyword '" + keyword + "', expected '" + ClassIdentifierWArray.PRIMITIVE_KEYWORD + "'");
        }
        return new ClassIdentifierWArray(name, dimensions.size(), keyword != null);
    }
}
