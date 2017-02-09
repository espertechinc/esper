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

import com.espertech.esper.client.EPException;
import com.espertech.esper.epl.generated.EsperEPL2GrammarParser;
import com.espertech.esper.epl.spec.ColumnDesc;
import com.espertech.esper.epl.spec.CreateSchemaDesc;
import org.antlr.v4.runtime.Token;

import java.util.*;

public class ASTCreateSchemaHelper {

    public static CreateSchemaDesc walkCreateSchema(EsperEPL2GrammarParser.CreateSchemaExprContext ctx) throws EPException {
        CreateSchemaDesc.AssignedType assignedType = CreateSchemaDesc.AssignedType.NONE;
        if (ctx.keyword != null) {
            assignedType = CreateSchemaDesc.AssignedType.parseKeyword(ctx.keyword.getText());
        }
        return getSchemaDesc(ctx.createSchemaDef(), assignedType);
    }

    private static CreateSchemaDesc getSchemaDesc(EsperEPL2GrammarParser.CreateSchemaDefContext ctx, CreateSchemaDesc.AssignedType assignedType) throws EPException {
        String schemaName = ctx.name.getText();
        List<ColumnDesc> columnTypes = getColTypeList(ctx.createColumnList());

        // get model-after types (could be multiple for variants)
        Set<String> typeNames = new LinkedHashSet<String>();
        if (ctx.variantList() != null) {
            List<EsperEPL2GrammarParser.VariantListElementContext> variantCtxs = ctx.variantList().variantListElement();
            for (EsperEPL2GrammarParser.VariantListElementContext variantCtx : variantCtxs) {
                typeNames.add(variantCtx.getText());
            }
        }

        // get inherited and start timestamp and end timestamps
        String startTimestamp = null;
        String endTimestamp = null;
        Set<String> inherited = new LinkedHashSet<String>();
        Set<String> copyFrom = new LinkedHashSet<String>();
        if (ctx.createSchemaQual() != null) {
            List<EsperEPL2GrammarParser.CreateSchemaQualContext> qualCtxs = ctx.createSchemaQual();
            for (EsperEPL2GrammarParser.CreateSchemaQualContext qualCtx : qualCtxs) {
                String qualName = qualCtx.i.getText().toLowerCase(Locale.ENGLISH);
                List<String> cols = ASTUtil.getIdentList(qualCtx.columnList());
                if (qualName.toLowerCase(Locale.ENGLISH).equals("inherits")) {
                    inherited.addAll(cols);
                    continue;
                } else if (qualName.toLowerCase(Locale.ENGLISH).equals("starttimestamp")) {
                    startTimestamp = cols.get(0);
                    continue;
                } else if (qualName.toLowerCase(Locale.ENGLISH).equals("endtimestamp")) {
                    endTimestamp = cols.get(0);
                    continue;
                } else if (qualName.toLowerCase(Locale.ENGLISH).equals("copyfrom")) {
                    copyFrom.addAll(cols);
                    continue;
                }
                throw new EPException("Expected 'inherits', 'starttimestamp', 'endtimestamp' or 'copyfrom' keyword after create-schema clause but encountered '" + qualName + "'");
            }
        }

        return new CreateSchemaDesc(schemaName, typeNames, columnTypes, inherited, assignedType, startTimestamp, endTimestamp, copyFrom);
    }

    public static List<ColumnDesc> getColTypeList(EsperEPL2GrammarParser.CreateColumnListContext ctx) {
        if (ctx == null) {
            return Collections.emptyList();
        }
        List<ColumnDesc> result = new ArrayList<ColumnDesc>(ctx.createColumnListElement().size());
        for (EsperEPL2GrammarParser.CreateColumnListElementContext colctx : ctx.createColumnListElement()) {
            List<EsperEPL2GrammarParser.ClassIdentifierContext> idents = colctx.classIdentifier();
            String name = ASTUtil.unescapeClassIdent(idents.get(0));
            String type;
            if (colctx.VALUE_NULL() != null) {
                type = null;
            } else {
                type = ASTUtil.unescapeClassIdent(idents.get(1));
            }
            boolean array = colctx.b != null;
            boolean primitiveArray = validateIsPrimitiveArray(colctx.p);
            result.add(new ColumnDesc(name, type, array, primitiveArray));
        }
        return result;
    }

    protected static boolean validateIsPrimitiveArray(Token p) {
        if (p != null) {
            if (!p.getText().toLowerCase(Locale.ENGLISH).equals("primitive")) {
                throw ASTWalkException.from("Column type keyword '" + p.getText() + "' not recognized, expecting '[primitive]'");
            }
            return true;
        }
        return false;
    }
}
