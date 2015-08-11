// Generated from EsperEPL2Grammar.g by ANTLR 4.1

  package com.espertech.esper.epl.generated;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link EsperEPL2GrammarParser}.
 */
public interface EsperEPL2GrammarListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#contextExpr}.
	 * @param ctx the parse tree
	 */
	void enterContextExpr(@NotNull EsperEPL2GrammarParser.ContextExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#contextExpr}.
	 * @param ctx the parse tree
	 */
	void exitContextExpr(@NotNull EsperEPL2GrammarParser.ContextExprContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionListWithNamed}.
	 * @param ctx the parse tree
	 */
	void enterExpressionListWithNamed(@NotNull EsperEPL2GrammarParser.ExpressionListWithNamedContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionListWithNamed}.
	 * @param ctx the parse tree
	 */
	void exitExpressionListWithNamed(@NotNull EsperEPL2GrammarParser.ExpressionListWithNamedContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionList}.
	 * @param ctx the parse tree
	 */
	void enterExpressionList(@NotNull EsperEPL2GrammarParser.ExpressionListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionList}.
	 * @param ctx the parse tree
	 */
	void exitExpressionList(@NotNull EsperEPL2GrammarParser.ExpressionListContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#selectionList}.
	 * @param ctx the parse tree
	 */
	void enterSelectionList(@NotNull EsperEPL2GrammarParser.SelectionListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#selectionList}.
	 * @param ctx the parse tree
	 */
	void exitSelectionList(@NotNull EsperEPL2GrammarParser.SelectionListContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#fafInsert}.
	 * @param ctx the parse tree
	 */
	void enterFafInsert(@NotNull EsperEPL2GrammarParser.FafInsertContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#fafInsert}.
	 * @param ctx the parse tree
	 */
	void exitFafInsert(@NotNull EsperEPL2GrammarParser.FafInsertContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#evalRelationalExpression}.
	 * @param ctx the parse tree
	 */
	void enterEvalRelationalExpression(@NotNull EsperEPL2GrammarParser.EvalRelationalExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#evalRelationalExpression}.
	 * @param ctx the parse tree
	 */
	void exitEvalRelationalExpression(@NotNull EsperEPL2GrammarParser.EvalRelationalExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#patternInclusionExpression}.
	 * @param ctx the parse tree
	 */
	void enterPatternInclusionExpression(@NotNull EsperEPL2GrammarParser.PatternInclusionExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#patternInclusionExpression}.
	 * @param ctx the parse tree
	 */
	void exitPatternInclusionExpression(@NotNull EsperEPL2GrammarParser.PatternInclusionExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#builtin_groupingid}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_groupingid(@NotNull EsperEPL2GrammarParser.Builtin_groupingidContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#builtin_groupingid}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_groupingid(@NotNull EsperEPL2GrammarParser.Builtin_groupingidContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#libFunction}.
	 * @param ctx the parse tree
	 */
	void enterLibFunction(@NotNull EsperEPL2GrammarParser.LibFunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#libFunction}.
	 * @param ctx the parse tree
	 */
	void exitLibFunction(@NotNull EsperEPL2GrammarParser.LibFunctionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#builtin_coalesce}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_coalesce(@NotNull EsperEPL2GrammarParser.Builtin_coalesceContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#builtin_coalesce}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_coalesce(@NotNull EsperEPL2GrammarParser.Builtin_coalesceContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#selectionListElement}.
	 * @param ctx the parse tree
	 */
	void enterSelectionListElement(@NotNull EsperEPL2GrammarParser.SelectionListElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#selectionListElement}.
	 * @param ctx the parse tree
	 */
	void exitSelectionListElement(@NotNull EsperEPL2GrammarParser.SelectionListElementContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#gopOutTypeList}.
	 * @param ctx the parse tree
	 */
	void enterGopOutTypeList(@NotNull EsperEPL2GrammarParser.GopOutTypeListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#gopOutTypeList}.
	 * @param ctx the parse tree
	 */
	void exitGopOutTypeList(@NotNull EsperEPL2GrammarParser.GopOutTypeListContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#gopOutTypeItem}.
	 * @param ctx the parse tree
	 */
	void enterGopOutTypeItem(@NotNull EsperEPL2GrammarParser.GopOutTypeItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#gopOutTypeItem}.
	 * @param ctx the parse tree
	 */
	void exitGopOutTypeItem(@NotNull EsperEPL2GrammarParser.GopOutTypeItemContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecog}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecog(@NotNull EsperEPL2GrammarParser.MatchRecogContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecog}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecog(@NotNull EsperEPL2GrammarParser.MatchRecogContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPatternRepeat}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogPatternRepeat(@NotNull EsperEPL2GrammarParser.MatchRecogPatternRepeatContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPatternRepeat}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogPatternRepeat(@NotNull EsperEPL2GrammarParser.MatchRecogPatternRepeatContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#jsonmembers}.
	 * @param ctx the parse tree
	 */
	void enterJsonmembers(@NotNull EsperEPL2GrammarParser.JsonmembersContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#jsonmembers}.
	 * @param ctx the parse tree
	 */
	void exitJsonmembers(@NotNull EsperEPL2GrammarParser.JsonmembersContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#number}.
	 * @param ctx the parse tree
	 */
	void enterNumber(@NotNull EsperEPL2GrammarParser.NumberContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#number}.
	 * @param ctx the parse tree
	 */
	void exitNumber(@NotNull EsperEPL2GrammarParser.NumberContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#variantList}.
	 * @param ctx the parse tree
	 */
	void enterVariantList(@NotNull EsperEPL2GrammarParser.VariantListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#variantList}.
	 * @param ctx the parse tree
	 */
	void exitVariantList(@NotNull EsperEPL2GrammarParser.VariantListContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPartitionBy}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogPartitionBy(@NotNull EsperEPL2GrammarParser.MatchRecogPartitionByContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPartitionBy}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogPartitionBy(@NotNull EsperEPL2GrammarParser.MatchRecogPartitionByContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#outputLimitAfter}.
	 * @param ctx the parse tree
	 */
	void enterOutputLimitAfter(@NotNull EsperEPL2GrammarParser.OutputLimitAfterContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#outputLimitAfter}.
	 * @param ctx the parse tree
	 */
	void exitOutputLimitAfter(@NotNull EsperEPL2GrammarParser.OutputLimitAfterContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createColumnList}.
	 * @param ctx the parse tree
	 */
	void enterCreateColumnList(@NotNull EsperEPL2GrammarParser.CreateColumnListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createColumnList}.
	 * @param ctx the parse tree
	 */
	void exitCreateColumnList(@NotNull EsperEPL2GrammarParser.CreateColumnListContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#mergeMatchedItem}.
	 * @param ctx the parse tree
	 */
	void enterMergeMatchedItem(@NotNull EsperEPL2GrammarParser.MergeMatchedItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#mergeMatchedItem}.
	 * @param ctx the parse tree
	 */
	void exitMergeMatchedItem(@NotNull EsperEPL2GrammarParser.MergeMatchedItemContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#builtin_firstlastwindow}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_firstlastwindow(@NotNull EsperEPL2GrammarParser.Builtin_firstlastwindowContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#builtin_firstlastwindow}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_firstlastwindow(@NotNull EsperEPL2GrammarParser.Builtin_firstlastwindowContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogMatchesSelection}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogMatchesSelection(@NotNull EsperEPL2GrammarParser.MatchRecogMatchesSelectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogMatchesSelection}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogMatchesSelection(@NotNull EsperEPL2GrammarParser.MatchRecogMatchesSelectionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#classIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterClassIdentifier(@NotNull EsperEPL2GrammarParser.ClassIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#classIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitClassIdentifier(@NotNull EsperEPL2GrammarParser.ClassIdentifierContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#subQueryExpr}.
	 * @param ctx the parse tree
	 */
	void enterSubQueryExpr(@NotNull EsperEPL2GrammarParser.SubQueryExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#subQueryExpr}.
	 * @param ctx the parse tree
	 */
	void exitSubQueryExpr(@NotNull EsperEPL2GrammarParser.SubQueryExprContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#databaseJoinExpression}.
	 * @param ctx the parse tree
	 */
	void enterDatabaseJoinExpression(@NotNull EsperEPL2GrammarParser.DatabaseJoinExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#databaseJoinExpression}.
	 * @param ctx the parse tree
	 */
	void exitDatabaseJoinExpression(@NotNull EsperEPL2GrammarParser.DatabaseJoinExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogDefineItem}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogDefineItem(@NotNull EsperEPL2GrammarParser.MatchRecogDefineItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogDefineItem}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogDefineItem(@NotNull EsperEPL2GrammarParser.MatchRecogDefineItemContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#libFunctionArgs}.
	 * @param ctx the parse tree
	 */
	void enterLibFunctionArgs(@NotNull EsperEPL2GrammarParser.LibFunctionArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#libFunctionArgs}.
	 * @param ctx the parse tree
	 */
	void exitLibFunctionArgs(@NotNull EsperEPL2GrammarParser.LibFunctionArgsContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#mergeUnmatchedItem}.
	 * @param ctx the parse tree
	 */
	void enterMergeUnmatchedItem(@NotNull EsperEPL2GrammarParser.MergeUnmatchedItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#mergeUnmatchedItem}.
	 * @param ctx the parse tree
	 */
	void exitMergeUnmatchedItem(@NotNull EsperEPL2GrammarParser.MergeUnmatchedItemContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#builtin_typeof}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_typeof(@NotNull EsperEPL2GrammarParser.Builtin_typeofContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#builtin_typeof}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_typeof(@NotNull EsperEPL2GrammarParser.Builtin_typeofContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#havingClause}.
	 * @param ctx the parse tree
	 */
	void enterHavingClause(@NotNull EsperEPL2GrammarParser.HavingClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#havingClause}.
	 * @param ctx the parse tree
	 */
	void exitHavingClause(@NotNull EsperEPL2GrammarParser.HavingClauseContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogMeasureItem}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogMeasureItem(@NotNull EsperEPL2GrammarParser.MatchRecogMeasureItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogMeasureItem}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogMeasureItem(@NotNull EsperEPL2GrammarParser.MatchRecogMeasureItemContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#andExpression}.
	 * @param ctx the parse tree
	 */
	void enterAndExpression(@NotNull EsperEPL2GrammarParser.AndExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#andExpression}.
	 * @param ctx the parse tree
	 */
	void exitAndExpression(@NotNull EsperEPL2GrammarParser.AndExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogMatchesInterval}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogMatchesInterval(@NotNull EsperEPL2GrammarParser.MatchRecogMatchesIntervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogMatchesInterval}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogMatchesInterval(@NotNull EsperEPL2GrammarParser.MatchRecogMatchesIntervalContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#observerExpression}.
	 * @param ctx the parse tree
	 */
	void enterObserverExpression(@NotNull EsperEPL2GrammarParser.ObserverExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#observerExpression}.
	 * @param ctx the parse tree
	 */
	void exitObserverExpression(@NotNull EsperEPL2GrammarParser.ObserverExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPatternNested}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogPatternNested(@NotNull EsperEPL2GrammarParser.MatchRecogPatternNestedContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPatternNested}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogPatternNested(@NotNull EsperEPL2GrammarParser.MatchRecogPatternNestedContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createContextFilter}.
	 * @param ctx the parse tree
	 */
	void enterCreateContextFilter(@NotNull EsperEPL2GrammarParser.CreateContextFilterContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createContextFilter}.
	 * @param ctx the parse tree
	 */
	void exitCreateContextFilter(@NotNull EsperEPL2GrammarParser.CreateContextFilterContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#evalOrExpression}.
	 * @param ctx the parse tree
	 */
	void enterEvalOrExpression(@NotNull EsperEPL2GrammarParser.EvalOrExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#evalOrExpression}.
	 * @param ctx the parse tree
	 */
	void exitEvalOrExpression(@NotNull EsperEPL2GrammarParser.EvalOrExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionDef}.
	 * @param ctx the parse tree
	 */
	void enterExpressionDef(@NotNull EsperEPL2GrammarParser.ExpressionDefContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionDef}.
	 * @param ctx the parse tree
	 */
	void exitExpressionDef(@NotNull EsperEPL2GrammarParser.ExpressionDefContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#outputLimitAndTerm}.
	 * @param ctx the parse tree
	 */
	void enterOutputLimitAndTerm(@NotNull EsperEPL2GrammarParser.OutputLimitAndTermContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#outputLimitAndTerm}.
	 * @param ctx the parse tree
	 */
	void exitOutputLimitAndTerm(@NotNull EsperEPL2GrammarParser.OutputLimitAndTermContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createTableColumnPlain}.
	 * @param ctx the parse tree
	 */
	void enterCreateTableColumnPlain(@NotNull EsperEPL2GrammarParser.CreateTableColumnPlainContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createTableColumnPlain}.
	 * @param ctx the parse tree
	 */
	void exitCreateTableColumnPlain(@NotNull EsperEPL2GrammarParser.CreateTableColumnPlainContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#groupByListChoice}.
	 * @param ctx the parse tree
	 */
	void enterGroupByListChoice(@NotNull EsperEPL2GrammarParser.GroupByListChoiceContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#groupByListChoice}.
	 * @param ctx the parse tree
	 */
	void exitGroupByListChoice(@NotNull EsperEPL2GrammarParser.GroupByListChoiceContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#numericListParameter}.
	 * @param ctx the parse tree
	 */
	void enterNumericListParameter(@NotNull EsperEPL2GrammarParser.NumericListParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#numericListParameter}.
	 * @param ctx the parse tree
	 */
	void exitNumericListParameter(@NotNull EsperEPL2GrammarParser.NumericListParameterContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#timePeriod}.
	 * @param ctx the parse tree
	 */
	void enterTimePeriod(@NotNull EsperEPL2GrammarParser.TimePeriodContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#timePeriod}.
	 * @param ctx the parse tree
	 */
	void exitTimePeriod(@NotNull EsperEPL2GrammarParser.TimePeriodContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#eventPropertyAtomic}.
	 * @param ctx the parse tree
	 */
	void enterEventPropertyAtomic(@NotNull EsperEPL2GrammarParser.EventPropertyAtomicContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#eventPropertyAtomic}.
	 * @param ctx the parse tree
	 */
	void exitEventPropertyAtomic(@NotNull EsperEPL2GrammarParser.EventPropertyAtomicContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#subSelectGroupExpression}.
	 * @param ctx the parse tree
	 */
	void enterSubSelectGroupExpression(@NotNull EsperEPL2GrammarParser.SubSelectGroupExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#subSelectGroupExpression}.
	 * @param ctx the parse tree
	 */
	void exitSubSelectGroupExpression(@NotNull EsperEPL2GrammarParser.SubSelectGroupExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#outerJoinList}.
	 * @param ctx the parse tree
	 */
	void enterOuterJoinList(@NotNull EsperEPL2GrammarParser.OuterJoinListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#outerJoinList}.
	 * @param ctx the parse tree
	 */
	void exitOuterJoinList(@NotNull EsperEPL2GrammarParser.OuterJoinListContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#selectionListElementExpr}.
	 * @param ctx the parse tree
	 */
	void enterSelectionListElementExpr(@NotNull EsperEPL2GrammarParser.SelectionListElementExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#selectionListElementExpr}.
	 * @param ctx the parse tree
	 */
	void exitSelectionListElementExpr(@NotNull EsperEPL2GrammarParser.SelectionListElementExprContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#eventFilterExpression}.
	 * @param ctx the parse tree
	 */
	void enterEventFilterExpression(@NotNull EsperEPL2GrammarParser.EventFilterExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#eventFilterExpression}.
	 * @param ctx the parse tree
	 */
	void exitEventFilterExpression(@NotNull EsperEPL2GrammarParser.EventFilterExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionWithNamedWithTime}.
	 * @param ctx the parse tree
	 */
	void enterExpressionWithNamedWithTime(@NotNull EsperEPL2GrammarParser.ExpressionWithNamedWithTimeContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionWithNamedWithTime}.
	 * @param ctx the parse tree
	 */
	void exitExpressionWithNamedWithTime(@NotNull EsperEPL2GrammarParser.ExpressionWithNamedWithTimeContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#gopParamsItemList}.
	 * @param ctx the parse tree
	 */
	void enterGopParamsItemList(@NotNull EsperEPL2GrammarParser.GopParamsItemListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#gopParamsItemList}.
	 * @param ctx the parse tree
	 */
	void exitGopParamsItemList(@NotNull EsperEPL2GrammarParser.GopParamsItemListContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPatternConcat}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogPatternConcat(@NotNull EsperEPL2GrammarParser.MatchRecogPatternConcatContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPatternConcat}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogPatternConcat(@NotNull EsperEPL2GrammarParser.MatchRecogPatternConcatContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createTableExpr}.
	 * @param ctx the parse tree
	 */
	void enterCreateTableExpr(@NotNull EsperEPL2GrammarParser.CreateTableExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createTableExpr}.
	 * @param ctx the parse tree
	 */
	void exitCreateTableExpr(@NotNull EsperEPL2GrammarParser.CreateTableExprContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#chainedFunction}.
	 * @param ctx the parse tree
	 */
	void enterChainedFunction(@NotNull EsperEPL2GrammarParser.ChainedFunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#chainedFunction}.
	 * @param ctx the parse tree
	 */
	void exitChainedFunction(@NotNull EsperEPL2GrammarParser.ChainedFunctionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#numberconstant}.
	 * @param ctx the parse tree
	 */
	void enterNumberconstant(@NotNull EsperEPL2GrammarParser.NumberconstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#numberconstant}.
	 * @param ctx the parse tree
	 */
	void exitNumberconstant(@NotNull EsperEPL2GrammarParser.NumberconstantContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#onSetAssignment}.
	 * @param ctx the parse tree
	 */
	void enterOnSetAssignment(@NotNull EsperEPL2GrammarParser.OnSetAssignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#onSetAssignment}.
	 * @param ctx the parse tree
	 */
	void exitOnSetAssignment(@NotNull EsperEPL2GrammarParser.OnSetAssignmentContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#contextContextNested}.
	 * @param ctx the parse tree
	 */
	void enterContextContextNested(@NotNull EsperEPL2GrammarParser.ContextContextNestedContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#contextContextNested}.
	 * @param ctx the parse tree
	 */
	void exitContextContextNested(@NotNull EsperEPL2GrammarParser.ContextContextNestedContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionWithTime}.
	 * @param ctx the parse tree
	 */
	void enterExpressionWithTime(@NotNull EsperEPL2GrammarParser.ExpressionWithTimeContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionWithTime}.
	 * @param ctx the parse tree
	 */
	void exitExpressionWithTime(@NotNull EsperEPL2GrammarParser.ExpressionWithTimeContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPattern}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogPattern(@NotNull EsperEPL2GrammarParser.MatchRecogPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPattern}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogPattern(@NotNull EsperEPL2GrammarParser.MatchRecogPatternContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#builtin_avedev}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_avedev(@NotNull EsperEPL2GrammarParser.Builtin_avedevContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#builtin_avedev}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_avedev(@NotNull EsperEPL2GrammarParser.Builtin_avedevContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#mergeInsert}.
	 * @param ctx the parse tree
	 */
	void enterMergeInsert(@NotNull EsperEPL2GrammarParser.MergeInsertContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#mergeInsert}.
	 * @param ctx the parse tree
	 */
	void exitMergeInsert(@NotNull EsperEPL2GrammarParser.MergeInsertContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#orderByListExpr}.
	 * @param ctx the parse tree
	 */
	void enterOrderByListExpr(@NotNull EsperEPL2GrammarParser.OrderByListExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#orderByListExpr}.
	 * @param ctx the parse tree
	 */
	void exitOrderByListExpr(@NotNull EsperEPL2GrammarParser.OrderByListExprContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#builtin_prevcount}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_prevcount(@NotNull EsperEPL2GrammarParser.Builtin_prevcountContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#builtin_prevcount}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_prevcount(@NotNull EsperEPL2GrammarParser.Builtin_prevcountContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#elementValuePairsEnum}.
	 * @param ctx the parse tree
	 */
	void enterElementValuePairsEnum(@NotNull EsperEPL2GrammarParser.ElementValuePairsEnumContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#elementValuePairsEnum}.
	 * @param ctx the parse tree
	 */
	void exitElementValuePairsEnum(@NotNull EsperEPL2GrammarParser.ElementValuePairsEnumContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#distinctExpressionAtom}.
	 * @param ctx the parse tree
	 */
	void enterDistinctExpressionAtom(@NotNull EsperEPL2GrammarParser.DistinctExpressionAtomContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#distinctExpressionAtom}.
	 * @param ctx the parse tree
	 */
	void exitDistinctExpressionAtom(@NotNull EsperEPL2GrammarParser.DistinctExpressionAtomContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(@NotNull EsperEPL2GrammarParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(@NotNull EsperEPL2GrammarParser.ExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#whereClause}.
	 * @param ctx the parse tree
	 */
	void enterWhereClause(@NotNull EsperEPL2GrammarParser.WhereClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#whereClause}.
	 * @param ctx the parse tree
	 */
	void exitWhereClause(@NotNull EsperEPL2GrammarParser.WhereClauseContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createColumnListElement}.
	 * @param ctx the parse tree
	 */
	void enterCreateColumnListElement(@NotNull EsperEPL2GrammarParser.CreateColumnListElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createColumnListElement}.
	 * @param ctx the parse tree
	 */
	void exitCreateColumnListElement(@NotNull EsperEPL2GrammarParser.CreateColumnListElementContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#gopList}.
	 * @param ctx the parse tree
	 */
	void enterGopList(@NotNull EsperEPL2GrammarParser.GopListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#gopList}.
	 * @param ctx the parse tree
	 */
	void exitGopList(@NotNull EsperEPL2GrammarParser.GopListContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#patternFilterAnnotation}.
	 * @param ctx the parse tree
	 */
	void enterPatternFilterAnnotation(@NotNull EsperEPL2GrammarParser.PatternFilterAnnotationContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#patternFilterAnnotation}.
	 * @param ctx the parse tree
	 */
	void exitPatternFilterAnnotation(@NotNull EsperEPL2GrammarParser.PatternFilterAnnotationContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#followedByRepeat}.
	 * @param ctx the parse tree
	 */
	void enterFollowedByRepeat(@NotNull EsperEPL2GrammarParser.FollowedByRepeatContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#followedByRepeat}.
	 * @param ctx the parse tree
	 */
	void exitFollowedByRepeat(@NotNull EsperEPL2GrammarParser.FollowedByRepeatContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#elementValueArrayEnum}.
	 * @param ctx the parse tree
	 */
	void enterElementValueArrayEnum(@NotNull EsperEPL2GrammarParser.ElementValueArrayEnumContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#elementValueArrayEnum}.
	 * @param ctx the parse tree
	 */
	void exitElementValueArrayEnum(@NotNull EsperEPL2GrammarParser.ElementValueArrayEnumContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#hourPart}.
	 * @param ctx the parse tree
	 */
	void enterHourPart(@NotNull EsperEPL2GrammarParser.HourPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#hourPart}.
	 * @param ctx the parse tree
	 */
	void exitHourPart(@NotNull EsperEPL2GrammarParser.HourPartContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#onDeleteExpr}.
	 * @param ctx the parse tree
	 */
	void enterOnDeleteExpr(@NotNull EsperEPL2GrammarParser.OnDeleteExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#onDeleteExpr}.
	 * @param ctx the parse tree
	 */
	void exitOnDeleteExpr(@NotNull EsperEPL2GrammarParser.OnDeleteExprContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPatternAtom}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogPatternAtom(@NotNull EsperEPL2GrammarParser.MatchRecogPatternAtomContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPatternAtom}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogPatternAtom(@NotNull EsperEPL2GrammarParser.MatchRecogPatternAtomContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#updateExpr}.
	 * @param ctx the parse tree
	 */
	void enterUpdateExpr(@NotNull EsperEPL2GrammarParser.UpdateExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#updateExpr}.
	 * @param ctx the parse tree
	 */
	void exitUpdateExpr(@NotNull EsperEPL2GrammarParser.UpdateExprContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#onSetAssignmentList}.
	 * @param ctx the parse tree
	 */
	void enterOnSetAssignmentList(@NotNull EsperEPL2GrammarParser.OnSetAssignmentListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#onSetAssignmentList}.
	 * @param ctx the parse tree
	 */
	void exitOnSetAssignmentList(@NotNull EsperEPL2GrammarParser.OnSetAssignmentListContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#frequencyOperand}.
	 * @param ctx the parse tree
	 */
	void enterFrequencyOperand(@NotNull EsperEPL2GrammarParser.FrequencyOperandContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#frequencyOperand}.
	 * @param ctx the parse tree
	 */
	void exitFrequencyOperand(@NotNull EsperEPL2GrammarParser.FrequencyOperandContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#gopOutTypeParam}.
	 * @param ctx the parse tree
	 */
	void enterGopOutTypeParam(@NotNull EsperEPL2GrammarParser.GopOutTypeParamContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#gopOutTypeParam}.
	 * @param ctx the parse tree
	 */
	void exitGopOutTypeParam(@NotNull EsperEPL2GrammarParser.GopOutTypeParamContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#mergeItem}.
	 * @param ctx the parse tree
	 */
	void enterMergeItem(@NotNull EsperEPL2GrammarParser.MergeItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#mergeItem}.
	 * @param ctx the parse tree
	 */
	void exitMergeItem(@NotNull EsperEPL2GrammarParser.MergeItemContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#substitutionCanChain}.
	 * @param ctx the parse tree
	 */
	void enterSubstitutionCanChain(@NotNull EsperEPL2GrammarParser.SubstitutionCanChainContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#substitutionCanChain}.
	 * @param ctx the parse tree
	 */
	void exitSubstitutionCanChain(@NotNull EsperEPL2GrammarParser.SubstitutionCanChainContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#yearPart}.
	 * @param ctx the parse tree
	 */
	void enterYearPart(@NotNull EsperEPL2GrammarParser.YearPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#yearPart}.
	 * @param ctx the parse tree
	 */
	void exitYearPart(@NotNull EsperEPL2GrammarParser.YearPartContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#builtin_stddev}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_stddev(@NotNull EsperEPL2GrammarParser.Builtin_stddevContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#builtin_stddev}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_stddev(@NotNull EsperEPL2GrammarParser.Builtin_stddevContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#eventPropertyOrLibFunction}.
	 * @param ctx the parse tree
	 */
	void enterEventPropertyOrLibFunction(@NotNull EsperEPL2GrammarParser.EventPropertyOrLibFunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#eventPropertyOrLibFunction}.
	 * @param ctx the parse tree
	 */
	void exitEventPropertyOrLibFunction(@NotNull EsperEPL2GrammarParser.EventPropertyOrLibFunctionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#propertyStreamSelector}.
	 * @param ctx the parse tree
	 */
	void enterPropertyStreamSelector(@NotNull EsperEPL2GrammarParser.PropertyStreamSelectorContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#propertyStreamSelector}.
	 * @param ctx the parse tree
	 */
	void exitPropertyStreamSelector(@NotNull EsperEPL2GrammarParser.PropertyStreamSelectorContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createDataflow}.
	 * @param ctx the parse tree
	 */
	void enterCreateDataflow(@NotNull EsperEPL2GrammarParser.CreateDataflowContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createDataflow}.
	 * @param ctx the parse tree
	 */
	void exitCreateDataflow(@NotNull EsperEPL2GrammarParser.CreateDataflowContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#builtin_sum}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_sum(@NotNull EsperEPL2GrammarParser.Builtin_sumContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#builtin_sum}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_sum(@NotNull EsperEPL2GrammarParser.Builtin_sumContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#insertIntoExpr}.
	 * @param ctx the parse tree
	 */
	void enterInsertIntoExpr(@NotNull EsperEPL2GrammarParser.InsertIntoExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#insertIntoExpr}.
	 * @param ctx the parse tree
	 */
	void exitInsertIntoExpr(@NotNull EsperEPL2GrammarParser.InsertIntoExprContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createVariableExpr}.
	 * @param ctx the parse tree
	 */
	void enterCreateVariableExpr(@NotNull EsperEPL2GrammarParser.CreateVariableExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createVariableExpr}.
	 * @param ctx the parse tree
	 */
	void exitCreateVariableExpr(@NotNull EsperEPL2GrammarParser.CreateVariableExprContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#followedByExpression}.
	 * @param ctx the parse tree
	 */
	void enterFollowedByExpression(@NotNull EsperEPL2GrammarParser.FollowedByExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#followedByExpression}.
	 * @param ctx the parse tree
	 */
	void exitFollowedByExpression(@NotNull EsperEPL2GrammarParser.FollowedByExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#gopParamsItem}.
	 * @param ctx the parse tree
	 */
	void enterGopParamsItem(@NotNull EsperEPL2GrammarParser.GopParamsItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#gopParamsItem}.
	 * @param ctx the parse tree
	 */
	void exitGopParamsItem(@NotNull EsperEPL2GrammarParser.GopParamsItemContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#onStreamExpr}.
	 * @param ctx the parse tree
	 */
	void enterOnStreamExpr(@NotNull EsperEPL2GrammarParser.OnStreamExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#onStreamExpr}.
	 * @param ctx the parse tree
	 */
	void exitOnStreamExpr(@NotNull EsperEPL2GrammarParser.OnStreamExprContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#propertyExpressionAtomic}.
	 * @param ctx the parse tree
	 */
	void enterPropertyExpressionAtomic(@NotNull EsperEPL2GrammarParser.PropertyExpressionAtomicContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#propertyExpressionAtomic}.
	 * @param ctx the parse tree
	 */
	void exitPropertyExpressionAtomic(@NotNull EsperEPL2GrammarParser.PropertyExpressionAtomicContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#gopDetail}.
	 * @param ctx the parse tree
	 */
	void enterGopDetail(@NotNull EsperEPL2GrammarParser.GopDetailContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#gopDetail}.
	 * @param ctx the parse tree
	 */
	void exitGopDetail(@NotNull EsperEPL2GrammarParser.GopDetailContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#gop}.
	 * @param ctx the parse tree
	 */
	void enterGop(@NotNull EsperEPL2GrammarParser.GopContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#gop}.
	 * @param ctx the parse tree
	 */
	void exitGop(@NotNull EsperEPL2GrammarParser.GopContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#outputClauseInsert}.
	 * @param ctx the parse tree
	 */
	void enterOutputClauseInsert(@NotNull EsperEPL2GrammarParser.OutputClauseInsertContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#outputClauseInsert}.
	 * @param ctx the parse tree
	 */
	void exitOutputClauseInsert(@NotNull EsperEPL2GrammarParser.OutputClauseInsertContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#eplExpression}.
	 * @param ctx the parse tree
	 */
	void enterEplExpression(@NotNull EsperEPL2GrammarParser.EplExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#eplExpression}.
	 * @param ctx the parse tree
	 */
	void exitEplExpression(@NotNull EsperEPL2GrammarParser.EplExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#onMergeExpr}.
	 * @param ctx the parse tree
	 */
	void enterOnMergeExpr(@NotNull EsperEPL2GrammarParser.OnMergeExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#onMergeExpr}.
	 * @param ctx the parse tree
	 */
	void exitOnMergeExpr(@NotNull EsperEPL2GrammarParser.OnMergeExprContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#fafUpdate}.
	 * @param ctx the parse tree
	 */
	void enterFafUpdate(@NotNull EsperEPL2GrammarParser.FafUpdateContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#fafUpdate}.
	 * @param ctx the parse tree
	 */
	void exitFafUpdate(@NotNull EsperEPL2GrammarParser.FafUpdateContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createSelectionList}.
	 * @param ctx the parse tree
	 */
	void enterCreateSelectionList(@NotNull EsperEPL2GrammarParser.CreateSelectionListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createSelectionList}.
	 * @param ctx the parse tree
	 */
	void exitCreateSelectionList(@NotNull EsperEPL2GrammarParser.CreateSelectionListContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#orExpression}.
	 * @param ctx the parse tree
	 */
	void enterOrExpression(@NotNull EsperEPL2GrammarParser.OrExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#orExpression}.
	 * @param ctx the parse tree
	 */
	void exitOrExpression(@NotNull EsperEPL2GrammarParser.OrExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#onSetExpr}.
	 * @param ctx the parse tree
	 */
	void enterOnSetExpr(@NotNull EsperEPL2GrammarParser.OnSetExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#onSetExpr}.
	 * @param ctx the parse tree
	 */
	void exitOnSetExpr(@NotNull EsperEPL2GrammarParser.OnSetExprContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#bitWiseExpression}.
	 * @param ctx the parse tree
	 */
	void enterBitWiseExpression(@NotNull EsperEPL2GrammarParser.BitWiseExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#bitWiseExpression}.
	 * @param ctx the parse tree
	 */
	void exitBitWiseExpression(@NotNull EsperEPL2GrammarParser.BitWiseExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPatternUnary}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogPatternUnary(@NotNull EsperEPL2GrammarParser.MatchRecogPatternUnaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPatternUnary}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogPatternUnary(@NotNull EsperEPL2GrammarParser.MatchRecogPatternUnaryContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#betweenList}.
	 * @param ctx the parse tree
	 */
	void enterBetweenList(@NotNull EsperEPL2GrammarParser.BetweenListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#betweenList}.
	 * @param ctx the parse tree
	 */
	void exitBetweenList(@NotNull EsperEPL2GrammarParser.BetweenListContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#secondPart}.
	 * @param ctx the parse tree
	 */
	void enterSecondPart(@NotNull EsperEPL2GrammarParser.SecondPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#secondPart}.
	 * @param ctx the parse tree
	 */
	void exitSecondPart(@NotNull EsperEPL2GrammarParser.SecondPartContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#evalEqualsExpression}.
	 * @param ctx the parse tree
	 */
	void enterEvalEqualsExpression(@NotNull EsperEPL2GrammarParser.EvalEqualsExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#evalEqualsExpression}.
	 * @param ctx the parse tree
	 */
	void exitEvalEqualsExpression(@NotNull EsperEPL2GrammarParser.EvalEqualsExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#slashIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterSlashIdentifier(@NotNull EsperEPL2GrammarParser.SlashIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#slashIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitSlashIdentifier(@NotNull EsperEPL2GrammarParser.SlashIdentifierContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#gopConfig}.
	 * @param ctx the parse tree
	 */
	void enterGopConfig(@NotNull EsperEPL2GrammarParser.GopConfigContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#gopConfig}.
	 * @param ctx the parse tree
	 */
	void exitGopConfig(@NotNull EsperEPL2GrammarParser.GopConfigContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createSelectionListElement}.
	 * @param ctx the parse tree
	 */
	void enterCreateSelectionListElement(@NotNull EsperEPL2GrammarParser.CreateSelectionListElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createSelectionListElement}.
	 * @param ctx the parse tree
	 */
	void exitCreateSelectionListElement(@NotNull EsperEPL2GrammarParser.CreateSelectionListElementContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#fafDelete}.
	 * @param ctx the parse tree
	 */
	void enterFafDelete(@NotNull EsperEPL2GrammarParser.FafDeleteContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#fafDelete}.
	 * @param ctx the parse tree
	 */
	void exitFafDelete(@NotNull EsperEPL2GrammarParser.FafDeleteContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#dayPart}.
	 * @param ctx the parse tree
	 */
	void enterDayPart(@NotNull EsperEPL2GrammarParser.DayPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#dayPart}.
	 * @param ctx the parse tree
	 */
	void exitDayPart(@NotNull EsperEPL2GrammarParser.DayPartContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#builtin_exists}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_exists(@NotNull EsperEPL2GrammarParser.Builtin_existsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#builtin_exists}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_exists(@NotNull EsperEPL2GrammarParser.Builtin_existsContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterConstant(@NotNull EsperEPL2GrammarParser.ConstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitConstant(@NotNull EsperEPL2GrammarParser.ConstantContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#mergeMatched}.
	 * @param ctx the parse tree
	 */
	void enterMergeMatched(@NotNull EsperEPL2GrammarParser.MergeMatchedContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#mergeMatched}.
	 * @param ctx the parse tree
	 */
	void exitMergeMatched(@NotNull EsperEPL2GrammarParser.MergeMatchedContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#gopOut}.
	 * @param ctx the parse tree
	 */
	void enterGopOut(@NotNull EsperEPL2GrammarParser.GopOutContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#gopOut}.
	 * @param ctx the parse tree
	 */
	void exitGopOut(@NotNull EsperEPL2GrammarParser.GopOutContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#builtin_prior}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_prior(@NotNull EsperEPL2GrammarParser.Builtin_priorContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#builtin_prior}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_prior(@NotNull EsperEPL2GrammarParser.Builtin_priorContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#guardWhereExpression}.
	 * @param ctx the parse tree
	 */
	void enterGuardWhereExpression(@NotNull EsperEPL2GrammarParser.GuardWhereExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#guardWhereExpression}.
	 * @param ctx the parse tree
	 */
	void exitGuardWhereExpression(@NotNull EsperEPL2GrammarParser.GuardWhereExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#keywordAllowedIdent}.
	 * @param ctx the parse tree
	 */
	void enterKeywordAllowedIdent(@NotNull EsperEPL2GrammarParser.KeywordAllowedIdentContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#keywordAllowedIdent}.
	 * @param ctx the parse tree
	 */
	void exitKeywordAllowedIdent(@NotNull EsperEPL2GrammarParser.KeywordAllowedIdentContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#qualifyExpression}.
	 * @param ctx the parse tree
	 */
	void enterQualifyExpression(@NotNull EsperEPL2GrammarParser.QualifyExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#qualifyExpression}.
	 * @param ctx the parse tree
	 */
	void exitQualifyExpression(@NotNull EsperEPL2GrammarParser.QualifyExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createContextGroupItem}.
	 * @param ctx the parse tree
	 */
	void enterCreateContextGroupItem(@NotNull EsperEPL2GrammarParser.CreateContextGroupItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createContextGroupItem}.
	 * @param ctx the parse tree
	 */
	void exitCreateContextGroupItem(@NotNull EsperEPL2GrammarParser.CreateContextGroupItemContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#evalAndExpression}.
	 * @param ctx the parse tree
	 */
	void enterEvalAndExpression(@NotNull EsperEPL2GrammarParser.EvalAndExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#evalAndExpression}.
	 * @param ctx the parse tree
	 */
	void exitEvalAndExpression(@NotNull EsperEPL2GrammarParser.EvalAndExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#builtin_instanceof}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_instanceof(@NotNull EsperEPL2GrammarParser.Builtin_instanceofContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#builtin_instanceof}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_instanceof(@NotNull EsperEPL2GrammarParser.Builtin_instanceofContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#multiplyExpression}.
	 * @param ctx the parse tree
	 */
	void enterMultiplyExpression(@NotNull EsperEPL2GrammarParser.MultiplyExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#multiplyExpression}.
	 * @param ctx the parse tree
	 */
	void exitMultiplyExpression(@NotNull EsperEPL2GrammarParser.MultiplyExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionNamedParameter}.
	 * @param ctx the parse tree
	 */
	void enterExpressionNamedParameter(@NotNull EsperEPL2GrammarParser.ExpressionNamedParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionNamedParameter}.
	 * @param ctx the parse tree
	 */
	void exitExpressionNamedParameter(@NotNull EsperEPL2GrammarParser.ExpressionNamedParameterContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionLambdaDecl}.
	 * @param ctx the parse tree
	 */
	void enterExpressionLambdaDecl(@NotNull EsperEPL2GrammarParser.ExpressionLambdaDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionLambdaDecl}.
	 * @param ctx the parse tree
	 */
	void exitExpressionLambdaDecl(@NotNull EsperEPL2GrammarParser.ExpressionLambdaDeclContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#propertyExpression}.
	 * @param ctx the parse tree
	 */
	void enterPropertyExpression(@NotNull EsperEPL2GrammarParser.PropertyExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#propertyExpression}.
	 * @param ctx the parse tree
	 */
	void exitPropertyExpression(@NotNull EsperEPL2GrammarParser.PropertyExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#outerJoinIdentPair}.
	 * @param ctx the parse tree
	 */
	void enterOuterJoinIdentPair(@NotNull EsperEPL2GrammarParser.OuterJoinIdentPairContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#outerJoinIdentPair}.
	 * @param ctx the parse tree
	 */
	void exitOuterJoinIdentPair(@NotNull EsperEPL2GrammarParser.OuterJoinIdentPairContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#builtin_currts}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_currts(@NotNull EsperEPL2GrammarParser.Builtin_currtsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#builtin_currts}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_currts(@NotNull EsperEPL2GrammarParser.Builtin_currtsContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#gopOutItem}.
	 * @param ctx the parse tree
	 */
	void enterGopOutItem(@NotNull EsperEPL2GrammarParser.GopOutItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#gopOutItem}.
	 * @param ctx the parse tree
	 */
	void exitGopOutItem(@NotNull EsperEPL2GrammarParser.GopOutItemContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#propertyExpressionSelect}.
	 * @param ctx the parse tree
	 */
	void enterPropertyExpressionSelect(@NotNull EsperEPL2GrammarParser.PropertyExpressionSelectContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#propertyExpressionSelect}.
	 * @param ctx the parse tree
	 */
	void exitPropertyExpressionSelect(@NotNull EsperEPL2GrammarParser.PropertyExpressionSelectContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#forExpr}.
	 * @param ctx the parse tree
	 */
	void enterForExpr(@NotNull EsperEPL2GrammarParser.ForExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#forExpr}.
	 * @param ctx the parse tree
	 */
	void exitForExpr(@NotNull EsperEPL2GrammarParser.ForExprContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionQualifyable}.
	 * @param ctx the parse tree
	 */
	void enterExpressionQualifyable(@NotNull EsperEPL2GrammarParser.ExpressionQualifyableContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionQualifyable}.
	 * @param ctx the parse tree
	 */
	void exitExpressionQualifyable(@NotNull EsperEPL2GrammarParser.ExpressionQualifyableContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionDialect}.
	 * @param ctx the parse tree
	 */
	void enterExpressionDialect(@NotNull EsperEPL2GrammarParser.ExpressionDialectContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionDialect}.
	 * @param ctx the parse tree
	 */
	void exitExpressionDialect(@NotNull EsperEPL2GrammarParser.ExpressionDialectContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#startEventPropertyRule}.
	 * @param ctx the parse tree
	 */
	void enterStartEventPropertyRule(@NotNull EsperEPL2GrammarParser.StartEventPropertyRuleContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#startEventPropertyRule}.
	 * @param ctx the parse tree
	 */
	void exitStartEventPropertyRule(@NotNull EsperEPL2GrammarParser.StartEventPropertyRuleContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#propertySelectionListElement}.
	 * @param ctx the parse tree
	 */
	void enterPropertySelectionListElement(@NotNull EsperEPL2GrammarParser.PropertySelectionListElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#propertySelectionListElement}.
	 * @param ctx the parse tree
	 */
	void exitPropertySelectionListElement(@NotNull EsperEPL2GrammarParser.PropertySelectionListElementContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionDecl}.
	 * @param ctx the parse tree
	 */
	void enterExpressionDecl(@NotNull EsperEPL2GrammarParser.ExpressionDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionDecl}.
	 * @param ctx the parse tree
	 */
	void exitExpressionDecl(@NotNull EsperEPL2GrammarParser.ExpressionDeclContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#substitution}.
	 * @param ctx the parse tree
	 */
	void enterSubstitution(@NotNull EsperEPL2GrammarParser.SubstitutionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#substitution}.
	 * @param ctx the parse tree
	 */
	void exitSubstitution(@NotNull EsperEPL2GrammarParser.SubstitutionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchUntilExpression}.
	 * @param ctx the parse tree
	 */
	void enterMatchUntilExpression(@NotNull EsperEPL2GrammarParser.MatchUntilExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchUntilExpression}.
	 * @param ctx the parse tree
	 */
	void exitMatchUntilExpression(@NotNull EsperEPL2GrammarParser.MatchUntilExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#funcIdentChained}.
	 * @param ctx the parse tree
	 */
	void enterFuncIdentChained(@NotNull EsperEPL2GrammarParser.FuncIdentChainedContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#funcIdentChained}.
	 * @param ctx the parse tree
	 */
	void exitFuncIdentChained(@NotNull EsperEPL2GrammarParser.FuncIdentChainedContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#crontabLimitParameterSet}.
	 * @param ctx the parse tree
	 */
	void enterCrontabLimitParameterSet(@NotNull EsperEPL2GrammarParser.CrontabLimitParameterSetContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#crontabLimitParameterSet}.
	 * @param ctx the parse tree
	 */
	void exitCrontabLimitParameterSet(@NotNull EsperEPL2GrammarParser.CrontabLimitParameterSetContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#weekDayOperator}.
	 * @param ctx the parse tree
	 */
	void enterWeekDayOperator(@NotNull EsperEPL2GrammarParser.WeekDayOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#weekDayOperator}.
	 * @param ctx the parse tree
	 */
	void exitWeekDayOperator(@NotNull EsperEPL2GrammarParser.WeekDayOperatorContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#whenClause}.
	 * @param ctx the parse tree
	 */
	void enterWhenClause(@NotNull EsperEPL2GrammarParser.WhenClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#whenClause}.
	 * @param ctx the parse tree
	 */
	void exitWhenClause(@NotNull EsperEPL2GrammarParser.WhenClauseContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#newAssign}.
	 * @param ctx the parse tree
	 */
	void enterNewAssign(@NotNull EsperEPL2GrammarParser.NewAssignContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#newAssign}.
	 * @param ctx the parse tree
	 */
	void exitNewAssign(@NotNull EsperEPL2GrammarParser.NewAssignContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#lastWeekdayOperand}.
	 * @param ctx the parse tree
	 */
	void enterLastWeekdayOperand(@NotNull EsperEPL2GrammarParser.LastWeekdayOperandContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#lastWeekdayOperand}.
	 * @param ctx the parse tree
	 */
	void exitLastWeekdayOperand(@NotNull EsperEPL2GrammarParser.LastWeekdayOperandContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#groupByListExpr}.
	 * @param ctx the parse tree
	 */
	void enterGroupByListExpr(@NotNull EsperEPL2GrammarParser.GroupByListExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#groupByListExpr}.
	 * @param ctx the parse tree
	 */
	void exitGroupByListExpr(@NotNull EsperEPL2GrammarParser.GroupByListExprContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#streamSelector}.
	 * @param ctx the parse tree
	 */
	void enterStreamSelector(@NotNull EsperEPL2GrammarParser.StreamSelectorContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#streamSelector}.
	 * @param ctx the parse tree
	 */
	void exitStreamSelector(@NotNull EsperEPL2GrammarParser.StreamSelectorContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#startJsonValueRule}.
	 * @param ctx the parse tree
	 */
	void enterStartJsonValueRule(@NotNull EsperEPL2GrammarParser.StartJsonValueRuleContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#startJsonValueRule}.
	 * @param ctx the parse tree
	 */
	void exitStartJsonValueRule(@NotNull EsperEPL2GrammarParser.StartJsonValueRuleContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#streamExpression}.
	 * @param ctx the parse tree
	 */
	void enterStreamExpression(@NotNull EsperEPL2GrammarParser.StreamExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#streamExpression}.
	 * @param ctx the parse tree
	 */
	void exitStreamExpression(@NotNull EsperEPL2GrammarParser.StreamExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#builtin_median}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_median(@NotNull EsperEPL2GrammarParser.Builtin_medianContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#builtin_median}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_median(@NotNull EsperEPL2GrammarParser.Builtin_medianContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#outerJoinIdent}.
	 * @param ctx the parse tree
	 */
	void enterOuterJoinIdent(@NotNull EsperEPL2GrammarParser.OuterJoinIdentContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#outerJoinIdent}.
	 * @param ctx the parse tree
	 */
	void exitOuterJoinIdent(@NotNull EsperEPL2GrammarParser.OuterJoinIdentContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createIndexColumnList}.
	 * @param ctx the parse tree
	 */
	void enterCreateIndexColumnList(@NotNull EsperEPL2GrammarParser.CreateIndexColumnListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createIndexColumnList}.
	 * @param ctx the parse tree
	 */
	void exitCreateIndexColumnList(@NotNull EsperEPL2GrammarParser.CreateIndexColumnListContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#viewExpression}.
	 * @param ctx the parse tree
	 */
	void enterViewExpression(@NotNull EsperEPL2GrammarParser.ViewExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#viewExpression}.
	 * @param ctx the parse tree
	 */
	void exitViewExpression(@NotNull EsperEPL2GrammarParser.ViewExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#patternFilterExpression}.
	 * @param ctx the parse tree
	 */
	void enterPatternFilterExpression(@NotNull EsperEPL2GrammarParser.PatternFilterExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#patternFilterExpression}.
	 * @param ctx the parse tree
	 */
	void exitPatternFilterExpression(@NotNull EsperEPL2GrammarParser.PatternFilterExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#columnList}.
	 * @param ctx the parse tree
	 */
	void enterColumnList(@NotNull EsperEPL2GrammarParser.ColumnListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#columnList}.
	 * @param ctx the parse tree
	 */
	void exitColumnList(@NotNull EsperEPL2GrammarParser.ColumnListContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#jsonpair}.
	 * @param ctx the parse tree
	 */
	void enterJsonpair(@NotNull EsperEPL2GrammarParser.JsonpairContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#jsonpair}.
	 * @param ctx the parse tree
	 */
	void exitJsonpair(@NotNull EsperEPL2GrammarParser.JsonpairContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createTableColumn}.
	 * @param ctx the parse tree
	 */
	void enterCreateTableColumn(@NotNull EsperEPL2GrammarParser.CreateTableColumnContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createTableColumn}.
	 * @param ctx the parse tree
	 */
	void exitCreateTableColumn(@NotNull EsperEPL2GrammarParser.CreateTableColumnContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#onSelectExpr}.
	 * @param ctx the parse tree
	 */
	void enterOnSelectExpr(@NotNull EsperEPL2GrammarParser.OnSelectExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#onSelectExpr}.
	 * @param ctx the parse tree
	 */
	void exitOnSelectExpr(@NotNull EsperEPL2GrammarParser.OnSelectExprContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#startPatternExpressionRule}.
	 * @param ctx the parse tree
	 */
	void enterStartPatternExpressionRule(@NotNull EsperEPL2GrammarParser.StartPatternExpressionRuleContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#startPatternExpressionRule}.
	 * @param ctx the parse tree
	 */
	void exitStartPatternExpressionRule(@NotNull EsperEPL2GrammarParser.StartPatternExpressionRuleContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#elementValuePairEnum}.
	 * @param ctx the parse tree
	 */
	void enterElementValuePairEnum(@NotNull EsperEPL2GrammarParser.ElementValuePairEnumContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#elementValuePairEnum}.
	 * @param ctx the parse tree
	 */
	void exitElementValuePairEnum(@NotNull EsperEPL2GrammarParser.ElementValuePairEnumContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#rowSubSelectExpression}.
	 * @param ctx the parse tree
	 */
	void enterRowSubSelectExpression(@NotNull EsperEPL2GrammarParser.RowSubSelectExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#rowSubSelectExpression}.
	 * @param ctx the parse tree
	 */
	void exitRowSubSelectExpression(@NotNull EsperEPL2GrammarParser.RowSubSelectExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#selectionListElementAnno}.
	 * @param ctx the parse tree
	 */
	void enterSelectionListElementAnno(@NotNull EsperEPL2GrammarParser.SelectionListElementAnnoContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#selectionListElementAnno}.
	 * @param ctx the parse tree
	 */
	void exitSelectionListElementAnno(@NotNull EsperEPL2GrammarParser.SelectionListElementAnnoContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#outputLimit}.
	 * @param ctx the parse tree
	 */
	void enterOutputLimit(@NotNull EsperEPL2GrammarParser.OutputLimitContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#outputLimit}.
	 * @param ctx the parse tree
	 */
	void exitOutputLimit(@NotNull EsperEPL2GrammarParser.OutputLimitContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createContextDistinct}.
	 * @param ctx the parse tree
	 */
	void enterCreateContextDistinct(@NotNull EsperEPL2GrammarParser.CreateContextDistinctContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createContextDistinct}.
	 * @param ctx the parse tree
	 */
	void exitCreateContextDistinct(@NotNull EsperEPL2GrammarParser.CreateContextDistinctContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#jsonelements}.
	 * @param ctx the parse tree
	 */
	void enterJsonelements(@NotNull EsperEPL2GrammarParser.JsonelementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#jsonelements}.
	 * @param ctx the parse tree
	 */
	void exitJsonelements(@NotNull EsperEPL2GrammarParser.JsonelementsContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#numericParameterList}.
	 * @param ctx the parse tree
	 */
	void enterNumericParameterList(@NotNull EsperEPL2GrammarParser.NumericParameterListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#numericParameterList}.
	 * @param ctx the parse tree
	 */
	void exitNumericParameterList(@NotNull EsperEPL2GrammarParser.NumericParameterListContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#libFunctionWithClass}.
	 * @param ctx the parse tree
	 */
	void enterLibFunctionWithClass(@NotNull EsperEPL2GrammarParser.LibFunctionWithClassContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#libFunctionWithClass}.
	 * @param ctx the parse tree
	 */
	void exitLibFunctionWithClass(@NotNull EsperEPL2GrammarParser.LibFunctionWithClassContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#propertyExpressionAnnotation}.
	 * @param ctx the parse tree
	 */
	void enterPropertyExpressionAnnotation(@NotNull EsperEPL2GrammarParser.PropertyExpressionAnnotationContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#propertyExpressionAnnotation}.
	 * @param ctx the parse tree
	 */
	void exitPropertyExpressionAnnotation(@NotNull EsperEPL2GrammarParser.PropertyExpressionAnnotationContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#stringconstant}.
	 * @param ctx the parse tree
	 */
	void enterStringconstant(@NotNull EsperEPL2GrammarParser.StringconstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#stringconstant}.
	 * @param ctx the parse tree
	 */
	void exitStringconstant(@NotNull EsperEPL2GrammarParser.StringconstantContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createSchemaExpr}.
	 * @param ctx the parse tree
	 */
	void enterCreateSchemaExpr(@NotNull EsperEPL2GrammarParser.CreateSchemaExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createSchemaExpr}.
	 * @param ctx the parse tree
	 */
	void exitCreateSchemaExpr(@NotNull EsperEPL2GrammarParser.CreateSchemaExprContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#elseClause}.
	 * @param ctx the parse tree
	 */
	void enterElseClause(@NotNull EsperEPL2GrammarParser.ElseClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#elseClause}.
	 * @param ctx the parse tree
	 */
	void exitElseClause(@NotNull EsperEPL2GrammarParser.ElseClauseContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#builtin_avg}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_avg(@NotNull EsperEPL2GrammarParser.Builtin_avgContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#builtin_avg}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_avg(@NotNull EsperEPL2GrammarParser.Builtin_avgContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#guardWhileExpression}.
	 * @param ctx the parse tree
	 */
	void enterGuardWhileExpression(@NotNull EsperEPL2GrammarParser.GuardWhileExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#guardWhileExpression}.
	 * @param ctx the parse tree
	 */
	void exitGuardWhileExpression(@NotNull EsperEPL2GrammarParser.GuardWhileExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createWindowExprModelAfter}.
	 * @param ctx the parse tree
	 */
	void enterCreateWindowExprModelAfter(@NotNull EsperEPL2GrammarParser.CreateWindowExprModelAfterContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createWindowExprModelAfter}.
	 * @param ctx the parse tree
	 */
	void exitCreateWindowExprModelAfter(@NotNull EsperEPL2GrammarParser.CreateWindowExprModelAfterContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createTableColumnList}.
	 * @param ctx the parse tree
	 */
	void enterCreateTableColumnList(@NotNull EsperEPL2GrammarParser.CreateTableColumnListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createTableColumnList}.
	 * @param ctx the parse tree
	 */
	void exitCreateTableColumnList(@NotNull EsperEPL2GrammarParser.CreateTableColumnListContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogMatchesAfterSkip}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogMatchesAfterSkip(@NotNull EsperEPL2GrammarParser.MatchRecogMatchesAfterSkipContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogMatchesAfterSkip}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogMatchesAfterSkip(@NotNull EsperEPL2GrammarParser.MatchRecogMatchesAfterSkipContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createContextDetail}.
	 * @param ctx the parse tree
	 */
	void enterCreateContextDetail(@NotNull EsperEPL2GrammarParser.CreateContextDetailContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createContextDetail}.
	 * @param ctx the parse tree
	 */
	void exitCreateContextDetail(@NotNull EsperEPL2GrammarParser.CreateContextDetailContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionNamedParameterWithTime}.
	 * @param ctx the parse tree
	 */
	void enterExpressionNamedParameterWithTime(@NotNull EsperEPL2GrammarParser.ExpressionNamedParameterWithTimeContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionNamedParameterWithTime}.
	 * @param ctx the parse tree
	 */
	void exitExpressionNamedParameterWithTime(@NotNull EsperEPL2GrammarParser.ExpressionNamedParameterWithTimeContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#monthPart}.
	 * @param ctx the parse tree
	 */
	void enterMonthPart(@NotNull EsperEPL2GrammarParser.MonthPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#monthPart}.
	 * @param ctx the parse tree
	 */
	void exitMonthPart(@NotNull EsperEPL2GrammarParser.MonthPartContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#patternExpression}.
	 * @param ctx the parse tree
	 */
	void enterPatternExpression(@NotNull EsperEPL2GrammarParser.PatternExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#patternExpression}.
	 * @param ctx the parse tree
	 */
	void exitPatternExpression(@NotNull EsperEPL2GrammarParser.PatternExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#lastOperator}.
	 * @param ctx the parse tree
	 */
	void enterLastOperator(@NotNull EsperEPL2GrammarParser.LastOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#lastOperator}.
	 * @param ctx the parse tree
	 */
	void exitLastOperator(@NotNull EsperEPL2GrammarParser.LastOperatorContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createSchemaDef}.
	 * @param ctx the parse tree
	 */
	void enterCreateSchemaDef(@NotNull EsperEPL2GrammarParser.CreateSchemaDefContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createSchemaDef}.
	 * @param ctx the parse tree
	 */
	void exitCreateSchemaDef(@NotNull EsperEPL2GrammarParser.CreateSchemaDefContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#eventPropertyIdent}.
	 * @param ctx the parse tree
	 */
	void enterEventPropertyIdent(@NotNull EsperEPL2GrammarParser.EventPropertyIdentContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#eventPropertyIdent}.
	 * @param ctx the parse tree
	 */
	void exitEventPropertyIdent(@NotNull EsperEPL2GrammarParser.EventPropertyIdentContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPatternPermute}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogPatternPermute(@NotNull EsperEPL2GrammarParser.MatchRecogPatternPermuteContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPatternPermute}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogPatternPermute(@NotNull EsperEPL2GrammarParser.MatchRecogPatternPermuteContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createIndexExpr}.
	 * @param ctx the parse tree
	 */
	void enterCreateIndexExpr(@NotNull EsperEPL2GrammarParser.CreateIndexExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createIndexExpr}.
	 * @param ctx the parse tree
	 */
	void exitCreateIndexExpr(@NotNull EsperEPL2GrammarParser.CreateIndexExprContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#atomicExpression}.
	 * @param ctx the parse tree
	 */
	void enterAtomicExpression(@NotNull EsperEPL2GrammarParser.AtomicExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#atomicExpression}.
	 * @param ctx the parse tree
	 */
	void exitAtomicExpression(@NotNull EsperEPL2GrammarParser.AtomicExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#jsonvalue}.
	 * @param ctx the parse tree
	 */
	void enterJsonvalue(@NotNull EsperEPL2GrammarParser.JsonvalueContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#jsonvalue}.
	 * @param ctx the parse tree
	 */
	void exitJsonvalue(@NotNull EsperEPL2GrammarParser.JsonvalueContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#libFunctionNoClass}.
	 * @param ctx the parse tree
	 */
	void enterLibFunctionNoClass(@NotNull EsperEPL2GrammarParser.LibFunctionNoClassContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#libFunctionNoClass}.
	 * @param ctx the parse tree
	 */
	void exitLibFunctionNoClass(@NotNull EsperEPL2GrammarParser.LibFunctionNoClassContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#elementValueEnum}.
	 * @param ctx the parse tree
	 */
	void enterElementValueEnum(@NotNull EsperEPL2GrammarParser.ElementValueEnumContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#elementValueEnum}.
	 * @param ctx the parse tree
	 */
	void exitElementValueEnum(@NotNull EsperEPL2GrammarParser.ElementValueEnumContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#builtin_cast}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_cast(@NotNull EsperEPL2GrammarParser.Builtin_castContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#builtin_cast}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_cast(@NotNull EsperEPL2GrammarParser.Builtin_castContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#onUpdateExpr}.
	 * @param ctx the parse tree
	 */
	void enterOnUpdateExpr(@NotNull EsperEPL2GrammarParser.OnUpdateExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#onUpdateExpr}.
	 * @param ctx the parse tree
	 */
	void exitOnUpdateExpr(@NotNull EsperEPL2GrammarParser.OnUpdateExprContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#annotationEnum}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationEnum(@NotNull EsperEPL2GrammarParser.AnnotationEnumContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#annotationEnum}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationEnum(@NotNull EsperEPL2GrammarParser.AnnotationEnumContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createContextExpr}.
	 * @param ctx the parse tree
	 */
	void enterCreateContextExpr(@NotNull EsperEPL2GrammarParser.CreateContextExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createContextExpr}.
	 * @param ctx the parse tree
	 */
	void exitCreateContextExpr(@NotNull EsperEPL2GrammarParser.CreateContextExprContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#lastOperand}.
	 * @param ctx the parse tree
	 */
	void enterLastOperand(@NotNull EsperEPL2GrammarParser.LastOperandContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#lastOperand}.
	 * @param ctx the parse tree
	 */
	void exitLastOperand(@NotNull EsperEPL2GrammarParser.LastOperandContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionWithTimeInclLast}.
	 * @param ctx the parse tree
	 */
	void enterExpressionWithTimeInclLast(@NotNull EsperEPL2GrammarParser.ExpressionWithTimeInclLastContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionWithTimeInclLast}.
	 * @param ctx the parse tree
	 */
	void exitExpressionWithTimeInclLast(@NotNull EsperEPL2GrammarParser.ExpressionWithTimeInclLastContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createContextPartitionItem}.
	 * @param ctx the parse tree
	 */
	void enterCreateContextPartitionItem(@NotNull EsperEPL2GrammarParser.CreateContextPartitionItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createContextPartitionItem}.
	 * @param ctx the parse tree
	 */
	void exitCreateContextPartitionItem(@NotNull EsperEPL2GrammarParser.CreateContextPartitionItemContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createWindowExpr}.
	 * @param ctx the parse tree
	 */
	void enterCreateWindowExpr(@NotNull EsperEPL2GrammarParser.CreateWindowExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createWindowExpr}.
	 * @param ctx the parse tree
	 */
	void exitCreateWindowExpr(@NotNull EsperEPL2GrammarParser.CreateWindowExprContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#builtin_cnt}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_cnt(@NotNull EsperEPL2GrammarParser.Builtin_cntContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#builtin_cnt}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_cnt(@NotNull EsperEPL2GrammarParser.Builtin_cntContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#variantListElement}.
	 * @param ctx the parse tree
	 */
	void enterVariantListElement(@NotNull EsperEPL2GrammarParser.VariantListElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#variantListElement}.
	 * @param ctx the parse tree
	 */
	void exitVariantListElement(@NotNull EsperEPL2GrammarParser.VariantListElementContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createExpressionExpr}.
	 * @param ctx the parse tree
	 */
	void enterCreateExpressionExpr(@NotNull EsperEPL2GrammarParser.CreateExpressionExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createExpressionExpr}.
	 * @param ctx the parse tree
	 */
	void exitCreateExpressionExpr(@NotNull EsperEPL2GrammarParser.CreateExpressionExprContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#rangeOperand}.
	 * @param ctx the parse tree
	 */
	void enterRangeOperand(@NotNull EsperEPL2GrammarParser.RangeOperandContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#rangeOperand}.
	 * @param ctx the parse tree
	 */
	void exitRangeOperand(@NotNull EsperEPL2GrammarParser.RangeOperandContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#guardPostFix}.
	 * @param ctx the parse tree
	 */
	void enterGuardPostFix(@NotNull EsperEPL2GrammarParser.GuardPostFixContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#guardPostFix}.
	 * @param ctx the parse tree
	 */
	void exitGuardPostFix(@NotNull EsperEPL2GrammarParser.GuardPostFixContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#groupBySetsChoice}.
	 * @param ctx the parse tree
	 */
	void enterGroupBySetsChoice(@NotNull EsperEPL2GrammarParser.GroupBySetsChoiceContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#groupBySetsChoice}.
	 * @param ctx the parse tree
	 */
	void exitGroupBySetsChoice(@NotNull EsperEPL2GrammarParser.GroupBySetsChoiceContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#inSubSelectQuery}.
	 * @param ctx the parse tree
	 */
	void enterInSubSelectQuery(@NotNull EsperEPL2GrammarParser.InSubSelectQueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#inSubSelectQuery}.
	 * @param ctx the parse tree
	 */
	void exitInSubSelectQuery(@NotNull EsperEPL2GrammarParser.InSubSelectQueryContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#escapableStr}.
	 * @param ctx the parse tree
	 */
	void enterEscapableStr(@NotNull EsperEPL2GrammarParser.EscapableStrContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#escapableStr}.
	 * @param ctx the parse tree
	 */
	void exitEscapableStr(@NotNull EsperEPL2GrammarParser.EscapableStrContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#distinctExpressionList}.
	 * @param ctx the parse tree
	 */
	void enterDistinctExpressionList(@NotNull EsperEPL2GrammarParser.DistinctExpressionListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#distinctExpressionList}.
	 * @param ctx the parse tree
	 */
	void exitDistinctExpressionList(@NotNull EsperEPL2GrammarParser.DistinctExpressionListContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterUnaryExpression(@NotNull EsperEPL2GrammarParser.UnaryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitUnaryExpression(@NotNull EsperEPL2GrammarParser.UnaryExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#onSelectInsertExpr}.
	 * @param ctx the parse tree
	 */
	void enterOnSelectInsertExpr(@NotNull EsperEPL2GrammarParser.OnSelectInsertExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#onSelectInsertExpr}.
	 * @param ctx the parse tree
	 */
	void exitOnSelectInsertExpr(@NotNull EsperEPL2GrammarParser.OnSelectInsertExprContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#selectClause}.
	 * @param ctx the parse tree
	 */
	void enterSelectClause(@NotNull EsperEPL2GrammarParser.SelectClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#selectClause}.
	 * @param ctx the parse tree
	 */
	void exitSelectClause(@NotNull EsperEPL2GrammarParser.SelectClauseContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#concatenationExpr}.
	 * @param ctx the parse tree
	 */
	void enterConcatenationExpr(@NotNull EsperEPL2GrammarParser.ConcatenationExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#concatenationExpr}.
	 * @param ctx the parse tree
	 */
	void exitConcatenationExpr(@NotNull EsperEPL2GrammarParser.ConcatenationExprContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#startEPLExpressionRule}.
	 * @param ctx the parse tree
	 */
	void enterStartEPLExpressionRule(@NotNull EsperEPL2GrammarParser.StartEPLExpressionRuleContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#startEPLExpressionRule}.
	 * @param ctx the parse tree
	 */
	void exitStartEPLExpressionRule(@NotNull EsperEPL2GrammarParser.StartEPLExpressionRuleContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#subSelectFilterExpr}.
	 * @param ctx the parse tree
	 */
	void enterSubSelectFilterExpr(@NotNull EsperEPL2GrammarParser.SubSelectFilterExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#subSelectFilterExpr}.
	 * @param ctx the parse tree
	 */
	void exitSubSelectFilterExpr(@NotNull EsperEPL2GrammarParser.SubSelectFilterExprContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createContextCoalesceItem}.
	 * @param ctx the parse tree
	 */
	void enterCreateContextCoalesceItem(@NotNull EsperEPL2GrammarParser.CreateContextCoalesceItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createContextCoalesceItem}.
	 * @param ctx the parse tree
	 */
	void exitCreateContextCoalesceItem(@NotNull EsperEPL2GrammarParser.CreateContextCoalesceItemContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#funcIdentTop}.
	 * @param ctx the parse tree
	 */
	void enterFuncIdentTop(@NotNull EsperEPL2GrammarParser.FuncIdentTopContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#funcIdentTop}.
	 * @param ctx the parse tree
	 */
	void exitFuncIdentTop(@NotNull EsperEPL2GrammarParser.FuncIdentTopContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#millisecondPart}.
	 * @param ctx the parse tree
	 */
	void enterMillisecondPart(@NotNull EsperEPL2GrammarParser.MillisecondPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#millisecondPart}.
	 * @param ctx the parse tree
	 */
	void exitMillisecondPart(@NotNull EsperEPL2GrammarParser.MillisecondPartContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#onExprFrom}.
	 * @param ctx the parse tree
	 */
	void enterOnExprFrom(@NotNull EsperEPL2GrammarParser.OnExprFromContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#onExprFrom}.
	 * @param ctx the parse tree
	 */
	void exitOnExprFrom(@NotNull EsperEPL2GrammarParser.OnExprFromContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#groupByCubeOrRollup}.
	 * @param ctx the parse tree
	 */
	void enterGroupByCubeOrRollup(@NotNull EsperEPL2GrammarParser.GroupByCubeOrRollupContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#groupByCubeOrRollup}.
	 * @param ctx the parse tree
	 */
	void exitGroupByCubeOrRollup(@NotNull EsperEPL2GrammarParser.GroupByCubeOrRollupContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#negatedExpression}.
	 * @param ctx the parse tree
	 */
	void enterNegatedExpression(@NotNull EsperEPL2GrammarParser.NegatedExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#negatedExpression}.
	 * @param ctx the parse tree
	 */
	void exitNegatedExpression(@NotNull EsperEPL2GrammarParser.NegatedExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#builtin_grouping}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_grouping(@NotNull EsperEPL2GrammarParser.Builtin_groupingContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#builtin_grouping}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_grouping(@NotNull EsperEPL2GrammarParser.Builtin_groupingContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#builtin_prev}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_prev(@NotNull EsperEPL2GrammarParser.Builtin_prevContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#builtin_prev}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_prev(@NotNull EsperEPL2GrammarParser.Builtin_prevContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#selectExpr}.
	 * @param ctx the parse tree
	 */
	void enterSelectExpr(@NotNull EsperEPL2GrammarParser.SelectExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#selectExpr}.
	 * @param ctx the parse tree
	 */
	void exitSelectExpr(@NotNull EsperEPL2GrammarParser.SelectExprContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogMeasures}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogMeasures(@NotNull EsperEPL2GrammarParser.MatchRecogMeasuresContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogMeasures}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogMeasures(@NotNull EsperEPL2GrammarParser.MatchRecogMeasuresContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#additiveExpression}.
	 * @param ctx the parse tree
	 */
	void enterAdditiveExpression(@NotNull EsperEPL2GrammarParser.AdditiveExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#additiveExpression}.
	 * @param ctx the parse tree
	 */
	void exitAdditiveExpression(@NotNull EsperEPL2GrammarParser.AdditiveExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#funcIdentInner}.
	 * @param ctx the parse tree
	 */
	void enterFuncIdentInner(@NotNull EsperEPL2GrammarParser.FuncIdentInnerContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#funcIdentInner}.
	 * @param ctx the parse tree
	 */
	void exitFuncIdentInner(@NotNull EsperEPL2GrammarParser.FuncIdentInnerContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#eventProperty}.
	 * @param ctx the parse tree
	 */
	void enterEventProperty(@NotNull EsperEPL2GrammarParser.EventPropertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#eventProperty}.
	 * @param ctx the parse tree
	 */
	void exitEventProperty(@NotNull EsperEPL2GrammarParser.EventPropertyContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#jsonarray}.
	 * @param ctx the parse tree
	 */
	void enterJsonarray(@NotNull EsperEPL2GrammarParser.JsonarrayContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#jsonarray}.
	 * @param ctx the parse tree
	 */
	void exitJsonarray(@NotNull EsperEPL2GrammarParser.JsonarrayContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#jsonobject}.
	 * @param ctx the parse tree
	 */
	void enterJsonobject(@NotNull EsperEPL2GrammarParser.JsonobjectContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#jsonobject}.
	 * @param ctx the parse tree
	 */
	void exitJsonobject(@NotNull EsperEPL2GrammarParser.JsonobjectContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#outerJoin}.
	 * @param ctx the parse tree
	 */
	void enterOuterJoin(@NotNull EsperEPL2GrammarParser.OuterJoinContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#outerJoin}.
	 * @param ctx the parse tree
	 */
	void exitOuterJoin(@NotNull EsperEPL2GrammarParser.OuterJoinContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#groupByGroupingSets}.
	 * @param ctx the parse tree
	 */
	void enterGroupByGroupingSets(@NotNull EsperEPL2GrammarParser.GroupByGroupingSetsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#groupByGroupingSets}.
	 * @param ctx the parse tree
	 */
	void exitGroupByGroupingSets(@NotNull EsperEPL2GrammarParser.GroupByGroupingSetsContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#firstLastWindowAggregation}.
	 * @param ctx the parse tree
	 */
	void enterFirstLastWindowAggregation(@NotNull EsperEPL2GrammarParser.FirstLastWindowAggregationContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#firstLastWindowAggregation}.
	 * @param ctx the parse tree
	 */
	void exitFirstLastWindowAggregation(@NotNull EsperEPL2GrammarParser.FirstLastWindowAggregationContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#escapableIdent}.
	 * @param ctx the parse tree
	 */
	void enterEscapableIdent(@NotNull EsperEPL2GrammarParser.EscapableIdentContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#escapableIdent}.
	 * @param ctx the parse tree
	 */
	void exitEscapableIdent(@NotNull EsperEPL2GrammarParser.EscapableIdentContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#fromClause}.
	 * @param ctx the parse tree
	 */
	void enterFromClause(@NotNull EsperEPL2GrammarParser.FromClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#fromClause}.
	 * @param ctx the parse tree
	 */
	void exitFromClause(@NotNull EsperEPL2GrammarParser.FromClauseContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#onExpr}.
	 * @param ctx the parse tree
	 */
	void enterOnExpr(@NotNull EsperEPL2GrammarParser.OnExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#onExpr}.
	 * @param ctx the parse tree
	 */
	void exitOnExpr(@NotNull EsperEPL2GrammarParser.OnExprContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#gopParamsItemMany}.
	 * @param ctx the parse tree
	 */
	void enterGopParamsItemMany(@NotNull EsperEPL2GrammarParser.GopParamsItemManyContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#gopParamsItemMany}.
	 * @param ctx the parse tree
	 */
	void exitGopParamsItemMany(@NotNull EsperEPL2GrammarParser.GopParamsItemManyContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#propertySelectionList}.
	 * @param ctx the parse tree
	 */
	void enterPropertySelectionList(@NotNull EsperEPL2GrammarParser.PropertySelectionListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#propertySelectionList}.
	 * @param ctx the parse tree
	 */
	void exitPropertySelectionList(@NotNull EsperEPL2GrammarParser.PropertySelectionListContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#weekPart}.
	 * @param ctx the parse tree
	 */
	void enterWeekPart(@NotNull EsperEPL2GrammarParser.WeekPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#weekPart}.
	 * @param ctx the parse tree
	 */
	void exitWeekPart(@NotNull EsperEPL2GrammarParser.WeekPartContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPatternAlteration}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogPatternAlteration(@NotNull EsperEPL2GrammarParser.MatchRecogPatternAlterationContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPatternAlteration}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogPatternAlteration(@NotNull EsperEPL2GrammarParser.MatchRecogPatternAlterationContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#gopParams}.
	 * @param ctx the parse tree
	 */
	void enterGopParams(@NotNull EsperEPL2GrammarParser.GopParamsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#gopParams}.
	 * @param ctx the parse tree
	 */
	void exitGopParams(@NotNull EsperEPL2GrammarParser.GopParamsContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionWithNamed}.
	 * @param ctx the parse tree
	 */
	void enterExpressionWithNamed(@NotNull EsperEPL2GrammarParser.ExpressionWithNamedContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionWithNamed}.
	 * @param ctx the parse tree
	 */
	void exitExpressionWithNamed(@NotNull EsperEPL2GrammarParser.ExpressionWithNamedContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#intoTableExpr}.
	 * @param ctx the parse tree
	 */
	void enterIntoTableExpr(@NotNull EsperEPL2GrammarParser.IntoTableExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#intoTableExpr}.
	 * @param ctx the parse tree
	 */
	void exitIntoTableExpr(@NotNull EsperEPL2GrammarParser.IntoTableExprContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createContextChoice}.
	 * @param ctx the parse tree
	 */
	void enterCreateContextChoice(@NotNull EsperEPL2GrammarParser.CreateContextChoiceContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createContextChoice}.
	 * @param ctx the parse tree
	 */
	void exitCreateContextChoice(@NotNull EsperEPL2GrammarParser.CreateContextChoiceContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#caseExpression}.
	 * @param ctx the parse tree
	 */
	void enterCaseExpression(@NotNull EsperEPL2GrammarParser.CaseExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#caseExpression}.
	 * @param ctx the parse tree
	 */
	void exitCaseExpression(@NotNull EsperEPL2GrammarParser.CaseExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#builtin_istream}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_istream(@NotNull EsperEPL2GrammarParser.Builtin_istreamContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#builtin_istream}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_istream(@NotNull EsperEPL2GrammarParser.Builtin_istreamContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createIndexColumn}.
	 * @param ctx the parse tree
	 */
	void enterCreateIndexColumn(@NotNull EsperEPL2GrammarParser.CreateIndexColumnContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createIndexColumn}.
	 * @param ctx the parse tree
	 */
	void exitCreateIndexColumn(@NotNull EsperEPL2GrammarParser.CreateIndexColumnContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#groupByCombinableExpr}.
	 * @param ctx the parse tree
	 */
	void enterGroupByCombinableExpr(@NotNull EsperEPL2GrammarParser.GroupByCombinableExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#groupByCombinableExpr}.
	 * @param ctx the parse tree
	 */
	void exitGroupByCombinableExpr(@NotNull EsperEPL2GrammarParser.GroupByCombinableExprContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionWithTimeList}.
	 * @param ctx the parse tree
	 */
	void enterExpressionWithTimeList(@NotNull EsperEPL2GrammarParser.ExpressionWithTimeListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionWithTimeList}.
	 * @param ctx the parse tree
	 */
	void exitExpressionWithTimeList(@NotNull EsperEPL2GrammarParser.ExpressionWithTimeListContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#gopParamsItemAs}.
	 * @param ctx the parse tree
	 */
	void enterGopParamsItemAs(@NotNull EsperEPL2GrammarParser.GopParamsItemAsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#gopParamsItemAs}.
	 * @param ctx the parse tree
	 */
	void exitGopParamsItemAs(@NotNull EsperEPL2GrammarParser.GopParamsItemAsContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#rowLimit}.
	 * @param ctx the parse tree
	 */
	void enterRowLimit(@NotNull EsperEPL2GrammarParser.RowLimitContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#rowLimit}.
	 * @param ctx the parse tree
	 */
	void exitRowLimit(@NotNull EsperEPL2GrammarParser.RowLimitContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchUntilRange}.
	 * @param ctx the parse tree
	 */
	void enterMatchUntilRange(@NotNull EsperEPL2GrammarParser.MatchUntilRangeContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchUntilRange}.
	 * @param ctx the parse tree
	 */
	void exitMatchUntilRange(@NotNull EsperEPL2GrammarParser.MatchUntilRangeContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createSchemaQual}.
	 * @param ctx the parse tree
	 */
	void enterCreateSchemaQual(@NotNull EsperEPL2GrammarParser.CreateSchemaQualContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createSchemaQual}.
	 * @param ctx the parse tree
	 */
	void exitCreateSchemaQual(@NotNull EsperEPL2GrammarParser.CreateSchemaQualContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogDefine}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogDefine(@NotNull EsperEPL2GrammarParser.MatchRecogDefineContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogDefine}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogDefine(@NotNull EsperEPL2GrammarParser.MatchRecogDefineContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#orderByListElement}.
	 * @param ctx the parse tree
	 */
	void enterOrderByListElement(@NotNull EsperEPL2GrammarParser.OrderByListElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#orderByListElement}.
	 * @param ctx the parse tree
	 */
	void exitOrderByListElement(@NotNull EsperEPL2GrammarParser.OrderByListElementContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#minutePart}.
	 * @param ctx the parse tree
	 */
	void enterMinutePart(@NotNull EsperEPL2GrammarParser.MinutePartContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#minutePart}.
	 * @param ctx the parse tree
	 */
	void exitMinutePart(@NotNull EsperEPL2GrammarParser.MinutePartContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#builtin_prevwindow}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_prevwindow(@NotNull EsperEPL2GrammarParser.Builtin_prevwindowContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#builtin_prevwindow}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_prevwindow(@NotNull EsperEPL2GrammarParser.Builtin_prevwindowContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#mergeUnmatched}.
	 * @param ctx the parse tree
	 */
	void enterMergeUnmatched(@NotNull EsperEPL2GrammarParser.MergeUnmatchedContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#mergeUnmatched}.
	 * @param ctx the parse tree
	 */
	void exitMergeUnmatched(@NotNull EsperEPL2GrammarParser.MergeUnmatchedContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#methodJoinExpression}.
	 * @param ctx the parse tree
	 */
	void enterMethodJoinExpression(@NotNull EsperEPL2GrammarParser.MethodJoinExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#methodJoinExpression}.
	 * @param ctx the parse tree
	 */
	void exitMethodJoinExpression(@NotNull EsperEPL2GrammarParser.MethodJoinExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#existsSubSelectExpression}.
	 * @param ctx the parse tree
	 */
	void enterExistsSubSelectExpression(@NotNull EsperEPL2GrammarParser.ExistsSubSelectExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#existsSubSelectExpression}.
	 * @param ctx the parse tree
	 */
	void exitExistsSubSelectExpression(@NotNull EsperEPL2GrammarParser.ExistsSubSelectExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createContextRangePoint}.
	 * @param ctx the parse tree
	 */
	void enterCreateContextRangePoint(@NotNull EsperEPL2GrammarParser.CreateContextRangePointContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createContextRangePoint}.
	 * @param ctx the parse tree
	 */
	void exitCreateContextRangePoint(@NotNull EsperEPL2GrammarParser.CreateContextRangePointContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#libFunctionArgItem}.
	 * @param ctx the parse tree
	 */
	void enterLibFunctionArgItem(@NotNull EsperEPL2GrammarParser.LibFunctionArgItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#libFunctionArgItem}.
	 * @param ctx the parse tree
	 */
	void exitLibFunctionArgItem(@NotNull EsperEPL2GrammarParser.LibFunctionArgItemContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#builtin_prevtail}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_prevtail(@NotNull EsperEPL2GrammarParser.Builtin_prevtailContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#builtin_prevtail}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_prevtail(@NotNull EsperEPL2GrammarParser.Builtin_prevtailContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#regularJoin}.
	 * @param ctx the parse tree
	 */
	void enterRegularJoin(@NotNull EsperEPL2GrammarParser.RegularJoinContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#regularJoin}.
	 * @param ctx the parse tree
	 */
	void exitRegularJoin(@NotNull EsperEPL2GrammarParser.RegularJoinContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#updateDetails}.
	 * @param ctx the parse tree
	 */
	void enterUpdateDetails(@NotNull EsperEPL2GrammarParser.UpdateDetailsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#updateDetails}.
	 * @param ctx the parse tree
	 */
	void exitUpdateDetails(@NotNull EsperEPL2GrammarParser.UpdateDetailsContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#arrayExpression}.
	 * @param ctx the parse tree
	 */
	void enterArrayExpression(@NotNull EsperEPL2GrammarParser.ArrayExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#arrayExpression}.
	 * @param ctx the parse tree
	 */
	void exitArrayExpression(@NotNull EsperEPL2GrammarParser.ArrayExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionListWithNamedWithTime}.
	 * @param ctx the parse tree
	 */
	void enterExpressionListWithNamedWithTime(@NotNull EsperEPL2GrammarParser.ExpressionListWithNamedWithTimeContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionListWithNamedWithTime}.
	 * @param ctx the parse tree
	 */
	void exitExpressionListWithNamedWithTime(@NotNull EsperEPL2GrammarParser.ExpressionListWithNamedWithTimeContext ctx);
}