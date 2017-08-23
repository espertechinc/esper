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
import com.espertech.esper.epl.generated.EsperEPL2GrammarParser;
import com.espertech.esper.epl.spec.*;
import org.antlr.v4.runtime.tree.Tree;

import java.util.*;

public class ASTGraphHelper {
    public static CreateDataFlowDesc walkCreateDataFlow(EsperEPL2GrammarParser.CreateDataflowContext ctx, Map<Tree, Object> astGraphNodeMap, EngineImportService engineImportService) {
        String graphName = ctx.name.getText();

        List<GraphOperatorSpec> ops = new ArrayList<GraphOperatorSpec>();
        List<CreateSchemaDesc> schemas = new ArrayList<CreateSchemaDesc>();

        List<EsperEPL2GrammarParser.GopContext> gopctxs = ctx.gopList().gop();
        for (EsperEPL2GrammarParser.GopContext gopctx : gopctxs) {
            if (gopctx.createSchemaExpr() != null) {
                schemas.add(ASTCreateSchemaHelper.walkCreateSchema(gopctx.createSchemaExpr()));
            } else {
                ops.add(parseOp(gopctx, astGraphNodeMap, engineImportService));
            }
        }
        return new CreateDataFlowDesc(graphName, ops, schemas);
    }

    private static GraphOperatorSpec parseOp(EsperEPL2GrammarParser.GopContext ctx, Map<Tree, Object> astGraphNodeMap, EngineImportService engineImportService) {
        String operatorName = ctx.opName != null ? ctx.opName.getText() : ctx.s.getText();

        GraphOperatorInput input = new GraphOperatorInput();
        if (ctx.gopParams() != null) {
            parseParams(ctx.gopParams(), input);
        }

        GraphOperatorOutput output = new GraphOperatorOutput();
        if (ctx.gopOut() != null) {
            parseOutput(ctx.gopOut(), output);
        }

        GraphOperatorDetail detail = null;
        if (ctx.gopDetail() != null) {
            Map<String, Object> configs = new LinkedHashMap<String, Object>();
            List<EsperEPL2GrammarParser.GopConfigContext> cfgctxs = ctx.gopDetail().gopConfig();
            for (EsperEPL2GrammarParser.GopConfigContext cfgctx : cfgctxs) {
                String name;
                Object value = astGraphNodeMap.remove(cfgctx);
                if (cfgctx.n != null) {
                    name = cfgctx.n.getText();
                } else {
                    name = "select";
                }
                configs.put(name, value);
            }
            detail = new GraphOperatorDetail(configs);
        }

        List<AnnotationDesc> annotations;
        if (ctx.annotationEnum() != null) {
            List<EsperEPL2GrammarParser.AnnotationEnumContext> annoctxs = ctx.annotationEnum();
            annotations = new ArrayList<AnnotationDesc>();
            for (EsperEPL2GrammarParser.AnnotationEnumContext annoctx : annoctxs) {
                annotations.add(ASTAnnotationHelper.walk(annoctx, engineImportService));
            }
        } else {
            annotations = Collections.emptyList();
        }

        return new GraphOperatorSpec(operatorName, input, output, detail, annotations);
    }

    private static void parseParams(EsperEPL2GrammarParser.GopParamsContext ctx, GraphOperatorInput input) {
        if (ctx.gopParamsItemList() == null) {
            return;
        }
        List<EsperEPL2GrammarParser.GopParamsItemContext> items = ctx.gopParamsItemList().gopParamsItem();
        for (EsperEPL2GrammarParser.GopParamsItemContext item : items) {
            String[] streamNames = parseParamsStreamNames(item);
            String aliasName = item.gopParamsItemAs() != null ? item.gopParamsItemAs().a.getText() : null;
            input.getStreamNamesAndAliases().add(new GraphOperatorInputNamesAlias(streamNames, aliasName));
        }
    }

    private static String[] parseParamsStreamNames(EsperEPL2GrammarParser.GopParamsItemContext item) {
        List<String> paramNames = new ArrayList<String>(1);
        if (item.gopParamsItemMany() != null) {
            for (EsperEPL2GrammarParser.ClassIdentifierContext ctx : item.gopParamsItemMany().classIdentifier()) {
                paramNames.add(ctx.getText());
            }
        } else {
            paramNames.add(ASTUtil.unescapeClassIdent(item.classIdentifier()));
        }
        return paramNames.toArray(new String[paramNames.size()]);
    }

    private static void parseOutput(EsperEPL2GrammarParser.GopOutContext ctx, GraphOperatorOutput output) {
        if (ctx == null) {
            return;
        }
        List<EsperEPL2GrammarParser.GopOutItemContext> items = ctx.gopOutItem();
        for (EsperEPL2GrammarParser.GopOutItemContext item : items) {
            String streamName = item.n.getText();

            List<GraphOperatorOutputItemType> types = new ArrayList<GraphOperatorOutputItemType>();
            if (item.gopOutTypeList() != null) {
                for (EsperEPL2GrammarParser.GopOutTypeParamContext pctx : item.gopOutTypeList().gopOutTypeParam()) {
                    GraphOperatorOutputItemType type = parseType(pctx);
                    types.add(type);
                }
            }
            output.getItems().add(new GraphOperatorOutputItem(streamName, types));
        }
    }

    private static GraphOperatorOutputItemType parseType(EsperEPL2GrammarParser.GopOutTypeParamContext ctx) {

        if (ctx.q != null) {
            return new GraphOperatorOutputItemType(true, null, null);
        }

        String className = ASTUtil.unescapeClassIdent(ctx.gopOutTypeItem().classIdentifier());
        List<GraphOperatorOutputItemType> typeParams = new ArrayList<GraphOperatorOutputItemType>();
        if (ctx.gopOutTypeItem().gopOutTypeList() != null) {
            List<EsperEPL2GrammarParser.GopOutTypeParamContext> pctxs = ctx.gopOutTypeItem().gopOutTypeList().gopOutTypeParam();
            for (EsperEPL2GrammarParser.GopOutTypeParamContext pctx : pctxs) {
                GraphOperatorOutputItemType type = parseType(pctx);
                typeParams.add(type);
            }
        }
        return new GraphOperatorOutputItemType(false, className, typeParams);
    }
}
