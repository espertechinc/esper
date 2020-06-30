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

import com.espertech.esper.common.internal.type.ClassDescriptor;
import com.espertech.esper.compiler.internal.generated.EsperEPL2GrammarParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ASTClassIdentifierHelper {
    public static ClassDescriptor walk(EsperEPL2GrammarParser.ClassIdentifierNoDimensionsContext ctx) throws ASTWalkException {
        if (ctx == null) {
            return null;
        }

        String name = ASTUtil.unescapeClassIdent(ctx.classIdentifier());
        if (ctx.typeParameters() == null) {
            return new ClassDescriptor(name);
        }

        List<ClassDescriptor> typeParameters = walkTypeParameters(ctx.typeParameters());
        return new ClassDescriptor(name, typeParameters, 0, false);
    }

    public static ClassDescriptor walk(EsperEPL2GrammarParser.ClassIdentifierWithDimensionsContext ctx) throws ASTWalkException {
        if (ctx == null) {
            return null;
        }

        String name = ASTUtil.unescapeClassIdent(ctx.classIdentifier());
        List<EsperEPL2GrammarParser.DimensionsContext> dimensions = ctx.dimensions();

        if (dimensions.isEmpty() && ctx.typeParameters() == null) {
            return new ClassDescriptor(name);
        }

        List<ClassDescriptor> typeParameters = walkTypeParameters(ctx.typeParameters());
        if (dimensions.isEmpty()) {
            return new ClassDescriptor(name, typeParameters, 0, false);
        }

        EsperEPL2GrammarParser.DimensionsContext first = dimensions.get(0);
        String keyword = first.IDENT() != null ? first.IDENT().toString().trim().toLowerCase(Locale.ENGLISH) : null;
        if (keyword != null) {
            if (!keyword.equals(ClassDescriptor.PRIMITIVE_KEYWORD)) {
                throw ASTWalkException.from("Invalid array keyword '" + keyword + "', expected '" + ClassDescriptor.PRIMITIVE_KEYWORD + "'");
            }
            if (!typeParameters.isEmpty()) {
                throw ASTWalkException.from("Cannot use the '" + ClassDescriptor.PRIMITIVE_KEYWORD + "' keyword with type parameters");
            }
        }
        return new ClassDescriptor(name, typeParameters, dimensions.size(), keyword != null);
    }

    private static List<ClassDescriptor> walkTypeParameters(EsperEPL2GrammarParser.TypeParametersContext typeParameters) {
        if (typeParameters == null) {
            return Collections.emptyList();
        }
        List<ClassDescriptor> result = new ArrayList<>(typeParameters.classIdentifierWithDimensions().size());
        for (EsperEPL2GrammarParser.ClassIdentifierWithDimensionsContext typeParamCtx : typeParameters.classIdentifierWithDimensions()) {
            ClassDescriptor typeParam = walk(typeParamCtx);
            result.add(typeParam);
        }
        return result;
    }
}
