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
import com.espertech.esper.client.EPException;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.context.mgr.ContextManagementService;
import com.espertech.esper.core.context.util.ContextDescriptor;
import com.espertech.esper.core.service.EPAdministratorHelper;
import com.espertech.esper.epl.agg.access.AggregationStateType;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.db.DatabasePollingViewableFactory;
import com.espertech.esper.epl.declexpr.ExprDeclaredHelper;
import com.espertech.esper.epl.declexpr.ExprDeclaredNodeImpl;
import com.espertech.esper.epl.declexpr.ExprDeclaredService;
import com.espertech.esper.epl.expression.accessagg.ExprAggMultiFunctionLinearAccessNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.dot.ExprDotNode;
import com.espertech.esper.epl.expression.dot.ExprDotNodeImpl;
import com.espertech.esper.epl.expression.funcs.*;
import com.espertech.esper.epl.expression.methodagg.*;
import com.espertech.esper.epl.expression.ops.*;
import com.espertech.esper.epl.expression.prev.ExprPreviousNode;
import com.espertech.esper.epl.expression.prev.ExprPreviousNodePreviousType;
import com.espertech.esper.epl.expression.prior.ExprPriorNode;
import com.espertech.esper.epl.expression.subquery.*;
import com.espertech.esper.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.epl.expression.table.ExprTableAccessNodeSubprop;
import com.espertech.esper.epl.expression.table.ExprTableAccessNodeTopLevel;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;
import com.espertech.esper.epl.expression.time.ExprTimestampNode;
import com.espertech.esper.epl.generated.EsperEPL2GrammarLexer;
import com.espertech.esper.epl.generated.EsperEPL2GrammarListener;
import com.espertech.esper.epl.generated.EsperEPL2GrammarParser;
import com.espertech.esper.epl.script.ExprNodeScript;
import com.espertech.esper.epl.spec.*;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.variable.VariableMetaData;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.epl.variable.VariableServiceUtil;
import com.espertech.esper.event.property.PropertyParser;
import com.espertech.esper.pattern.EvalFactoryNode;
import com.espertech.esper.pattern.PatternLevelAnnotationFlags;
import com.espertech.esper.pattern.PatternLevelAnnotationUtil;
import com.espertech.esper.pattern.PatternNodeFactory;
import com.espertech.esper.pattern.guard.GuardEnum;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionFactory;
import com.espertech.esper.rowregex.*;
import com.espertech.esper.type.*;
import com.espertech.esper.util.*;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Tree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Called during the walks of a EPL expression AST tree as specified in the grammar file.
 * Constructs filter and view specifications etc.
 */
public class EPLTreeWalkerListener implements EsperEPL2GrammarListener {
    private static final Logger log = LoggerFactory.getLogger(EPLTreeWalkerListener.class);

    private final static Set<Integer> EVENT_FILTER_WALK_EXCEPTIONS_RECURSIVE = new HashSet<>();
    private final static Set<Integer> WHERE_CLAUSE_WALK_EXCEPTIONS_RECURSIVE = new HashSet<>();
    private final static Set<Integer> EVENT_PROPERTY_WALK_EXCEPTIONS_PARENT = new HashSet<>();
    private final static Set<Integer> SELECT_EXPRELE_WALK_EXCEPTIONS_RECURSIVE = new HashSet<>();

    static {
        EVENT_FILTER_WALK_EXCEPTIONS_RECURSIVE.add(EsperEPL2GrammarParser.RULE_createContextDetail);
        EVENT_FILTER_WALK_EXCEPTIONS_RECURSIVE.add(EsperEPL2GrammarParser.RULE_createContextFilter);
        EVENT_FILTER_WALK_EXCEPTIONS_RECURSIVE.add(EsperEPL2GrammarParser.RULE_createContextPartitionItem);
        EVENT_FILTER_WALK_EXCEPTIONS_RECURSIVE.add(EsperEPL2GrammarParser.RULE_createContextCoalesceItem);

        WHERE_CLAUSE_WALK_EXCEPTIONS_RECURSIVE.add(EsperEPL2GrammarParser.RULE_patternExpression);
        WHERE_CLAUSE_WALK_EXCEPTIONS_RECURSIVE.add(EsperEPL2GrammarParser.RULE_mergeMatchedItem);
        WHERE_CLAUSE_WALK_EXCEPTIONS_RECURSIVE.add(EsperEPL2GrammarParser.RULE_mergeInsert);
        WHERE_CLAUSE_WALK_EXCEPTIONS_RECURSIVE.add(EsperEPL2GrammarParser.RULE_updateDetails);
        WHERE_CLAUSE_WALK_EXCEPTIONS_RECURSIVE.add(EsperEPL2GrammarParser.RULE_onSetExpr);
        WHERE_CLAUSE_WALK_EXCEPTIONS_RECURSIVE.add(EsperEPL2GrammarParser.RULE_onUpdateExpr);

        EVENT_PROPERTY_WALK_EXCEPTIONS_PARENT.add(EsperEPL2GrammarParser.RULE_newAssign);
        EVENT_PROPERTY_WALK_EXCEPTIONS_PARENT.add(EsperEPL2GrammarParser.RULE_createContextPartitionItem);
        EVENT_PROPERTY_WALK_EXCEPTIONS_PARENT.add(EsperEPL2GrammarParser.RULE_createContextDetail);
        EVENT_PROPERTY_WALK_EXCEPTIONS_PARENT.add(EsperEPL2GrammarParser.RULE_createContextFilter);
        EVENT_PROPERTY_WALK_EXCEPTIONS_PARENT.add(EsperEPL2GrammarParser.RULE_createContextCoalesceItem);

        SELECT_EXPRELE_WALK_EXCEPTIONS_RECURSIVE.add(EsperEPL2GrammarParser.RULE_mergeInsert);
    }

    // private holding areas for accumulated info
    private Map<Tree, ExprNode> astExprNodeMap = new LinkedHashMap<>();
    private final Stack<Map<Tree, ExprNode>> astExprNodeMapStack;

    private final Map<Tree, EvalFactoryNode> astPatternNodeMap = new LinkedHashMap<>();

    private final Map<Tree, RowRegexExprNode> astRowRegexNodeMap = new HashMap<>();

    private final Map<Tree, Object> astGOPNodeMap = new HashMap<>();

    private final Map<Tree, StatementSpecRaw> astStatementSpecMap = new HashMap<>();

    private Map<StatementSpecRaw, OnTriggerSplitStreamFromClause> onTriggerSplitPropertyEvals;

    private LazyAllocatedMap<ConfigurationPlugInAggregationMultiFunction, PlugInAggregationMultiFunctionFactory> plugInAggregations = new LazyAllocatedMap<>();

    private FilterSpecRaw filterSpec;
    private final List<ViewSpec> viewSpecs = new LinkedList<>();

    // AST Walk result
    private List<ExprSubstitutionNode> substitutionParamNodes = new ArrayList<>();
    private StatementSpecRaw statementSpec;
    private final Stack<StatementSpecRaw> statementSpecStack;

    private List<SelectClauseElementRaw> propertySelectRaw;
    private PropertyEvalSpec propertyEvalSpec;
    private List<OnTriggerMergeMatched> mergeMatcheds;
    private List<OnTriggerMergeAction> mergeActions;
    private OnTriggerMergeActionInsert mergeInsertNoMatch;
    private ContextDescriptor contextDescriptor;

    private final CommonTokenStream tokenStream;
    private final EngineImportService engineImportService;
    private final VariableService variableService;
    private final SelectClauseStreamSelectorEnum defaultStreamSelector;
    private final String engineURI;
    private final ConfigurationInformation configurationInformation;
    private final PatternNodeFactory patternNodeFactory;
    private final ContextManagementService contextManagementService;
    private final List<String> scriptBodies;
    private final ExprDeclaredService exprDeclaredService;
    private final List<ExpressionScriptProvided> scriptExpressions;
    private final ExpressionDeclDesc expressionDeclarations;
    private final TableService tableService;

    public EPLTreeWalkerListener(CommonTokenStream tokenStream,
                                 EngineImportService engineImportService,
                                 VariableService variableService,
                                 SelectClauseStreamSelectorEnum defaultStreamSelector,
                                 String engineURI,
                                 ConfigurationInformation configurationInformation,
                                 PatternNodeFactory patternNodeFactory,
                                 ContextManagementService contextManagementService,
                                 List<String> scriptBodies,
                                 ExprDeclaredService exprDeclaredService,
                                 TableService tableService) {
        this.tokenStream = tokenStream;
        this.engineImportService = engineImportService;
        this.variableService = variableService;
        this.defaultStreamSelector = defaultStreamSelector;
        this.patternNodeFactory = patternNodeFactory;
        this.engineURI = engineURI;
        this.configurationInformation = configurationInformation;
        this.contextManagementService = contextManagementService;
        this.scriptBodies = scriptBodies;
        this.exprDeclaredService = exprDeclaredService;
        this.tableService = tableService;

        if (defaultStreamSelector == null) {
            throw ASTWalkException.from("Default stream selector is null");
        }

        statementSpec = new StatementSpecRaw(defaultStreamSelector);
        statementSpecStack = new Stack<>();
        astExprNodeMapStack = new Stack<>();

        // statement-global items
        expressionDeclarations = new ExpressionDeclDesc();
        statementSpec.setExpressionDeclDesc(expressionDeclarations);
        scriptExpressions = new ArrayList<>(1);
        statementSpec.setScriptExpressions(scriptExpressions);
    }

    /**
     * Pushes a statement into the stack, creating a new empty statement to fill in.
     * The leave node method for lookup statements pops from the stack.
     * The leave node method for lookup statements pops from the stack.
     */
    private void pushStatementContext() {
        statementSpecStack.push(statementSpec);
        astExprNodeMapStack.push(astExprNodeMap);

        statementSpec = new StatementSpecRaw(defaultStreamSelector);
        astExprNodeMap = new HashMap<>();
    }

    private void popStatementContext(ParseTree ctx) {
        StatementSpecRaw currentSpec = statementSpec;
        statementSpec = statementSpecStack.pop();
        if (currentSpec.isHasVariables()) {
            statementSpec.setHasVariables(true);
        }
        ASTTableExprHelper.addTableExpressionReference(statementSpec, currentSpec.getTableExpressions());
        if (currentSpec.getReferencedVariables() != null) {
            for (String var : currentSpec.getReferencedVariables()) {
                ASTExprHelper.addVariableReference(statementSpec, var);
            }
        }
        astExprNodeMap = astExprNodeMapStack.pop();
        astStatementSpecMap.put(ctx, currentSpec);
    }

    public StatementSpecRaw getStatementSpec() {
        return statementSpec;
    }

    public void exitContextExpr(EsperEPL2GrammarParser.ContextExprContext ctx) {
        String contextName = ctx.i.getText();
        statementSpec.setOptionalContextName(contextName);
        contextDescriptor = contextManagementService.getContextDescriptor(contextName);
    }

    public void exitEvalRelationalExpression(EsperEPL2GrammarParser.EvalRelationalExpressionContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        boolean isNot = ctx.n != null;
        ExprNode exprNode;
        if (ctx.like != null) {
            exprNode = new ExprLikeNode(isNot);
        } else if (ctx.in != null && ctx.col != null) { // range
            boolean isLowInclude = ctx.LBRACK() != null;
            boolean isHighInclude = ctx.RBRACK() != null;
            exprNode = new ExprBetweenNodeImpl(isLowInclude, isHighInclude, isNot);
        } else if (ctx.in != null) {
            exprNode = new ExprInNodeImpl(isNot);
        } else if (ctx.inSubSelectQuery() != null) {
            StatementSpecRaw currentSpec = astStatementSpecMap.remove(ctx.inSubSelectQuery().subQueryExpr());
            exprNode = new ExprSubselectInNode(currentSpec, isNot);
        } else if (ctx.between != null) {
            exprNode = new ExprBetweenNodeImpl(true, true, isNot);
        } else if (ctx.regex != null) {
            exprNode = new ExprRegexpNode(isNot);
        } else if (ctx.r != null) {
            RelationalOpEnum relationalOpEnum;
            switch (ctx.r.getType()) {
                case EsperEPL2GrammarLexer.LT:
                    relationalOpEnum = RelationalOpEnum.LT;
                    break;
                case EsperEPL2GrammarLexer.GT:
                    relationalOpEnum = RelationalOpEnum.GT;
                    break;
                case EsperEPL2GrammarLexer.LE:
                    relationalOpEnum = RelationalOpEnum.LE;
                    break;
                case EsperEPL2GrammarLexer.GE:
                    relationalOpEnum = RelationalOpEnum.GE;
                    break;
                default:
                    throw ASTWalkException.from("Encountered unrecognized node type " + ctx.r.getType(), tokenStream, ctx);
            }

            boolean isAll = ctx.g != null && ctx.g.getType() == EsperEPL2GrammarLexer.ALL;
            boolean isAny = ctx.g != null && (ctx.g.getType() == EsperEPL2GrammarLexer.ANY || ctx.g.getType() == EsperEPL2GrammarLexer.SOME);

            if (isAll || isAny) {
                if (ctx.subSelectGroupExpression() != null && !ctx.subSelectGroupExpression().isEmpty()) {
                    StatementSpecRaw currentSpec = astStatementSpecMap.remove(ctx.subSelectGroupExpression().get(0).subQueryExpr());
                    exprNode = new ExprSubselectAllSomeAnyNode(currentSpec, false, isAll, relationalOpEnum);
                } else {
                    exprNode = new ExprRelationalOpAllAnyNode(relationalOpEnum, isAll);
                }
            } else {
                exprNode = new ExprRelationalOpNodeImpl(relationalOpEnum);
            }
        } else {
            throw ASTWalkException.from("Encountered unrecognized relational op", tokenStream, ctx);
        }
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(exprNode, ctx, astExprNodeMap);
        if (ctx.like != null && ctx.stringconstant() != null) {
            exprNode.addChildNode(new ExprConstantNodeImpl(ASTConstantHelper.parse(ctx.stringconstant())));
        }
    }

    public void exitLibFunction(EsperEPL2GrammarParser.LibFunctionContext ctx) {
        ASTLibFunctionHelper.handleLibFunc(tokenStream, ctx, configurationInformation, engineImportService, astExprNodeMap, plugInAggregations, engineURI, expressionDeclarations, exprDeclaredService, scriptExpressions, contextDescriptor, tableService, statementSpec, variableService);
    }

    public void exitMatchRecog(EsperEPL2GrammarParser.MatchRecogContext ctx) {
        boolean allMatches = ctx.matchRecogMatchesSelection() != null && ctx.matchRecogMatchesSelection().ALL() != null;
        if (ctx.matchRecogMatchesAfterSkip() != null) {
            MatchRecognizeSkipEnum skip = ASTMatchRecognizeHelper.parseSkip(tokenStream, ctx.matchRecogMatchesAfterSkip());
            statementSpec.getMatchRecognizeSpec().getSkip().setSkip(skip);
        }

        if (ctx.matchRecogMatchesInterval() != null) {
            if (!ctx.matchRecogMatchesInterval().i.getText().toLowerCase(Locale.ENGLISH).equals("interval")) {
                throw ASTWalkException.from("Invalid interval-clause within match-recognize, expecting keyword INTERVAL", tokenStream, ctx.matchRecogMatchesInterval());
            }
            ExprNode expression = ASTExprHelper.exprCollectSubNodes(ctx.matchRecogMatchesInterval().timePeriod(), 0, astExprNodeMap).get(0);
            ExprTimePeriod timePeriodExpr = (ExprTimePeriod) expression;
            boolean orTerminated = ctx.matchRecogMatchesInterval().TERMINATED() != null;
            statementSpec.getMatchRecognizeSpec().setInterval(new MatchRecognizeInterval(timePeriodExpr, orTerminated));
        }

        statementSpec.getMatchRecognizeSpec().setAllMatches(allMatches);
    }

    public void exitMatchRecogPartitionBy(EsperEPL2GrammarParser.MatchRecogPartitionByContext ctx) {
        if (statementSpec.getMatchRecognizeSpec() == null) {
            statementSpec.setMatchRecognizeSpec(new MatchRecognizeSpec());
        }
        List<ExprNode> nodes = ASTExprHelper.exprCollectSubNodes(ctx, 0, astExprNodeMap);
        statementSpec.getMatchRecognizeSpec().getPartitionByExpressions().addAll(nodes);
    }

    public void exitMergeMatchedItem(EsperEPL2GrammarParser.MergeMatchedItemContext ctx) {
        if (mergeActions == null) {
            mergeActions = new ArrayList<>();
        }
        ExprNode whereCond = null;
        if (ctx.whereClause() != null) {
            whereCond = ASTExprHelper.exprCollectSubNodes(ctx.whereClause(), 0, astExprNodeMap).get(0);
        }
        if (ctx.d != null) {
            mergeActions.add(new OnTriggerMergeActionDelete(whereCond));
        }
        if (ctx.u != null) {
            List<OnTriggerSetAssignment> sets = ASTExprHelper.getOnTriggerSetAssignments(ctx.onSetAssignmentList(), astExprNodeMap);
            mergeActions.add(new OnTriggerMergeActionUpdate(whereCond, sets));
        }
        if (ctx.mergeInsert() != null) {
            handleMergeInsert(ctx.mergeInsert());
        }
    }

    public void enterSubQueryExpr(EsperEPL2GrammarParser.SubQueryExprContext ctx) {
        pushStatementContext();
    }

    public void exitSubQueryExpr(EsperEPL2GrammarParser.SubQueryExprContext ctx) {
        popStatementContext(ctx);
    }

    public void exitMatchRecogDefineItem(EsperEPL2GrammarParser.MatchRecogDefineItemContext ctx) {
        String first = ctx.i.getText();
        ExprNode exprNode = ASTExprHelper.exprCollectSubNodes(ctx, 0, astExprNodeMap).get(0);
        statementSpec.getMatchRecognizeSpec().getDefines().add(new MatchRecognizeDefineItem(first, exprNode));
    }

    public void exitOnMergeDirectInsert(EsperEPL2GrammarParser.OnMergeDirectInsertContext ctx) {
        List<SelectClauseElementRaw> expressions = new ArrayList<>(statementSpec.getSelectClauseSpec().getSelectExprList());
        statementSpec.getSelectClauseSpec().getSelectExprList().clear();
        List<String> columsList = ASTUtil.getIdentList(ctx.columnList());
        mergeInsertNoMatch = new OnTriggerMergeActionInsert(null, null, columsList, expressions);
    }

    public void exitMergeUnmatchedItem(EsperEPL2GrammarParser.MergeUnmatchedItemContext ctx) {
        if (mergeActions == null) {
            mergeActions = new ArrayList<>();
        }
        handleMergeInsert(ctx.mergeInsert());
    }

    public void exitHavingClause(EsperEPL2GrammarParser.HavingClauseContext ctx) {
        if (astExprNodeMap.size() != 1) {
            throw new IllegalStateException("Having clause generated zero or more then one expression nodes");
        }
        statementSpec.setHavingExprRootNode(ASTExprHelper.exprCollectSubNodes(ctx, 0, astExprNodeMap).get(0));
        astExprNodeMap.clear();
    }

    public void exitMatchRecogMeasureItem(EsperEPL2GrammarParser.MatchRecogMeasureItemContext ctx) {
        if (statementSpec.getMatchRecognizeSpec() == null) {
            statementSpec.setMatchRecognizeSpec(new MatchRecognizeSpec());
        }
        ExprNode exprNode = ASTExprHelper.exprCollectSubNodes(ctx, 0, astExprNodeMap).get(0);
        String name = ctx.i != null ? ctx.i.getText() : null;
        statementSpec.getMatchRecognizeSpec().addMeasureItem(new MatchRecognizeMeasureItem(exprNode, name));
    }

    public void exitObserverExpression(EsperEPL2GrammarParser.ObserverExpressionContext ctx) {
        String objectNamespace = ctx.ns.getText();
        String objectName = ctx.a != null ? ctx.a.getText() : ctx.nm.getText();
        List<ExprNode> obsParameters = ASTExprHelper.exprCollectSubNodes(ctx, 2, astExprNodeMap);

        PatternObserverSpec observerSpec = new PatternObserverSpec(objectNamespace, objectName, obsParameters);
        EvalFactoryNode observerNode = this.patternNodeFactory.makeObserverNode(observerSpec);
        ASTExprHelper.patternCollectAddSubnodesAddParentNode(observerNode, ctx, astPatternNodeMap);
    }

    public void exitMatchRecogPatternNested(EsperEPL2GrammarParser.MatchRecogPatternNestedContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        RegexNFATypeEnum type = RegexNFATypeEnum.SINGLE;
        if (ctx.s != null) {
            type = RegexNFATypeEnum.fromString(ctx.s.getText(), null);
        }
        RowRegexExprRepeatDesc repeat = ASTMatchRecognizeHelper.walkOptionalRepeat(ctx.matchRecogPatternRepeat(), astExprNodeMap);
        RowRegexExprNodeNested nestedNode = new RowRegexExprNodeNested(type, repeat);
        ASTExprHelper.regExCollectAddSubNodesAddParentNode(nestedNode, ctx, astRowRegexNodeMap);
    }

    public void exitMatchRecogPatternPermute(EsperEPL2GrammarParser.MatchRecogPatternPermuteContext ctx) {
        RowRegexExprNodePermute permuteNode = new RowRegexExprNodePermute();
        ASTExprHelper.regExCollectAddSubNodesAddParentNode(permuteNode, ctx, astRowRegexNodeMap);
    }

    public void exitEvalOrExpression(EsperEPL2GrammarParser.EvalOrExpressionContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        ExprOrNode or = new ExprOrNode();
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(or, ctx, astExprNodeMap);
    }

    public void exitTimePeriod(EsperEPL2GrammarParser.TimePeriodContext ctx) {
        ExprTimePeriod timeNode = ASTExprHelper.timePeriodGetExprAllParams(ctx, astExprNodeMap, variableService, statementSpec, configurationInformation, engineImportService.getTimeAbacus());
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(timeNode, ctx, astExprNodeMap);
    }

    public void exitSelectionListElementExpr(EsperEPL2GrammarParser.SelectionListElementExprContext ctx) {
        ExprNode exprNode;
        if (ASTUtil.isRecursiveParentRule(ctx, SELECT_EXPRELE_WALK_EXCEPTIONS_RECURSIVE)) {
            exprNode = ASTExprHelper.exprCollectSubNodes(ctx, 0, astExprNodeMap).get(0);
        } else {
            if ((astExprNodeMap.size() > 1) || ((astExprNodeMap.isEmpty()))) {
                throw ASTWalkException.from("Unexpected AST tree contains zero or more then 1 child element for root", tokenStream, ctx);
            }
            exprNode = astExprNodeMap.values().iterator().next();
            astExprNodeMap.clear();
        }

        // Get list element name
        String optionalName = null;
        if (ctx.keywordAllowedIdent() != null) {
            optionalName = ctx.keywordAllowedIdent().getText();
        }

        boolean eventsAnnotation = false;
        if (ctx.selectionListElementAnno() != null) {
            String annotation = ctx.selectionListElementAnno().i.getText().toLowerCase(Locale.ENGLISH);
            if (annotation.equals("eventbean")) {
                eventsAnnotation = true;
            } else {
                throw ASTWalkException.from("Failed to recognize select-expression annotation '" + annotation + "', expected 'eventbean'", tokenStream, ctx);
            }
        }

        // Add as selection element
        statementSpec.getSelectClauseSpec().add(new SelectClauseExprRawSpec(exprNode, optionalName, eventsAnnotation));
    }

    public void exitEventFilterExpression(EsperEPL2GrammarParser.EventFilterExpressionContext ctx) {
        if (ASTUtil.isRecursiveParentRule(ctx, EVENT_FILTER_WALK_EXCEPTIONS_RECURSIVE)) {
            return;
        }

        // for event streams we keep the filter spec around for use when the stream definition is completed
        filterSpec = ASTFilterSpecHelper.walkFilterSpec(ctx, propertyEvalSpec, astExprNodeMap);

        // set property eval to null
        propertyEvalSpec = null;

        astExprNodeMap.clear();
    }

    public void exitMatchRecogPatternConcat(EsperEPL2GrammarParser.MatchRecogPatternConcatContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        RowRegexExprNodeConcatenation concatNode = new RowRegexExprNodeConcatenation();
        ASTExprHelper.regExCollectAddSubNodesAddParentNode(concatNode, ctx, astRowRegexNodeMap);
    }

    public void exitNumberconstant(EsperEPL2GrammarParser.NumberconstantContext ctx) {
        // if the parent is constant, don't need an expression
        if (ctx.getParent().getRuleContext().getRuleIndex() == EsperEPL2GrammarParser.RULE_constant) {
            return;
        }
        ExprConstantNode constantNode = new ExprConstantNodeImpl(ASTConstantHelper.parse(ctx));
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(constantNode, ctx, astExprNodeMap);
    }

    public void exitMatchRecogPattern(EsperEPL2GrammarParser.MatchRecogPatternContext ctx) {
        RowRegexExprNode exprNode = ASTExprHelper.regExGetRemoveTopNode(ctx, astRowRegexNodeMap);
        if (exprNode == null) {
            throw new IllegalStateException("Expression node for AST node not found");
        }
        statementSpec.getMatchRecognizeSpec().setPattern(exprNode);
    }

    public void exitWhereClause(EsperEPL2GrammarParser.WhereClauseContext ctx) {
        if (ctx.getParent().getRuleIndex() != EsperEPL2GrammarParser.RULE_subQueryExpr &&
                ASTUtil.isRecursiveParentRule(ctx, WHERE_CLAUSE_WALK_EXCEPTIONS_RECURSIVE)) { // ignore pattern
            return;
        }
        if (astExprNodeMap.size() != 1) {
            throw new IllegalStateException("Where clause generated zero or more then one expression nodes");
        }

        // Just assign the single root ExprNode not consumed yet
        statementSpec.setFilterExprRootNode(astExprNodeMap.values().iterator().next());
        astExprNodeMap.clear();
    }

    public void exitMatchRecogPatternAtom(EsperEPL2GrammarParser.MatchRecogPatternAtomContext ctx) {
        String first = ctx.i.getText();
        RegexNFATypeEnum type = RegexNFATypeEnum.SINGLE;
        if (ctx.reluctant != null && ctx.s != null) {
            type = RegexNFATypeEnum.fromString(ctx.s.getText(), ctx.reluctant.getText());
        } else if (ctx.s != null) {
            type = RegexNFATypeEnum.fromString(ctx.s.getText(), null);
        }

        RowRegexExprRepeatDesc repeat = ASTMatchRecognizeHelper.walkOptionalRepeat(ctx.matchRecogPatternRepeat(), astExprNodeMap);
        RowRegexExprNodeAtom item = new RowRegexExprNodeAtom(first, type, repeat);
        ASTExprHelper.regExCollectAddSubNodesAddParentNode(item, ctx, astRowRegexNodeMap);
    }

    public void exitUpdateExpr(EsperEPL2GrammarParser.UpdateExprContext ctx) {
        EsperEPL2GrammarParser.UpdateDetailsContext updctx = ctx.updateDetails();
        String eventTypeName = ASTUtil.unescapeClassIdent(updctx.classIdentifier());
        FilterStreamSpecRaw streamSpec = new FilterStreamSpecRaw(new FilterSpecRaw(eventTypeName, Collections.<ExprNode>emptyList(), null), ViewSpec.EMPTY_VIEWSPEC_ARRAY, eventTypeName, StreamSpecOptions.DEFAULT);
        statementSpec.getStreamSpecs().add(streamSpec);
        String optionalStreamName = ASTUtil.getStreamNameUnescapedOptional(updctx.identOrTicked());
        List<OnTriggerSetAssignment> assignments = ASTExprHelper.getOnTriggerSetAssignments(updctx.onSetAssignmentList(), astExprNodeMap);
        ExprNode whereClause = updctx.WHERE() != null ? ASTExprHelper.exprCollectSubNodes(updctx.whereClause(), 0, astExprNodeMap).get(0) : null;
        statementSpec.setUpdateDesc(new UpdateDesc(optionalStreamName, assignments, whereClause));
    }

    public void exitFrequencyOperand(EsperEPL2GrammarParser.FrequencyOperandContext ctx) {
        ExprNumberSetFrequency exprNode = new ExprNumberSetFrequency();
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(exprNode, ctx, astExprNodeMap);
        ASTExprHelper.addOptionalNumber(exprNode, ctx.number());
        ASTExprHelper.addOptionalSimpleProperty(exprNode, ctx.i, variableService, statementSpec);
    }

    public void exitCreateDataflow(EsperEPL2GrammarParser.CreateDataflowContext ctx) {
        CreateDataFlowDesc graphDesc = ASTGraphHelper.walkCreateDataFlow(ctx, astGOPNodeMap, engineImportService);
        statementSpec.setCreateDataFlowDesc(graphDesc);
    }

    public void exitInsertIntoExpr(EsperEPL2GrammarParser.InsertIntoExprContext ctx) {
        SelectClauseStreamSelectorEnum selector = SelectClauseStreamSelectorEnum.ISTREAM_ONLY;
        if (ctx.r != null) {
            selector = SelectClauseStreamSelectorEnum.RSTREAM_ONLY;
        } else if (ctx.ir != null) {
            selector = SelectClauseStreamSelectorEnum.RSTREAM_ISTREAM_BOTH;
        }

        // type name
        String eventTypeName = ASTUtil.unescapeClassIdent(ctx.classIdentifier());
        InsertIntoDesc insertIntoDesc = new InsertIntoDesc(selector, eventTypeName);

        // optional columns
        if (ctx.columnList() != null) {
            for (int i = 0; i < ctx.columnList().getChildCount(); i++) {
                ParseTree node = ctx.columnList().getChild(i);
                if (ASTUtil.isTerminatedOfType(node, EsperEPL2GrammarLexer.IDENT)) {
                    insertIntoDesc.add(node.getText());
                }
            }
        }

        statementSpec.setInsertIntoDesc(insertIntoDesc);
    }

    public void exitCreateVariableExpr(EsperEPL2GrammarParser.CreateVariableExprContext ctx) {

        boolean constant = false;
        if (ctx.c != null) {
            String text = ctx.c.getText();
            if (text.equals("constant") || text.equals("const")) {
                constant = true;
            } else {
                throw new EPException("Expected 'constant' or 'const' keyword after create for create-variable syntax but encountered '" + text + "'");
            }
        }

        String variableType = ASTUtil.unescapeClassIdent(ctx.classIdentifier());
        String variableName = ctx.n.getText();

        boolean array = ctx.arr != null;
        boolean arrayOfPrimitive = ASTCreateSchemaHelper.validateIsPrimitiveArray(ctx.p);

        ExprNode assignment = null;
        if (ctx.EQUALS() != null) {
            assignment = ASTExprHelper.exprCollectSubNodes(ctx.expression(), 0, astExprNodeMap).get(0);
        }

        CreateVariableDesc desc = new CreateVariableDesc(variableType, variableName, assignment, constant, array, arrayOfPrimitive);
        statementSpec.setCreateVariableDesc(desc);
    }

    public void exitOnStreamExpr(EsperEPL2GrammarParser.OnStreamExprContext ctx) {
        String streamAsName = ASTUtil.getStreamNameUnescapedOptional(ctx.identOrTicked());

        // get stream to use (pattern or filter)
        StreamSpecRaw streamSpec;
        if (ctx.eventFilterExpression() != null) {
            streamSpec = new FilterStreamSpecRaw(filterSpec, ViewSpec.EMPTY_VIEWSPEC_ARRAY, streamAsName, StreamSpecOptions.DEFAULT);
        } else if (ctx.patternInclusionExpression() != null) {
            if ((astPatternNodeMap.size() > 1) || ((astPatternNodeMap.isEmpty()))) {
                throw ASTWalkException.from("Unexpected AST tree contains zero or more then 1 child elements for root");
            }
            // Get expression node sub-tree from the AST nodes placed so far
            EvalFactoryNode evalNode = astPatternNodeMap.values().iterator().next();
            PatternLevelAnnotationFlags flags = getPatternFlags(ctx.patternInclusionExpression().annotationEnum());
            streamSpec = new PatternStreamSpecRaw(evalNode, ViewSpec.toArray(viewSpecs), streamAsName, StreamSpecOptions.DEFAULT, flags.isSuppressSameEventMatches(), flags.isDiscardPartialsOnMatch());
            astPatternNodeMap.clear();
        } else {
            throw new IllegalStateException("Invalid AST type node, cannot map to stream specification");
        }
        statementSpec.getStreamSpecs().add(streamSpec);
    }

    public void exitOnSelectInsertFromClause(EsperEPL2GrammarParser.OnSelectInsertFromClauseContext ctx) {
        if (onTriggerSplitPropertyEvals == null) {
            onTriggerSplitPropertyEvals = new HashMap<>();
        }
        String streamName = ASTUtil.getStreamNameUnescapedOptional(ctx.identOrTicked());
        onTriggerSplitPropertyEvals.put(statementSpec, new OnTriggerSplitStreamFromClause(propertyEvalSpec, streamName));
        propertyEvalSpec = null;
    }

    public void exitPropertyExpressionAtomic(EsperEPL2GrammarParser.PropertyExpressionAtomicContext ctx) {
        // initialize if not set
        if (propertyEvalSpec == null) {
            propertyEvalSpec = new PropertyEvalSpec();
        }

        // get select clause
        SelectClauseSpecRaw optionalSelectClause = new SelectClauseSpecRaw();
        if (propertySelectRaw != null) {
            optionalSelectClause.getSelectExprList().addAll(propertySelectRaw);
            propertySelectRaw = null;
        }

        // get the splitter expression
        ExprNode splitterExpression = ASTExprHelper.exprCollectSubNodes(ctx.expression(0), 0, astExprNodeMap).get(0);

        // get where-clause, if any
        ExprNode optionalWhereClause = ctx.where == null ? null : ASTExprHelper.exprCollectSubNodes(ctx.where, 0, astExprNodeMap).get(0);

        String optionalAsName = ctx.n == null ? null : ctx.n.getText();

        String splitterEventTypeName = ASTTypeExpressionAnnoHelper.expectMayTypeAnno(ctx.typeExpressionAnnotation(), tokenStream);
        PropertyEvalAtom atom = new PropertyEvalAtom(splitterExpression, splitterEventTypeName, optionalAsName, optionalSelectClause, optionalWhereClause);
        propertyEvalSpec.add(atom);
    }

    public void exitFafUpdate(EsperEPL2GrammarParser.FafUpdateContext ctx) {
        handleFAFNamedWindowStream(ctx.updateDetails().classIdentifier(), ctx.updateDetails().identOrTicked());
        List<OnTriggerSetAssignment> assignments = ASTExprHelper.getOnTriggerSetAssignments(ctx.updateDetails().onSetAssignmentList(), astExprNodeMap);
        ExprNode whereClause = ctx.updateDetails().whereClause() == null ? null : ASTExprHelper.exprCollectSubNodes(ctx.updateDetails().whereClause(), 0, astExprNodeMap).get(0);
        statementSpec.setFilterExprRootNode(whereClause);
        statementSpec.setFireAndForgetSpec(new FireAndForgetSpecUpdate(assignments));
    }

    public void exitBitWiseExpression(EsperEPL2GrammarParser.BitWiseExpressionContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        BitWiseOpEnum bitWiseOpEnum;
        int token = ASTUtil.getAssertTerminatedTokenType(ctx.getChild(1));
        switch (token) {
            case EsperEPL2GrammarLexer.BAND:
                bitWiseOpEnum = BitWiseOpEnum.BAND;
                break;
            case EsperEPL2GrammarLexer.BOR:
                bitWiseOpEnum = BitWiseOpEnum.BOR;
                break;
            case EsperEPL2GrammarLexer.BXOR:
                bitWiseOpEnum = BitWiseOpEnum.BXOR;
                break;
            default:
                throw ASTWalkException.from("Node type " + token + " not a recognized bit wise node type", tokenStream, ctx);
        }

        ExprBitWiseNode bwNode = new ExprBitWiseNode(bitWiseOpEnum);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(bwNode, ctx, astExprNodeMap);
    }

    public void exitEvalEqualsExpression(EsperEPL2GrammarParser.EvalEqualsExpressionContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        ExprNode exprNode;
        boolean isNot = ctx.ne != null || ctx.isnot != null || ctx.sqlne != null;
        if (ctx.a == null) {
            boolean isIs = ctx.is != null || ctx.isnot != null;
            exprNode = new ExprEqualsNodeImpl(isNot, isIs);
        } else {
            boolean isAll = ctx.a.getType() == EsperEPL2GrammarLexer.ALL;
            List<EsperEPL2GrammarParser.SubSelectGroupExpressionContext> subselect = ctx.subSelectGroupExpression();
            if (subselect != null && !subselect.isEmpty()) {
                StatementSpecRaw currentSpec = astStatementSpecMap.remove(ctx.subSelectGroupExpression().get(0).subQueryExpr());
                exprNode = new ExprSubselectAllSomeAnyNode(currentSpec, isNot, isAll, null);
            } else {
                exprNode = new ExprEqualsAllAnyNode(isNot, isAll);
            }
        }
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(exprNode, ctx, astExprNodeMap);
    }

    public void exitGopConfig(EsperEPL2GrammarParser.GopConfigContext ctx) {

        Object value;
        if (ctx.SELECT() == null) {
            if (ctx.expression() != null) {
                value = ASTExprHelper.exprCollectSubNodes(ctx, 0, astExprNodeMap).get(0);
            } else {
                if (ctx.jsonarray() != null) {
                    value = new ExprConstantNodeImpl(ASTJsonHelper.walkArray(tokenStream, ctx.jsonarray()));
                } else {
                    value = new ExprConstantNodeImpl(ASTJsonHelper.walkObject(tokenStream, ctx.jsonobject()));
                }
                ASTExprHelper.exprCollectSubNodes(ctx, 0, astExprNodeMap);
            }
        } else {
            StatementSpecRaw newSpec = new StatementSpecRaw(defaultStreamSelector);
            newSpec.getAnnotations().addAll(statementSpec.getAnnotations());

            StatementSpecRaw existingSpec = statementSpec;
            existingSpec.setCreateSchemaDesc(null);
            value = existingSpec;
            existingSpec.setAnnotations(Collections.<AnnotationDesc>emptyList());  // clearing property-level annotations

            statementSpec = newSpec;
        }
        astGOPNodeMap.put(ctx, value);
    }

    public void exitCreateSelectionListElement(EsperEPL2GrammarParser.CreateSelectionListElementContext ctx) {
        if (ctx.STAR() != null) {
            statementSpec.getSelectClauseSpec().add(new SelectClauseElementWildcard());
        } else {
            ExprNode expr = ASTExprHelper.exprCollectSubNodes(ctx, 0, astExprNodeMap).get(0);
            String asName = ctx.i != null ? ctx.i.getText() : null;
            statementSpec.getSelectClauseSpec().add(new SelectClauseExprRawSpec(expr, asName, false));
        }
    }

    public void exitFafDelete(EsperEPL2GrammarParser.FafDeleteContext ctx) {
        handleFAFNamedWindowStream(ctx.classIdentifier(), ctx.identOrTicked());
        statementSpec.setFireAndForgetSpec(new FireAndForgetSpecDelete());
    }

    public void exitConstant(EsperEPL2GrammarParser.ConstantContext ctx) {
        ExprConstantNode constantNode = new ExprConstantNodeImpl(ASTConstantHelper.parse(ctx.getChild(0)));
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(constantNode, ctx, astExprNodeMap);
    }

    public void exitMergeMatched(EsperEPL2GrammarParser.MergeMatchedContext ctx) {
        handleMergeMatchedUnmatched(ctx.expression(), true);
    }

    public void exitEvalAndExpression(EsperEPL2GrammarParser.EvalAndExpressionContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        ExprAndNode and = new ExprAndNodeImpl();
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(and, ctx, astExprNodeMap);
    }

    public void exitForExpr(EsperEPL2GrammarParser.ForExprContext ctx) {
        if (statementSpec.getForClauseSpec() == null) {
            statementSpec.setForClauseSpec(new ForClauseSpec());
        }
        String ident = ctx.i.getText();
        List<ExprNode> expressions = ASTExprHelper.exprCollectSubNodes(ctx, 0, astExprNodeMap);
        statementSpec.getForClauseSpec().getClauses().add(new ForClauseItemSpec(ident, expressions));
    }

    public void exitExpressionQualifyable(EsperEPL2GrammarParser.ExpressionQualifyableContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        if (ctx.s != null) {
            ExprNode node = ASTExprHelper.timePeriodGetExprJustSeconds(ctx.expression(), astExprNodeMap, configurationInformation, engineImportService.getTimeAbacus());
            astExprNodeMap.put(ctx, node);
        } else if (ctx.a != null || ctx.d != null) {
            boolean isDescending = ctx.d != null;
            ExprNode node = ASTExprHelper.exprCollectSubNodes(ctx.expression(), 0, astExprNodeMap).get(0);
            ExprOrderedExpr exprNode = new ExprOrderedExpr(isDescending);
            exprNode.addChildNode(node);
            astExprNodeMap.put(ctx, exprNode);
        }
    }

    public void exitPropertySelectionListElement(EsperEPL2GrammarParser.PropertySelectionListElementContext ctx) {
        SelectClauseElementRaw raw;
        if (ctx.s != null) {
            raw = new SelectClauseElementWildcard();
        } else if (ctx.propertyStreamSelector() != null) {
            raw = new SelectClauseStreamRawSpec(ctx.propertyStreamSelector().s.getText(),
                    ctx.propertyStreamSelector().i != null ? ctx.propertyStreamSelector().i.getText() : null);
        } else {
            ExprNode exprNode = ASTExprHelper.exprCollectSubNodes(ctx.expression(), 0, astExprNodeMap).get(0);
            String optionalName = ctx.keywordAllowedIdent() != null ? ctx.keywordAllowedIdent().getText() : null;
            raw = new SelectClauseExprRawSpec(exprNode, optionalName, false);
        }

        // Add as selection element
        if (propertySelectRaw == null) {
            propertySelectRaw = new ArrayList<>();
        }
        this.propertySelectRaw.add(raw);
    }

    public void exitExpressionDecl(EsperEPL2GrammarParser.ExpressionDeclContext ctx) {
        if (ctx.parent.getRuleIndex() == EsperEPL2GrammarParser.RULE_createExpressionExpr) {
            return;
        }

        Pair<ExpressionDeclItem, ExpressionScriptProvided> pair = ASTExpressionDeclHelper.walkExpressionDecl(ctx, scriptBodies, astExprNodeMap, tokenStream);
        if (pair.getFirst() != null) {
            expressionDeclarations.add(pair.getFirst());
        } else {
            scriptExpressions.add(pair.getSecond());
        }
    }

    public void exitSubstitutionCanChain(EsperEPL2GrammarParser.SubstitutionCanChainContext ctx) {
        if (ctx.chainedFunction() == null) {
            return;
        }
        ExprSubstitutionNode substitutionNode = (ExprSubstitutionNode) astExprNodeMap.remove(ctx.substitution());
        List<ExprChainedSpec> chainSpec = ASTLibFunctionHelper.getLibFuncChain(ctx.chainedFunction().libFunctionNoClass(), astExprNodeMap);
        ExprDotNode exprNode = new ExprDotNodeImpl(chainSpec, engineImportService.isDuckType(), engineImportService.isUdfCache());
        exprNode.addChildNode(substitutionNode);
        astExprNodeMap.put(ctx, exprNode);
    }

    public void exitSubstitution(EsperEPL2GrammarParser.SubstitutionContext ctx) {
        int currentSize = this.substitutionParamNodes.size();
        ExprSubstitutionNode substitutionNode;
        if (ctx.slashIdentifier() != null) {
            String name = ASTUtil.unescapeSlashIdentifier(ctx.slashIdentifier());
            substitutionNode = new ExprSubstitutionNode(name);
        } else {
            substitutionNode = new ExprSubstitutionNode(currentSize + 1);
        }
        ASTSubstitutionHelper.validateNewSubstitution(substitutionParamNodes, substitutionNode);
        substitutionParamNodes.add(substitutionNode);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(substitutionNode, ctx, astExprNodeMap);
    }

    public void exitWeekDayOperator(EsperEPL2GrammarParser.WeekDayOperatorContext ctx) {
        ExprNumberSetCronParam exprNode = new ExprNumberSetCronParam(CronOperatorEnum.WEEKDAY);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(exprNode, ctx, astExprNodeMap);
        ASTExprHelper.addOptionalNumber(exprNode, ctx.number());
        ASTExprHelper.addOptionalSimpleProperty(exprNode, ctx.i, variableService, statementSpec);
    }

    public void exitLastWeekdayOperand(EsperEPL2GrammarParser.LastWeekdayOperandContext ctx) {
        ExprNumberSetCronParam exprNode = new ExprNumberSetCronParam(CronOperatorEnum.LASTWEEKDAY);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(exprNode, ctx, astExprNodeMap);
    }

    public void exitGroupByListExpr(EsperEPL2GrammarParser.GroupByListExprContext ctx) {
        ASTGroupByHelper.walkGroupBy(ctx, astExprNodeMap, statementSpec.getGroupByExpressions());
        astExprNodeMap.clear();
    }

    public void exitStreamSelector(EsperEPL2GrammarParser.StreamSelectorContext ctx) {
        String streamName = ctx.s.getText();
        String optionalName = ctx.i != null ? ctx.i.getText() : null;
        statementSpec.getSelectClauseSpec().add(new SelectClauseStreamRawSpec(streamName, optionalName));
    }

    public void exitStreamExpression(EsperEPL2GrammarParser.StreamExpressionContext ctx) {
        // Determine the optional stream name
        String streamName = ASTUtil.getStreamNameUnescapedOptional(ctx.identOrTicked());

        boolean isUnidirectional = ctx.UNIDIRECTIONAL() != null;
        boolean isRetainUnion = ctx.RETAINUNION() != null;
        boolean isRetainIntersection = ctx.RETAININTERSECTION() != null;

        // Convert to a stream specification instance
        StreamSpecRaw streamSpec;
        StreamSpecOptions options = new StreamSpecOptions(isUnidirectional, isRetainUnion, isRetainIntersection);

        // If the first subnode is a filter node, we have a filter stream specification
        if (ASTUtil.getRuleIndexIfProvided(ctx.getChild(0)) == EsperEPL2GrammarParser.RULE_eventFilterExpression) {
            streamSpec = new FilterStreamSpecRaw(filterSpec, ViewSpec.toArray(viewSpecs), streamName, options);
        } else if (ASTUtil.getRuleIndexIfProvided(ctx.getChild(0)) == EsperEPL2GrammarParser.RULE_patternInclusionExpression) {
            if ((astPatternNodeMap.size() > 1) || ((astPatternNodeMap.isEmpty()))) {
                throw ASTWalkException.from("Unexpected AST tree contains zero or more then 1 child elements for root");
            }
            EsperEPL2GrammarParser.PatternInclusionExpressionContext pctx = (EsperEPL2GrammarParser.PatternInclusionExpressionContext) ctx.getChild(0);

            // Get expression node sub-tree from the AST nodes placed so far
            EvalFactoryNode evalNode = astPatternNodeMap.values().iterator().next();
            PatternLevelAnnotationFlags flags = getPatternFlags(pctx.annotationEnum());
            streamSpec = new PatternStreamSpecRaw(evalNode, ViewSpec.toArray(viewSpecs), streamName, options, flags.isSuppressSameEventMatches(), flags.isDiscardPartialsOnMatch());
            astPatternNodeMap.clear();
        } else if (ctx.databaseJoinExpression() != null) {
            EsperEPL2GrammarParser.DatabaseJoinExpressionContext dbctx = ctx.databaseJoinExpression();
            String dbName = dbctx.i.getText();
            String sqlWithParams = StringValue.parseString(dbctx.s.getText());

            // determine if there is variables used
            List<PlaceholderParser.Fragment> sqlFragments;
            try {
                sqlFragments = PlaceholderParser.parsePlaceholder(sqlWithParams);
                for (PlaceholderParser.Fragment fragment : sqlFragments) {
                    if (!(fragment instanceof PlaceholderParser.ParameterFragment)) {
                        continue;
                    }

                    // Parse expression, store for substitution parameters
                    String expression = fragment.getValue();
                    if (expression.toUpperCase(Locale.ENGLISH).equals(DatabasePollingViewableFactory.SAMPLE_WHERECLAUSE_PLACEHOLDER)) {
                        continue;
                    }

                    if (expression.trim().length() == 0) {
                        throw ASTWalkException.from("Missing expression within ${...} in SQL statement");
                    }
                    String toCompile = "select * from java.lang.Object where " + expression;
                    StatementSpecRaw raw = EPAdministratorHelper.compileEPL(toCompile, expression, false, null, SelectClauseStreamSelectorEnum.ISTREAM_ONLY,
                            engineImportService, variableService, engineURI, configurationInformation, patternNodeFactory, contextManagementService, exprDeclaredService, tableService);

                    if ((raw.getSubstitutionParameters() != null) && (raw.getSubstitutionParameters().size() > 0)) {
                        throw ASTWalkException.from("EPL substitution parameters are not allowed in SQL ${...} expressions, consider using a variable instead");
                    }

                    if (raw.isHasVariables()) {
                        statementSpec.setHasVariables(true);
                    }

                    // add expression
                    if (statementSpec.getSqlParameters() == null) {
                        statementSpec.setSqlParameters(new HashMap<Integer, List<ExprNode>>());
                    }
                    List<ExprNode> listExp = statementSpec.getSqlParameters().get(statementSpec.getStreamSpecs().size());
                    if (listExp == null) {
                        listExp = new ArrayList<>();
                        statementSpec.getSqlParameters().put(statementSpec.getStreamSpecs().size(), listExp);
                    }
                    listExp.add(raw.getFilterRootNode());
                }
            } catch (PlaceholderParseException ex) {
                log.warn("Failed to parse SQL text '" + sqlWithParams + "' :" + ex.getMessage());
                // Let the view construction handle the validation
            }

            String sampleSQL = null;
            if (dbctx.s2 != null) {
                sampleSQL = dbctx.s2.getText();
                sampleSQL = StringValue.parseString(sampleSQL.trim());
            }

            streamSpec = new DBStatementStreamSpec(streamName, ViewSpec.toArray(viewSpecs), dbName, sqlWithParams, sampleSQL);
        } else if (ctx.methodJoinExpression() != null) {
            EsperEPL2GrammarParser.MethodJoinExpressionContext mthctx = ctx.methodJoinExpression();
            String prefixIdent = mthctx.i.getText();
            String fullName = ASTUtil.unescapeClassIdent(mthctx.classIdentifier());

            int indexDot = fullName.lastIndexOf('.');
            String classNamePart;
            String methodNamePart;
            if (indexDot == -1) {
                classNamePart = null;
                methodNamePart = fullName;
            } else {
                classNamePart = fullName.substring(0, indexDot);
                methodNamePart = fullName.substring(indexDot + 1);
            }
            List<ExprNode> exprNodes = ASTExprHelper.exprCollectSubNodes(mthctx, 0, astExprNodeMap);

            if (variableService.getVariableMetaData(classNamePart) != null) {
                statementSpec.setHasVariables(true);
            }

            String eventTypeName = ASTTypeExpressionAnnoHelper.expectMayTypeAnno(ctx.methodJoinExpression().typeExpressionAnnotation(), tokenStream);

            streamSpec = new MethodStreamSpec(streamName, ViewSpec.toArray(viewSpecs), prefixIdent, classNamePart, methodNamePart, exprNodes, eventTypeName);
        } else {
            throw ASTWalkException.from("Unexpected AST child node to stream expression", tokenStream, ctx);
        }
        viewSpecs.clear();
        statementSpec.getStreamSpecs().add(streamSpec);
    }

    public void exitViewExpressionWNamespace(EsperEPL2GrammarParser.ViewExpressionWNamespaceContext ctx) {
        String objectNamespace = ctx.getChild(0).getText();
        String objectName = ctx.viewWParameters().getChild(0).getText();
        List<ExprNode> viewParameters = ASTExprHelper.exprCollectSubNodes(ctx.viewWParameters(), 1, astExprNodeMap);
        viewSpecs.add(new ViewSpec(objectNamespace, objectName, viewParameters));
    }

    public void exitViewExpressionOptNamespace(EsperEPL2GrammarParser.ViewExpressionOptNamespaceContext ctx) {
        String objectNamespace = null;
        String objectName = ctx.viewWParameters().getChild(0).getText();
        if (ctx.ns != null) {
            objectNamespace = ctx.ns.getText();
        }
        List<ExprNode> viewParameters = ASTExprHelper.exprCollectSubNodes(ctx.viewWParameters(), 1, astExprNodeMap);
        viewSpecs.add(new ViewSpec(objectNamespace, objectName, viewParameters));
    }

    public void exitPatternFilterExpression(EsperEPL2GrammarParser.PatternFilterExpressionContext ctx) {
        String optionalPatternTagName = null;
        if (ctx.i != null) {
            optionalPatternTagName = ctx.i.getText();
        }

        String eventName = ASTUtil.unescapeClassIdent(ctx.classIdentifier());

        EsperEPL2GrammarParser.PatternFilterAnnotationContext anno = ctx.patternFilterAnnotation();
        Integer consumption = null;
        if (anno != null) {
            String name = ctx.patternFilterAnnotation().i.getText();
            if (!name.toUpperCase(Locale.ENGLISH).equals("CONSUME")) {
                throw new EPException("Unexpected pattern filter @ annotation, expecting 'consume' but received '" + name + "'");
            }
            if (anno.number() != null) {
                Object val = ASTConstantHelper.parse(anno.number());
                consumption = ((Number) val).intValue();
            } else {
                consumption = 1;
            }
        }

        List<ExprNode> exprNodes = ASTExprHelper.exprCollectSubNodes(ctx, 0, astExprNodeMap);

        FilterSpecRaw rawFilterSpec = new FilterSpecRaw(eventName, exprNodes, propertyEvalSpec);
        propertyEvalSpec = null;
        EvalFactoryNode filterNode = patternNodeFactory.makeFilterNode(rawFilterSpec, optionalPatternTagName, consumption);
        ASTExprHelper.patternCollectAddSubnodesAddParentNode(filterNode, ctx, astPatternNodeMap);
    }

    public void exitOnSelectExpr(EsperEPL2GrammarParser.OnSelectExprContext ctx) {
        statementSpec.getSelectClauseSpec().setDistinct(ctx.DISTINCT() != null);
    }

    public void exitStartPatternExpressionRule(EsperEPL2GrammarParser.StartPatternExpressionRuleContext ctx) {
        if ((astPatternNodeMap.size() > 1) || ((astPatternNodeMap.isEmpty()))) {
            throw ASTWalkException.from("Unexpected AST tree contains zero or more then 1 child elements for root");
        }

        // Get expression node sub-tree from the AST nodes placed so far
        EvalFactoryNode evalNode = astPatternNodeMap.values().iterator().next();

        PatternStreamSpecRaw streamSpec = new PatternStreamSpecRaw(evalNode, ViewSpec.EMPTY_VIEWSPEC_ARRAY, null, StreamSpecOptions.DEFAULT, false, false);
        statementSpec.getStreamSpecs().add(streamSpec);
        statementSpec.setSubstitutionParameters(substitutionParamNodes);

        astPatternNodeMap.clear();
    }

    public void exitOutputLimit(EsperEPL2GrammarParser.OutputLimitContext ctx) {
        OutputLimitSpec spec = ASTOutputLimitHelper.buildOutputLimitSpec(tokenStream, ctx, astExprNodeMap);
        statementSpec.setOutputLimitSpec(spec);
        if (spec.getVariableName() != null) {
            statementSpec.setHasVariables(true);
            ASTExprHelper.addVariableReference(statementSpec, spec.getVariableName());
        }
    }

    public void exitNumericParameterList(EsperEPL2GrammarParser.NumericParameterListContext ctx) {
        ExprNumberSetList exprNode = new ExprNumberSetList();
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(exprNode, ctx, astExprNodeMap);
    }

    public void exitCreateSchemaExpr(EsperEPL2GrammarParser.CreateSchemaExprContext ctx) {
        CreateSchemaDesc createSchema = ASTCreateSchemaHelper.walkCreateSchema(ctx);
        if (ctx.parent.getRuleIndex() == EsperEPL2GrammarParser.RULE_eplExpression) {
            statementSpec.getStreamSpecs().add(new FilterStreamSpecRaw(new FilterSpecRaw(Object.class.getName(), Collections.<ExprNode>emptyList(), null), ViewSpec.EMPTY_VIEWSPEC_ARRAY, null, StreamSpecOptions.DEFAULT));
        }
        statementSpec.setCreateSchemaDesc(createSchema);
    }

    public void exitLastOperator(EsperEPL2GrammarParser.LastOperatorContext ctx) {
        ExprNumberSetCronParam exprNode = new ExprNumberSetCronParam(CronOperatorEnum.LASTDAY);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(exprNode, ctx, astExprNodeMap);
        ASTExprHelper.addOptionalNumber(exprNode, ctx.number());
        ASTExprHelper.addOptionalSimpleProperty(exprNode, ctx.i, variableService, statementSpec);
    }

    public void exitCreateIndexExpr(EsperEPL2GrammarParser.CreateIndexExprContext ctx) {
        CreateIndexDesc desc = ASTIndexHelper.walk(ctx, astExprNodeMap);
        statementSpec.setCreateIndexDesc(desc);
    }

    public void exitAnnotationEnum(EsperEPL2GrammarParser.AnnotationEnumContext ctx) {
        if (ctx.parent.getRuleIndex() != EsperEPL2GrammarParser.RULE_startEPLExpressionRule &&
                ctx.parent.getRuleIndex() != EsperEPL2GrammarParser.RULE_startPatternExpressionRule) {
            return;
        }

        statementSpec.getAnnotations().add(ASTAnnotationHelper.walk(ctx, this.engineImportService));
        astExprNodeMap.clear();
    }

    public void exitCreateContextExpr(EsperEPL2GrammarParser.CreateContextExprContext ctx) {
        CreateContextDesc contextDesc = ASTContextHelper.walkCreateContext(ctx, astExprNodeMap, astPatternNodeMap, propertyEvalSpec, filterSpec);
        filterSpec = null;
        propertyEvalSpec = null;
        statementSpec.setCreateContextDesc(contextDesc);
    }

    public void exitLastOperand(EsperEPL2GrammarParser.LastOperandContext ctx) {
        ExprNumberSetCronParam exprNode = new ExprNumberSetCronParam(CronOperatorEnum.LASTDAY);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(exprNode, ctx, astExprNodeMap);
    }

    public void exitCreateWindowExpr(EsperEPL2GrammarParser.CreateWindowExprContext ctx) {
        String windowName = ctx.i.getText();

        String eventName = "java.lang.Object";
        if (ctx.createWindowExprModelAfter() != null) {
            eventName = ASTUtil.unescapeClassIdent(ctx.createWindowExprModelAfter().classIdentifier());
        }

        boolean isRetainUnion = ctx.ru != null;
        boolean isRetainIntersection = ctx.ri != null;
        StreamSpecOptions streamSpecOptions = new StreamSpecOptions(false, isRetainUnion, isRetainIntersection);

        // handle table-create clause, i.e. (col1 type, col2 type)
        List<ColumnDesc> colums = ASTCreateSchemaHelper.getColTypeList(ctx.createColumnList());

        boolean isInsert = ctx.INSERT() != null;
        ExprNode insertWhereExpr = null;
        if (isInsert && ctx.expression() != null) {
            insertWhereExpr = ASTExprHelper.exprCollectSubNodes(ctx.expression(), 0, this.astExprNodeMap).get(0);
        }

        CreateWindowDesc desc = new CreateWindowDesc(windowName, viewSpecs, streamSpecOptions, isInsert, insertWhereExpr, colums, eventName);
        statementSpec.setCreateWindowDesc(desc);

        // this is good for indicating what is being selected from
        FilterSpecRaw rawFilterSpec = new FilterSpecRaw(eventName, new LinkedList<ExprNode>(), null);
        FilterStreamSpecRaw streamSpec = new FilterStreamSpecRaw(rawFilterSpec, ViewSpec.EMPTY_VIEWSPEC_ARRAY, null, streamSpecOptions);
        statementSpec.getStreamSpecs().add(streamSpec);
    }

    public void exitCreateExpressionExpr(EsperEPL2GrammarParser.CreateExpressionExprContext ctx) {
        Pair<ExpressionDeclItem, ExpressionScriptProvided> pair = ASTExpressionDeclHelper.walkExpressionDecl(ctx.expressionDecl(), scriptBodies, astExprNodeMap, tokenStream);
        statementSpec.setCreateExpressionDesc(new CreateExpressionDesc(pair));
    }

    public void exitRangeOperand(EsperEPL2GrammarParser.RangeOperandContext ctx) {
        ExprNumberSetRange exprNode = new ExprNumberSetRange();
        astExprNodeMap.put(ctx, exprNode);
        if (ctx.s1 != null) {
            ASTExprHelper.exprCollectAddSubNodes(exprNode, ctx.s1, astExprNodeMap);
        }
        ASTExprHelper.addOptionalNumber(exprNode, ctx.n1);
        ASTExprHelper.addOptionalSimpleProperty(exprNode, ctx.i1, variableService, statementSpec);
        if (ctx.s2 != null) {
            ASTExprHelper.exprCollectAddSubNodes(exprNode, ctx.s2, astExprNodeMap);
        }
        ASTExprHelper.addOptionalNumber(exprNode, ctx.n2);
        ASTExprHelper.addOptionalSimpleProperty(exprNode, ctx.i2, variableService, statementSpec);
    }

    public void exitRowSubSelectExpression(EsperEPL2GrammarParser.RowSubSelectExpressionContext ctx) {
        StatementSpecRaw statementSpec = astStatementSpecMap.remove(ctx.subQueryExpr());
        ExprSubselectRowNode subselectNode = new ExprSubselectRowNode(statementSpec);
        if (ctx.chainedFunction() != null) {
            handleChainedFunction(ctx, ctx.chainedFunction(), subselectNode);
        } else {
            ASTExprHelper.exprCollectAddSubNodesAddParentNode(subselectNode, ctx, astExprNodeMap);
        }
    }

    public void exitUnaryExpression(EsperEPL2GrammarParser.UnaryExpressionContext ctx) {
        if (ctx.inner != null && ctx.chainedFunction() != null) {
            handleChainedFunction(ctx, ctx.chainedFunction(), null);
        }
        if (ctx.NEWKW() != null && ctx.newAssign() != null) {
            List<String> columnNames = new ArrayList<>();
            List<ExprNode> expressions = new ArrayList<>();
            List<EsperEPL2GrammarParser.NewAssignContext> assigns = ctx.newAssign();
            for (EsperEPL2GrammarParser.NewAssignContext assign : assigns) {
                String property = ASTUtil.getPropertyName(assign.eventProperty(), 0);
                columnNames.add(property);
                ExprNode expr;
                if (assign.expression() != null) {
                    expr = ASTExprHelper.exprCollectSubNodes(assign.expression(), 0, astExprNodeMap).get(0);
                } else {
                    expr = new ExprIdentNodeImpl(property);
                }
                expressions.add(expr);
            }
            String[] columns = columnNames.toArray(new String[columnNames.size()]);
            ExprNewStructNode newNode = new ExprNewStructNode(columns);
            newNode.addChildNodes(expressions);
            astExprNodeMap.put(ctx, newNode);
        }
        if (ctx.NEWKW() != null && ctx.classIdentifier() != null) {
            String classIdent = ASTUtil.unescapeClassIdent(ctx.classIdentifier());
            ExprNode exprNode;
            ExprNode newNode = new ExprNewInstanceNode(classIdent);
            if (ctx.chainedFunction() != null) {
                List<ExprChainedSpec> chainSpec = ASTLibFunctionHelper.getLibFuncChain(ctx.chainedFunction().libFunctionNoClass(), astExprNodeMap);
                ExprDotNode dotNode = new ExprDotNodeImpl(chainSpec, engineImportService.isDuckType(), engineImportService.isUdfCache());
                dotNode.addChildNode(newNode);
                exprNode = dotNode;
            } else {
                exprNode = newNode;
            }
            ASTExprHelper.exprCollectAddSubNodes(newNode, ctx, astExprNodeMap);
            astExprNodeMap.put(ctx, exprNode);
        }
        if (ctx.b != null) {
            // handle "variable[xxx]"
            String tableName = ctx.b.getText();
            ExprNode exprNode;
            ExprTableAccessNode tableNode;
            if (ctx.chainedFunction() == null) {
                tableNode = new ExprTableAccessNodeTopLevel(tableName);
                exprNode = tableNode;
            } else {
                List<ExprChainedSpec> chainSpec = ASTLibFunctionHelper.getLibFuncChain(ctx.chainedFunction().libFunctionNoClass(), astExprNodeMap);
                Pair<ExprTableAccessNode, List<ExprChainedSpec>> pair = ASTTableExprHelper.getTableExprChainable(engineImportService, plugInAggregations, engineURI, tableName, chainSpec);
                tableNode = pair.getFirst();
                if (pair.getSecond().isEmpty()) {
                    exprNode = tableNode;
                } else {
                    exprNode = new ExprDotNodeImpl(pair.getSecond(), engineImportService.isDuckType(), engineImportService.isUdfCache());
                    exprNode.addChildNode(tableNode);
                }
            }
            ASTExprHelper.exprCollectAddSubNodesAddParentNode(tableNode, ctx, astExprNodeMap);
            astExprNodeMap.put(ctx, exprNode);
            ASTTableExprHelper.addTableExpressionReference(statementSpec, tableNode);
        }
    }

    public void enterOnSelectInsertExpr(EsperEPL2GrammarParser.OnSelectInsertExprContext ctx) {
        pushStatementContext();
    }

    public void exitSelectClause(EsperEPL2GrammarParser.SelectClauseContext ctx) {
        SelectClauseStreamSelectorEnum selector;
        if (ctx.s != null) {
            if (ctx.s.getType() == EsperEPL2GrammarLexer.RSTREAM) {
                selector = SelectClauseStreamSelectorEnum.RSTREAM_ONLY;
            } else if (ctx.s.getType() == EsperEPL2GrammarLexer.ISTREAM) {
                selector = SelectClauseStreamSelectorEnum.ISTREAM_ONLY;
            } else if (ctx.s.getType() == EsperEPL2GrammarLexer.IRSTREAM) {
                selector = SelectClauseStreamSelectorEnum.RSTREAM_ISTREAM_BOTH;
            } else {
                throw ASTWalkException.from("Encountered unrecognized token type " + ctx.s.getType(), tokenStream, ctx);
            }
            statementSpec.setSelectStreamDirEnum(selector);
        }
        statementSpec.getSelectClauseSpec().setDistinct(ctx.d != null);
    }

    public void exitConcatenationExpr(EsperEPL2GrammarParser.ConcatenationExprContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        ExprConcatNode concatNode = new ExprConcatNode();
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(concatNode, ctx, astExprNodeMap);
    }

    public void exitSubSelectFilterExpr(EsperEPL2GrammarParser.SubSelectFilterExprContext ctx) {
        String streamName = ASTUtil.getStreamNameUnescapedOptional(ctx.identOrTicked());
        boolean isRetainUnion = ctx.ru != null;
        boolean isRetainIntersection = ctx.ri != null;
        StreamSpecOptions options = new StreamSpecOptions(false, isRetainUnion, isRetainIntersection);
        StreamSpecRaw streamSpec = new FilterStreamSpecRaw(filterSpec, ViewSpec.toArray(viewSpecs), streamName, options);
        viewSpecs.clear();
        statementSpec.getStreamSpecs().add(streamSpec);
    }

    public void exitNegatedExpression(EsperEPL2GrammarParser.NegatedExpressionContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        ExprNotNode notNode = new ExprNotNode();
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(notNode, ctx, astExprNodeMap);
    }

    public void exitAdditiveExpression(EsperEPL2GrammarParser.AdditiveExpressionContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        ExprNode expr = ASTExprHelper.mathGetExpr(ctx, astExprNodeMap, configurationInformation);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(expr, ctx, astExprNodeMap);
    }

    public void exitMultiplyExpression(EsperEPL2GrammarParser.MultiplyExpressionContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        ExprNode expr = ASTExprHelper.mathGetExpr(ctx, astExprNodeMap, configurationInformation);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(expr, ctx, astExprNodeMap);
    }

    public void exitEventProperty(EsperEPL2GrammarParser.EventPropertyContext ctx) {
        if (EVENT_PROPERTY_WALK_EXCEPTIONS_PARENT.contains(ctx.getParent().getRuleIndex())) {
            return;
        }

        if (ctx.getChildCount() == 0) {
            throw new IllegalStateException("Empty event property expression encountered");
        }

        ExprNode exprNode;
        String propertyName;

        // The stream name may precede the event property name, but cannot be told apart from the property name:
        //      s0.p1 could be a nested property, or could be stream 's0' and property 'p1'

        // A single entry means this must be the property name.
        // And a non-simple property means that it cannot be a stream name.
        if (ctx.eventPropertyAtomic().size() == 1 || PropertyParser.isNestedPropertyWithNonSimpleLead(ctx)) {
            propertyName = ctx.getText();
            exprNode = new ExprIdentNodeImpl(propertyName);

            EsperEPL2GrammarParser.EventPropertyAtomicContext first = ctx.eventPropertyAtomic().get(0);

            // test table access expression
            if (first.lb != null) {
                String nameText = first.eventPropertyIdent().getText();
                if (tableService.getTableMetadata(nameText) != null) {
                    ExprTableAccessNode tableNode;
                    if (ctx.eventPropertyAtomic().size() == 1) {
                        tableNode = new ExprTableAccessNodeTopLevel(nameText);
                    } else if (ctx.eventPropertyAtomic().size() == 2) {
                        String column = ctx.eventPropertyAtomic().get(1).getText();
                        tableNode = new ExprTableAccessNodeSubprop(nameText, column);
                    } else {
                        throw ASTWalkException.from("Invalid table expression '" + tokenStream.getText(ctx));
                    }
                    exprNode = tableNode;
                    ASTTableExprHelper.addTableExpressionReference(statementSpec, tableNode);
                    ASTExprHelper.addOptionalNumber(tableNode, first.ni);
                }
            }

            // test script
            if (first.lp != null) {
                String ident = ASTUtil.escapeDot(first.eventPropertyIdent().getText());
                String key = StringValue.parseString(first.s.getText());
                List<ExprNode> params = Collections.<ExprNode>singletonList(new ExprConstantNodeImpl(key));
                ExprNodeScript scriptNode = ExprDeclaredHelper.getExistsScript(getDefaultDialect(), ident, params, scriptExpressions, exprDeclaredService);
                if (scriptNode != null) {
                    exprNode = scriptNode;
                }
            }

            ExprDeclaredNodeImpl found = ExprDeclaredHelper.getExistsDeclaredExpr(propertyName, Collections.<ExprNode>emptyList(), expressionDeclarations.getExpressions(), exprDeclaredService, contextDescriptor);
            if (found != null) {
                exprNode = found;
            }
        } else {
            // --> this is more then one child node, and the first child node is a simple property
            // we may have a stream name in the first simple property, or a nested property
            // i.e. 's0.p0' could mean that the event has a nested property to 's0' of name 'p0', or 's0' is the stream name
            String leadingIdentifier = ctx.getChild(0).getChild(0).getText();
            String streamOrNestedPropertyName = ASTUtil.escapeDot(leadingIdentifier);
            propertyName = ASTUtil.getPropertyName(ctx, 2);

            Pair<ExprTableAccessNode, ExprDotNode> tableNode = ASTTableExprHelper.checkTableNameGetExprForSubproperty(tableService, streamOrNestedPropertyName, propertyName);
            VariableMetaData variableMetaData = variableService.getVariableMetaData(leadingIdentifier);
            if (tableNode != null) {
                if (tableNode.getSecond() != null) {
                    exprNode = tableNode.getSecond();
                } else {
                    exprNode = tableNode.getFirst();
                }
                ASTTableExprHelper.addTableExpressionReference(statementSpec, tableNode.getFirst());
            } else if (variableMetaData != null) {
                exprNode = new ExprVariableNodeImpl(variableMetaData, propertyName);
                statementSpec.setHasVariables(true);
                String message = VariableServiceUtil.checkVariableContextName(statementSpec.getOptionalContextName(), variableMetaData);
                if (message != null) {
                    throw ASTWalkException.from(message);
                }
                ASTExprHelper.addVariableReference(statementSpec, variableMetaData.getVariableName());
            } else if (contextDescriptor != null && contextDescriptor.getContextPropertyRegistry().isContextPropertyPrefix(streamOrNestedPropertyName)) {
                exprNode = new ExprContextPropertyNodeImpl(propertyName);
            } else {
                exprNode = new ExprIdentNodeImpl(propertyName, streamOrNestedPropertyName);
            }
        }

        // handle variable
        VariableMetaData variableMetaData = variableService.getVariableMetaData(propertyName);
        if (variableMetaData != null) {
            exprNode = new ExprVariableNodeImpl(variableMetaData, null);
            statementSpec.setHasVariables(true);
            String message = VariableServiceUtil.checkVariableContextName(statementSpec.getOptionalContextName(), variableMetaData);
            if (message != null) {
                throw ASTWalkException.from(message);
            }
            ASTExprHelper.addVariableReference(statementSpec, variableMetaData.getVariableName());
        }

        // handle table
        ExprTableAccessNode table = ASTTableExprHelper.checkTableNameGetExprForProperty(tableService, propertyName);
        if (table != null) {
            exprNode = table;
            ASTTableExprHelper.addTableExpressionReference(statementSpec, table);
        }

        ASTExprHelper.exprCollectAddSubNodesAddParentNode(exprNode, ctx, astExprNodeMap);
    }

    public void exitOuterJoin(EsperEPL2GrammarParser.OuterJoinContext ctx) {
        OuterJoinType joinType;
        if (ctx.i != null) {
            joinType = OuterJoinType.INNER;
        } else if (ctx.tr != null) {
            joinType = OuterJoinType.RIGHT;
        } else if (ctx.tl != null) {
            joinType = OuterJoinType.LEFT;
        } else if (ctx.tf != null) {
            joinType = OuterJoinType.FULL;
        } else {
            joinType = OuterJoinType.INNER;
        }

        // always starts with ON-token, so as to not produce an empty node
        ExprIdentNode left = null;
        ExprIdentNode right = null;
        ExprIdentNode[] addLeftArr = null;
        ExprIdentNode[] addRightArr = null;

        // get subnodes representing the on-expression, if provided
        if (ctx.outerJoinIdent() != null) {
            List<EsperEPL2GrammarParser.OuterJoinIdentPairContext> pairs = ctx.outerJoinIdent().outerJoinIdentPair();
            List<EsperEPL2GrammarParser.EventPropertyContext> props = pairs.get(0).eventProperty();
            left = validateOuterJoinGetIdentNode(ASTExprHelper.exprCollectSubNodes(props.get(0), 0, astExprNodeMap).get(0));
            right = validateOuterJoinGetIdentNode(ASTExprHelper.exprCollectSubNodes(props.get(1), 0, astExprNodeMap).get(0));

            if (pairs.size() > 1) {
                ArrayList<ExprIdentNode> addLeft = new ArrayList<>(pairs.size() - 1);
                ArrayList<ExprIdentNode> addRight = new ArrayList<>(pairs.size() - 1);
                for (int i = 1; i < pairs.size(); i++) {
                    props = pairs.get(i).eventProperty();
                    ExprIdentNode moreLeft = validateOuterJoinGetIdentNode(ASTExprHelper.exprCollectSubNodes(props.get(0), 0, astExprNodeMap).get(0));
                    ExprIdentNode moreRight = validateOuterJoinGetIdentNode(ASTExprHelper.exprCollectSubNodes(props.get(1), 0, astExprNodeMap).get(0));
                    addLeft.add(moreLeft);
                    addRight.add(moreRight);
                }
                addLeftArr = addLeft.toArray(new ExprIdentNode[addLeft.size()]);
                addRightArr = addRight.toArray(new ExprIdentNode[addRight.size()]);
            }
        }

        OuterJoinDesc outerJoinDesc = new OuterJoinDesc(joinType, left, right, addLeftArr, addRightArr);
        statementSpec.getOuterJoinDescList().add(outerJoinDesc);
    }

    public void exitOnExpr(EsperEPL2GrammarParser.OnExprContext ctx) {
        if (ctx.onMergeExpr() != null) {
            String windowName = ctx.onMergeExpr().n.getText();
            String asName = ASTUtil.getStreamNameUnescapedOptional(ctx.onMergeExpr().identOrTicked());
            OnTriggerMergeDesc desc = new OnTriggerMergeDesc(windowName, asName, mergeInsertNoMatch, mergeMatcheds == null ? Collections.<OnTriggerMergeMatched>emptyList() : mergeMatcheds);
            statementSpec.setOnTriggerDesc(desc);
        } else if (ctx.onSetExpr() == null) {
            UniformPair<String> windowName = getOnExprWindowName(ctx);
            boolean deleteAndSelect = ctx.onSelectExpr() != null && ctx.onSelectExpr().d != null;
            if (windowName == null) {
                // on the statement spec, the deepest spec is the outermost
                List<OnTriggerSplitStream> splitStreams = new ArrayList<>();
                for (int i = 1; i <= statementSpecStack.size() - 1; i++) {
                    StatementSpecRaw raw = statementSpecStack.get(i);
                    OnTriggerSplitStreamFromClause fromClause = onTriggerSplitPropertyEvals == null ? null : onTriggerSplitPropertyEvals.get(raw);
                    splitStreams.add(new OnTriggerSplitStream(raw.getInsertIntoDesc(), raw.getSelectClauseSpec(), fromClause, raw.getFilterExprRootNode()));
                }
                OnTriggerSplitStreamFromClause fromClause = onTriggerSplitPropertyEvals == null ? null : onTriggerSplitPropertyEvals.get(statementSpec);
                splitStreams.add(new OnTriggerSplitStream(statementSpec.getInsertIntoDesc(), statementSpec.getSelectClauseSpec(), fromClause, statementSpec.getFilterExprRootNode()));
                if (!statementSpecStack.isEmpty()) {
                    statementSpec = statementSpecStack.get(0);
                }
                boolean isFirst = ctx.outputClauseInsert() == null || ctx.outputClauseInsert().ALL() == null;
                statementSpec.setOnTriggerDesc(new OnTriggerSplitStreamDesc(OnTriggerType.ON_SPLITSTREAM, isFirst, splitStreams));
                statementSpecStack.clear();
            } else if (ctx.onUpdateExpr() != null) {
                List<OnTriggerSetAssignment> assignments = ASTExprHelper.getOnTriggerSetAssignments(ctx.onUpdateExpr().onSetAssignmentList(), astExprNodeMap);
                statementSpec.setOnTriggerDesc(new OnTriggerWindowUpdateDesc(windowName.getFirst(), windowName.getSecond(), assignments));
                if (ctx.onUpdateExpr().whereClause() != null) {
                    statementSpec.setFilterExprRootNode(ASTExprHelper.exprCollectSubNodes(ctx.onUpdateExpr().whereClause(), 0, astExprNodeMap).get(0));
                }
            } else {
                statementSpec.setOnTriggerDesc(new OnTriggerWindowDesc(windowName.getFirst(), windowName.getSecond(), ctx.onDeleteExpr() != null ? OnTriggerType.ON_DELETE : OnTriggerType.ON_SELECT, deleteAndSelect));
            }
        } else {
            List<OnTriggerSetAssignment> assignments = ASTExprHelper.getOnTriggerSetAssignments(ctx.onSetExpr().onSetAssignmentList(), astExprNodeMap);
            statementSpec.setOnTriggerDesc(new OnTriggerSetDesc(assignments));
        }
    }

    public void exitMatchRecogPatternAlteration(EsperEPL2GrammarParser.MatchRecogPatternAlterationContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        RowRegexExprNodeAlteration alterNode = new RowRegexExprNodeAlteration();
        ASTExprHelper.regExCollectAddSubNodesAddParentNode(alterNode, ctx, astRowRegexNodeMap);
    }

    public void exitCaseExpression(EsperEPL2GrammarParser.CaseExpressionContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        if (astExprNodeMap.isEmpty()) {
            throw ASTWalkException.from("Unexpected AST tree contains zero child element for case node", tokenStream, ctx);
        }
        if (astExprNodeMap.size() == 1) {
            throw ASTWalkException.from("AST tree does not contain at least when node for case node", tokenStream, ctx);
        }

        ExprCaseNode caseNode = new ExprCaseNode(ctx.expression() != null);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(caseNode, ctx, astExprNodeMap);
    }

    public void exitRowLimit(EsperEPL2GrammarParser.RowLimitContext ctx) {
        RowLimitSpec spec = ASTOutputLimitHelper.buildRowLimitSpec(ctx);
        statementSpec.setRowLimitSpec(spec);

        if ((spec.getNumRowsVariable() != null) || (spec.getOptionalOffsetVariable() != null)) {
            statementSpec.setHasVariables(true);
            ASTExprHelper.addVariableReference(statementSpec, spec.getOptionalOffsetVariable());
        }
        astExprNodeMap.clear();
    }

    public void exitOrderByListElement(EsperEPL2GrammarParser.OrderByListElementContext ctx) {
        ExprNode exprNode = ASTExprHelper.exprCollectSubNodes(ctx, 0, astExprNodeMap).get(0);
        astExprNodeMap.clear();
        boolean descending = ctx.d != null;
        statementSpec.getOrderByList().add(new OrderByItem(exprNode, descending));
    }

    public void exitMergeUnmatched(EsperEPL2GrammarParser.MergeUnmatchedContext ctx) {
        handleMergeMatchedUnmatched(ctx.expression(), false);
    }

    public void exitExistsSubSelectExpression(EsperEPL2GrammarParser.ExistsSubSelectExpressionContext ctx) {
        StatementSpecRaw currentSpec = astStatementSpecMap.remove(ctx.subQueryExpr());
        ExprSubselectNode subselectNode = new ExprSubselectExistsNode(currentSpec);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(subselectNode, ctx, astExprNodeMap);
    }

    public void exitArrayExpression(EsperEPL2GrammarParser.ArrayExpressionContext ctx) {
        ExprArrayNode arrayNode = new ExprArrayNode();
        if (ctx.chainedFunction() != null) {
            ASTExprHelper.exprCollectAddSubNodesExpressionCtx(arrayNode, ctx.expression(), astExprNodeMap);
            handleChainedFunction(ctx, ctx.chainedFunction(), arrayNode);
        } else {
            ASTExprHelper.exprCollectAddSubNodesAddParentNode(arrayNode, ctx, astExprNodeMap);
        }
    }

    public void visitTerminal(TerminalNode terminalNode) {
        if (terminalNode.getSymbol().getType() == EsperEPL2GrammarLexer.STAR) {
            int ruleIndex = ASTUtil.getRuleIndexIfProvided(terminalNode.getParent());
            if (ruleIndex == EsperEPL2GrammarParser.RULE_selectionListElement) {
                statementSpec.getSelectClauseSpec().add(new SelectClauseElementWildcard());
            }
            if (ruleIndex == EsperEPL2GrammarParser.STAR || ruleIndex == EsperEPL2GrammarParser.RULE_expressionWithTime) {
                ExprWildcardImpl exprNode = new ExprWildcardImpl();
                ASTExprHelper.exprCollectAddSubNodesAddParentNode(exprNode, terminalNode, astExprNodeMap);
            }
        }
    }

    public void exitAndExpression(EsperEPL2GrammarParser.AndExpressionContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        EvalFactoryNode andNode = patternNodeFactory.makeAndNode();
        ASTExprHelper.patternCollectAddSubnodesAddParentNode(andNode, ctx, astPatternNodeMap);
    }

    public void exitFollowedByExpression(EsperEPL2GrammarParser.FollowedByExpressionContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        List<EsperEPL2GrammarParser.FollowedByRepeatContext> repeats = ctx.followedByRepeat();
        ExprNode[] maxExpressions = new ExprNode[ctx.getChildCount() - 1];
        for (int i = 0; i < repeats.size(); i++) {
            EsperEPL2GrammarParser.FollowedByRepeatContext repeat = repeats.get(i);
            if (repeat.expression() != null) {
                maxExpressions[i] = ASTExprHelper.exprCollectSubNodes(repeat.expression(), 0, astExprNodeMap).get(0);
            }
        }

        List<ExprNode> expressions = Collections.emptyList();
        if (!CollectionUtil.isAllNullArray(maxExpressions)) {
            expressions = Arrays.asList(maxExpressions); // can contain null elements as max/no-max can be mixed
        }

        EvalFactoryNode fbNode = patternNodeFactory.makeFollowedByNode(expressions, configurationInformation.getEngineDefaults().getPatterns().getMaxSubexpressions() != null);
        ASTExprHelper.patternCollectAddSubnodesAddParentNode(fbNode, ctx, astPatternNodeMap);
    }

    public void exitOrExpression(EsperEPL2GrammarParser.OrExpressionContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        EvalFactoryNode orNode = patternNodeFactory.makeOrNode();
        ASTExprHelper.patternCollectAddSubnodesAddParentNode(orNode, ctx, astPatternNodeMap);
    }

    public void exitQualifyExpression(EsperEPL2GrammarParser.QualifyExpressionContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        if (ctx.matchUntilRange() != null) {
            EvalFactoryNode matchUntil = makeMatchUntil(ctx.matchUntilRange(), false);
            ASTExprHelper.patternCollectAddSubnodesAddParentNode(matchUntil, ctx.guardPostFix(), astPatternNodeMap);
        }

        EvalFactoryNode theNode;
        if (ctx.e != null) {
            theNode = this.patternNodeFactory.makeEveryNode();
        } else if (ctx.n != null) {
            theNode = this.patternNodeFactory.makeNotNode();
        } else if (ctx.d != null) {
            List<ExprNode> exprNodes = ASTExprHelper.exprCollectSubNodes(ctx.distinctExpressionList(), 0, astExprNodeMap);
            theNode = this.patternNodeFactory.makeEveryDistinctNode(exprNodes);
        } else {
            throw ASTWalkException.from("Failed to recognize node");
        }
        ASTExprHelper.patternCollectAddSubnodesAddParentNode(theNode, ctx, astPatternNodeMap);
    }

    public void exitMatchUntilExpression(EsperEPL2GrammarParser.MatchUntilExpressionContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        EvalFactoryNode node;
        if (ctx.matchUntilRange() != null) {
            node = makeMatchUntil(ctx.matchUntilRange(), ctx.until != null);
        } else {
            node = this.patternNodeFactory.makeMatchUntilNode(null, null, null);
        }
        ASTExprHelper.patternCollectAddSubnodesAddParentNode(node, ctx, astPatternNodeMap);
    }

    private EvalFactoryNode makeMatchUntil(EsperEPL2GrammarParser.MatchUntilRangeContext range, boolean hasUntil) {

        boolean hasRange = true;
        ExprNode low = null;
        ExprNode high = null;
        ExprNode single = null;
        boolean allowZeroLowerBounds = false;

        if (range.low != null && range.c1 != null && range.high == null) { // [expr:]
            low = ASTExprHelper.exprCollectSubNodes(range.low, 0, astExprNodeMap).get(0);
        } else if (range.c2 != null && range.upper != null) { // [:expr]
            high = ASTExprHelper.exprCollectSubNodes(range.upper, 0, astExprNodeMap).get(0);
        } else if (range.low != null && range.c1 == null) { // [expr]
            single = ASTExprHelper.exprCollectSubNodes(range.low, 0, astExprNodeMap).get(0);
            hasRange = false;
        } else if (range.low != null) { // [expr:expr]
            low = ASTExprHelper.exprCollectSubNodes(range.low, 0, astExprNodeMap).get(0);
            high = ASTExprHelper.exprCollectSubNodes(range.high, 0, astExprNodeMap).get(0);
            allowZeroLowerBounds = true;
        }

        boolean tightlyBound;
        if (single != null) {
            ASTMatchUntilHelper.validate(single, single, allowZeroLowerBounds);
            tightlyBound = true;
        } else {
            tightlyBound = ASTMatchUntilHelper.validate(low, high, allowZeroLowerBounds);
        }
        if (hasRange && !tightlyBound && !hasUntil) {
            throw ASTWalkException.from("Variable bounds repeat operator requires an until-expression");
        }
        return this.patternNodeFactory.makeMatchUntilNode(low, high, single);
    }

    public void exitGuardPostFix(EsperEPL2GrammarParser.GuardPostFixContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        if (ctx.guardWhereExpression() == null && ctx.guardWhileExpression() == null) { // nested
            return;
        }
        String objectNamespace;
        String objectName;
        List<ExprNode> obsParameters;
        if (ctx.guardWhereExpression() != null) {
            objectNamespace = ctx.guardWhereExpression().getChild(0).getText();
            objectName = ctx.guardWhereExpression().getChild(2).getText();
            obsParameters = ASTExprHelper.exprCollectSubNodes(ctx.guardWhereExpression(), 3, astExprNodeMap);
        } else {
            objectNamespace = GuardEnum.WHILE_GUARD.getNamespace();
            objectName = GuardEnum.WHILE_GUARD.getName();
            obsParameters = ASTExprHelper.exprCollectSubNodes(ctx.guardWhileExpression(), 1, astExprNodeMap);
        }

        PatternGuardSpec guardSpec = new PatternGuardSpec(objectNamespace, objectName, obsParameters);
        EvalFactoryNode guardNode = patternNodeFactory.makeGuardNode(guardSpec);
        ASTExprHelper.patternCollectAddSubnodesAddParentNode(guardNode, ctx, astPatternNodeMap);
    }

    public void exitBuiltin_coalesce(EsperEPL2GrammarParser.Builtin_coalesceContext ctx) {
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(new ExprCoalesceNode(), ctx, astExprNodeMap);
    }

    public void exitBuiltin_typeof(EsperEPL2GrammarParser.Builtin_typeofContext ctx) {
        ExprTypeofNode typeofNode = new ExprTypeofNode();
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(typeofNode, ctx, astExprNodeMap);
    }

    public void exitBuiltin_avedev(EsperEPL2GrammarParser.Builtin_avedevContext ctx) {
        ExprAggregateNode aggregateNode = new ExprAvedevNode(ctx.DISTINCT() != null);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(aggregateNode, ctx, astExprNodeMap);
    }

    public void exitBuiltin_prevcount(EsperEPL2GrammarParser.Builtin_prevcountContext ctx) {
        ExprPreviousNode previousNode = new ExprPreviousNode(ExprPreviousNodePreviousType.PREVCOUNT);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(previousNode, ctx, astExprNodeMap);
    }

    public void exitBuiltin_stddev(EsperEPL2GrammarParser.Builtin_stddevContext ctx) {
        ExprAggregateNode aggregateNode = new ExprStddevNode(ctx.DISTINCT() != null);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(aggregateNode, ctx, astExprNodeMap);
    }

    public void exitBuiltin_sum(EsperEPL2GrammarParser.Builtin_sumContext ctx) {
        ExprAggregateNode aggregateNode = new ExprSumNode(ctx.DISTINCT() != null);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(aggregateNode, ctx, astExprNodeMap);
    }

    public void exitBuiltin_exists(EsperEPL2GrammarParser.Builtin_existsContext ctx) {
        ExprPropertyExistsNode existsNode = new ExprPropertyExistsNode();
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(existsNode, ctx, astExprNodeMap);
    }

    public void exitBuiltin_prior(EsperEPL2GrammarParser.Builtin_priorContext ctx) {
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(new ExprPriorNode(), ctx, astExprNodeMap);
    }

    public void exitBuiltin_instanceof(EsperEPL2GrammarParser.Builtin_instanceofContext ctx) {
        // get class identifiers
        List<String> classes = new ArrayList<>();
        List<EsperEPL2GrammarParser.ClassIdentifierContext> classCtxs = ctx.classIdentifier();
        for (EsperEPL2GrammarParser.ClassIdentifierContext classCtx : classCtxs) {
            classes.add(ASTUtil.unescapeClassIdent(classCtx));
        }

        String[] idents = classes.toArray(new String[classes.size()]);
        ExprInstanceofNode instanceofNode = new ExprInstanceofNode(idents);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(instanceofNode, ctx, astExprNodeMap);
    }

    public void exitBuiltin_currts(EsperEPL2GrammarParser.Builtin_currtsContext ctx) {
        ExprTimestampNode timeNode = new ExprTimestampNode();
        if (ctx.chainedFunction() != null) {
            handleChainedFunction(ctx, ctx.chainedFunction(), timeNode);
        } else {
            astExprNodeMap.put(ctx, timeNode);
        }
    }

    public void exitBuiltin_median(EsperEPL2GrammarParser.Builtin_medianContext ctx) {
        ExprAggregateNode aggregateNode = new ExprMedianNode(ctx.DISTINCT() != null);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(aggregateNode, ctx, astExprNodeMap);
    }

    public void exitBuiltin_firstlastwindow(EsperEPL2GrammarParser.Builtin_firstlastwindowContext ctx) {
        AggregationStateType stateType = AggregationStateType.fromString(ctx.firstLastWindowAggregation().q.getText());
        ExprNode expr = new ExprAggMultiFunctionLinearAccessNode(stateType);
        ASTExprHelper.exprCollectAddSubNodes(expr, ctx.firstLastWindowAggregation().expressionListWithNamed(), astExprNodeMap);
        if (ctx.firstLastWindowAggregation().chainedFunction() != null) {
            handleChainedFunction(ctx, ctx.firstLastWindowAggregation().chainedFunction(), expr);
        } else {
            astExprNodeMap.put(ctx, expr);
        }
    }

    public void exitBuiltin_avg(EsperEPL2GrammarParser.Builtin_avgContext ctx) {
        ExprAggregateNode aggregateNode = new ExprAvgNode(ctx.DISTINCT() != null);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(aggregateNode, ctx, astExprNodeMap);
    }

    public void exitBuiltin_cast(EsperEPL2GrammarParser.Builtin_castContext ctx) {
        String classIdent = ASTUtil.unescapeClassIdent(ctx.classIdentifier());
        ExprCastNode castNode = new ExprCastNode(classIdent);
        if (ctx.chainedFunction() != null) {
            ASTExprHelper.exprCollectAddSubNodes(castNode, ctx.expression(), astExprNodeMap);
            ASTExprHelper.exprCollectAddSingle(castNode, ctx.expressionNamedParameter(), astExprNodeMap);
            handleChainedFunction(ctx, ctx.chainedFunction(), castNode);
        } else {
            ASTExprHelper.exprCollectAddSubNodesAddParentNode(castNode, ctx, astExprNodeMap);
        }
    }

    public void exitBuiltin_cnt(EsperEPL2GrammarParser.Builtin_cntContext ctx) {
        ExprAggregateNode aggregateNode = new ExprCountNode(ctx.DISTINCT() != null);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(aggregateNode, ctx, astExprNodeMap);
    }

    public void exitBuiltin_prev(EsperEPL2GrammarParser.Builtin_prevContext ctx) {
        ExprPreviousNode previousNode = new ExprPreviousNode(ExprPreviousNodePreviousType.PREV);
        if (ctx.chainedFunction() != null) {
            ASTExprHelper.exprCollectAddSubNodesExpressionCtx(previousNode, ctx.expression(), astExprNodeMap);
            handleChainedFunction(ctx, ctx.chainedFunction(), previousNode);
        } else {
            ASTExprHelper.exprCollectAddSubNodesAddParentNode(previousNode, ctx, astExprNodeMap);
        }
    }

    public void exitBuiltin_istream(EsperEPL2GrammarParser.Builtin_istreamContext ctx) {
        ExprIStreamNode istreamNode = new ExprIStreamNode();
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(istreamNode, ctx, astExprNodeMap);
    }

    public void exitBuiltin_prevwindow(EsperEPL2GrammarParser.Builtin_prevwindowContext ctx) {
        ExprPreviousNode previousNode = new ExprPreviousNode(ExprPreviousNodePreviousType.PREVWINDOW);
        if (ctx.chainedFunction() != null) {
            ASTExprHelper.exprCollectAddSubNodes(previousNode, ctx.expression(), astExprNodeMap);
            handleChainedFunction(ctx, ctx.chainedFunction(), previousNode);
        } else {
            ASTExprHelper.exprCollectAddSubNodesAddParentNode(previousNode, ctx, astExprNodeMap);
        }
    }

    public void exitBuiltin_prevtail(EsperEPL2GrammarParser.Builtin_prevtailContext ctx) {
        ExprPreviousNode previousNode = new ExprPreviousNode(ExprPreviousNodePreviousType.PREVTAIL);
        if (ctx.chainedFunction() != null) {
            ASTExprHelper.exprCollectAddSubNodesExpressionCtx(previousNode, ctx.expression(), astExprNodeMap);
            handleChainedFunction(ctx, ctx.chainedFunction(), previousNode);
        } else {
            ASTExprHelper.exprCollectAddSubNodesAddParentNode(previousNode, ctx, astExprNodeMap);
        }
    }

    private PatternLevelAnnotationFlags getPatternFlags(List<EsperEPL2GrammarParser.AnnotationEnumContext> ctxList) {
        PatternLevelAnnotationFlags flags = new PatternLevelAnnotationFlags();
        if (ctxList != null) {
            for (EsperEPL2GrammarParser.AnnotationEnumContext ctx : ctxList) {
                AnnotationDesc desc = ASTAnnotationHelper.walk(ctx, engineImportService);
                PatternLevelAnnotationUtil.validateSetFlags(flags, desc.getName());
            }
        }
        return flags;
    }

    private UniformPair<String> getOnExprWindowName(EsperEPL2GrammarParser.OnExprContext ctx) {
        if (ctx.onDeleteExpr() != null) {
            return getOnExprWindowName(ctx.onDeleteExpr().onExprFrom());
        }
        if (ctx.onSelectExpr() != null && ctx.onSelectExpr().onExprFrom() != null) {
            return getOnExprWindowName(ctx.onSelectExpr().onExprFrom());
        }
        if (ctx.onUpdateExpr() != null) {
            String name = ASTUtil.getStreamNameUnescapedOptional(ctx.onUpdateExpr().identOrTicked());
            return new UniformPair<>(ctx.onUpdateExpr().n.getText(), name);
        }
        return null;
    }

    private UniformPair<String> getOnExprWindowName(EsperEPL2GrammarParser.OnExprFromContext ctx) {
        String windowName = ctx.n.getText();
        String windowStreamName = ASTUtil.getStreamNameUnescapedOptional(ctx.identOrTicked());
        return new UniformPair<>(windowName, windowStreamName);
    }

    private String getDefaultDialect() {
        return configurationInformation.getEngineDefaults().getScripts().getDefaultDialect();
    }

    private void handleMergeMatchedUnmatched(EsperEPL2GrammarParser.ExpressionContext expression, boolean b) {
        if (mergeMatcheds == null) {
            mergeMatcheds = new ArrayList<>();
        }
        ExprNode filterSpec = null;
        if (expression != null) {
            filterSpec = ASTExprHelper.exprCollectSubNodes(expression, 0, astExprNodeMap).get(0);
        }
        mergeMatcheds.add(new OnTriggerMergeMatched(b, filterSpec, mergeActions));
        mergeActions = null;
    }

    private void handleMergeInsert(EsperEPL2GrammarParser.MergeInsertContext mergeInsertContext) {
        ExprNode whereCond = null;
        if (mergeInsertContext.whereClause() != null) {
            whereCond = ASTExprHelper.exprCollectSubNodes(mergeInsertContext.whereClause(), 0, astExprNodeMap).get(0);
        }
        List<SelectClauseElementRaw> expressions = new ArrayList<>(statementSpec.getSelectClauseSpec().getSelectExprList());
        statementSpec.getSelectClauseSpec().getSelectExprList().clear();

        String optionalInsertName = mergeInsertContext.classIdentifier() != null ? ASTUtil.unescapeClassIdent(mergeInsertContext.classIdentifier()) : null;
        List<String> columsList = ASTUtil.getIdentList(mergeInsertContext.columnList());
        mergeActions.add(new OnTriggerMergeActionInsert(whereCond, optionalInsertName, columsList, expressions));
    }

    private void handleChainedFunction(ParserRuleContext parentCtx, EsperEPL2GrammarParser.ChainedFunctionContext chainedCtx, ExprNode childExpression) {
        List<ExprChainedSpec> chainSpec = ASTLibFunctionHelper.getLibFuncChain(chainedCtx.libFunctionNoClass(), astExprNodeMap);
        ExprDotNode dotNode = new ExprDotNodeImpl(chainSpec, configurationInformation.getEngineDefaults().getExpression().isDuckTyping(),
                configurationInformation.getEngineDefaults().getExpression().isUdfCache());
        if (childExpression != null) {
            dotNode.addChildNode(childExpression);
        }
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(dotNode, parentCtx, astExprNodeMap);
    }

    private void handleFAFNamedWindowStream(EsperEPL2GrammarParser.ClassIdentifierContext node, EsperEPL2GrammarParser.IdentOrTickedContext asClause) {
        String windowName = ASTUtil.unescapeClassIdent(node);
        String alias = ASTUtil.getStreamNameUnescapedOptional(asClause);
        statementSpec.getStreamSpecs().add(new FilterStreamSpecRaw(new FilterSpecRaw(windowName, Collections.<ExprNode>emptyList(), null), ViewSpec.toArray(viewSpecs), alias, StreamSpecOptions.DEFAULT));
    }

    public void exitFafInsert(EsperEPL2GrammarParser.FafInsertContext ctx) {
        List<EsperEPL2GrammarParser.ExpressionContext> valueExprs = ctx.expressionList().expression();
        for (EsperEPL2GrammarParser.ExpressionContext valueExpr : valueExprs) {
            ExprNode expr = ASTExprHelper.exprCollectSubNodes(valueExpr, 0, astExprNodeMap).get(0);
            statementSpec.getSelectClauseSpec().add(new SelectClauseExprRawSpec(expr, null, false));
        }
        statementSpec.setFireAndForgetSpec(new FireAndForgetSpecInsert(true));
    }

    protected void end() throws ASTWalkException {
        if (astExprNodeMap.size() > 1) {
            throw ASTWalkException.from("Unexpected AST tree contains left over child elements," +
                    " not all expression nodes have been removed from AST-to-expression nodes map");
        }
        if (astPatternNodeMap.size() > 1) {
            throw ASTWalkException.from("Unexpected AST tree contains left over child elements," +
                    " not all pattern nodes have been removed from AST-to-pattern nodes map");
        }

        // detect insert-into fire-and-forget query
        if (statementSpec.getInsertIntoDesc() != null && statementSpec.getStreamSpecs().isEmpty() && statementSpec.getFireAndForgetSpec() == null) {
            statementSpec.setFireAndForgetSpec(new FireAndForgetSpecInsert(false));
        }

        statementSpec.setSubstitutionParameters(substitutionParamNodes);
    }

    public void exitBuiltin_grouping(EsperEPL2GrammarParser.Builtin_groupingContext ctx) {
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(new ExprGroupingNode(), ctx, astExprNodeMap);
    }


    public void exitBuiltin_groupingid(EsperEPL2GrammarParser.Builtin_groupingidContext ctx) {
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(new ExprGroupingIdNode(), ctx, astExprNodeMap);
    }

    public void exitIntoTableExpr(EsperEPL2GrammarParser.IntoTableExprContext ctx) {
        String name = ctx.i.getText();
        statementSpec.setIntoTableSpec(new IntoTableSpec(name));
    }

    public void exitCreateTableExpr(EsperEPL2GrammarParser.CreateTableExprContext ctx) {
        String tableName = ctx.n.getText();

        // obtain item declarations
        List<CreateTableColumn> cols = ASTTableHelper.getColumns(ctx.createTableColumnList().createTableColumn(), astExprNodeMap, engineImportService);
        statementSpec.setCreateTableDesc(new CreateTableDesc(tableName, cols));
    }

    public void exitJsonobject(EsperEPL2GrammarParser.JsonobjectContext ctx) {
        ExprConstantNodeImpl node = new ExprConstantNodeImpl(ASTJsonHelper.walkObject(tokenStream, ctx));
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(node, ctx, astExprNodeMap);
    }

    public void exitPropertyStreamSelector(EsperEPL2GrammarParser.PropertyStreamSelectorContext ctx) {
        String streamWildcard = ctx.s.getText();
        ExprStreamUnderlyingNodeImpl node = new ExprStreamUnderlyingNodeImpl(streamWildcard, true);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(node, ctx, astExprNodeMap);
    }

    public void exitExpressionNamedParameter(EsperEPL2GrammarParser.ExpressionNamedParameterContext ctx) {
        ExprNamedParameterNodeImpl named = new ExprNamedParameterNodeImpl(ctx.IDENT().getText());
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(named, ctx, astExprNodeMap);
    }

    public void exitExpressionNamedParameterWithTime(EsperEPL2GrammarParser.ExpressionNamedParameterWithTimeContext ctx) {
        ExprNamedParameterNodeImpl named = new ExprNamedParameterNodeImpl(ctx.IDENT().getText());
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(named, ctx, astExprNodeMap);
    }

    private ExprIdentNode validateOuterJoinGetIdentNode(ExprNode exprNode) {
        if (exprNode instanceof ExprIdentNode) {
            return (ExprIdentNode) exprNode;
        }
        if (exprNode instanceof ExprTableAccessNodeSubprop) {
            ExprTableAccessNodeSubprop subprop = (ExprTableAccessNodeSubprop) exprNode;
            return new ExprIdentNodeImpl(subprop.getSubpropName(), subprop.getTableName());
        }
        throw ASTWalkException.from("Failed to validated 'on'-keyword expressions in outer join, expected identifiers only");
    }

    public void enterContextExpr(EsperEPL2GrammarParser.ContextExprContext ctx) {
    }

    public void enterExpressionList(EsperEPL2GrammarParser.ExpressionListContext ctx) {
    }

    public void exitExpressionList(EsperEPL2GrammarParser.ExpressionListContext ctx) {
    }

    public void enterSelectionList(EsperEPL2GrammarParser.SelectionListContext ctx) {
    }

    public void exitSelectionList(EsperEPL2GrammarParser.SelectionListContext ctx) {
    }

    public void enterEvalRelationalExpression(EsperEPL2GrammarParser.EvalRelationalExpressionContext ctx) {
    }

    public void enterPatternInclusionExpression(EsperEPL2GrammarParser.PatternInclusionExpressionContext ctx) {
    }

    public void exitPatternInclusionExpression(EsperEPL2GrammarParser.PatternInclusionExpressionContext ctx) {
    }

    public void enterLibFunction(EsperEPL2GrammarParser.LibFunctionContext ctx) {
    }

    public void enterSelectionListElement(EsperEPL2GrammarParser.SelectionListElementContext ctx) {
    }

    public void exitSelectionListElement(EsperEPL2GrammarParser.SelectionListElementContext ctx) {
    }

    public void enterGopOutTypeList(EsperEPL2GrammarParser.GopOutTypeListContext ctx) {
    }

    public void exitGopOutTypeList(EsperEPL2GrammarParser.GopOutTypeListContext ctx) {
    }

    public void enterGopOutTypeItem(EsperEPL2GrammarParser.GopOutTypeItemContext ctx) {
    }

    public void exitGopOutTypeItem(EsperEPL2GrammarParser.GopOutTypeItemContext ctx) {
    }

    public void enterMatchRecog(EsperEPL2GrammarParser.MatchRecogContext ctx) {
    }

    public void enterJsonmembers(EsperEPL2GrammarParser.JsonmembersContext ctx) {
    }

    public void exitJsonmembers(EsperEPL2GrammarParser.JsonmembersContext ctx) {
    }

    public void enterNumber(EsperEPL2GrammarParser.NumberContext ctx) {
    }

    public void exitNumber(EsperEPL2GrammarParser.NumberContext ctx) {
    }

    public void enterVariantList(EsperEPL2GrammarParser.VariantListContext ctx) {
    }

    public void exitVariantList(EsperEPL2GrammarParser.VariantListContext ctx) {
    }

    public void enterMatchRecogPartitionBy(EsperEPL2GrammarParser.MatchRecogPartitionByContext ctx) {
    }

    public void enterOutputLimitAfter(EsperEPL2GrammarParser.OutputLimitAfterContext ctx) {
    }

    public void exitOutputLimitAfter(EsperEPL2GrammarParser.OutputLimitAfterContext ctx) {
    }

    public void enterCreateColumnList(EsperEPL2GrammarParser.CreateColumnListContext ctx) {
    }

    public void exitCreateColumnList(EsperEPL2GrammarParser.CreateColumnListContext ctx) {
    }

    public void enterMergeMatchedItem(EsperEPL2GrammarParser.MergeMatchedItemContext ctx) {
    }

    public void enterMatchRecogMatchesSelection(EsperEPL2GrammarParser.MatchRecogMatchesSelectionContext ctx) {
    }

    public void exitMatchRecogMatchesSelection(EsperEPL2GrammarParser.MatchRecogMatchesSelectionContext ctx) {
    }

    public void enterClassIdentifier(EsperEPL2GrammarParser.ClassIdentifierContext ctx) {
    }

    public void exitClassIdentifier(EsperEPL2GrammarParser.ClassIdentifierContext ctx) {
    }

    public void enterDatabaseJoinExpression(EsperEPL2GrammarParser.DatabaseJoinExpressionContext ctx) {
    }

    public void exitDatabaseJoinExpression(EsperEPL2GrammarParser.DatabaseJoinExpressionContext ctx) {
    }

    public void enterMatchRecogDefineItem(EsperEPL2GrammarParser.MatchRecogDefineItemContext ctx) {
    }

    public void enterLibFunctionArgs(EsperEPL2GrammarParser.LibFunctionArgsContext ctx) {
    }

    public void exitLibFunctionArgs(EsperEPL2GrammarParser.LibFunctionArgsContext ctx) {
    }

    public void enterMergeUnmatchedItem(EsperEPL2GrammarParser.MergeUnmatchedItemContext ctx) {
    }

    public void enterHavingClause(EsperEPL2GrammarParser.HavingClauseContext ctx) {
    }

    public void enterMatchRecogMeasureItem(EsperEPL2GrammarParser.MatchRecogMeasureItemContext ctx) {
    }

    public void enterMatchRecogMatchesInterval(EsperEPL2GrammarParser.MatchRecogMatchesIntervalContext ctx) {
    }

    public void exitMatchRecogMatchesInterval(EsperEPL2GrammarParser.MatchRecogMatchesIntervalContext ctx) {
    }

    public void enterObserverExpression(EsperEPL2GrammarParser.ObserverExpressionContext ctx) {
    }

    public void enterMatchRecogPatternNested(EsperEPL2GrammarParser.MatchRecogPatternNestedContext ctx) {
    }

    public void enterCreateContextFilter(EsperEPL2GrammarParser.CreateContextFilterContext ctx) {
    }

    public void exitCreateContextFilter(EsperEPL2GrammarParser.CreateContextFilterContext ctx) {
    }

    public void enterEvalOrExpression(EsperEPL2GrammarParser.EvalOrExpressionContext ctx) {
    }

    public void enterExpressionDef(EsperEPL2GrammarParser.ExpressionDefContext ctx) {
    }

    public void exitExpressionDef(EsperEPL2GrammarParser.ExpressionDefContext ctx) {
    }

    public void enterOutputLimitAndTerm(EsperEPL2GrammarParser.OutputLimitAndTermContext ctx) {
    }

    public void exitOutputLimitAndTerm(EsperEPL2GrammarParser.OutputLimitAndTermContext ctx) {
    }

    public void enterNumericListParameter(EsperEPL2GrammarParser.NumericListParameterContext ctx) {
    }

    public void exitNumericListParameter(EsperEPL2GrammarParser.NumericListParameterContext ctx) {
    }

    public void enterTimePeriod(EsperEPL2GrammarParser.TimePeriodContext ctx) {
    }

    public void enterEventPropertyAtomic(EsperEPL2GrammarParser.EventPropertyAtomicContext ctx) {
    }

    public void exitEventPropertyAtomic(EsperEPL2GrammarParser.EventPropertyAtomicContext ctx) {
    }

    public void enterSubSelectGroupExpression(EsperEPL2GrammarParser.SubSelectGroupExpressionContext ctx) {
    }

    public void exitSubSelectGroupExpression(EsperEPL2GrammarParser.SubSelectGroupExpressionContext ctx) {
    }

    public void enterOuterJoinList(EsperEPL2GrammarParser.OuterJoinListContext ctx) {
    }

    public void exitOuterJoinList(EsperEPL2GrammarParser.OuterJoinListContext ctx) {
    }

    public void enterSelectionListElementExpr(EsperEPL2GrammarParser.SelectionListElementExprContext ctx) {
    }

    public void enterEventFilterExpression(EsperEPL2GrammarParser.EventFilterExpressionContext ctx) {
    }

    public void enterGopParamsItemList(EsperEPL2GrammarParser.GopParamsItemListContext ctx) {
    }

    public void exitGopParamsItemList(EsperEPL2GrammarParser.GopParamsItemListContext ctx) {
    }

    public void enterMatchRecogPatternConcat(EsperEPL2GrammarParser.MatchRecogPatternConcatContext ctx) {
    }

    public void enterNumberconstant(EsperEPL2GrammarParser.NumberconstantContext ctx) {
    }

    public void enterOnSetAssignment(EsperEPL2GrammarParser.OnSetAssignmentContext ctx) {
    }

    public void exitOnSetAssignment(EsperEPL2GrammarParser.OnSetAssignmentContext ctx) {
    }

    public void enterContextContextNested(EsperEPL2GrammarParser.ContextContextNestedContext ctx) {
    }

    public void exitContextContextNested(EsperEPL2GrammarParser.ContextContextNestedContext ctx) {
    }

    public void enterExpressionWithTime(EsperEPL2GrammarParser.ExpressionWithTimeContext ctx) {
    }

    public void exitExpressionWithTime(EsperEPL2GrammarParser.ExpressionWithTimeContext ctx) {
    }

    public void enterMatchRecogPattern(EsperEPL2GrammarParser.MatchRecogPatternContext ctx) {
    }

    public void enterMergeInsert(EsperEPL2GrammarParser.MergeInsertContext ctx) {
    }

    public void exitMergeInsert(EsperEPL2GrammarParser.MergeInsertContext ctx) {
    }

    public void enterOrderByListExpr(EsperEPL2GrammarParser.OrderByListExprContext ctx) {
    }

    public void exitOrderByListExpr(EsperEPL2GrammarParser.OrderByListExprContext ctx) {
    }

    public void enterElementValuePairsEnum(EsperEPL2GrammarParser.ElementValuePairsEnumContext ctx) {
    }

    public void exitElementValuePairsEnum(EsperEPL2GrammarParser.ElementValuePairsEnumContext ctx) {
    }

    public void enterDistinctExpressionAtom(EsperEPL2GrammarParser.DistinctExpressionAtomContext ctx) {
    }

    public void exitDistinctExpressionAtom(EsperEPL2GrammarParser.DistinctExpressionAtomContext ctx) {
    }

    public void enterExpression(EsperEPL2GrammarParser.ExpressionContext ctx) {
    }

    public void exitExpression(EsperEPL2GrammarParser.ExpressionContext ctx) {
    }

    public void enterWhereClause(EsperEPL2GrammarParser.WhereClauseContext ctx) {
    }

    public void enterCreateColumnListElement(EsperEPL2GrammarParser.CreateColumnListElementContext ctx) {
    }

    public void exitCreateColumnListElement(EsperEPL2GrammarParser.CreateColumnListElementContext ctx) {
    }

    public void enterGopList(EsperEPL2GrammarParser.GopListContext ctx) {
    }

    public void exitGopList(EsperEPL2GrammarParser.GopListContext ctx) {
    }

    public void enterPatternFilterAnnotation(EsperEPL2GrammarParser.PatternFilterAnnotationContext ctx) {
    }

    public void exitPatternFilterAnnotation(EsperEPL2GrammarParser.PatternFilterAnnotationContext ctx) {
    }

    public void enterElementValueArrayEnum(EsperEPL2GrammarParser.ElementValueArrayEnumContext ctx) {
    }

    public void exitElementValueArrayEnum(EsperEPL2GrammarParser.ElementValueArrayEnumContext ctx) {
    }

    public void enterHourPart(EsperEPL2GrammarParser.HourPartContext ctx) {
    }

    public void exitHourPart(EsperEPL2GrammarParser.HourPartContext ctx) {
    }

    public void enterOnDeleteExpr(EsperEPL2GrammarParser.OnDeleteExprContext ctx) {
    }

    public void exitOnDeleteExpr(EsperEPL2GrammarParser.OnDeleteExprContext ctx) {
    }

    public void enterMatchRecogPatternAtom(EsperEPL2GrammarParser.MatchRecogPatternAtomContext ctx) {
    }

    public void enterGopOutTypeParam(EsperEPL2GrammarParser.GopOutTypeParamContext ctx) {
    }

    public void exitGopOutTypeParam(EsperEPL2GrammarParser.GopOutTypeParamContext ctx) {
    }

    public void enterMergeItem(EsperEPL2GrammarParser.MergeItemContext ctx) {
    }

    public void exitMergeItem(EsperEPL2GrammarParser.MergeItemContext ctx) {
    }

    public void enterYearPart(EsperEPL2GrammarParser.YearPartContext ctx) {
    }

    public void exitYearPart(EsperEPL2GrammarParser.YearPartContext ctx) {
    }

    public void enterEventPropertyOrLibFunction(EsperEPL2GrammarParser.EventPropertyOrLibFunctionContext ctx) {
    }

    public void exitEventPropertyOrLibFunction(EsperEPL2GrammarParser.EventPropertyOrLibFunctionContext ctx) {
    }

    public void enterCreateDataflow(EsperEPL2GrammarParser.CreateDataflowContext ctx) {
    }

    public void enterUpdateExpr(EsperEPL2GrammarParser.UpdateExprContext ctx) {
    }

    public void enterFrequencyOperand(EsperEPL2GrammarParser.FrequencyOperandContext ctx) {
    }

    public void enterOnSetAssignmentList(EsperEPL2GrammarParser.OnSetAssignmentListContext ctx) {
    }

    public void exitOnSetAssignmentList(EsperEPL2GrammarParser.OnSetAssignmentListContext ctx) {
    }

    public void enterPropertyStreamSelector(EsperEPL2GrammarParser.PropertyStreamSelectorContext ctx) {
    }

    public void enterInsertIntoExpr(EsperEPL2GrammarParser.InsertIntoExprContext ctx) {
    }

    public void enterCreateVariableExpr(EsperEPL2GrammarParser.CreateVariableExprContext ctx) {
    }

    public void enterGopParamsItem(EsperEPL2GrammarParser.GopParamsItemContext ctx) {
    }

    public void exitGopParamsItem(EsperEPL2GrammarParser.GopParamsItemContext ctx) {
    }

    public void enterOnStreamExpr(EsperEPL2GrammarParser.OnStreamExprContext ctx) {
    }

    public void enterPropertyExpressionAtomic(EsperEPL2GrammarParser.PropertyExpressionAtomicContext ctx) {
    }

    public void enterGopDetail(EsperEPL2GrammarParser.GopDetailContext ctx) {
    }

    public void exitGopDetail(EsperEPL2GrammarParser.GopDetailContext ctx) {
    }

    public void enterGop(EsperEPL2GrammarParser.GopContext ctx) {
    }

    public void exitGop(EsperEPL2GrammarParser.GopContext ctx) {
    }

    public void enterOutputClauseInsert(EsperEPL2GrammarParser.OutputClauseInsertContext ctx) {
    }

    public void exitOutputClauseInsert(EsperEPL2GrammarParser.OutputClauseInsertContext ctx) {
    }

    public void enterEplExpression(EsperEPL2GrammarParser.EplExpressionContext ctx) {
    }

    public void exitEplExpression(EsperEPL2GrammarParser.EplExpressionContext ctx) {
    }

    public void enterOnMergeExpr(EsperEPL2GrammarParser.OnMergeExprContext ctx) {
    }

    public void exitOnMergeExpr(EsperEPL2GrammarParser.OnMergeExprContext ctx) {
    }

    public void enterFafUpdate(EsperEPL2GrammarParser.FafUpdateContext ctx) {
    }

    public void enterCreateSelectionList(EsperEPL2GrammarParser.CreateSelectionListContext ctx) {
    }

    public void exitCreateSelectionList(EsperEPL2GrammarParser.CreateSelectionListContext ctx) {
    }

    public void enterOnSetExpr(EsperEPL2GrammarParser.OnSetExprContext ctx) {
    }

    public void exitOnSetExpr(EsperEPL2GrammarParser.OnSetExprContext ctx) {
    }

    public void enterBitWiseExpression(EsperEPL2GrammarParser.BitWiseExpressionContext ctx) {
    }

    public void enterChainedFunction(EsperEPL2GrammarParser.ChainedFunctionContext ctx) {
    }

    public void exitChainedFunction(EsperEPL2GrammarParser.ChainedFunctionContext ctx) {
    }

    public void enterMatchRecogPatternUnary(EsperEPL2GrammarParser.MatchRecogPatternUnaryContext ctx) {
    }

    public void exitMatchRecogPatternUnary(EsperEPL2GrammarParser.MatchRecogPatternUnaryContext ctx) {
    }

    public void enterBetweenList(EsperEPL2GrammarParser.BetweenListContext ctx) {
    }

    public void exitBetweenList(EsperEPL2GrammarParser.BetweenListContext ctx) {
    }

    public void enterSecondPart(EsperEPL2GrammarParser.SecondPartContext ctx) {
    }

    public void exitSecondPart(EsperEPL2GrammarParser.SecondPartContext ctx) {
    }

    public void enterEvalEqualsExpression(EsperEPL2GrammarParser.EvalEqualsExpressionContext ctx) {
    }

    public void enterGopConfig(EsperEPL2GrammarParser.GopConfigContext ctx) {
    }

    public void enterMergeMatched(EsperEPL2GrammarParser.MergeMatchedContext ctx) {
    }

    public void enterCreateSelectionListElement(EsperEPL2GrammarParser.CreateSelectionListElementContext ctx) {
    }

    public void enterFafDelete(EsperEPL2GrammarParser.FafDeleteContext ctx) {
    }

    public void enterDayPart(EsperEPL2GrammarParser.DayPartContext ctx) {
    }

    public void exitDayPart(EsperEPL2GrammarParser.DayPartContext ctx) {
    }

    public void enterConstant(EsperEPL2GrammarParser.ConstantContext ctx) {
    }

    public void enterGopOut(EsperEPL2GrammarParser.GopOutContext ctx) {
    }

    public void exitGopOut(EsperEPL2GrammarParser.GopOutContext ctx) {
    }

    public void enterGuardWhereExpression(EsperEPL2GrammarParser.GuardWhereExpressionContext ctx) {
    }

    public void exitGuardWhereExpression(EsperEPL2GrammarParser.GuardWhereExpressionContext ctx) {
    }

    public void enterKeywordAllowedIdent(EsperEPL2GrammarParser.KeywordAllowedIdentContext ctx) {
    }

    public void exitKeywordAllowedIdent(EsperEPL2GrammarParser.KeywordAllowedIdentContext ctx) {
    }

    public void enterCreateContextGroupItem(EsperEPL2GrammarParser.CreateContextGroupItemContext ctx) {
    }

    public void exitCreateContextGroupItem(EsperEPL2GrammarParser.CreateContextGroupItemContext ctx) {
    }

    public void enterEvalAndExpression(EsperEPL2GrammarParser.EvalAndExpressionContext ctx) {
    }

    public void enterMultiplyExpression(EsperEPL2GrammarParser.MultiplyExpressionContext ctx) {
    }

    public void enterExpressionLambdaDecl(EsperEPL2GrammarParser.ExpressionLambdaDeclContext ctx) {
    }

    public void exitExpressionLambdaDecl(EsperEPL2GrammarParser.ExpressionLambdaDeclContext ctx) {
    }

    public void enterPropertyExpression(EsperEPL2GrammarParser.PropertyExpressionContext ctx) {
    }

    public void exitPropertyExpression(EsperEPL2GrammarParser.PropertyExpressionContext ctx) {
    }

    public void enterOuterJoinIdentPair(EsperEPL2GrammarParser.OuterJoinIdentPairContext ctx) {
    }

    public void exitOuterJoinIdentPair(EsperEPL2GrammarParser.OuterJoinIdentPairContext ctx) {
    }

    public void enterGopOutItem(EsperEPL2GrammarParser.GopOutItemContext ctx) {
    }

    public void exitGopOutItem(EsperEPL2GrammarParser.GopOutItemContext ctx) {
    }

    public void enterForExpr(EsperEPL2GrammarParser.ForExprContext ctx) {
    }

    public void enterPropertyExpressionSelect(EsperEPL2GrammarParser.PropertyExpressionSelectContext ctx) {
    }

    public void exitPropertyExpressionSelect(EsperEPL2GrammarParser.PropertyExpressionSelectContext ctx) {
    }

    public void enterExpressionQualifyable(EsperEPL2GrammarParser.ExpressionQualifyableContext ctx) {
    }

    public void enterExpressionDialect(EsperEPL2GrammarParser.ExpressionDialectContext ctx) {
    }

    public void exitExpressionDialect(EsperEPL2GrammarParser.ExpressionDialectContext ctx) {
    }

    public void enterStartEventPropertyRule(EsperEPL2GrammarParser.StartEventPropertyRuleContext ctx) {
    }

    public void exitStartEventPropertyRule(EsperEPL2GrammarParser.StartEventPropertyRuleContext ctx) {
    }

    public void enterPropertySelectionListElement(EsperEPL2GrammarParser.PropertySelectionListElementContext ctx) {
    }

    public void enterExpressionDecl(EsperEPL2GrammarParser.ExpressionDeclContext ctx) {
    }

    public void enterSubstitution(EsperEPL2GrammarParser.SubstitutionContext ctx) {
    }

    public void enterCrontabLimitParameterSet(EsperEPL2GrammarParser.CrontabLimitParameterSetContext ctx) {
    }

    public void exitCrontabLimitParameterSet(EsperEPL2GrammarParser.CrontabLimitParameterSetContext ctx) {
    }

    public void enterWeekDayOperator(EsperEPL2GrammarParser.WeekDayOperatorContext ctx) {
    }

    public void enterWhenClause(EsperEPL2GrammarParser.WhenClauseContext ctx) {
    }

    public void exitWhenClause(EsperEPL2GrammarParser.WhenClauseContext ctx) {
    }

    public void enterNewAssign(EsperEPL2GrammarParser.NewAssignContext ctx) {
    }

    public void exitNewAssign(EsperEPL2GrammarParser.NewAssignContext ctx) {
    }

    public void enterLastWeekdayOperand(EsperEPL2GrammarParser.LastWeekdayOperandContext ctx) {
    }

    public void enterGroupByListExpr(EsperEPL2GrammarParser.GroupByListExprContext ctx) {
    }

    public void enterStreamSelector(EsperEPL2GrammarParser.StreamSelectorContext ctx) {
    }

    public void enterStartJsonValueRule(EsperEPL2GrammarParser.StartJsonValueRuleContext ctx) {
    }

    public void exitStartJsonValueRule(EsperEPL2GrammarParser.StartJsonValueRuleContext ctx) {
    }

    public void enterStreamExpression(EsperEPL2GrammarParser.StreamExpressionContext ctx) {
    }

    public void enterOuterJoinIdent(EsperEPL2GrammarParser.OuterJoinIdentContext ctx) {
    }

    public void exitOuterJoinIdent(EsperEPL2GrammarParser.OuterJoinIdentContext ctx) {
    }

    public void enterCreateIndexColumnList(EsperEPL2GrammarParser.CreateIndexColumnListContext ctx) {
    }

    public void exitCreateIndexColumnList(EsperEPL2GrammarParser.CreateIndexColumnListContext ctx) {
    }

    public void enterColumnList(EsperEPL2GrammarParser.ColumnListContext ctx) {
    }

    public void exitColumnList(EsperEPL2GrammarParser.ColumnListContext ctx) {
    }

    public void enterPatternFilterExpression(EsperEPL2GrammarParser.PatternFilterExpressionContext ctx) {
    }

    public void enterJsonpair(EsperEPL2GrammarParser.JsonpairContext ctx) {
    }

    public void exitJsonpair(EsperEPL2GrammarParser.JsonpairContext ctx) {
    }

    public void enterOnSelectExpr(EsperEPL2GrammarParser.OnSelectExprContext ctx) {
    }

    public void enterElementValuePairEnum(EsperEPL2GrammarParser.ElementValuePairEnumContext ctx) {
    }

    public void exitElementValuePairEnum(EsperEPL2GrammarParser.ElementValuePairEnumContext ctx) {
    }

    public void enterStartPatternExpressionRule(EsperEPL2GrammarParser.StartPatternExpressionRuleContext ctx) {
    }

    public void enterSelectionListElementAnno(EsperEPL2GrammarParser.SelectionListElementAnnoContext ctx) {
    }

    public void exitSelectionListElementAnno(EsperEPL2GrammarParser.SelectionListElementAnnoContext ctx) {
    }

    public void enterOutputLimit(EsperEPL2GrammarParser.OutputLimitContext ctx) {
    }

    public void enterCreateContextDistinct(EsperEPL2GrammarParser.CreateContextDistinctContext ctx) {
    }

    public void exitCreateContextDistinct(EsperEPL2GrammarParser.CreateContextDistinctContext ctx) {
    }

    public void enterJsonelements(EsperEPL2GrammarParser.JsonelementsContext ctx) {
    }

    public void exitJsonelements(EsperEPL2GrammarParser.JsonelementsContext ctx) {
    }

    public void enterNumericParameterList(EsperEPL2GrammarParser.NumericParameterListContext ctx) {
    }

    public void enterLibFunctionWithClass(EsperEPL2GrammarParser.LibFunctionWithClassContext ctx) {
    }

    public void exitLibFunctionWithClass(EsperEPL2GrammarParser.LibFunctionWithClassContext ctx) {
    }

    public void enterStringconstant(EsperEPL2GrammarParser.StringconstantContext ctx) {
    }

    public void exitStringconstant(EsperEPL2GrammarParser.StringconstantContext ctx) {
    }

    public void enterCreateSchemaExpr(EsperEPL2GrammarParser.CreateSchemaExprContext ctx) {
    }

    public void enterElseClause(EsperEPL2GrammarParser.ElseClauseContext ctx) {
    }

    public void exitElseClause(EsperEPL2GrammarParser.ElseClauseContext ctx) {
    }

    public void enterGuardWhileExpression(EsperEPL2GrammarParser.GuardWhileExpressionContext ctx) {
    }

    public void exitGuardWhileExpression(EsperEPL2GrammarParser.GuardWhileExpressionContext ctx) {
    }

    public void enterCreateWindowExprModelAfter(EsperEPL2GrammarParser.CreateWindowExprModelAfterContext ctx) {
    }

    public void exitCreateWindowExprModelAfter(EsperEPL2GrammarParser.CreateWindowExprModelAfterContext ctx) {
    }

    public void enterMatchRecogMatchesAfterSkip(EsperEPL2GrammarParser.MatchRecogMatchesAfterSkipContext ctx) {
    }

    public void exitMatchRecogMatchesAfterSkip(EsperEPL2GrammarParser.MatchRecogMatchesAfterSkipContext ctx) {
    }

    public void enterCreateContextDetail(EsperEPL2GrammarParser.CreateContextDetailContext ctx) {
    }

    public void exitCreateContextDetail(EsperEPL2GrammarParser.CreateContextDetailContext ctx) {
    }

    public void enterMonthPart(EsperEPL2GrammarParser.MonthPartContext ctx) {
    }

    public void exitMonthPart(EsperEPL2GrammarParser.MonthPartContext ctx) {
    }

    public void enterPatternExpression(EsperEPL2GrammarParser.PatternExpressionContext ctx) {
    }

    public void exitPatternExpression(EsperEPL2GrammarParser.PatternExpressionContext ctx) {
    }

    public void enterLastOperator(EsperEPL2GrammarParser.LastOperatorContext ctx) {
    }

    public void enterCreateSchemaDef(EsperEPL2GrammarParser.CreateSchemaDefContext ctx) {
    }

    public void exitCreateSchemaDef(EsperEPL2GrammarParser.CreateSchemaDefContext ctx) {
    }

    public void enterEventPropertyIdent(EsperEPL2GrammarParser.EventPropertyIdentContext ctx) {
    }

    public void exitEventPropertyIdent(EsperEPL2GrammarParser.EventPropertyIdentContext ctx) {
    }

    public void enterCreateIndexExpr(EsperEPL2GrammarParser.CreateIndexExprContext ctx) {
    }

    public void enterAtomicExpression(EsperEPL2GrammarParser.AtomicExpressionContext ctx) {
    }

    public void exitAtomicExpression(EsperEPL2GrammarParser.AtomicExpressionContext ctx) {
    }

    public void enterJsonvalue(EsperEPL2GrammarParser.JsonvalueContext ctx) {
    }

    public void exitJsonvalue(EsperEPL2GrammarParser.JsonvalueContext ctx) {
    }

    public void enterLibFunctionNoClass(EsperEPL2GrammarParser.LibFunctionNoClassContext ctx) {
    }

    public void exitLibFunctionNoClass(EsperEPL2GrammarParser.LibFunctionNoClassContext ctx) {
    }

    public void enterElementValueEnum(EsperEPL2GrammarParser.ElementValueEnumContext ctx) {
    }

    public void exitElementValueEnum(EsperEPL2GrammarParser.ElementValueEnumContext ctx) {
    }

    public void enterOnUpdateExpr(EsperEPL2GrammarParser.OnUpdateExprContext ctx) {
    }

    public void exitOnUpdateExpr(EsperEPL2GrammarParser.OnUpdateExprContext ctx) {
    }

    public void enterAnnotationEnum(EsperEPL2GrammarParser.AnnotationEnumContext ctx) {
    }

    public void enterCreateContextExpr(EsperEPL2GrammarParser.CreateContextExprContext ctx) {
    }

    public void enterLastOperand(EsperEPL2GrammarParser.LastOperandContext ctx) {
    }

    public void enterExpressionWithTimeInclLast(EsperEPL2GrammarParser.ExpressionWithTimeInclLastContext ctx) {
    }

    public void exitExpressionWithTimeInclLast(EsperEPL2GrammarParser.ExpressionWithTimeInclLastContext ctx) {
    }

    public void enterCreateContextPartitionItem(EsperEPL2GrammarParser.CreateContextPartitionItemContext ctx) {
    }

    public void exitCreateContextPartitionItem(EsperEPL2GrammarParser.CreateContextPartitionItemContext ctx) {
    }

    public void enterCreateWindowExpr(EsperEPL2GrammarParser.CreateWindowExprContext ctx) {
    }

    public void enterVariantListElement(EsperEPL2GrammarParser.VariantListElementContext ctx) {
    }

    public void exitVariantListElement(EsperEPL2GrammarParser.VariantListElementContext ctx) {
    }

    public void enterCreateExpressionExpr(EsperEPL2GrammarParser.CreateExpressionExprContext ctx) {
    }

    public void enterRangeOperand(EsperEPL2GrammarParser.RangeOperandContext ctx) {
    }

    public void enterInSubSelectQuery(EsperEPL2GrammarParser.InSubSelectQueryContext ctx) {
    }

    public void exitInSubSelectQuery(EsperEPL2GrammarParser.InSubSelectQueryContext ctx) {
    }

    public void enterEscapableStr(EsperEPL2GrammarParser.EscapableStrContext ctx) {
    }

    public void exitEscapableStr(EsperEPL2GrammarParser.EscapableStrContext ctx) {
    }

    public void enterRowSubSelectExpression(EsperEPL2GrammarParser.RowSubSelectExpressionContext ctx) {
    }

    public void enterUnaryExpression(EsperEPL2GrammarParser.UnaryExpressionContext ctx) {
    }

    public void enterDistinctExpressionList(EsperEPL2GrammarParser.DistinctExpressionListContext ctx) {
    }

    public void exitDistinctExpressionList(EsperEPL2GrammarParser.DistinctExpressionListContext ctx) {
    }

    public void exitOnSelectInsertExpr(EsperEPL2GrammarParser.OnSelectInsertExprContext ctx) {
    }

    public void enterSelectClause(EsperEPL2GrammarParser.SelectClauseContext ctx) {
    }

    public void enterConcatenationExpr(EsperEPL2GrammarParser.ConcatenationExprContext ctx) {
    }

    public void enterStartEPLExpressionRule(EsperEPL2GrammarParser.StartEPLExpressionRuleContext ctx) {
    }

    public void exitStartEPLExpressionRule(EsperEPL2GrammarParser.StartEPLExpressionRuleContext ctx) {
    }

    public void enterSubSelectFilterExpr(EsperEPL2GrammarParser.SubSelectFilterExprContext ctx) {
    }

    public void enterCreateContextCoalesceItem(EsperEPL2GrammarParser.CreateContextCoalesceItemContext ctx) {
    }

    public void exitCreateContextCoalesceItem(EsperEPL2GrammarParser.CreateContextCoalesceItemContext ctx) {
    }

    public void enterMillisecondPart(EsperEPL2GrammarParser.MillisecondPartContext ctx) {
    }

    public void exitMillisecondPart(EsperEPL2GrammarParser.MillisecondPartContext ctx) {
    }

    public void enterMicrosecondPart(EsperEPL2GrammarParser.MicrosecondPartContext ctx) {
    }

    public void exitMicrosecondPart(EsperEPL2GrammarParser.MicrosecondPartContext ctx) {
    }

    public void enterOnExprFrom(EsperEPL2GrammarParser.OnExprFromContext ctx) {
    }

    public void exitOnExprFrom(EsperEPL2GrammarParser.OnExprFromContext ctx) {
    }

    public void enterNegatedExpression(EsperEPL2GrammarParser.NegatedExpressionContext ctx) {
    }

    public void enterSelectExpr(EsperEPL2GrammarParser.SelectExprContext ctx) {
    }

    public void enterMatchRecogMeasures(EsperEPL2GrammarParser.MatchRecogMeasuresContext ctx) {
    }

    public void exitMatchRecogMeasures(EsperEPL2GrammarParser.MatchRecogMeasuresContext ctx) {
    }

    public void enterAdditiveExpression(EsperEPL2GrammarParser.AdditiveExpressionContext ctx) {
    }

    public void enterEventProperty(EsperEPL2GrammarParser.EventPropertyContext ctx) {
    }

    public void enterJsonarray(EsperEPL2GrammarParser.JsonarrayContext ctx) {
    }

    public void exitJsonarray(EsperEPL2GrammarParser.JsonarrayContext ctx) {
    }

    public void enterJsonobject(EsperEPL2GrammarParser.JsonobjectContext ctx) {
    }

    public void enterOuterJoin(EsperEPL2GrammarParser.OuterJoinContext ctx) {
    }

    public void enterEscapableIdent(EsperEPL2GrammarParser.EscapableIdentContext ctx) {
    }

    public void exitEscapableIdent(EsperEPL2GrammarParser.EscapableIdentContext ctx) {
    }

    public void enterFromClause(EsperEPL2GrammarParser.FromClauseContext ctx) {
    }

    public void exitFromClause(EsperEPL2GrammarParser.FromClauseContext ctx) {
    }

    public void enterOnExpr(EsperEPL2GrammarParser.OnExprContext ctx) {
    }

    public void enterGopParamsItemMany(EsperEPL2GrammarParser.GopParamsItemManyContext ctx) {
    }

    public void exitGopParamsItemMany(EsperEPL2GrammarParser.GopParamsItemManyContext ctx) {
    }

    public void enterPropertySelectionList(EsperEPL2GrammarParser.PropertySelectionListContext ctx) {
    }

    public void exitPropertySelectionList(EsperEPL2GrammarParser.PropertySelectionListContext ctx) {
    }

    public void enterWeekPart(EsperEPL2GrammarParser.WeekPartContext ctx) {
    }

    public void exitWeekPart(EsperEPL2GrammarParser.WeekPartContext ctx) {
    }

    public void enterMatchRecogPatternAlteration(EsperEPL2GrammarParser.MatchRecogPatternAlterationContext ctx) {
    }

    public void enterGopParams(EsperEPL2GrammarParser.GopParamsContext ctx) {
    }

    public void exitGopParams(EsperEPL2GrammarParser.GopParamsContext ctx) {
    }

    public void enterCreateContextChoice(EsperEPL2GrammarParser.CreateContextChoiceContext ctx) {
    }

    public void exitCreateContextChoice(EsperEPL2GrammarParser.CreateContextChoiceContext ctx) {
    }

    public void enterCaseExpression(EsperEPL2GrammarParser.CaseExpressionContext ctx) {
    }

    public void enterCreateIndexColumn(EsperEPL2GrammarParser.CreateIndexColumnContext ctx) {
    }

    public void exitCreateIndexColumn(EsperEPL2GrammarParser.CreateIndexColumnContext ctx) {
    }

    public void enterExpressionWithTimeList(EsperEPL2GrammarParser.ExpressionWithTimeListContext ctx) {
    }

    public void exitExpressionWithTimeList(EsperEPL2GrammarParser.ExpressionWithTimeListContext ctx) {
    }

    public void enterGopParamsItemAs(EsperEPL2GrammarParser.GopParamsItemAsContext ctx) {
    }

    public void exitGopParamsItemAs(EsperEPL2GrammarParser.GopParamsItemAsContext ctx) {
    }

    public void enterRowLimit(EsperEPL2GrammarParser.RowLimitContext ctx) {
    }

    public void enterCreateSchemaQual(EsperEPL2GrammarParser.CreateSchemaQualContext ctx) {
    }

    public void exitCreateSchemaQual(EsperEPL2GrammarParser.CreateSchemaQualContext ctx) {
    }

    public void enterMatchUntilRange(EsperEPL2GrammarParser.MatchUntilRangeContext ctx) {
    }

    public void exitMatchUntilRange(EsperEPL2GrammarParser.MatchUntilRangeContext ctx) {
    }

    public void enterMatchRecogDefine(EsperEPL2GrammarParser.MatchRecogDefineContext ctx) {
    }

    public void exitMatchRecogDefine(EsperEPL2GrammarParser.MatchRecogDefineContext ctx) {
    }

    public void enterOrderByListElement(EsperEPL2GrammarParser.OrderByListElementContext ctx) {
    }

    public void enterMinutePart(EsperEPL2GrammarParser.MinutePartContext ctx) {
    }

    public void exitMinutePart(EsperEPL2GrammarParser.MinutePartContext ctx) {
    }

    public void enterMergeUnmatched(EsperEPL2GrammarParser.MergeUnmatchedContext ctx) {
    }

    public void enterMethodJoinExpression(EsperEPL2GrammarParser.MethodJoinExpressionContext ctx) {
    }

    public void exitMethodJoinExpression(EsperEPL2GrammarParser.MethodJoinExpressionContext ctx) {
    }

    public void enterExistsSubSelectExpression(EsperEPL2GrammarParser.ExistsSubSelectExpressionContext ctx) {
    }

    public void enterCreateContextRangePoint(EsperEPL2GrammarParser.CreateContextRangePointContext ctx) {
    }

    public void exitCreateContextRangePoint(EsperEPL2GrammarParser.CreateContextRangePointContext ctx) {
    }

    public void enterLibFunctionArgItem(EsperEPL2GrammarParser.LibFunctionArgItemContext ctx) {
    }

    public void exitLibFunctionArgItem(EsperEPL2GrammarParser.LibFunctionArgItemContext ctx) {
    }

    public void enterRegularJoin(EsperEPL2GrammarParser.RegularJoinContext ctx) {
    }

    public void exitRegularJoin(EsperEPL2GrammarParser.RegularJoinContext ctx) {
    }

    public void enterUpdateDetails(EsperEPL2GrammarParser.UpdateDetailsContext ctx) {
    }

    public void exitUpdateDetails(EsperEPL2GrammarParser.UpdateDetailsContext ctx) {
    }

    public void enterArrayExpression(EsperEPL2GrammarParser.ArrayExpressionContext ctx) {
    }

    public void visitErrorNode(ErrorNode errorNode) {
    }

    public void enterEveryRule(ParserRuleContext parserRuleContext) {
    }

    public void exitEveryRule(ParserRuleContext parserRuleContext) {
    }

    public void enterAndExpression(EsperEPL2GrammarParser.AndExpressionContext ctx) {
    }

    public void enterFollowedByRepeat(EsperEPL2GrammarParser.FollowedByRepeatContext ctx) {
    }

    public void exitFollowedByRepeat(EsperEPL2GrammarParser.FollowedByRepeatContext ctx) {
    }

    public void enterFollowedByExpression(EsperEPL2GrammarParser.FollowedByExpressionContext ctx) {
    }

    public void enterOrExpression(EsperEPL2GrammarParser.OrExpressionContext ctx) {
    }

    public void enterQualifyExpression(EsperEPL2GrammarParser.QualifyExpressionContext ctx) {
    }

    public void enterMatchUntilExpression(EsperEPL2GrammarParser.MatchUntilExpressionContext ctx) {
    }

    public void enterGuardPostFix(EsperEPL2GrammarParser.GuardPostFixContext ctx) {
    }

    public void enterBuiltin_coalesce(EsperEPL2GrammarParser.Builtin_coalesceContext ctx) {
    }

    public void enterBuiltin_typeof(EsperEPL2GrammarParser.Builtin_typeofContext ctx) {
    }

    public void enterBuiltin_avedev(EsperEPL2GrammarParser.Builtin_avedevContext ctx) {
    }

    public void enterBuiltin_prevcount(EsperEPL2GrammarParser.Builtin_prevcountContext ctx) {
    }

    public void enterBuiltin_stddev(EsperEPL2GrammarParser.Builtin_stddevContext ctx) {
    }

    public void enterBuiltin_sum(EsperEPL2GrammarParser.Builtin_sumContext ctx) {
    }

    public void enterBuiltin_exists(EsperEPL2GrammarParser.Builtin_existsContext ctx) {
    }

    public void enterBuiltin_prior(EsperEPL2GrammarParser.Builtin_priorContext ctx) {
    }

    public void enterBuiltin_instanceof(EsperEPL2GrammarParser.Builtin_instanceofContext ctx) {
    }

    public void enterBuiltin_currts(EsperEPL2GrammarParser.Builtin_currtsContext ctx) {
    }

    public void enterBuiltin_median(EsperEPL2GrammarParser.Builtin_medianContext ctx) {
    }

    public void enterFuncIdentChained(EsperEPL2GrammarParser.FuncIdentChainedContext ctx) {
    }

    public void exitFuncIdentChained(EsperEPL2GrammarParser.FuncIdentChainedContext ctx) {
    }

    public void enterFuncIdentTop(EsperEPL2GrammarParser.FuncIdentTopContext ctx) {
    }

    public void exitFuncIdentTop(EsperEPL2GrammarParser.FuncIdentTopContext ctx) {
    }

    public void enterBuiltin_avg(EsperEPL2GrammarParser.Builtin_avgContext ctx) {
    }

    public void enterBuiltin_cast(EsperEPL2GrammarParser.Builtin_castContext ctx) {
    }

    public void enterBuiltin_cnt(EsperEPL2GrammarParser.Builtin_cntContext ctx) {
    }

    public void enterBuiltin_prev(EsperEPL2GrammarParser.Builtin_prevContext ctx) {
    }

    public void enterBuiltin_istream(EsperEPL2GrammarParser.Builtin_istreamContext ctx) {
    }

    public void enterBuiltin_prevwindow(EsperEPL2GrammarParser.Builtin_prevwindowContext ctx) {
    }

    public void enterBuiltin_prevtail(EsperEPL2GrammarParser.Builtin_prevtailContext ctx) {
    }

    public void enterFafInsert(EsperEPL2GrammarParser.FafInsertContext ctx) {
    }

    public void enterGroupByListChoice(EsperEPL2GrammarParser.GroupByListChoiceContext ctx) {
    }

    public void exitGroupByListChoice(EsperEPL2GrammarParser.GroupByListChoiceContext ctx) {
    }

    public void enterGroupBySetsChoice(EsperEPL2GrammarParser.GroupBySetsChoiceContext ctx) {
    }

    public void exitGroupBySetsChoice(EsperEPL2GrammarParser.GroupBySetsChoiceContext ctx) {
    }

    public void exitSelectExpr(EsperEPL2GrammarParser.SelectExprContext ctx) {
    }

    public void enterGroupByCubeOrRollup(EsperEPL2GrammarParser.GroupByCubeOrRollupContext ctx) {
    }

    public void exitGroupByCubeOrRollup(EsperEPL2GrammarParser.GroupByCubeOrRollupContext ctx) {
    }

    public void enterGroupByGroupingSets(EsperEPL2GrammarParser.GroupByGroupingSetsContext ctx) {
    }

    public void exitGroupByGroupingSets(EsperEPL2GrammarParser.GroupByGroupingSetsContext ctx) {
    }

    public void enterGroupByCombinableExpr(EsperEPL2GrammarParser.GroupByCombinableExprContext ctx) {
    }

    public void exitGroupByCombinableExpr(EsperEPL2GrammarParser.GroupByCombinableExprContext ctx) {
    }

    public void enterBuiltin_grouping(EsperEPL2GrammarParser.Builtin_groupingContext ctx) {
    }

    public void enterBuiltin_groupingid(EsperEPL2GrammarParser.Builtin_groupingidContext ctx) {
    }

    public void enterFuncIdentInner(EsperEPL2GrammarParser.FuncIdentInnerContext ctx) {
    }

    public void exitFuncIdentInner(EsperEPL2GrammarParser.FuncIdentInnerContext ctx) {
    }

    public void enterCreateTableColumnPlain(EsperEPL2GrammarParser.CreateTableColumnPlainContext ctx) {
    }

    public void exitCreateTableColumnPlain(EsperEPL2GrammarParser.CreateTableColumnPlainContext ctx) {
    }

    public void enterCreateTableExpr(EsperEPL2GrammarParser.CreateTableExprContext ctx) {
    }

    public void enterCreateTableColumn(EsperEPL2GrammarParser.CreateTableColumnContext ctx) {
    }

    public void exitCreateTableColumn(EsperEPL2GrammarParser.CreateTableColumnContext ctx) {
    }

    public void enterCreateTableColumnList(EsperEPL2GrammarParser.CreateTableColumnListContext ctx) {
    }

    public void exitCreateTableColumnList(EsperEPL2GrammarParser.CreateTableColumnListContext ctx) {
    }

    public void enterIntoTableExpr(EsperEPL2GrammarParser.IntoTableExprContext ctx) {
    }

    public void enterSubstitutionCanChain(EsperEPL2GrammarParser.SubstitutionCanChainContext ctx) {
    }

    public void enterSlashIdentifier(EsperEPL2GrammarParser.SlashIdentifierContext ctx) {
    }

    public void exitSlashIdentifier(EsperEPL2GrammarParser.SlashIdentifierContext ctx) {
    }

    public void enterMatchRecogPatternRepeat(EsperEPL2GrammarParser.MatchRecogPatternRepeatContext ctx) {
    }

    public void exitMatchRecogPatternRepeat(EsperEPL2GrammarParser.MatchRecogPatternRepeatContext ctx) {
    }

    public void enterMatchRecogPatternPermute(EsperEPL2GrammarParser.MatchRecogPatternPermuteContext ctx) {
    }

    public void enterExpressionListWithNamed(EsperEPL2GrammarParser.ExpressionListWithNamedContext ctx) {
    }

    public void exitExpressionListWithNamed(EsperEPL2GrammarParser.ExpressionListWithNamedContext ctx) {
    }

    public void enterExpressionNamedParameter(EsperEPL2GrammarParser.ExpressionNamedParameterContext ctx) {
    }

    public void enterExpressionWithNamed(EsperEPL2GrammarParser.ExpressionWithNamedContext ctx) {
    }

    public void exitExpressionWithNamed(EsperEPL2GrammarParser.ExpressionWithNamedContext ctx) {
    }

    public void enterBuiltin_firstlastwindow(EsperEPL2GrammarParser.Builtin_firstlastwindowContext ctx) {
    }

    public void enterFirstLastWindowAggregation(EsperEPL2GrammarParser.FirstLastWindowAggregationContext ctx) {
    }

    public void exitFirstLastWindowAggregation(EsperEPL2GrammarParser.FirstLastWindowAggregationContext ctx) {
    }

    public void enterExpressionWithNamedWithTime(EsperEPL2GrammarParser.ExpressionWithNamedWithTimeContext ctx) {
    }

    public void exitExpressionWithNamedWithTime(EsperEPL2GrammarParser.ExpressionWithNamedWithTimeContext ctx) {
    }

    public void enterExpressionNamedParameterWithTime(EsperEPL2GrammarParser.ExpressionNamedParameterWithTimeContext ctx) {
    }

    public void enterExpressionListWithNamedWithTime(EsperEPL2GrammarParser.ExpressionListWithNamedWithTimeContext ctx) {
    }

    public void exitExpressionListWithNamedWithTime(EsperEPL2GrammarParser.ExpressionListWithNamedWithTimeContext ctx) {
    }

    public void enterViewExpressions(EsperEPL2GrammarParser.ViewExpressionsContext ctx) {
    }

    public void exitViewExpressions(EsperEPL2GrammarParser.ViewExpressionsContext ctx) {
    }

    public void enterViewWParameters(EsperEPL2GrammarParser.ViewWParametersContext ctx) {
    }

    public void enterViewExpressionWNamespace(EsperEPL2GrammarParser.ViewExpressionWNamespaceContext ctx) {
    }

    public void exitViewWParameters(EsperEPL2GrammarParser.ViewWParametersContext ctx) {
    }

    public void enterViewExpressionOptNamespace(EsperEPL2GrammarParser.ViewExpressionOptNamespaceContext ctx) {
    }

    public void enterOnSelectInsertFromClause(EsperEPL2GrammarParser.OnSelectInsertFromClauseContext ctx) {
    }

    public void enterExpressionTypeAnno(EsperEPL2GrammarParser.ExpressionTypeAnnoContext ctx) {
    }

    public void exitExpressionTypeAnno(EsperEPL2GrammarParser.ExpressionTypeAnnoContext ctx) {
    }

    public void enterTypeExpressionAnnotation(EsperEPL2GrammarParser.TypeExpressionAnnotationContext ctx) {
    }

    public void exitTypeExpressionAnnotation(EsperEPL2GrammarParser.TypeExpressionAnnotationContext ctx) {
    }

    public void enterIdentOrTicked(EsperEPL2GrammarParser.IdentOrTickedContext ctx) {
    }

    public void exitIdentOrTicked(EsperEPL2GrammarParser.IdentOrTickedContext ctx) {
    }

    public void enterOnMergeDirectInsert(EsperEPL2GrammarParser.OnMergeDirectInsertContext ctx) {
    }

    public void enterCreateContextPartitionInit(EsperEPL2GrammarParser.CreateContextPartitionInitContext ctx) {
    }

    public void exitCreateContextPartitionInit(EsperEPL2GrammarParser.CreateContextPartitionInitContext ctx) {
    }

    public void enterCreateContextPartitionTerm(EsperEPL2GrammarParser.CreateContextPartitionTermContext ctx) {
    }

    public void exitCreateContextPartitionTerm(EsperEPL2GrammarParser.CreateContextPartitionTermContext ctx) {
    }
}
