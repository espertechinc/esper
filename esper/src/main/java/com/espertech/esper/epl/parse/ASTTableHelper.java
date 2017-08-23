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

import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.generated.EsperEPL2GrammarParser;
import com.espertech.esper.epl.spec.AnnotationDesc;
import com.espertech.esper.epl.spec.CreateTableColumn;
import org.antlr.v4.runtime.tree.Tree;

import java.util.*;

public class ASTTableHelper {

    public static List<CreateTableColumn> getColumns(List<EsperEPL2GrammarParser.CreateTableColumnContext> ctxs, Map<Tree, ExprNode> astExprNodeMap, EngineImportService engineImportService) {
        List<CreateTableColumn> cols = new ArrayList<CreateTableColumn>(ctxs.size());
        for (EsperEPL2GrammarParser.CreateTableColumnContext colctx : ctxs) {
            cols.add(getColumn(colctx, astExprNodeMap, engineImportService));
        }
        return cols;
    }

    private static CreateTableColumn getColumn(EsperEPL2GrammarParser.CreateTableColumnContext ctx, Map<Tree, ExprNode> astExprNodeMap, EngineImportService engineImportService) {

        String columnName = ctx.n.getText();

        ExprNode optExpression = null;
        if (ctx.builtinFunc() != null || ctx.libFunction() != null) {
            optExpression = ASTExprHelper.exprCollectSubNodes(ctx, 0, astExprNodeMap).get(0);
        }

        String optTypeName = null;
        Boolean optTypeIsArray = null;
        Boolean optTypeIsPrimitiveArray = null;
        if (ctx.createTableColumnPlain() != null) {
            EsperEPL2GrammarParser.CreateTableColumnPlainContext sub = ctx.createTableColumnPlain();
            optTypeName = ASTUtil.unescapeClassIdent(sub.classIdentifier());
            optTypeIsArray = sub.b != null;
            optTypeIsPrimitiveArray = ASTCreateSchemaHelper.validateIsPrimitiveArray(sub.p);
        }

        Boolean primaryKey = false;
        if (ctx.p != null) {
            if (!ctx.p.getText().toLowerCase(Locale.ENGLISH).equals("primary")) {
                throw ASTWalkException.from("Invalid keyword '" + ctx.p.getText() + "' encountered, expected 'primary key'");
            }
            if (!ctx.k.getText().toLowerCase(Locale.ENGLISH).equals("key")) {
                throw ASTWalkException.from("Invalid keyword '" + ctx.k.getText() + "' encountered, expected 'primary key'");
            }
            primaryKey = true;
        }

        List<AnnotationDesc> annots = Collections.emptyList();
        if (ctx.annotationEnum() != null) {
            annots = new ArrayList<AnnotationDesc>(ctx.annotationEnum().size());
            for (EsperEPL2GrammarParser.AnnotationEnumContext anctx : ctx.annotationEnum()) {
                annots.add(ASTAnnotationHelper.walk(anctx, engineImportService));
            }
        }
        if (ctx.typeExpressionAnnotation() != null) {
            if (annots.isEmpty()) {
                annots = new ArrayList<AnnotationDesc>();
            }
            for (EsperEPL2GrammarParser.TypeExpressionAnnotationContext anno : ctx.typeExpressionAnnotation()) {
                annots.add(new AnnotationDesc(anno.n.getText(), anno.v.getText()));
            }
        }

        return new CreateTableColumn(columnName, optExpression, optTypeName, optTypeIsArray, optTypeIsPrimitiveArray, annots, primaryKey);
    }
}
