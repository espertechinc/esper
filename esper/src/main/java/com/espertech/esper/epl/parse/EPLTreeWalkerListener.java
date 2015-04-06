/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
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
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.db.DatabasePollingViewableFactory;
import com.espertech.esper.epl.declexpr.ExprDeclaredHelper;
import com.espertech.esper.epl.declexpr.ExprDeclaredNodeImpl;
import com.espertech.esper.epl.declexpr.ExprDeclaredService;
import com.espertech.esper.epl.expression.accessagg.ExprAggMultiFunctionLinearAccessNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.dot.ExprDotNode;
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
import com.espertech.esper.schedule.SchedulingService;
import com.espertech.esper.schedule.TimeProvider;
import com.espertech.esper.type.*;
import com.espertech.esper.type.StringValue;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.util.LazyAllocatedMap;
import com.espertech.esper.util.PlaceholderParseException;
import com.espertech.esper.util.PlaceholderParser;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Tree;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * Called during the walks of a EPL expression AST tree as specified in the grammar file.
 * Constructs filter and view specifications etc.
 */
public class EPLTreeWalkerListener implements EsperEPL2GrammarListener
{
    private static final Log log = LogFactory.getLog(EPLTreeWalkerListener.class);

    private static Set<Integer> EVENT_FILTER_WALK_EXCEPTIONS__RECURSIVE = new HashSet<Integer>();
    private static Set<Integer> WHERE_CLAUSE_WALK_EXCEPTIONS__RECURSIVE = new HashSet<Integer>();
    private static Set<Integer> EVENT_PROPERTY_WALK_EXCEPTIONS__PARENT = new HashSet<Integer>();
    private static Set<Integer> SELECT_EXPRELE_WALK_EXCEPTIONS__RECURSIVE = new HashSet<Integer>();

    static {
        EVENT_FILTER_WALK_EXCEPTIONS__RECURSIVE.add(EsperEPL2GrammarParser.RULE_createContextDetail);
        EVENT_FILTER_WALK_EXCEPTIONS__RECURSIVE.add(EsperEPL2GrammarParser.RULE_createContextFilter);
        EVENT_FILTER_WALK_EXCEPTIONS__RECURSIVE.add(EsperEPL2GrammarParser.RULE_createContextPartitionItem);
        EVENT_FILTER_WALK_EXCEPTIONS__RECURSIVE.add(EsperEPL2GrammarParser.RULE_createContextCoalesceItem);

        WHERE_CLAUSE_WALK_EXCEPTIONS__RECURSIVE.add(EsperEPL2GrammarParser.RULE_patternExpression);
        WHERE_CLAUSE_WALK_EXCEPTIONS__RECURSIVE.add(EsperEPL2GrammarParser.RULE_mergeMatchedItem);
        WHERE_CLAUSE_WALK_EXCEPTIONS__RECURSIVE.add(EsperEPL2GrammarParser.RULE_mergeInsert);
        WHERE_CLAUSE_WALK_EXCEPTIONS__RECURSIVE.add(EsperEPL2GrammarParser.RULE_updateDetails);
        WHERE_CLAUSE_WALK_EXCEPTIONS__RECURSIVE.add(EsperEPL2GrammarParser.RULE_onSetExpr);
        WHERE_CLAUSE_WALK_EXCEPTIONS__RECURSIVE.add(EsperEPL2GrammarParser.RULE_onUpdateExpr);

        EVENT_PROPERTY_WALK_EXCEPTIONS__PARENT.add(EsperEPL2GrammarParser.RULE_newAssign);
        EVENT_PROPERTY_WALK_EXCEPTIONS__PARENT.add(EsperEPL2GrammarParser.RULE_createContextPartitionItem);
        EVENT_PROPERTY_WALK_EXCEPTIONS__PARENT.add(EsperEPL2GrammarParser.RULE_createContextDetail);
        EVENT_PROPERTY_WALK_EXCEPTIONS__PARENT.add(EsperEPL2GrammarParser.RULE_createContextFilter);
        EVENT_PROPERTY_WALK_EXCEPTIONS__PARENT.add(EsperEPL2GrammarParser.RULE_createContextCoalesceItem);

        SELECT_EXPRELE_WALK_EXCEPTIONS__RECURSIVE.add(EsperEPL2GrammarParser.RULE_mergeInsert);
    }

    // private holding areas for accumulated info
    private Map<Tree, ExprNode> astExprNodeMap = new LinkedHashMap<Tree, ExprNode>();
    private final Stack<Map<Tree, ExprNode>> astExprNodeMapStack;

    private final Map<Tree, EvalFactoryNode> astPatternNodeMap = new LinkedHashMap<Tree, EvalFactoryNode>();

    private final Map<Tree, RowRegexExprNode> astRowRegexNodeMap = new HashMap<Tree, RowRegexExprNode>();

    private final Map<Tree, Object> astGOPNodeMap = new HashMap<Tree, Object>();

    private final Map<Tree, StatementSpecRaw> astStatementSpecMap = new HashMap<Tree, StatementSpecRaw>();

    private LazyAllocatedMap<ConfigurationPlugInAggregationMultiFunction, PlugInAggregationMultiFunctionFactory> plugInAggregations = new LazyAllocatedMap<ConfigurationPlugInAggregationMultiFunction, PlugInAggregationMultiFunctionFactory>();

    private FilterSpecRaw filterSpec;
    private final List<ViewSpec> viewSpecs = new LinkedList<ViewSpec>();

    // AST Walk result
    private List<ExprSubstitutionNode> substitutionParamNodes = new ArrayList<ExprSubstitutionNode>();
    private StatementSpecRaw statementSpec;
    private final Stack<StatementSpecRaw> statementSpecStack;

    private List<SelectClauseElementRaw> propertySelectRaw;
    private PropertyEvalSpec propertyEvalSpec;
    private List<OnTriggerMergeMatched> mergeMatcheds;
    private List<OnTriggerMergeAction> mergeActions;
    private ContextDescriptor contextDescriptor;

    private final CommonTokenStream tokenStream;
    private final EngineImportService engineImportService;
    private final VariableService variableService;
    private final TimeProvider timeProvider;
    private final ExprEvaluatorContext exprEvaluatorContext;
    private final SelectClauseStreamSelectorEnum defaultStreamSelector;
    private final String engineURI;
    private final ConfigurationInformation configurationInformation;
    private final SchedulingService schedulingService;
    private final PatternNodeFactory patternNodeFactory;
    private final ContextManagementService contextManagementService;
    private final List<String> scriptBodies;
    private final ExprDeclaredService exprDeclaredService;
    private final List<ExpressionScriptProvided> scriptExpressions;
    private final ExpressionDeclDesc expressionDeclarations;
    private final TableService tableService;

    /**
     * Ctor.
     * @param engineImportService is required to resolve lib-calls into static methods or configured aggregation functions
     * @param variableService for variable access
     * @param defaultStreamSelector - the configuration for which insert or remove streams (or both) to produce
     * @param engineURI engine URI
     * @param configurationInformation configuration info
     */
    public EPLTreeWalkerListener(CommonTokenStream tokenStream,
                                 EngineImportService engineImportService,
                                 VariableService variableService,
                                 SchedulingService schedulingService,
                                 SelectClauseStreamSelectorEnum defaultStreamSelector,
                                 String engineURI,
                                 ConfigurationInformation configurationInformation,
                                 PatternNodeFactory patternNodeFactory,
                                 ContextManagementService contextManagementService,
                                 List<String> scriptBodies,
                                 ExprDeclaredService exprDeclaredService,
                                 TableService tableService)
    {
        this.tokenStream = tokenStream;
        this.engineImportService = engineImportService;
        this.variableService = variableService;
        this.defaultStreamSelector = defaultStreamSelector;
        this.timeProvider = schedulingService;
        this.patternNodeFactory = patternNodeFactory;
        this.exprEvaluatorContext = new ExprEvaluatorContextTimeOnly(timeProvider);
        this.engineURI = engineURI;
        this.configurationInformation = configurationInformation;
        this.schedulingService = schedulingService;
        this.contextManagementService = contextManagementService;
        this.scriptBodies = scriptBodies;
        this.exprDeclaredService = exprDeclaredService;
        this.tableService = tableService;

        if (defaultStreamSelector == null)
        {
            throw ASTWalkException.from("Default stream selector is null");
        }

        statementSpec = new StatementSpecRaw(defaultStreamSelector);
        statementSpecStack = new Stack<StatementSpecRaw>();
        astExprNodeMapStack = new Stack<Map<Tree, ExprNode>>();

        // statement-global items
        expressionDeclarations = new ExpressionDeclDesc();
        statementSpec.setExpressionDeclDesc(expressionDeclarations);
        scriptExpressions = new ArrayList<ExpressionScriptProvided>(1);
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
        astExprNodeMap = new HashMap<Tree, ExprNode>();
    }

    private void popStatementContext(ParseTree ctx)
    {
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

    public void exitContextExpr(@NotNull EsperEPL2GrammarParser.ContextExprContext ctx) {
        String contextName = ctx.i.getText();
        statementSpec.setOptionalContextName(contextName);
        contextDescriptor = contextManagementService.getContextDescriptor(contextName);
    }

    public void exitEvalRelationalExpression(@NotNull EsperEPL2GrammarParser.EvalRelationalExpressionContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        boolean isNot = ctx.n != null;
        ExprNode exprNode;
        if (ctx.like != null) {
            exprNode = new ExprLikeNode(isNot);
        }
        else if (ctx.in != null && ctx.col != null) { // range
            boolean isLowInclude = ctx.LBRACK() != null;
            boolean isHighInclude = ctx.RBRACK() != null;
            exprNode = new ExprBetweenNodeImpl(isLowInclude, isHighInclude, isNot);
        }
        else if (ctx.in != null) {
            exprNode = new ExprInNodeImpl(isNot);
        }
        else if (ctx.inSubSelectQuery() != null) {
            StatementSpecRaw currentSpec = astStatementSpecMap.remove(ctx.inSubSelectQuery().subQueryExpr());
            exprNode = new ExprSubselectInNode(currentSpec, isNot);
        }
        else if (ctx.between != null) {
            exprNode = new ExprBetweenNodeImpl(true, true, isNot);
        }
        else if (ctx.regex != null) {
            exprNode = new ExprRegexpNode(isNot);
        }
        else if (ctx.r != null) {
            RelationalOpEnum relationalOpEnum;
            switch (ctx.r.getType()) {
                case EsperEPL2GrammarLexer.LT :
                    relationalOpEnum = RelationalOpEnum.LT;
                    break;
                case EsperEPL2GrammarLexer.GT :
                    relationalOpEnum = RelationalOpEnum.GT;
                    break;
                case EsperEPL2GrammarLexer.LE :
                    relationalOpEnum = RelationalOpEnum.LE;
                    break;
                case EsperEPL2GrammarLexer.GE :
                    relationalOpEnum = RelationalOpEnum.GE;
                    break;
                default :
                    throw ASTWalkException.from("Encountered unrecognized node type " + ctx.r.getType(), tokenStream, ctx);
            }

            boolean isAll = ctx.g != null && ctx.g.getType() == EsperEPL2GrammarLexer.ALL;
            boolean isAny = ctx.g != null && (ctx.g.getType() == EsperEPL2GrammarLexer.ANY || ctx.g.getType() == EsperEPL2GrammarLexer.SOME);

            if (isAll || isAny) {
                if (ctx.subSelectGroupExpression() != null) {
                    StatementSpecRaw currentSpec = astStatementSpecMap.remove(ctx.subSelectGroupExpression().subQueryExpr());
                    exprNode = new ExprSubselectAllSomeAnyNode(currentSpec, false, isAll, relationalOpEnum);
                }
                else {
                    exprNode = new ExprRelationalOpAllAnyNode(relationalOpEnum, isAll);
                }
            }
            else {
                exprNode = new ExprRelationalOpNodeImpl(relationalOpEnum);
            }
        }
        else {
            throw ASTWalkException.from("Encountered unrecognized relational op", tokenStream, ctx);
        }
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(exprNode, ctx, astExprNodeMap);
        if (ctx.like != null && ctx.stringconstant() != null) {
            exprNode.addChildNode(new ExprConstantNodeImpl(ASTConstantHelper.parse(ctx.stringconstant())));
        }
    }

    public void exitLibFunction(@NotNull EsperEPL2GrammarParser.LibFunctionContext ctx) {
        ASTLibFunctionHelper.handleLibFunc(tokenStream, ctx, configurationInformation, engineImportService, astExprNodeMap, plugInAggregations, engineURI, expressionDeclarations, exprDeclaredService, scriptExpressions, contextDescriptor, tableService, statementSpec);
    }

    public void exitMatchRecog(@NotNull EsperEPL2GrammarParser.MatchRecogContext ctx) {
        boolean allMatches = ctx.matchRecogMatchesSelection() != null && ctx.matchRecogMatchesSelection().ALL() != null;
        if (ctx.matchRecogMatchesAfterSkip() != null) {
            MatchRecognizeSkipEnum skip = ASTMatchRecognizeHelper.parseSkip(tokenStream, ctx.matchRecogMatchesAfterSkip());
            statementSpec.getMatchRecognizeSpec().getSkip().setSkip(skip);
        }

        if (ctx.matchRecogMatchesInterval() != null) {
            if (!ctx.matchRecogMatchesInterval().i.getText().toLowerCase().equals("interval")) {
                throw ASTWalkException.from("Invalid interval-clause within match-recognize, expecting keyword INTERVAL", tokenStream, ctx.matchRecogMatchesInterval());
            }
            ExprNode expression = ASTExprHelper.exprCollectSubNodes(ctx.matchRecogMatchesInterval().timePeriod(), 0, astExprNodeMap).get(0);
            ExprTimePeriod timePeriodExpr = (ExprTimePeriod) expression;
            boolean orTerminated = ctx.matchRecogMatchesInterval().TERMINATED() != null;
            statementSpec.getMatchRecognizeSpec().setInterval(new MatchRecognizeInterval(timePeriodExpr, orTerminated));
        }

        statementSpec.getMatchRecognizeSpec().setAllMatches(allMatches);
    }

    public void exitMatchRecogPartitionBy(@NotNull EsperEPL2GrammarParser.MatchRecogPartitionByContext ctx) {
        if (statementSpec.getMatchRecognizeSpec() == null) {
            statementSpec.setMatchRecognizeSpec(new MatchRecognizeSpec());
        }
        List<ExprNode> nodes = ASTExprHelper.exprCollectSubNodes(ctx, 0, astExprNodeMap);
        statementSpec.getMatchRecognizeSpec().getPartitionByExpressions().addAll(nodes);
    }

    public void exitMergeMatchedItem(@NotNull EsperEPL2GrammarParser.MergeMatchedItemContext ctx) {
        if (mergeActions == null) {
            mergeActions = new ArrayList<OnTriggerMergeAction>();
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

    public void enterSubQueryExpr(@NotNull EsperEPL2GrammarParser.SubQueryExprContext ctx) {
        pushStatementContext();
    }

    public void exitSubQueryExpr(@NotNull EsperEPL2GrammarParser.SubQueryExprContext ctx) {
        popStatementContext(ctx);
    }

    public void exitMatchRecogDefineItem(@NotNull EsperEPL2GrammarParser.MatchRecogDefineItemContext ctx) {
        String first = ctx.i.getText();
        ExprNode exprNode = ASTExprHelper.exprCollectSubNodes(ctx, 0, astExprNodeMap).get(0);
        statementSpec.getMatchRecognizeSpec().getDefines().add(new MatchRecognizeDefineItem(first, exprNode));
    }

    public void exitMergeUnmatchedItem(@NotNull EsperEPL2GrammarParser.MergeUnmatchedItemContext ctx) {
        if (mergeActions == null) {
            mergeActions = new ArrayList<OnTriggerMergeAction>();
        }
        handleMergeInsert(ctx.mergeInsert());
    }

    public void exitHavingClause(@NotNull EsperEPL2GrammarParser.HavingClauseContext ctx) {
        if (astExprNodeMap.size() != 1) {
            throw new IllegalStateException("Having clause generated zero or more then one expression nodes");
        }
        statementSpec.setHavingExprRootNode(ASTExprHelper.exprCollectSubNodes(ctx, 0, astExprNodeMap).get(0));
        astExprNodeMap.clear();
    }

    public void exitMatchRecogMeasureItem(@NotNull EsperEPL2GrammarParser.MatchRecogMeasureItemContext ctx) {
        if (statementSpec.getMatchRecognizeSpec() == null) {
            statementSpec.setMatchRecognizeSpec(new MatchRecognizeSpec());
        }
        ExprNode exprNode = ASTExprHelper.exprCollectSubNodes(ctx, 0, astExprNodeMap).get(0);
        String name = ctx.i != null ? ctx.i.getText() : null;
        statementSpec.getMatchRecognizeSpec().addMeasureItem(new MatchRecognizeMeasureItem(exprNode, name));
    }

    public void exitObserverExpression(@NotNull EsperEPL2GrammarParser.ObserverExpressionContext ctx) {
        String objectNamespace = ctx.ns.getText();
        String objectName = ctx.a != null ? ctx.a.getText() : ctx.nm.getText();
        List<ExprNode> obsParameters = ASTExprHelper.exprCollectSubNodes(ctx, 2, astExprNodeMap);

        PatternObserverSpec observerSpec = new PatternObserverSpec(objectNamespace, objectName, obsParameters);
        EvalFactoryNode observerNode = this.patternNodeFactory.makeObserverNode(observerSpec);
        ASTExprHelper.patternCollectAddSubnodesAddParentNode(observerNode, ctx, astPatternNodeMap);
    }

    public void exitMatchRecogPatternNested(@NotNull EsperEPL2GrammarParser.MatchRecogPatternNestedContext ctx) {
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

    public void exitMatchRecogPatternPermute(@NotNull EsperEPL2GrammarParser.MatchRecogPatternPermuteContext ctx) {
        RowRegexExprNodePermute permuteNode = new RowRegexExprNodePermute();
        ASTExprHelper.regExCollectAddSubNodesAddParentNode(permuteNode, ctx, astRowRegexNodeMap);
    }

    public void exitEvalOrExpression(@NotNull EsperEPL2GrammarParser.EvalOrExpressionContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        ExprOrNode or = new ExprOrNode();
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(or, ctx, astExprNodeMap);
    }

    public void exitTimePeriod(@NotNull EsperEPL2GrammarParser.TimePeriodContext ctx) {
        ExprTimePeriod timeNode = ASTExprHelper.timePeriodGetExprAllParams(ctx, astExprNodeMap, variableService, statementSpec);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(timeNode, ctx, astExprNodeMap);
    }

    public void exitSelectionListElementExpr(@NotNull EsperEPL2GrammarParser.SelectionListElementExprContext ctx) {
        ExprNode exprNode;
        if (ASTUtil.isRecursiveParentRule(ctx, SELECT_EXPRELE_WALK_EXCEPTIONS__RECURSIVE)) {
            exprNode = ASTExprHelper.exprCollectSubNodes(ctx, 0, astExprNodeMap).get(0);
        }
        else {
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
            String annotation = ctx.selectionListElementAnno().i.getText().toLowerCase();
            if (annotation.equals("eventbean") || annotation.equals("eventbean")) {
                eventsAnnotation = true;
            }
            else {
                throw ASTWalkException.from("Failed to recognize select-expression annotation '" + annotation + "', expected 'eventbean'", tokenStream, ctx);
            }
        }

        // Add as selection element
        statementSpec.getSelectClauseSpec().add(new SelectClauseExprRawSpec(exprNode, optionalName, eventsAnnotation));
    }

    public void exitEventFilterExpression(@NotNull EsperEPL2GrammarParser.EventFilterExpressionContext ctx) {
        if (ASTUtil.isRecursiveParentRule(ctx, EVENT_FILTER_WALK_EXCEPTIONS__RECURSIVE)) {
            return;
        }

        // for event streams we keep the filter spec around for use when the stream definition is completed
        filterSpec = ASTFilterSpecHelper.walkFilterSpec(ctx, propertyEvalSpec, astExprNodeMap);

        // set property eval to null
        propertyEvalSpec = null;

        astExprNodeMap.clear();
    }

    public void exitMatchRecogPatternConcat(@NotNull EsperEPL2GrammarParser.MatchRecogPatternConcatContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        RowRegexExprNodeConcatenation concatNode = new RowRegexExprNodeConcatenation();
        ASTExprHelper.regExCollectAddSubNodesAddParentNode(concatNode, ctx, astRowRegexNodeMap);
    }

    public void exitNumberconstant(@NotNull EsperEPL2GrammarParser.NumberconstantContext ctx) {
        // if the parent is constant, don't need an expression
        if (ctx.getParent().getRuleContext().getRuleIndex() == EsperEPL2GrammarParser.RULE_constant) {
            return;
        }
        ExprConstantNode constantNode = new ExprConstantNodeImpl(ASTConstantHelper.parse(ctx));
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(constantNode, ctx, astExprNodeMap);
    }

    public void exitMatchRecogPattern(@NotNull EsperEPL2GrammarParser.MatchRecogPatternContext ctx) {
        RowRegexExprNode exprNode = ASTExprHelper.regExGetRemoveTopNode(ctx, astRowRegexNodeMap);
        if (exprNode == null) {
            throw new IllegalStateException("Expression node for AST node not found");
        }
        statementSpec.getMatchRecognizeSpec().setPattern(exprNode);
    }

    public void exitWhereClause(@NotNull EsperEPL2GrammarParser.WhereClauseContext ctx) {
        if (ctx.getParent().getRuleIndex() != EsperEPL2GrammarParser.RULE_subQueryExpr &&
                ASTUtil.isRecursiveParentRule(ctx, WHERE_CLAUSE_WALK_EXCEPTIONS__RECURSIVE)) { // ignore pattern
            return;
        }
        if (astExprNodeMap.size() != 1) {
            throw new IllegalStateException("Where clause generated zero or more then one expression nodes");
        }

        // Just assign the single root ExprNode not consumed yet
        statementSpec.setFilterExprRootNode(astExprNodeMap.values().iterator().next());
        astExprNodeMap.clear();
    }

    public void exitMatchRecogPatternAtom(@NotNull EsperEPL2GrammarParser.MatchRecogPatternAtomContext ctx) {
        String first = ctx.i.getText();
        RegexNFATypeEnum type = RegexNFATypeEnum.SINGLE;
        if (ctx.reluctant != null && ctx.s != null) {
            type = RegexNFATypeEnum.fromString(ctx.s.getText(), ctx.reluctant.getText());
        }
        else if (ctx.s != null) {
            type = RegexNFATypeEnum.fromString(ctx.s.getText(), null);
        }

        RowRegexExprRepeatDesc repeat = ASTMatchRecognizeHelper.walkOptionalRepeat(ctx.matchRecogPatternRepeat(), astExprNodeMap);
        RowRegexExprNodeAtom item = new RowRegexExprNodeAtom(first, type, repeat);
        ASTExprHelper.regExCollectAddSubNodesAddParentNode(item, ctx, astRowRegexNodeMap);
    }

    public void exitUpdateExpr(@NotNull EsperEPL2GrammarParser.UpdateExprContext ctx) {
        EsperEPL2GrammarParser.UpdateDetailsContext updctx = ctx.updateDetails();
        String eventTypeName = ASTUtil.unescapeClassIdent(updctx.classIdentifier());
        FilterStreamSpecRaw streamSpec = new FilterStreamSpecRaw(new FilterSpecRaw(eventTypeName, Collections.<ExprNode>emptyList(), null), ViewSpec.EMPTY_VIEWSPEC_ARRAY, eventTypeName, new StreamSpecOptions());
        statementSpec.getStreamSpecs().add(streamSpec);
        String optionalStreamName = updctx.i != null ? updctx.i.getText() : null;
        List<OnTriggerSetAssignment> assignments = ASTExprHelper.getOnTriggerSetAssignments(updctx.onSetAssignmentList(), astExprNodeMap);
        ExprNode whereClause = updctx.WHERE() != null ? ASTExprHelper.exprCollectSubNodes(updctx.whereClause(), 0, astExprNodeMap).get(0) : null;
        statementSpec.setUpdateDesc(new UpdateDesc(optionalStreamName, assignments, whereClause));
    }

    public void exitFrequencyOperand(@NotNull EsperEPL2GrammarParser.FrequencyOperandContext ctx) {
        ExprNumberSetFrequency exprNode = new ExprNumberSetFrequency();
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(exprNode, ctx, astExprNodeMap);
        ASTExprHelper.addOptionalNumber(exprNode, ctx.number());
        ASTExprHelper.addOptionalSimpleProperty(exprNode, ctx.i, variableService, statementSpec);
    }

    public void exitCreateDataflow(@NotNull EsperEPL2GrammarParser.CreateDataflowContext ctx) {
        CreateDataFlowDesc graphDesc = ASTGraphHelper.walkCreateDataFlow(ctx, astGOPNodeMap, engineImportService);
        statementSpec.setCreateDataFlowDesc(graphDesc);
    }

    public void exitInsertIntoExpr(@NotNull EsperEPL2GrammarParser.InsertIntoExprContext ctx) {
        SelectClauseStreamSelectorEnum selector = SelectClauseStreamSelectorEnum.ISTREAM_ONLY;
        if (ctx.r != null) {
            selector = SelectClauseStreamSelectorEnum.RSTREAM_ONLY;
        }
        else if (ctx.ir != null) {
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

    public void exitCreateVariableExpr(@NotNull EsperEPL2GrammarParser.CreateVariableExprContext ctx) {

        boolean constant = false;
        if (ctx.c != null) {
            String text = ctx.c.getText();
            if (text.equals("constant") || text.equals("const")) {
                constant = true;
            }
            else {
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

    public void exitOnStreamExpr(@NotNull EsperEPL2GrammarParser.OnStreamExprContext ctx) {
        String streamAsName = ctx.i != null ? ctx.i.getText() : null;

        // get stream to use (pattern or filter)
        StreamSpecRaw streamSpec;
        if (ctx.eventFilterExpression() != null) {
            streamSpec = new FilterStreamSpecRaw(filterSpec, ViewSpec.EMPTY_VIEWSPEC_ARRAY, streamAsName, new StreamSpecOptions());
        }
        else if (ctx.patternInclusionExpression() != null) {
            if ((astPatternNodeMap.size() > 1) || ((astPatternNodeMap.isEmpty()))) {
                throw ASTWalkException.from("Unexpected AST tree contains zero or more then 1 child elements for root");
            }
            // Get expression node sub-tree from the AST nodes placed so far
            EvalFactoryNode evalNode = astPatternNodeMap.values().iterator().next();
            PatternLevelAnnotationFlags flags = getPatternFlags(ctx.patternInclusionExpression().annotationEnum());
            streamSpec = new PatternStreamSpecRaw(evalNode, ViewSpec.toArray(viewSpecs), streamAsName, new StreamSpecOptions(), flags.isSuppressSameEventMatches(), flags.isDiscardPartialsOnMatch());
            astPatternNodeMap.clear();
        }
        else {
            throw new IllegalStateException("Invalid AST type node, cannot map to stream specification");
        }
        statementSpec.getStreamSpecs().add(streamSpec);
    }

    public void exitPropertyExpressionAtomic(@NotNull EsperEPL2GrammarParser.PropertyExpressionAtomicContext ctx) {
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

        String splitterEventTypeName = null;
        if (ctx.propertyExpressionAnnotation() != null) {
            String annoName = ctx.propertyExpressionAnnotation().n.getText();
            if (!annoName.toLowerCase().equals("type")) {
                throw ASTWalkException.from("Invalid annotation for property selection, expected 'type' but found '" + annoName + "'", tokenStream, ctx);
            }
            splitterEventTypeName = ctx.propertyExpressionAnnotation().v.getText();
        }

        PropertyEvalAtom atom = new PropertyEvalAtom(splitterExpression, splitterEventTypeName, optionalAsName, optionalSelectClause, optionalWhereClause);
        propertyEvalSpec.add(atom);
    }

    public void exitFafUpdate(@NotNull EsperEPL2GrammarParser.FafUpdateContext ctx) {
        handleFAFNamedWindowStream(ctx.updateDetails().classIdentifier(), ctx.updateDetails().i);
        List<OnTriggerSetAssignment> assignments = ASTExprHelper.getOnTriggerSetAssignments(ctx.updateDetails().onSetAssignmentList(), astExprNodeMap);
        ExprNode whereClause = ctx.updateDetails().whereClause() == null ? null : ASTExprHelper.exprCollectSubNodes(ctx.updateDetails().whereClause(), 0, astExprNodeMap).get(0);
        statementSpec.setFilterExprRootNode(whereClause);
        statementSpec.setFireAndForgetSpec(new FireAndForgetSpecUpdate(assignments));
    }

    public void exitBitWiseExpression(@NotNull EsperEPL2GrammarParser.BitWiseExpressionContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        BitWiseOpEnum bitWiseOpEnum;
        int token = ASTUtil.getAssertTerminatedTokenType(ctx.getChild(1));
        switch (token)
        {
            case EsperEPL2GrammarLexer.BAND :
                bitWiseOpEnum = BitWiseOpEnum.BAND;
                break;
            case EsperEPL2GrammarLexer.BOR :
                bitWiseOpEnum = BitWiseOpEnum.BOR;
                break;
            case EsperEPL2GrammarLexer.BXOR :
                bitWiseOpEnum = BitWiseOpEnum.BXOR;
                break;
            default :
                throw ASTWalkException.from("Node type " + token + " not a recognized bit wise node type", tokenStream, ctx);
        }

        ExprBitWiseNode bwNode = new ExprBitWiseNode(bitWiseOpEnum);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(bwNode, ctx, astExprNodeMap);
    }

    public void exitEvalEqualsExpression(@NotNull EsperEPL2GrammarParser.EvalEqualsExpressionContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        ExprNode exprNode;
        boolean isNot = ctx.ne != null || ctx.isnot != null || ctx.sqlne != null;
        if (ctx.a == null) {
            boolean isIs = ctx.is != null || ctx.isnot != null;
            exprNode = new ExprEqualsNodeImpl(isNot, isIs);
        }
        else {
            boolean isAll = ctx.a.getType() == EsperEPL2GrammarLexer.ALL;
            List<EsperEPL2GrammarParser.SubSelectGroupExpressionContext> subselect = ctx.subSelectGroupExpression();
            if (subselect != null && !subselect.isEmpty()) {
                StatementSpecRaw currentSpec = astStatementSpecMap.remove(ctx.subSelectGroupExpression().get(0).subQueryExpr());
                exprNode = new ExprSubselectAllSomeAnyNode(currentSpec, isNot, isAll, null);
            }
            else {
                exprNode = new ExprEqualsAllAnyNode(isNot, isAll);
            }
        }
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(exprNode, ctx, astExprNodeMap);
    }

    public void exitGopConfig(@NotNull EsperEPL2GrammarParser.GopConfigContext ctx) {
        if (log.isDebugEnabled()) {
        }

        Object value;
        if (ctx.SELECT() == null) {
            if (ctx.expression() != null) {
                value = ASTExprHelper.exprCollectSubNodes(ctx, 0, astExprNodeMap).get(0);
            }
            else {
                if (ctx.jsonarray() != null) {
                    value = new ExprConstantNodeImpl(ASTJsonHelper.walkArray(tokenStream, ctx.jsonarray()));
                }
                else {
                    value = new ExprConstantNodeImpl(ASTJsonHelper.walkObject(tokenStream, ctx.jsonobject()));
                }
                ASTExprHelper.exprCollectSubNodes(ctx, 0, astExprNodeMap);
            }
        }
        else {
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

    public void exitCreateSelectionListElement(@NotNull EsperEPL2GrammarParser.CreateSelectionListElementContext ctx) {
        if (ctx.STAR() != null) {
            statementSpec.getSelectClauseSpec().add(new SelectClauseElementWildcard());
        }
        else {
            ExprNode expr = ASTExprHelper.exprCollectSubNodes(ctx, 0, astExprNodeMap).get(0);
            String asName = ctx.i != null ? ctx.i.getText() : null;
            statementSpec.getSelectClauseSpec().add(new SelectClauseExprRawSpec(expr, asName, false));
        }
    }

    public void exitFafDelete(@NotNull EsperEPL2GrammarParser.FafDeleteContext ctx) {
        handleFAFNamedWindowStream(ctx.classIdentifier(), ctx.i);
        statementSpec.setFireAndForgetSpec(new FireAndForgetSpecDelete());
    }

    public void exitConstant(@NotNull EsperEPL2GrammarParser.ConstantContext ctx) {
        ExprConstantNode constantNode = new ExprConstantNodeImpl(ASTConstantHelper.parse(ctx.getChild(0)));
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(constantNode, ctx, astExprNodeMap);
    }

    public void exitMergeMatched(@NotNull EsperEPL2GrammarParser.MergeMatchedContext ctx) {
        handleMergeMatchedUnmatched(ctx.expression(), true);
    }

    public void exitEvalAndExpression(@NotNull EsperEPL2GrammarParser.EvalAndExpressionContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        ExprAndNode and = new ExprAndNodeImpl();
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(and, ctx, astExprNodeMap);
    }

    public void exitForExpr(@NotNull EsperEPL2GrammarParser.ForExprContext ctx) {
        if (statementSpec.getForClauseSpec() == null) {
            statementSpec.setForClauseSpec(new ForClauseSpec());
        }
        String ident = ctx.i.getText();
        List<ExprNode> expressions = ASTExprHelper.exprCollectSubNodes(ctx, 0, astExprNodeMap);
        statementSpec.getForClauseSpec().getClauses().add(new ForClauseItemSpec(ident, expressions));
    }

    public void exitExpressionQualifyable(@NotNull EsperEPL2GrammarParser.ExpressionQualifyableContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        if (ctx.s != null) {
            ExprNode node = ASTExprHelper.timePeriodGetExprJustSeconds(ctx.expression(), astExprNodeMap);
            astExprNodeMap.put(ctx, node);
        }
        else if (ctx.a != null || ctx.d != null) {
            boolean isDescending = ctx.d != null;
            ExprNode node = ASTExprHelper.exprCollectSubNodes(ctx.expression(), 0, astExprNodeMap).get(0);
            ExprOrderedExpr exprNode = new ExprOrderedExpr(isDescending);
            exprNode.addChildNode(node);
            astExprNodeMap.put(ctx, exprNode);
        }
    }

    public void exitPropertySelectionListElement(@NotNull EsperEPL2GrammarParser.PropertySelectionListElementContext ctx) {
        SelectClauseElementRaw raw;
        if (ctx.s != null) {
            raw = new SelectClauseElementWildcard();
        }
        else if (ctx.propertyStreamSelector() != null) {
            raw = new SelectClauseStreamRawSpec(ctx.propertyStreamSelector().s.getText(),
                    ctx.propertyStreamSelector().i != null ? ctx.propertyStreamSelector().i.getText() : null);
        }
        else {
            ExprNode exprNode = ASTExprHelper.exprCollectSubNodes(ctx.expression(), 0, astExprNodeMap).get(0);
            String optionalName = ctx.keywordAllowedIdent() != null ? ctx.keywordAllowedIdent().getText() : null;
            raw = new SelectClauseExprRawSpec(exprNode, optionalName, false);
        }

        // Add as selection element
        if (propertySelectRaw == null) {
            propertySelectRaw = new ArrayList<SelectClauseElementRaw>();
        }
        this.propertySelectRaw.add(raw);
    }

    public void exitExpressionDecl(@NotNull EsperEPL2GrammarParser.ExpressionDeclContext ctx) {
        if (ctx.parent.getRuleIndex() == EsperEPL2GrammarParser.RULE_createExpressionExpr) {
            return;
        }

        Pair<ExpressionDeclItem, ExpressionScriptProvided> pair = ASTExpressionDeclHelper.walkExpressionDecl(ctx, scriptBodies, astExprNodeMap, tokenStream);
        if (pair.getFirst() != null) {
            expressionDeclarations.add(pair.getFirst());
        }
        else {
            scriptExpressions.add(pair.getSecond());
        }
    }

    public void exitSubstitutionCanChain(@NotNull EsperEPL2GrammarParser.SubstitutionCanChainContext ctx) {
        if (ctx.chainedFunction() == null) {
            return;
        }
        ExprSubstitutionNode substitutionNode = (ExprSubstitutionNode) astExprNodeMap.remove(ctx.substitution());
        List<ExprChainedSpec> chainSpec = ASTLibFunctionHelper.getLibFuncChain(ctx.chainedFunction().libFunctionNoClass(), astExprNodeMap);
        ExprDotNode exprNode = new ExprDotNode(chainSpec, engineImportService.isDuckType(), engineImportService.isUdfCache());
        exprNode.addChildNode(substitutionNode);
        astExprNodeMap.put(ctx, exprNode);
    }

    public void exitSubstitution(@NotNull EsperEPL2GrammarParser.SubstitutionContext ctx) {
        int currentSize = this.substitutionParamNodes.size();
        ExprSubstitutionNode substitutionNode;
        if (ctx.slashIdentifier() != null) {
            String name = ASTUtil.unescapeSlashIdentifier(ctx.slashIdentifier());
            substitutionNode = new ExprSubstitutionNode(name);
        }
        else {
            substitutionNode = new ExprSubstitutionNode(currentSize + 1);
        }
        ASTSubstitutionHelper.validateNewSubstitution(substitutionParamNodes, substitutionNode);
        substitutionParamNodes.add(substitutionNode);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(substitutionNode, ctx, astExprNodeMap);
    }

    public void exitWeekDayOperator(@NotNull EsperEPL2GrammarParser.WeekDayOperatorContext ctx) {
        ExprNumberSetCronParam exprNode = new ExprNumberSetCronParam(CronOperatorEnum.WEEKDAY);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(exprNode, ctx, astExprNodeMap);
        ASTExprHelper.addOptionalNumber(exprNode, ctx.number());
        ASTExprHelper.addOptionalSimpleProperty(exprNode, ctx.i, variableService, statementSpec);
    }

    public void exitLastWeekdayOperand(@NotNull EsperEPL2GrammarParser.LastWeekdayOperandContext ctx) {
        ExprNumberSetCronParam exprNode = new ExprNumberSetCronParam(CronOperatorEnum.LASTWEEKDAY);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(exprNode, ctx, astExprNodeMap);
    }

    public void exitGroupByListExpr(@NotNull EsperEPL2GrammarParser.GroupByListExprContext ctx) {
        ASTGroupByHelper.walkGroupBy(ctx, astExprNodeMap, statementSpec.getGroupByExpressions());
        astExprNodeMap.clear();
    }

    public void exitStreamSelector(@NotNull EsperEPL2GrammarParser.StreamSelectorContext ctx) {
        String streamName = ctx.s.getText();
        String optionalName = ctx.i != null ? ctx.i.getText() : null;
        statementSpec.getSelectClauseSpec().add(new SelectClauseStreamRawSpec(streamName, optionalName));
    }

    public void exitStreamExpression(@NotNull EsperEPL2GrammarParser.StreamExpressionContext ctx) {
        // Determine the optional stream name
        String streamName = ctx.i != null ? ctx.i.getText() : null;

        boolean isUnidirectional = ctx.UNIDIRECTIONAL() != null;
        boolean isRetainUnion = ctx.RETAINUNION() != null;
        boolean isRetainIntersection = ctx.RETAININTERSECTION() != null;

        // Convert to a stream specification instance
        StreamSpecRaw streamSpec;
        StreamSpecOptions options = new StreamSpecOptions(isUnidirectional, isRetainUnion, isRetainIntersection);

        // If the first subnode is a filter node, we have a filter stream specification
        if (ASTUtil.getRuleIndexIfProvided(ctx.getChild(0)) == EsperEPL2GrammarParser.RULE_eventFilterExpression) {
            streamSpec = new FilterStreamSpecRaw(filterSpec, ViewSpec.toArray(viewSpecs), streamName, options);
        }
        else if (ASTUtil.getRuleIndexIfProvided(ctx.getChild(0)) == EsperEPL2GrammarParser.RULE_patternInclusionExpression) {
            if ((astPatternNodeMap.size() > 1) || ((astPatternNodeMap.isEmpty()))) {
                throw ASTWalkException.from("Unexpected AST tree contains zero or more then 1 child elements for root");
            }
            EsperEPL2GrammarParser.PatternInclusionExpressionContext pctx = (EsperEPL2GrammarParser.PatternInclusionExpressionContext) ctx.getChild(0);

            // Get expression node sub-tree from the AST nodes placed so far
            EvalFactoryNode evalNode = astPatternNodeMap.values().iterator().next();
            PatternLevelAnnotationFlags flags = getPatternFlags(pctx.annotationEnum());
            streamSpec = new PatternStreamSpecRaw(evalNode, ViewSpec.toArray(viewSpecs), streamName, options, flags.isSuppressSameEventMatches(), flags.isDiscardPartialsOnMatch());
            astPatternNodeMap.clear();
        }
        else if (ctx.databaseJoinExpression() != null) {
            EsperEPL2GrammarParser.DatabaseJoinExpressionContext dbctx = ctx.databaseJoinExpression();
            String dbName = dbctx.i.getText();
            String sqlWithParams = StringValue.parseString(dbctx.s.getText());

            // determine if there is variables used
            List<PlaceholderParser.Fragment> sqlFragments;
            try
            {
                sqlFragments = PlaceholderParser.parsePlaceholder(sqlWithParams);
                for (PlaceholderParser.Fragment fragment : sqlFragments)
                {
                    if (!(fragment instanceof PlaceholderParser.ParameterFragment)) {
                        continue;
                    }

                    // Parse expression, store for substitution parameters
                    String expression = fragment.getValue();
                    if (expression.toUpperCase().equals(DatabasePollingViewableFactory.SAMPLE_WHERECLAUSE_PLACEHOLDER)) {
                        continue;
                    }

                    if (expression.trim().length() == 0) {
                        throw ASTWalkException.from("Missing expression within ${...} in SQL statement");
                    }
                    String toCompile = "select * from java.lang.Object where " + expression;
                    StatementSpecRaw raw = EPAdministratorHelper.compileEPL(toCompile, expression, false, null, SelectClauseStreamSelectorEnum.ISTREAM_ONLY,
                            engineImportService, variableService, schedulingService, engineURI, configurationInformation, patternNodeFactory, contextManagementService, exprDeclaredService, tableService);

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
                        listExp = new ArrayList<ExprNode>();
                        statementSpec.getSqlParameters().put(statementSpec.getStreamSpecs().size(), listExp);
                    }
                    listExp.add(raw.getFilterRootNode());
                }
            }
            catch (PlaceholderParseException ex) {
                log.warn("Failed to parse SQL text '" + sqlWithParams + "' :" + ex.getMessage());
                // Let the view construction handle the validation
            }

            String sampleSQL = null;
            if (dbctx.s2 != null) {
                sampleSQL = dbctx.s2.getText();
                sampleSQL = StringValue.parseString(sampleSQL.trim());
            }

            streamSpec = new DBStatementStreamSpec(streamName, ViewSpec.toArray(viewSpecs), dbName, sqlWithParams, sampleSQL);
        }
        else if (ctx.methodJoinExpression() != null)
        {
            EsperEPL2GrammarParser.MethodJoinExpressionContext mthctx = ctx.methodJoinExpression();
            String prefixIdent = mthctx.i.getText();
            String className = ASTUtil.unescapeClassIdent(mthctx.classIdentifier());

            int indexDot = className.lastIndexOf('.');
            String classNamePart;
            String methodNamePart;
            if (indexDot == -1)
            {
                classNamePart = className;
                methodNamePart = null;
            }
            else
            {
                classNamePart = className.substring(0, indexDot);
                methodNamePart = className.substring(indexDot + 1);
            }
            List<ExprNode> exprNodes = ASTExprHelper.exprCollectSubNodes(mthctx, 0, astExprNodeMap);

            if (variableService.getVariableMetaData(classNamePart) != null) {
                statementSpec.setHasVariables(true);
            }

            streamSpec = new MethodStreamSpec(streamName, ViewSpec.toArray(viewSpecs), prefixIdent, classNamePart, methodNamePart, exprNodes);
        }
        else {
            throw ASTWalkException.from("Unexpected AST child node to stream expression", tokenStream, ctx);
        }
        viewSpecs.clear();
        statementSpec.getStreamSpecs().add(streamSpec);
    }

    public void exitViewExpression(@NotNull EsperEPL2GrammarParser.ViewExpressionContext ctx) {
        String objectNamespace = ctx.getChild(0).getText();
        String objectName = ctx.getChild(2).getText();
        List<ExprNode> viewParameters = ASTExprHelper.exprCollectSubNodes(ctx, 2, astExprNodeMap);
        viewSpecs.add(new ViewSpec(objectNamespace, objectName, viewParameters));
    }

    public void exitPatternFilterExpression(@NotNull EsperEPL2GrammarParser.PatternFilterExpressionContext ctx) {
        String optionalPatternTagName = null;
        if (ctx.i != null) {
            optionalPatternTagName = ctx.i.getText();
        }

        String eventName = ASTUtil.unescapeClassIdent(ctx.classIdentifier());

        EsperEPL2GrammarParser.PatternFilterAnnotationContext anno = ctx.patternFilterAnnotation();
        Integer consumption = null;
        if (anno != null) {
            String name = ctx.patternFilterAnnotation().i.getText();
            if (!name.toUpperCase().equals("CONSUME")) {
                throw new EPException("Unexpected pattern filter @ annotation, expecting 'consume' but received '" + name + "'");
            }
            if (anno.number() != null) {
                Object val = ASTConstantHelper.parse(anno.number());
                consumption = ((Number) val).intValue();
            }
            else {
                consumption = 1;
            }
        }

        List<ExprNode> exprNodes = ASTExprHelper.exprCollectSubNodes(ctx, 0, astExprNodeMap);

        FilterSpecRaw rawFilterSpec = new FilterSpecRaw(eventName, exprNodes, propertyEvalSpec);
        propertyEvalSpec = null;
        EvalFactoryNode filterNode = patternNodeFactory.makeFilterNode(rawFilterSpec, optionalPatternTagName, consumption);
        ASTExprHelper.patternCollectAddSubnodesAddParentNode(filterNode, ctx, astPatternNodeMap);
    }

    public void exitOnSelectExpr(@NotNull EsperEPL2GrammarParser.OnSelectExprContext ctx) {
        statementSpec.getSelectClauseSpec().setDistinct(ctx.DISTINCT() != null);
    }

    public void exitStartPatternExpressionRule(@NotNull EsperEPL2GrammarParser.StartPatternExpressionRuleContext ctx) {
        if ((astPatternNodeMap.size() > 1) || ((astPatternNodeMap.isEmpty()))) {
            throw ASTWalkException.from("Unexpected AST tree contains zero or more then 1 child elements for root");
        }

        // Get expression node sub-tree from the AST nodes placed so far
        EvalFactoryNode evalNode = astPatternNodeMap.values().iterator().next();

        PatternStreamSpecRaw streamSpec = new PatternStreamSpecRaw(evalNode, ViewSpec.EMPTY_VIEWSPEC_ARRAY, null, new StreamSpecOptions(), false, false);
        statementSpec.getStreamSpecs().add(streamSpec);
        statementSpec.setSubstitutionParameters(substitutionParamNodes);

        astPatternNodeMap.clear();
    }

    public void exitOutputLimit(@NotNull EsperEPL2GrammarParser.OutputLimitContext ctx) {
        OutputLimitSpec spec = ASTOutputLimitHelper.buildOutputLimitSpec(tokenStream, ctx, astExprNodeMap, variableService, engineURI, timeProvider, exprEvaluatorContext);
        statementSpec.setOutputLimitSpec(spec);
        if (spec.getVariableName() != null) {
            statementSpec.setHasVariables(true);
            ASTExprHelper.addVariableReference(statementSpec, spec.getVariableName());
        }
    }

    public void exitNumericParameterList(@NotNull EsperEPL2GrammarParser.NumericParameterListContext ctx) {
        ExprNumberSetList exprNode = new ExprNumberSetList();
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(exprNode, ctx, astExprNodeMap);
    }

    public void exitCreateSchemaExpr(@NotNull EsperEPL2GrammarParser.CreateSchemaExprContext ctx) {
        CreateSchemaDesc createSchema = ASTCreateSchemaHelper.walkCreateSchema(ctx);
        if (ctx.parent.getRuleIndex() == EsperEPL2GrammarParser.RULE_eplExpression) {
            statementSpec.getStreamSpecs().add(new FilterStreamSpecRaw(new FilterSpecRaw(Object.class.getName(), Collections.<ExprNode>emptyList(), null), ViewSpec.EMPTY_VIEWSPEC_ARRAY, null, new StreamSpecOptions()));
        }
        statementSpec.setCreateSchemaDesc(createSchema);
    }

    public void exitLastOperator(@NotNull EsperEPL2GrammarParser.LastOperatorContext ctx) {
        ExprNumberSetCronParam exprNode = new ExprNumberSetCronParam(CronOperatorEnum.LASTDAY);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(exprNode, ctx, astExprNodeMap);
        ASTExprHelper.addOptionalNumber(exprNode, ctx.number());
        ASTExprHelper.addOptionalSimpleProperty(exprNode, ctx.i, variableService, statementSpec);
    }

    public void exitCreateIndexExpr(@NotNull EsperEPL2GrammarParser.CreateIndexExprContext ctx) {
        String indexName = ctx.n.getText();
        String windowName = ctx.w.getText();

        List<CreateIndexItem> columns = new ArrayList<CreateIndexItem>();
        boolean unique = false;
        List<EsperEPL2GrammarParser.CreateIndexColumnContext> cols = ctx.createIndexColumnList().createIndexColumn();
        for (EsperEPL2GrammarParser.CreateIndexColumnContext col : cols) {
            CreateIndexType type = CreateIndexType.HASH;
            String columnName = col.c.getText();
            if (col.t != null) {
                String typeName = col.t.getText();
                try {
                    type = CreateIndexType.valueOf(typeName.toUpperCase());
                }
                catch (RuntimeException ex) {
                    throw ASTWalkException.from("Invalid column index type '" + typeName + "' encountered, please use any of the following index type names " + Arrays.asList(CreateIndexType.values()));
                }
            }
            columns.add(new CreateIndexItem(columnName, type));
        }

        if (ctx.u != null) {
            String ident = ctx.u.getText();
            if (ident.toLowerCase().trim().equals("unique")) {
                unique = true;
            }
            else {
                throw ASTWalkException.from("Invalid keyword '" + ident + "' in create-index encountered, expected 'unique'");
            }
        }

        statementSpec.setCreateIndexDesc(new CreateIndexDesc(unique, indexName, windowName, columns));
    }

    public void exitAnnotationEnum(@NotNull EsperEPL2GrammarParser.AnnotationEnumContext ctx) {
        if (ctx.parent.getRuleIndex() != EsperEPL2GrammarParser.RULE_startEPLExpressionRule &&
                ctx.parent.getRuleIndex() != EsperEPL2GrammarParser.RULE_startPatternExpressionRule) {
            return;
        }

        statementSpec.getAnnotations().add(ASTAnnotationHelper.walk(ctx, this.engineImportService));
        astExprNodeMap.clear();
    }

    public void exitCreateContextExpr(@NotNull EsperEPL2GrammarParser.CreateContextExprContext ctx) {
        CreateContextDesc contextDesc = ASTContextHelper.walkCreateContext(ctx, astExprNodeMap, astPatternNodeMap, propertyEvalSpec, filterSpec);
        filterSpec = null;
        propertyEvalSpec = null;
        statementSpec.setCreateContextDesc(contextDesc);
    }

    public void exitLastOperand(@NotNull EsperEPL2GrammarParser.LastOperandContext ctx) {
        ExprNumberSetCronParam exprNode = new ExprNumberSetCronParam(CronOperatorEnum.LASTDAY);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(exprNode, ctx, astExprNodeMap);
    }

    public void exitCreateWindowExpr(@NotNull EsperEPL2GrammarParser.CreateWindowExprContext ctx) {
        String windowName = ctx.i.getText();

        String eventName = "java.lang.Object";
        if (ctx.createWindowExprModelAfter() != null) {
            eventName = ASTUtil.unescapeClassIdent(ctx.createWindowExprModelAfter().classIdentifier());
        }

        boolean isRetainUnion = ctx.ru != null;
        boolean isRetainIntersection = ctx.ri != null;
        StreamSpecOptions streamSpecOptions = new StreamSpecOptions(false,isRetainUnion,isRetainIntersection);

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

    public void exitCreateExpressionExpr(@NotNull EsperEPL2GrammarParser.CreateExpressionExprContext ctx) {
        Pair<ExpressionDeclItem, ExpressionScriptProvided> pair = ASTExpressionDeclHelper.walkExpressionDecl(ctx.expressionDecl(), scriptBodies, astExprNodeMap, tokenStream);
        statementSpec.setCreateExpressionDesc(new CreateExpressionDesc(pair));
    }

    public void exitRangeOperand(@NotNull EsperEPL2GrammarParser.RangeOperandContext ctx) {
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

    public void exitRowSubSelectExpression(@NotNull EsperEPL2GrammarParser.RowSubSelectExpressionContext ctx) {
        StatementSpecRaw statementSpec = astStatementSpecMap.remove(ctx.subQueryExpr());
        ExprSubselectRowNode subselectNode = new ExprSubselectRowNode(statementSpec);
        if (ctx.chainedFunction() != null) {
            handleChainedFunction(ctx, ctx.chainedFunction(), subselectNode);
        }
        else {
            ASTExprHelper.exprCollectAddSubNodesAddParentNode(subselectNode, ctx, astExprNodeMap);
        }
    }

    public void exitUnaryExpression(@NotNull EsperEPL2GrammarParser.UnaryExpressionContext ctx) {
        if (ctx.inner != null && ctx.chainedFunction() != null) {
            handleChainedFunction(ctx, ctx.chainedFunction(), null);
        }
        if (ctx.NEWKW() != null && ctx.newAssign() != null) {
            List<String> columnNames = new ArrayList<String>();
            List<ExprNode> expressions = new ArrayList<ExprNode>();
            List<EsperEPL2GrammarParser.NewAssignContext> assigns = ctx.newAssign();
            for (EsperEPL2GrammarParser.NewAssignContext assign : assigns) {
                String property = ASTUtil.getPropertyName(assign.eventProperty(), 0);
                columnNames.add(property);
                ExprNode expr;
                if (assign.expression() != null) {
                    expr = ASTExprHelper.exprCollectSubNodes(assign.expression(), 0, astExprNodeMap).get(0);
                }
                else {
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
                ExprDotNode dotNode = new ExprDotNode(chainSpec, engineImportService.isDuckType(), engineImportService.isUdfCache());
                dotNode.addChildNode(newNode);
                exprNode = dotNode;
            }
            else {
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
            }
            else {
                List<ExprChainedSpec> chainSpec = ASTLibFunctionHelper.getLibFuncChain(ctx.chainedFunction().libFunctionNoClass(), astExprNodeMap);
                Pair<ExprTableAccessNode, List<ExprChainedSpec>> pair = ASTTableExprHelper.getTableExprChainable(engineImportService, plugInAggregations, engineURI, tableName, chainSpec);
                tableNode = pair.getFirst();
                if (pair.getSecond().isEmpty()) {
                    exprNode = tableNode;
                }
                else {
                    exprNode = new ExprDotNode(pair.getSecond(), engineImportService.isDuckType(), engineImportService.isUdfCache());
                    exprNode.addChildNode(tableNode);
                }
            }
            ASTExprHelper.exprCollectAddSubNodesAddParentNode(tableNode, ctx, astExprNodeMap);
            astExprNodeMap.put(ctx, exprNode);
            ASTTableExprHelper.addTableExpressionReference(statementSpec, tableNode);
        }
    }

    public void enterOnSelectInsertExpr(@NotNull EsperEPL2GrammarParser.OnSelectInsertExprContext ctx) {
        pushStatementContext();
    }

    public void exitSelectClause(@NotNull EsperEPL2GrammarParser.SelectClauseContext ctx) {
        SelectClauseStreamSelectorEnum selector;
        if (ctx.s != null) {
            if (ctx.s.getType() == EsperEPL2GrammarLexer.RSTREAM) {
                selector = SelectClauseStreamSelectorEnum.RSTREAM_ONLY;
            }
            else if (ctx.s.getType() == EsperEPL2GrammarLexer.ISTREAM) {
                selector = SelectClauseStreamSelectorEnum.ISTREAM_ONLY;
            }
            else if (ctx.s.getType() == EsperEPL2GrammarLexer.IRSTREAM) {
                selector = SelectClauseStreamSelectorEnum.RSTREAM_ISTREAM_BOTH;
            }
            else {
                throw ASTWalkException.from("Encountered unrecognized token type " + ctx.s.getType(), tokenStream, ctx);
            }
            statementSpec.setSelectStreamDirEnum(selector);
        }
        statementSpec.getSelectClauseSpec().setDistinct(ctx.d != null);
    }

    public void exitConcatenationExpr(@NotNull EsperEPL2GrammarParser.ConcatenationExprContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        ExprConcatNode concatNode = new ExprConcatNode();
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(concatNode, ctx, astExprNodeMap);
    }

    public void exitSubSelectFilterExpr(@NotNull EsperEPL2GrammarParser.SubSelectFilterExprContext ctx) {
        String streamName = ctx.i != null ? ctx.i.getText() : null;
        boolean isRetainUnion = ctx.ru != null;
        boolean isRetainIntersection = ctx.ri != null;
        StreamSpecOptions options = new StreamSpecOptions(false, isRetainUnion, isRetainIntersection);
        StreamSpecRaw streamSpec = new FilterStreamSpecRaw(filterSpec, ViewSpec.toArray(viewSpecs), streamName, options);
        viewSpecs.clear();
        statementSpec.getStreamSpecs().add(streamSpec);
    }

    public void exitNegatedExpression(@NotNull EsperEPL2GrammarParser.NegatedExpressionContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        ExprNotNode notNode = new ExprNotNode();
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(notNode, ctx, astExprNodeMap);
    }

    public void exitAdditiveExpression(@NotNull EsperEPL2GrammarParser.AdditiveExpressionContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        ExprNode expr = ASTExprHelper.mathGetExpr(ctx, astExprNodeMap, configurationInformation);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(expr, ctx, astExprNodeMap);
    }

    public void exitMultiplyExpression(@NotNull EsperEPL2GrammarParser.MultiplyExpressionContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        ExprNode expr = ASTExprHelper.mathGetExpr(ctx, astExprNodeMap, configurationInformation);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(expr, ctx, astExprNodeMap);
    }

    public void exitEventProperty(@NotNull EsperEPL2GrammarParser.EventPropertyContext ctx) {
        if (EVENT_PROPERTY_WALK_EXCEPTIONS__PARENT.contains(ctx.getParent().getRuleIndex())) {
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
        if (ctx.eventPropertyAtomic().size() == 1 || PropertyParser.isNestedPropertyWithNonSimpleLead(ctx))
        {
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
                    }
                    else if (ctx.eventPropertyAtomic().size() == 2) {
                        String column = ctx.eventPropertyAtomic().get(1).getText();
                        tableNode = new ExprTableAccessNodeSubprop(nameText, column);
                    }
                    else {
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
        }
        // --> this is more then one child node, and the first child node is a simple property
        // we may have a stream name in the first simple property, or a nested property
        // i.e. 's0.p0' could mean that the event has a nested property to 's0' of name 'p0', or 's0' is the stream name
        else
        {
            String leadingIdentifier = ctx.getChild(0).getChild(0).getText();
            String streamOrNestedPropertyName = ASTUtil.escapeDot(leadingIdentifier);
            propertyName = ASTUtil.getPropertyName(ctx, 2);

            Pair<ExprTableAccessNode, ExprDotNode> tableNode = ASTTableExprHelper.checkTableNameGetExprForSubproperty(tableService, streamOrNestedPropertyName, propertyName);
            VariableMetaData variableMetaData = variableService.getVariableMetaData(leadingIdentifier);
            if (tableNode != null) {
                if (tableNode.getSecond() != null) {
                    exprNode = tableNode.getSecond();
                }
                else {
                    exprNode = tableNode.getFirst();
                }
                ASTTableExprHelper.addTableExpressionReference(statementSpec, tableNode.getFirst());
            }
            else if (variableMetaData != null)
            {
                exprNode = new ExprVariableNodeImpl(variableMetaData, propertyName);
                statementSpec.setHasVariables(true);
                String message = VariableServiceUtil.checkVariableContextName(statementSpec.getOptionalContextName(), variableMetaData);
                if (message != null) {
                    throw ASTWalkException.from(message);
                }
                ASTExprHelper.addVariableReference(statementSpec, variableMetaData.getVariableName());
            }
            else if (contextDescriptor != null && contextDescriptor.getContextPropertyRegistry().isContextPropertyPrefix(streamOrNestedPropertyName)) {
                exprNode = new ExprContextPropertyNode(propertyName);
            }
            else {
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

    public void exitOuterJoin(@NotNull EsperEPL2GrammarParser.OuterJoinContext ctx) {
        OuterJoinType joinType;
        if (ctx.i != null) {
            joinType = OuterJoinType.INNER;
        }
        else if (ctx.tr != null) {
            joinType = OuterJoinType.RIGHT;
        }
        else if (ctx.tl != null) {
            joinType = OuterJoinType.LEFT;
        }
        else if (ctx.tf != null) {
            joinType = OuterJoinType.FULL;
        }
        else {
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
                ArrayList<ExprIdentNode> addLeft = new ArrayList<ExprIdentNode>(pairs.size() - 1);
                ArrayList<ExprIdentNode> addRight = new ArrayList<ExprIdentNode>(pairs.size() - 1);
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

    public void exitOnExpr(@NotNull EsperEPL2GrammarParser.OnExprContext ctx) {
        if (ctx.onMergeExpr() != null) {
            String windowName = ctx.onMergeExpr().n.getText();
            String asName = ctx.onMergeExpr().i != null ? ctx.onMergeExpr().i.getText() : null;
            OnTriggerMergeDesc desc = new OnTriggerMergeDesc(windowName, asName, mergeMatcheds == null ? Collections.<OnTriggerMergeMatched>emptyList() : mergeMatcheds);
            statementSpec.setOnTriggerDesc(desc);
        }
        else if (ctx.onSetExpr() == null)
        {
            UniformPair<String> windowName = getOnExprWindowName(ctx);
            boolean deleteAndSelect = ctx.onSelectExpr() != null && ctx.onSelectExpr().d != null;
            if (windowName == null) {
                // on the statement spec, the deepest spec is the outermost
                List<OnTriggerSplitStream> splitStreams = new ArrayList<OnTriggerSplitStream>();
                for (int i = 1; i <= statementSpecStack.size() - 1; i++) {
                    StatementSpecRaw raw = statementSpecStack.get(i);
                    splitStreams.add(new OnTriggerSplitStream(raw.getInsertIntoDesc(), raw.getSelectClauseSpec(), raw.getFilterExprRootNode()));
                }
                splitStreams.add(new OnTriggerSplitStream(statementSpec.getInsertIntoDesc(), statementSpec.getSelectClauseSpec(), statementSpec.getFilterExprRootNode()));
                if (!statementSpecStack.isEmpty()) {
                    statementSpec = statementSpecStack.get(0);
                }
                boolean isFirst = ctx.outputClauseInsert() == null || ctx.outputClauseInsert().ALL() == null;
                statementSpec.setOnTriggerDesc(new OnTriggerSplitStreamDesc(OnTriggerType.ON_SPLITSTREAM, isFirst, splitStreams));
                statementSpecStack.clear();
            }
            else if (ctx.onUpdateExpr() != null) {
                List<OnTriggerSetAssignment> assignments = ASTExprHelper.getOnTriggerSetAssignments(ctx.onUpdateExpr().onSetAssignmentList(), astExprNodeMap);
                statementSpec.setOnTriggerDesc(new OnTriggerWindowUpdateDesc(windowName.getFirst(), windowName.getSecond(), assignments));
                if (ctx.onUpdateExpr().whereClause() != null) {
                    statementSpec.setFilterExprRootNode(ASTExprHelper.exprCollectSubNodes(ctx.onUpdateExpr().whereClause(), 0, astExprNodeMap).get(0));
                }
            }
            else
            {
                statementSpec.setOnTriggerDesc(new OnTriggerWindowDesc(windowName.getFirst(), windowName.getSecond(), ctx.onDeleteExpr() != null ? OnTriggerType.ON_DELETE : OnTriggerType.ON_SELECT, deleteAndSelect));
            }
        }
        else
        {
            List<OnTriggerSetAssignment> assignments = ASTExprHelper.getOnTriggerSetAssignments(ctx.onSetExpr().onSetAssignmentList(), astExprNodeMap);
            statementSpec.setOnTriggerDesc(new OnTriggerSetDesc(assignments));
        }
    }

    public void exitMatchRecogPatternAlteration(@NotNull EsperEPL2GrammarParser.MatchRecogPatternAlterationContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        RowRegexExprNodeAlteration alterNode = new RowRegexExprNodeAlteration();
        ASTExprHelper.regExCollectAddSubNodesAddParentNode(alterNode, ctx, astRowRegexNodeMap);
    }

    public void exitCaseExpression(@NotNull EsperEPL2GrammarParser.CaseExpressionContext ctx) {
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

    public void exitRowLimit(@NotNull EsperEPL2GrammarParser.RowLimitContext ctx) {
        RowLimitSpec spec = ASTOutputLimitHelper.buildRowLimitSpec(ctx);
        statementSpec.setRowLimitSpec(spec);

        if ((spec.getNumRowsVariable() != null) || (spec.getOptionalOffsetVariable() != null)) {
            statementSpec.setHasVariables(true);
            ASTExprHelper.addVariableReference(statementSpec, spec.getOptionalOffsetVariable());
        }
        astExprNodeMap.clear();
    }

    public void exitOrderByListElement(@NotNull EsperEPL2GrammarParser.OrderByListElementContext ctx) {
        ExprNode exprNode = ASTExprHelper.exprCollectSubNodes(ctx, 0, astExprNodeMap).get(0);
        astExprNodeMap.clear();
        boolean descending = ctx.d != null;
        statementSpec.getOrderByList().add(new OrderByItem(exprNode, descending));
    }

    public void exitMergeUnmatched(@NotNull EsperEPL2GrammarParser.MergeUnmatchedContext ctx) {
        handleMergeMatchedUnmatched(ctx.expression(), false);
    }

    public void exitExistsSubSelectExpression(@NotNull EsperEPL2GrammarParser.ExistsSubSelectExpressionContext ctx) {
        StatementSpecRaw currentSpec = astStatementSpecMap.remove(ctx.subQueryExpr());
        ExprSubselectNode subselectNode = new ExprSubselectExistsNode(currentSpec);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(subselectNode, ctx, astExprNodeMap);
    }

    public void exitArrayExpression(@NotNull EsperEPL2GrammarParser.ArrayExpressionContext ctx) {
        ExprArrayNode arrayNode = new ExprArrayNode();
        if (ctx.chainedFunction() != null) {
            ASTExprHelper.exprCollectAddSubNodesExpressionCtx(arrayNode, ctx.expression(), astExprNodeMap);
            handleChainedFunction(ctx, ctx.chainedFunction(), arrayNode);
        }
        else {
            ASTExprHelper.exprCollectAddSubNodesAddParentNode(arrayNode, ctx, astExprNodeMap);
        }
    }

    public void visitTerminal(@NotNull TerminalNode terminalNode) {
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

    public void exitAndExpression(@NotNull EsperEPL2GrammarParser.AndExpressionContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        EvalFactoryNode andNode = patternNodeFactory.makeAndNode();
        ASTExprHelper.patternCollectAddSubnodesAddParentNode(andNode, ctx, astPatternNodeMap);
    }

    public void exitFollowedByExpression(@NotNull EsperEPL2GrammarParser.FollowedByExpressionContext ctx) {
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

    public void exitOrExpression(@NotNull EsperEPL2GrammarParser.OrExpressionContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        EvalFactoryNode orNode = patternNodeFactory.makeOrNode();
        ASTExprHelper.patternCollectAddSubnodesAddParentNode(orNode, ctx, astPatternNodeMap);
    }

    public void exitQualifyExpression(@NotNull EsperEPL2GrammarParser.QualifyExpressionContext ctx) {
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
        }
        else if (ctx.n != null) {
            theNode = this.patternNodeFactory.makeNotNode();
        }
        else if (ctx.d != null) {
            List<ExprNode> exprNodes = ASTExprHelper.exprCollectSubNodes(ctx.distinctExpressionList(), 0, astExprNodeMap);
            theNode = this.patternNodeFactory.makeEveryDistinctNode(exprNodes);
        }
        else {
            throw ASTWalkException.from("Failed to recognize node");
        }
        ASTExprHelper.patternCollectAddSubnodesAddParentNode(theNode, ctx, astPatternNodeMap);
    }

    public void exitMatchUntilExpression(@NotNull EsperEPL2GrammarParser.MatchUntilExpressionContext ctx) {
        if (ctx.getChildCount() < 2) {
            return;
        }
        EvalFactoryNode node;
        if (ctx.matchUntilRange() != null) {
            node = makeMatchUntil(ctx.matchUntilRange(), ctx.until != null);
        }
        else {
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
        }
        else if (range.c2 != null && range.upper != null) { // [:expr]
            high = ASTExprHelper.exprCollectSubNodes(range.upper, 0, astExprNodeMap).get(0);
        }
        else if (range.low != null && range.c1 == null) { // [expr]
            single = ASTExprHelper.exprCollectSubNodes(range.low, 0, astExprNodeMap).get(0);
            hasRange = false;
        }
        else if (range.low != null && range.c1 != null && range.high != null) { // [expr:expr]
            low = ASTExprHelper.exprCollectSubNodes(range.low, 0, astExprNodeMap).get(0);
            high = ASTExprHelper.exprCollectSubNodes(range.high, 0, astExprNodeMap).get(0);
            allowZeroLowerBounds = true;
        }

        boolean tightlyBound;
        if (single != null) {
            ASTMatchUntilHelper.validate(single, single, allowZeroLowerBounds);
            tightlyBound = true;
        }
        else {
            tightlyBound = ASTMatchUntilHelper.validate(low, high, allowZeroLowerBounds);
        }
        if (hasRange && !tightlyBound && !hasUntil) {
            throw ASTWalkException.from("Variable bounds repeat operator requires an until-expression");
        }
        return this.patternNodeFactory.makeMatchUntilNode(low, high, single);
    }

    public void exitGuardPostFix(@NotNull EsperEPL2GrammarParser.GuardPostFixContext ctx) {
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
        }
        else {
            objectNamespace = GuardEnum.WHILE_GUARD.getNamespace();
            objectName = GuardEnum.WHILE_GUARD.getName();
            obsParameters = ASTExprHelper.exprCollectSubNodes(ctx.guardWhileExpression(), 1, astExprNodeMap);
        }

        PatternGuardSpec guardSpec = new PatternGuardSpec(objectNamespace, objectName, obsParameters);
        EvalFactoryNode guardNode = patternNodeFactory.makeGuardNode(guardSpec);
        ASTExprHelper.patternCollectAddSubnodesAddParentNode(guardNode, ctx, astPatternNodeMap);
    }

    public void exitBuiltin_coalesce(@NotNull EsperEPL2GrammarParser.Builtin_coalesceContext ctx) {
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(new ExprCoalesceNode(), ctx, astExprNodeMap);
    }

    public void exitBuiltin_typeof(@NotNull EsperEPL2GrammarParser.Builtin_typeofContext ctx) {
        ExprTypeofNode typeofNode = new ExprTypeofNode();
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(typeofNode, ctx, astExprNodeMap);
    }

    public void exitBuiltin_avedev(@NotNull EsperEPL2GrammarParser.Builtin_avedevContext ctx) {
        ExprAggregateNode aggregateNode = new ExprAvedevNode(ctx.DISTINCT() != null);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(aggregateNode, ctx, astExprNodeMap);
    }

    public void exitBuiltin_prevcount(@NotNull EsperEPL2GrammarParser.Builtin_prevcountContext ctx) {
        ExprPreviousNode previousNode = new ExprPreviousNode(ExprPreviousNodePreviousType.PREVCOUNT);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(previousNode, ctx, astExprNodeMap);
    }

    public void exitBuiltin_stddev(@NotNull EsperEPL2GrammarParser.Builtin_stddevContext ctx) {
        ExprAggregateNode aggregateNode = new ExprStddevNode(ctx.DISTINCT() != null);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(aggregateNode, ctx, astExprNodeMap);
    }

    public void exitBuiltin_sum(@NotNull EsperEPL2GrammarParser.Builtin_sumContext ctx) {
        ExprAggregateNode aggregateNode = new ExprSumNode(ctx.DISTINCT() != null);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(aggregateNode, ctx, astExprNodeMap);
    }

    public void exitBuiltin_exists(@NotNull EsperEPL2GrammarParser.Builtin_existsContext ctx) {
        ExprPropertyExistsNode existsNode = new ExprPropertyExistsNode();
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(existsNode, ctx, astExprNodeMap);
    }

    public void exitBuiltin_prior(@NotNull EsperEPL2GrammarParser.Builtin_priorContext ctx) {
        ExprPriorNode priorNode = new ExprPriorNode();
        ExprConstantNode number = new ExprConstantNodeImpl(ASTConstantHelper.parse(ctx.number()));
        priorNode.addChildNode(number);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(priorNode, ctx, astExprNodeMap);
    }

    public void exitBuiltin_instanceof(@NotNull EsperEPL2GrammarParser.Builtin_instanceofContext ctx) {
        // get class identifiers
        List<String> classes = new ArrayList<String>();
        List<EsperEPL2GrammarParser.ClassIdentifierContext> classCtxs = ctx.classIdentifier();
        for (EsperEPL2GrammarParser.ClassIdentifierContext classCtx : classCtxs) {
            classes.add(ASTUtil.unescapeClassIdent(classCtx));
        }

        String idents[] = classes.toArray(new String[classes.size()]);
        ExprInstanceofNode instanceofNode = new ExprInstanceofNode(idents);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(instanceofNode, ctx, astExprNodeMap);
    }

    public void exitBuiltin_currts(@NotNull EsperEPL2GrammarParser.Builtin_currtsContext ctx) {
        ExprTimestampNode timeNode = new ExprTimestampNode();
        if (ctx.chainedFunction() != null) {
            handleChainedFunction(ctx, ctx.chainedFunction(), timeNode);
        }
        else {
            astExprNodeMap.put(ctx, timeNode);
        }
    }

    public void exitBuiltin_median(@NotNull EsperEPL2GrammarParser.Builtin_medianContext ctx) {
        ExprAggregateNode aggregateNode = new ExprMedianNode(ctx.DISTINCT() != null);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(aggregateNode, ctx, astExprNodeMap);
    }

    public void exitBuiltin_firstlastwindow(@NotNull EsperEPL2GrammarParser.Builtin_firstlastwindowContext ctx) {
        AggregationStateType stateType = AggregationStateType.fromString(ctx.firstLastWindowAggregation().q.getText());
        ExprNode expr = new ExprAggMultiFunctionLinearAccessNode(stateType);
        ASTExprHelper.exprCollectAddSubNodes(expr, ctx.firstLastWindowAggregation().expressionListWithNamed(), astExprNodeMap);
        if (ctx.firstLastWindowAggregation().chainedFunction() != null) {
            handleChainedFunction(ctx, ctx.firstLastWindowAggregation().chainedFunction(), expr);
        }
        else {
            astExprNodeMap.put(ctx, expr);
        }
    }

    public void exitBuiltin_avg(@NotNull EsperEPL2GrammarParser.Builtin_avgContext ctx) {
        ExprAggregateNode aggregateNode = new ExprAvgNode(ctx.DISTINCT() != null);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(aggregateNode, ctx, astExprNodeMap);
    }

    public void exitBuiltin_cast(@NotNull EsperEPL2GrammarParser.Builtin_castContext ctx) {
        String classIdent = ASTUtil.unescapeClassIdent(ctx.classIdentifier());
        ExprCastNode castNode = new ExprCastNode(classIdent);
        if (ctx.chainedFunction() != null) {
            ASTExprHelper.exprCollectAddSubNodes(castNode, ctx.expression(), astExprNodeMap);
            ASTExprHelper.exprCollectAddSingle(castNode, ctx.expressionNamedParameter(), astExprNodeMap);
            handleChainedFunction(ctx, ctx.chainedFunction(), castNode);
        }
        else {
            ASTExprHelper.exprCollectAddSubNodesAddParentNode(castNode, ctx, astExprNodeMap);
        }
    }

    public void exitBuiltin_cnt(@NotNull EsperEPL2GrammarParser.Builtin_cntContext ctx) {
        ExprAggregateNode aggregateNode = new ExprCountNode(ctx.DISTINCT() != null);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(aggregateNode, ctx, astExprNodeMap);
    }

    public void exitBuiltin_prev(@NotNull EsperEPL2GrammarParser.Builtin_prevContext ctx) {
        ExprPreviousNode previousNode = new ExprPreviousNode(ExprPreviousNodePreviousType.PREV);
        if (ctx.chainedFunction() != null) {
            ASTExprHelper.exprCollectAddSubNodesExpressionCtx(previousNode, ctx.expression(), astExprNodeMap);
            handleChainedFunction(ctx, ctx.chainedFunction(), previousNode);
        }
        else {
            ASTExprHelper.exprCollectAddSubNodesAddParentNode(previousNode, ctx, astExprNodeMap);
        }
    }

    public void exitBuiltin_istream(@NotNull EsperEPL2GrammarParser.Builtin_istreamContext ctx) {
        ExprIStreamNode istreamNode = new ExprIStreamNode();
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(istreamNode, ctx, astExprNodeMap);
    }

    public void exitBuiltin_prevwindow(@NotNull EsperEPL2GrammarParser.Builtin_prevwindowContext ctx) {
        ExprPreviousNode previousNode = new ExprPreviousNode(ExprPreviousNodePreviousType.PREVWINDOW);
        if (ctx.chainedFunction() != null) {
            ASTExprHelper.exprCollectAddSubNodes(previousNode, ctx.expression(), astExprNodeMap);
            handleChainedFunction(ctx, ctx.chainedFunction(), previousNode);
        }
        else {
            ASTExprHelper.exprCollectAddSubNodesAddParentNode(previousNode, ctx, astExprNodeMap);
        }
    }

    public void exitBuiltin_prevtail(@NotNull EsperEPL2GrammarParser.Builtin_prevtailContext ctx) {
        ExprPreviousNode previousNode = new ExprPreviousNode(ExprPreviousNodePreviousType.PREVTAIL);
        if (ctx.chainedFunction() != null) {
            ASTExprHelper.exprCollectAddSubNodesExpressionCtx(previousNode, ctx.expression(), astExprNodeMap);
            handleChainedFunction(ctx, ctx.chainedFunction(), previousNode);
        }
        else {
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
            Token alias = ctx.onUpdateExpr().i;
            return new UniformPair<String>(ctx.onUpdateExpr().n.getText(), alias != null ? alias.getText() : null);
        }
        return null;
    }

    private UniformPair<String> getOnExprWindowName(EsperEPL2GrammarParser.OnExprFromContext ctx) {
        String windowName = ctx.n.getText();
        String windowStreamName = ctx.i != null ? ctx.i.getText() : null;
        return new UniformPair<String>(windowName, windowStreamName);
    }

    private String getDefaultDialect() {
        return configurationInformation.getEngineDefaults().getScripts().getDefaultDialect();
    }

    private void handleMergeMatchedUnmatched(EsperEPL2GrammarParser.ExpressionContext expression, boolean b) {
        if (mergeMatcheds == null) {
            mergeMatcheds = new ArrayList<OnTriggerMergeMatched>();
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
        List<SelectClauseElementRaw> expressions = new ArrayList<SelectClauseElementRaw>(statementSpec.getSelectClauseSpec().getSelectExprList());
        statementSpec.getSelectClauseSpec().getSelectExprList().clear();

        String optionalInsertName = mergeInsertContext.classIdentifier() != null ? ASTUtil.unescapeClassIdent(mergeInsertContext.classIdentifier()) : null;
        List<String> columsList = ASTUtil.getIdentList(mergeInsertContext.columnList());
        mergeActions.add(new OnTriggerMergeActionInsert(whereCond, optionalInsertName, columsList, expressions));
    }

    private void handleChainedFunction(ParserRuleContext parentCtx, EsperEPL2GrammarParser.ChainedFunctionContext chainedCtx, ExprNode childExpression) {
        List<ExprChainedSpec> chainSpec = ASTLibFunctionHelper.getLibFuncChain(chainedCtx.libFunctionNoClass(), astExprNodeMap);
        ExprDotNode dotNode = new ExprDotNode(chainSpec, configurationInformation.getEngineDefaults().getExpression().isDuckTyping(),
                configurationInformation.getEngineDefaults().getExpression().isUdfCache());
        if (childExpression != null) {
            dotNode.addChildNode(childExpression);
        }
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(dotNode, parentCtx, astExprNodeMap);
    }

    private void handleFAFNamedWindowStream(EsperEPL2GrammarParser.ClassIdentifierContext node, Token i) {
        String windowName = ASTUtil.unescapeClassIdent(node);
        String alias = i != null ? i.getText() : null;
        statementSpec.getStreamSpecs().add(new FilterStreamSpecRaw(new FilterSpecRaw(windowName, Collections.<ExprNode>emptyList(), null), ViewSpec.toArray(viewSpecs), alias, new StreamSpecOptions()));
    }

    public void exitFafInsert(@NotNull EsperEPL2GrammarParser.FafInsertContext ctx) {
        List<EsperEPL2GrammarParser.ExpressionContext> valueExprs = ctx.expressionList().expression();
        for (EsperEPL2GrammarParser.ExpressionContext valueExpr : valueExprs) {
            ExprNode expr = ASTExprHelper.exprCollectSubNodes(valueExpr, 0, astExprNodeMap).get(0);
            statementSpec.getSelectClauseSpec().add(new SelectClauseExprRawSpec(expr, null, false));
        }
        statementSpec.setFireAndForgetSpec(new FireAndForgetSpecInsert(true));
    }

    protected void end() throws ASTWalkException {
        if (astExprNodeMap.size() > 1)
        {
            throw ASTWalkException.from("Unexpected AST tree contains left over child elements," +
                    " not all expression nodes have been removed from AST-to-expression nodes map");
        }
        if (astPatternNodeMap.size() > 1)
        {
            throw ASTWalkException.from("Unexpected AST tree contains left over child elements," +
                    " not all pattern nodes have been removed from AST-to-pattern nodes map");
        }

        // detect insert-into fire-and-forget query
        if (statementSpec.getInsertIntoDesc() != null && statementSpec.getStreamSpecs().isEmpty() && statementSpec.getFireAndForgetSpec() == null) {
            statementSpec.setFireAndForgetSpec(new FireAndForgetSpecInsert(false));
        }

        statementSpec.setSubstitutionParameters(substitutionParamNodes);
    }

    public void exitBuiltin_grouping(@NotNull EsperEPL2GrammarParser.Builtin_groupingContext ctx) {
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(new ExprGroupingNode(), ctx, astExprNodeMap);
    }


    public void exitBuiltin_groupingid(@NotNull EsperEPL2GrammarParser.Builtin_groupingidContext ctx) {
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(new ExprGroupingIdNode(), ctx, astExprNodeMap);
    }

    public void exitIntoTableExpr(@NotNull EsperEPL2GrammarParser.IntoTableExprContext ctx) {
        String name = ctx.i.getText();
        statementSpec.setIntoTableSpec(new IntoTableSpec(name));
    }

    public void exitCreateTableExpr(@NotNull EsperEPL2GrammarParser.CreateTableExprContext ctx) {
        String tableName = ctx.n.getText();

        // obtain item declarations
        List<CreateTableColumn> cols = ASTTableHelper.getColumns(ctx.createTableColumnList().createTableColumn(), astExprNodeMap, engineImportService);
        statementSpec.setCreateTableDesc(new CreateTableDesc(tableName, cols));
    }

    public void exitJsonobject(@NotNull EsperEPL2GrammarParser.JsonobjectContext ctx) {
        ExprConstantNodeImpl node = new ExprConstantNodeImpl(ASTJsonHelper.walkObject(tokenStream, ctx));
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(node, ctx, astExprNodeMap);
    }

    public void exitPropertyStreamSelector(@NotNull EsperEPL2GrammarParser.PropertyStreamSelectorContext ctx) {
        String streamWildcard = ctx.s.getText();
        ExprStreamUnderlyingNodeImpl node = new ExprStreamUnderlyingNodeImpl(streamWildcard, true);
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(node, ctx, astExprNodeMap);
    }

    public void exitExpressionNamedParameter(@NotNull EsperEPL2GrammarParser.ExpressionNamedParameterContext ctx) {
        ExprNamedParameterNodeImpl named = new ExprNamedParameterNodeImpl(ctx.IDENT().getText());
        ASTExprHelper.exprCollectAddSubNodesAddParentNode(named, ctx, astExprNodeMap);
    }

    public void exitExpressionNamedParameterWithTime(@NotNull EsperEPL2GrammarParser.ExpressionNamedParameterWithTimeContext ctx) {
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

    public void enterContextExpr(@NotNull EsperEPL2GrammarParser.ContextExprContext ctx) {}
    public void enterExpressionList(@NotNull EsperEPL2GrammarParser.ExpressionListContext ctx) {}
    public void exitExpressionList(@NotNull EsperEPL2GrammarParser.ExpressionListContext ctx) {}
    public void enterSelectionList(@NotNull EsperEPL2GrammarParser.SelectionListContext ctx) {}
    public void exitSelectionList(@NotNull EsperEPL2GrammarParser.SelectionListContext ctx) {}
    public void enterEvalRelationalExpression(@NotNull EsperEPL2GrammarParser.EvalRelationalExpressionContext ctx) {}
    public void enterPatternInclusionExpression(@NotNull EsperEPL2GrammarParser.PatternInclusionExpressionContext ctx) {}
    public void exitPatternInclusionExpression(@NotNull EsperEPL2GrammarParser.PatternInclusionExpressionContext ctx) {}
    public void enterLibFunction(@NotNull EsperEPL2GrammarParser.LibFunctionContext ctx) {}
    public void enterSelectionListElement(@NotNull EsperEPL2GrammarParser.SelectionListElementContext ctx) {}
    public void exitSelectionListElement(@NotNull EsperEPL2GrammarParser.SelectionListElementContext ctx) {}
    public void enterGopOutTypeList(@NotNull EsperEPL2GrammarParser.GopOutTypeListContext ctx) {}
    public void exitGopOutTypeList(@NotNull EsperEPL2GrammarParser.GopOutTypeListContext ctx) {}
    public void enterGopOutTypeItem(@NotNull EsperEPL2GrammarParser.GopOutTypeItemContext ctx) {}
    public void exitGopOutTypeItem(@NotNull EsperEPL2GrammarParser.GopOutTypeItemContext ctx) {}
    public void enterMatchRecog(@NotNull EsperEPL2GrammarParser.MatchRecogContext ctx) {}
    public void enterJsonmembers(@NotNull EsperEPL2GrammarParser.JsonmembersContext ctx) {}
    public void exitJsonmembers(@NotNull EsperEPL2GrammarParser.JsonmembersContext ctx) {}
    public void enterNumber(@NotNull EsperEPL2GrammarParser.NumberContext ctx) {}
    public void exitNumber(@NotNull EsperEPL2GrammarParser.NumberContext ctx) {}
    public void enterVariantList(@NotNull EsperEPL2GrammarParser.VariantListContext ctx) {}
    public void exitVariantList(@NotNull EsperEPL2GrammarParser.VariantListContext ctx) {}
    public void enterMatchRecogPartitionBy(@NotNull EsperEPL2GrammarParser.MatchRecogPartitionByContext ctx) {}
    public void enterOutputLimitAfter(@NotNull EsperEPL2GrammarParser.OutputLimitAfterContext ctx) {}
    public void exitOutputLimitAfter(@NotNull EsperEPL2GrammarParser.OutputLimitAfterContext ctx) {}
    public void enterCreateColumnList(@NotNull EsperEPL2GrammarParser.CreateColumnListContext ctx) {}
    public void exitCreateColumnList(@NotNull EsperEPL2GrammarParser.CreateColumnListContext ctx) {}
    public void enterMergeMatchedItem(@NotNull EsperEPL2GrammarParser.MergeMatchedItemContext ctx) {}
    public void enterMatchRecogMatchesSelection(@NotNull EsperEPL2GrammarParser.MatchRecogMatchesSelectionContext ctx) {}
    public void exitMatchRecogMatchesSelection(@NotNull EsperEPL2GrammarParser.MatchRecogMatchesSelectionContext ctx) {}
    public void enterClassIdentifier(@NotNull EsperEPL2GrammarParser.ClassIdentifierContext ctx) {}
    public void exitClassIdentifier(@NotNull EsperEPL2GrammarParser.ClassIdentifierContext ctx) {}
    public void enterDatabaseJoinExpression(@NotNull EsperEPL2GrammarParser.DatabaseJoinExpressionContext ctx) {}
    public void exitDatabaseJoinExpression(@NotNull EsperEPL2GrammarParser.DatabaseJoinExpressionContext ctx) {}
    public void enterMatchRecogDefineItem(@NotNull EsperEPL2GrammarParser.MatchRecogDefineItemContext ctx) {}
    public void enterLibFunctionArgs(@NotNull EsperEPL2GrammarParser.LibFunctionArgsContext ctx) {}
    public void exitLibFunctionArgs(@NotNull EsperEPL2GrammarParser.LibFunctionArgsContext ctx) {}
    public void enterMergeUnmatchedItem(@NotNull EsperEPL2GrammarParser.MergeUnmatchedItemContext ctx) {}
    public void enterHavingClause(@NotNull EsperEPL2GrammarParser.HavingClauseContext ctx) {}
    public void enterMatchRecogMeasureItem(@NotNull EsperEPL2GrammarParser.MatchRecogMeasureItemContext ctx) {}
    public void enterMatchRecogMatchesInterval(@NotNull EsperEPL2GrammarParser.MatchRecogMatchesIntervalContext ctx) {}
    public void exitMatchRecogMatchesInterval(@NotNull EsperEPL2GrammarParser.MatchRecogMatchesIntervalContext ctx) {}
    public void enterObserverExpression(@NotNull EsperEPL2GrammarParser.ObserverExpressionContext ctx) {}
    public void enterMatchRecogPatternNested(@NotNull EsperEPL2GrammarParser.MatchRecogPatternNestedContext ctx) {}
    public void enterCreateContextFilter(@NotNull EsperEPL2GrammarParser.CreateContextFilterContext ctx) {}
    public void exitCreateContextFilter(@NotNull EsperEPL2GrammarParser.CreateContextFilterContext ctx) {}
    public void enterEvalOrExpression(@NotNull EsperEPL2GrammarParser.EvalOrExpressionContext ctx) {}
    public void enterExpressionDef(@NotNull EsperEPL2GrammarParser.ExpressionDefContext ctx) {}
    public void exitExpressionDef(@NotNull EsperEPL2GrammarParser.ExpressionDefContext ctx) {}
    public void enterOutputLimitAndTerm(@NotNull EsperEPL2GrammarParser.OutputLimitAndTermContext ctx) {}
    public void exitOutputLimitAndTerm(@NotNull EsperEPL2GrammarParser.OutputLimitAndTermContext ctx) {}
    public void enterNumericListParameter(@NotNull EsperEPL2GrammarParser.NumericListParameterContext ctx) {}
    public void exitNumericListParameter(@NotNull EsperEPL2GrammarParser.NumericListParameterContext ctx) {}
    public void enterTimePeriod(@NotNull EsperEPL2GrammarParser.TimePeriodContext ctx) {}
    public void enterEventPropertyAtomic(@NotNull EsperEPL2GrammarParser.EventPropertyAtomicContext ctx) {}
    public void exitEventPropertyAtomic(@NotNull EsperEPL2GrammarParser.EventPropertyAtomicContext ctx) {}
    public void enterSubSelectGroupExpression(@NotNull EsperEPL2GrammarParser.SubSelectGroupExpressionContext ctx) {}
    public void exitSubSelectGroupExpression(@NotNull EsperEPL2GrammarParser.SubSelectGroupExpressionContext ctx) {}
    public void enterOuterJoinList(@NotNull EsperEPL2GrammarParser.OuterJoinListContext ctx) {}
    public void exitOuterJoinList(@NotNull EsperEPL2GrammarParser.OuterJoinListContext ctx) {}
    public void enterSelectionListElementExpr(@NotNull EsperEPL2GrammarParser.SelectionListElementExprContext ctx) {}
    public void enterEventFilterExpression(@NotNull EsperEPL2GrammarParser.EventFilterExpressionContext ctx) {}
    public void enterGopParamsItemList(@NotNull EsperEPL2GrammarParser.GopParamsItemListContext ctx) {}
    public void exitGopParamsItemList(@NotNull EsperEPL2GrammarParser.GopParamsItemListContext ctx) {}
    public void enterMatchRecogPatternConcat(@NotNull EsperEPL2GrammarParser.MatchRecogPatternConcatContext ctx) {}
    public void enterNumberconstant(@NotNull EsperEPL2GrammarParser.NumberconstantContext ctx) {}
    public void enterOnSetAssignment(@NotNull EsperEPL2GrammarParser.OnSetAssignmentContext ctx) {}
    public void exitOnSetAssignment(@NotNull EsperEPL2GrammarParser.OnSetAssignmentContext ctx) {}
    public void enterContextContextNested(@NotNull EsperEPL2GrammarParser.ContextContextNestedContext ctx) {}
    public void exitContextContextNested(@NotNull EsperEPL2GrammarParser.ContextContextNestedContext ctx) {}
    public void enterExpressionWithTime(@NotNull EsperEPL2GrammarParser.ExpressionWithTimeContext ctx) {}
    public void exitExpressionWithTime(@NotNull EsperEPL2GrammarParser.ExpressionWithTimeContext ctx) {}
    public void enterMatchRecogPattern(@NotNull EsperEPL2GrammarParser.MatchRecogPatternContext ctx) {}
    public void enterMergeInsert(@NotNull EsperEPL2GrammarParser.MergeInsertContext ctx) {}
    public void exitMergeInsert(@NotNull EsperEPL2GrammarParser.MergeInsertContext ctx) {}
    public void enterOrderByListExpr(@NotNull EsperEPL2GrammarParser.OrderByListExprContext ctx) {}
    public void exitOrderByListExpr(@NotNull EsperEPL2GrammarParser.OrderByListExprContext ctx) {}
    public void enterElementValuePairsEnum(@NotNull EsperEPL2GrammarParser.ElementValuePairsEnumContext ctx) {}
    public void exitElementValuePairsEnum(@NotNull EsperEPL2GrammarParser.ElementValuePairsEnumContext ctx) {}
    public void enterDistinctExpressionAtom(@NotNull EsperEPL2GrammarParser.DistinctExpressionAtomContext ctx) {}
    public void exitDistinctExpressionAtom(@NotNull EsperEPL2GrammarParser.DistinctExpressionAtomContext ctx) {}
    public void enterExpression(@NotNull EsperEPL2GrammarParser.ExpressionContext ctx) {}
    public void exitExpression(@NotNull EsperEPL2GrammarParser.ExpressionContext ctx) {}
    public void enterWhereClause(@NotNull EsperEPL2GrammarParser.WhereClauseContext ctx) {}
    public void enterCreateColumnListElement(@NotNull EsperEPL2GrammarParser.CreateColumnListElementContext ctx) {}
    public void exitCreateColumnListElement(@NotNull EsperEPL2GrammarParser.CreateColumnListElementContext ctx) {}
    public void enterGopList(@NotNull EsperEPL2GrammarParser.GopListContext ctx) {}
    public void exitGopList(@NotNull EsperEPL2GrammarParser.GopListContext ctx) {}
    public void enterPatternFilterAnnotation(@NotNull EsperEPL2GrammarParser.PatternFilterAnnotationContext ctx) {}
    public void exitPatternFilterAnnotation(@NotNull EsperEPL2GrammarParser.PatternFilterAnnotationContext ctx) {}
    public void enterElementValueArrayEnum(@NotNull EsperEPL2GrammarParser.ElementValueArrayEnumContext ctx) {}
    public void exitElementValueArrayEnum(@NotNull EsperEPL2GrammarParser.ElementValueArrayEnumContext ctx) {}
    public void enterHourPart(@NotNull EsperEPL2GrammarParser.HourPartContext ctx) {}
    public void exitHourPart(@NotNull EsperEPL2GrammarParser.HourPartContext ctx) {}
    public void enterOnDeleteExpr(@NotNull EsperEPL2GrammarParser.OnDeleteExprContext ctx) {}
    public void exitOnDeleteExpr(@NotNull EsperEPL2GrammarParser.OnDeleteExprContext ctx) {}
    public void enterMatchRecogPatternAtom(@NotNull EsperEPL2GrammarParser.MatchRecogPatternAtomContext ctx) {}
    public void enterGopOutTypeParam(@NotNull EsperEPL2GrammarParser.GopOutTypeParamContext ctx) {}
    public void exitGopOutTypeParam(@NotNull EsperEPL2GrammarParser.GopOutTypeParamContext ctx) {}
    public void enterMergeItem(@NotNull EsperEPL2GrammarParser.MergeItemContext ctx) {}
    public void exitMergeItem(@NotNull EsperEPL2GrammarParser.MergeItemContext ctx) {}
    public void enterYearPart(@NotNull EsperEPL2GrammarParser.YearPartContext ctx) {}
    public void exitYearPart(@NotNull EsperEPL2GrammarParser.YearPartContext ctx) {}
    public void enterEventPropertyOrLibFunction(@NotNull EsperEPL2GrammarParser.EventPropertyOrLibFunctionContext ctx) {}
    public void exitEventPropertyOrLibFunction(@NotNull EsperEPL2GrammarParser.EventPropertyOrLibFunctionContext ctx) {}
    public void enterCreateDataflow(@NotNull EsperEPL2GrammarParser.CreateDataflowContext ctx) {}
    public void enterUpdateExpr(@NotNull EsperEPL2GrammarParser.UpdateExprContext ctx) {}
    public void enterFrequencyOperand(@NotNull EsperEPL2GrammarParser.FrequencyOperandContext ctx) {}
    public void enterOnSetAssignmentList(@NotNull EsperEPL2GrammarParser.OnSetAssignmentListContext ctx) {}
    public void exitOnSetAssignmentList(@NotNull EsperEPL2GrammarParser.OnSetAssignmentListContext ctx) {}
    public void enterPropertyStreamSelector(@NotNull EsperEPL2GrammarParser.PropertyStreamSelectorContext ctx) {}
    public void enterInsertIntoExpr(@NotNull EsperEPL2GrammarParser.InsertIntoExprContext ctx) {}
    public void enterCreateVariableExpr(@NotNull EsperEPL2GrammarParser.CreateVariableExprContext ctx) {}
    public void enterGopParamsItem(@NotNull EsperEPL2GrammarParser.GopParamsItemContext ctx) {}
    public void exitGopParamsItem(@NotNull EsperEPL2GrammarParser.GopParamsItemContext ctx) {}
    public void enterOnStreamExpr(@NotNull EsperEPL2GrammarParser.OnStreamExprContext ctx) {}
    public void enterPropertyExpressionAtomic(@NotNull EsperEPL2GrammarParser.PropertyExpressionAtomicContext ctx) {}
    public void enterGopDetail(@NotNull EsperEPL2GrammarParser.GopDetailContext ctx) {}
    public void exitGopDetail(@NotNull EsperEPL2GrammarParser.GopDetailContext ctx) {}
    public void enterGop(@NotNull EsperEPL2GrammarParser.GopContext ctx) {}
    public void exitGop(@NotNull EsperEPL2GrammarParser.GopContext ctx) {}
    public void enterOutputClauseInsert(@NotNull EsperEPL2GrammarParser.OutputClauseInsertContext ctx) {}
    public void exitOutputClauseInsert(@NotNull EsperEPL2GrammarParser.OutputClauseInsertContext ctx) {}
    public void enterEplExpression(@NotNull EsperEPL2GrammarParser.EplExpressionContext ctx) {}
    public void exitEplExpression(@NotNull EsperEPL2GrammarParser.EplExpressionContext ctx) {}
    public void enterOnMergeExpr(@NotNull EsperEPL2GrammarParser.OnMergeExprContext ctx) {}
    public void exitOnMergeExpr(@NotNull EsperEPL2GrammarParser.OnMergeExprContext ctx) {}
    public void enterFafUpdate(@NotNull EsperEPL2GrammarParser.FafUpdateContext ctx) {}
    public void enterCreateSelectionList(@NotNull EsperEPL2GrammarParser.CreateSelectionListContext ctx) {}
    public void exitCreateSelectionList(@NotNull EsperEPL2GrammarParser.CreateSelectionListContext ctx) {}
    public void enterOnSetExpr(@NotNull EsperEPL2GrammarParser.OnSetExprContext ctx) {}
    public void exitOnSetExpr(@NotNull EsperEPL2GrammarParser.OnSetExprContext ctx) {}
    public void enterBitWiseExpression(@NotNull EsperEPL2GrammarParser.BitWiseExpressionContext ctx) {}
    public void enterChainedFunction(@NotNull EsperEPL2GrammarParser.ChainedFunctionContext ctx) {}
    public void exitChainedFunction(@NotNull EsperEPL2GrammarParser.ChainedFunctionContext ctx) {}
    public void enterMatchRecogPatternUnary(@NotNull EsperEPL2GrammarParser.MatchRecogPatternUnaryContext ctx) {}
    public void exitMatchRecogPatternUnary(@NotNull EsperEPL2GrammarParser.MatchRecogPatternUnaryContext ctx) {}
    public void enterBetweenList(@NotNull EsperEPL2GrammarParser.BetweenListContext ctx) {}
    public void exitBetweenList(@NotNull EsperEPL2GrammarParser.BetweenListContext ctx) {}
    public void enterSecondPart(@NotNull EsperEPL2GrammarParser.SecondPartContext ctx) {}
    public void exitSecondPart(@NotNull EsperEPL2GrammarParser.SecondPartContext ctx) {}
    public void enterEvalEqualsExpression(@NotNull EsperEPL2GrammarParser.EvalEqualsExpressionContext ctx) {}
    public void enterGopConfig(@NotNull EsperEPL2GrammarParser.GopConfigContext ctx) {}
    public void enterMergeMatched(@NotNull EsperEPL2GrammarParser.MergeMatchedContext ctx) {}
    public void enterCreateSelectionListElement(@NotNull EsperEPL2GrammarParser.CreateSelectionListElementContext ctx) {}
    public void enterFafDelete(@NotNull EsperEPL2GrammarParser.FafDeleteContext ctx) {}
    public void enterDayPart(@NotNull EsperEPL2GrammarParser.DayPartContext ctx) {}
    public void exitDayPart(@NotNull EsperEPL2GrammarParser.DayPartContext ctx) {}
    public void enterConstant(@NotNull EsperEPL2GrammarParser.ConstantContext ctx) {}
    public void enterGopOut(@NotNull EsperEPL2GrammarParser.GopOutContext ctx) {}
    public void exitGopOut(@NotNull EsperEPL2GrammarParser.GopOutContext ctx) {}
    public void enterGuardWhereExpression(@NotNull EsperEPL2GrammarParser.GuardWhereExpressionContext ctx) {}
    public void exitGuardWhereExpression(@NotNull EsperEPL2GrammarParser.GuardWhereExpressionContext ctx) {}
    public void enterKeywordAllowedIdent(@NotNull EsperEPL2GrammarParser.KeywordAllowedIdentContext ctx) {}
    public void exitKeywordAllowedIdent(@NotNull EsperEPL2GrammarParser.KeywordAllowedIdentContext ctx) {}
    public void enterCreateContextGroupItem(@NotNull EsperEPL2GrammarParser.CreateContextGroupItemContext ctx) {}
    public void exitCreateContextGroupItem(@NotNull EsperEPL2GrammarParser.CreateContextGroupItemContext ctx) {}
    public void enterEvalAndExpression(@NotNull EsperEPL2GrammarParser.EvalAndExpressionContext ctx) {}
    public void enterMultiplyExpression(@NotNull EsperEPL2GrammarParser.MultiplyExpressionContext ctx) {}
    public void enterExpressionLambdaDecl(@NotNull EsperEPL2GrammarParser.ExpressionLambdaDeclContext ctx) {}
    public void exitExpressionLambdaDecl(@NotNull EsperEPL2GrammarParser.ExpressionLambdaDeclContext ctx) {}
    public void enterPropertyExpression(@NotNull EsperEPL2GrammarParser.PropertyExpressionContext ctx) {}
    public void exitPropertyExpression(@NotNull EsperEPL2GrammarParser.PropertyExpressionContext ctx) {}
    public void enterOuterJoinIdentPair(@NotNull EsperEPL2GrammarParser.OuterJoinIdentPairContext ctx) {}
    public void exitOuterJoinIdentPair(@NotNull EsperEPL2GrammarParser.OuterJoinIdentPairContext ctx) {}
    public void enterGopOutItem(@NotNull EsperEPL2GrammarParser.GopOutItemContext ctx) {}
    public void exitGopOutItem(@NotNull EsperEPL2GrammarParser.GopOutItemContext ctx) {}
    public void enterForExpr(@NotNull EsperEPL2GrammarParser.ForExprContext ctx) {}
    public void enterPropertyExpressionSelect(@NotNull EsperEPL2GrammarParser.PropertyExpressionSelectContext ctx) {}
    public void exitPropertyExpressionSelect(@NotNull EsperEPL2GrammarParser.PropertyExpressionSelectContext ctx) {}
    public void enterExpressionQualifyable(@NotNull EsperEPL2GrammarParser.ExpressionQualifyableContext ctx) {}
    public void enterExpressionDialect(@NotNull EsperEPL2GrammarParser.ExpressionDialectContext ctx) {}
    public void exitExpressionDialect(@NotNull EsperEPL2GrammarParser.ExpressionDialectContext ctx) {}
    public void enterStartEventPropertyRule(@NotNull EsperEPL2GrammarParser.StartEventPropertyRuleContext ctx) {}
    public void exitStartEventPropertyRule(@NotNull EsperEPL2GrammarParser.StartEventPropertyRuleContext ctx) {}
    public void enterPropertySelectionListElement(@NotNull EsperEPL2GrammarParser.PropertySelectionListElementContext ctx) {}
    public void enterExpressionDecl(@NotNull EsperEPL2GrammarParser.ExpressionDeclContext ctx) {}
    public void enterSubstitution(@NotNull EsperEPL2GrammarParser.SubstitutionContext ctx) {}
    public void enterCrontabLimitParameterSet(@NotNull EsperEPL2GrammarParser.CrontabLimitParameterSetContext ctx) {}
    public void exitCrontabLimitParameterSet(@NotNull EsperEPL2GrammarParser.CrontabLimitParameterSetContext ctx) {}
    public void enterWeekDayOperator(@NotNull EsperEPL2GrammarParser.WeekDayOperatorContext ctx) {}
    public void enterWhenClause(@NotNull EsperEPL2GrammarParser.WhenClauseContext ctx) {}
    public void exitWhenClause(@NotNull EsperEPL2GrammarParser.WhenClauseContext ctx) {}
    public void enterNewAssign(@NotNull EsperEPL2GrammarParser.NewAssignContext ctx) {}
    public void exitNewAssign(@NotNull EsperEPL2GrammarParser.NewAssignContext ctx) {}
    public void enterLastWeekdayOperand(@NotNull EsperEPL2GrammarParser.LastWeekdayOperandContext ctx) {}
    public void enterGroupByListExpr(@NotNull EsperEPL2GrammarParser.GroupByListExprContext ctx) {}
    public void enterStreamSelector(@NotNull EsperEPL2GrammarParser.StreamSelectorContext ctx) {}
    public void enterStartJsonValueRule(@NotNull EsperEPL2GrammarParser.StartJsonValueRuleContext ctx) {}
    public void exitStartJsonValueRule(@NotNull EsperEPL2GrammarParser.StartJsonValueRuleContext ctx) {}
    public void enterStreamExpression(@NotNull EsperEPL2GrammarParser.StreamExpressionContext ctx) {}
    public void enterOuterJoinIdent(@NotNull EsperEPL2GrammarParser.OuterJoinIdentContext ctx) {}
    public void exitOuterJoinIdent(@NotNull EsperEPL2GrammarParser.OuterJoinIdentContext ctx) {}
    public void enterCreateIndexColumnList(@NotNull EsperEPL2GrammarParser.CreateIndexColumnListContext ctx) {}
    public void exitCreateIndexColumnList(@NotNull EsperEPL2GrammarParser.CreateIndexColumnListContext ctx) {}
    public void enterViewExpression(@NotNull EsperEPL2GrammarParser.ViewExpressionContext ctx) {}
    public void enterColumnList(@NotNull EsperEPL2GrammarParser.ColumnListContext ctx) {}
    public void exitColumnList(@NotNull EsperEPL2GrammarParser.ColumnListContext ctx) {}
    public void enterPatternFilterExpression(@NotNull EsperEPL2GrammarParser.PatternFilterExpressionContext ctx) {}
    public void enterJsonpair(@NotNull EsperEPL2GrammarParser.JsonpairContext ctx) {}
    public void exitJsonpair(@NotNull EsperEPL2GrammarParser.JsonpairContext ctx) {}
    public void enterOnSelectExpr(@NotNull EsperEPL2GrammarParser.OnSelectExprContext ctx) {}
    public void enterElementValuePairEnum(@NotNull EsperEPL2GrammarParser.ElementValuePairEnumContext ctx) {}
    public void exitElementValuePairEnum(@NotNull EsperEPL2GrammarParser.ElementValuePairEnumContext ctx) {}
    public void enterStartPatternExpressionRule(@NotNull EsperEPL2GrammarParser.StartPatternExpressionRuleContext ctx) {}
    public void enterSelectionListElementAnno(@NotNull EsperEPL2GrammarParser.SelectionListElementAnnoContext ctx) {}
    public void exitSelectionListElementAnno(@NotNull EsperEPL2GrammarParser.SelectionListElementAnnoContext ctx) {}
    public void enterOutputLimit(@NotNull EsperEPL2GrammarParser.OutputLimitContext ctx) {}
    public void enterCreateContextDistinct(@NotNull EsperEPL2GrammarParser.CreateContextDistinctContext ctx) {}
    public void exitCreateContextDistinct(@NotNull EsperEPL2GrammarParser.CreateContextDistinctContext ctx) {}
    public void enterJsonelements(@NotNull EsperEPL2GrammarParser.JsonelementsContext ctx) {}
    public void exitJsonelements(@NotNull EsperEPL2GrammarParser.JsonelementsContext ctx) {}
    public void enterNumericParameterList(@NotNull EsperEPL2GrammarParser.NumericParameterListContext ctx) {}
    public void enterLibFunctionWithClass(@NotNull EsperEPL2GrammarParser.LibFunctionWithClassContext ctx) {}
    public void exitLibFunctionWithClass(@NotNull EsperEPL2GrammarParser.LibFunctionWithClassContext ctx) {}
    public void enterPropertyExpressionAnnotation(@NotNull EsperEPL2GrammarParser.PropertyExpressionAnnotationContext ctx) {}
    public void exitPropertyExpressionAnnotation(@NotNull EsperEPL2GrammarParser.PropertyExpressionAnnotationContext ctx) {}
    public void enterStringconstant(@NotNull EsperEPL2GrammarParser.StringconstantContext ctx) {}
    public void exitStringconstant(@NotNull EsperEPL2GrammarParser.StringconstantContext ctx) {}
    public void enterCreateSchemaExpr(@NotNull EsperEPL2GrammarParser.CreateSchemaExprContext ctx) {}
    public void enterElseClause(@NotNull EsperEPL2GrammarParser.ElseClauseContext ctx) {}
    public void exitElseClause(@NotNull EsperEPL2GrammarParser.ElseClauseContext ctx) {}
    public void enterGuardWhileExpression(@NotNull EsperEPL2GrammarParser.GuardWhileExpressionContext ctx) {}
    public void exitGuardWhileExpression(@NotNull EsperEPL2GrammarParser.GuardWhileExpressionContext ctx) {}
    public void enterCreateWindowExprModelAfter(@NotNull EsperEPL2GrammarParser.CreateWindowExprModelAfterContext ctx) {}
    public void exitCreateWindowExprModelAfter(@NotNull EsperEPL2GrammarParser.CreateWindowExprModelAfterContext ctx) {}
    public void enterMatchRecogMatchesAfterSkip(@NotNull EsperEPL2GrammarParser.MatchRecogMatchesAfterSkipContext ctx) {}
    public void exitMatchRecogMatchesAfterSkip(@NotNull EsperEPL2GrammarParser.MatchRecogMatchesAfterSkipContext ctx) {}
    public void enterCreateContextDetail(@NotNull EsperEPL2GrammarParser.CreateContextDetailContext ctx) {}
    public void exitCreateContextDetail(@NotNull EsperEPL2GrammarParser.CreateContextDetailContext ctx) {}
    public void enterMonthPart(@NotNull EsperEPL2GrammarParser.MonthPartContext ctx) {}
    public void exitMonthPart(@NotNull EsperEPL2GrammarParser.MonthPartContext ctx) {}
    public void enterPatternExpression(@NotNull EsperEPL2GrammarParser.PatternExpressionContext ctx) {}
    public void exitPatternExpression(@NotNull EsperEPL2GrammarParser.PatternExpressionContext ctx) {}
    public void enterLastOperator(@NotNull EsperEPL2GrammarParser.LastOperatorContext ctx) {}
    public void enterCreateSchemaDef(@NotNull EsperEPL2GrammarParser.CreateSchemaDefContext ctx) {}
    public void exitCreateSchemaDef(@NotNull EsperEPL2GrammarParser.CreateSchemaDefContext ctx) {}
    public void enterEventPropertyIdent(@NotNull EsperEPL2GrammarParser.EventPropertyIdentContext ctx) {}
    public void exitEventPropertyIdent(@NotNull EsperEPL2GrammarParser.EventPropertyIdentContext ctx) {}
    public void enterCreateIndexExpr(@NotNull EsperEPL2GrammarParser.CreateIndexExprContext ctx) {}
    public void enterAtomicExpression(@NotNull EsperEPL2GrammarParser.AtomicExpressionContext ctx) {}
    public void exitAtomicExpression(@NotNull EsperEPL2GrammarParser.AtomicExpressionContext ctx) {}
    public void enterJsonvalue(@NotNull EsperEPL2GrammarParser.JsonvalueContext ctx) {}
    public void exitJsonvalue(@NotNull EsperEPL2GrammarParser.JsonvalueContext ctx) {}
    public void enterLibFunctionNoClass(@NotNull EsperEPL2GrammarParser.LibFunctionNoClassContext ctx) {}
    public void exitLibFunctionNoClass(@NotNull EsperEPL2GrammarParser.LibFunctionNoClassContext ctx) {}
    public void enterElementValueEnum(@NotNull EsperEPL2GrammarParser.ElementValueEnumContext ctx) {}
    public void exitElementValueEnum(@NotNull EsperEPL2GrammarParser.ElementValueEnumContext ctx) {}
    public void enterOnUpdateExpr(@NotNull EsperEPL2GrammarParser.OnUpdateExprContext ctx) {}
    public void exitOnUpdateExpr(@NotNull EsperEPL2GrammarParser.OnUpdateExprContext ctx) {}
    public void enterAnnotationEnum(@NotNull EsperEPL2GrammarParser.AnnotationEnumContext ctx) {}
    public void enterCreateContextExpr(@NotNull EsperEPL2GrammarParser.CreateContextExprContext ctx) {}
    public void enterLastOperand(@NotNull EsperEPL2GrammarParser.LastOperandContext ctx) {}
    public void enterExpressionWithTimeInclLast(@NotNull EsperEPL2GrammarParser.ExpressionWithTimeInclLastContext ctx) {}
    public void exitExpressionWithTimeInclLast(@NotNull EsperEPL2GrammarParser.ExpressionWithTimeInclLastContext ctx) {}
    public void enterCreateContextPartitionItem(@NotNull EsperEPL2GrammarParser.CreateContextPartitionItemContext ctx) {}
    public void exitCreateContextPartitionItem(@NotNull EsperEPL2GrammarParser.CreateContextPartitionItemContext ctx) {}
    public void enterCreateWindowExpr(@NotNull EsperEPL2GrammarParser.CreateWindowExprContext ctx) {}
    public void enterVariantListElement(@NotNull EsperEPL2GrammarParser.VariantListElementContext ctx) {}
    public void exitVariantListElement(@NotNull EsperEPL2GrammarParser.VariantListElementContext ctx) {}
    public void enterCreateExpressionExpr(@NotNull EsperEPL2GrammarParser.CreateExpressionExprContext ctx) {}
    public void enterRangeOperand(@NotNull EsperEPL2GrammarParser.RangeOperandContext ctx) {}
    public void enterInSubSelectQuery(@NotNull EsperEPL2GrammarParser.InSubSelectQueryContext ctx) {}
    public void exitInSubSelectQuery(@NotNull EsperEPL2GrammarParser.InSubSelectQueryContext ctx) {}
    public void enterEscapableStr(@NotNull EsperEPL2GrammarParser.EscapableStrContext ctx) {}
    public void exitEscapableStr(@NotNull EsperEPL2GrammarParser.EscapableStrContext ctx) {}
    public void enterRowSubSelectExpression(@NotNull EsperEPL2GrammarParser.RowSubSelectExpressionContext ctx) {}
    public void enterUnaryExpression(@NotNull EsperEPL2GrammarParser.UnaryExpressionContext ctx) {}
    public void enterDistinctExpressionList(@NotNull EsperEPL2GrammarParser.DistinctExpressionListContext ctx) {}
    public void exitDistinctExpressionList(@NotNull EsperEPL2GrammarParser.DistinctExpressionListContext ctx) {}
    public void exitOnSelectInsertExpr(@NotNull EsperEPL2GrammarParser.OnSelectInsertExprContext ctx) {}
    public void enterSelectClause(@NotNull EsperEPL2GrammarParser.SelectClauseContext ctx) {}
    public void enterConcatenationExpr(@NotNull EsperEPL2GrammarParser.ConcatenationExprContext ctx) {}
    public void enterStartEPLExpressionRule(@NotNull EsperEPL2GrammarParser.StartEPLExpressionRuleContext ctx) {}
    public void exitStartEPLExpressionRule(@NotNull EsperEPL2GrammarParser.StartEPLExpressionRuleContext ctx) {}
    public void enterSubSelectFilterExpr(@NotNull EsperEPL2GrammarParser.SubSelectFilterExprContext ctx) {}
    public void enterCreateContextCoalesceItem(@NotNull EsperEPL2GrammarParser.CreateContextCoalesceItemContext ctx) {}
    public void exitCreateContextCoalesceItem(@NotNull EsperEPL2GrammarParser.CreateContextCoalesceItemContext ctx) {}
    public void enterMillisecondPart(@NotNull EsperEPL2GrammarParser.MillisecondPartContext ctx) {}
    public void exitMillisecondPart(@NotNull EsperEPL2GrammarParser.MillisecondPartContext ctx) {}
    public void enterOnExprFrom(@NotNull EsperEPL2GrammarParser.OnExprFromContext ctx) {}
    public void exitOnExprFrom(@NotNull EsperEPL2GrammarParser.OnExprFromContext ctx) {}
    public void enterNegatedExpression(@NotNull EsperEPL2GrammarParser.NegatedExpressionContext ctx) {}
    public void enterSelectExpr(@NotNull EsperEPL2GrammarParser.SelectExprContext ctx) {}
    public void enterMatchRecogMeasures(@NotNull EsperEPL2GrammarParser.MatchRecogMeasuresContext ctx) {}
    public void exitMatchRecogMeasures(@NotNull EsperEPL2GrammarParser.MatchRecogMeasuresContext ctx) {}
    public void enterAdditiveExpression(@NotNull EsperEPL2GrammarParser.AdditiveExpressionContext ctx) {}
    public void enterEventProperty(@NotNull EsperEPL2GrammarParser.EventPropertyContext ctx) {}
    public void enterJsonarray(@NotNull EsperEPL2GrammarParser.JsonarrayContext ctx) {}
    public void exitJsonarray(@NotNull EsperEPL2GrammarParser.JsonarrayContext ctx) {}
    public void enterJsonobject(@NotNull EsperEPL2GrammarParser.JsonobjectContext ctx) {}
    public void enterOuterJoin(@NotNull EsperEPL2GrammarParser.OuterJoinContext ctx) {}
    public void enterEscapableIdent(@NotNull EsperEPL2GrammarParser.EscapableIdentContext ctx) {}
    public void exitEscapableIdent(@NotNull EsperEPL2GrammarParser.EscapableIdentContext ctx) {}
    public void enterFromClause(@NotNull EsperEPL2GrammarParser.FromClauseContext ctx) {}
    public void exitFromClause(@NotNull EsperEPL2GrammarParser.FromClauseContext ctx) {}
    public void enterOnExpr(@NotNull EsperEPL2GrammarParser.OnExprContext ctx) {}
    public void enterGopParamsItemMany(@NotNull EsperEPL2GrammarParser.GopParamsItemManyContext ctx) {}
    public void exitGopParamsItemMany(@NotNull EsperEPL2GrammarParser.GopParamsItemManyContext ctx) {}
    public void enterPropertySelectionList(@NotNull EsperEPL2GrammarParser.PropertySelectionListContext ctx) {}
    public void exitPropertySelectionList(@NotNull EsperEPL2GrammarParser.PropertySelectionListContext ctx) {}
    public void enterWeekPart(@NotNull EsperEPL2GrammarParser.WeekPartContext ctx) {}
    public void exitWeekPart(@NotNull EsperEPL2GrammarParser.WeekPartContext ctx) {}
    public void enterMatchRecogPatternAlteration(@NotNull EsperEPL2GrammarParser.MatchRecogPatternAlterationContext ctx) {}
    public void enterGopParams(@NotNull EsperEPL2GrammarParser.GopParamsContext ctx) {}
    public void exitGopParams(@NotNull EsperEPL2GrammarParser.GopParamsContext ctx) {}
    public void enterCreateContextChoice(@NotNull EsperEPL2GrammarParser.CreateContextChoiceContext ctx) {}
    public void exitCreateContextChoice(@NotNull EsperEPL2GrammarParser.CreateContextChoiceContext ctx) {}
    public void enterCaseExpression(@NotNull EsperEPL2GrammarParser.CaseExpressionContext ctx) {}
    public void enterCreateIndexColumn(@NotNull EsperEPL2GrammarParser.CreateIndexColumnContext ctx) {}
    public void exitCreateIndexColumn(@NotNull EsperEPL2GrammarParser.CreateIndexColumnContext ctx) {}
    public void enterExpressionWithTimeList(@NotNull EsperEPL2GrammarParser.ExpressionWithTimeListContext ctx) {}
    public void exitExpressionWithTimeList(@NotNull EsperEPL2GrammarParser.ExpressionWithTimeListContext ctx) {}
    public void enterGopParamsItemAs(@NotNull EsperEPL2GrammarParser.GopParamsItemAsContext ctx) {}
    public void exitGopParamsItemAs(@NotNull EsperEPL2GrammarParser.GopParamsItemAsContext ctx) {}
    public void enterRowLimit(@NotNull EsperEPL2GrammarParser.RowLimitContext ctx) {}
    public void enterCreateSchemaQual(@NotNull EsperEPL2GrammarParser.CreateSchemaQualContext ctx) {}
    public void exitCreateSchemaQual(@NotNull EsperEPL2GrammarParser.CreateSchemaQualContext ctx) {}
    public void enterMatchUntilRange(@NotNull EsperEPL2GrammarParser.MatchUntilRangeContext ctx) {}
    public void exitMatchUntilRange(@NotNull EsperEPL2GrammarParser.MatchUntilRangeContext ctx) {}
    public void enterMatchRecogDefine(@NotNull EsperEPL2GrammarParser.MatchRecogDefineContext ctx) {}
    public void exitMatchRecogDefine(@NotNull EsperEPL2GrammarParser.MatchRecogDefineContext ctx) {}
    public void enterOrderByListElement(@NotNull EsperEPL2GrammarParser.OrderByListElementContext ctx) {}
    public void enterMinutePart(@NotNull EsperEPL2GrammarParser.MinutePartContext ctx) {}
    public void exitMinutePart(@NotNull EsperEPL2GrammarParser.MinutePartContext ctx) {}
    public void enterMergeUnmatched(@NotNull EsperEPL2GrammarParser.MergeUnmatchedContext ctx) {}
    public void enterMethodJoinExpression(@NotNull EsperEPL2GrammarParser.MethodJoinExpressionContext ctx) {}
    public void exitMethodJoinExpression(@NotNull EsperEPL2GrammarParser.MethodJoinExpressionContext ctx) {}
    public void enterExistsSubSelectExpression(@NotNull EsperEPL2GrammarParser.ExistsSubSelectExpressionContext ctx) {}
    public void enterCreateContextRangePoint(@NotNull EsperEPL2GrammarParser.CreateContextRangePointContext ctx) {}
    public void exitCreateContextRangePoint(@NotNull EsperEPL2GrammarParser.CreateContextRangePointContext ctx) {}
    public void enterLibFunctionArgItem(@NotNull EsperEPL2GrammarParser.LibFunctionArgItemContext ctx) {}
    public void exitLibFunctionArgItem(@NotNull EsperEPL2GrammarParser.LibFunctionArgItemContext ctx) {}
    public void enterRegularJoin(@NotNull EsperEPL2GrammarParser.RegularJoinContext ctx) {}
    public void exitRegularJoin(@NotNull EsperEPL2GrammarParser.RegularJoinContext ctx) {}
    public void enterUpdateDetails(@NotNull EsperEPL2GrammarParser.UpdateDetailsContext ctx) {}
    public void exitUpdateDetails(@NotNull EsperEPL2GrammarParser.UpdateDetailsContext ctx) {}
    public void enterArrayExpression(@NotNull EsperEPL2GrammarParser.ArrayExpressionContext ctx) {}
    public void visitErrorNode(@NotNull ErrorNode errorNode) {}
    public void enterEveryRule(@NotNull ParserRuleContext parserRuleContext) {}
    public void exitEveryRule(@NotNull ParserRuleContext parserRuleContext) {}
    public void enterAndExpression(@NotNull EsperEPL2GrammarParser.AndExpressionContext ctx) {}
    public void enterFollowedByRepeat(@NotNull EsperEPL2GrammarParser.FollowedByRepeatContext ctx) {}
    public void exitFollowedByRepeat(@NotNull EsperEPL2GrammarParser.FollowedByRepeatContext ctx) {}
    public void enterFollowedByExpression(@NotNull EsperEPL2GrammarParser.FollowedByExpressionContext ctx) {}
    public void enterOrExpression(@NotNull EsperEPL2GrammarParser.OrExpressionContext ctx) {}
    public void enterQualifyExpression(@NotNull EsperEPL2GrammarParser.QualifyExpressionContext ctx) {}
    public void enterMatchUntilExpression(@NotNull EsperEPL2GrammarParser.MatchUntilExpressionContext ctx) {}
    public void enterGuardPostFix(@NotNull EsperEPL2GrammarParser.GuardPostFixContext ctx) {}
    public void enterBuiltin_coalesce(@NotNull EsperEPL2GrammarParser.Builtin_coalesceContext ctx) {}
    public void enterBuiltin_typeof(@NotNull EsperEPL2GrammarParser.Builtin_typeofContext ctx) {}
    public void enterBuiltin_avedev(@NotNull EsperEPL2GrammarParser.Builtin_avedevContext ctx) {}
    public void enterBuiltin_prevcount(@NotNull EsperEPL2GrammarParser.Builtin_prevcountContext ctx) {}
    public void enterBuiltin_stddev(@NotNull EsperEPL2GrammarParser.Builtin_stddevContext ctx) {}
    public void enterBuiltin_sum(@NotNull EsperEPL2GrammarParser.Builtin_sumContext ctx) {}
    public void enterBuiltin_exists(@NotNull EsperEPL2GrammarParser.Builtin_existsContext ctx) {}
    public void enterBuiltin_prior(@NotNull EsperEPL2GrammarParser.Builtin_priorContext ctx) {}
    public void enterBuiltin_instanceof(@NotNull EsperEPL2GrammarParser.Builtin_instanceofContext ctx) {}
    public void enterBuiltin_currts(@NotNull EsperEPL2GrammarParser.Builtin_currtsContext ctx) {}
    public void enterBuiltin_median(@NotNull EsperEPL2GrammarParser.Builtin_medianContext ctx) {}
    public void enterFuncIdentChained(@NotNull EsperEPL2GrammarParser.FuncIdentChainedContext ctx) {}
    public void exitFuncIdentChained(@NotNull EsperEPL2GrammarParser.FuncIdentChainedContext ctx) {}
    public void enterFuncIdentTop(@NotNull EsperEPL2GrammarParser.FuncIdentTopContext ctx) {}
    public void exitFuncIdentTop(@NotNull EsperEPL2GrammarParser.FuncIdentTopContext ctx) {}
    public void enterBuiltin_avg(@NotNull EsperEPL2GrammarParser.Builtin_avgContext ctx) {}
    public void enterBuiltin_cast(@NotNull EsperEPL2GrammarParser.Builtin_castContext ctx) {}
    public void enterBuiltin_cnt(@NotNull EsperEPL2GrammarParser.Builtin_cntContext ctx) {}
    public void enterBuiltin_prev(@NotNull EsperEPL2GrammarParser.Builtin_prevContext ctx) {}
    public void enterBuiltin_istream(@NotNull EsperEPL2GrammarParser.Builtin_istreamContext ctx) {}
    public void enterBuiltin_prevwindow(@NotNull EsperEPL2GrammarParser.Builtin_prevwindowContext ctx) {}
    public void enterBuiltin_prevtail(@NotNull EsperEPL2GrammarParser.Builtin_prevtailContext ctx) {}
    public void enterFafInsert(@NotNull EsperEPL2GrammarParser.FafInsertContext ctx) {}
    public void enterGroupByListChoice(@NotNull EsperEPL2GrammarParser.GroupByListChoiceContext ctx) {}
    public void exitGroupByListChoice(@NotNull EsperEPL2GrammarParser.GroupByListChoiceContext ctx) {}
    public void enterGroupBySetsChoice(@NotNull EsperEPL2GrammarParser.GroupBySetsChoiceContext ctx) {}
    public void exitGroupBySetsChoice(@NotNull EsperEPL2GrammarParser.GroupBySetsChoiceContext ctx) {}
    public void exitSelectExpr(@NotNull EsperEPL2GrammarParser.SelectExprContext ctx) {}
    public void enterGroupByCubeOrRollup(@NotNull EsperEPL2GrammarParser.GroupByCubeOrRollupContext ctx) {}
    public void exitGroupByCubeOrRollup(@NotNull EsperEPL2GrammarParser.GroupByCubeOrRollupContext ctx) {}
    public void enterGroupByGroupingSets(@NotNull EsperEPL2GrammarParser.GroupByGroupingSetsContext ctx) {}
    public void exitGroupByGroupingSets(@NotNull EsperEPL2GrammarParser.GroupByGroupingSetsContext ctx) {}
    public void enterGroupByCombinableExpr(@NotNull EsperEPL2GrammarParser.GroupByCombinableExprContext ctx) {}
    public void exitGroupByCombinableExpr(@NotNull EsperEPL2GrammarParser.GroupByCombinableExprContext ctx) {}
    public void enterBuiltin_grouping(@NotNull EsperEPL2GrammarParser.Builtin_groupingContext ctx) {}
    public void enterBuiltin_groupingid(@NotNull EsperEPL2GrammarParser.Builtin_groupingidContext ctx) {}
    public void enterFuncIdentInner(@NotNull EsperEPL2GrammarParser.FuncIdentInnerContext ctx) {}
    public void exitFuncIdentInner(@NotNull EsperEPL2GrammarParser.FuncIdentInnerContext ctx) {}
    public void enterCreateTableColumnPlain(@NotNull EsperEPL2GrammarParser.CreateTableColumnPlainContext ctx) {}
    public void exitCreateTableColumnPlain(@NotNull EsperEPL2GrammarParser.CreateTableColumnPlainContext ctx) {}
    public void enterCreateTableExpr(@NotNull EsperEPL2GrammarParser.CreateTableExprContext ctx) {}
    public void enterCreateTableColumn(@NotNull EsperEPL2GrammarParser.CreateTableColumnContext ctx) {}
    public void exitCreateTableColumn(@NotNull EsperEPL2GrammarParser.CreateTableColumnContext ctx) {}
    public void enterCreateTableColumnList(@NotNull EsperEPL2GrammarParser.CreateTableColumnListContext ctx) {}
    public void exitCreateTableColumnList(@NotNull EsperEPL2GrammarParser.CreateTableColumnListContext ctx) {}
    public void enterIntoTableExpr(@NotNull EsperEPL2GrammarParser.IntoTableExprContext ctx) {}
    public void enterSubstitutionCanChain(@NotNull EsperEPL2GrammarParser.SubstitutionCanChainContext ctx) {}
    public void enterSlashIdentifier(@NotNull EsperEPL2GrammarParser.SlashIdentifierContext ctx) {}
    public void exitSlashIdentifier(@NotNull EsperEPL2GrammarParser.SlashIdentifierContext ctx) {}
    public void enterMatchRecogPatternRepeat(@NotNull EsperEPL2GrammarParser.MatchRecogPatternRepeatContext ctx) {}
    public void exitMatchRecogPatternRepeat(@NotNull EsperEPL2GrammarParser.MatchRecogPatternRepeatContext ctx) {}
    public void enterMatchRecogPatternPermute(@NotNull EsperEPL2GrammarParser.MatchRecogPatternPermuteContext ctx) {}
    public void enterExpressionListWithNamed(@NotNull EsperEPL2GrammarParser.ExpressionListWithNamedContext ctx) {}
    public void exitExpressionListWithNamed(@NotNull EsperEPL2GrammarParser.ExpressionListWithNamedContext ctx) {}
    public void enterExpressionNamedParameter(@NotNull EsperEPL2GrammarParser.ExpressionNamedParameterContext ctx) {}
    public void enterExpressionWithNamed(@NotNull EsperEPL2GrammarParser.ExpressionWithNamedContext ctx) {}
    public void exitExpressionWithNamed(@NotNull EsperEPL2GrammarParser.ExpressionWithNamedContext ctx) {}
    public void enterBuiltin_firstlastwindow(@NotNull EsperEPL2GrammarParser.Builtin_firstlastwindowContext ctx) {}
    public void enterFirstLastWindowAggregation(@NotNull EsperEPL2GrammarParser.FirstLastWindowAggregationContext ctx) {}
    public void exitFirstLastWindowAggregation(@NotNull EsperEPL2GrammarParser.FirstLastWindowAggregationContext ctx) {}
    public void enterExpressionWithNamedWithTime(@NotNull EsperEPL2GrammarParser.ExpressionWithNamedWithTimeContext ctx) {}
    public void exitExpressionWithNamedWithTime(@NotNull EsperEPL2GrammarParser.ExpressionWithNamedWithTimeContext ctx) {}
    public void enterExpressionNamedParameterWithTime(@NotNull EsperEPL2GrammarParser.ExpressionNamedParameterWithTimeContext ctx) {}
    public void enterExpressionListWithNamedWithTime(@NotNull EsperEPL2GrammarParser.ExpressionListWithNamedWithTimeContext ctx) {}
    public void exitExpressionListWithNamedWithTime(@NotNull EsperEPL2GrammarParser.ExpressionListWithNamedWithTimeContext ctx) {}
}
