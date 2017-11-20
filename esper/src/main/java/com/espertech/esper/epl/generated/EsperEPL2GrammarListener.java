// Generated from EsperEPL2Grammar.g by ANTLR 4.7

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
package com.espertech.esper.epl.generated;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link EsperEPL2GrammarParser}.
 */
public interface EsperEPL2GrammarListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#startPatternExpressionRule}.
	 * @param ctx the parse tree
	 */
	void enterStartPatternExpressionRule(EsperEPL2GrammarParser.StartPatternExpressionRuleContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#startPatternExpressionRule}.
	 * @param ctx the parse tree
	 */
	void exitStartPatternExpressionRule(EsperEPL2GrammarParser.StartPatternExpressionRuleContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#startEPLExpressionRule}.
	 * @param ctx the parse tree
	 */
	void enterStartEPLExpressionRule(EsperEPL2GrammarParser.StartEPLExpressionRuleContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#startEPLExpressionRule}.
	 * @param ctx the parse tree
	 */
	void exitStartEPLExpressionRule(EsperEPL2GrammarParser.StartEPLExpressionRuleContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#startEventPropertyRule}.
	 * @param ctx the parse tree
	 */
	void enterStartEventPropertyRule(EsperEPL2GrammarParser.StartEventPropertyRuleContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#startEventPropertyRule}.
	 * @param ctx the parse tree
	 */
	void exitStartEventPropertyRule(EsperEPL2GrammarParser.StartEventPropertyRuleContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#startJsonValueRule}.
	 * @param ctx the parse tree
	 */
	void enterStartJsonValueRule(EsperEPL2GrammarParser.StartJsonValueRuleContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#startJsonValueRule}.
	 * @param ctx the parse tree
	 */
	void exitStartJsonValueRule(EsperEPL2GrammarParser.StartJsonValueRuleContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionDecl}.
	 * @param ctx the parse tree
	 */
	void enterExpressionDecl(EsperEPL2GrammarParser.ExpressionDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionDecl}.
	 * @param ctx the parse tree
	 */
	void exitExpressionDecl(EsperEPL2GrammarParser.ExpressionDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionDialect}.
	 * @param ctx the parse tree
	 */
	void enterExpressionDialect(EsperEPL2GrammarParser.ExpressionDialectContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionDialect}.
	 * @param ctx the parse tree
	 */
	void exitExpressionDialect(EsperEPL2GrammarParser.ExpressionDialectContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionDef}.
	 * @param ctx the parse tree
	 */
	void enterExpressionDef(EsperEPL2GrammarParser.ExpressionDefContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionDef}.
	 * @param ctx the parse tree
	 */
	void exitExpressionDef(EsperEPL2GrammarParser.ExpressionDefContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionLambdaDecl}.
	 * @param ctx the parse tree
	 */
	void enterExpressionLambdaDecl(EsperEPL2GrammarParser.ExpressionLambdaDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionLambdaDecl}.
	 * @param ctx the parse tree
	 */
	void exitExpressionLambdaDecl(EsperEPL2GrammarParser.ExpressionLambdaDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionTypeAnno}.
	 * @param ctx the parse tree
	 */
	void enterExpressionTypeAnno(EsperEPL2GrammarParser.ExpressionTypeAnnoContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionTypeAnno}.
	 * @param ctx the parse tree
	 */
	void exitExpressionTypeAnno(EsperEPL2GrammarParser.ExpressionTypeAnnoContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#annotationEnum}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationEnum(EsperEPL2GrammarParser.AnnotationEnumContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#annotationEnum}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationEnum(EsperEPL2GrammarParser.AnnotationEnumContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#elementValuePairsEnum}.
	 * @param ctx the parse tree
	 */
	void enterElementValuePairsEnum(EsperEPL2GrammarParser.ElementValuePairsEnumContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#elementValuePairsEnum}.
	 * @param ctx the parse tree
	 */
	void exitElementValuePairsEnum(EsperEPL2GrammarParser.ElementValuePairsEnumContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#elementValuePairEnum}.
	 * @param ctx the parse tree
	 */
	void enterElementValuePairEnum(EsperEPL2GrammarParser.ElementValuePairEnumContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#elementValuePairEnum}.
	 * @param ctx the parse tree
	 */
	void exitElementValuePairEnum(EsperEPL2GrammarParser.ElementValuePairEnumContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#elementValueEnum}.
	 * @param ctx the parse tree
	 */
	void enterElementValueEnum(EsperEPL2GrammarParser.ElementValueEnumContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#elementValueEnum}.
	 * @param ctx the parse tree
	 */
	void exitElementValueEnum(EsperEPL2GrammarParser.ElementValueEnumContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#elementValueArrayEnum}.
	 * @param ctx the parse tree
	 */
	void enterElementValueArrayEnum(EsperEPL2GrammarParser.ElementValueArrayEnumContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#elementValueArrayEnum}.
	 * @param ctx the parse tree
	 */
	void exitElementValueArrayEnum(EsperEPL2GrammarParser.ElementValueArrayEnumContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#eplExpression}.
	 * @param ctx the parse tree
	 */
	void enterEplExpression(EsperEPL2GrammarParser.EplExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#eplExpression}.
	 * @param ctx the parse tree
	 */
	void exitEplExpression(EsperEPL2GrammarParser.EplExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#contextExpr}.
	 * @param ctx the parse tree
	 */
	void enterContextExpr(EsperEPL2GrammarParser.ContextExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#contextExpr}.
	 * @param ctx the parse tree
	 */
	void exitContextExpr(EsperEPL2GrammarParser.ContextExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#selectExpr}.
	 * @param ctx the parse tree
	 */
	void enterSelectExpr(EsperEPL2GrammarParser.SelectExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#selectExpr}.
	 * @param ctx the parse tree
	 */
	void exitSelectExpr(EsperEPL2GrammarParser.SelectExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#onExpr}.
	 * @param ctx the parse tree
	 */
	void enterOnExpr(EsperEPL2GrammarParser.OnExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#onExpr}.
	 * @param ctx the parse tree
	 */
	void exitOnExpr(EsperEPL2GrammarParser.OnExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#onStreamExpr}.
	 * @param ctx the parse tree
	 */
	void enterOnStreamExpr(EsperEPL2GrammarParser.OnStreamExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#onStreamExpr}.
	 * @param ctx the parse tree
	 */
	void exitOnStreamExpr(EsperEPL2GrammarParser.OnStreamExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#updateExpr}.
	 * @param ctx the parse tree
	 */
	void enterUpdateExpr(EsperEPL2GrammarParser.UpdateExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#updateExpr}.
	 * @param ctx the parse tree
	 */
	void exitUpdateExpr(EsperEPL2GrammarParser.UpdateExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#updateDetails}.
	 * @param ctx the parse tree
	 */
	void enterUpdateDetails(EsperEPL2GrammarParser.UpdateDetailsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#updateDetails}.
	 * @param ctx the parse tree
	 */
	void exitUpdateDetails(EsperEPL2GrammarParser.UpdateDetailsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#onMergeExpr}.
	 * @param ctx the parse tree
	 */
	void enterOnMergeExpr(EsperEPL2GrammarParser.OnMergeExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#onMergeExpr}.
	 * @param ctx the parse tree
	 */
	void exitOnMergeExpr(EsperEPL2GrammarParser.OnMergeExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#mergeItem}.
	 * @param ctx the parse tree
	 */
	void enterMergeItem(EsperEPL2GrammarParser.MergeItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#mergeItem}.
	 * @param ctx the parse tree
	 */
	void exitMergeItem(EsperEPL2GrammarParser.MergeItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#mergeMatched}.
	 * @param ctx the parse tree
	 */
	void enterMergeMatched(EsperEPL2GrammarParser.MergeMatchedContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#mergeMatched}.
	 * @param ctx the parse tree
	 */
	void exitMergeMatched(EsperEPL2GrammarParser.MergeMatchedContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#mergeMatchedItem}.
	 * @param ctx the parse tree
	 */
	void enterMergeMatchedItem(EsperEPL2GrammarParser.MergeMatchedItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#mergeMatchedItem}.
	 * @param ctx the parse tree
	 */
	void exitMergeMatchedItem(EsperEPL2GrammarParser.MergeMatchedItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#onMergeDirectInsert}.
	 * @param ctx the parse tree
	 */
	void enterOnMergeDirectInsert(EsperEPL2GrammarParser.OnMergeDirectInsertContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#onMergeDirectInsert}.
	 * @param ctx the parse tree
	 */
	void exitOnMergeDirectInsert(EsperEPL2GrammarParser.OnMergeDirectInsertContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#mergeUnmatched}.
	 * @param ctx the parse tree
	 */
	void enterMergeUnmatched(EsperEPL2GrammarParser.MergeUnmatchedContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#mergeUnmatched}.
	 * @param ctx the parse tree
	 */
	void exitMergeUnmatched(EsperEPL2GrammarParser.MergeUnmatchedContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#mergeUnmatchedItem}.
	 * @param ctx the parse tree
	 */
	void enterMergeUnmatchedItem(EsperEPL2GrammarParser.MergeUnmatchedItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#mergeUnmatchedItem}.
	 * @param ctx the parse tree
	 */
	void exitMergeUnmatchedItem(EsperEPL2GrammarParser.MergeUnmatchedItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#mergeInsert}.
	 * @param ctx the parse tree
	 */
	void enterMergeInsert(EsperEPL2GrammarParser.MergeInsertContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#mergeInsert}.
	 * @param ctx the parse tree
	 */
	void exitMergeInsert(EsperEPL2GrammarParser.MergeInsertContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#onSelectExpr}.
	 * @param ctx the parse tree
	 */
	void enterOnSelectExpr(EsperEPL2GrammarParser.OnSelectExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#onSelectExpr}.
	 * @param ctx the parse tree
	 */
	void exitOnSelectExpr(EsperEPL2GrammarParser.OnSelectExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#onUpdateExpr}.
	 * @param ctx the parse tree
	 */
	void enterOnUpdateExpr(EsperEPL2GrammarParser.OnUpdateExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#onUpdateExpr}.
	 * @param ctx the parse tree
	 */
	void exitOnUpdateExpr(EsperEPL2GrammarParser.OnUpdateExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#onSelectInsertExpr}.
	 * @param ctx the parse tree
	 */
	void enterOnSelectInsertExpr(EsperEPL2GrammarParser.OnSelectInsertExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#onSelectInsertExpr}.
	 * @param ctx the parse tree
	 */
	void exitOnSelectInsertExpr(EsperEPL2GrammarParser.OnSelectInsertExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#onSelectInsertFromClause}.
	 * @param ctx the parse tree
	 */
	void enterOnSelectInsertFromClause(EsperEPL2GrammarParser.OnSelectInsertFromClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#onSelectInsertFromClause}.
	 * @param ctx the parse tree
	 */
	void exitOnSelectInsertFromClause(EsperEPL2GrammarParser.OnSelectInsertFromClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#outputClauseInsert}.
	 * @param ctx the parse tree
	 */
	void enterOutputClauseInsert(EsperEPL2GrammarParser.OutputClauseInsertContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#outputClauseInsert}.
	 * @param ctx the parse tree
	 */
	void exitOutputClauseInsert(EsperEPL2GrammarParser.OutputClauseInsertContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#onDeleteExpr}.
	 * @param ctx the parse tree
	 */
	void enterOnDeleteExpr(EsperEPL2GrammarParser.OnDeleteExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#onDeleteExpr}.
	 * @param ctx the parse tree
	 */
	void exitOnDeleteExpr(EsperEPL2GrammarParser.OnDeleteExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#onSetExpr}.
	 * @param ctx the parse tree
	 */
	void enterOnSetExpr(EsperEPL2GrammarParser.OnSetExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#onSetExpr}.
	 * @param ctx the parse tree
	 */
	void exitOnSetExpr(EsperEPL2GrammarParser.OnSetExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#onSetAssignmentList}.
	 * @param ctx the parse tree
	 */
	void enterOnSetAssignmentList(EsperEPL2GrammarParser.OnSetAssignmentListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#onSetAssignmentList}.
	 * @param ctx the parse tree
	 */
	void exitOnSetAssignmentList(EsperEPL2GrammarParser.OnSetAssignmentListContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#onSetAssignment}.
	 * @param ctx the parse tree
	 */
	void enterOnSetAssignment(EsperEPL2GrammarParser.OnSetAssignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#onSetAssignment}.
	 * @param ctx the parse tree
	 */
	void exitOnSetAssignment(EsperEPL2GrammarParser.OnSetAssignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#onExprFrom}.
	 * @param ctx the parse tree
	 */
	void enterOnExprFrom(EsperEPL2GrammarParser.OnExprFromContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#onExprFrom}.
	 * @param ctx the parse tree
	 */
	void exitOnExprFrom(EsperEPL2GrammarParser.OnExprFromContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createWindowExpr}.
	 * @param ctx the parse tree
	 */
	void enterCreateWindowExpr(EsperEPL2GrammarParser.CreateWindowExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createWindowExpr}.
	 * @param ctx the parse tree
	 */
	void exitCreateWindowExpr(EsperEPL2GrammarParser.CreateWindowExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createWindowExprModelAfter}.
	 * @param ctx the parse tree
	 */
	void enterCreateWindowExprModelAfter(EsperEPL2GrammarParser.CreateWindowExprModelAfterContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createWindowExprModelAfter}.
	 * @param ctx the parse tree
	 */
	void exitCreateWindowExprModelAfter(EsperEPL2GrammarParser.CreateWindowExprModelAfterContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createIndexExpr}.
	 * @param ctx the parse tree
	 */
	void enterCreateIndexExpr(EsperEPL2GrammarParser.CreateIndexExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createIndexExpr}.
	 * @param ctx the parse tree
	 */
	void exitCreateIndexExpr(EsperEPL2GrammarParser.CreateIndexExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createIndexColumnList}.
	 * @param ctx the parse tree
	 */
	void enterCreateIndexColumnList(EsperEPL2GrammarParser.CreateIndexColumnListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createIndexColumnList}.
	 * @param ctx the parse tree
	 */
	void exitCreateIndexColumnList(EsperEPL2GrammarParser.CreateIndexColumnListContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createIndexColumn}.
	 * @param ctx the parse tree
	 */
	void enterCreateIndexColumn(EsperEPL2GrammarParser.CreateIndexColumnContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createIndexColumn}.
	 * @param ctx the parse tree
	 */
	void exitCreateIndexColumn(EsperEPL2GrammarParser.CreateIndexColumnContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createVariableExpr}.
	 * @param ctx the parse tree
	 */
	void enterCreateVariableExpr(EsperEPL2GrammarParser.CreateVariableExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createVariableExpr}.
	 * @param ctx the parse tree
	 */
	void exitCreateVariableExpr(EsperEPL2GrammarParser.CreateVariableExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createTableExpr}.
	 * @param ctx the parse tree
	 */
	void enterCreateTableExpr(EsperEPL2GrammarParser.CreateTableExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createTableExpr}.
	 * @param ctx the parse tree
	 */
	void exitCreateTableExpr(EsperEPL2GrammarParser.CreateTableExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createTableColumnList}.
	 * @param ctx the parse tree
	 */
	void enterCreateTableColumnList(EsperEPL2GrammarParser.CreateTableColumnListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createTableColumnList}.
	 * @param ctx the parse tree
	 */
	void exitCreateTableColumnList(EsperEPL2GrammarParser.CreateTableColumnListContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createTableColumn}.
	 * @param ctx the parse tree
	 */
	void enterCreateTableColumn(EsperEPL2GrammarParser.CreateTableColumnContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createTableColumn}.
	 * @param ctx the parse tree
	 */
	void exitCreateTableColumn(EsperEPL2GrammarParser.CreateTableColumnContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createTableColumnPlain}.
	 * @param ctx the parse tree
	 */
	void enterCreateTableColumnPlain(EsperEPL2GrammarParser.CreateTableColumnPlainContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createTableColumnPlain}.
	 * @param ctx the parse tree
	 */
	void exitCreateTableColumnPlain(EsperEPL2GrammarParser.CreateTableColumnPlainContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createColumnList}.
	 * @param ctx the parse tree
	 */
	void enterCreateColumnList(EsperEPL2GrammarParser.CreateColumnListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createColumnList}.
	 * @param ctx the parse tree
	 */
	void exitCreateColumnList(EsperEPL2GrammarParser.CreateColumnListContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createColumnListElement}.
	 * @param ctx the parse tree
	 */
	void enterCreateColumnListElement(EsperEPL2GrammarParser.CreateColumnListElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createColumnListElement}.
	 * @param ctx the parse tree
	 */
	void exitCreateColumnListElement(EsperEPL2GrammarParser.CreateColumnListElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createSelectionList}.
	 * @param ctx the parse tree
	 */
	void enterCreateSelectionList(EsperEPL2GrammarParser.CreateSelectionListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createSelectionList}.
	 * @param ctx the parse tree
	 */
	void exitCreateSelectionList(EsperEPL2GrammarParser.CreateSelectionListContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createSelectionListElement}.
	 * @param ctx the parse tree
	 */
	void enterCreateSelectionListElement(EsperEPL2GrammarParser.CreateSelectionListElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createSelectionListElement}.
	 * @param ctx the parse tree
	 */
	void exitCreateSelectionListElement(EsperEPL2GrammarParser.CreateSelectionListElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createSchemaExpr}.
	 * @param ctx the parse tree
	 */
	void enterCreateSchemaExpr(EsperEPL2GrammarParser.CreateSchemaExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createSchemaExpr}.
	 * @param ctx the parse tree
	 */
	void exitCreateSchemaExpr(EsperEPL2GrammarParser.CreateSchemaExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createSchemaDef}.
	 * @param ctx the parse tree
	 */
	void enterCreateSchemaDef(EsperEPL2GrammarParser.CreateSchemaDefContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createSchemaDef}.
	 * @param ctx the parse tree
	 */
	void exitCreateSchemaDef(EsperEPL2GrammarParser.CreateSchemaDefContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#fafDelete}.
	 * @param ctx the parse tree
	 */
	void enterFafDelete(EsperEPL2GrammarParser.FafDeleteContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#fafDelete}.
	 * @param ctx the parse tree
	 */
	void exitFafDelete(EsperEPL2GrammarParser.FafDeleteContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#fafUpdate}.
	 * @param ctx the parse tree
	 */
	void enterFafUpdate(EsperEPL2GrammarParser.FafUpdateContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#fafUpdate}.
	 * @param ctx the parse tree
	 */
	void exitFafUpdate(EsperEPL2GrammarParser.FafUpdateContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#fafInsert}.
	 * @param ctx the parse tree
	 */
	void enterFafInsert(EsperEPL2GrammarParser.FafInsertContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#fafInsert}.
	 * @param ctx the parse tree
	 */
	void exitFafInsert(EsperEPL2GrammarParser.FafInsertContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createDataflow}.
	 * @param ctx the parse tree
	 */
	void enterCreateDataflow(EsperEPL2GrammarParser.CreateDataflowContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createDataflow}.
	 * @param ctx the parse tree
	 */
	void exitCreateDataflow(EsperEPL2GrammarParser.CreateDataflowContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#gopList}.
	 * @param ctx the parse tree
	 */
	void enterGopList(EsperEPL2GrammarParser.GopListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#gopList}.
	 * @param ctx the parse tree
	 */
	void exitGopList(EsperEPL2GrammarParser.GopListContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#gop}.
	 * @param ctx the parse tree
	 */
	void enterGop(EsperEPL2GrammarParser.GopContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#gop}.
	 * @param ctx the parse tree
	 */
	void exitGop(EsperEPL2GrammarParser.GopContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#gopParams}.
	 * @param ctx the parse tree
	 */
	void enterGopParams(EsperEPL2GrammarParser.GopParamsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#gopParams}.
	 * @param ctx the parse tree
	 */
	void exitGopParams(EsperEPL2GrammarParser.GopParamsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#gopParamsItemList}.
	 * @param ctx the parse tree
	 */
	void enterGopParamsItemList(EsperEPL2GrammarParser.GopParamsItemListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#gopParamsItemList}.
	 * @param ctx the parse tree
	 */
	void exitGopParamsItemList(EsperEPL2GrammarParser.GopParamsItemListContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#gopParamsItem}.
	 * @param ctx the parse tree
	 */
	void enterGopParamsItem(EsperEPL2GrammarParser.GopParamsItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#gopParamsItem}.
	 * @param ctx the parse tree
	 */
	void exitGopParamsItem(EsperEPL2GrammarParser.GopParamsItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#gopParamsItemMany}.
	 * @param ctx the parse tree
	 */
	void enterGopParamsItemMany(EsperEPL2GrammarParser.GopParamsItemManyContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#gopParamsItemMany}.
	 * @param ctx the parse tree
	 */
	void exitGopParamsItemMany(EsperEPL2GrammarParser.GopParamsItemManyContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#gopParamsItemAs}.
	 * @param ctx the parse tree
	 */
	void enterGopParamsItemAs(EsperEPL2GrammarParser.GopParamsItemAsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#gopParamsItemAs}.
	 * @param ctx the parse tree
	 */
	void exitGopParamsItemAs(EsperEPL2GrammarParser.GopParamsItemAsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#gopOut}.
	 * @param ctx the parse tree
	 */
	void enterGopOut(EsperEPL2GrammarParser.GopOutContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#gopOut}.
	 * @param ctx the parse tree
	 */
	void exitGopOut(EsperEPL2GrammarParser.GopOutContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#gopOutItem}.
	 * @param ctx the parse tree
	 */
	void enterGopOutItem(EsperEPL2GrammarParser.GopOutItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#gopOutItem}.
	 * @param ctx the parse tree
	 */
	void exitGopOutItem(EsperEPL2GrammarParser.GopOutItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#gopOutTypeList}.
	 * @param ctx the parse tree
	 */
	void enterGopOutTypeList(EsperEPL2GrammarParser.GopOutTypeListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#gopOutTypeList}.
	 * @param ctx the parse tree
	 */
	void exitGopOutTypeList(EsperEPL2GrammarParser.GopOutTypeListContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#gopOutTypeParam}.
	 * @param ctx the parse tree
	 */
	void enterGopOutTypeParam(EsperEPL2GrammarParser.GopOutTypeParamContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#gopOutTypeParam}.
	 * @param ctx the parse tree
	 */
	void exitGopOutTypeParam(EsperEPL2GrammarParser.GopOutTypeParamContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#gopOutTypeItem}.
	 * @param ctx the parse tree
	 */
	void enterGopOutTypeItem(EsperEPL2GrammarParser.GopOutTypeItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#gopOutTypeItem}.
	 * @param ctx the parse tree
	 */
	void exitGopOutTypeItem(EsperEPL2GrammarParser.GopOutTypeItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#gopDetail}.
	 * @param ctx the parse tree
	 */
	void enterGopDetail(EsperEPL2GrammarParser.GopDetailContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#gopDetail}.
	 * @param ctx the parse tree
	 */
	void exitGopDetail(EsperEPL2GrammarParser.GopDetailContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#gopConfig}.
	 * @param ctx the parse tree
	 */
	void enterGopConfig(EsperEPL2GrammarParser.GopConfigContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#gopConfig}.
	 * @param ctx the parse tree
	 */
	void exitGopConfig(EsperEPL2GrammarParser.GopConfigContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createContextExpr}.
	 * @param ctx the parse tree
	 */
	void enterCreateContextExpr(EsperEPL2GrammarParser.CreateContextExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createContextExpr}.
	 * @param ctx the parse tree
	 */
	void exitCreateContextExpr(EsperEPL2GrammarParser.CreateContextExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createExpressionExpr}.
	 * @param ctx the parse tree
	 */
	void enterCreateExpressionExpr(EsperEPL2GrammarParser.CreateExpressionExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createExpressionExpr}.
	 * @param ctx the parse tree
	 */
	void exitCreateExpressionExpr(EsperEPL2GrammarParser.CreateExpressionExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createContextDetail}.
	 * @param ctx the parse tree
	 */
	void enterCreateContextDetail(EsperEPL2GrammarParser.CreateContextDetailContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createContextDetail}.
	 * @param ctx the parse tree
	 */
	void exitCreateContextDetail(EsperEPL2GrammarParser.CreateContextDetailContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#contextContextNested}.
	 * @param ctx the parse tree
	 */
	void enterContextContextNested(EsperEPL2GrammarParser.ContextContextNestedContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#contextContextNested}.
	 * @param ctx the parse tree
	 */
	void exitContextContextNested(EsperEPL2GrammarParser.ContextContextNestedContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createContextChoice}.
	 * @param ctx the parse tree
	 */
	void enterCreateContextChoice(EsperEPL2GrammarParser.CreateContextChoiceContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createContextChoice}.
	 * @param ctx the parse tree
	 */
	void exitCreateContextChoice(EsperEPL2GrammarParser.CreateContextChoiceContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createContextDistinct}.
	 * @param ctx the parse tree
	 */
	void enterCreateContextDistinct(EsperEPL2GrammarParser.CreateContextDistinctContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createContextDistinct}.
	 * @param ctx the parse tree
	 */
	void exitCreateContextDistinct(EsperEPL2GrammarParser.CreateContextDistinctContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createContextRangePoint}.
	 * @param ctx the parse tree
	 */
	void enterCreateContextRangePoint(EsperEPL2GrammarParser.CreateContextRangePointContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createContextRangePoint}.
	 * @param ctx the parse tree
	 */
	void exitCreateContextRangePoint(EsperEPL2GrammarParser.CreateContextRangePointContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createContextFilter}.
	 * @param ctx the parse tree
	 */
	void enterCreateContextFilter(EsperEPL2GrammarParser.CreateContextFilterContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createContextFilter}.
	 * @param ctx the parse tree
	 */
	void exitCreateContextFilter(EsperEPL2GrammarParser.CreateContextFilterContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createContextPartitionItem}.
	 * @param ctx the parse tree
	 */
	void enterCreateContextPartitionItem(EsperEPL2GrammarParser.CreateContextPartitionItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createContextPartitionItem}.
	 * @param ctx the parse tree
	 */
	void exitCreateContextPartitionItem(EsperEPL2GrammarParser.CreateContextPartitionItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createContextCoalesceItem}.
	 * @param ctx the parse tree
	 */
	void enterCreateContextCoalesceItem(EsperEPL2GrammarParser.CreateContextCoalesceItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createContextCoalesceItem}.
	 * @param ctx the parse tree
	 */
	void exitCreateContextCoalesceItem(EsperEPL2GrammarParser.CreateContextCoalesceItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createContextGroupItem}.
	 * @param ctx the parse tree
	 */
	void enterCreateContextGroupItem(EsperEPL2GrammarParser.CreateContextGroupItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createContextGroupItem}.
	 * @param ctx the parse tree
	 */
	void exitCreateContextGroupItem(EsperEPL2GrammarParser.CreateContextGroupItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createContextPartitionInit}.
	 * @param ctx the parse tree
	 */
	void enterCreateContextPartitionInit(EsperEPL2GrammarParser.CreateContextPartitionInitContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createContextPartitionInit}.
	 * @param ctx the parse tree
	 */
	void exitCreateContextPartitionInit(EsperEPL2GrammarParser.CreateContextPartitionInitContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createContextPartitionTerm}.
	 * @param ctx the parse tree
	 */
	void enterCreateContextPartitionTerm(EsperEPL2GrammarParser.CreateContextPartitionTermContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createContextPartitionTerm}.
	 * @param ctx the parse tree
	 */
	void exitCreateContextPartitionTerm(EsperEPL2GrammarParser.CreateContextPartitionTermContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#createSchemaQual}.
	 * @param ctx the parse tree
	 */
	void enterCreateSchemaQual(EsperEPL2GrammarParser.CreateSchemaQualContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#createSchemaQual}.
	 * @param ctx the parse tree
	 */
	void exitCreateSchemaQual(EsperEPL2GrammarParser.CreateSchemaQualContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#variantList}.
	 * @param ctx the parse tree
	 */
	void enterVariantList(EsperEPL2GrammarParser.VariantListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#variantList}.
	 * @param ctx the parse tree
	 */
	void exitVariantList(EsperEPL2GrammarParser.VariantListContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#variantListElement}.
	 * @param ctx the parse tree
	 */
	void enterVariantListElement(EsperEPL2GrammarParser.VariantListElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#variantListElement}.
	 * @param ctx the parse tree
	 */
	void exitVariantListElement(EsperEPL2GrammarParser.VariantListElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#intoTableExpr}.
	 * @param ctx the parse tree
	 */
	void enterIntoTableExpr(EsperEPL2GrammarParser.IntoTableExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#intoTableExpr}.
	 * @param ctx the parse tree
	 */
	void exitIntoTableExpr(EsperEPL2GrammarParser.IntoTableExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#insertIntoExpr}.
	 * @param ctx the parse tree
	 */
	void enterInsertIntoExpr(EsperEPL2GrammarParser.InsertIntoExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#insertIntoExpr}.
	 * @param ctx the parse tree
	 */
	void exitInsertIntoExpr(EsperEPL2GrammarParser.InsertIntoExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#columnList}.
	 * @param ctx the parse tree
	 */
	void enterColumnList(EsperEPL2GrammarParser.ColumnListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#columnList}.
	 * @param ctx the parse tree
	 */
	void exitColumnList(EsperEPL2GrammarParser.ColumnListContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#fromClause}.
	 * @param ctx the parse tree
	 */
	void enterFromClause(EsperEPL2GrammarParser.FromClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#fromClause}.
	 * @param ctx the parse tree
	 */
	void exitFromClause(EsperEPL2GrammarParser.FromClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#regularJoin}.
	 * @param ctx the parse tree
	 */
	void enterRegularJoin(EsperEPL2GrammarParser.RegularJoinContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#regularJoin}.
	 * @param ctx the parse tree
	 */
	void exitRegularJoin(EsperEPL2GrammarParser.RegularJoinContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#outerJoinList}.
	 * @param ctx the parse tree
	 */
	void enterOuterJoinList(EsperEPL2GrammarParser.OuterJoinListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#outerJoinList}.
	 * @param ctx the parse tree
	 */
	void exitOuterJoinList(EsperEPL2GrammarParser.OuterJoinListContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#outerJoin}.
	 * @param ctx the parse tree
	 */
	void enterOuterJoin(EsperEPL2GrammarParser.OuterJoinContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#outerJoin}.
	 * @param ctx the parse tree
	 */
	void exitOuterJoin(EsperEPL2GrammarParser.OuterJoinContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#outerJoinIdent}.
	 * @param ctx the parse tree
	 */
	void enterOuterJoinIdent(EsperEPL2GrammarParser.OuterJoinIdentContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#outerJoinIdent}.
	 * @param ctx the parse tree
	 */
	void exitOuterJoinIdent(EsperEPL2GrammarParser.OuterJoinIdentContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#outerJoinIdentPair}.
	 * @param ctx the parse tree
	 */
	void enterOuterJoinIdentPair(EsperEPL2GrammarParser.OuterJoinIdentPairContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#outerJoinIdentPair}.
	 * @param ctx the parse tree
	 */
	void exitOuterJoinIdentPair(EsperEPL2GrammarParser.OuterJoinIdentPairContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#whereClause}.
	 * @param ctx the parse tree
	 */
	void enterWhereClause(EsperEPL2GrammarParser.WhereClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#whereClause}.
	 * @param ctx the parse tree
	 */
	void exitWhereClause(EsperEPL2GrammarParser.WhereClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#selectClause}.
	 * @param ctx the parse tree
	 */
	void enterSelectClause(EsperEPL2GrammarParser.SelectClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#selectClause}.
	 * @param ctx the parse tree
	 */
	void exitSelectClause(EsperEPL2GrammarParser.SelectClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#selectionList}.
	 * @param ctx the parse tree
	 */
	void enterSelectionList(EsperEPL2GrammarParser.SelectionListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#selectionList}.
	 * @param ctx the parse tree
	 */
	void exitSelectionList(EsperEPL2GrammarParser.SelectionListContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#selectionListElement}.
	 * @param ctx the parse tree
	 */
	void enterSelectionListElement(EsperEPL2GrammarParser.SelectionListElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#selectionListElement}.
	 * @param ctx the parse tree
	 */
	void exitSelectionListElement(EsperEPL2GrammarParser.SelectionListElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#selectionListElementExpr}.
	 * @param ctx the parse tree
	 */
	void enterSelectionListElementExpr(EsperEPL2GrammarParser.SelectionListElementExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#selectionListElementExpr}.
	 * @param ctx the parse tree
	 */
	void exitSelectionListElementExpr(EsperEPL2GrammarParser.SelectionListElementExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#selectionListElementAnno}.
	 * @param ctx the parse tree
	 */
	void enterSelectionListElementAnno(EsperEPL2GrammarParser.SelectionListElementAnnoContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#selectionListElementAnno}.
	 * @param ctx the parse tree
	 */
	void exitSelectionListElementAnno(EsperEPL2GrammarParser.SelectionListElementAnnoContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#streamSelector}.
	 * @param ctx the parse tree
	 */
	void enterStreamSelector(EsperEPL2GrammarParser.StreamSelectorContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#streamSelector}.
	 * @param ctx the parse tree
	 */
	void exitStreamSelector(EsperEPL2GrammarParser.StreamSelectorContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#streamExpression}.
	 * @param ctx the parse tree
	 */
	void enterStreamExpression(EsperEPL2GrammarParser.StreamExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#streamExpression}.
	 * @param ctx the parse tree
	 */
	void exitStreamExpression(EsperEPL2GrammarParser.StreamExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#forExpr}.
	 * @param ctx the parse tree
	 */
	void enterForExpr(EsperEPL2GrammarParser.ForExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#forExpr}.
	 * @param ctx the parse tree
	 */
	void exitForExpr(EsperEPL2GrammarParser.ForExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#patternInclusionExpression}.
	 * @param ctx the parse tree
	 */
	void enterPatternInclusionExpression(EsperEPL2GrammarParser.PatternInclusionExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#patternInclusionExpression}.
	 * @param ctx the parse tree
	 */
	void exitPatternInclusionExpression(EsperEPL2GrammarParser.PatternInclusionExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#databaseJoinExpression}.
	 * @param ctx the parse tree
	 */
	void enterDatabaseJoinExpression(EsperEPL2GrammarParser.DatabaseJoinExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#databaseJoinExpression}.
	 * @param ctx the parse tree
	 */
	void exitDatabaseJoinExpression(EsperEPL2GrammarParser.DatabaseJoinExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#methodJoinExpression}.
	 * @param ctx the parse tree
	 */
	void enterMethodJoinExpression(EsperEPL2GrammarParser.MethodJoinExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#methodJoinExpression}.
	 * @param ctx the parse tree
	 */
	void exitMethodJoinExpression(EsperEPL2GrammarParser.MethodJoinExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#viewExpressions}.
	 * @param ctx the parse tree
	 */
	void enterViewExpressions(EsperEPL2GrammarParser.ViewExpressionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#viewExpressions}.
	 * @param ctx the parse tree
	 */
	void exitViewExpressions(EsperEPL2GrammarParser.ViewExpressionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#viewExpressionWNamespace}.
	 * @param ctx the parse tree
	 */
	void enterViewExpressionWNamespace(EsperEPL2GrammarParser.ViewExpressionWNamespaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#viewExpressionWNamespace}.
	 * @param ctx the parse tree
	 */
	void exitViewExpressionWNamespace(EsperEPL2GrammarParser.ViewExpressionWNamespaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#viewExpressionOptNamespace}.
	 * @param ctx the parse tree
	 */
	void enterViewExpressionOptNamespace(EsperEPL2GrammarParser.ViewExpressionOptNamespaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#viewExpressionOptNamespace}.
	 * @param ctx the parse tree
	 */
	void exitViewExpressionOptNamespace(EsperEPL2GrammarParser.ViewExpressionOptNamespaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#viewWParameters}.
	 * @param ctx the parse tree
	 */
	void enterViewWParameters(EsperEPL2GrammarParser.ViewWParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#viewWParameters}.
	 * @param ctx the parse tree
	 */
	void exitViewWParameters(EsperEPL2GrammarParser.ViewWParametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#groupByListExpr}.
	 * @param ctx the parse tree
	 */
	void enterGroupByListExpr(EsperEPL2GrammarParser.GroupByListExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#groupByListExpr}.
	 * @param ctx the parse tree
	 */
	void exitGroupByListExpr(EsperEPL2GrammarParser.GroupByListExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#groupByListChoice}.
	 * @param ctx the parse tree
	 */
	void enterGroupByListChoice(EsperEPL2GrammarParser.GroupByListChoiceContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#groupByListChoice}.
	 * @param ctx the parse tree
	 */
	void exitGroupByListChoice(EsperEPL2GrammarParser.GroupByListChoiceContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#groupByCubeOrRollup}.
	 * @param ctx the parse tree
	 */
	void enterGroupByCubeOrRollup(EsperEPL2GrammarParser.GroupByCubeOrRollupContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#groupByCubeOrRollup}.
	 * @param ctx the parse tree
	 */
	void exitGroupByCubeOrRollup(EsperEPL2GrammarParser.GroupByCubeOrRollupContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#groupByGroupingSets}.
	 * @param ctx the parse tree
	 */
	void enterGroupByGroupingSets(EsperEPL2GrammarParser.GroupByGroupingSetsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#groupByGroupingSets}.
	 * @param ctx the parse tree
	 */
	void exitGroupByGroupingSets(EsperEPL2GrammarParser.GroupByGroupingSetsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#groupBySetsChoice}.
	 * @param ctx the parse tree
	 */
	void enterGroupBySetsChoice(EsperEPL2GrammarParser.GroupBySetsChoiceContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#groupBySetsChoice}.
	 * @param ctx the parse tree
	 */
	void exitGroupBySetsChoice(EsperEPL2GrammarParser.GroupBySetsChoiceContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#groupByCombinableExpr}.
	 * @param ctx the parse tree
	 */
	void enterGroupByCombinableExpr(EsperEPL2GrammarParser.GroupByCombinableExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#groupByCombinableExpr}.
	 * @param ctx the parse tree
	 */
	void exitGroupByCombinableExpr(EsperEPL2GrammarParser.GroupByCombinableExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#orderByListExpr}.
	 * @param ctx the parse tree
	 */
	void enterOrderByListExpr(EsperEPL2GrammarParser.OrderByListExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#orderByListExpr}.
	 * @param ctx the parse tree
	 */
	void exitOrderByListExpr(EsperEPL2GrammarParser.OrderByListExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#orderByListElement}.
	 * @param ctx the parse tree
	 */
	void enterOrderByListElement(EsperEPL2GrammarParser.OrderByListElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#orderByListElement}.
	 * @param ctx the parse tree
	 */
	void exitOrderByListElement(EsperEPL2GrammarParser.OrderByListElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#havingClause}.
	 * @param ctx the parse tree
	 */
	void enterHavingClause(EsperEPL2GrammarParser.HavingClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#havingClause}.
	 * @param ctx the parse tree
	 */
	void exitHavingClause(EsperEPL2GrammarParser.HavingClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#outputLimit}.
	 * @param ctx the parse tree
	 */
	void enterOutputLimit(EsperEPL2GrammarParser.OutputLimitContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#outputLimit}.
	 * @param ctx the parse tree
	 */
	void exitOutputLimit(EsperEPL2GrammarParser.OutputLimitContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#outputLimitAndTerm}.
	 * @param ctx the parse tree
	 */
	void enterOutputLimitAndTerm(EsperEPL2GrammarParser.OutputLimitAndTermContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#outputLimitAndTerm}.
	 * @param ctx the parse tree
	 */
	void exitOutputLimitAndTerm(EsperEPL2GrammarParser.OutputLimitAndTermContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#outputLimitAfter}.
	 * @param ctx the parse tree
	 */
	void enterOutputLimitAfter(EsperEPL2GrammarParser.OutputLimitAfterContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#outputLimitAfter}.
	 * @param ctx the parse tree
	 */
	void exitOutputLimitAfter(EsperEPL2GrammarParser.OutputLimitAfterContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#rowLimit}.
	 * @param ctx the parse tree
	 */
	void enterRowLimit(EsperEPL2GrammarParser.RowLimitContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#rowLimit}.
	 * @param ctx the parse tree
	 */
	void exitRowLimit(EsperEPL2GrammarParser.RowLimitContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#crontabLimitParameterSet}.
	 * @param ctx the parse tree
	 */
	void enterCrontabLimitParameterSet(EsperEPL2GrammarParser.CrontabLimitParameterSetContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#crontabLimitParameterSet}.
	 * @param ctx the parse tree
	 */
	void exitCrontabLimitParameterSet(EsperEPL2GrammarParser.CrontabLimitParameterSetContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#whenClause}.
	 * @param ctx the parse tree
	 */
	void enterWhenClause(EsperEPL2GrammarParser.WhenClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#whenClause}.
	 * @param ctx the parse tree
	 */
	void exitWhenClause(EsperEPL2GrammarParser.WhenClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#elseClause}.
	 * @param ctx the parse tree
	 */
	void enterElseClause(EsperEPL2GrammarParser.ElseClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#elseClause}.
	 * @param ctx the parse tree
	 */
	void exitElseClause(EsperEPL2GrammarParser.ElseClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecog}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecog(EsperEPL2GrammarParser.MatchRecogContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecog}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecog(EsperEPL2GrammarParser.MatchRecogContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPartitionBy}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogPartitionBy(EsperEPL2GrammarParser.MatchRecogPartitionByContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPartitionBy}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogPartitionBy(EsperEPL2GrammarParser.MatchRecogPartitionByContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogMeasures}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogMeasures(EsperEPL2GrammarParser.MatchRecogMeasuresContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogMeasures}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogMeasures(EsperEPL2GrammarParser.MatchRecogMeasuresContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogMeasureItem}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogMeasureItem(EsperEPL2GrammarParser.MatchRecogMeasureItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogMeasureItem}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogMeasureItem(EsperEPL2GrammarParser.MatchRecogMeasureItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogMatchesSelection}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogMatchesSelection(EsperEPL2GrammarParser.MatchRecogMatchesSelectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogMatchesSelection}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogMatchesSelection(EsperEPL2GrammarParser.MatchRecogMatchesSelectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPattern}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogPattern(EsperEPL2GrammarParser.MatchRecogPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPattern}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogPattern(EsperEPL2GrammarParser.MatchRecogPatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogMatchesAfterSkip}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogMatchesAfterSkip(EsperEPL2GrammarParser.MatchRecogMatchesAfterSkipContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogMatchesAfterSkip}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogMatchesAfterSkip(EsperEPL2GrammarParser.MatchRecogMatchesAfterSkipContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogMatchesInterval}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogMatchesInterval(EsperEPL2GrammarParser.MatchRecogMatchesIntervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogMatchesInterval}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogMatchesInterval(EsperEPL2GrammarParser.MatchRecogMatchesIntervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPatternAlteration}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogPatternAlteration(EsperEPL2GrammarParser.MatchRecogPatternAlterationContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPatternAlteration}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogPatternAlteration(EsperEPL2GrammarParser.MatchRecogPatternAlterationContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPatternConcat}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogPatternConcat(EsperEPL2GrammarParser.MatchRecogPatternConcatContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPatternConcat}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogPatternConcat(EsperEPL2GrammarParser.MatchRecogPatternConcatContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPatternUnary}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogPatternUnary(EsperEPL2GrammarParser.MatchRecogPatternUnaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPatternUnary}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogPatternUnary(EsperEPL2GrammarParser.MatchRecogPatternUnaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPatternNested}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogPatternNested(EsperEPL2GrammarParser.MatchRecogPatternNestedContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPatternNested}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogPatternNested(EsperEPL2GrammarParser.MatchRecogPatternNestedContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPatternPermute}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogPatternPermute(EsperEPL2GrammarParser.MatchRecogPatternPermuteContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPatternPermute}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogPatternPermute(EsperEPL2GrammarParser.MatchRecogPatternPermuteContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPatternAtom}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogPatternAtom(EsperEPL2GrammarParser.MatchRecogPatternAtomContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPatternAtom}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogPatternAtom(EsperEPL2GrammarParser.MatchRecogPatternAtomContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPatternRepeat}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogPatternRepeat(EsperEPL2GrammarParser.MatchRecogPatternRepeatContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogPatternRepeat}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogPatternRepeat(EsperEPL2GrammarParser.MatchRecogPatternRepeatContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogDefine}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogDefine(EsperEPL2GrammarParser.MatchRecogDefineContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogDefine}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogDefine(EsperEPL2GrammarParser.MatchRecogDefineContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogDefineItem}.
	 * @param ctx the parse tree
	 */
	void enterMatchRecogDefineItem(EsperEPL2GrammarParser.MatchRecogDefineItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchRecogDefineItem}.
	 * @param ctx the parse tree
	 */
	void exitMatchRecogDefineItem(EsperEPL2GrammarParser.MatchRecogDefineItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(EsperEPL2GrammarParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(EsperEPL2GrammarParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#caseExpression}.
	 * @param ctx the parse tree
	 */
	void enterCaseExpression(EsperEPL2GrammarParser.CaseExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#caseExpression}.
	 * @param ctx the parse tree
	 */
	void exitCaseExpression(EsperEPL2GrammarParser.CaseExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#evalOrExpression}.
	 * @param ctx the parse tree
	 */
	void enterEvalOrExpression(EsperEPL2GrammarParser.EvalOrExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#evalOrExpression}.
	 * @param ctx the parse tree
	 */
	void exitEvalOrExpression(EsperEPL2GrammarParser.EvalOrExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#evalAndExpression}.
	 * @param ctx the parse tree
	 */
	void enterEvalAndExpression(EsperEPL2GrammarParser.EvalAndExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#evalAndExpression}.
	 * @param ctx the parse tree
	 */
	void exitEvalAndExpression(EsperEPL2GrammarParser.EvalAndExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#bitWiseExpression}.
	 * @param ctx the parse tree
	 */
	void enterBitWiseExpression(EsperEPL2GrammarParser.BitWiseExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#bitWiseExpression}.
	 * @param ctx the parse tree
	 */
	void exitBitWiseExpression(EsperEPL2GrammarParser.BitWiseExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#negatedExpression}.
	 * @param ctx the parse tree
	 */
	void enterNegatedExpression(EsperEPL2GrammarParser.NegatedExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#negatedExpression}.
	 * @param ctx the parse tree
	 */
	void exitNegatedExpression(EsperEPL2GrammarParser.NegatedExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#evalEqualsExpression}.
	 * @param ctx the parse tree
	 */
	void enterEvalEqualsExpression(EsperEPL2GrammarParser.EvalEqualsExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#evalEqualsExpression}.
	 * @param ctx the parse tree
	 */
	void exitEvalEqualsExpression(EsperEPL2GrammarParser.EvalEqualsExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#evalRelationalExpression}.
	 * @param ctx the parse tree
	 */
	void enterEvalRelationalExpression(EsperEPL2GrammarParser.EvalRelationalExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#evalRelationalExpression}.
	 * @param ctx the parse tree
	 */
	void exitEvalRelationalExpression(EsperEPL2GrammarParser.EvalRelationalExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#inSubSelectQuery}.
	 * @param ctx the parse tree
	 */
	void enterInSubSelectQuery(EsperEPL2GrammarParser.InSubSelectQueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#inSubSelectQuery}.
	 * @param ctx the parse tree
	 */
	void exitInSubSelectQuery(EsperEPL2GrammarParser.InSubSelectQueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#concatenationExpr}.
	 * @param ctx the parse tree
	 */
	void enterConcatenationExpr(EsperEPL2GrammarParser.ConcatenationExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#concatenationExpr}.
	 * @param ctx the parse tree
	 */
	void exitConcatenationExpr(EsperEPL2GrammarParser.ConcatenationExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#additiveExpression}.
	 * @param ctx the parse tree
	 */
	void enterAdditiveExpression(EsperEPL2GrammarParser.AdditiveExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#additiveExpression}.
	 * @param ctx the parse tree
	 */
	void exitAdditiveExpression(EsperEPL2GrammarParser.AdditiveExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#multiplyExpression}.
	 * @param ctx the parse tree
	 */
	void enterMultiplyExpression(EsperEPL2GrammarParser.MultiplyExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#multiplyExpression}.
	 * @param ctx the parse tree
	 */
	void exitMultiplyExpression(EsperEPL2GrammarParser.MultiplyExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterUnaryExpression(EsperEPL2GrammarParser.UnaryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitUnaryExpression(EsperEPL2GrammarParser.UnaryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#substitutionCanChain}.
	 * @param ctx the parse tree
	 */
	void enterSubstitutionCanChain(EsperEPL2GrammarParser.SubstitutionCanChainContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#substitutionCanChain}.
	 * @param ctx the parse tree
	 */
	void exitSubstitutionCanChain(EsperEPL2GrammarParser.SubstitutionCanChainContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#chainedFunction}.
	 * @param ctx the parse tree
	 */
	void enterChainedFunction(EsperEPL2GrammarParser.ChainedFunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#chainedFunction}.
	 * @param ctx the parse tree
	 */
	void exitChainedFunction(EsperEPL2GrammarParser.ChainedFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#newAssign}.
	 * @param ctx the parse tree
	 */
	void enterNewAssign(EsperEPL2GrammarParser.NewAssignContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#newAssign}.
	 * @param ctx the parse tree
	 */
	void exitNewAssign(EsperEPL2GrammarParser.NewAssignContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#rowSubSelectExpression}.
	 * @param ctx the parse tree
	 */
	void enterRowSubSelectExpression(EsperEPL2GrammarParser.RowSubSelectExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#rowSubSelectExpression}.
	 * @param ctx the parse tree
	 */
	void exitRowSubSelectExpression(EsperEPL2GrammarParser.RowSubSelectExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#subSelectGroupExpression}.
	 * @param ctx the parse tree
	 */
	void enterSubSelectGroupExpression(EsperEPL2GrammarParser.SubSelectGroupExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#subSelectGroupExpression}.
	 * @param ctx the parse tree
	 */
	void exitSubSelectGroupExpression(EsperEPL2GrammarParser.SubSelectGroupExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#existsSubSelectExpression}.
	 * @param ctx the parse tree
	 */
	void enterExistsSubSelectExpression(EsperEPL2GrammarParser.ExistsSubSelectExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#existsSubSelectExpression}.
	 * @param ctx the parse tree
	 */
	void exitExistsSubSelectExpression(EsperEPL2GrammarParser.ExistsSubSelectExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#subQueryExpr}.
	 * @param ctx the parse tree
	 */
	void enterSubQueryExpr(EsperEPL2GrammarParser.SubQueryExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#subQueryExpr}.
	 * @param ctx the parse tree
	 */
	void exitSubQueryExpr(EsperEPL2GrammarParser.SubQueryExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#subSelectFilterExpr}.
	 * @param ctx the parse tree
	 */
	void enterSubSelectFilterExpr(EsperEPL2GrammarParser.SubSelectFilterExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#subSelectFilterExpr}.
	 * @param ctx the parse tree
	 */
	void exitSubSelectFilterExpr(EsperEPL2GrammarParser.SubSelectFilterExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#arrayExpression}.
	 * @param ctx the parse tree
	 */
	void enterArrayExpression(EsperEPL2GrammarParser.ArrayExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#arrayExpression}.
	 * @param ctx the parse tree
	 */
	void exitArrayExpression(EsperEPL2GrammarParser.ArrayExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code builtin_sum}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_sum(EsperEPL2GrammarParser.Builtin_sumContext ctx);
	/**
	 * Exit a parse tree produced by the {@code builtin_sum}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_sum(EsperEPL2GrammarParser.Builtin_sumContext ctx);
	/**
	 * Enter a parse tree produced by the {@code builtin_avg}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_avg(EsperEPL2GrammarParser.Builtin_avgContext ctx);
	/**
	 * Exit a parse tree produced by the {@code builtin_avg}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_avg(EsperEPL2GrammarParser.Builtin_avgContext ctx);
	/**
	 * Enter a parse tree produced by the {@code builtin_cnt}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_cnt(EsperEPL2GrammarParser.Builtin_cntContext ctx);
	/**
	 * Exit a parse tree produced by the {@code builtin_cnt}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_cnt(EsperEPL2GrammarParser.Builtin_cntContext ctx);
	/**
	 * Enter a parse tree produced by the {@code builtin_median}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_median(EsperEPL2GrammarParser.Builtin_medianContext ctx);
	/**
	 * Exit a parse tree produced by the {@code builtin_median}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_median(EsperEPL2GrammarParser.Builtin_medianContext ctx);
	/**
	 * Enter a parse tree produced by the {@code builtin_stddev}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_stddev(EsperEPL2GrammarParser.Builtin_stddevContext ctx);
	/**
	 * Exit a parse tree produced by the {@code builtin_stddev}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_stddev(EsperEPL2GrammarParser.Builtin_stddevContext ctx);
	/**
	 * Enter a parse tree produced by the {@code builtin_avedev}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_avedev(EsperEPL2GrammarParser.Builtin_avedevContext ctx);
	/**
	 * Exit a parse tree produced by the {@code builtin_avedev}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_avedev(EsperEPL2GrammarParser.Builtin_avedevContext ctx);
	/**
	 * Enter a parse tree produced by the {@code builtin_firstlastwindow}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_firstlastwindow(EsperEPL2GrammarParser.Builtin_firstlastwindowContext ctx);
	/**
	 * Exit a parse tree produced by the {@code builtin_firstlastwindow}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_firstlastwindow(EsperEPL2GrammarParser.Builtin_firstlastwindowContext ctx);
	/**
	 * Enter a parse tree produced by the {@code builtin_coalesce}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_coalesce(EsperEPL2GrammarParser.Builtin_coalesceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code builtin_coalesce}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_coalesce(EsperEPL2GrammarParser.Builtin_coalesceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code builtin_prev}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_prev(EsperEPL2GrammarParser.Builtin_prevContext ctx);
	/**
	 * Exit a parse tree produced by the {@code builtin_prev}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_prev(EsperEPL2GrammarParser.Builtin_prevContext ctx);
	/**
	 * Enter a parse tree produced by the {@code builtin_prevtail}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_prevtail(EsperEPL2GrammarParser.Builtin_prevtailContext ctx);
	/**
	 * Exit a parse tree produced by the {@code builtin_prevtail}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_prevtail(EsperEPL2GrammarParser.Builtin_prevtailContext ctx);
	/**
	 * Enter a parse tree produced by the {@code builtin_prevcount}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_prevcount(EsperEPL2GrammarParser.Builtin_prevcountContext ctx);
	/**
	 * Exit a parse tree produced by the {@code builtin_prevcount}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_prevcount(EsperEPL2GrammarParser.Builtin_prevcountContext ctx);
	/**
	 * Enter a parse tree produced by the {@code builtin_prevwindow}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_prevwindow(EsperEPL2GrammarParser.Builtin_prevwindowContext ctx);
	/**
	 * Exit a parse tree produced by the {@code builtin_prevwindow}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_prevwindow(EsperEPL2GrammarParser.Builtin_prevwindowContext ctx);
	/**
	 * Enter a parse tree produced by the {@code builtin_prior}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_prior(EsperEPL2GrammarParser.Builtin_priorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code builtin_prior}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_prior(EsperEPL2GrammarParser.Builtin_priorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code builtin_grouping}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_grouping(EsperEPL2GrammarParser.Builtin_groupingContext ctx);
	/**
	 * Exit a parse tree produced by the {@code builtin_grouping}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_grouping(EsperEPL2GrammarParser.Builtin_groupingContext ctx);
	/**
	 * Enter a parse tree produced by the {@code builtin_groupingid}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_groupingid(EsperEPL2GrammarParser.Builtin_groupingidContext ctx);
	/**
	 * Exit a parse tree produced by the {@code builtin_groupingid}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_groupingid(EsperEPL2GrammarParser.Builtin_groupingidContext ctx);
	/**
	 * Enter a parse tree produced by the {@code builtin_instanceof}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_instanceof(EsperEPL2GrammarParser.Builtin_instanceofContext ctx);
	/**
	 * Exit a parse tree produced by the {@code builtin_instanceof}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_instanceof(EsperEPL2GrammarParser.Builtin_instanceofContext ctx);
	/**
	 * Enter a parse tree produced by the {@code builtin_typeof}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_typeof(EsperEPL2GrammarParser.Builtin_typeofContext ctx);
	/**
	 * Exit a parse tree produced by the {@code builtin_typeof}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_typeof(EsperEPL2GrammarParser.Builtin_typeofContext ctx);
	/**
	 * Enter a parse tree produced by the {@code builtin_cast}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_cast(EsperEPL2GrammarParser.Builtin_castContext ctx);
	/**
	 * Exit a parse tree produced by the {@code builtin_cast}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_cast(EsperEPL2GrammarParser.Builtin_castContext ctx);
	/**
	 * Enter a parse tree produced by the {@code builtin_exists}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_exists(EsperEPL2GrammarParser.Builtin_existsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code builtin_exists}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_exists(EsperEPL2GrammarParser.Builtin_existsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code builtin_currts}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_currts(EsperEPL2GrammarParser.Builtin_currtsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code builtin_currts}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_currts(EsperEPL2GrammarParser.Builtin_currtsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code builtin_istream}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_istream(EsperEPL2GrammarParser.Builtin_istreamContext ctx);
	/**
	 * Exit a parse tree produced by the {@code builtin_istream}
	 * labeled alternative in {@link EsperEPL2GrammarParser#builtinFunc}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_istream(EsperEPL2GrammarParser.Builtin_istreamContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#firstLastWindowAggregation}.
	 * @param ctx the parse tree
	 */
	void enterFirstLastWindowAggregation(EsperEPL2GrammarParser.FirstLastWindowAggregationContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#firstLastWindowAggregation}.
	 * @param ctx the parse tree
	 */
	void exitFirstLastWindowAggregation(EsperEPL2GrammarParser.FirstLastWindowAggregationContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#eventPropertyOrLibFunction}.
	 * @param ctx the parse tree
	 */
	void enterEventPropertyOrLibFunction(EsperEPL2GrammarParser.EventPropertyOrLibFunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#eventPropertyOrLibFunction}.
	 * @param ctx the parse tree
	 */
	void exitEventPropertyOrLibFunction(EsperEPL2GrammarParser.EventPropertyOrLibFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#libFunction}.
	 * @param ctx the parse tree
	 */
	void enterLibFunction(EsperEPL2GrammarParser.LibFunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#libFunction}.
	 * @param ctx the parse tree
	 */
	void exitLibFunction(EsperEPL2GrammarParser.LibFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#libFunctionWithClass}.
	 * @param ctx the parse tree
	 */
	void enterLibFunctionWithClass(EsperEPL2GrammarParser.LibFunctionWithClassContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#libFunctionWithClass}.
	 * @param ctx the parse tree
	 */
	void exitLibFunctionWithClass(EsperEPL2GrammarParser.LibFunctionWithClassContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#libFunctionNoClass}.
	 * @param ctx the parse tree
	 */
	void enterLibFunctionNoClass(EsperEPL2GrammarParser.LibFunctionNoClassContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#libFunctionNoClass}.
	 * @param ctx the parse tree
	 */
	void exitLibFunctionNoClass(EsperEPL2GrammarParser.LibFunctionNoClassContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#funcIdentTop}.
	 * @param ctx the parse tree
	 */
	void enterFuncIdentTop(EsperEPL2GrammarParser.FuncIdentTopContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#funcIdentTop}.
	 * @param ctx the parse tree
	 */
	void exitFuncIdentTop(EsperEPL2GrammarParser.FuncIdentTopContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#funcIdentInner}.
	 * @param ctx the parse tree
	 */
	void enterFuncIdentInner(EsperEPL2GrammarParser.FuncIdentInnerContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#funcIdentInner}.
	 * @param ctx the parse tree
	 */
	void exitFuncIdentInner(EsperEPL2GrammarParser.FuncIdentInnerContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#funcIdentChained}.
	 * @param ctx the parse tree
	 */
	void enterFuncIdentChained(EsperEPL2GrammarParser.FuncIdentChainedContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#funcIdentChained}.
	 * @param ctx the parse tree
	 */
	void exitFuncIdentChained(EsperEPL2GrammarParser.FuncIdentChainedContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#libFunctionArgs}.
	 * @param ctx the parse tree
	 */
	void enterLibFunctionArgs(EsperEPL2GrammarParser.LibFunctionArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#libFunctionArgs}.
	 * @param ctx the parse tree
	 */
	void exitLibFunctionArgs(EsperEPL2GrammarParser.LibFunctionArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#libFunctionArgItem}.
	 * @param ctx the parse tree
	 */
	void enterLibFunctionArgItem(EsperEPL2GrammarParser.LibFunctionArgItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#libFunctionArgItem}.
	 * @param ctx the parse tree
	 */
	void exitLibFunctionArgItem(EsperEPL2GrammarParser.LibFunctionArgItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#betweenList}.
	 * @param ctx the parse tree
	 */
	void enterBetweenList(EsperEPL2GrammarParser.BetweenListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#betweenList}.
	 * @param ctx the parse tree
	 */
	void exitBetweenList(EsperEPL2GrammarParser.BetweenListContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#patternExpression}.
	 * @param ctx the parse tree
	 */
	void enterPatternExpression(EsperEPL2GrammarParser.PatternExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#patternExpression}.
	 * @param ctx the parse tree
	 */
	void exitPatternExpression(EsperEPL2GrammarParser.PatternExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#followedByExpression}.
	 * @param ctx the parse tree
	 */
	void enterFollowedByExpression(EsperEPL2GrammarParser.FollowedByExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#followedByExpression}.
	 * @param ctx the parse tree
	 */
	void exitFollowedByExpression(EsperEPL2GrammarParser.FollowedByExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#followedByRepeat}.
	 * @param ctx the parse tree
	 */
	void enterFollowedByRepeat(EsperEPL2GrammarParser.FollowedByRepeatContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#followedByRepeat}.
	 * @param ctx the parse tree
	 */
	void exitFollowedByRepeat(EsperEPL2GrammarParser.FollowedByRepeatContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#orExpression}.
	 * @param ctx the parse tree
	 */
	void enterOrExpression(EsperEPL2GrammarParser.OrExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#orExpression}.
	 * @param ctx the parse tree
	 */
	void exitOrExpression(EsperEPL2GrammarParser.OrExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#andExpression}.
	 * @param ctx the parse tree
	 */
	void enterAndExpression(EsperEPL2GrammarParser.AndExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#andExpression}.
	 * @param ctx the parse tree
	 */
	void exitAndExpression(EsperEPL2GrammarParser.AndExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchUntilExpression}.
	 * @param ctx the parse tree
	 */
	void enterMatchUntilExpression(EsperEPL2GrammarParser.MatchUntilExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchUntilExpression}.
	 * @param ctx the parse tree
	 */
	void exitMatchUntilExpression(EsperEPL2GrammarParser.MatchUntilExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#qualifyExpression}.
	 * @param ctx the parse tree
	 */
	void enterQualifyExpression(EsperEPL2GrammarParser.QualifyExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#qualifyExpression}.
	 * @param ctx the parse tree
	 */
	void exitQualifyExpression(EsperEPL2GrammarParser.QualifyExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#guardPostFix}.
	 * @param ctx the parse tree
	 */
	void enterGuardPostFix(EsperEPL2GrammarParser.GuardPostFixContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#guardPostFix}.
	 * @param ctx the parse tree
	 */
	void exitGuardPostFix(EsperEPL2GrammarParser.GuardPostFixContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#distinctExpressionList}.
	 * @param ctx the parse tree
	 */
	void enterDistinctExpressionList(EsperEPL2GrammarParser.DistinctExpressionListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#distinctExpressionList}.
	 * @param ctx the parse tree
	 */
	void exitDistinctExpressionList(EsperEPL2GrammarParser.DistinctExpressionListContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#distinctExpressionAtom}.
	 * @param ctx the parse tree
	 */
	void enterDistinctExpressionAtom(EsperEPL2GrammarParser.DistinctExpressionAtomContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#distinctExpressionAtom}.
	 * @param ctx the parse tree
	 */
	void exitDistinctExpressionAtom(EsperEPL2GrammarParser.DistinctExpressionAtomContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#atomicExpression}.
	 * @param ctx the parse tree
	 */
	void enterAtomicExpression(EsperEPL2GrammarParser.AtomicExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#atomicExpression}.
	 * @param ctx the parse tree
	 */
	void exitAtomicExpression(EsperEPL2GrammarParser.AtomicExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#observerExpression}.
	 * @param ctx the parse tree
	 */
	void enterObserverExpression(EsperEPL2GrammarParser.ObserverExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#observerExpression}.
	 * @param ctx the parse tree
	 */
	void exitObserverExpression(EsperEPL2GrammarParser.ObserverExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#guardWhereExpression}.
	 * @param ctx the parse tree
	 */
	void enterGuardWhereExpression(EsperEPL2GrammarParser.GuardWhereExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#guardWhereExpression}.
	 * @param ctx the parse tree
	 */
	void exitGuardWhereExpression(EsperEPL2GrammarParser.GuardWhereExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#guardWhileExpression}.
	 * @param ctx the parse tree
	 */
	void enterGuardWhileExpression(EsperEPL2GrammarParser.GuardWhileExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#guardWhileExpression}.
	 * @param ctx the parse tree
	 */
	void exitGuardWhileExpression(EsperEPL2GrammarParser.GuardWhileExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#matchUntilRange}.
	 * @param ctx the parse tree
	 */
	void enterMatchUntilRange(EsperEPL2GrammarParser.MatchUntilRangeContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#matchUntilRange}.
	 * @param ctx the parse tree
	 */
	void exitMatchUntilRange(EsperEPL2GrammarParser.MatchUntilRangeContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#eventFilterExpression}.
	 * @param ctx the parse tree
	 */
	void enterEventFilterExpression(EsperEPL2GrammarParser.EventFilterExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#eventFilterExpression}.
	 * @param ctx the parse tree
	 */
	void exitEventFilterExpression(EsperEPL2GrammarParser.EventFilterExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#propertyExpression}.
	 * @param ctx the parse tree
	 */
	void enterPropertyExpression(EsperEPL2GrammarParser.PropertyExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#propertyExpression}.
	 * @param ctx the parse tree
	 */
	void exitPropertyExpression(EsperEPL2GrammarParser.PropertyExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#propertyExpressionAtomic}.
	 * @param ctx the parse tree
	 */
	void enterPropertyExpressionAtomic(EsperEPL2GrammarParser.PropertyExpressionAtomicContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#propertyExpressionAtomic}.
	 * @param ctx the parse tree
	 */
	void exitPropertyExpressionAtomic(EsperEPL2GrammarParser.PropertyExpressionAtomicContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#propertyExpressionSelect}.
	 * @param ctx the parse tree
	 */
	void enterPropertyExpressionSelect(EsperEPL2GrammarParser.PropertyExpressionSelectContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#propertyExpressionSelect}.
	 * @param ctx the parse tree
	 */
	void exitPropertyExpressionSelect(EsperEPL2GrammarParser.PropertyExpressionSelectContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#propertySelectionList}.
	 * @param ctx the parse tree
	 */
	void enterPropertySelectionList(EsperEPL2GrammarParser.PropertySelectionListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#propertySelectionList}.
	 * @param ctx the parse tree
	 */
	void exitPropertySelectionList(EsperEPL2GrammarParser.PropertySelectionListContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#propertySelectionListElement}.
	 * @param ctx the parse tree
	 */
	void enterPropertySelectionListElement(EsperEPL2GrammarParser.PropertySelectionListElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#propertySelectionListElement}.
	 * @param ctx the parse tree
	 */
	void exitPropertySelectionListElement(EsperEPL2GrammarParser.PropertySelectionListElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#propertyStreamSelector}.
	 * @param ctx the parse tree
	 */
	void enterPropertyStreamSelector(EsperEPL2GrammarParser.PropertyStreamSelectorContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#propertyStreamSelector}.
	 * @param ctx the parse tree
	 */
	void exitPropertyStreamSelector(EsperEPL2GrammarParser.PropertyStreamSelectorContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#typeExpressionAnnotation}.
	 * @param ctx the parse tree
	 */
	void enterTypeExpressionAnnotation(EsperEPL2GrammarParser.TypeExpressionAnnotationContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#typeExpressionAnnotation}.
	 * @param ctx the parse tree
	 */
	void exitTypeExpressionAnnotation(EsperEPL2GrammarParser.TypeExpressionAnnotationContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#patternFilterExpression}.
	 * @param ctx the parse tree
	 */
	void enterPatternFilterExpression(EsperEPL2GrammarParser.PatternFilterExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#patternFilterExpression}.
	 * @param ctx the parse tree
	 */
	void exitPatternFilterExpression(EsperEPL2GrammarParser.PatternFilterExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#patternFilterAnnotation}.
	 * @param ctx the parse tree
	 */
	void enterPatternFilterAnnotation(EsperEPL2GrammarParser.PatternFilterAnnotationContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#patternFilterAnnotation}.
	 * @param ctx the parse tree
	 */
	void exitPatternFilterAnnotation(EsperEPL2GrammarParser.PatternFilterAnnotationContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#classIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterClassIdentifier(EsperEPL2GrammarParser.ClassIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#classIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitClassIdentifier(EsperEPL2GrammarParser.ClassIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#slashIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterSlashIdentifier(EsperEPL2GrammarParser.SlashIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#slashIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitSlashIdentifier(EsperEPL2GrammarParser.SlashIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionListWithNamed}.
	 * @param ctx the parse tree
	 */
	void enterExpressionListWithNamed(EsperEPL2GrammarParser.ExpressionListWithNamedContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionListWithNamed}.
	 * @param ctx the parse tree
	 */
	void exitExpressionListWithNamed(EsperEPL2GrammarParser.ExpressionListWithNamedContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionListWithNamedWithTime}.
	 * @param ctx the parse tree
	 */
	void enterExpressionListWithNamedWithTime(EsperEPL2GrammarParser.ExpressionListWithNamedWithTimeContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionListWithNamedWithTime}.
	 * @param ctx the parse tree
	 */
	void exitExpressionListWithNamedWithTime(EsperEPL2GrammarParser.ExpressionListWithNamedWithTimeContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionWithNamed}.
	 * @param ctx the parse tree
	 */
	void enterExpressionWithNamed(EsperEPL2GrammarParser.ExpressionWithNamedContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionWithNamed}.
	 * @param ctx the parse tree
	 */
	void exitExpressionWithNamed(EsperEPL2GrammarParser.ExpressionWithNamedContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionWithNamedWithTime}.
	 * @param ctx the parse tree
	 */
	void enterExpressionWithNamedWithTime(EsperEPL2GrammarParser.ExpressionWithNamedWithTimeContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionWithNamedWithTime}.
	 * @param ctx the parse tree
	 */
	void exitExpressionWithNamedWithTime(EsperEPL2GrammarParser.ExpressionWithNamedWithTimeContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionNamedParameter}.
	 * @param ctx the parse tree
	 */
	void enterExpressionNamedParameter(EsperEPL2GrammarParser.ExpressionNamedParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionNamedParameter}.
	 * @param ctx the parse tree
	 */
	void exitExpressionNamedParameter(EsperEPL2GrammarParser.ExpressionNamedParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionNamedParameterWithTime}.
	 * @param ctx the parse tree
	 */
	void enterExpressionNamedParameterWithTime(EsperEPL2GrammarParser.ExpressionNamedParameterWithTimeContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionNamedParameterWithTime}.
	 * @param ctx the parse tree
	 */
	void exitExpressionNamedParameterWithTime(EsperEPL2GrammarParser.ExpressionNamedParameterWithTimeContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionList}.
	 * @param ctx the parse tree
	 */
	void enterExpressionList(EsperEPL2GrammarParser.ExpressionListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionList}.
	 * @param ctx the parse tree
	 */
	void exitExpressionList(EsperEPL2GrammarParser.ExpressionListContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionWithTimeList}.
	 * @param ctx the parse tree
	 */
	void enterExpressionWithTimeList(EsperEPL2GrammarParser.ExpressionWithTimeListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionWithTimeList}.
	 * @param ctx the parse tree
	 */
	void exitExpressionWithTimeList(EsperEPL2GrammarParser.ExpressionWithTimeListContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionWithTime}.
	 * @param ctx the parse tree
	 */
	void enterExpressionWithTime(EsperEPL2GrammarParser.ExpressionWithTimeContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionWithTime}.
	 * @param ctx the parse tree
	 */
	void exitExpressionWithTime(EsperEPL2GrammarParser.ExpressionWithTimeContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionWithTimeInclLast}.
	 * @param ctx the parse tree
	 */
	void enterExpressionWithTimeInclLast(EsperEPL2GrammarParser.ExpressionWithTimeInclLastContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionWithTimeInclLast}.
	 * @param ctx the parse tree
	 */
	void exitExpressionWithTimeInclLast(EsperEPL2GrammarParser.ExpressionWithTimeInclLastContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#expressionQualifyable}.
	 * @param ctx the parse tree
	 */
	void enterExpressionQualifyable(EsperEPL2GrammarParser.ExpressionQualifyableContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#expressionQualifyable}.
	 * @param ctx the parse tree
	 */
	void exitExpressionQualifyable(EsperEPL2GrammarParser.ExpressionQualifyableContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#lastWeekdayOperand}.
	 * @param ctx the parse tree
	 */
	void enterLastWeekdayOperand(EsperEPL2GrammarParser.LastWeekdayOperandContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#lastWeekdayOperand}.
	 * @param ctx the parse tree
	 */
	void exitLastWeekdayOperand(EsperEPL2GrammarParser.LastWeekdayOperandContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#lastOperand}.
	 * @param ctx the parse tree
	 */
	void enterLastOperand(EsperEPL2GrammarParser.LastOperandContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#lastOperand}.
	 * @param ctx the parse tree
	 */
	void exitLastOperand(EsperEPL2GrammarParser.LastOperandContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#frequencyOperand}.
	 * @param ctx the parse tree
	 */
	void enterFrequencyOperand(EsperEPL2GrammarParser.FrequencyOperandContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#frequencyOperand}.
	 * @param ctx the parse tree
	 */
	void exitFrequencyOperand(EsperEPL2GrammarParser.FrequencyOperandContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#rangeOperand}.
	 * @param ctx the parse tree
	 */
	void enterRangeOperand(EsperEPL2GrammarParser.RangeOperandContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#rangeOperand}.
	 * @param ctx the parse tree
	 */
	void exitRangeOperand(EsperEPL2GrammarParser.RangeOperandContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#lastOperator}.
	 * @param ctx the parse tree
	 */
	void enterLastOperator(EsperEPL2GrammarParser.LastOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#lastOperator}.
	 * @param ctx the parse tree
	 */
	void exitLastOperator(EsperEPL2GrammarParser.LastOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#weekDayOperator}.
	 * @param ctx the parse tree
	 */
	void enterWeekDayOperator(EsperEPL2GrammarParser.WeekDayOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#weekDayOperator}.
	 * @param ctx the parse tree
	 */
	void exitWeekDayOperator(EsperEPL2GrammarParser.WeekDayOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#numericParameterList}.
	 * @param ctx the parse tree
	 */
	void enterNumericParameterList(EsperEPL2GrammarParser.NumericParameterListContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#numericParameterList}.
	 * @param ctx the parse tree
	 */
	void exitNumericParameterList(EsperEPL2GrammarParser.NumericParameterListContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#numericListParameter}.
	 * @param ctx the parse tree
	 */
	void enterNumericListParameter(EsperEPL2GrammarParser.NumericListParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#numericListParameter}.
	 * @param ctx the parse tree
	 */
	void exitNumericListParameter(EsperEPL2GrammarParser.NumericListParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#eventProperty}.
	 * @param ctx the parse tree
	 */
	void enterEventProperty(EsperEPL2GrammarParser.EventPropertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#eventProperty}.
	 * @param ctx the parse tree
	 */
	void exitEventProperty(EsperEPL2GrammarParser.EventPropertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#eventPropertyAtomic}.
	 * @param ctx the parse tree
	 */
	void enterEventPropertyAtomic(EsperEPL2GrammarParser.EventPropertyAtomicContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#eventPropertyAtomic}.
	 * @param ctx the parse tree
	 */
	void exitEventPropertyAtomic(EsperEPL2GrammarParser.EventPropertyAtomicContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#eventPropertyIdent}.
	 * @param ctx the parse tree
	 */
	void enterEventPropertyIdent(EsperEPL2GrammarParser.EventPropertyIdentContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#eventPropertyIdent}.
	 * @param ctx the parse tree
	 */
	void exitEventPropertyIdent(EsperEPL2GrammarParser.EventPropertyIdentContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#identOrTicked}.
	 * @param ctx the parse tree
	 */
	void enterIdentOrTicked(EsperEPL2GrammarParser.IdentOrTickedContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#identOrTicked}.
	 * @param ctx the parse tree
	 */
	void exitIdentOrTicked(EsperEPL2GrammarParser.IdentOrTickedContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#keywordAllowedIdent}.
	 * @param ctx the parse tree
	 */
	void enterKeywordAllowedIdent(EsperEPL2GrammarParser.KeywordAllowedIdentContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#keywordAllowedIdent}.
	 * @param ctx the parse tree
	 */
	void exitKeywordAllowedIdent(EsperEPL2GrammarParser.KeywordAllowedIdentContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#escapableStr}.
	 * @param ctx the parse tree
	 */
	void enterEscapableStr(EsperEPL2GrammarParser.EscapableStrContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#escapableStr}.
	 * @param ctx the parse tree
	 */
	void exitEscapableStr(EsperEPL2GrammarParser.EscapableStrContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#escapableIdent}.
	 * @param ctx the parse tree
	 */
	void enterEscapableIdent(EsperEPL2GrammarParser.EscapableIdentContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#escapableIdent}.
	 * @param ctx the parse tree
	 */
	void exitEscapableIdent(EsperEPL2GrammarParser.EscapableIdentContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#timePeriod}.
	 * @param ctx the parse tree
	 */
	void enterTimePeriod(EsperEPL2GrammarParser.TimePeriodContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#timePeriod}.
	 * @param ctx the parse tree
	 */
	void exitTimePeriod(EsperEPL2GrammarParser.TimePeriodContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#yearPart}.
	 * @param ctx the parse tree
	 */
	void enterYearPart(EsperEPL2GrammarParser.YearPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#yearPart}.
	 * @param ctx the parse tree
	 */
	void exitYearPart(EsperEPL2GrammarParser.YearPartContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#monthPart}.
	 * @param ctx the parse tree
	 */
	void enterMonthPart(EsperEPL2GrammarParser.MonthPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#monthPart}.
	 * @param ctx the parse tree
	 */
	void exitMonthPart(EsperEPL2GrammarParser.MonthPartContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#weekPart}.
	 * @param ctx the parse tree
	 */
	void enterWeekPart(EsperEPL2GrammarParser.WeekPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#weekPart}.
	 * @param ctx the parse tree
	 */
	void exitWeekPart(EsperEPL2GrammarParser.WeekPartContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#dayPart}.
	 * @param ctx the parse tree
	 */
	void enterDayPart(EsperEPL2GrammarParser.DayPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#dayPart}.
	 * @param ctx the parse tree
	 */
	void exitDayPart(EsperEPL2GrammarParser.DayPartContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#hourPart}.
	 * @param ctx the parse tree
	 */
	void enterHourPart(EsperEPL2GrammarParser.HourPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#hourPart}.
	 * @param ctx the parse tree
	 */
	void exitHourPart(EsperEPL2GrammarParser.HourPartContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#minutePart}.
	 * @param ctx the parse tree
	 */
	void enterMinutePart(EsperEPL2GrammarParser.MinutePartContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#minutePart}.
	 * @param ctx the parse tree
	 */
	void exitMinutePart(EsperEPL2GrammarParser.MinutePartContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#secondPart}.
	 * @param ctx the parse tree
	 */
	void enterSecondPart(EsperEPL2GrammarParser.SecondPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#secondPart}.
	 * @param ctx the parse tree
	 */
	void exitSecondPart(EsperEPL2GrammarParser.SecondPartContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#millisecondPart}.
	 * @param ctx the parse tree
	 */
	void enterMillisecondPart(EsperEPL2GrammarParser.MillisecondPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#millisecondPart}.
	 * @param ctx the parse tree
	 */
	void exitMillisecondPart(EsperEPL2GrammarParser.MillisecondPartContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#microsecondPart}.
	 * @param ctx the parse tree
	 */
	void enterMicrosecondPart(EsperEPL2GrammarParser.MicrosecondPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#microsecondPart}.
	 * @param ctx the parse tree
	 */
	void exitMicrosecondPart(EsperEPL2GrammarParser.MicrosecondPartContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#number}.
	 * @param ctx the parse tree
	 */
	void enterNumber(EsperEPL2GrammarParser.NumberContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#number}.
	 * @param ctx the parse tree
	 */
	void exitNumber(EsperEPL2GrammarParser.NumberContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#substitution}.
	 * @param ctx the parse tree
	 */
	void enterSubstitution(EsperEPL2GrammarParser.SubstitutionContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#substitution}.
	 * @param ctx the parse tree
	 */
	void exitSubstitution(EsperEPL2GrammarParser.SubstitutionContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterConstant(EsperEPL2GrammarParser.ConstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitConstant(EsperEPL2GrammarParser.ConstantContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#numberconstant}.
	 * @param ctx the parse tree
	 */
	void enterNumberconstant(EsperEPL2GrammarParser.NumberconstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#numberconstant}.
	 * @param ctx the parse tree
	 */
	void exitNumberconstant(EsperEPL2GrammarParser.NumberconstantContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#stringconstant}.
	 * @param ctx the parse tree
	 */
	void enterStringconstant(EsperEPL2GrammarParser.StringconstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#stringconstant}.
	 * @param ctx the parse tree
	 */
	void exitStringconstant(EsperEPL2GrammarParser.StringconstantContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#jsonvalue}.
	 * @param ctx the parse tree
	 */
	void enterJsonvalue(EsperEPL2GrammarParser.JsonvalueContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#jsonvalue}.
	 * @param ctx the parse tree
	 */
	void exitJsonvalue(EsperEPL2GrammarParser.JsonvalueContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#jsonobject}.
	 * @param ctx the parse tree
	 */
	void enterJsonobject(EsperEPL2GrammarParser.JsonobjectContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#jsonobject}.
	 * @param ctx the parse tree
	 */
	void exitJsonobject(EsperEPL2GrammarParser.JsonobjectContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#jsonarray}.
	 * @param ctx the parse tree
	 */
	void enterJsonarray(EsperEPL2GrammarParser.JsonarrayContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#jsonarray}.
	 * @param ctx the parse tree
	 */
	void exitJsonarray(EsperEPL2GrammarParser.JsonarrayContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#jsonelements}.
	 * @param ctx the parse tree
	 */
	void enterJsonelements(EsperEPL2GrammarParser.JsonelementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#jsonelements}.
	 * @param ctx the parse tree
	 */
	void exitJsonelements(EsperEPL2GrammarParser.JsonelementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#jsonmembers}.
	 * @param ctx the parse tree
	 */
	void enterJsonmembers(EsperEPL2GrammarParser.JsonmembersContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#jsonmembers}.
	 * @param ctx the parse tree
	 */
	void exitJsonmembers(EsperEPL2GrammarParser.JsonmembersContext ctx);
	/**
	 * Enter a parse tree produced by {@link EsperEPL2GrammarParser#jsonpair}.
	 * @param ctx the parse tree
	 */
	void enterJsonpair(EsperEPL2GrammarParser.JsonpairContext ctx);
	/**
	 * Exit a parse tree produced by {@link EsperEPL2GrammarParser#jsonpair}.
	 * @param ctx the parse tree
	 */
	void exitJsonpair(EsperEPL2GrammarParser.JsonpairContext ctx);
}