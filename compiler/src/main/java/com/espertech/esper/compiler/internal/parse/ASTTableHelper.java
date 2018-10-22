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

import com.espertech.esper.common.internal.compile.stage1.spec.AnnotationDesc;
import com.espertech.esper.common.internal.compile.stage1.spec.CreateTableColumn;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;
import com.espertech.esper.common.internal.type.ClassIdentifierWArray;
import com.espertech.esper.compiler.internal.generated.EsperEPL2GrammarParser;
import org.antlr.v4.runtime.tree.Tree;

import java.util.*;

public class ASTTableHelper {

    public static List<CreateTableColumn> getColumns(List<EsperEPL2GrammarParser.CreateTableColumnContext> ctxs, Map<Tree, ExprNode> astExprNodeMap, ClasspathImportServiceCompileTime classpathImportService) {
        List<CreateTableColumn> cols = new ArrayList<CreateTableColumn>(ctxs.size());
        for (EsperEPL2GrammarParser.CreateTableColumnContext colctx : ctxs) {
            cols.add(getColumn(colctx, astExprNodeMap, classpathImportService));
        }
        return cols;
    }

    private static CreateTableColumn getColumn(EsperEPL2GrammarParser.CreateTableColumnContext ctx, Map<Tree, ExprNode> astExprNodeMap, ClasspathImportServiceCompileTime classpathImportService) {

        String columnName = ctx.n.getText();

        ExprNode optExpression = null;
        if (ctx.builtinFunc() != null || ctx.libFunction() != null) {
            optExpression = ASTExprHelper.exprCollectSubNodes(ctx, 0, astExprNodeMap).get(0);
        }

        ClassIdentifierWArray optType = ASTClassIdentifierHelper.walk(ctx.classIdentifierWithDimensions());

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
                annots.add(ASTAnnotationHelper.walk(anctx, classpathImportService));
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

        return new CreateTableColumn(columnName, optExpression, optType, annots, primaryKey);
    }
}
