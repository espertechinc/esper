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

import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage1.spec.AnnotationDesc;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.settings.ClasspathImportCompileTimeUtil;
import com.espertech.esper.common.internal.settings.ClasspathImportException;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;
import com.espertech.esper.common.internal.util.ValueAndFieldDesc;
import com.espertech.esper.compiler.internal.generated.EsperEPL2GrammarParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Walker to annotation stuctures.
 */
public class ASTAnnotationHelper {
    /**
     * Walk an annotation root name or child node (nested annotations).
     *
     * @param ctx                    annotation walk node
     * @param classpathImportService for imports
     * @return annotation descriptor
     * @throws ASTWalkException if the walk failed
     */
    public static AnnotationDesc walk(EsperEPL2GrammarParser.AnnotationEnumContext ctx, ClasspathImportServiceCompileTime classpathImportService) throws ASTWalkException {
        String name = ASTUtil.unescapeClassIdent(ctx.classIdentifier());
        List<Pair<String, Object>> values = new ArrayList<Pair<String, Object>>();
        if (ctx.elementValueEnum() != null) {
            Object value = walkValue(ctx.elementValueEnum(), classpathImportService);
            values.add(new Pair<String, Object>("value", value));
        } else if (ctx.elementValuePairsEnum() != null) {
            walkValuePairs(ctx.elementValuePairsEnum(), values, classpathImportService);
        }

        return new AnnotationDesc(name, values);
    }

    private static void walkValuePairs(EsperEPL2GrammarParser.ElementValuePairsEnumContext elementValuePairsEnumContext,
                                       List<Pair<String, Object>> values,
                                       ClasspathImportServiceCompileTime classpathImportService) {

        for (EsperEPL2GrammarParser.ElementValuePairEnumContext ctx : elementValuePairsEnumContext.elementValuePairEnum()) {
            Pair<String, Object> pair = walkValuePair(ctx, classpathImportService);
            values.add(pair);
        }
    }

    private static Object walkValue(EsperEPL2GrammarParser.ElementValueEnumContext ctx, ClasspathImportServiceCompileTime classpathImportService) {
        if (ctx.elementValueArrayEnum() != null) {
            return walkArray(ctx.elementValueArrayEnum(), classpathImportService);
        }
        if (ctx.annotationEnum() != null) {
            return walk(ctx.annotationEnum(), classpathImportService);
        } else if (ctx.v != null) {
            return ctx.v.getText();
        } else if (ctx.classIdentifier() != null) {
            return walkClassIdent(ctx.classIdentifier(), classpathImportService);
        } else {
            return ASTConstantHelper.parse(ctx.constant());
        }
    }

    private static Pair<String, Object> walkValuePair(EsperEPL2GrammarParser.ElementValuePairEnumContext ctx, ClasspathImportServiceCompileTime classpathImportService) {
        String name = ctx.keywordAllowedIdent().getText();
        Object value = walkValue(ctx.elementValueEnum(), classpathImportService);
        return new Pair<String, Object>(name, value);
    }

    private static Object walkClassIdent(EsperEPL2GrammarParser.ClassIdentifierContext ctx, ClasspathImportServiceCompileTime classpathImportService) {
        String enumValueText = ctx.getText();
        ValueAndFieldDesc enumValueAndField;
        try {
            enumValueAndField = ClasspathImportCompileTimeUtil.resolveIdentAsEnumConst(enumValueText, classpathImportService, true);
        } catch (ExprValidationException e) {
            throw ASTWalkException.from("Annotation value '" + enumValueText + "' is not recognized as an enumeration value, please check imports or use a primitive or string type");
        }
        if (enumValueAndField != null) {
            return enumValueAndField.getValue();
        }

        // resolve as class
        Object enumValue = null;
        if (enumValueText.endsWith(".class") && enumValueText.length() > 6) {
            try {
                String name = enumValueText.substring(0, enumValueText.length() - 6);
                enumValue = classpathImportService.resolveClass(name, true);
            } catch (ClasspathImportException e) {
                // expected
            }
        }

        if (enumValue != null) {
            return enumValue;
        }

        throw ASTWalkException.from("Annotation enumeration value '" + enumValueText + "' not recognized as an enumeration class, please check imports or type used");
    }

    private static Object[] walkArray(EsperEPL2GrammarParser.ElementValueArrayEnumContext ctx, ClasspathImportServiceCompileTime classpathImportService) {
        List<EsperEPL2GrammarParser.ElementValueEnumContext> elements = ctx.elementValueEnum();
        Object[] values = new Object[elements.size()];
        for (int i = 0; i < elements.size(); i++) {
            values[i] = walkValue(elements.get(i), classpathImportService);
        }
        return values;
    }
}
