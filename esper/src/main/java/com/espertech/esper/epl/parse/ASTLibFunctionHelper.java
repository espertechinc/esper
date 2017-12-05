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

import com.espertech.esper.client.ConfigurationInformation;
import com.espertech.esper.client.ConfigurationPlugInAggregationMultiFunction;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.context.util.ContextDescriptor;
import com.espertech.esper.epl.core.engineimport.EngineImportException;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.engineimport.EngineImportSingleRowDesc;
import com.espertech.esper.epl.core.engineimport.EngineImportUndefinedException;
import com.espertech.esper.epl.declexpr.ExprDeclaredHelper;
import com.espertech.esper.epl.declexpr.ExprDeclaredNodeImpl;
import com.espertech.esper.epl.declexpr.ExprDeclaredService;
import com.espertech.esper.epl.enummethod.dot.ExprLambdaGoesNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeUtil;
import com.espertech.esper.epl.expression.core.ExprChainedSpec;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.MinMaxTypeEnum;
import com.espertech.esper.epl.expression.dot.ExprDotNode;
import com.espertech.esper.epl.expression.dot.ExprDotNodeImpl;
import com.espertech.esper.epl.expression.funcs.ExprMinMaxRowNode;
import com.espertech.esper.epl.expression.funcs.ExprPlugInSingleRowNode;
import com.espertech.esper.epl.expression.methodagg.ExprMinMaxAggrNode;
import com.espertech.esper.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.epl.generated.EsperEPL2GrammarParser;
import com.espertech.esper.epl.script.ExprNodeScript;
import com.espertech.esper.epl.spec.ExpressionDeclDesc;
import com.espertech.esper.epl.spec.ExpressionScriptProvided;
import com.espertech.esper.epl.spec.StatementSpecRaw;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionFactory;
import com.espertech.esper.util.LazyAllocatedMap;
import com.espertech.esper.util.StringValue;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.Tree;

import java.io.StringWriter;
import java.util.*;

public class ASTLibFunctionHelper {

    public static List<ExprChainedSpec> getLibFuncChain(List<EsperEPL2GrammarParser.LibFunctionNoClassContext> ctxs, Map<Tree, ExprNode> astExprNodeMap) {

        List<ExprChainedSpec> chained = new ArrayList<ExprChainedSpec>(ctxs.size());
        for (EsperEPL2GrammarParser.LibFunctionNoClassContext ctx : ctxs) {
            ExprChainedSpec chainSpec = ASTLibFunctionHelper.getLibFunctionChainSpec(ctx, astExprNodeMap);
            chained.add(chainSpec);
        }
        return chained;
    }

    public static ExprChainedSpec getLibFunctionChainSpec(EsperEPL2GrammarParser.LibFunctionNoClassContext ctx, Map<Tree, ExprNode> astExprNodeMap) {
        String methodName = StringValue.removeTicks(ctx.funcIdentChained().getText());

        List<ExprNode> parameters = getExprNodesLibFunc(ctx.libFunctionArgs(), astExprNodeMap);
        boolean property = ctx.l == null;
        return new ExprChainedSpec(methodName, parameters, property);
    }

    public static List<ExprNode> getExprNodesLibFunc(EsperEPL2GrammarParser.LibFunctionArgsContext ctx, Map<Tree, ExprNode> astExprNodeMap) {
        if (ctx == null) {
            return Collections.emptyList();
        }
        List<EsperEPL2GrammarParser.LibFunctionArgItemContext> args = ctx.libFunctionArgItem();
        if (args == null || args.isEmpty()) {
            return Collections.emptyList();
        }
        List<ExprNode> parameters = new ArrayList<ExprNode>(args.size());
        for (EsperEPL2GrammarParser.LibFunctionArgItemContext arg : args) {
            if (arg.expressionLambdaDecl() != null) {
                List<String> lambdaparams = getLambdaGoesParams(arg.expressionLambdaDecl());
                ExprLambdaGoesNode goes = new ExprLambdaGoesNode(lambdaparams);
                ExprNode lambdaExpr = ASTExprHelper.exprCollectSubNodes(arg.expressionWithNamed(), 0, astExprNodeMap).get(0);
                goes.addChildNode(lambdaExpr);
                parameters.add(goes);
            } else {
                ExprNode parameter = ASTExprHelper.exprCollectSubNodes(arg.expressionWithNamed(), 0, astExprNodeMap).get(0);
                parameters.add(parameter);
            }
        }
        return parameters;
    }

    protected static List<String> getLambdaGoesParams(EsperEPL2GrammarParser.ExpressionLambdaDeclContext ctx) {
        List<String> parameters;
        if (ctx.i != null) {
            parameters = new ArrayList<String>(1);
            parameters.add(ctx.i.getText());
        } else {
            parameters = ASTUtil.getIdentList(ctx.columnList());
        }
        return parameters;
    }

    public static void handleLibFunc(CommonTokenStream tokenStream,
                                     EsperEPL2GrammarParser.LibFunctionContext ctx,
                                     ConfigurationInformation configurationInformation,
                                     EngineImportService engineImportService,
                                     Map<Tree, ExprNode> astExprNodeMap,
                                     LazyAllocatedMap<ConfigurationPlugInAggregationMultiFunction, PlugInAggregationMultiFunctionFactory> plugInAggregations,
                                     String engineURI,
                                     ExpressionDeclDesc expressionDeclarations,
                                     ExprDeclaredService exprDeclaredService,
                                     List<ExpressionScriptProvided> scriptExpressions,
                                     ContextDescriptor contextDescriptor,
                                     TableService tableService,
                                     StatementSpecRaw statementSpec,
                                     VariableService variableService) {

        ASTLibModel model = getModel(ctx, tokenStream);
        boolean duckType = configurationInformation.getEngineDefaults().getExpression().isDuckTyping();
        boolean udfCache = configurationInformation.getEngineDefaults().getExpression().isUdfCache();

        // handle "some.xyz(...)" or "some.other.xyz(...)"
        if (model.chainElements.size() == 1 &&
                model.optionalClassIdent != null &&
                ASTTableExprHelper.checkTableNameGetExprForProperty(tableService, model.optionalClassIdent) == null) {

            ExprChainedSpec chainSpec = getLibFunctionChainSpec(model.chainElements.get(0), astExprNodeMap);

            ExprDeclaredNodeImpl declaredNode = ExprDeclaredHelper.getExistsDeclaredExpr(model.optionalClassIdent, Collections.<ExprNode>emptyList(), expressionDeclarations.getExpressions(), exprDeclaredService, contextDescriptor);
            if (declaredNode != null) {
                ExprNode exprNode = new ExprDotNodeImpl(Collections.singletonList(chainSpec), duckType, udfCache);
                exprNode.addChildNode(declaredNode);
                ASTExprHelper.exprCollectAddSubNodesAddParentNode(exprNode, ctx, astExprNodeMap);
                return;
            }

            List<ExprChainedSpec> chain = new ArrayList<ExprChainedSpec>(2);
            chain.add(new ExprChainedSpec(model.getOptionalClassIdent(), Collections.<ExprNode>emptyList(), true));
            chain.add(chainSpec);
            ExprDotNode dotNode = new ExprDotNodeImpl(chain, configurationInformation.getEngineDefaults().getExpression().isDuckTyping(), configurationInformation.getEngineDefaults().getExpression().isUdfCache());
            if (dotNode.isVariableOpGetName(variableService) != null) {
                statementSpec.setHasVariables(true);
            }
            ASTExprHelper.exprCollectAddSubNodesAddParentNode(dotNode, ctx, astExprNodeMap);
            return;
        }

        // try additional built-in single-row function
        ExprNode singleRowExtNode = engineImportService.resolveSingleRowExtendedBuiltin(model.getChainElements().get(0).getFuncName());
        if (singleRowExtNode != null) {
            if (model.chainElements.size() == 1) {
                ASTExprHelper.exprCollectAddSubNodesAddParentNode(singleRowExtNode, ctx, astExprNodeMap);
                return;
            }
            List<ExprChainedSpec> spec = new ArrayList<ExprChainedSpec>();
            EsperEPL2GrammarParser.LibFunctionArgsContext firstArgs = model.getChainElements().get(0).getArgs();
            List<ExprNode> childExpressions = ASTLibFunctionHelper.getExprNodesLibFunc(firstArgs, astExprNodeMap);
            singleRowExtNode.addChildNodes(childExpressions);
            addChainRemainderFromOffset(model.getChainElements(), 1, spec, astExprNodeMap);
            ExprDotNode dotNode = new ExprDotNodeImpl(spec, configurationInformation.getEngineDefaults().getExpression().isDuckTyping(), configurationInformation.getEngineDefaults().getExpression().isUdfCache());
            dotNode.addChildNode(singleRowExtNode);
            ASTExprHelper.exprCollectAddSubNodesAddParentNode(dotNode, ctx, astExprNodeMap);
            return;
        }

        // try plug-in single-row function
        try {
            String firstFunction = model.getChainElements().get(0).getFuncName();
            boolean firstFunctionIsProperty = !model.getChainElements().get(0).isHasLeftParen();
            Pair<Class, EngineImportSingleRowDesc> classMethodPair = engineImportService.resolveSingleRow(firstFunction);
            List<ExprChainedSpec> spec = new ArrayList<ExprChainedSpec>();
            EsperEPL2GrammarParser.LibFunctionArgsContext firstArgs = model.getChainElements().get(0).getArgs();
            List<ExprNode> childExpressions = ASTLibFunctionHelper.getExprNodesLibFunc(firstArgs, astExprNodeMap);
            spec.add(new ExprChainedSpec(classMethodPair.getSecond().getMethodName(), childExpressions, firstFunctionIsProperty));
            addChainRemainderFromOffset(model.getChainElements(), 1, spec, astExprNodeMap);
            ExprPlugInSingleRowNode plugin = new ExprPlugInSingleRowNode(firstFunction, classMethodPair.getFirst(), spec, classMethodPair.getSecond());
            ASTExprHelper.exprCollectAddSubNodesAddParentNode(plugin, ctx, astExprNodeMap);
            return;
        } catch (EngineImportUndefinedException e) {
            // Not an single-row function
        } catch (EngineImportException e) {
            throw new IllegalStateException("Error resolving single-row function: " + e.getMessage(), e);
        }

        // special case for min,max
        String firstFunction = model.getChainElements().get(0).getFuncName();
        if (model.getOptionalClassIdent() == null && (firstFunction.toLowerCase(Locale.ENGLISH).equals("max")) || (firstFunction.toLowerCase(Locale.ENGLISH).equals("min")) ||
                (firstFunction.toLowerCase(Locale.ENGLISH).equals("fmax")) || (firstFunction.toLowerCase(Locale.ENGLISH).equals("fmin"))) {
            EsperEPL2GrammarParser.LibFunctionArgsContext firstArgs = model.getChainElements().get(0).getArgs();
            handleMinMax(firstFunction, firstArgs, astExprNodeMap);
            if (model.getChainElements().size() <= 1) {
                return;
            }
            List<ExprChainedSpec> chain = new ArrayList<>();
            addChainRemainderFromOffset(model.getChainElements(), 1, chain, astExprNodeMap);
            ExprDotNodeImpl exprNode = new ExprDotNodeImpl(chain, duckType, udfCache);
            exprNode.addChildNode(astExprNodeMap.remove(firstArgs));
            astExprNodeMap.put(ctx, exprNode);
            return;
        }

        // obtain chain with actual expressions
        List<ExprChainedSpec> chain = new ArrayList<ExprChainedSpec>();
        addChainRemainderFromOffset(model.getChainElements(), 0, chain, astExprNodeMap);

        // add chain element for class info, if any
        boolean distinct = model.getChainElements().get(0).getArgs() != null && model.getChainElements().get(0).getArgs().DISTINCT() != null;
        if (model.getOptionalClassIdent() != null) {
            chain.add(0, new ExprChainedSpec(model.getOptionalClassIdent(), Collections.<ExprNode>emptyList(), true));
            distinct = false;
        }
        firstFunction = chain.get(0).getName();

        // try plug-in aggregation function
        ExprNode aggregationNode = ASTAggregationHelper.tryResolveAsAggregation(engineImportService, distinct, firstFunction, plugInAggregations, engineURI);
        if (aggregationNode != null) {
            ExprChainedSpec firstSpec = chain.remove(0);
            aggregationNode.addChildNodes(firstSpec.getParameters());
            ExprNode exprNode;
            if (chain.isEmpty()) {
                exprNode = aggregationNode;
            } else {
                exprNode = new ExprDotNodeImpl(chain, duckType, udfCache);
                exprNode.addChildNode(aggregationNode);
            }
            ASTExprHelper.exprCollectAddSubNodesAddParentNode(exprNode, ctx, astExprNodeMap);
            return;
        }

        // try declared or alias expression
        ExprDeclaredNodeImpl declaredNode = ExprDeclaredHelper.getExistsDeclaredExpr(firstFunction, chain.get(0).getParameters(), expressionDeclarations.getExpressions(), exprDeclaredService, contextDescriptor);
        if (declaredNode != null) {
            chain.remove(0);
            ExprNode exprNode;
            if (chain.isEmpty()) {
                exprNode = declaredNode;
            } else {
                exprNode = new ExprDotNodeImpl(chain, duckType, udfCache);
                exprNode.addChildNode(declaredNode);
            }
            ASTExprHelper.exprCollectAddSubNodesAddParentNode(exprNode, ctx, astExprNodeMap);
            return;
        }

        // try script
        ExprNodeScript scriptNode = ExprDeclaredHelper.getExistsScript(configurationInformation.getEngineDefaults().getScripts().getDefaultDialect(), chain.get(0).getName(), chain.get(0).getParameters(), scriptExpressions, exprDeclaredService);
        if (scriptNode != null) {
            chain.remove(0);
            ExprNode exprNode;
            if (chain.isEmpty()) {
                exprNode = scriptNode;
            } else {
                exprNode = new ExprDotNodeImpl(chain, duckType, udfCache);
                exprNode.addChildNode(scriptNode);
            }
            ASTExprHelper.exprCollectAddSubNodesAddParentNode(exprNode, ctx, astExprNodeMap);
            return;
        }

        // try table
        Pair<ExprTableAccessNode, List<ExprChainedSpec>> tableInfo = ASTTableExprHelper.checkTableNameGetLibFunc(tableService, engineImportService, plugInAggregations, engineURI, firstFunction, chain);
        if (tableInfo != null) {
            ASTTableExprHelper.addTableExpressionReference(statementSpec, tableInfo.getFirst());
            chain = tableInfo.getSecond();
            ExprNode exprNode;
            if (chain.isEmpty()) {
                exprNode = tableInfo.getFirst();
            } else {
                exprNode = new ExprDotNodeImpl(chain, duckType, udfCache);
                exprNode.addChildNode(tableInfo.getFirst());
            }
            ASTExprHelper.exprCollectAddSubNodesAddParentNode(exprNode, ctx, astExprNodeMap);
            return;
        }

        // Could be a mapped property with an expression-parameter "mapped(expr)" or array property with an expression-parameter "array(expr)".
        ExprDotNode dotNode;
        if (chain.size() == 1) {
            dotNode = new ExprDotNodeImpl(chain, false, false);
        } else {
            dotNode = new ExprDotNodeImpl(chain, duckType, udfCache);
        }
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(dotNode, ctx, astExprNodeMap);
    }

    private static void addChainRemainderFromOffset(List<ASTLibModelChainElement> chainElements, int offset, List<ExprChainedSpec> specList, Map<Tree, ExprNode> astExprNodeMap) {
        for (int i = offset; i < chainElements.size(); i++) {
            ExprChainedSpec spec = getLibFunctionChainSpec(chainElements.get(i), astExprNodeMap);
            specList.add(spec);
        }
    }

    private static ExprChainedSpec getLibFunctionChainSpec(ASTLibModelChainElement element, Map<Tree, ExprNode> astExprNodeMap) {
        String methodName = StringValue.removeTicks(element.getFuncName());
        List<ExprNode> parameters = ASTLibFunctionHelper.getExprNodesLibFunc(element.getArgs(), astExprNodeMap);
        return new ExprChainedSpec(methodName, parameters, !element.isHasLeftParen());
    }

    private static ASTLibModel getModel(EsperEPL2GrammarParser.LibFunctionContext ctx, CommonTokenStream tokenStream) {
        EsperEPL2GrammarParser.LibFunctionWithClassContext root = ctx.libFunctionWithClass();
        List<EsperEPL2GrammarParser.LibFunctionNoClassContext> ctxElements = ctx.libFunctionNoClass();

        // there are no additional methods
        if (ctxElements == null || ctxElements.isEmpty()) {
            String classIdent = root.classIdentifier() == null ? null : ASTUtil.unescapeClassIdent(root.classIdentifier());
            ASTLibModelChainElement ele = fromRoot(root);
            return new ASTLibModel(classIdent, Collections.singletonList(ele));
        }

        // add root and chain to just a list of elements
        List<ASTLibModelChainElement> chainElements = new ArrayList<ASTLibModelChainElement>(ctxElements.size() + 1);
        ASTLibModelChainElement rootElement = fromRoot(root);
        chainElements.add(rootElement);
        for (EsperEPL2GrammarParser.LibFunctionNoClassContext chainedCtx : ctxElements) {
            ASTLibModelChainElement chainedElement = new ASTLibModelChainElement(chainedCtx.funcIdentChained().getText(), chainedCtx.libFunctionArgs(), chainedCtx.l != null);
            chainElements.add(chainedElement);
        }

        // determine/remove the list of chain elements, from the start and uninterrupted, that don't have parameters (no parenthesis 'l')
        List<ASTLibModelChainElement> chainElementsNoArgs = new ArrayList<ASTLibModelChainElement>(chainElements.size());
        Iterator<ASTLibModelChainElement> iterator = chainElements.iterator();
        for (; iterator.hasNext(); ) {
            ASTLibModelChainElement element = iterator.next();
            if (!element.isHasLeftParen()) {    // has no parenthesis, therefore part of class identifier
                chainElementsNoArgs.add(element);
                iterator.remove(); //
            } else { // else stop here
                break;
            }
        }

        // write the class identifier including the no-arg chain elements
        StringWriter classIdentBuf = new StringWriter();
        String delimiter = "";
        if (root.classIdentifier() != null) {
            classIdentBuf.append(ASTUtil.unescapeClassIdent(root.classIdentifier()));
            delimiter = ".";
        }
        for (ASTLibModelChainElement noarg : chainElementsNoArgs) {
            classIdentBuf.append(delimiter);
            classIdentBuf.append(noarg.getFuncName());
            delimiter = ".";
        }

        if (chainElements.isEmpty()) {
            // would this be an event property, but then that is handled greedily by parser
            throw ASTWalkException.from("Encountered unrecognized lib function call", tokenStream, ctx);
        }

        // class ident can be null if empty
        String classIdentifierString = classIdentBuf.toString();
        String classIdentifier = classIdentifierString.length() > 0 ? classIdentifierString : null;

        return new ASTLibModel(classIdentifier, chainElements);
    }

    public static ASTLibModelChainElement fromRoot(EsperEPL2GrammarParser.LibFunctionWithClassContext root) {
        if (root.funcIdentTop() != null) {
            return new ASTLibModelChainElement(root.funcIdentTop().getText(), root.libFunctionArgs(), root.l != null);
        } else {
            return new ASTLibModelChainElement(root.funcIdentInner().getText(), root.libFunctionArgs(), root.l != null);
        }
    }

    // Min/Max nodes can be either an aggregate or a per-row function depending on the number or arguments
    private static void handleMinMax(String ident, EsperEPL2GrammarParser.LibFunctionArgsContext ctxArgs, Map<Tree, ExprNode> astExprNodeMap) {
        // Determine min or max
        String childNodeText = ident;
        MinMaxTypeEnum minMaxTypeEnum;
        boolean filtered = childNodeText.startsWith("f");
        if (childNodeText.toLowerCase(Locale.ENGLISH).equals("min") || childNodeText.toLowerCase(Locale.ENGLISH).equals("fmin")) {
            minMaxTypeEnum = MinMaxTypeEnum.MIN;
        } else if (childNodeText.toLowerCase(Locale.ENGLISH).equals("max") || childNodeText.toLowerCase(Locale.ENGLISH).equals("fmax")) {
            minMaxTypeEnum = MinMaxTypeEnum.MAX;
        } else {
            throw ASTWalkException.from("Uncountered unrecognized min or max node '" + ident + "'");
        }

        List<ExprNode> args = Collections.emptyList();
        if (ctxArgs != null && ctxArgs.libFunctionArgItem() != null) {
            args = ASTExprHelper.exprCollectSubNodes(ctxArgs, 0, astExprNodeMap);
        }
        int numArgsPositional = ExprAggregateNodeUtil.countPositionalArgs(args);

        boolean isDistinct = ctxArgs != null && ctxArgs.DISTINCT() != null;
        if (numArgsPositional > 1 && isDistinct && !filtered) {
            throw ASTWalkException.from("The distinct keyword is not valid in per-row min and max " +
                    "functions with multiple sub-expressions");
        }

        ExprNode minMaxNode;
        if (!isDistinct && numArgsPositional > 1 && !filtered) {
            // use the row function
            minMaxNode = new ExprMinMaxRowNode(minMaxTypeEnum);
        } else {
            // use the aggregation function
            minMaxNode = new ExprMinMaxAggrNode(isDistinct, minMaxTypeEnum, filtered, false);
        }
        minMaxNode.addChildNodes(args);
        astExprNodeMap.put(ctxArgs, minMaxNode);
    }

    public static class ASTLibModel {
        private final String optionalClassIdent;
        private final List<ASTLibModelChainElement> chainElements;

        public ASTLibModel(String optionalClassIdent, List<ASTLibModelChainElement> chainElements) {
            this.optionalClassIdent = optionalClassIdent;
            this.chainElements = chainElements;
        }

        public String getOptionalClassIdent() {
            return optionalClassIdent;
        }

        public List<ASTLibModelChainElement> getChainElements() {
            return chainElements;
        }
    }

    public static class ASTLibModelChainElement {
        private final String funcName;
        private final EsperEPL2GrammarParser.LibFunctionArgsContext args;
        private final boolean hasLeftParen;

        public ASTLibModelChainElement(String funcName, EsperEPL2GrammarParser.LibFunctionArgsContext args, boolean hasLeftParen) {
            this.funcName = funcName;
            this.args = args;
            this.hasLeftParen = hasLeftParen;
        }

        public String getFuncName() {
            return funcName;
        }

        public EsperEPL2GrammarParser.LibFunctionArgsContext getArgs() {
            return args;
        }

        public boolean isHasLeftParen() {
            return hasLeftParen;
        }
    }
}
