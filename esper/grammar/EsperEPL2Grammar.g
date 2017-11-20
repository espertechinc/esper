grammar EsperEPL2Grammar;

@header {
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
}

@members {
  // provide nice error messages
  private java.util.Stack<String> paraphrases = new java.util.Stack<String>();
  
  // static information initialized once
  private static java.util.Map<Integer, String> lexerTokenParaphases = new java.util.HashMap<Integer, String>();
  private static java.util.Map<Integer, String> parserTokenParaphases = new java.util.HashMap<Integer, String>();
  private static java.util.Set<String> parserKeywordSet = new java.util.HashSet<String>();
  private static java.util.Set<Integer> afterScriptTokens = new java.util.HashSet<Integer>();
    
  public java.util.Stack getParaphrases() {
    return paraphrases;
  }

  public java.util.Set<String> getKeywords() {
  	getParserTokenParaphrases();
  	return parserKeywordSet;
  }
    
  public synchronized static java.util.Map<Integer, String> getLexerTokenParaphrases() {
    if (lexerTokenParaphases.size() == 0) {
      	lexerTokenParaphases.put(IDENT, "an identifier");
	lexerTokenParaphases.put(FOLLOWED_BY, "an followed-by '->'");
	lexerTokenParaphases.put(EQUALS, "an equals '='");
	lexerTokenParaphases.put(SQL_NE, "a sql-style not equals '<>'");
	lexerTokenParaphases.put(QUESTION, "a questionmark '?'");
	lexerTokenParaphases.put(LPAREN, "an opening parenthesis '('");
	lexerTokenParaphases.put(RPAREN, "a closing parenthesis ')'");
	lexerTokenParaphases.put(LBRACK, "a left angle bracket '['");
	lexerTokenParaphases.put(RBRACK, "a right angle bracket ']'");
	lexerTokenParaphases.put(LCURLY, "a left curly bracket '{'");
	lexerTokenParaphases.put(RCURLY, "a right curly bracket '}'");
	lexerTokenParaphases.put(COLON, "a colon ':'");
	lexerTokenParaphases.put(COMMA, "a comma ','");
	lexerTokenParaphases.put(EQUAL, "an equals compare '=='");
	lexerTokenParaphases.put(LNOT, "a not '!'");
	lexerTokenParaphases.put(BNOT, "a binary not '~'");
	lexerTokenParaphases.put(NOT_EQUAL, "a not equals '!='");
	lexerTokenParaphases.put(DIV, "a division operator '\'");
	lexerTokenParaphases.put(DIV_ASSIGN, "a division assign '/='");
	lexerTokenParaphases.put(PLUS, "a plus operator '+'");
	lexerTokenParaphases.put(PLUS_ASSIGN, "a plus assign '+='");
	lexerTokenParaphases.put(INC, "an increment operator '++'");
	lexerTokenParaphases.put(MINUS, "a minus '-'");
	lexerTokenParaphases.put(MINUS_ASSIGN, "a minus assign '-='");
	lexerTokenParaphases.put(DEC, "a decrement operator '--'");
	lexerTokenParaphases.put(STAR, "a star '*'");
	lexerTokenParaphases.put(STAR_ASSIGN, "a star assign '*='");
	lexerTokenParaphases.put(MOD, "a modulo");
	lexerTokenParaphases.put(MOD_ASSIGN, "a modulo assign");
	lexerTokenParaphases.put(GE, "a greater equals '>='");
	lexerTokenParaphases.put(GT, "a greater then '>'");
	lexerTokenParaphases.put(LE, "a less equals '<='");
	lexerTokenParaphases.put(LT, "a lesser then '<'");
	lexerTokenParaphases.put(BXOR, "a binary xor '^'");
	lexerTokenParaphases.put(BXOR_ASSIGN, "a binary xor assign '^='");
	lexerTokenParaphases.put(BOR, "a binary or '|'");
	lexerTokenParaphases.put(BOR_ASSIGN, "a binary or assign '|='");
	lexerTokenParaphases.put(LOR, "a logical or '||'");
	lexerTokenParaphases.put(BAND, "a binary and '&'");
	lexerTokenParaphases.put(BAND_ASSIGN, "a binary and assign '&='");
	lexerTokenParaphases.put(LAND, "a logical and '&&'");
	lexerTokenParaphases.put(SEMI, "a semicolon ';'");
	lexerTokenParaphases.put(DOT, "a dot '.'");		
    }
    return lexerTokenParaphases;
  }
  
  public synchronized static java.util.Map<Integer, String> getParserTokenParaphrases() {
    if (parserTokenParaphases.size() == 0) {
	parserTokenParaphases.put(CREATE, "'create'");
	parserTokenParaphases.put(WINDOW, "'window'");
	parserTokenParaphases.put(IN_SET, "'in'");
	parserTokenParaphases.put(BETWEEN, "'between'");
	parserTokenParaphases.put(LIKE, "'like'");
	parserTokenParaphases.put(REGEXP, "'regexp'");
	parserTokenParaphases.put(ESCAPE, "'escape'");
	parserTokenParaphases.put(OR_EXPR, "'or'");
	parserTokenParaphases.put(AND_EXPR, "'and'");
	parserTokenParaphases.put(NOT_EXPR, "'not'");
	parserTokenParaphases.put(EVERY_EXPR, "'every'");
	parserTokenParaphases.put(EVERY_DISTINCT_EXPR, "'every-distinct'");
	parserTokenParaphases.put(WHERE, "'where'");
	parserTokenParaphases.put(AS, "'as'");	
	parserTokenParaphases.put(SUM, "'sum'");
	parserTokenParaphases.put(AVG, "'avg'");
	parserTokenParaphases.put(MAX, "'max'");
	parserTokenParaphases.put(MIN, "'min'");
	parserTokenParaphases.put(COALESCE, "'coalesce'");
	parserTokenParaphases.put(MEDIAN, "'median'");
	parserTokenParaphases.put(STDDEV, "'stddev'");
	parserTokenParaphases.put(AVEDEV, "'avedev'");
	parserTokenParaphases.put(COUNT, "'count'");
	parserTokenParaphases.put(SELECT, "'select'");
	parserTokenParaphases.put(CASE, "'case'");
	parserTokenParaphases.put(ELSE, "'else'");
	parserTokenParaphases.put(WHEN, "'when'");
	parserTokenParaphases.put(THEN, "'then'");
	parserTokenParaphases.put(END, "'end'");
	parserTokenParaphases.put(FROM, "'from'");
	parserTokenParaphases.put(OUTER, "'outer'");
	parserTokenParaphases.put(INNER, "'inner'");
	parserTokenParaphases.put(JOIN, "'join'");
	parserTokenParaphases.put(LEFT, "'left'");
	parserTokenParaphases.put(RIGHT, "'right'");
	parserTokenParaphases.put(FULL, "'full'");
	parserTokenParaphases.put(ON, "'on'");	
	parserTokenParaphases.put(IS, "'is'");
	parserTokenParaphases.put(BY, "'by'");
	parserTokenParaphases.put(GROUP, "'group'");
	parserTokenParaphases.put(HAVING, "'having'");
	parserTokenParaphases.put(ALL, "'all'");
	parserTokenParaphases.put(ANY, "'any'");
	parserTokenParaphases.put(SOME, "'some'");
	parserTokenParaphases.put(OUTPUT, "'output'");
	parserTokenParaphases.put(EVENTS, "'events'");
	parserTokenParaphases.put(FIRST, "'first'");
	parserTokenParaphases.put(LAST, "'last'");
	parserTokenParaphases.put(INSERT, "'insert'");
	parserTokenParaphases.put(INTO, "'into'");
	parserTokenParaphases.put(ORDER, "'order'");
	parserTokenParaphases.put(ASC, "'asc'");
	parserTokenParaphases.put(DESC, "'desc'");
	parserTokenParaphases.put(RSTREAM, "'rstream'");
	parserTokenParaphases.put(ISTREAM, "'istream'");
	parserTokenParaphases.put(IRSTREAM, "'irstream'");
	parserTokenParaphases.put(SCHEMA, "'schema'");
	parserTokenParaphases.put(UNIDIRECTIONAL, "'unidirectional'");
	parserTokenParaphases.put(RETAINUNION, "'retain-union'");
	parserTokenParaphases.put(RETAININTERSECTION, "'retain-intersection'");
	parserTokenParaphases.put(PATTERN, "'pattern'");
	parserTokenParaphases.put(SQL, "'sql'");
	parserTokenParaphases.put(METADATASQL, "'metadatasql'");
	parserTokenParaphases.put(PREVIOUS, "'prev'");
	parserTokenParaphases.put(PREVIOUSTAIL, "'prevtail'");
	parserTokenParaphases.put(PREVIOUSCOUNT, "'prevcount'");
	parserTokenParaphases.put(PREVIOUSWINDOW, "'prevwindow'");
	parserTokenParaphases.put(PRIOR, "'prior'");
	parserTokenParaphases.put(EXISTS, "'exists'");
	parserTokenParaphases.put(WEEKDAY, "'weekday'");
	parserTokenParaphases.put(LW, "'lastweekday'");
	parserTokenParaphases.put(INSTANCEOF, "'instanceof'");
	parserTokenParaphases.put(TYPEOF, "'typeof'");
	parserTokenParaphases.put(CAST, "'cast'");
	parserTokenParaphases.put(CURRENT_TIMESTAMP, "'current_timestamp'");
	parserTokenParaphases.put(DELETE, "'delete'");
	parserTokenParaphases.put(DISTINCT, "'distinct'");
	parserTokenParaphases.put(SNAPSHOT, "'snapshot'");
	parserTokenParaphases.put(SET, "'set'");
	parserTokenParaphases.put(VARIABLE, "'variable'");
	parserTokenParaphases.put(TABLE, "'table'");
	parserTokenParaphases.put(INDEX, "'index'");
	parserTokenParaphases.put(UNTIL, "'until'");
	parserTokenParaphases.put(AT, "'at'");
	parserTokenParaphases.put(TIMEPERIOD_YEAR, "'year'");
	parserTokenParaphases.put(TIMEPERIOD_YEARS, "'years'");
	parserTokenParaphases.put(TIMEPERIOD_MONTH, "'month'");
	parserTokenParaphases.put(TIMEPERIOD_MONTHS, "'months'");
	parserTokenParaphases.put(TIMEPERIOD_WEEK, "'week'");
	parserTokenParaphases.put(TIMEPERIOD_WEEKS, "'weeks'");
	parserTokenParaphases.put(TIMEPERIOD_DAY, "'day'");
	parserTokenParaphases.put(TIMEPERIOD_DAYS, "'days'");
	parserTokenParaphases.put(TIMEPERIOD_HOUR, "'hour'");
	parserTokenParaphases.put(TIMEPERIOD_HOURS, "'hours'");
	parserTokenParaphases.put(TIMEPERIOD_MINUTE, "'minute'");
	parserTokenParaphases.put(TIMEPERIOD_MINUTES, "'minutes'");
	parserTokenParaphases.put(TIMEPERIOD_SEC, "'sec'");
	parserTokenParaphases.put(TIMEPERIOD_SECOND, "'second'");
	parserTokenParaphases.put(TIMEPERIOD_SECONDS, "'seconds'");
	parserTokenParaphases.put(TIMEPERIOD_MILLISEC, "'msec'");
	parserTokenParaphases.put(TIMEPERIOD_MILLISECOND, "'millisecond'");
	parserTokenParaphases.put(TIMEPERIOD_MILLISECONDS, "'milliseconds'");
	parserTokenParaphases.put(TIMEPERIOD_MICROSEC, "'usec'");
	parserTokenParaphases.put(TIMEPERIOD_MICROSECOND, "'microsecond'");
	parserTokenParaphases.put(TIMEPERIOD_MICROSECONDS, "'microseconds'");
	parserTokenParaphases.put(BOOLEAN_TRUE, "'true'");
	parserTokenParaphases.put(BOOLEAN_FALSE, "'false'");
	parserTokenParaphases.put(VALUE_NULL, "'null'");
	parserTokenParaphases.put(ROW_LIMIT_EXPR, "'limit'");
	parserTokenParaphases.put(OFFSET, "'offset'");
	parserTokenParaphases.put(UPDATE, "'update'");
	parserTokenParaphases.put(MATCH_RECOGNIZE, "'match_recognize'");
	parserTokenParaphases.put(MEASURES, "'measures'");
	parserTokenParaphases.put(DEFINE, "'define'");
	parserTokenParaphases.put(PARTITION, "'partition'");
	parserTokenParaphases.put(MATCHES, "'matches'");
	parserTokenParaphases.put(AFTER, "'after'");
	parserTokenParaphases.put(FOR, "'for'");
	parserTokenParaphases.put(WHILE, "'while'");
	parserTokenParaphases.put(MERGE, "'merge'");
	parserTokenParaphases.put(MATCHED, "'matched'");
	parserTokenParaphases.put(CONTEXT, "'context'");
	parserTokenParaphases.put(START, "'start'");
	parserTokenParaphases.put(END, "'end'");
	parserTokenParaphases.put(INITIATED, "'initiated'");
	parserTokenParaphases.put(TERMINATED, "'terminated'");
	parserTokenParaphases.put(USING, "'using'");
	parserTokenParaphases.put(EXPRESSIONDECL, "'expression'");
	parserTokenParaphases.put(NEWKW, "'new'");
	parserTokenParaphases.put(DATAFLOW, "'dataflow'");
	parserTokenParaphases.put(VALUES, "'values'");
	parserTokenParaphases.put(CUBE, "'cube'");
	parserTokenParaphases.put(ROLLUP, "'rollup'");
	parserTokenParaphases.put(GROUPING, "'grouping'");
	parserTokenParaphases.put(GROUPING_ID, "'grouping_id'");
	parserTokenParaphases.put(SETS, "'sets'");

	parserKeywordSet = new java.util.TreeSet<String>(parserTokenParaphases.values());
    }
    return parserTokenParaphases;
  }

  public synchronized static java.util.Set<Integer> getAfterScriptTokens() {
    if (afterScriptTokens.size() == 0) {
	afterScriptTokens.add(CREATE);
	afterScriptTokens.add(EXPRESSIONDECL);
	afterScriptTokens.add(SELECT);
	afterScriptTokens.add(INSERT);
	afterScriptTokens.add(ON);
	afterScriptTokens.add(DELETE);
	afterScriptTokens.add(UPDATE);
	afterScriptTokens.add(ATCHAR);
    }
    return afterScriptTokens;
  }
}

//----------------------------------------------------------------------------
// Start Rules
//----------------------------------------------------------------------------
startPatternExpressionRule : (annotationEnum | expressionDecl)* patternExpression EOF;
	
startEPLExpressionRule : (annotationEnum | expressionDecl)* eplExpression EOF;

startEventPropertyRule : eventProperty EOF;

startJsonValueRule : jsonvalue EOF;

//----------------------------------------------------------------------------
// Expression Declaration
//----------------------------------------------------------------------------
expressionDecl : EXPRESSIONDECL classIdentifier? (array=LBRACK RBRACK)? typeExpressionAnnotation? expressionDialect? name=IDENT (LPAREN columnList? RPAREN)? (alias=IDENT FOR)? expressionDef;

expressionDialect : d=IDENT COLON;
	
expressionDef :	LCURLY expressionLambdaDecl? expression RCURLY 		
		| LBRACK stringconstant RBRACK 
		;

expressionLambdaDecl : (i=IDENT | (LPAREN columnList RPAREN)) (GOES | FOLLOWED_BY);

expressionTypeAnno : ATCHAR n=IDENT (LPAREN v=IDENT RPAREN);

//----------------------------------------------------------------------------
// Annotations
//----------------------------------------------------------------------------
annotationEnum : ATCHAR classIdentifier ( '(' ( elementValuePairsEnum | elementValueEnum )? ')' )?;
    
elementValuePairsEnum : elementValuePairEnum (COMMA elementValuePairEnum)*;
    
elementValuePairEnum : keywordAllowedIdent '=' elementValueEnum;
    
elementValueEnum : annotationEnum
		| elementValueArrayEnum 
		| constant
		| v=IDENT
		| classIdentifier
    		;

elementValueArrayEnum : '{' (elementValueEnum (',' elementValueEnum)*)? (',')? '}';
    
//----------------------------------------------------------------------------
// EPL expression
//----------------------------------------------------------------------------
eplExpression : contextExpr? 
		(selectExpr
		| createWindowExpr
		| createIndexExpr
		| createVariableExpr
		| createTableExpr
		| createSchemaExpr
		| createContextExpr
		| createExpressionExpr
		| onExpr
		| updateExpr
		| createDataflow
		| fafDelete
		| fafUpdate
		| fafInsert) forExpr?
		;
	
contextExpr : CONTEXT i=IDENT;
	
selectExpr :    (INTO intoTableExpr)?
		(INSERT insertIntoExpr)? 
		SELECT selectClause
		(FROM fromClause)?
		matchRecog?
		(WHERE whereClause)?
		(GROUP BY groupByListExpr)? 
		(HAVING havingClause)?
		(OUTPUT outputLimit)?
		(ORDER BY orderByListExpr)?
		(ROW_LIMIT_EXPR rowLimit)?
		;
	
onExpr : ON onStreamExpr
	(onDeleteExpr | onSelectExpr (onSelectInsertExpr+ outputClauseInsert?)? | onSetExpr | onUpdateExpr | onMergeExpr)
	;
	
onStreamExpr : (eventFilterExpression | patternInclusionExpression) (AS identOrTicked | identOrTicked)?;

updateExpr : UPDATE ISTREAM updateDetails;
	
updateDetails :	classIdentifier (AS identOrTicked | identOrTicked)? SET onSetAssignmentList (WHERE whereClause)?;

onMergeExpr : MERGE INTO? n=IDENT (AS identOrTicked | identOrTicked)? (onMergeDirectInsert | (WHERE whereClause)? mergeItem+);

mergeItem : (mergeMatched | mergeUnmatched);
	
mergeMatched : WHEN MATCHED (AND_EXPR expression)? mergeMatchedItem+;

mergeMatchedItem : THEN (
		  ( u=UPDATE SET onSetAssignmentList) (WHERE whereClause)?
		  | d=DELETE (WHERE whereClause)? 
		  | mergeInsert
		  )
		  ;		
		  
onMergeDirectInsert: INSERT (LPAREN columnList RPAREN)? SELECT selectionList;

mergeUnmatched : WHEN NOT_EXPR MATCHED (AND_EXPR expression)? mergeUnmatchedItem+;
	
mergeUnmatchedItem : THEN mergeInsert;		
	
mergeInsert : INSERT (INTO classIdentifier)? (LPAREN columnList RPAREN)? SELECT selectionList (WHERE whereClause)?;
	
onSelectExpr	
@init  { paraphrases.push("on-select clause"); }
@after { paraphrases.pop(); }
		: (INSERT insertIntoExpr)?		
		SELECT (AND_EXPR? d=DELETE)? DISTINCT? selectionList
		onExprFrom?
		(WHERE whereClause)?		
		(GROUP BY groupByListExpr)?
		(HAVING havingClause)?
		(ORDER BY orderByListExpr)?
		(ROW_LIMIT_EXPR rowLimit)?
		;
	
onUpdateExpr	
@init  { paraphrases.push("on-update clause"); }
@after { paraphrases.pop(); }
		: UPDATE n=IDENT (AS identOrTicked | identOrTicked)? SET onSetAssignmentList (WHERE whereClause)?;

onSelectInsertExpr
@init  { paraphrases.push("on-select-insert clause"); }
@after { paraphrases.pop(); }
		: INSERT insertIntoExpr SELECT selectionList onSelectInsertFromClause? (WHERE whereClause)?;
	
onSelectInsertFromClause
		: FROM propertyExpression (AS identOrTicked | identOrTicked)?;

outputClauseInsert : OUTPUT (f=FIRST | a=ALL);
	
onDeleteExpr	
@init  { paraphrases.push("on-delete clause"); }
@after { paraphrases.pop(); }
		: DELETE onExprFrom (WHERE whereClause)?;
	
onSetExpr
@init  { paraphrases.push("on-set clause"); }
@after { paraphrases.pop(); }
		: SET onSetAssignmentList;

onSetAssignmentList : onSetAssignment (COMMA onSetAssignment)*;
	
onSetAssignment : eventProperty EQUALS expression | expression;
		
onExprFrom : FROM n=IDENT (AS identOrTicked | identOrTicked)?;

createWindowExpr : CREATE WINDOW i=IDENT viewExpressions? (ru=RETAINUNION|ri=RETAININTERSECTION)? AS? 
		  (
		  	createWindowExprModelAfter		  
		  |   	LPAREN createColumnList RPAREN
		  )		
		  (i1=INSERT (WHERE expression)? )?;

createWindowExprModelAfter : (SELECT createSelectionList FROM)? classIdentifier;
		
createIndexExpr : CREATE (u=IDENT)? INDEX n=IDENT ON w=IDENT LPAREN createIndexColumnList RPAREN;
	
createIndexColumnList : createIndexColumn (COMMA createIndexColumn)*;	

createIndexColumn : (expression | LPAREN i=expressionList? RPAREN) (t=IDENT (LPAREN p=expressionList? RPAREN)? )?;	

createVariableExpr : CREATE c=IDENT? VARIABLE classIdentifier (arr=LBRACK p=IDENT? RBRACK)? n=IDENT (EQUALS expression)?;

createTableExpr : CREATE TABLE n=IDENT AS? LPAREN createTableColumnList RPAREN; 

createTableColumnList : createTableColumn (COMMA createTableColumn)*;

createTableColumn : n=IDENT (createTableColumnPlain | builtinFunc | libFunction) p=IDENT? k=IDENT? (typeExpressionAnnotation | annotationEnum)*;

createTableColumnPlain : classIdentifier (b=LBRACK p=IDENT? RBRACK)?;

createColumnList 	
@init  { paraphrases.push("column list"); }
@after { paraphrases.pop(); }
		: createColumnListElement (COMMA createColumnListElement)*;
	
createColumnListElement : classIdentifier (VALUE_NULL | (classIdentifier (b=LBRACK p=IDENT? RBRACK)?)) ;

createSelectionList 	
@init  { paraphrases.push("select clause"); }
@after { paraphrases.pop(); }
		: createSelectionListElement (COMMA createSelectionListElement)* ;

createSelectionListElement : s=STAR
			     | eventProperty (AS i=IDENT)?
			     | constant AS i=IDENT;

createSchemaExpr : CREATE keyword=IDENT? createSchemaDef;

createSchemaDef : SCHEMA name=IDENT AS? 
		  (
			variantList
		  |   	LPAREN createColumnList? RPAREN 
		  ) createSchemaQual*;

fafDelete : DELETE FROM classIdentifier (AS identOrTicked | identOrTicked)? (WHERE whereClause)?;

fafUpdate : UPDATE updateDetails;

fafInsert : INSERT insertIntoExpr VALUES LPAREN expressionList RPAREN;

createDataflow : CREATE DATAFLOW name=IDENT AS? gopList;
	
gopList : gop gop*;
	
gop : annotationEnum* (opName=IDENT | s=SELECT) gopParams? gopOut? LCURLY gopDetail? COMMA? RCURLY
                | createSchemaExpr COMMA;	
	
gopParams : LPAREN gopParamsItemList RPAREN;
	
gopParamsItemList : gopParamsItem (COMMA gopParamsItem)*;
		
gopParamsItem :	(n=classIdentifier | gopParamsItemMany) gopParamsItemAs?;

gopParamsItemMany : LPAREN classIdentifier (COMMA classIdentifier) RPAREN;

gopParamsItemAs : AS a=IDENT;

gopOut : FOLLOWED_BY gopOutItem (COMMA gopOutItem)*;

gopOutItem : n=classIdentifier gopOutTypeList?;
	
gopOutTypeList : LT gopOutTypeParam (COMMA gopOutTypeParam)* GT;	

gopOutTypeParam : (gopOutTypeItem | q=QUESTION);

gopOutTypeItem : classIdentifier gopOutTypeList?;

gopDetail : gopConfig (COMMA gopConfig)*;

gopConfig : SELECT (COLON|EQUALS) LPAREN selectExpr RPAREN
                | n=IDENT (COLON|EQUALS) (expression | jsonobject | jsonarray);

createContextExpr : CREATE CONTEXT name=IDENT AS? createContextDetail;
	
createExpressionExpr : CREATE expressionDecl;

createContextDetail : createContextChoice
                | contextContextNested COMMA contextContextNested (COMMA contextContextNested)*;
	
contextContextNested : CONTEXT name=IDENT AS? createContextChoice;
	
createContextChoice : START (ATCHAR i=IDENT | r1=createContextRangePoint) (END r2=createContextRangePoint)?
		| INITIATED (BY)? createContextDistinct? (ATCHAR i=IDENT AND_EXPR)? r1=createContextRangePoint (TERMINATED (BY)? r2=createContextRangePoint)?
		| PARTITION (BY)? createContextPartitionItem (COMMA createContextPartitionItem)* createContextPartitionInit? createContextPartitionTerm? 
		| createContextGroupItem (COMMA createContextGroupItem)* FROM eventFilterExpression
		| COALESCE (BY)? createContextCoalesceItem (COMMA createContextCoalesceItem)* g=IDENT number (p=IDENT)?;
	
createContextDistinct :	DISTINCT LPAREN expressionList? RPAREN;
	
createContextRangePoint : createContextFilter 
                | patternInclusionExpression (ATCHAR i=IDENT)?
                | crontabLimitParameterSet
                | AFTER timePeriod;
		
createContextFilter : eventFilterExpression (AS? i=IDENT)?;

createContextPartitionItem : eventProperty ((AND_EXPR|COMMA) eventProperty)* FROM eventFilterExpression (AS? keywordAllowedIdent)?;
	
createContextCoalesceItem : libFunctionNoClass FROM eventFilterExpression;

createContextGroupItem : GROUP BY? expression AS i=IDENT;	

createContextPartitionInit : INITIATED (BY)? createContextFilter (COMMA createContextFilter)*;

createContextPartitionTerm : TERMINATED (BY)? createContextRangePoint;

createSchemaQual : i=IDENT columnList;

variantList : variantListElement (COMMA variantListElement)*;

variantListElement : STAR 
                | classIdentifier;


intoTableExpr
@init  { paraphrases.push("into-table clause"); }
@after { paraphrases.pop(); }
		: TABLE i=IDENT;

insertIntoExpr
@init  { paraphrases.push("insert-into clause"); }
@after { paraphrases.pop(); }
		: (i=ISTREAM | r=RSTREAM | ir=IRSTREAM)? INTO classIdentifier (LPAREN columnList? RPAREN)?;
		
columnList : IDENT (COMMA IDENT)*;
	
fromClause 
@init  { paraphrases.push("from clause"); }
@after { paraphrases.pop(); }
		: streamExpression (regularJoin | outerJoinList);
	
regularJoin : (COMMA streamExpression)*;
	
outerJoinList :	outerJoin (outerJoin)*;

outerJoin
@init  { paraphrases.push("outer join"); }
@after { paraphrases.pop(); }
		: (
	          ((tl=LEFT|tr=RIGHT|tf=FULL) OUTER)? 
	          | (i=INNER)
	        ) JOIN streamExpression outerJoinIdent?;

outerJoinIdent : ON outerJoinIdentPair (AND_EXPR outerJoinIdentPair)*;
	
outerJoinIdentPair : eventProperty EQUALS eventProperty ;

whereClause
@init  { paraphrases.push("where clause"); }
@after { paraphrases.pop(); }
		: evalOrExpression;
	
selectClause
@init  { paraphrases.push("select clause"); }
@after { paraphrases.pop(); }
		: (s=RSTREAM | s=ISTREAM | s=IRSTREAM)? d=DISTINCT? selectionList;

selectionList :	selectionListElement (COMMA selectionListElement)*;

selectionListElement : s=STAR
                | streamSelector
                | selectionListElementExpr;
	
selectionListElementExpr : expression selectionListElementAnno? (AS? keywordAllowedIdent)?;

selectionListElementAnno : ATCHAR i=IDENT;
	
streamSelector : s=IDENT DOT STAR (AS i=IDENT)?;
	
streamExpression : (eventFilterExpression | patternInclusionExpression | databaseJoinExpression | methodJoinExpression )
		viewExpressions? (AS identOrTicked | identOrTicked)? (u=UNIDIRECTIONAL)? (ru=RETAINUNION|ri=RETAININTERSECTION)?;
		
forExpr : FOR i=IDENT (LPAREN expressionList? RPAREN)?;


patternInclusionExpression : PATTERN annotationEnum* LBRACK patternExpression RBRACK;
	
databaseJoinExpression
@init  { paraphrases.push("relational data join"); }
@after { paraphrases.pop(); }
		: SQL COLON i=IDENT LBRACK (s=STRING_LITERAL | s=QUOTED_STRING_LITERAL) (METADATASQL (s2=STRING_LITERAL | s2=QUOTED_STRING_LITERAL))? RBRACK;	
	
methodJoinExpression
@init  { paraphrases.push("method invocation join"); }
@after { paraphrases.pop(); }
    		: i=IDENT COLON classIdentifier (LPAREN expressionList? RPAREN)? typeExpressionAnnotation?;

viewExpressions 
@init  { paraphrases.push("view specifications"); }
@after { paraphrases.pop(); }
		: (DOT viewExpressionWNamespace (DOT viewExpressionWNamespace)*) 
		| (HASHCHAR viewExpressionOptNamespace (HASHCHAR viewExpressionOptNamespace)*);

viewExpressionWNamespace : ns=IDENT COLON viewWParameters;

viewExpressionOptNamespace : (ns=IDENT COLON)? viewWParameters;

viewWParameters : (i=IDENT|m=MERGE) (LPAREN expressionWithTimeList? RPAREN)?;

groupByListExpr
@init  { paraphrases.push("group-by clause"); }
@after { paraphrases.pop(); }
		: groupByListChoice (COMMA groupByListChoice)*;

groupByListChoice : e1=expression | groupByCubeOrRollup | groupByGroupingSets;

groupByCubeOrRollup : (CUBE | ROLLUP) LPAREN groupByCombinableExpr (COMMA groupByCombinableExpr)* RPAREN;

groupByGroupingSets : GROUPING SETS LPAREN groupBySetsChoice (COMMA groupBySetsChoice)* RPAREN;

groupBySetsChoice : groupByCubeOrRollup | groupByCombinableExpr;
		
groupByCombinableExpr : e1=expression | LPAREN (expression (COMMA expression)*)? RPAREN;

orderByListExpr
@init  { paraphrases.push("order by clause"); }
@after { paraphrases.pop(); }
		: orderByListElement (COMMA orderByListElement)*;

orderByListElement
		: expression (a=ASC|d=DESC)?;

havingClause
@init  { paraphrases.push("having clause"); }
@after { paraphrases.pop(); }
		: evalOrExpression;

outputLimit
@init  { paraphrases.push("output rate clause"); }
@after { paraphrases.pop(); }
		: outputLimitAfter?
 	       (k=ALL|k=FIRST|k=LAST|k=SNAPSHOT)? 
	        (
	          ( ev=EVERY_EXPR 
		    ( 
		      timePeriod
		    | (number | i=IDENT) (e=EVENTS)
		    )
		  )
		  |
		  ( at=AT crontabLimitParameterSet)
		  |
		  ( wh=WHEN expression (THEN onSetExpr)? )
		  |
		  ( t=WHEN TERMINATED (AND_EXPR expression)? (THEN onSetExpr)? )
		  |
	        ) 
	        outputLimitAndTerm?;
	
outputLimitAndTerm : AND_EXPR WHEN TERMINATED (AND_EXPR expression)? (THEN onSetExpr)?;

outputLimitAfter : a=AFTER (timePeriod | number EVENTS);	

rowLimit
@init  { paraphrases.push("row limit clause"); }
@after { paraphrases.pop(); }
		: (n1=numberconstant | i1=IDENT) ((c=COMMA | o=OFFSET) (n2=numberconstant | i2=IDENT))?;	

crontabLimitParameterSet : LPAREN expressionWithTimeList RPAREN;			

whenClause : (WHEN expression THEN expression);

elseClause : (ELSE expression);

//----------------------------------------------------------------------------
// Match recognize
//----------------------------------------------------------------------------
//
// Lowest precedence is listed first, order is (highest to lowest):  
// Single-character-ERE duplication * + ? {m,n}
// Concatenation
// Anchoring ^ $
// Alternation  |
//
matchRecog : MATCH_RECOGNIZE LPAREN matchRecogPartitionBy? matchRecogMeasures matchRecogMatchesSelection? matchRecogMatchesAfterSkip? matchRecogPattern 
		matchRecogMatchesInterval? matchRecogDefine? RPAREN ;

matchRecogPartitionBy : PARTITION BY expression (COMMA expression)*;		
		
matchRecogMeasures : MEASURES matchRecogMeasureItem (COMMA matchRecogMeasureItem)*;
	
matchRecogMeasureItem : expression (AS (i=IDENT)? )?;
	
matchRecogMatchesSelection : ALL MATCHES;
		
matchRecogPattern : PATTERN LPAREN matchRecogPatternAlteration RPAREN;
	
matchRecogMatchesAfterSkip : AFTER i1=keywordAllowedIdent i2=keywordAllowedIdent i3=keywordAllowedIdent i4=keywordAllowedIdent i5=keywordAllowedIdent;

matchRecogMatchesInterval : i=IDENT timePeriod (OR_EXPR t=TERMINATED)?;
		
matchRecogPatternAlteration : matchRecogPatternConcat (o=BOR matchRecogPatternConcat)*;	

matchRecogPatternConcat : matchRecogPatternUnary+;	

matchRecogPatternUnary : matchRecogPatternPermute | matchRecogPatternNested | matchRecogPatternAtom;

matchRecogPatternNested : LPAREN matchRecogPatternAlteration RPAREN (s=STAR | s=PLUS | s=QUESTION)? matchRecogPatternRepeat?;

matchRecogPatternPermute : MATCH_RECOGNIZE_PERMUTE LPAREN matchRecogPatternAlteration (COMMA matchRecogPatternAlteration)* RPAREN;
		
matchRecogPatternAtom :	i=IDENT ((s=STAR | s=PLUS | s=QUESTION) (reluctant=QUESTION)? )? matchRecogPatternRepeat?;
	
matchRecogPatternRepeat : LCURLY e1=expression? comma=COMMA? e2=expression? RCURLY;

matchRecogDefine : DEFINE matchRecogDefineItem (COMMA matchRecogDefineItem)*;	

matchRecogDefineItem : i=IDENT AS expression;	

//----------------------------------------------------------------------------
// Expression
//----------------------------------------------------------------------------
expression : caseExpression;

caseExpression : { paraphrases.push("case expression"); }  CASE whenClause+ elseClause? END { paraphrases.pop(); }
		| { paraphrases.push("case expression"); }  CASE expression whenClause+ elseClause? END { paraphrases.pop(); }
		| evalOrExpression;

evalOrExpression : evalAndExpression (op=OR_EXPR evalAndExpression)*;

evalAndExpression : bitWiseExpression (op=AND_EXPR bitWiseExpression)*;

bitWiseExpression : negatedExpression ( (BAND|BOR|BXOR) negatedExpression)* ;		

negatedExpression : evalEqualsExpression 
		| NOT_EXPR evalEqualsExpression;		

evalEqualsExpression : evalRelationalExpression ( 
			    (eq=EQUALS
			      |  is=IS
			      |  isnot=IS NOT_EXPR
			      |  sqlne=SQL_NE
			      |  ne=NOT_EQUAL
			     ) 
		       (
			evalRelationalExpression
			|  (a=ANY | a=SOME | a=ALL) ( (LPAREN expressionList? RPAREN) | subSelectGroupExpression )
		       )
		     )*;

evalRelationalExpression : concatenationExpr ( 
			( 
			  ( 
			    (r=LT|r=GT|r=LE|r=GE) 
			    	(
			    	  concatenationExpr
			    	  | (g=ANY | g=SOME | g=ALL) ( (LPAREN expressionList? RPAREN) | subSelectGroupExpression )
			    	)
			    	
			  )*
			)  
			| (n=NOT_EXPR)? 
			(
				// Represent the optional NOT prefix using the token type by
				// testing 'n' and setting the token type accordingly.
				(in=IN_SET
					  (l=LPAREN | l=LBRACK) expression	// brackets are for inclusive/exclusive
						(
							( col=COLON (expression) )		// range
							|
							( (COMMA expression)* )		// list of values
						)
					  (r=RPAREN | r=RBRACK)	
					)
				| inset=IN_SET inSubSelectQuery
				| between=BETWEEN betweenList
				| like=LIKE concatenationExpr (ESCAPE stringconstant)?
				| regex=REGEXP concatenationExpr
			)	
		);
	
inSubSelectQuery : subQueryExpr;
			
concatenationExpr : additiveExpression ( c=LOR additiveExpression ( LOR additiveExpression)* )?;

additiveExpression : multiplyExpression ( (PLUS|MINUS) multiplyExpression )*;

multiplyExpression : unaryExpression ( (STAR|DIV|MOD) unaryExpression )*;
	
unaryExpression : MINUS eventProperty
		| constant
		| substitutionCanChain
		| inner=LPAREN expression RPAREN chainedFunction?
		| builtinFunc
		| eventPropertyOrLibFunction
		| arrayExpression
		| rowSubSelectExpression 
		| existsSubSelectExpression
		| NEWKW LCURLY newAssign (COMMA newAssign)* RCURLY
		| NEWKW classIdentifier LPAREN (expression (COMMA expression)*)? RPAREN chainedFunction?
		| b=IDENT LBRACK expression (COMMA expression)* RBRACK chainedFunction?
		| jsonobject
		;

substitutionCanChain : substitution chainedFunction?;
	
chainedFunction : d=DOT libFunctionNoClass (d=DOT libFunctionNoClass)*;
	
newAssign : eventProperty (EQUALS expression)?;
	
rowSubSelectExpression : subQueryExpr chainedFunction?;

subSelectGroupExpression : subQueryExpr;

existsSubSelectExpression : EXISTS subQueryExpr;

subQueryExpr 
@init  { paraphrases.push("subquery"); }
@after { paraphrases.pop(); }
		: LPAREN  SELECT DISTINCT? selectionList FROM subSelectFilterExpr (WHERE whereClause)? (GROUP BY groupByListExpr)? (HAVING havingClause)? RPAREN;
	
subSelectFilterExpr
@init  { paraphrases.push("subquery filter specification"); }
@after { paraphrases.pop(); }
		: eventFilterExpression viewExpressions? (AS identOrTicked | identOrTicked)? (ru=RETAINUNION|ri=RETAININTERSECTION)?;
		
arrayExpression : LCURLY (expression (COMMA expression)* )? RCURLY chainedFunction?;

builtinFunc : SUM LPAREN (ALL | DISTINCT)? expressionListWithNamed RPAREN   			#builtin_sum
		| AVG LPAREN (ALL | DISTINCT)? expressionListWithNamed RPAREN			#builtin_avg
		| COUNT LPAREN (a=ALL | d=DISTINCT)? expressionListWithNamed RPAREN		#builtin_cnt
		| MEDIAN LPAREN (ALL | DISTINCT)? expressionListWithNamed RPAREN		#builtin_median
		| STDDEV LPAREN (ALL | DISTINCT)? expressionListWithNamed RPAREN		#builtin_stddev
		| AVEDEV LPAREN (ALL | DISTINCT)? expressionListWithNamed RPAREN		#builtin_avedev
		| firstLastWindowAggregation							#builtin_firstlastwindow
		| COALESCE LPAREN expression COMMA expression (COMMA expression)* RPAREN	#builtin_coalesce
		| PREVIOUS LPAREN expression (COMMA expression)? RPAREN chainedFunction?	#builtin_prev
		| PREVIOUSTAIL LPAREN expression (COMMA expression)? RPAREN chainedFunction?	#builtin_prevtail
		| PREVIOUSCOUNT LPAREN expression RPAREN					#builtin_prevcount
		| PREVIOUSWINDOW LPAREN expression RPAREN chainedFunction?			#builtin_prevwindow
		| PRIOR LPAREN expression COMMA eventProperty RPAREN				#builtin_prior
		| GROUPING LPAREN expression RPAREN						#builtin_grouping
		| GROUPING_ID LPAREN expressionList RPAREN					#builtin_groupingid
		// MIN and MAX can also be "Math.min" static function and "min(price)" aggregation function and "min(a, b, c...)" built-in function
		// therefore handled in code via libFunction as below
		| INSTANCEOF LPAREN expression COMMA classIdentifier (COMMA classIdentifier)* RPAREN	#builtin_instanceof
		| TYPEOF LPAREN expression RPAREN							#builtin_typeof
		| CAST LPAREN expression (COMMA | AS) classIdentifier (COMMA expressionNamedParameter)? RPAREN chainedFunction?	#builtin_cast
		| EXISTS LPAREN eventProperty RPAREN						#builtin_exists
		| CURRENT_TIMESTAMP (LPAREN RPAREN)? chainedFunction?				#builtin_currts
		| ISTREAM LPAREN RPAREN								#builtin_istream
		;
	
firstLastWindowAggregation : (q=FIRST | q=LAST | q=WINDOW) LPAREN expressionListWithNamed? RPAREN chainedFunction?;
		
eventPropertyOrLibFunction : eventProperty | libFunction;
	
libFunction: libFunctionWithClass (DOT libFunctionNoClass)*;
				
libFunctionWithClass : ((classIdentifier DOT funcIdentInner) | funcIdentTop) (l=LPAREN libFunctionArgs? RPAREN)?;

libFunctionNoClass : funcIdentChained (l=LPAREN libFunctionArgs? RPAREN)?;	

funcIdentTop : escapableIdent
		| MAX 
		| MIN;

funcIdentInner : escapableIdent
		| LAST 
		| FIRST
		| WINDOW;

funcIdentChained : escapableIdent
		| LAST 
		| FIRST
		| WINDOW
		| MAX 
		| MIN 
		| WHERE 
		| SET 
		| AFTER 
		| BETWEEN;
	
libFunctionArgs : (ALL | DISTINCT)? libFunctionArgItem (COMMA libFunctionArgItem)*;
	
libFunctionArgItem : expressionLambdaDecl? expressionWithNamed;

betweenList : concatenationExpr AND_EXPR concatenationExpr;

//----------------------------------------------------------------------------
// Pattern event expressions / event pattern operators
//   Operators are: followed-by (->), or, and, not, every, where
//   Lowest precedence is listed first, order is (lowest to highest):  ->, or, and, not/every, within.
//   On the atomic level an expression has filters, and observer-statements.
//----------------------------------------------------------------------------
patternExpression
@init  { paraphrases.push("pattern expression"); }
@after { paraphrases.pop(); }
		: followedByExpression;

followedByExpression : orExpression (followedByRepeat)*;
	
followedByRepeat : (f=FOLLOWED_BY | (g=FOLLOWMAX_BEGIN expression FOLLOWMAX_END)) orExpression;
	
orExpression : andExpression (o=OR_EXPR andExpression)*;

andExpression :	matchUntilExpression (a=AND_EXPR matchUntilExpression)*;

matchUntilExpression : (r=matchUntilRange)? qualifyExpression (UNTIL until=qualifyExpression)?;

qualifyExpression : ((e=EVERY_EXPR | n=NOT_EXPR | d=EVERY_DISTINCT_EXPR distinctExpressionList) matchUntilRange? )? guardPostFix;

guardPostFix : (atomicExpression | l=LPAREN patternExpression RPAREN) ((wh=WHERE guardWhereExpression) | (wi=WHILE guardWhileExpression))?;
		
distinctExpressionList : LPAREN distinctExpressionAtom (COMMA distinctExpressionAtom)* RPAREN;

distinctExpressionAtom : expressionWithTime;

atomicExpression : observerExpression | patternFilterExpression;
		
observerExpression : ns=IDENT COLON (nm=IDENT | a=AT) LPAREN expressionListWithNamedWithTime? RPAREN;

guardWhereExpression : IDENT COLON IDENT LPAREN (expressionWithTimeList)? RPAREN;
	
guardWhileExpression : LPAREN expression RPAREN;

// syntax is [a:b] or [:b] or [a:] or [a]
matchUntilRange : LBRACK ( low=expression (c1=COLON high=expression?)? | c2=COLON upper=expression) RBRACK;
	
//----------------------------------------------------------------------------
// Filter expressions
//   Operators are the usual bunch =, <, >, <=, >= 
//	 Ranges such as 'property in [a,b]' are allowed and ([ and )] distinguish open/closed range endpoints
//----------------------------------------------------------------------------
eventFilterExpression
@init  { paraphrases.push("filter specification"); }
@after { paraphrases.pop(); }
    :   (i=IDENT EQUALS)? classIdentifier (LPAREN expressionList? RPAREN)? propertyExpression?;
    
propertyExpression : propertyExpressionAtomic (propertyExpressionAtomic)*;

propertyExpressionAtomic : LBRACK propertyExpressionSelect? expression typeExpressionAnnotation? (AS n=IDENT)? (WHERE where=expression)? RBRACK;
       	
propertyExpressionSelect : SELECT propertySelectionList FROM;
		
propertySelectionList : propertySelectionListElement (COMMA propertySelectionListElement)*;

propertySelectionListElement : s=STAR
		| propertyStreamSelector
		| expression (AS keywordAllowedIdent)?;
	
propertyStreamSelector : s=IDENT DOT STAR (AS i=IDENT)?;

typeExpressionAnnotation : ATCHAR n=IDENT (LPAREN v=IDENT RPAREN);
	
patternFilterExpression
@init  { paraphrases.push("filter specification"); }
@after { paraphrases.pop(); }
    		: (i=IDENT EQUALS)? classIdentifier (LPAREN expressionList? RPAREN)? propertyExpression? patternFilterAnnotation?;
       	
patternFilterAnnotation : ATCHAR i=IDENT (LPAREN number RPAREN)?;

classIdentifier : i1=escapableStr (DOT i2=escapableStr)*;
		
slashIdentifier : (d=DIV)? i1=escapableStr (DIV i2=escapableStr)*;

expressionListWithNamed : expressionWithNamed (COMMA expressionWithNamed)*;

expressionListWithNamedWithTime : expressionWithNamedWithTime (COMMA expressionWithNamedWithTime)*;

expressionWithNamed : expressionNamedParameter | expressionWithTime;

expressionWithNamedWithTime : expressionNamedParameterWithTime | expressionWithTimeInclLast;

expressionNamedParameter : IDENT COLON (expression | LPAREN expressionList? RPAREN);

expressionNamedParameterWithTime : IDENT COLON (expressionWithTime | LPAREN expressionWithTimeList? RPAREN);

expressionList : expression (COMMA expression)*;
   	
expressionWithTimeList : expressionWithTimeInclLast (COMMA expressionWithTimeInclLast)*;

expressionWithTime : lastWeekdayOperand
		| timePeriod
		| expressionQualifyable
		| rangeOperand
		| frequencyOperand
		| lastOperator
		| weekDayOperator
		| numericParameterList
		| STAR
		| propertyStreamSelector
		;

expressionWithTimeInclLast : lastOperand
		| expressionWithTime
		;

expressionQualifyable : expression (a=ASC|d=DESC|s=TIMEPERIOD_SECONDS|s=TIMEPERIOD_SECOND|s=TIMEPERIOD_SEC)?;
		
lastWeekdayOperand : LW;
	
lastOperand : LAST;

frequencyOperand : STAR DIV (number|i=IDENT|substitution);

rangeOperand : (n1=number|i1=IDENT|s1=substitution) COLON (n2=number|i2=IDENT|s2=substitution);

lastOperator : (number|i=IDENT|substitution) LAST;

weekDayOperator : (number|i=IDENT|substitution) WEEKDAY;

numericParameterList : LBRACK numericListParameter (COMMA numericListParameter)* RBRACK;

numericListParameter : rangeOperand
		| frequencyOperand
		| numberconstant;
	    
eventProperty : eventPropertyAtomic (DOT eventPropertyAtomic)*;
	
eventPropertyAtomic : eventPropertyIdent (
			lb=LBRACK ni=number RBRACK (q=QUESTION)?
			|
			lp=LPAREN (s=STRING_LITERAL | s=QUOTED_STRING_LITERAL) RPAREN (q=QUESTION)?
			|
			q1=QUESTION 
			)?;
		
eventPropertyIdent : ipi=keywordAllowedIdent (ESCAPECHAR DOT ipi2=keywordAllowedIdent?)*;

identOrTicked : i1=IDENT | i2=TICKED_STRING_LITERAL;
	
keywordAllowedIdent : i1=IDENT
		| i2=TICKED_STRING_LITERAL
		| AT
		| COUNT
		| ESCAPE
    		| EVERY_EXPR
		| SCHEMA
		| SUM
		| AVG
		| MAX
		| MIN
		| COALESCE
		| MEDIAN
		| STDDEV
		| AVEDEV
		| EVENTS
		| FIRST
		| LAST
		| WHILE
		| MERGE
		| MATCHED
		| UNIDIRECTIONAL
		| RETAINUNION
		| RETAININTERSECTION
		| UNTIL
		| PATTERN
		| SQL
		| METADATASQL
		| PREVIOUS
		| PREVIOUSTAIL
		| PRIOR
		| WEEKDAY
		| LW
		| INSTANCEOF
		| TYPEOF
		| CAST
		| SNAPSHOT
		| VARIABLE
		| TABLE
		| INDEX
		| WINDOW
		| LEFT
		| RIGHT
		| OUTER
		| FULL
		| JOIN
		| DEFINE
		| PARTITION
		| MATCHES
		| CONTEXT
		| FOR
		| USING;
		
escapableStr : i1=IDENT | i2=EVENTS | i3=TICKED_STRING_LITERAL;
	
escapableIdent : IDENT | t=TICKED_STRING_LITERAL;

timePeriod : (	yearPart monthPart? weekPart? dayPart? hourPart? minutePart? secondPart? millisecondPart? microsecondPart?
		| monthPart weekPart? dayPart? hourPart? minutePart? secondPart? millisecondPart? microsecondPart?
		| weekPart dayPart? hourPart? minutePart? secondPart? millisecondPart? microsecondPart?
		| dayPart hourPart? minutePart? secondPart? millisecondPart? microsecondPart?
		| hourPart minutePart? secondPart? millisecondPart? microsecondPart?
		| minutePart secondPart? millisecondPart? microsecondPart?
		| secondPart millisecondPart? microsecondPart?
		| millisecondPart microsecondPart?
		| microsecondPart 
		);

yearPart : (numberconstant|i=IDENT|substitution) (TIMEPERIOD_YEARS | TIMEPERIOD_YEAR);

monthPart : (numberconstant|i=IDENT|substitution) (TIMEPERIOD_MONTHS | TIMEPERIOD_MONTH);

weekPart : (numberconstant|i=IDENT|substitution) (TIMEPERIOD_WEEKS | TIMEPERIOD_WEEK);

dayPart : (numberconstant|i=IDENT|substitution) (TIMEPERIOD_DAYS | TIMEPERIOD_DAY);

hourPart : (numberconstant|i=IDENT|substitution) (TIMEPERIOD_HOURS | TIMEPERIOD_HOUR);

minutePart : (numberconstant|i=IDENT|substitution) (TIMEPERIOD_MINUTES | TIMEPERIOD_MINUTE | MIN);
	
secondPart : (numberconstant|i=IDENT|substitution) (TIMEPERIOD_SECONDS | TIMEPERIOD_SECOND | TIMEPERIOD_SEC);
	
millisecondPart : (numberconstant|i=IDENT|substitution) (TIMEPERIOD_MILLISECONDS | TIMEPERIOD_MILLISECOND | TIMEPERIOD_MILLISEC);
	
microsecondPart : (numberconstant|i=IDENT|substitution) (TIMEPERIOD_MICROSECONDS | TIMEPERIOD_MICROSECOND | TIMEPERIOD_MICROSEC);

number : IntegerLiteral | FloatingPointLiteral;

substitution : q=QUESTION (COLON slashIdentifier)?;
	
constant : numberconstant
		| stringconstant
		| t=BOOLEAN_TRUE 
		| f=BOOLEAN_FALSE 
		| nu=VALUE_NULL;

numberconstant : (m=MINUS | p=PLUS)? number;

stringconstant : sl=STRING_LITERAL
		| qsl=QUOTED_STRING_LITERAL;

//----------------------------------------------------------------------------
// JSON
//----------------------------------------------------------------------------
jsonvalue : constant 
		| jsonobject
		| jsonarray;

jsonobject : LCURLY jsonmembers RCURLY;
	
jsonarray : LBRACK jsonelements? RBRACK;

jsonelements : jsonvalue (COMMA jsonvalue)* (COMMA)?;
	
jsonmembers : jsonpair (COMMA jsonpair)* (COMMA)?;
	 
jsonpair : (stringconstant | keywordAllowedIdent) COLON jsonvalue;

//----------------------------------------------------------------------------
// LEXER
//----------------------------------------------------------------------------

// Tokens
CREATE:'create';
WINDOW:'window';
IN_SET:'in';
BETWEEN:'between';
LIKE:'like';
REGEXP:'regexp';
ESCAPE:'escape';
OR_EXPR:'or';
AND_EXPR:'and';
NOT_EXPR:'not';
EVERY_EXPR:'every';
EVERY_DISTINCT_EXPR:'every-distinct';
WHERE:'where';
AS:'as';	
SUM:'sum';
AVG:'avg';
MAX:'max';
MIN:'min';
COALESCE:'coalesce';
MEDIAN:'median';
STDDEV:'stddev';
AVEDEV:'avedev';
COUNT:'count';
SELECT:'select';
CASE:'case';
ELSE:'else';
WHEN:'when';
THEN:'then';
END:'end';
FROM:'from';
OUTER:'outer';
INNER:'inner';
JOIN:'join';
LEFT:'left';
RIGHT:'right';
FULL:'full';
ON:'on';	
IS:'is';
BY:'by';
GROUP:'group';
HAVING:'having';
DISTINCT:'distinct';
ALL:'all';
ANY:'any';
SOME:'some';
OUTPUT:'output';
EVENTS:'events';
FIRST:'first';
LAST:'last';
INSERT:'insert';
INTO:'into';
VALUES:'values';
ORDER:'order';
ASC:'asc';
DESC:'desc';
RSTREAM:'rstream';
ISTREAM:'istream';
IRSTREAM:'irstream';
SCHEMA:'schema';
UNIDIRECTIONAL:'unidirectional';
RETAINUNION:'retain-union';
RETAININTERSECTION:'retain-intersection';
PATTERN:'pattern';
SQL:'sql';
METADATASQL:'metadatasql';
PREVIOUS:'prev';
PREVIOUSTAIL:'prevtail';
PREVIOUSCOUNT:'prevcount';
PREVIOUSWINDOW:'prevwindow';
PRIOR:'prior';
EXISTS:'exists';
WEEKDAY:'weekday';
LW:'lastweekday';
INSTANCEOF:'instanceof';
TYPEOF:'typeof';
CAST:'cast';
CURRENT_TIMESTAMP:'current_timestamp';
DELETE:'delete';
SNAPSHOT:'snapshot';
SET:'set';
VARIABLE:'variable';
TABLE:'table';
UNTIL:'until';
AT:'at';
INDEX:'index';
TIMEPERIOD_YEAR:'year';
TIMEPERIOD_YEARS:'years';
TIMEPERIOD_MONTH:'month';
TIMEPERIOD_MONTHS:'months';
TIMEPERIOD_WEEK:'week';
TIMEPERIOD_WEEKS:'weeks';
TIMEPERIOD_DAY:'day';
TIMEPERIOD_DAYS:'days';
TIMEPERIOD_HOUR:'hour';
TIMEPERIOD_HOURS:'hours';
TIMEPERIOD_MINUTE:'minute';
TIMEPERIOD_MINUTES:'minutes';
TIMEPERIOD_SEC:'sec';
TIMEPERIOD_SECOND:'second';
TIMEPERIOD_SECONDS:'seconds';	
TIMEPERIOD_MILLISEC:'msec';
TIMEPERIOD_MILLISECOND:'millisecond';
TIMEPERIOD_MILLISECONDS:'milliseconds';
TIMEPERIOD_MICROSEC:'usec';
TIMEPERIOD_MICROSECOND:'microsecond';
TIMEPERIOD_MICROSECONDS:'microseconds';
BOOLEAN_TRUE:'true';
BOOLEAN_FALSE:'false';
VALUE_NULL:'null';
ROW_LIMIT_EXPR:'limit';
OFFSET:'offset';
UPDATE:'update';
MATCH_RECOGNIZE:'match_recognize';
MATCH_RECOGNIZE_PERMUTE:'match_recognize_permute';
MEASURES:'measures';
DEFINE:'define';
PARTITION:'partition';
MATCHES:'matches';
AFTER:'after';	
FOR:'for';	
WHILE:'while';	
USING:'using';
MERGE:'merge';
MATCHED:'matched';
EXPRESSIONDECL:'expression';
NEWKW:'new';
START:'start';
CONTEXT:'context';
INITIATED:'initiated';
TERMINATED:'terminated';
DATAFLOW:'dataflow';
CUBE:'cube';
ROLLUP:'rollup';
GROUPING:'grouping';
GROUPING_ID:'grouping_id';
SETS:'sets';

// Operators
FOLLOWMAX_BEGIN : '-[';
FOLLOWMAX_END   : ']>';
FOLLOWED_BY 	: '->';
GOES 		: '=>';
EQUALS 		: '=';
SQL_NE 		: '<>';
QUESTION 	: '?';
LPAREN 		: '(';
RPAREN 		: ')';
LBRACK 		: '[';
RBRACK 		: ']';
LCURLY 		: '{';
RCURLY 		: '}';
COLON 		: ':';
COMMA 		: ',';
EQUAL 		: '==';
LNOT 		: '!';
BNOT 		: '~';
NOT_EQUAL 	: '!=';
DIV 		: '/';
DIV_ASSIGN 	: '/=';
PLUS 		: '+';
PLUS_ASSIGN	: '+=';
INC 		: '++';
MINUS 		: '-';
MINUS_ASSIGN 	: '-=';
DEC 		: '--';
STAR 		: '*';
STAR_ASSIGN 	: '*=';
MOD 		: '%';
MOD_ASSIGN 	: '%=';
GE 		: '>=';
GT 		: '>';
LE 		: '<=';
LT 		: '<';
BXOR 		: '^';
BXOR_ASSIGN 	: '^=';
BOR		: '|';
BOR_ASSIGN 	: '|=';
LOR		: '||';
BAND 		: '&';
BAND_ASSIGN 	: '&=';
LAND 		: '&&';
SEMI 		: ';';
DOT 		: '.';
NUM_LONG	: '\u18FF';  // assign bogus unicode characters so the token exists
NUM_DOUBLE	: '\u18FE';
NUM_FLOAT	: '\u18FD';
ESCAPECHAR	: '\\';
ESCAPEBACKTICK	: '`';
ATCHAR		: '@';
HASHCHAR	: '#';

// Whitespace -- ignored
WS	:	(	' '
		|	'\t'
		|	'\f'
			// handle newlines
		|	(
				'\r'    // Macintosh
			|	'\n'    // Unix (the right way)
			)
		)+		
		-> channel(HIDDEN)
	;

// Single-line comments
SL_COMMENT
	:	'//'
		(~('\n'|'\r'))* ('\n'|'\r'('\n')?)?
		-> channel(HIDDEN)
	;

// multiple-line comments
ML_COMMENT
    	:   	'/*' (.)*? '*/'
		-> channel(HIDDEN)
    	;

TICKED_STRING_LITERAL
    :   '`' ( EscapeSequence | ~('`'|'\\') )* '`'
    ;

QUOTED_STRING_LITERAL
    :   '\'' ( EscapeSequence | ~('\''|'\\') )* '\''
    ;

STRING_LITERAL
    :  '"' ( EscapeSequence | ~('\\'|'"') )* '"'
    ;

fragment
EscapeSequence	:	'\\'
		(	'n'
		|	'r'
		|	't'
		|	'b'
		|	'f'
		|	'"'
		|	'\''
		|	'\\'
		|	UnicodeEscape
		|	OctalEscape
		|	. // unknown, leave as it is
		)
    ;    

// an identifier.  Note that testLiterals is set to true!  This means
// that after we match the rule, we look in the literals table to see
// if it's a literal or really an identifer
IDENT	
	:	('a'..'z'|'_'|'$') ('a'..'z'|'_'|'0'..'9'|'$')*
	;

IntegerLiteral
    :   DecimalIntegerLiteral
    |   HexIntegerLiteral
    |   OctalIntegerLiteral
    |   BinaryIntegerLiteral
    ;
 
FloatingPointLiteral
    :   DecimalFloatingPointLiteral
    |   HexadecimalFloatingPointLiteral
    ;

fragment
OctalEscape
    :   '\\' ('0'..'3') ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7')
    ;

fragment
UnicodeEscape
    :   '\\' 'u' HexDigit HexDigit HexDigit HexDigit
    ;
    
fragment
DecimalIntegerLiteral
    :   DecimalNumeral IntegerTypeSuffix?
    ;

fragment
HexIntegerLiteral
    :   HexNumeral IntegerTypeSuffix?
    ;

fragment
OctalIntegerLiteral
    :   OctalNumeral IntegerTypeSuffix?
    ;

fragment
BinaryIntegerLiteral
    :   BinaryNumeral IntegerTypeSuffix?
    ;

fragment
IntegerTypeSuffix
    :   [lL]
    ;

fragment
DecimalNumeral
    :   '0'
    |   ('0')* NonZeroDigit (Digits? | Underscores Digits)
    ;

fragment
Digits
    :   Digit (DigitOrUnderscore* Digit)?
    ;

fragment
Digit
    :   '0'
    |   NonZeroDigit
    ;

fragment
NonZeroDigit
    :   [1-9]
    ;

fragment
DigitOrUnderscore
    :   Digit
    |   '_'
    ;

fragment
Underscores
    :   '_'+
    ;

fragment
HexNumeral
    :   '0' [xX] HexDigits
    ;

fragment
HexDigits
    :   HexDigit (HexDigitOrUnderscore* HexDigit)?
    ;

fragment
HexDigit
    :   [0-9a-fA-F]
    ;

fragment
HexDigitOrUnderscore
    :   HexDigit
    |   '_'
    ;

fragment
OctalNumeral
    :   '0' Underscores? OctalDigits
    ;

fragment
OctalDigits
    :   OctalDigit (OctalDigitOrUnderscore* OctalDigit)?
    ;

fragment
OctalDigit
    :   [0-7]
    ;

fragment
OctalDigitOrUnderscore
    :   OctalDigit
    |   '_'
    ;

fragment
BinaryNumeral
    :   '0' [bB] BinaryDigits
    ;

fragment
BinaryDigits
    :   BinaryDigit (BinaryDigitOrUnderscore* BinaryDigit)?
    ;

fragment
BinaryDigit
    :   [01]
    ;

fragment
BinaryDigitOrUnderscore
    :   BinaryDigit
    |   '_'
    ;

fragment
DecimalFloatingPointLiteral
    :   Digits '.' Digits? ExponentPart? FloatTypeSuffix?
    |   '.' Digits ExponentPart? FloatTypeSuffix?
    |   Digits ExponentPart FloatTypeSuffix?
    |   Digits FloatTypeSuffix
    ;

fragment
ExponentPart
    :   ExponentIndicator SignedInteger
    ;

fragment
ExponentIndicator
    :   [eE]
    ;

fragment
SignedInteger
    :   Sign? Digits
    ;

fragment
Sign
    :   [+-]
    ;

fragment
FloatTypeSuffix
    :   [fFdD]
    ;

fragment
HexadecimalFloatingPointLiteral
    :   HexSignificand BinaryExponent FloatTypeSuffix?
    ;

fragment
HexSignificand
    :   HexNumeral '.'?
    |   '0' [xX] HexDigits? '.' HexDigits
    ;

fragment
BinaryExponent
    :   BinaryExponentIndicator SignedInteger
    ;

fragment
BinaryExponentIndicator
    :   [pP]
    ;    