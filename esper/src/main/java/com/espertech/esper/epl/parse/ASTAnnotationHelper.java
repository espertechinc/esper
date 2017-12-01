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

import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.engineimport.EngineImportUtil;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.generated.EsperEPL2GrammarParser;
import com.espertech.esper.epl.spec.AnnotationDesc;

import java.util.ArrayList;
import java.util.List;

/**
 * Walker to annotation stuctures.
 */
public class ASTAnnotationHelper {
    /**
     * Walk an annotation root name or child node (nested annotations).
     *
     * @param ctx                 annotation walk node
     * @param engineImportService for engine imports
     * @return annotation descriptor
     * @throws ASTWalkException if the walk failed
     */
    public static AnnotationDesc walk(EsperEPL2GrammarParser.AnnotationEnumContext ctx, EngineImportService engineImportService) throws ASTWalkException {
        String name = ASTUtil.unescapeClassIdent(ctx.classIdentifier());
        List<Pair<String, Object>> values = new ArrayList<Pair<String, Object>>();
        if (ctx.elementValueEnum() != null) {
            Object value = walkValue(ctx.elementValueEnum(), engineImportService);
            values.add(new Pair<String, Object>("value", value));
        } else if (ctx.elementValuePairsEnum() != null) {
            walkValuePairs(ctx.elementValuePairsEnum(), values, engineImportService);
        }

        return new AnnotationDesc(name, values);
    }

    private static void walkValuePairs(EsperEPL2GrammarParser.ElementValuePairsEnumContext elementValuePairsEnumContext,
                                       List<Pair<String, Object>> values,
                                       EngineImportService engineImportService) {

        for (EsperEPL2GrammarParser.ElementValuePairEnumContext ctx : elementValuePairsEnumContext.elementValuePairEnum()) {
            Pair<String, Object> pair = walkValuePair(ctx, engineImportService);
            values.add(pair);
        }
    }

    private static Object walkValue(EsperEPL2GrammarParser.ElementValueEnumContext ctx, EngineImportService engineImportService) {
        if (ctx.elementValueArrayEnum() != null) {
            return walkArray(ctx.elementValueArrayEnum(), engineImportService);
        }
        if (ctx.annotationEnum() != null) {
            return walk(ctx.annotationEnum(), engineImportService);
        } else if (ctx.v != null) {
            return ctx.v.getText();
        } else if (ctx.classIdentifier() != null) {
            return walkClassIdent(ctx.classIdentifier(), engineImportService);
        } else {
            return ASTConstantHelper.parse(ctx.constant());
        }
    }

    private static Pair<String, Object> walkValuePair(EsperEPL2GrammarParser.ElementValuePairEnumContext ctx, EngineImportService engineImportService) {
        String name = ctx.keywordAllowedIdent().getText();
        Object value = walkValue(ctx.elementValueEnum(), engineImportService);
        return new Pair<String, Object>(name, value);
    }

    private static Object walkClassIdent(EsperEPL2GrammarParser.ClassIdentifierContext ctx, EngineImportService engineImportService) {
        String enumValueText = ctx.getText();
        Object enumValue;
        try {
            enumValue = EngineImportUtil.resolveIdentAsEnumConst(enumValueText, engineImportService, true);
        } catch (ExprValidationException e) {
            throw ASTWalkException.from("Annotation value '" + enumValueText + "' is not recognized as an enumeration value, please check imports or use a primitive or string type");
        }
        if (enumValue != null) {
            return enumValue;
        }
        throw ASTWalkException.from("Annotation enumeration value '" + enumValueText + "' not recognized as an enumeration class, please check imports or type used");
    }

    private static Object[] walkArray(EsperEPL2GrammarParser.ElementValueArrayEnumContext ctx, EngineImportService engineImportService) {
        List<EsperEPL2GrammarParser.ElementValueEnumContext> elements = ctx.elementValueEnum();
        Object[] values = new Object[elements.size()];
        for (int i = 0; i < elements.size(); i++) {
            values[i] = walkValue(elements.get(i), engineImportService);
        }
        return values;
    }
}
