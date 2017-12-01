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

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class EsperEPL2GrammarLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.7", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		CREATE=1, WINDOW=2, IN_SET=3, BETWEEN=4, LIKE=5, REGEXP=6, ESCAPE=7, OR_EXPR=8, 
		AND_EXPR=9, NOT_EXPR=10, EVERY_EXPR=11, EVERY_DISTINCT_EXPR=12, WHERE=13, 
		AS=14, SUM=15, AVG=16, MAX=17, MIN=18, COALESCE=19, MEDIAN=20, STDDEV=21, 
		AVEDEV=22, COUNT=23, SELECT=24, CASE=25, ELSE=26, WHEN=27, THEN=28, END=29, 
		FROM=30, OUTER=31, INNER=32, JOIN=33, LEFT=34, RIGHT=35, FULL=36, ON=37, 
		IS=38, BY=39, GROUP=40, HAVING=41, DISTINCT=42, ALL=43, ANY=44, SOME=45, 
		OUTPUT=46, EVENTS=47, FIRST=48, LAST=49, INSERT=50, INTO=51, VALUES=52, 
		ORDER=53, ASC=54, DESC=55, RSTREAM=56, ISTREAM=57, IRSTREAM=58, SCHEMA=59, 
		UNIDIRECTIONAL=60, RETAINUNION=61, RETAININTERSECTION=62, PATTERN=63, 
		SQL=64, METADATASQL=65, PREVIOUS=66, PREVIOUSTAIL=67, PREVIOUSCOUNT=68, 
		PREVIOUSWINDOW=69, PRIOR=70, EXISTS=71, WEEKDAY=72, LW=73, INSTANCEOF=74, 
		TYPEOF=75, CAST=76, CURRENT_TIMESTAMP=77, DELETE=78, SNAPSHOT=79, SET=80, 
		VARIABLE=81, TABLE=82, UNTIL=83, AT=84, INDEX=85, TIMEPERIOD_YEAR=86, 
		TIMEPERIOD_YEARS=87, TIMEPERIOD_MONTH=88, TIMEPERIOD_MONTHS=89, TIMEPERIOD_WEEK=90, 
		TIMEPERIOD_WEEKS=91, TIMEPERIOD_DAY=92, TIMEPERIOD_DAYS=93, TIMEPERIOD_HOUR=94, 
		TIMEPERIOD_HOURS=95, TIMEPERIOD_MINUTE=96, TIMEPERIOD_MINUTES=97, TIMEPERIOD_SEC=98, 
		TIMEPERIOD_SECOND=99, TIMEPERIOD_SECONDS=100, TIMEPERIOD_MILLISEC=101, 
		TIMEPERIOD_MILLISECOND=102, TIMEPERIOD_MILLISECONDS=103, TIMEPERIOD_MICROSEC=104, 
		TIMEPERIOD_MICROSECOND=105, TIMEPERIOD_MICROSECONDS=106, BOOLEAN_TRUE=107, 
		BOOLEAN_FALSE=108, VALUE_NULL=109, ROW_LIMIT_EXPR=110, OFFSET=111, UPDATE=112, 
		MATCH_RECOGNIZE=113, MATCH_RECOGNIZE_PERMUTE=114, MEASURES=115, DEFINE=116, 
		PARTITION=117, MATCHES=118, AFTER=119, FOR=120, WHILE=121, USING=122, 
		MERGE=123, MATCHED=124, EXPRESSIONDECL=125, NEWKW=126, START=127, CONTEXT=128, 
		INITIATED=129, TERMINATED=130, DATAFLOW=131, CUBE=132, ROLLUP=133, GROUPING=134, 
		GROUPING_ID=135, SETS=136, FOLLOWMAX_BEGIN=137, FOLLOWMAX_END=138, FOLLOWED_BY=139, 
		GOES=140, EQUALS=141, SQL_NE=142, QUESTION=143, LPAREN=144, RPAREN=145, 
		LBRACK=146, RBRACK=147, LCURLY=148, RCURLY=149, COLON=150, COMMA=151, 
		EQUAL=152, LNOT=153, BNOT=154, NOT_EQUAL=155, DIV=156, DIV_ASSIGN=157, 
		PLUS=158, PLUS_ASSIGN=159, INC=160, MINUS=161, MINUS_ASSIGN=162, DEC=163, 
		STAR=164, STAR_ASSIGN=165, MOD=166, MOD_ASSIGN=167, GE=168, GT=169, LE=170, 
		LT=171, BXOR=172, BXOR_ASSIGN=173, BOR=174, BOR_ASSIGN=175, LOR=176, BAND=177, 
		BAND_ASSIGN=178, LAND=179, SEMI=180, DOT=181, NUM_LONG=182, NUM_DOUBLE=183, 
		NUM_FLOAT=184, ESCAPECHAR=185, ESCAPEBACKTICK=186, ATCHAR=187, HASHCHAR=188, 
		WS=189, SL_COMMENT=190, ML_COMMENT=191, TICKED_STRING_LITERAL=192, QUOTED_STRING_LITERAL=193, 
		STRING_LITERAL=194, IDENT=195, IntegerLiteral=196, FloatingPointLiteral=197;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"CREATE", "WINDOW", "IN_SET", "BETWEEN", "LIKE", "REGEXP", "ESCAPE", "OR_EXPR", 
		"AND_EXPR", "NOT_EXPR", "EVERY_EXPR", "EVERY_DISTINCT_EXPR", "WHERE", 
		"AS", "SUM", "AVG", "MAX", "MIN", "COALESCE", "MEDIAN", "STDDEV", "AVEDEV", 
		"COUNT", "SELECT", "CASE", "ELSE", "WHEN", "THEN", "END", "FROM", "OUTER", 
		"INNER", "JOIN", "LEFT", "RIGHT", "FULL", "ON", "IS", "BY", "GROUP", "HAVING", 
		"DISTINCT", "ALL", "ANY", "SOME", "OUTPUT", "EVENTS", "FIRST", "LAST", 
		"INSERT", "INTO", "VALUES", "ORDER", "ASC", "DESC", "RSTREAM", "ISTREAM", 
		"IRSTREAM", "SCHEMA", "UNIDIRECTIONAL", "RETAINUNION", "RETAININTERSECTION", 
		"PATTERN", "SQL", "METADATASQL", "PREVIOUS", "PREVIOUSTAIL", "PREVIOUSCOUNT", 
		"PREVIOUSWINDOW", "PRIOR", "EXISTS", "WEEKDAY", "LW", "INSTANCEOF", "TYPEOF", 
		"CAST", "CURRENT_TIMESTAMP", "DELETE", "SNAPSHOT", "SET", "VARIABLE", 
		"TABLE", "UNTIL", "AT", "INDEX", "TIMEPERIOD_YEAR", "TIMEPERIOD_YEARS", 
		"TIMEPERIOD_MONTH", "TIMEPERIOD_MONTHS", "TIMEPERIOD_WEEK", "TIMEPERIOD_WEEKS", 
		"TIMEPERIOD_DAY", "TIMEPERIOD_DAYS", "TIMEPERIOD_HOUR", "TIMEPERIOD_HOURS", 
		"TIMEPERIOD_MINUTE", "TIMEPERIOD_MINUTES", "TIMEPERIOD_SEC", "TIMEPERIOD_SECOND", 
		"TIMEPERIOD_SECONDS", "TIMEPERIOD_MILLISEC", "TIMEPERIOD_MILLISECOND", 
		"TIMEPERIOD_MILLISECONDS", "TIMEPERIOD_MICROSEC", "TIMEPERIOD_MICROSECOND", 
		"TIMEPERIOD_MICROSECONDS", "BOOLEAN_TRUE", "BOOLEAN_FALSE", "VALUE_NULL", 
		"ROW_LIMIT_EXPR", "OFFSET", "UPDATE", "MATCH_RECOGNIZE", "MATCH_RECOGNIZE_PERMUTE", 
		"MEASURES", "DEFINE", "PARTITION", "MATCHES", "AFTER", "FOR", "WHILE", 
		"USING", "MERGE", "MATCHED", "EXPRESSIONDECL", "NEWKW", "START", "CONTEXT", 
		"INITIATED", "TERMINATED", "DATAFLOW", "CUBE", "ROLLUP", "GROUPING", "GROUPING_ID", 
		"SETS", "FOLLOWMAX_BEGIN", "FOLLOWMAX_END", "FOLLOWED_BY", "GOES", "EQUALS", 
		"SQL_NE", "QUESTION", "LPAREN", "RPAREN", "LBRACK", "RBRACK", "LCURLY", 
		"RCURLY", "COLON", "COMMA", "EQUAL", "LNOT", "BNOT", "NOT_EQUAL", "DIV", 
		"DIV_ASSIGN", "PLUS", "PLUS_ASSIGN", "INC", "MINUS", "MINUS_ASSIGN", "DEC", 
		"STAR", "STAR_ASSIGN", "MOD", "MOD_ASSIGN", "GE", "GT", "LE", "LT", "BXOR", 
		"BXOR_ASSIGN", "BOR", "BOR_ASSIGN", "LOR", "BAND", "BAND_ASSIGN", "LAND", 
		"SEMI", "DOT", "NUM_LONG", "NUM_DOUBLE", "NUM_FLOAT", "ESCAPECHAR", "ESCAPEBACKTICK", 
		"ATCHAR", "HASHCHAR", "WS", "SL_COMMENT", "ML_COMMENT", "TICKED_STRING_LITERAL", 
		"QUOTED_STRING_LITERAL", "STRING_LITERAL", "EscapeSequence", "IDENT", 
		"IntegerLiteral", "FloatingPointLiteral", "OctalEscape", "UnicodeEscape", 
		"DecimalIntegerLiteral", "HexIntegerLiteral", "OctalIntegerLiteral", "BinaryIntegerLiteral", 
		"IntegerTypeSuffix", "DecimalNumeral", "Digits", "Digit", "NonZeroDigit", 
		"DigitOrUnderscore", "Underscores", "HexNumeral", "HexDigits", "HexDigit", 
		"HexDigitOrUnderscore", "OctalNumeral", "OctalDigits", "OctalDigit", "OctalDigitOrUnderscore", 
		"BinaryNumeral", "BinaryDigits", "BinaryDigit", "BinaryDigitOrUnderscore", 
		"DecimalFloatingPointLiteral", "ExponentPart", "ExponentIndicator", "SignedInteger", 
		"Sign", "FloatTypeSuffix", "HexadecimalFloatingPointLiteral", "HexSignificand", 
		"BinaryExponent", "BinaryExponentIndicator"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'create'", "'window'", "'in'", "'between'", "'like'", "'regexp'", 
		"'escape'", "'or'", "'and'", "'not'", "'every'", "'every-distinct'", "'where'", 
		"'as'", "'sum'", "'avg'", "'max'", "'min'", "'coalesce'", "'median'", 
		"'stddev'", "'avedev'", "'count'", "'select'", "'case'", "'else'", "'when'", 
		"'then'", "'end'", "'from'", "'outer'", "'inner'", "'join'", "'left'", 
		"'right'", "'full'", "'on'", "'is'", "'by'", "'group'", "'having'", "'distinct'", 
		"'all'", "'any'", "'some'", "'output'", "'events'", "'first'", "'last'", 
		"'insert'", "'into'", "'values'", "'order'", "'asc'", "'desc'", "'rstream'", 
		"'istream'", "'irstream'", "'schema'", "'unidirectional'", "'retain-union'", 
		"'retain-intersection'", "'pattern'", "'sql'", "'metadatasql'", "'prev'", 
		"'prevtail'", "'prevcount'", "'prevwindow'", "'prior'", "'exists'", "'weekday'", 
		"'lastweekday'", "'instanceof'", "'typeof'", "'cast'", "'current_timestamp'", 
		"'delete'", "'snapshot'", "'set'", "'variable'", "'table'", "'until'", 
		"'at'", "'index'", "'year'", "'years'", "'month'", "'months'", "'week'", 
		"'weeks'", "'day'", "'days'", "'hour'", "'hours'", "'minute'", "'minutes'", 
		"'sec'", "'second'", "'seconds'", "'msec'", "'millisecond'", "'milliseconds'", 
		"'usec'", "'microsecond'", "'microseconds'", "'true'", "'false'", "'null'", 
		"'limit'", "'offset'", "'update'", "'match_recognize'", "'match_recognize_permute'", 
		"'measures'", "'define'", "'partition'", "'matches'", "'after'", "'for'", 
		"'while'", "'using'", "'merge'", "'matched'", "'expression'", "'new'", 
		"'start'", "'context'", "'initiated'", "'terminated'", "'dataflow'", "'cube'", 
		"'rollup'", "'grouping'", "'grouping_id'", "'sets'", "'-['", "']>'", "'->'", 
		"'=>'", "'='", "'<>'", "'?'", "'('", "')'", "'['", "']'", "'{'", "'}'", 
		"':'", "','", "'=='", "'!'", "'~'", "'!='", "'/'", "'/='", "'+'", "'+='", 
		"'++'", "'-'", "'-='", "'--'", "'*'", "'*='", "'%'", "'%='", "'>='", "'>'", 
		"'<='", "'<'", "'^'", "'^='", "'|'", "'|='", "'||'", "'&'", "'&='", "'&&'", 
		"';'", "'.'", "'\u18FF'", "'\u18FE'", "'\u18FD'", "'\\'", "'`'", "'@'", 
		"'#'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "CREATE", "WINDOW", "IN_SET", "BETWEEN", "LIKE", "REGEXP", "ESCAPE", 
		"OR_EXPR", "AND_EXPR", "NOT_EXPR", "EVERY_EXPR", "EVERY_DISTINCT_EXPR", 
		"WHERE", "AS", "SUM", "AVG", "MAX", "MIN", "COALESCE", "MEDIAN", "STDDEV", 
		"AVEDEV", "COUNT", "SELECT", "CASE", "ELSE", "WHEN", "THEN", "END", "FROM", 
		"OUTER", "INNER", "JOIN", "LEFT", "RIGHT", "FULL", "ON", "IS", "BY", "GROUP", 
		"HAVING", "DISTINCT", "ALL", "ANY", "SOME", "OUTPUT", "EVENTS", "FIRST", 
		"LAST", "INSERT", "INTO", "VALUES", "ORDER", "ASC", "DESC", "RSTREAM", 
		"ISTREAM", "IRSTREAM", "SCHEMA", "UNIDIRECTIONAL", "RETAINUNION", "RETAININTERSECTION", 
		"PATTERN", "SQL", "METADATASQL", "PREVIOUS", "PREVIOUSTAIL", "PREVIOUSCOUNT", 
		"PREVIOUSWINDOW", "PRIOR", "EXISTS", "WEEKDAY", "LW", "INSTANCEOF", "TYPEOF", 
		"CAST", "CURRENT_TIMESTAMP", "DELETE", "SNAPSHOT", "SET", "VARIABLE", 
		"TABLE", "UNTIL", "AT", "INDEX", "TIMEPERIOD_YEAR", "TIMEPERIOD_YEARS", 
		"TIMEPERIOD_MONTH", "TIMEPERIOD_MONTHS", "TIMEPERIOD_WEEK", "TIMEPERIOD_WEEKS", 
		"TIMEPERIOD_DAY", "TIMEPERIOD_DAYS", "TIMEPERIOD_HOUR", "TIMEPERIOD_HOURS", 
		"TIMEPERIOD_MINUTE", "TIMEPERIOD_MINUTES", "TIMEPERIOD_SEC", "TIMEPERIOD_SECOND", 
		"TIMEPERIOD_SECONDS", "TIMEPERIOD_MILLISEC", "TIMEPERIOD_MILLISECOND", 
		"TIMEPERIOD_MILLISECONDS", "TIMEPERIOD_MICROSEC", "TIMEPERIOD_MICROSECOND", 
		"TIMEPERIOD_MICROSECONDS", "BOOLEAN_TRUE", "BOOLEAN_FALSE", "VALUE_NULL", 
		"ROW_LIMIT_EXPR", "OFFSET", "UPDATE", "MATCH_RECOGNIZE", "MATCH_RECOGNIZE_PERMUTE", 
		"MEASURES", "DEFINE", "PARTITION", "MATCHES", "AFTER", "FOR", "WHILE", 
		"USING", "MERGE", "MATCHED", "EXPRESSIONDECL", "NEWKW", "START", "CONTEXT", 
		"INITIATED", "TERMINATED", "DATAFLOW", "CUBE", "ROLLUP", "GROUPING", "GROUPING_ID", 
		"SETS", "FOLLOWMAX_BEGIN", "FOLLOWMAX_END", "FOLLOWED_BY", "GOES", "EQUALS", 
		"SQL_NE", "QUESTION", "LPAREN", "RPAREN", "LBRACK", "RBRACK", "LCURLY", 
		"RCURLY", "COLON", "COMMA", "EQUAL", "LNOT", "BNOT", "NOT_EQUAL", "DIV", 
		"DIV_ASSIGN", "PLUS", "PLUS_ASSIGN", "INC", "MINUS", "MINUS_ASSIGN", "DEC", 
		"STAR", "STAR_ASSIGN", "MOD", "MOD_ASSIGN", "GE", "GT", "LE", "LT", "BXOR", 
		"BXOR_ASSIGN", "BOR", "BOR_ASSIGN", "LOR", "BAND", "BAND_ASSIGN", "LAND", 
		"SEMI", "DOT", "NUM_LONG", "NUM_DOUBLE", "NUM_FLOAT", "ESCAPECHAR", "ESCAPEBACKTICK", 
		"ATCHAR", "HASHCHAR", "WS", "SL_COMMENT", "ML_COMMENT", "TICKED_STRING_LITERAL", 
		"QUOTED_STRING_LITERAL", "STRING_LITERAL", "IDENT", "IntegerLiteral", 
		"FloatingPointLiteral"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}


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


	public EsperEPL2GrammarLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "EsperEPL2Grammar.g"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\u00c7\u0742\b\1\4"+
		"\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n"+
		"\4\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t"+
		" \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t"+
		"+\4,\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64"+
		"\t\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\4=\t"+
		"=\4>\t>\4?\t?\4@\t@\4A\tA\4B\tB\4C\tC\4D\tD\4E\tE\4F\tF\4G\tG\4H\tH\4"+
		"I\tI\4J\tJ\4K\tK\4L\tL\4M\tM\4N\tN\4O\tO\4P\tP\4Q\tQ\4R\tR\4S\tS\4T\t"+
		"T\4U\tU\4V\tV\4W\tW\4X\tX\4Y\tY\4Z\tZ\4[\t[\4\\\t\\\4]\t]\4^\t^\4_\t_"+
		"\4`\t`\4a\ta\4b\tb\4c\tc\4d\td\4e\te\4f\tf\4g\tg\4h\th\4i\ti\4j\tj\4k"+
		"\tk\4l\tl\4m\tm\4n\tn\4o\to\4p\tp\4q\tq\4r\tr\4s\ts\4t\tt\4u\tu\4v\tv"+
		"\4w\tw\4x\tx\4y\ty\4z\tz\4{\t{\4|\t|\4}\t}\4~\t~\4\177\t\177\4\u0080\t"+
		"\u0080\4\u0081\t\u0081\4\u0082\t\u0082\4\u0083\t\u0083\4\u0084\t\u0084"+
		"\4\u0085\t\u0085\4\u0086\t\u0086\4\u0087\t\u0087\4\u0088\t\u0088\4\u0089"+
		"\t\u0089\4\u008a\t\u008a\4\u008b\t\u008b\4\u008c\t\u008c\4\u008d\t\u008d"+
		"\4\u008e\t\u008e\4\u008f\t\u008f\4\u0090\t\u0090\4\u0091\t\u0091\4\u0092"+
		"\t\u0092\4\u0093\t\u0093\4\u0094\t\u0094\4\u0095\t\u0095\4\u0096\t\u0096"+
		"\4\u0097\t\u0097\4\u0098\t\u0098\4\u0099\t\u0099\4\u009a\t\u009a\4\u009b"+
		"\t\u009b\4\u009c\t\u009c\4\u009d\t\u009d\4\u009e\t\u009e\4\u009f\t\u009f"+
		"\4\u00a0\t\u00a0\4\u00a1\t\u00a1\4\u00a2\t\u00a2\4\u00a3\t\u00a3\4\u00a4"+
		"\t\u00a4\4\u00a5\t\u00a5\4\u00a6\t\u00a6\4\u00a7\t\u00a7\4\u00a8\t\u00a8"+
		"\4\u00a9\t\u00a9\4\u00aa\t\u00aa\4\u00ab\t\u00ab\4\u00ac\t\u00ac\4\u00ad"+
		"\t\u00ad\4\u00ae\t\u00ae\4\u00af\t\u00af\4\u00b0\t\u00b0\4\u00b1\t\u00b1"+
		"\4\u00b2\t\u00b2\4\u00b3\t\u00b3\4\u00b4\t\u00b4\4\u00b5\t\u00b5\4\u00b6"+
		"\t\u00b6\4\u00b7\t\u00b7\4\u00b8\t\u00b8\4\u00b9\t\u00b9\4\u00ba\t\u00ba"+
		"\4\u00bb\t\u00bb\4\u00bc\t\u00bc\4\u00bd\t\u00bd\4\u00be\t\u00be\4\u00bf"+
		"\t\u00bf\4\u00c0\t\u00c0\4\u00c1\t\u00c1\4\u00c2\t\u00c2\4\u00c3\t\u00c3"+
		"\4\u00c4\t\u00c4\4\u00c5\t\u00c5\4\u00c6\t\u00c6\4\u00c7\t\u00c7\4\u00c8"+
		"\t\u00c8\4\u00c9\t\u00c9\4\u00ca\t\u00ca\4\u00cb\t\u00cb\4\u00cc\t\u00cc"+
		"\4\u00cd\t\u00cd\4\u00ce\t\u00ce\4\u00cf\t\u00cf\4\u00d0\t\u00d0\4\u00d1"+
		"\t\u00d1\4\u00d2\t\u00d2\4\u00d3\t\u00d3\4\u00d4\t\u00d4\4\u00d5\t\u00d5"+
		"\4\u00d6\t\u00d6\4\u00d7\t\u00d7\4\u00d8\t\u00d8\4\u00d9\t\u00d9\4\u00da"+
		"\t\u00da\4\u00db\t\u00db\4\u00dc\t\u00dc\4\u00dd\t\u00dd\4\u00de\t\u00de"+
		"\4\u00df\t\u00df\4\u00e0\t\u00e0\4\u00e1\t\u00e1\4\u00e2\t\u00e2\4\u00e3"+
		"\t\u00e3\4\u00e4\t\u00e4\4\u00e5\t\u00e5\4\u00e6\t\u00e6\4\u00e7\t\u00e7"+
		"\4\u00e8\t\u00e8\4\u00e9\t\u00e9\4\u00ea\t\u00ea\3\2\3\2\3\2\3\2\3\2\3"+
		"\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\4\3\4\3\4\3\5\3\5\3\5\3\5\3\5\3\5"+
		"\3\5\3\5\3\6\3\6\3\6\3\6\3\6\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\b\3\b\3\b\3"+
		"\b\3\b\3\b\3\b\3\t\3\t\3\t\3\n\3\n\3\n\3\n\3\13\3\13\3\13\3\13\3\f\3\f"+
		"\3\f\3\f\3\f\3\f\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3"+
		"\r\3\r\3\16\3\16\3\16\3\16\3\16\3\16\3\17\3\17\3\17\3\20\3\20\3\20\3\20"+
		"\3\21\3\21\3\21\3\21\3\22\3\22\3\22\3\22\3\23\3\23\3\23\3\23\3\24\3\24"+
		"\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\25\3\25\3\25\3\25\3\25\3\25\3\25"+
		"\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\27\3\27\3\27\3\27\3\27\3\27\3\27"+
		"\3\30\3\30\3\30\3\30\3\30\3\30\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\32"+
		"\3\32\3\32\3\32\3\32\3\33\3\33\3\33\3\33\3\33\3\34\3\34\3\34\3\34\3\34"+
		"\3\35\3\35\3\35\3\35\3\35\3\36\3\36\3\36\3\36\3\37\3\37\3\37\3\37\3\37"+
		"\3 \3 \3 \3 \3 \3 \3!\3!\3!\3!\3!\3!\3\"\3\"\3\"\3\"\3\"\3#\3#\3#\3#\3"+
		"#\3$\3$\3$\3$\3$\3$\3%\3%\3%\3%\3%\3&\3&\3&\3\'\3\'\3\'\3(\3(\3(\3)\3"+
		")\3)\3)\3)\3)\3*\3*\3*\3*\3*\3*\3*\3+\3+\3+\3+\3+\3+\3+\3+\3+\3,\3,\3"+
		",\3,\3-\3-\3-\3-\3.\3.\3.\3.\3.\3/\3/\3/\3/\3/\3/\3/\3\60\3\60\3\60\3"+
		"\60\3\60\3\60\3\60\3\61\3\61\3\61\3\61\3\61\3\61\3\62\3\62\3\62\3\62\3"+
		"\62\3\63\3\63\3\63\3\63\3\63\3\63\3\63\3\64\3\64\3\64\3\64\3\64\3\65\3"+
		"\65\3\65\3\65\3\65\3\65\3\65\3\66\3\66\3\66\3\66\3\66\3\66\3\67\3\67\3"+
		"\67\3\67\38\38\38\38\38\39\39\39\39\39\39\39\39\3:\3:\3:\3:\3:\3:\3:\3"+
		":\3;\3;\3;\3;\3;\3;\3;\3;\3;\3<\3<\3<\3<\3<\3<\3<\3=\3=\3=\3=\3=\3=\3"+
		"=\3=\3=\3=\3=\3=\3=\3=\3=\3>\3>\3>\3>\3>\3>\3>\3>\3>\3>\3>\3>\3>\3?\3"+
		"?\3?\3?\3?\3?\3?\3?\3?\3?\3?\3?\3?\3?\3?\3?\3?\3?\3?\3?\3@\3@\3@\3@\3"+
		"@\3@\3@\3@\3A\3A\3A\3A\3B\3B\3B\3B\3B\3B\3B\3B\3B\3B\3B\3B\3C\3C\3C\3"+
		"C\3C\3D\3D\3D\3D\3D\3D\3D\3D\3D\3E\3E\3E\3E\3E\3E\3E\3E\3E\3E\3F\3F\3"+
		"F\3F\3F\3F\3F\3F\3F\3F\3F\3G\3G\3G\3G\3G\3G\3H\3H\3H\3H\3H\3H\3H\3I\3"+
		"I\3I\3I\3I\3I\3I\3I\3J\3J\3J\3J\3J\3J\3J\3J\3J\3J\3J\3J\3K\3K\3K\3K\3"+
		"K\3K\3K\3K\3K\3K\3K\3L\3L\3L\3L\3L\3L\3L\3M\3M\3M\3M\3M\3N\3N\3N\3N\3"+
		"N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3O\3O\3O\3O\3O\3O\3O\3P\3P\3"+
		"P\3P\3P\3P\3P\3P\3P\3Q\3Q\3Q\3Q\3R\3R\3R\3R\3R\3R\3R\3R\3R\3S\3S\3S\3"+
		"S\3S\3S\3T\3T\3T\3T\3T\3T\3U\3U\3U\3V\3V\3V\3V\3V\3V\3W\3W\3W\3W\3W\3"+
		"X\3X\3X\3X\3X\3X\3Y\3Y\3Y\3Y\3Y\3Y\3Z\3Z\3Z\3Z\3Z\3Z\3Z\3[\3[\3[\3[\3"+
		"[\3\\\3\\\3\\\3\\\3\\\3\\\3]\3]\3]\3]\3^\3^\3^\3^\3^\3_\3_\3_\3_\3_\3"+
		"`\3`\3`\3`\3`\3`\3a\3a\3a\3a\3a\3a\3a\3b\3b\3b\3b\3b\3b\3b\3b\3c\3c\3"+
		"c\3c\3d\3d\3d\3d\3d\3d\3d\3e\3e\3e\3e\3e\3e\3e\3e\3f\3f\3f\3f\3f\3g\3"+
		"g\3g\3g\3g\3g\3g\3g\3g\3g\3g\3g\3h\3h\3h\3h\3h\3h\3h\3h\3h\3h\3h\3h\3"+
		"h\3i\3i\3i\3i\3i\3j\3j\3j\3j\3j\3j\3j\3j\3j\3j\3j\3j\3k\3k\3k\3k\3k\3"+
		"k\3k\3k\3k\3k\3k\3k\3k\3l\3l\3l\3l\3l\3m\3m\3m\3m\3m\3m\3n\3n\3n\3n\3"+
		"n\3o\3o\3o\3o\3o\3o\3p\3p\3p\3p\3p\3p\3p\3q\3q\3q\3q\3q\3q\3q\3r\3r\3"+
		"r\3r\3r\3r\3r\3r\3r\3r\3r\3r\3r\3r\3r\3r\3s\3s\3s\3s\3s\3s\3s\3s\3s\3"+
		"s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3t\3t\3t\3t\3t\3t\3t\3t\3"+
		"t\3u\3u\3u\3u\3u\3u\3u\3v\3v\3v\3v\3v\3v\3v\3v\3v\3v\3w\3w\3w\3w\3w\3"+
		"w\3w\3w\3x\3x\3x\3x\3x\3x\3y\3y\3y\3y\3z\3z\3z\3z\3z\3z\3{\3{\3{\3{\3"+
		"{\3{\3|\3|\3|\3|\3|\3|\3}\3}\3}\3}\3}\3}\3}\3}\3~\3~\3~\3~\3~\3~\3~\3"+
		"~\3~\3~\3~\3\177\3\177\3\177\3\177\3\u0080\3\u0080\3\u0080\3\u0080\3\u0080"+
		"\3\u0080\3\u0081\3\u0081\3\u0081\3\u0081\3\u0081\3\u0081\3\u0081\3\u0081"+
		"\3\u0082\3\u0082\3\u0082\3\u0082\3\u0082\3\u0082\3\u0082\3\u0082\3\u0082"+
		"\3\u0082\3\u0083\3\u0083\3\u0083\3\u0083\3\u0083\3\u0083\3\u0083\3\u0083"+
		"\3\u0083\3\u0083\3\u0083\3\u0084\3\u0084\3\u0084\3\u0084\3\u0084\3\u0084"+
		"\3\u0084\3\u0084\3\u0084\3\u0085\3\u0085\3\u0085\3\u0085\3\u0085\3\u0086"+
		"\3\u0086\3\u0086\3\u0086\3\u0086\3\u0086\3\u0086\3\u0087\3\u0087\3\u0087"+
		"\3\u0087\3\u0087\3\u0087\3\u0087\3\u0087\3\u0087\3\u0088\3\u0088\3\u0088"+
		"\3\u0088\3\u0088\3\u0088\3\u0088\3\u0088\3\u0088\3\u0088\3\u0088\3\u0088"+
		"\3\u0089\3\u0089\3\u0089\3\u0089\3\u0089\3\u008a\3\u008a\3\u008a\3\u008b"+
		"\3\u008b\3\u008b\3\u008c\3\u008c\3\u008c\3\u008d\3\u008d\3\u008d\3\u008e"+
		"\3\u008e\3\u008f\3\u008f\3\u008f\3\u0090\3\u0090\3\u0091\3\u0091\3\u0092"+
		"\3\u0092\3\u0093\3\u0093\3\u0094\3\u0094\3\u0095\3\u0095\3\u0096\3\u0096"+
		"\3\u0097\3\u0097\3\u0098\3\u0098\3\u0099\3\u0099\3\u0099\3\u009a\3\u009a"+
		"\3\u009b\3\u009b\3\u009c\3\u009c\3\u009c\3\u009d\3\u009d\3\u009e\3\u009e"+
		"\3\u009e\3\u009f\3\u009f\3\u00a0\3\u00a0\3\u00a0\3\u00a1\3\u00a1\3\u00a1"+
		"\3\u00a2\3\u00a2\3\u00a3\3\u00a3\3\u00a3\3\u00a4\3\u00a4\3\u00a4\3\u00a5"+
		"\3\u00a5\3\u00a6\3\u00a6\3\u00a6\3\u00a7\3\u00a7\3\u00a8\3\u00a8\3\u00a8"+
		"\3\u00a9\3\u00a9\3\u00a9\3\u00aa\3\u00aa\3\u00ab\3\u00ab\3\u00ab\3\u00ac"+
		"\3\u00ac\3\u00ad\3\u00ad\3\u00ae\3\u00ae\3\u00ae\3\u00af\3\u00af\3\u00b0"+
		"\3\u00b0\3\u00b0\3\u00b1\3\u00b1\3\u00b1\3\u00b2\3\u00b2\3\u00b3\3\u00b3"+
		"\3\u00b3\3\u00b4\3\u00b4\3\u00b4\3\u00b5\3\u00b5\3\u00b6\3\u00b6\3\u00b7"+
		"\3\u00b7\3\u00b8\3\u00b8\3\u00b9\3\u00b9\3\u00ba\3\u00ba\3\u00bb\3\u00bb"+
		"\3\u00bc\3\u00bc\3\u00bd\3\u00bd\3\u00be\6\u00be\u0618\n\u00be\r\u00be"+
		"\16\u00be\u0619\3\u00be\3\u00be\3\u00bf\3\u00bf\3\u00bf\3\u00bf\7\u00bf"+
		"\u0622\n\u00bf\f\u00bf\16\u00bf\u0625\13\u00bf\3\u00bf\3\u00bf\3\u00bf"+
		"\5\u00bf\u062a\n\u00bf\5\u00bf\u062c\n\u00bf\3\u00bf\3\u00bf\3\u00c0\3"+
		"\u00c0\3\u00c0\3\u00c0\7\u00c0\u0634\n\u00c0\f\u00c0\16\u00c0\u0637\13"+
		"\u00c0\3\u00c0\3\u00c0\3\u00c0\3\u00c0\3\u00c0\3\u00c1\3\u00c1\3\u00c1"+
		"\7\u00c1\u0641\n\u00c1\f\u00c1\16\u00c1\u0644\13\u00c1\3\u00c1\3\u00c1"+
		"\3\u00c2\3\u00c2\3\u00c2\7\u00c2\u064b\n\u00c2\f\u00c2\16\u00c2\u064e"+
		"\13\u00c2\3\u00c2\3\u00c2\3\u00c3\3\u00c3\3\u00c3\7\u00c3\u0655\n\u00c3"+
		"\f\u00c3\16\u00c3\u0658\13\u00c3\3\u00c3\3\u00c3\3\u00c4\3\u00c4\3\u00c4"+
		"\3\u00c4\3\u00c4\5\u00c4\u0661\n\u00c4\3\u00c5\3\u00c5\7\u00c5\u0665\n"+
		"\u00c5\f\u00c5\16\u00c5\u0668\13\u00c5\3\u00c6\3\u00c6\3\u00c6\3\u00c6"+
		"\5\u00c6\u066e\n\u00c6\3\u00c7\3\u00c7\5\u00c7\u0672\n\u00c7\3\u00c8\3"+
		"\u00c8\3\u00c8\3\u00c8\3\u00c8\3\u00c8\3\u00c8\3\u00c8\3\u00c8\5\u00c8"+
		"\u067d\n\u00c8\3\u00c9\3\u00c9\3\u00c9\3\u00c9\3\u00c9\3\u00c9\3\u00c9"+
		"\3\u00ca\3\u00ca\5\u00ca\u0688\n\u00ca\3\u00cb\3\u00cb\5\u00cb\u068c\n"+
		"\u00cb\3\u00cc\3\u00cc\5\u00cc\u0690\n\u00cc\3\u00cd\3\u00cd\5\u00cd\u0694"+
		"\n\u00cd\3\u00ce\3\u00ce\3\u00cf\3\u00cf\7\u00cf\u069a\n\u00cf\f\u00cf"+
		"\16\u00cf\u069d\13\u00cf\3\u00cf\3\u00cf\5\u00cf\u06a1\n\u00cf\3\u00cf"+
		"\3\u00cf\3\u00cf\5\u00cf\u06a6\n\u00cf\5\u00cf\u06a8\n\u00cf\3\u00d0\3"+
		"\u00d0\7\u00d0\u06ac\n\u00d0\f\u00d0\16\u00d0\u06af\13\u00d0\3\u00d0\5"+
		"\u00d0\u06b2\n\u00d0\3\u00d1\3\u00d1\5\u00d1\u06b6\n\u00d1\3\u00d2\3\u00d2"+
		"\3\u00d3\3\u00d3\5\u00d3\u06bc\n\u00d3\3\u00d4\6\u00d4\u06bf\n\u00d4\r"+
		"\u00d4\16\u00d4\u06c0\3\u00d5\3\u00d5\3\u00d5\3\u00d5\3\u00d6\3\u00d6"+
		"\7\u00d6\u06c9\n\u00d6\f\u00d6\16\u00d6\u06cc\13\u00d6\3\u00d6\5\u00d6"+
		"\u06cf\n\u00d6\3\u00d7\3\u00d7\3\u00d8\3\u00d8\5\u00d8\u06d5\n\u00d8\3"+
		"\u00d9\3\u00d9\5\u00d9\u06d9\n\u00d9\3\u00d9\3\u00d9\3\u00da\3\u00da\7"+
		"\u00da\u06df\n\u00da\f\u00da\16\u00da\u06e2\13\u00da\3\u00da\5\u00da\u06e5"+
		"\n\u00da\3\u00db\3\u00db\3\u00dc\3\u00dc\5\u00dc\u06eb\n\u00dc\3\u00dd"+
		"\3\u00dd\3\u00dd\3\u00dd\3\u00de\3\u00de\7\u00de\u06f3\n\u00de\f\u00de"+
		"\16\u00de\u06f6\13\u00de\3\u00de\5\u00de\u06f9\n\u00de\3\u00df\3\u00df"+
		"\3\u00e0\3\u00e0\5\u00e0\u06ff\n\u00e0\3\u00e1\3\u00e1\3\u00e1\5\u00e1"+
		"\u0704\n\u00e1\3\u00e1\5\u00e1\u0707\n\u00e1\3\u00e1\5\u00e1\u070a\n\u00e1"+
		"\3\u00e1\3\u00e1\3\u00e1\5\u00e1\u070f\n\u00e1\3\u00e1\5\u00e1\u0712\n"+
		"\u00e1\3\u00e1\3\u00e1\3\u00e1\5\u00e1\u0717\n\u00e1\3\u00e1\3\u00e1\3"+
		"\u00e1\5\u00e1\u071c\n\u00e1\3\u00e2\3\u00e2\3\u00e2\3\u00e3\3\u00e3\3"+
		"\u00e4\5\u00e4\u0724\n\u00e4\3\u00e4\3\u00e4\3\u00e5\3\u00e5\3\u00e6\3"+
		"\u00e6\3\u00e7\3\u00e7\3\u00e7\5\u00e7\u072f\n\u00e7\3\u00e8\3\u00e8\5"+
		"\u00e8\u0733\n\u00e8\3\u00e8\3\u00e8\3\u00e8\5\u00e8\u0738\n\u00e8\3\u00e8"+
		"\3\u00e8\5\u00e8\u073c\n\u00e8\3\u00e9\3\u00e9\3\u00e9\3\u00ea\3\u00ea"+
		"\3\u0635\2\u00eb\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31"+
		"\16\33\17\35\20\37\21!\22#\23%\24\'\25)\26+\27-\30/\31\61\32\63\33\65"+
		"\34\67\359\36;\37= ?!A\"C#E$G%I&K\'M(O)Q*S+U,W-Y.[/]\60_\61a\62c\63e\64"+
		"g\65i\66k\67m8o9q:s;u<w=y>{?}@\177A\u0081B\u0083C\u0085D\u0087E\u0089"+
		"F\u008bG\u008dH\u008fI\u0091J\u0093K\u0095L\u0097M\u0099N\u009bO\u009d"+
		"P\u009fQ\u00a1R\u00a3S\u00a5T\u00a7U\u00a9V\u00abW\u00adX\u00afY\u00b1"+
		"Z\u00b3[\u00b5\\\u00b7]\u00b9^\u00bb_\u00bd`\u00bfa\u00c1b\u00c3c\u00c5"+
		"d\u00c7e\u00c9f\u00cbg\u00cdh\u00cfi\u00d1j\u00d3k\u00d5l\u00d7m\u00d9"+
		"n\u00dbo\u00ddp\u00dfq\u00e1r\u00e3s\u00e5t\u00e7u\u00e9v\u00ebw\u00ed"+
		"x\u00efy\u00f1z\u00f3{\u00f5|\u00f7}\u00f9~\u00fb\177\u00fd\u0080\u00ff"+
		"\u0081\u0101\u0082\u0103\u0083\u0105\u0084\u0107\u0085\u0109\u0086\u010b"+
		"\u0087\u010d\u0088\u010f\u0089\u0111\u008a\u0113\u008b\u0115\u008c\u0117"+
		"\u008d\u0119\u008e\u011b\u008f\u011d\u0090\u011f\u0091\u0121\u0092\u0123"+
		"\u0093\u0125\u0094\u0127\u0095\u0129\u0096\u012b\u0097\u012d\u0098\u012f"+
		"\u0099\u0131\u009a\u0133\u009b\u0135\u009c\u0137\u009d\u0139\u009e\u013b"+
		"\u009f\u013d\u00a0\u013f\u00a1\u0141\u00a2\u0143\u00a3\u0145\u00a4\u0147"+
		"\u00a5\u0149\u00a6\u014b\u00a7\u014d\u00a8\u014f\u00a9\u0151\u00aa\u0153"+
		"\u00ab\u0155\u00ac\u0157\u00ad\u0159\u00ae\u015b\u00af\u015d\u00b0\u015f"+
		"\u00b1\u0161\u00b2\u0163\u00b3\u0165\u00b4\u0167\u00b5\u0169\u00b6\u016b"+
		"\u00b7\u016d\u00b8\u016f\u00b9\u0171\u00ba\u0173\u00bb\u0175\u00bc\u0177"+
		"\u00bd\u0179\u00be\u017b\u00bf\u017d\u00c0\u017f\u00c1\u0181\u00c2\u0183"+
		"\u00c3\u0185\u00c4\u0187\2\u0189\u00c5\u018b\u00c6\u018d\u00c7\u018f\2"+
		"\u0191\2\u0193\2\u0195\2\u0197\2\u0199\2\u019b\2\u019d\2\u019f\2\u01a1"+
		"\2\u01a3\2\u01a5\2\u01a7\2\u01a9\2\u01ab\2\u01ad\2\u01af\2\u01b1\2\u01b3"+
		"\2\u01b5\2\u01b7\2\u01b9\2\u01bb\2\u01bd\2\u01bf\2\u01c1\2\u01c3\2\u01c5"+
		"\2\u01c7\2\u01c9\2\u01cb\2\u01cd\2\u01cf\2\u01d1\2\u01d3\2\3\2\25\5\2"+
		"\13\f\16\17\"\"\4\2\f\f\17\17\4\2^^bb\4\2))^^\4\2$$^^\n\2$$))^^ddhhpp"+
		"ttvv\5\2&&aac|\6\2&&\62;aac|\4\2NNnn\3\2\63;\4\2ZZzz\5\2\62;CHch\3\2\62"+
		"9\4\2DDdd\3\2\62\63\4\2GGgg\4\2--//\6\2FFHHffhh\4\2RRrr\2\u0758\2\3\3"+
		"\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2"+
		"\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3"+
		"\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2"+
		"%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61"+
		"\3\2\2\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3\2\2\2"+
		"\2=\3\2\2\2\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2\2\2G\3\2\2\2\2I"+
		"\3\2\2\2\2K\3\2\2\2\2M\3\2\2\2\2O\3\2\2\2\2Q\3\2\2\2\2S\3\2\2\2\2U\3\2"+
		"\2\2\2W\3\2\2\2\2Y\3\2\2\2\2[\3\2\2\2\2]\3\2\2\2\2_\3\2\2\2\2a\3\2\2\2"+
		"\2c\3\2\2\2\2e\3\2\2\2\2g\3\2\2\2\2i\3\2\2\2\2k\3\2\2\2\2m\3\2\2\2\2o"+
		"\3\2\2\2\2q\3\2\2\2\2s\3\2\2\2\2u\3\2\2\2\2w\3\2\2\2\2y\3\2\2\2\2{\3\2"+
		"\2\2\2}\3\2\2\2\2\177\3\2\2\2\2\u0081\3\2\2\2\2\u0083\3\2\2\2\2\u0085"+
		"\3\2\2\2\2\u0087\3\2\2\2\2\u0089\3\2\2\2\2\u008b\3\2\2\2\2\u008d\3\2\2"+
		"\2\2\u008f\3\2\2\2\2\u0091\3\2\2\2\2\u0093\3\2\2\2\2\u0095\3\2\2\2\2\u0097"+
		"\3\2\2\2\2\u0099\3\2\2\2\2\u009b\3\2\2\2\2\u009d\3\2\2\2\2\u009f\3\2\2"+
		"\2\2\u00a1\3\2\2\2\2\u00a3\3\2\2\2\2\u00a5\3\2\2\2\2\u00a7\3\2\2\2\2\u00a9"+
		"\3\2\2\2\2\u00ab\3\2\2\2\2\u00ad\3\2\2\2\2\u00af\3\2\2\2\2\u00b1\3\2\2"+
		"\2\2\u00b3\3\2\2\2\2\u00b5\3\2\2\2\2\u00b7\3\2\2\2\2\u00b9\3\2\2\2\2\u00bb"+
		"\3\2\2\2\2\u00bd\3\2\2\2\2\u00bf\3\2\2\2\2\u00c1\3\2\2\2\2\u00c3\3\2\2"+
		"\2\2\u00c5\3\2\2\2\2\u00c7\3\2\2\2\2\u00c9\3\2\2\2\2\u00cb\3\2\2\2\2\u00cd"+
		"\3\2\2\2\2\u00cf\3\2\2\2\2\u00d1\3\2\2\2\2\u00d3\3\2\2\2\2\u00d5\3\2\2"+
		"\2\2\u00d7\3\2\2\2\2\u00d9\3\2\2\2\2\u00db\3\2\2\2\2\u00dd\3\2\2\2\2\u00df"+
		"\3\2\2\2\2\u00e1\3\2\2\2\2\u00e3\3\2\2\2\2\u00e5\3\2\2\2\2\u00e7\3\2\2"+
		"\2\2\u00e9\3\2\2\2\2\u00eb\3\2\2\2\2\u00ed\3\2\2\2\2\u00ef\3\2\2\2\2\u00f1"+
		"\3\2\2\2\2\u00f3\3\2\2\2\2\u00f5\3\2\2\2\2\u00f7\3\2\2\2\2\u00f9\3\2\2"+
		"\2\2\u00fb\3\2\2\2\2\u00fd\3\2\2\2\2\u00ff\3\2\2\2\2\u0101\3\2\2\2\2\u0103"+
		"\3\2\2\2\2\u0105\3\2\2\2\2\u0107\3\2\2\2\2\u0109\3\2\2\2\2\u010b\3\2\2"+
		"\2\2\u010d\3\2\2\2\2\u010f\3\2\2\2\2\u0111\3\2\2\2\2\u0113\3\2\2\2\2\u0115"+
		"\3\2\2\2\2\u0117\3\2\2\2\2\u0119\3\2\2\2\2\u011b\3\2\2\2\2\u011d\3\2\2"+
		"\2\2\u011f\3\2\2\2\2\u0121\3\2\2\2\2\u0123\3\2\2\2\2\u0125\3\2\2\2\2\u0127"+
		"\3\2\2\2\2\u0129\3\2\2\2\2\u012b\3\2\2\2\2\u012d\3\2\2\2\2\u012f\3\2\2"+
		"\2\2\u0131\3\2\2\2\2\u0133\3\2\2\2\2\u0135\3\2\2\2\2\u0137\3\2\2\2\2\u0139"+
		"\3\2\2\2\2\u013b\3\2\2\2\2\u013d\3\2\2\2\2\u013f\3\2\2\2\2\u0141\3\2\2"+
		"\2\2\u0143\3\2\2\2\2\u0145\3\2\2\2\2\u0147\3\2\2\2\2\u0149\3\2\2\2\2\u014b"+
		"\3\2\2\2\2\u014d\3\2\2\2\2\u014f\3\2\2\2\2\u0151\3\2\2\2\2\u0153\3\2\2"+
		"\2\2\u0155\3\2\2\2\2\u0157\3\2\2\2\2\u0159\3\2\2\2\2\u015b\3\2\2\2\2\u015d"+
		"\3\2\2\2\2\u015f\3\2\2\2\2\u0161\3\2\2\2\2\u0163\3\2\2\2\2\u0165\3\2\2"+
		"\2\2\u0167\3\2\2\2\2\u0169\3\2\2\2\2\u016b\3\2\2\2\2\u016d\3\2\2\2\2\u016f"+
		"\3\2\2\2\2\u0171\3\2\2\2\2\u0173\3\2\2\2\2\u0175\3\2\2\2\2\u0177\3\2\2"+
		"\2\2\u0179\3\2\2\2\2\u017b\3\2\2\2\2\u017d\3\2\2\2\2\u017f\3\2\2\2\2\u0181"+
		"\3\2\2\2\2\u0183\3\2\2\2\2\u0185\3\2\2\2\2\u0189\3\2\2\2\2\u018b\3\2\2"+
		"\2\2\u018d\3\2\2\2\3\u01d5\3\2\2\2\5\u01dc\3\2\2\2\7\u01e3\3\2\2\2\t\u01e6"+
		"\3\2\2\2\13\u01ee\3\2\2\2\r\u01f3\3\2\2\2\17\u01fa\3\2\2\2\21\u0201\3"+
		"\2\2\2\23\u0204\3\2\2\2\25\u0208\3\2\2\2\27\u020c\3\2\2\2\31\u0212\3\2"+
		"\2\2\33\u0221\3\2\2\2\35\u0227\3\2\2\2\37\u022a\3\2\2\2!\u022e\3\2\2\2"+
		"#\u0232\3\2\2\2%\u0236\3\2\2\2\'\u023a\3\2\2\2)\u0243\3\2\2\2+\u024a\3"+
		"\2\2\2-\u0251\3\2\2\2/\u0258\3\2\2\2\61\u025e\3\2\2\2\63\u0265\3\2\2\2"+
		"\65\u026a\3\2\2\2\67\u026f\3\2\2\29\u0274\3\2\2\2;\u0279\3\2\2\2=\u027d"+
		"\3\2\2\2?\u0282\3\2\2\2A\u0288\3\2\2\2C\u028e\3\2\2\2E\u0293\3\2\2\2G"+
		"\u0298\3\2\2\2I\u029e\3\2\2\2K\u02a3\3\2\2\2M\u02a6\3\2\2\2O\u02a9\3\2"+
		"\2\2Q\u02ac\3\2\2\2S\u02b2\3\2\2\2U\u02b9\3\2\2\2W\u02c2\3\2\2\2Y\u02c6"+
		"\3\2\2\2[\u02ca\3\2\2\2]\u02cf\3\2\2\2_\u02d6\3\2\2\2a\u02dd\3\2\2\2c"+
		"\u02e3\3\2\2\2e\u02e8\3\2\2\2g\u02ef\3\2\2\2i\u02f4\3\2\2\2k\u02fb\3\2"+
		"\2\2m\u0301\3\2\2\2o\u0305\3\2\2\2q\u030a\3\2\2\2s\u0312\3\2\2\2u\u031a"+
		"\3\2\2\2w\u0323\3\2\2\2y\u032a\3\2\2\2{\u0339\3\2\2\2}\u0346\3\2\2\2\177"+
		"\u035a\3\2\2\2\u0081\u0362\3\2\2\2\u0083\u0366\3\2\2\2\u0085\u0372\3\2"+
		"\2\2\u0087\u0377\3\2\2\2\u0089\u0380\3\2\2\2\u008b\u038a\3\2\2\2\u008d"+
		"\u0395\3\2\2\2\u008f\u039b\3\2\2\2\u0091\u03a2\3\2\2\2\u0093\u03aa\3\2"+
		"\2\2\u0095\u03b6\3\2\2\2\u0097\u03c1\3\2\2\2\u0099\u03c8\3\2\2\2\u009b"+
		"\u03cd\3\2\2\2\u009d\u03df\3\2\2\2\u009f\u03e6\3\2\2\2\u00a1\u03ef\3\2"+
		"\2\2\u00a3\u03f3\3\2\2\2\u00a5\u03fc\3\2\2\2\u00a7\u0402\3\2\2\2\u00a9"+
		"\u0408\3\2\2\2\u00ab\u040b\3\2\2\2\u00ad\u0411\3\2\2\2\u00af\u0416\3\2"+
		"\2\2\u00b1\u041c\3\2\2\2\u00b3\u0422\3\2\2\2\u00b5\u0429\3\2\2\2\u00b7"+
		"\u042e\3\2\2\2\u00b9\u0434\3\2\2\2\u00bb\u0438\3\2\2\2\u00bd\u043d\3\2"+
		"\2\2\u00bf\u0442\3\2\2\2\u00c1\u0448\3\2\2\2\u00c3\u044f\3\2\2\2\u00c5"+
		"\u0457\3\2\2\2\u00c7\u045b\3\2\2\2\u00c9\u0462\3\2\2\2\u00cb\u046a\3\2"+
		"\2\2\u00cd\u046f\3\2\2\2\u00cf\u047b\3\2\2\2\u00d1\u0488\3\2\2\2\u00d3"+
		"\u048d\3\2\2\2\u00d5\u0499\3\2\2\2\u00d7\u04a6\3\2\2\2\u00d9\u04ab\3\2"+
		"\2\2\u00db\u04b1\3\2\2\2\u00dd\u04b6\3\2\2\2\u00df\u04bc\3\2\2\2\u00e1"+
		"\u04c3\3\2\2\2\u00e3\u04ca\3\2\2\2\u00e5\u04da\3\2\2\2\u00e7\u04f2\3\2"+
		"\2\2\u00e9\u04fb\3\2\2\2\u00eb\u0502\3\2\2\2\u00ed\u050c\3\2\2\2\u00ef"+
		"\u0514\3\2\2\2\u00f1\u051a\3\2\2\2\u00f3\u051e\3\2\2\2\u00f5\u0524\3\2"+
		"\2\2\u00f7\u052a\3\2\2\2\u00f9\u0530\3\2\2\2\u00fb\u0538\3\2\2\2\u00fd"+
		"\u0543\3\2\2\2\u00ff\u0547\3\2\2\2\u0101\u054d\3\2\2\2\u0103\u0555\3\2"+
		"\2\2\u0105\u055f\3\2\2\2\u0107\u056a\3\2\2\2\u0109\u0573\3\2\2\2\u010b"+
		"\u0578\3\2\2\2\u010d\u057f\3\2\2\2\u010f\u0588\3\2\2\2\u0111\u0594\3\2"+
		"\2\2\u0113\u0599\3\2\2\2\u0115\u059c\3\2\2\2\u0117\u059f\3\2\2\2\u0119"+
		"\u05a2\3\2\2\2\u011b\u05a5\3\2\2\2\u011d\u05a7\3\2\2\2\u011f\u05aa\3\2"+
		"\2\2\u0121\u05ac\3\2\2\2\u0123\u05ae\3\2\2\2\u0125\u05b0\3\2\2\2\u0127"+
		"\u05b2\3\2\2\2\u0129\u05b4\3\2\2\2\u012b\u05b6\3\2\2\2\u012d\u05b8\3\2"+
		"\2\2\u012f\u05ba\3\2\2\2\u0131\u05bc\3\2\2\2\u0133\u05bf\3\2\2\2\u0135"+
		"\u05c1\3\2\2\2\u0137\u05c3\3\2\2\2\u0139\u05c6\3\2\2\2\u013b\u05c8\3\2"+
		"\2\2\u013d\u05cb\3\2\2\2\u013f\u05cd\3\2\2\2\u0141\u05d0\3\2\2\2\u0143"+
		"\u05d3\3\2\2\2\u0145\u05d5\3\2\2\2\u0147\u05d8\3\2\2\2\u0149\u05db\3\2"+
		"\2\2\u014b\u05dd\3\2\2\2\u014d\u05e0\3\2\2\2\u014f\u05e2\3\2\2\2\u0151"+
		"\u05e5\3\2\2\2\u0153\u05e8\3\2\2\2\u0155\u05ea\3\2\2\2\u0157\u05ed\3\2"+
		"\2\2\u0159\u05ef\3\2\2\2\u015b\u05f1\3\2\2\2\u015d\u05f4\3\2\2\2\u015f"+
		"\u05f6\3\2\2\2\u0161\u05f9\3\2\2\2\u0163\u05fc\3\2\2\2\u0165\u05fe\3\2"+
		"\2\2\u0167\u0601\3\2\2\2\u0169\u0604\3\2\2\2\u016b\u0606\3\2\2\2\u016d"+
		"\u0608\3\2\2\2\u016f\u060a\3\2\2\2\u0171\u060c\3\2\2\2\u0173\u060e\3\2"+
		"\2\2\u0175\u0610\3\2\2\2\u0177\u0612\3\2\2\2\u0179\u0614\3\2\2\2\u017b"+
		"\u0617\3\2\2\2\u017d\u061d\3\2\2\2\u017f\u062f\3\2\2\2\u0181\u063d\3\2"+
		"\2\2\u0183\u0647\3\2\2\2\u0185\u0651\3\2\2\2\u0187\u065b\3\2\2\2\u0189"+
		"\u0662\3\2\2\2\u018b\u066d\3\2\2\2\u018d\u0671\3\2\2\2\u018f\u067c\3\2"+
		"\2\2\u0191\u067e\3\2\2\2\u0193\u0685\3\2\2\2\u0195\u0689\3\2\2\2\u0197"+
		"\u068d\3\2\2\2\u0199\u0691\3\2\2\2\u019b\u0695\3\2\2\2\u019d\u06a7\3\2"+
		"\2\2\u019f\u06a9\3\2\2\2\u01a1\u06b5\3\2\2\2\u01a3\u06b7\3\2\2\2\u01a5"+
		"\u06bb\3\2\2\2\u01a7\u06be\3\2\2\2\u01a9\u06c2\3\2\2\2\u01ab\u06c6\3\2"+
		"\2\2\u01ad\u06d0\3\2\2\2\u01af\u06d4\3\2\2\2\u01b1\u06d6\3\2\2\2\u01b3"+
		"\u06dc\3\2\2\2\u01b5\u06e6\3\2\2\2\u01b7\u06ea\3\2\2\2\u01b9\u06ec\3\2"+
		"\2\2\u01bb\u06f0\3\2\2\2\u01bd\u06fa\3\2\2\2\u01bf\u06fe\3\2\2\2\u01c1"+
		"\u071b\3\2\2\2\u01c3\u071d\3\2\2\2\u01c5\u0720\3\2\2\2\u01c7\u0723\3\2"+
		"\2\2\u01c9\u0727\3\2\2\2\u01cb\u0729\3\2\2\2\u01cd\u072b\3\2\2\2\u01cf"+
		"\u073b\3\2\2\2\u01d1\u073d\3\2\2\2\u01d3\u0740\3\2\2\2\u01d5\u01d6\7e"+
		"\2\2\u01d6\u01d7\7t\2\2\u01d7\u01d8\7g\2\2\u01d8\u01d9\7c\2\2\u01d9\u01da"+
		"\7v\2\2\u01da\u01db\7g\2\2\u01db\4\3\2\2\2\u01dc\u01dd\7y\2\2\u01dd\u01de"+
		"\7k\2\2\u01de\u01df\7p\2\2\u01df\u01e0\7f\2\2\u01e0\u01e1\7q\2\2\u01e1"+
		"\u01e2\7y\2\2\u01e2\6\3\2\2\2\u01e3\u01e4\7k\2\2\u01e4\u01e5\7p\2\2\u01e5"+
		"\b\3\2\2\2\u01e6\u01e7\7d\2\2\u01e7\u01e8\7g\2\2\u01e8\u01e9\7v\2\2\u01e9"+
		"\u01ea\7y\2\2\u01ea\u01eb\7g\2\2\u01eb\u01ec\7g\2\2\u01ec\u01ed\7p\2\2"+
		"\u01ed\n\3\2\2\2\u01ee\u01ef\7n\2\2\u01ef\u01f0\7k\2\2\u01f0\u01f1\7m"+
		"\2\2\u01f1\u01f2\7g\2\2\u01f2\f\3\2\2\2\u01f3\u01f4\7t\2\2\u01f4\u01f5"+
		"\7g\2\2\u01f5\u01f6\7i\2\2\u01f6\u01f7\7g\2\2\u01f7\u01f8\7z\2\2\u01f8"+
		"\u01f9\7r\2\2\u01f9\16\3\2\2\2\u01fa\u01fb\7g\2\2\u01fb\u01fc\7u\2\2\u01fc"+
		"\u01fd\7e\2\2\u01fd\u01fe\7c\2\2\u01fe\u01ff\7r\2\2\u01ff\u0200\7g\2\2"+
		"\u0200\20\3\2\2\2\u0201\u0202\7q\2\2\u0202\u0203\7t\2\2\u0203\22\3\2\2"+
		"\2\u0204\u0205\7c\2\2\u0205\u0206\7p\2\2\u0206\u0207\7f\2\2\u0207\24\3"+
		"\2\2\2\u0208\u0209\7p\2\2\u0209\u020a\7q\2\2\u020a\u020b\7v\2\2\u020b"+
		"\26\3\2\2\2\u020c\u020d\7g\2\2\u020d\u020e\7x\2\2\u020e\u020f\7g\2\2\u020f"+
		"\u0210\7t\2\2\u0210\u0211\7{\2\2\u0211\30\3\2\2\2\u0212\u0213\7g\2\2\u0213"+
		"\u0214\7x\2\2\u0214\u0215\7g\2\2\u0215\u0216\7t\2\2\u0216\u0217\7{\2\2"+
		"\u0217\u0218\7/\2\2\u0218\u0219\7f\2\2\u0219\u021a\7k\2\2\u021a\u021b"+
		"\7u\2\2\u021b\u021c\7v\2\2\u021c\u021d\7k\2\2\u021d\u021e\7p\2\2\u021e"+
		"\u021f\7e\2\2\u021f\u0220\7v\2\2\u0220\32\3\2\2\2\u0221\u0222\7y\2\2\u0222"+
		"\u0223\7j\2\2\u0223\u0224\7g\2\2\u0224\u0225\7t\2\2\u0225\u0226\7g\2\2"+
		"\u0226\34\3\2\2\2\u0227\u0228\7c\2\2\u0228\u0229\7u\2\2\u0229\36\3\2\2"+
		"\2\u022a\u022b\7u\2\2\u022b\u022c\7w\2\2\u022c\u022d\7o\2\2\u022d \3\2"+
		"\2\2\u022e\u022f\7c\2\2\u022f\u0230\7x\2\2\u0230\u0231\7i\2\2\u0231\""+
		"\3\2\2\2\u0232\u0233\7o\2\2\u0233\u0234\7c\2\2\u0234\u0235\7z\2\2\u0235"+
		"$\3\2\2\2\u0236\u0237\7o\2\2\u0237\u0238\7k\2\2\u0238\u0239\7p\2\2\u0239"+
		"&\3\2\2\2\u023a\u023b\7e\2\2\u023b\u023c\7q\2\2\u023c\u023d\7c\2\2\u023d"+
		"\u023e\7n\2\2\u023e\u023f\7g\2\2\u023f\u0240\7u\2\2\u0240\u0241\7e\2\2"+
		"\u0241\u0242\7g\2\2\u0242(\3\2\2\2\u0243\u0244\7o\2\2\u0244\u0245\7g\2"+
		"\2\u0245\u0246\7f\2\2\u0246\u0247\7k\2\2\u0247\u0248\7c\2\2\u0248\u0249"+
		"\7p\2\2\u0249*\3\2\2\2\u024a\u024b\7u\2\2\u024b\u024c\7v\2\2\u024c\u024d"+
		"\7f\2\2\u024d\u024e\7f\2\2\u024e\u024f\7g\2\2\u024f\u0250\7x\2\2\u0250"+
		",\3\2\2\2\u0251\u0252\7c\2\2\u0252\u0253\7x\2\2\u0253\u0254\7g\2\2\u0254"+
		"\u0255\7f\2\2\u0255\u0256\7g\2\2\u0256\u0257\7x\2\2\u0257.\3\2\2\2\u0258"+
		"\u0259\7e\2\2\u0259\u025a\7q\2\2\u025a\u025b\7w\2\2\u025b\u025c\7p\2\2"+
		"\u025c\u025d\7v\2\2\u025d\60\3\2\2\2\u025e\u025f\7u\2\2\u025f\u0260\7"+
		"g\2\2\u0260\u0261\7n\2\2\u0261\u0262\7g\2\2\u0262\u0263\7e\2\2\u0263\u0264"+
		"\7v\2\2\u0264\62\3\2\2\2\u0265\u0266\7e\2\2\u0266\u0267\7c\2\2\u0267\u0268"+
		"\7u\2\2\u0268\u0269\7g\2\2\u0269\64\3\2\2\2\u026a\u026b\7g\2\2\u026b\u026c"+
		"\7n\2\2\u026c\u026d\7u\2\2\u026d\u026e\7g\2\2\u026e\66\3\2\2\2\u026f\u0270"+
		"\7y\2\2\u0270\u0271\7j\2\2\u0271\u0272\7g\2\2\u0272\u0273\7p\2\2\u0273"+
		"8\3\2\2\2\u0274\u0275\7v\2\2\u0275\u0276\7j\2\2\u0276\u0277\7g\2\2\u0277"+
		"\u0278\7p\2\2\u0278:\3\2\2\2\u0279\u027a\7g\2\2\u027a\u027b\7p\2\2\u027b"+
		"\u027c\7f\2\2\u027c<\3\2\2\2\u027d\u027e\7h\2\2\u027e\u027f\7t\2\2\u027f"+
		"\u0280\7q\2\2\u0280\u0281\7o\2\2\u0281>\3\2\2\2\u0282\u0283\7q\2\2\u0283"+
		"\u0284\7w\2\2\u0284\u0285\7v\2\2\u0285\u0286\7g\2\2\u0286\u0287\7t\2\2"+
		"\u0287@\3\2\2\2\u0288\u0289\7k\2\2\u0289\u028a\7p\2\2\u028a\u028b\7p\2"+
		"\2\u028b\u028c\7g\2\2\u028c\u028d\7t\2\2\u028dB\3\2\2\2\u028e\u028f\7"+
		"l\2\2\u028f\u0290\7q\2\2\u0290\u0291\7k\2\2\u0291\u0292\7p\2\2\u0292D"+
		"\3\2\2\2\u0293\u0294\7n\2\2\u0294\u0295\7g\2\2\u0295\u0296\7h\2\2\u0296"+
		"\u0297\7v\2\2\u0297F\3\2\2\2\u0298\u0299\7t\2\2\u0299\u029a\7k\2\2\u029a"+
		"\u029b\7i\2\2\u029b\u029c\7j\2\2\u029c\u029d\7v\2\2\u029dH\3\2\2\2\u029e"+
		"\u029f\7h\2\2\u029f\u02a0\7w\2\2\u02a0\u02a1\7n\2\2\u02a1\u02a2\7n\2\2"+
		"\u02a2J\3\2\2\2\u02a3\u02a4\7q\2\2\u02a4\u02a5\7p\2\2\u02a5L\3\2\2\2\u02a6"+
		"\u02a7\7k\2\2\u02a7\u02a8\7u\2\2\u02a8N\3\2\2\2\u02a9\u02aa\7d\2\2\u02aa"+
		"\u02ab\7{\2\2\u02abP\3\2\2\2\u02ac\u02ad\7i\2\2\u02ad\u02ae\7t\2\2\u02ae"+
		"\u02af\7q\2\2\u02af\u02b0\7w\2\2\u02b0\u02b1\7r\2\2\u02b1R\3\2\2\2\u02b2"+
		"\u02b3\7j\2\2\u02b3\u02b4\7c\2\2\u02b4\u02b5\7x\2\2\u02b5\u02b6\7k\2\2"+
		"\u02b6\u02b7\7p\2\2\u02b7\u02b8\7i\2\2\u02b8T\3\2\2\2\u02b9\u02ba\7f\2"+
		"\2\u02ba\u02bb\7k\2\2\u02bb\u02bc\7u\2\2\u02bc\u02bd\7v\2\2\u02bd\u02be"+
		"\7k\2\2\u02be\u02bf\7p\2\2\u02bf\u02c0\7e\2\2\u02c0\u02c1\7v\2\2\u02c1"+
		"V\3\2\2\2\u02c2\u02c3\7c\2\2\u02c3\u02c4\7n\2\2\u02c4\u02c5\7n\2\2\u02c5"+
		"X\3\2\2\2\u02c6\u02c7\7c\2\2\u02c7\u02c8\7p\2\2\u02c8\u02c9\7{\2\2\u02c9"+
		"Z\3\2\2\2\u02ca\u02cb\7u\2\2\u02cb\u02cc\7q\2\2\u02cc\u02cd\7o\2\2\u02cd"+
		"\u02ce\7g\2\2\u02ce\\\3\2\2\2\u02cf\u02d0\7q\2\2\u02d0\u02d1\7w\2\2\u02d1"+
		"\u02d2\7v\2\2\u02d2\u02d3\7r\2\2\u02d3\u02d4\7w\2\2\u02d4\u02d5\7v\2\2"+
		"\u02d5^\3\2\2\2\u02d6\u02d7\7g\2\2\u02d7\u02d8\7x\2\2\u02d8\u02d9\7g\2"+
		"\2\u02d9\u02da\7p\2\2\u02da\u02db\7v\2\2\u02db\u02dc\7u\2\2\u02dc`\3\2"+
		"\2\2\u02dd\u02de\7h\2\2\u02de\u02df\7k\2\2\u02df\u02e0\7t\2\2\u02e0\u02e1"+
		"\7u\2\2\u02e1\u02e2\7v\2\2\u02e2b\3\2\2\2\u02e3\u02e4\7n\2\2\u02e4\u02e5"+
		"\7c\2\2\u02e5\u02e6\7u\2\2\u02e6\u02e7\7v\2\2\u02e7d\3\2\2\2\u02e8\u02e9"+
		"\7k\2\2\u02e9\u02ea\7p\2\2\u02ea\u02eb\7u\2\2\u02eb\u02ec\7g\2\2\u02ec"+
		"\u02ed\7t\2\2\u02ed\u02ee\7v\2\2\u02eef\3\2\2\2\u02ef\u02f0\7k\2\2\u02f0"+
		"\u02f1\7p\2\2\u02f1\u02f2\7v\2\2\u02f2\u02f3\7q\2\2\u02f3h\3\2\2\2\u02f4"+
		"\u02f5\7x\2\2\u02f5\u02f6\7c\2\2\u02f6\u02f7\7n\2\2\u02f7\u02f8\7w\2\2"+
		"\u02f8\u02f9\7g\2\2\u02f9\u02fa\7u\2\2\u02faj\3\2\2\2\u02fb\u02fc\7q\2"+
		"\2\u02fc\u02fd\7t\2\2\u02fd\u02fe\7f\2\2\u02fe\u02ff\7g\2\2\u02ff\u0300"+
		"\7t\2\2\u0300l\3\2\2\2\u0301\u0302\7c\2\2\u0302\u0303\7u\2\2\u0303\u0304"+
		"\7e\2\2\u0304n\3\2\2\2\u0305\u0306\7f\2\2\u0306\u0307\7g\2\2\u0307\u0308"+
		"\7u\2\2\u0308\u0309\7e\2\2\u0309p\3\2\2\2\u030a\u030b\7t\2\2\u030b\u030c"+
		"\7u\2\2\u030c\u030d\7v\2\2\u030d\u030e\7t\2\2\u030e\u030f\7g\2\2\u030f"+
		"\u0310\7c\2\2\u0310\u0311\7o\2\2\u0311r\3\2\2\2\u0312\u0313\7k\2\2\u0313"+
		"\u0314\7u\2\2\u0314\u0315\7v\2\2\u0315\u0316\7t\2\2\u0316\u0317\7g\2\2"+
		"\u0317\u0318\7c\2\2\u0318\u0319\7o\2\2\u0319t\3\2\2\2\u031a\u031b\7k\2"+
		"\2\u031b\u031c\7t\2\2\u031c\u031d\7u\2\2\u031d\u031e\7v\2\2\u031e\u031f"+
		"\7t\2\2\u031f\u0320\7g\2\2\u0320\u0321\7c\2\2\u0321\u0322\7o\2\2\u0322"+
		"v\3\2\2\2\u0323\u0324\7u\2\2\u0324\u0325\7e\2\2\u0325\u0326\7j\2\2\u0326"+
		"\u0327\7g\2\2\u0327\u0328\7o\2\2\u0328\u0329\7c\2\2\u0329x\3\2\2\2\u032a"+
		"\u032b\7w\2\2\u032b\u032c\7p\2\2\u032c\u032d\7k\2\2\u032d\u032e\7f\2\2"+
		"\u032e\u032f\7k\2\2\u032f\u0330\7t\2\2\u0330\u0331\7g\2\2\u0331\u0332"+
		"\7e\2\2\u0332\u0333\7v\2\2\u0333\u0334\7k\2\2\u0334\u0335\7q\2\2\u0335"+
		"\u0336\7p\2\2\u0336\u0337\7c\2\2\u0337\u0338\7n\2\2\u0338z\3\2\2\2\u0339"+
		"\u033a\7t\2\2\u033a\u033b\7g\2\2\u033b\u033c\7v\2\2\u033c\u033d\7c\2\2"+
		"\u033d\u033e\7k\2\2\u033e\u033f\7p\2\2\u033f\u0340\7/\2\2\u0340\u0341"+
		"\7w\2\2\u0341\u0342\7p\2\2\u0342\u0343\7k\2\2\u0343\u0344\7q\2\2\u0344"+
		"\u0345\7p\2\2\u0345|\3\2\2\2\u0346\u0347\7t\2\2\u0347\u0348\7g\2\2\u0348"+
		"\u0349\7v\2\2\u0349\u034a\7c\2\2\u034a\u034b\7k\2\2\u034b\u034c\7p\2\2"+
		"\u034c\u034d\7/\2\2\u034d\u034e\7k\2\2\u034e\u034f\7p\2\2\u034f\u0350"+
		"\7v\2\2\u0350\u0351\7g\2\2\u0351\u0352\7t\2\2\u0352\u0353\7u\2\2\u0353"+
		"\u0354\7g\2\2\u0354\u0355\7e\2\2\u0355\u0356\7v\2\2\u0356\u0357\7k\2\2"+
		"\u0357\u0358\7q\2\2\u0358\u0359\7p\2\2\u0359~\3\2\2\2\u035a\u035b\7r\2"+
		"\2\u035b\u035c\7c\2\2\u035c\u035d\7v\2\2\u035d\u035e\7v\2\2\u035e\u035f"+
		"\7g\2\2\u035f\u0360\7t\2\2\u0360\u0361\7p\2\2\u0361\u0080\3\2\2\2\u0362"+
		"\u0363\7u\2\2\u0363\u0364\7s\2\2\u0364\u0365\7n\2\2\u0365\u0082\3\2\2"+
		"\2\u0366\u0367\7o\2\2\u0367\u0368\7g\2\2\u0368\u0369\7v\2\2\u0369\u036a"+
		"\7c\2\2\u036a\u036b\7f\2\2\u036b\u036c\7c\2\2\u036c\u036d\7v\2\2\u036d"+
		"\u036e\7c\2\2\u036e\u036f\7u\2\2\u036f\u0370\7s\2\2\u0370\u0371\7n\2\2"+
		"\u0371\u0084\3\2\2\2\u0372\u0373\7r\2\2\u0373\u0374\7t\2\2\u0374\u0375"+
		"\7g\2\2\u0375\u0376\7x\2\2\u0376\u0086\3\2\2\2\u0377\u0378\7r\2\2\u0378"+
		"\u0379\7t\2\2\u0379\u037a\7g\2\2\u037a\u037b\7x\2\2\u037b\u037c\7v\2\2"+
		"\u037c\u037d\7c\2\2\u037d\u037e\7k\2\2\u037e\u037f\7n\2\2\u037f\u0088"+
		"\3\2\2\2\u0380\u0381\7r\2\2\u0381\u0382\7t\2\2\u0382\u0383\7g\2\2\u0383"+
		"\u0384\7x\2\2\u0384\u0385\7e\2\2\u0385\u0386\7q\2\2\u0386\u0387\7w\2\2"+
		"\u0387\u0388\7p\2\2\u0388\u0389\7v\2\2\u0389\u008a\3\2\2\2\u038a\u038b"+
		"\7r\2\2\u038b\u038c\7t\2\2\u038c\u038d\7g\2\2\u038d\u038e\7x\2\2\u038e"+
		"\u038f\7y\2\2\u038f\u0390\7k\2\2\u0390\u0391\7p\2\2\u0391\u0392\7f\2\2"+
		"\u0392\u0393\7q\2\2\u0393\u0394\7y\2\2\u0394\u008c\3\2\2\2\u0395\u0396"+
		"\7r\2\2\u0396\u0397\7t\2\2\u0397\u0398\7k\2\2\u0398\u0399\7q\2\2\u0399"+
		"\u039a\7t\2\2\u039a\u008e\3\2\2\2\u039b\u039c\7g\2\2\u039c\u039d\7z\2"+
		"\2\u039d\u039e\7k\2\2\u039e\u039f\7u\2\2\u039f\u03a0\7v\2\2\u03a0\u03a1"+
		"\7u\2\2\u03a1\u0090\3\2\2\2\u03a2\u03a3\7y\2\2\u03a3\u03a4\7g\2\2\u03a4"+
		"\u03a5\7g\2\2\u03a5\u03a6\7m\2\2\u03a6\u03a7\7f\2\2\u03a7\u03a8\7c\2\2"+
		"\u03a8\u03a9\7{\2\2\u03a9\u0092\3\2\2\2\u03aa\u03ab\7n\2\2\u03ab\u03ac"+
		"\7c\2\2\u03ac\u03ad\7u\2\2\u03ad\u03ae\7v\2\2\u03ae\u03af\7y\2\2\u03af"+
		"\u03b0\7g\2\2\u03b0\u03b1\7g\2\2\u03b1\u03b2\7m\2\2\u03b2\u03b3\7f\2\2"+
		"\u03b3\u03b4\7c\2\2\u03b4\u03b5\7{\2\2\u03b5\u0094\3\2\2\2\u03b6\u03b7"+
		"\7k\2\2\u03b7\u03b8\7p\2\2\u03b8\u03b9\7u\2\2\u03b9\u03ba\7v\2\2\u03ba"+
		"\u03bb\7c\2\2\u03bb\u03bc\7p\2\2\u03bc\u03bd\7e\2\2\u03bd\u03be\7g\2\2"+
		"\u03be\u03bf\7q\2\2\u03bf\u03c0\7h\2\2\u03c0\u0096\3\2\2\2\u03c1\u03c2"+
		"\7v\2\2\u03c2\u03c3\7{\2\2\u03c3\u03c4\7r\2\2\u03c4\u03c5\7g\2\2\u03c5"+
		"\u03c6\7q\2\2\u03c6\u03c7\7h\2\2\u03c7\u0098\3\2\2\2\u03c8\u03c9\7e\2"+
		"\2\u03c9\u03ca\7c\2\2\u03ca\u03cb\7u\2\2\u03cb\u03cc\7v\2\2\u03cc\u009a"+
		"\3\2\2\2\u03cd\u03ce\7e\2\2\u03ce\u03cf\7w\2\2\u03cf\u03d0\7t\2\2\u03d0"+
		"\u03d1\7t\2\2\u03d1\u03d2\7g\2\2\u03d2\u03d3\7p\2\2\u03d3\u03d4\7v\2\2"+
		"\u03d4\u03d5\7a\2\2\u03d5\u03d6\7v\2\2\u03d6\u03d7\7k\2\2\u03d7\u03d8"+
		"\7o\2\2\u03d8\u03d9\7g\2\2\u03d9\u03da\7u\2\2\u03da\u03db\7v\2\2\u03db"+
		"\u03dc\7c\2\2\u03dc\u03dd\7o\2\2\u03dd\u03de\7r\2\2\u03de\u009c\3\2\2"+
		"\2\u03df\u03e0\7f\2\2\u03e0\u03e1\7g\2\2\u03e1\u03e2\7n\2\2\u03e2\u03e3"+
		"\7g\2\2\u03e3\u03e4\7v\2\2\u03e4\u03e5\7g\2\2\u03e5\u009e\3\2\2\2\u03e6"+
		"\u03e7\7u\2\2\u03e7\u03e8\7p\2\2\u03e8\u03e9\7c\2\2\u03e9\u03ea\7r\2\2"+
		"\u03ea\u03eb\7u\2\2\u03eb\u03ec\7j\2\2\u03ec\u03ed\7q\2\2\u03ed\u03ee"+
		"\7v\2\2\u03ee\u00a0\3\2\2\2\u03ef\u03f0\7u\2\2\u03f0\u03f1\7g\2\2\u03f1"+
		"\u03f2\7v\2\2\u03f2\u00a2\3\2\2\2\u03f3\u03f4\7x\2\2\u03f4\u03f5\7c\2"+
		"\2\u03f5\u03f6\7t\2\2\u03f6\u03f7\7k\2\2\u03f7\u03f8\7c\2\2\u03f8\u03f9"+
		"\7d\2\2\u03f9\u03fa\7n\2\2\u03fa\u03fb\7g\2\2\u03fb\u00a4\3\2\2\2\u03fc"+
		"\u03fd\7v\2\2\u03fd\u03fe\7c\2\2\u03fe\u03ff\7d\2\2\u03ff\u0400\7n\2\2"+
		"\u0400\u0401\7g\2\2\u0401\u00a6\3\2\2\2\u0402\u0403\7w\2\2\u0403\u0404"+
		"\7p\2\2\u0404\u0405\7v\2\2\u0405\u0406\7k\2\2\u0406\u0407\7n\2\2\u0407"+
		"\u00a8\3\2\2\2\u0408\u0409\7c\2\2\u0409\u040a\7v\2\2\u040a\u00aa\3\2\2"+
		"\2\u040b\u040c\7k\2\2\u040c\u040d\7p\2\2\u040d\u040e\7f\2\2\u040e\u040f"+
		"\7g\2\2\u040f\u0410\7z\2\2\u0410\u00ac\3\2\2\2\u0411\u0412\7{\2\2\u0412"+
		"\u0413\7g\2\2\u0413\u0414\7c\2\2\u0414\u0415\7t\2\2\u0415\u00ae\3\2\2"+
		"\2\u0416\u0417\7{\2\2\u0417\u0418\7g\2\2\u0418\u0419\7c\2\2\u0419\u041a"+
		"\7t\2\2\u041a\u041b\7u\2\2\u041b\u00b0\3\2\2\2\u041c\u041d\7o\2\2\u041d"+
		"\u041e\7q\2\2\u041e\u041f\7p\2\2\u041f\u0420\7v\2\2\u0420\u0421\7j\2\2"+
		"\u0421\u00b2\3\2\2\2\u0422\u0423\7o\2\2\u0423\u0424\7q\2\2\u0424\u0425"+
		"\7p\2\2\u0425\u0426\7v\2\2\u0426\u0427\7j\2\2\u0427\u0428\7u\2\2\u0428"+
		"\u00b4\3\2\2\2\u0429\u042a\7y\2\2\u042a\u042b\7g\2\2\u042b\u042c\7g\2"+
		"\2\u042c\u042d\7m\2\2\u042d\u00b6\3\2\2\2\u042e\u042f\7y\2\2\u042f\u0430"+
		"\7g\2\2\u0430\u0431\7g\2\2\u0431\u0432\7m\2\2\u0432\u0433\7u\2\2\u0433"+
		"\u00b8\3\2\2\2\u0434\u0435\7f\2\2\u0435\u0436\7c\2\2\u0436\u0437\7{\2"+
		"\2\u0437\u00ba\3\2\2\2\u0438\u0439\7f\2\2\u0439\u043a\7c\2\2\u043a\u043b"+
		"\7{\2\2\u043b\u043c\7u\2\2\u043c\u00bc\3\2\2\2\u043d\u043e\7j\2\2\u043e"+
		"\u043f\7q\2\2\u043f\u0440\7w\2\2\u0440\u0441\7t\2\2\u0441\u00be\3\2\2"+
		"\2\u0442\u0443\7j\2\2\u0443\u0444\7q\2\2\u0444\u0445\7w\2\2\u0445\u0446"+
		"\7t\2\2\u0446\u0447\7u\2\2\u0447\u00c0\3\2\2\2\u0448\u0449\7o\2\2\u0449"+
		"\u044a\7k\2\2\u044a\u044b\7p\2\2\u044b\u044c\7w\2\2\u044c\u044d\7v\2\2"+
		"\u044d\u044e\7g\2\2\u044e\u00c2\3\2\2\2\u044f\u0450\7o\2\2\u0450\u0451"+
		"\7k\2\2\u0451\u0452\7p\2\2\u0452\u0453\7w\2\2\u0453\u0454\7v\2\2\u0454"+
		"\u0455\7g\2\2\u0455\u0456\7u\2\2\u0456\u00c4\3\2\2\2\u0457\u0458\7u\2"+
		"\2\u0458\u0459\7g\2\2\u0459\u045a\7e\2\2\u045a\u00c6\3\2\2\2\u045b\u045c"+
		"\7u\2\2\u045c\u045d\7g\2\2\u045d\u045e\7e\2\2\u045e\u045f\7q\2\2\u045f"+
		"\u0460\7p\2\2\u0460\u0461\7f\2\2\u0461\u00c8\3\2\2\2\u0462\u0463\7u\2"+
		"\2\u0463\u0464\7g\2\2\u0464\u0465\7e\2\2\u0465\u0466\7q\2\2\u0466\u0467"+
		"\7p\2\2\u0467\u0468\7f\2\2\u0468\u0469\7u\2\2\u0469\u00ca\3\2\2\2\u046a"+
		"\u046b\7o\2\2\u046b\u046c\7u\2\2\u046c\u046d\7g\2\2\u046d\u046e\7e\2\2"+
		"\u046e\u00cc\3\2\2\2\u046f\u0470\7o\2\2\u0470\u0471\7k\2\2\u0471\u0472"+
		"\7n\2\2\u0472\u0473\7n\2\2\u0473\u0474\7k\2\2\u0474\u0475\7u\2\2\u0475"+
		"\u0476\7g\2\2\u0476\u0477\7e\2\2\u0477\u0478\7q\2\2\u0478\u0479\7p\2\2"+
		"\u0479\u047a\7f\2\2\u047a\u00ce\3\2\2\2\u047b\u047c\7o\2\2\u047c\u047d"+
		"\7k\2\2\u047d\u047e\7n\2\2\u047e\u047f\7n\2\2\u047f\u0480\7k\2\2\u0480"+
		"\u0481\7u\2\2\u0481\u0482\7g\2\2\u0482\u0483\7e\2\2\u0483\u0484\7q\2\2"+
		"\u0484\u0485\7p\2\2\u0485\u0486\7f\2\2\u0486\u0487\7u\2\2\u0487\u00d0"+
		"\3\2\2\2\u0488\u0489\7w\2\2\u0489\u048a\7u\2\2\u048a\u048b\7g\2\2\u048b"+
		"\u048c\7e\2\2\u048c\u00d2\3\2\2\2\u048d\u048e\7o\2\2\u048e\u048f\7k\2"+
		"\2\u048f\u0490\7e\2\2\u0490\u0491\7t\2\2\u0491\u0492\7q\2\2\u0492\u0493"+
		"\7u\2\2\u0493\u0494\7g\2\2\u0494\u0495\7e\2\2\u0495\u0496\7q\2\2\u0496"+
		"\u0497\7p\2\2\u0497\u0498\7f\2\2\u0498\u00d4\3\2\2\2\u0499\u049a\7o\2"+
		"\2\u049a\u049b\7k\2\2\u049b\u049c\7e\2\2\u049c\u049d\7t\2\2\u049d\u049e"+
		"\7q\2\2\u049e\u049f\7u\2\2\u049f\u04a0\7g\2\2\u04a0\u04a1\7e\2\2\u04a1"+
		"\u04a2\7q\2\2\u04a2\u04a3\7p\2\2\u04a3\u04a4\7f\2\2\u04a4\u04a5\7u\2\2"+
		"\u04a5\u00d6\3\2\2\2\u04a6\u04a7\7v\2\2\u04a7\u04a8\7t\2\2\u04a8\u04a9"+
		"\7w\2\2\u04a9\u04aa\7g\2\2\u04aa\u00d8\3\2\2\2\u04ab\u04ac\7h\2\2\u04ac"+
		"\u04ad\7c\2\2\u04ad\u04ae\7n\2\2\u04ae\u04af\7u\2\2\u04af\u04b0\7g\2\2"+
		"\u04b0\u00da\3\2\2\2\u04b1\u04b2\7p\2\2\u04b2\u04b3\7w\2\2\u04b3\u04b4"+
		"\7n\2\2\u04b4\u04b5\7n\2\2\u04b5\u00dc\3\2\2\2\u04b6\u04b7\7n\2\2\u04b7"+
		"\u04b8\7k\2\2\u04b8\u04b9\7o\2\2\u04b9\u04ba\7k\2\2\u04ba\u04bb\7v\2\2"+
		"\u04bb\u00de\3\2\2\2\u04bc\u04bd\7q\2\2\u04bd\u04be\7h\2\2\u04be\u04bf"+
		"\7h\2\2\u04bf\u04c0\7u\2\2\u04c0\u04c1\7g\2\2\u04c1\u04c2\7v\2\2\u04c2"+
		"\u00e0\3\2\2\2\u04c3\u04c4\7w\2\2\u04c4\u04c5\7r\2\2\u04c5\u04c6\7f\2"+
		"\2\u04c6\u04c7\7c\2\2\u04c7\u04c8\7v\2\2\u04c8\u04c9\7g\2\2\u04c9\u00e2"+
		"\3\2\2\2\u04ca\u04cb\7o\2\2\u04cb\u04cc\7c\2\2\u04cc\u04cd\7v\2\2\u04cd"+
		"\u04ce\7e\2\2\u04ce\u04cf\7j\2\2\u04cf\u04d0\7a\2\2\u04d0\u04d1\7t\2\2"+
		"\u04d1\u04d2\7g\2\2\u04d2\u04d3\7e\2\2\u04d3\u04d4\7q\2\2\u04d4\u04d5"+
		"\7i\2\2\u04d5\u04d6\7p\2\2\u04d6\u04d7\7k\2\2\u04d7\u04d8\7|\2\2\u04d8"+
		"\u04d9\7g\2\2\u04d9\u00e4\3\2\2\2\u04da\u04db\7o\2\2\u04db\u04dc\7c\2"+
		"\2\u04dc\u04dd\7v\2\2\u04dd\u04de\7e\2\2\u04de\u04df\7j\2\2\u04df\u04e0"+
		"\7a\2\2\u04e0\u04e1\7t\2\2\u04e1\u04e2\7g\2\2\u04e2\u04e3\7e\2\2\u04e3"+
		"\u04e4\7q\2\2\u04e4\u04e5\7i\2\2\u04e5\u04e6\7p\2\2\u04e6\u04e7\7k\2\2"+
		"\u04e7\u04e8\7|\2\2\u04e8\u04e9\7g\2\2\u04e9\u04ea\7a\2\2\u04ea\u04eb"+
		"\7r\2\2\u04eb\u04ec\7g\2\2\u04ec\u04ed\7t\2\2\u04ed\u04ee\7o\2\2\u04ee"+
		"\u04ef\7w\2\2\u04ef\u04f0\7v\2\2\u04f0\u04f1\7g\2\2\u04f1\u00e6\3\2\2"+
		"\2\u04f2\u04f3\7o\2\2\u04f3\u04f4\7g\2\2\u04f4\u04f5\7c\2\2\u04f5\u04f6"+
		"\7u\2\2\u04f6\u04f7\7w\2\2\u04f7\u04f8\7t\2\2\u04f8\u04f9\7g\2\2\u04f9"+
		"\u04fa\7u\2\2\u04fa\u00e8\3\2\2\2\u04fb\u04fc\7f\2\2\u04fc\u04fd\7g\2"+
		"\2\u04fd\u04fe\7h\2\2\u04fe\u04ff\7k\2\2\u04ff\u0500\7p\2\2\u0500\u0501"+
		"\7g\2\2\u0501\u00ea\3\2\2\2\u0502\u0503\7r\2\2\u0503\u0504\7c\2\2\u0504"+
		"\u0505\7t\2\2\u0505\u0506\7v\2\2\u0506\u0507\7k\2\2\u0507\u0508\7v\2\2"+
		"\u0508\u0509\7k\2\2\u0509\u050a\7q\2\2\u050a\u050b\7p\2\2\u050b\u00ec"+
		"\3\2\2\2\u050c\u050d\7o\2\2\u050d\u050e\7c\2\2\u050e\u050f\7v\2\2\u050f"+
		"\u0510\7e\2\2\u0510\u0511\7j\2\2\u0511\u0512\7g\2\2\u0512\u0513\7u\2\2"+
		"\u0513\u00ee\3\2\2\2\u0514\u0515\7c\2\2\u0515\u0516\7h\2\2\u0516\u0517"+
		"\7v\2\2\u0517\u0518\7g\2\2\u0518\u0519\7t\2\2\u0519\u00f0\3\2\2\2\u051a"+
		"\u051b\7h\2\2\u051b\u051c\7q\2\2\u051c\u051d\7t\2\2\u051d\u00f2\3\2\2"+
		"\2\u051e\u051f\7y\2\2\u051f\u0520\7j\2\2\u0520\u0521\7k\2\2\u0521\u0522"+
		"\7n\2\2\u0522\u0523\7g\2\2\u0523\u00f4\3\2\2\2\u0524\u0525\7w\2\2\u0525"+
		"\u0526\7u\2\2\u0526\u0527\7k\2\2\u0527\u0528\7p\2\2\u0528\u0529\7i\2\2"+
		"\u0529\u00f6\3\2\2\2\u052a\u052b\7o\2\2\u052b\u052c\7g\2\2\u052c\u052d"+
		"\7t\2\2\u052d\u052e\7i\2\2\u052e\u052f\7g\2\2\u052f\u00f8\3\2\2\2\u0530"+
		"\u0531\7o\2\2\u0531\u0532\7c\2\2\u0532\u0533\7v\2\2\u0533\u0534\7e\2\2"+
		"\u0534\u0535\7j\2\2\u0535\u0536\7g\2\2\u0536\u0537\7f\2\2\u0537\u00fa"+
		"\3\2\2\2\u0538\u0539\7g\2\2\u0539\u053a\7z\2\2\u053a\u053b\7r\2\2\u053b"+
		"\u053c\7t\2\2\u053c\u053d\7g\2\2\u053d\u053e\7u\2\2\u053e\u053f\7u\2\2"+
		"\u053f\u0540\7k\2\2\u0540\u0541\7q\2\2\u0541\u0542\7p\2\2\u0542\u00fc"+
		"\3\2\2\2\u0543\u0544\7p\2\2\u0544\u0545\7g\2\2\u0545\u0546\7y\2\2\u0546"+
		"\u00fe\3\2\2\2\u0547\u0548\7u\2\2\u0548\u0549\7v\2\2\u0549\u054a\7c\2"+
		"\2\u054a\u054b\7t\2\2\u054b\u054c\7v\2\2\u054c\u0100\3\2\2\2\u054d\u054e"+
		"\7e\2\2\u054e\u054f\7q\2\2\u054f\u0550\7p\2\2\u0550\u0551\7v\2\2\u0551"+
		"\u0552\7g\2\2\u0552\u0553\7z\2\2\u0553\u0554\7v\2\2\u0554\u0102\3\2\2"+
		"\2\u0555\u0556\7k\2\2\u0556\u0557\7p\2\2\u0557\u0558\7k\2\2\u0558\u0559"+
		"\7v\2\2\u0559\u055a\7k\2\2\u055a\u055b\7c\2\2\u055b\u055c\7v\2\2\u055c"+
		"\u055d\7g\2\2\u055d\u055e\7f\2\2\u055e\u0104\3\2\2\2\u055f\u0560\7v\2"+
		"\2\u0560\u0561\7g\2\2\u0561\u0562\7t\2\2\u0562\u0563\7o\2\2\u0563\u0564"+
		"\7k\2\2\u0564\u0565\7p\2\2\u0565\u0566\7c\2\2\u0566\u0567\7v\2\2\u0567"+
		"\u0568\7g\2\2\u0568\u0569\7f\2\2\u0569\u0106\3\2\2\2\u056a\u056b\7f\2"+
		"\2\u056b\u056c\7c\2\2\u056c\u056d\7v\2\2\u056d\u056e\7c\2\2\u056e\u056f"+
		"\7h\2\2\u056f\u0570\7n\2\2\u0570\u0571\7q\2\2\u0571\u0572\7y\2\2\u0572"+
		"\u0108\3\2\2\2\u0573\u0574\7e\2\2\u0574\u0575\7w\2\2\u0575\u0576\7d\2"+
		"\2\u0576\u0577\7g\2\2\u0577\u010a\3\2\2\2\u0578\u0579\7t\2\2\u0579\u057a"+
		"\7q\2\2\u057a\u057b\7n\2\2\u057b\u057c\7n\2\2\u057c\u057d\7w\2\2\u057d"+
		"\u057e\7r\2\2\u057e\u010c\3\2\2\2\u057f\u0580\7i\2\2\u0580\u0581\7t\2"+
		"\2\u0581\u0582\7q\2\2\u0582\u0583\7w\2\2\u0583\u0584\7r\2\2\u0584\u0585"+
		"\7k\2\2\u0585\u0586\7p\2\2\u0586\u0587\7i\2\2\u0587\u010e\3\2\2\2\u0588"+
		"\u0589\7i\2\2\u0589\u058a\7t\2\2\u058a\u058b\7q\2\2\u058b\u058c\7w\2\2"+
		"\u058c\u058d\7r\2\2\u058d\u058e\7k\2\2\u058e\u058f\7p\2\2\u058f\u0590"+
		"\7i\2\2\u0590\u0591\7a\2\2\u0591\u0592\7k\2\2\u0592\u0593\7f\2\2\u0593"+
		"\u0110\3\2\2\2\u0594\u0595\7u\2\2\u0595\u0596\7g\2\2\u0596\u0597\7v\2"+
		"\2\u0597\u0598\7u\2\2\u0598\u0112\3\2\2\2\u0599\u059a\7/\2\2\u059a\u059b"+
		"\7]\2\2\u059b\u0114\3\2\2\2\u059c\u059d\7_\2\2\u059d\u059e\7@\2\2\u059e"+
		"\u0116\3\2\2\2\u059f\u05a0\7/\2\2\u05a0\u05a1\7@\2\2\u05a1\u0118\3\2\2"+
		"\2\u05a2\u05a3\7?\2\2\u05a3\u05a4\7@\2\2\u05a4\u011a\3\2\2\2\u05a5\u05a6"+
		"\7?\2\2\u05a6\u011c\3\2\2\2\u05a7\u05a8\7>\2\2\u05a8\u05a9\7@\2\2\u05a9"+
		"\u011e\3\2\2\2\u05aa\u05ab\7A\2\2\u05ab\u0120\3\2\2\2\u05ac\u05ad\7*\2"+
		"\2\u05ad\u0122\3\2\2\2\u05ae\u05af\7+\2\2\u05af\u0124\3\2\2\2\u05b0\u05b1"+
		"\7]\2\2\u05b1\u0126\3\2\2\2\u05b2\u05b3\7_\2\2\u05b3\u0128\3\2\2\2\u05b4"+
		"\u05b5\7}\2\2\u05b5\u012a\3\2\2\2\u05b6\u05b7\7\177\2\2\u05b7\u012c\3"+
		"\2\2\2\u05b8\u05b9\7<\2\2\u05b9\u012e\3\2\2\2\u05ba\u05bb\7.\2\2\u05bb"+
		"\u0130\3\2\2\2\u05bc\u05bd\7?\2\2\u05bd\u05be\7?\2\2\u05be\u0132\3\2\2"+
		"\2\u05bf\u05c0\7#\2\2\u05c0\u0134\3\2\2\2\u05c1\u05c2\7\u0080\2\2\u05c2"+
		"\u0136\3\2\2\2\u05c3\u05c4\7#\2\2\u05c4\u05c5\7?\2\2\u05c5\u0138\3\2\2"+
		"\2\u05c6\u05c7\7\61\2\2\u05c7\u013a\3\2\2\2\u05c8\u05c9\7\61\2\2\u05c9"+
		"\u05ca\7?\2\2\u05ca\u013c\3\2\2\2\u05cb\u05cc\7-\2\2\u05cc\u013e\3\2\2"+
		"\2\u05cd\u05ce\7-\2\2\u05ce\u05cf\7?\2\2\u05cf\u0140\3\2\2\2\u05d0\u05d1"+
		"\7-\2\2\u05d1\u05d2\7-\2\2\u05d2\u0142\3\2\2\2\u05d3\u05d4\7/\2\2\u05d4"+
		"\u0144\3\2\2\2\u05d5\u05d6\7/\2\2\u05d6\u05d7\7?\2\2\u05d7\u0146\3\2\2"+
		"\2\u05d8\u05d9\7/\2\2\u05d9\u05da\7/\2\2\u05da\u0148\3\2\2\2\u05db\u05dc"+
		"\7,\2\2\u05dc\u014a\3\2\2\2\u05dd\u05de\7,\2\2\u05de\u05df\7?\2\2\u05df"+
		"\u014c\3\2\2\2\u05e0\u05e1\7\'\2\2\u05e1\u014e\3\2\2\2\u05e2\u05e3\7\'"+
		"\2\2\u05e3\u05e4\7?\2\2\u05e4\u0150\3\2\2\2\u05e5\u05e6\7@\2\2\u05e6\u05e7"+
		"\7?\2\2\u05e7\u0152\3\2\2\2\u05e8\u05e9\7@\2\2\u05e9\u0154\3\2\2\2\u05ea"+
		"\u05eb\7>\2\2\u05eb\u05ec\7?\2\2\u05ec\u0156\3\2\2\2\u05ed\u05ee\7>\2"+
		"\2\u05ee\u0158\3\2\2\2\u05ef\u05f0\7`\2\2\u05f0\u015a\3\2\2\2\u05f1\u05f2"+
		"\7`\2\2\u05f2\u05f3\7?\2\2\u05f3\u015c\3\2\2\2\u05f4\u05f5\7~\2\2\u05f5"+
		"\u015e\3\2\2\2\u05f6\u05f7\7~\2\2\u05f7\u05f8\7?\2\2\u05f8\u0160\3\2\2"+
		"\2\u05f9\u05fa\7~\2\2\u05fa\u05fb\7~\2\2\u05fb\u0162\3\2\2\2\u05fc\u05fd"+
		"\7(\2\2\u05fd\u0164\3\2\2\2\u05fe\u05ff\7(\2\2\u05ff\u0600\7?\2\2\u0600"+
		"\u0166\3\2\2\2\u0601\u0602\7(\2\2\u0602\u0603\7(\2\2\u0603\u0168\3\2\2"+
		"\2\u0604\u0605\7=\2\2\u0605\u016a\3\2\2\2\u0606\u0607\7\60\2\2\u0607\u016c"+
		"\3\2\2\2\u0608\u0609\7\u1901\2\2\u0609\u016e\3\2\2\2\u060a\u060b\7\u1900"+
		"\2\2\u060b\u0170\3\2\2\2\u060c\u060d\7\u18ff\2\2\u060d\u0172\3\2\2\2\u060e"+
		"\u060f\7^\2\2\u060f\u0174\3\2\2\2\u0610\u0611\7b\2\2\u0611\u0176\3\2\2"+
		"\2\u0612\u0613\7B\2\2\u0613\u0178\3\2\2\2\u0614\u0615\7%\2\2\u0615\u017a"+
		"\3\2\2\2\u0616\u0618\t\2\2\2\u0617\u0616\3\2\2\2\u0618\u0619\3\2\2\2\u0619"+
		"\u0617\3\2\2\2\u0619\u061a\3\2\2\2\u061a\u061b\3\2\2\2\u061b\u061c\b\u00be"+
		"\2\2\u061c\u017c\3\2\2\2\u061d\u061e\7\61\2\2\u061e\u061f\7\61\2\2\u061f"+
		"\u0623\3\2\2\2\u0620\u0622\n\3\2\2\u0621\u0620\3\2\2\2\u0622\u0625\3\2"+
		"\2\2\u0623\u0621\3\2\2\2\u0623\u0624\3\2\2\2\u0624\u062b\3\2\2\2\u0625"+
		"\u0623\3\2\2\2\u0626\u062c\7\f\2\2\u0627\u0629\7\17\2\2\u0628\u062a\7"+
		"\f\2\2\u0629\u0628\3\2\2\2\u0629\u062a\3\2\2\2\u062a\u062c\3\2\2\2\u062b"+
		"\u0626\3\2\2\2\u062b\u0627\3\2\2\2\u062b\u062c\3\2\2\2\u062c\u062d\3\2"+
		"\2\2\u062d\u062e\b\u00bf\2\2\u062e\u017e\3\2\2\2\u062f\u0630\7\61\2\2"+
		"\u0630\u0631\7,\2\2\u0631\u0635\3\2\2\2\u0632\u0634\13\2\2\2\u0633\u0632"+
		"\3\2\2\2\u0634\u0637\3\2\2\2\u0635\u0636\3\2\2\2\u0635\u0633\3\2\2\2\u0636"+
		"\u0638\3\2\2\2\u0637\u0635\3\2\2\2\u0638\u0639\7,\2\2\u0639\u063a\7\61"+
		"\2\2\u063a\u063b\3\2\2\2\u063b\u063c\b\u00c0\2\2\u063c\u0180\3\2\2\2\u063d"+
		"\u0642\7b\2\2\u063e\u0641\5\u0187\u00c4\2\u063f\u0641\n\4\2\2\u0640\u063e"+
		"\3\2\2\2\u0640\u063f\3\2\2\2\u0641\u0644\3\2\2\2\u0642\u0640\3\2\2\2\u0642"+
		"\u0643\3\2\2\2\u0643\u0645\3\2\2\2\u0644\u0642\3\2\2\2\u0645\u0646\7b"+
		"\2\2\u0646\u0182\3\2\2\2\u0647\u064c\7)\2\2\u0648\u064b\5\u0187\u00c4"+
		"\2\u0649\u064b\n\5\2\2\u064a\u0648\3\2\2\2\u064a\u0649\3\2\2\2\u064b\u064e"+
		"\3\2\2\2\u064c\u064a\3\2\2\2\u064c\u064d\3\2\2\2\u064d\u064f\3\2\2\2\u064e"+
		"\u064c\3\2\2\2\u064f\u0650\7)\2\2\u0650\u0184\3\2\2\2\u0651\u0656\7$\2"+
		"\2\u0652\u0655\5\u0187\u00c4\2\u0653\u0655\n\6\2\2\u0654\u0652\3\2\2\2"+
		"\u0654\u0653\3\2\2\2\u0655\u0658\3\2\2\2\u0656\u0654\3\2\2\2\u0656\u0657"+
		"\3\2\2\2\u0657\u0659\3\2\2\2\u0658\u0656\3\2\2\2\u0659\u065a\7$\2\2\u065a"+
		"\u0186\3\2\2\2\u065b\u0660\7^\2\2\u065c\u0661\t\7\2\2\u065d\u0661\5\u0191"+
		"\u00c9\2\u065e\u0661\5\u018f\u00c8\2\u065f\u0661\13\2\2\2\u0660\u065c"+
		"\3\2\2\2\u0660\u065d\3\2\2\2\u0660\u065e\3\2\2\2\u0660\u065f\3\2\2\2\u0661"+
		"\u0188\3\2\2\2\u0662\u0666\t\b\2\2\u0663\u0665\t\t\2\2\u0664\u0663\3\2"+
		"\2\2\u0665\u0668\3\2\2\2\u0666\u0664\3\2\2\2\u0666\u0667\3\2\2\2\u0667"+
		"\u018a\3\2\2\2\u0668\u0666\3\2\2\2\u0669\u066e\5\u0193\u00ca\2\u066a\u066e"+
		"\5\u0195\u00cb\2\u066b\u066e\5\u0197\u00cc\2\u066c\u066e\5\u0199\u00cd"+
		"\2\u066d\u0669\3\2\2\2\u066d\u066a\3\2\2\2\u066d\u066b\3\2\2\2\u066d\u066c"+
		"\3\2\2\2\u066e\u018c\3\2\2\2\u066f\u0672\5\u01c1\u00e1\2\u0670\u0672\5"+
		"\u01cd\u00e7\2\u0671\u066f\3\2\2\2\u0671\u0670\3\2\2\2\u0672\u018e\3\2"+
		"\2\2\u0673\u0674\7^\2\2\u0674\u0675\4\62\65\2\u0675\u0676\4\629\2\u0676"+
		"\u067d\4\629\2\u0677\u0678\7^\2\2\u0678\u0679\4\629\2\u0679\u067d\4\62"+
		"9\2\u067a\u067b\7^\2\2\u067b\u067d\4\629\2\u067c\u0673\3\2\2\2\u067c\u0677"+
		"\3\2\2\2\u067c\u067a\3\2\2\2\u067d\u0190\3\2\2\2\u067e\u067f\7^\2\2\u067f"+
		"\u0680\7w\2\2\u0680\u0681\5\u01ad\u00d7\2\u0681\u0682\5\u01ad\u00d7\2"+
		"\u0682\u0683\5\u01ad\u00d7\2\u0683\u0684\5\u01ad\u00d7\2\u0684\u0192\3"+
		"\2\2\2\u0685\u0687\5\u019d\u00cf\2\u0686\u0688\5\u019b\u00ce\2\u0687\u0686"+
		"\3\2\2\2\u0687\u0688\3\2\2\2\u0688\u0194\3\2\2\2\u0689\u068b\5\u01a9\u00d5"+
		"\2\u068a\u068c\5\u019b\u00ce\2\u068b\u068a\3\2\2\2\u068b\u068c\3\2\2\2"+
		"\u068c\u0196\3\2\2\2\u068d\u068f\5\u01b1\u00d9\2\u068e\u0690\5\u019b\u00ce"+
		"\2\u068f\u068e\3\2\2\2\u068f\u0690\3\2\2\2\u0690\u0198\3\2\2\2\u0691\u0693"+
		"\5\u01b9\u00dd\2\u0692\u0694\5\u019b\u00ce\2\u0693\u0692\3\2\2\2\u0693"+
		"\u0694\3\2\2\2\u0694\u019a\3\2\2\2\u0695\u0696\t\n\2\2\u0696\u019c\3\2"+
		"\2\2\u0697\u06a8\7\62\2\2\u0698\u069a\7\62\2\2\u0699\u0698\3\2\2\2\u069a"+
		"\u069d\3\2\2\2\u069b\u0699\3\2\2\2\u069b\u069c\3\2\2\2\u069c\u069e\3\2"+
		"\2\2\u069d\u069b\3\2\2\2\u069e\u06a5\5\u01a3\u00d2\2\u069f\u06a1\5\u019f"+
		"\u00d0\2\u06a0\u069f\3\2\2\2\u06a0\u06a1\3\2\2\2\u06a1\u06a6\3\2\2\2\u06a2"+
		"\u06a3\5\u01a7\u00d4\2\u06a3\u06a4\5\u019f\u00d0\2\u06a4\u06a6\3\2\2\2"+
		"\u06a5\u06a0\3\2\2\2\u06a5\u06a2\3\2\2\2\u06a6\u06a8\3\2\2\2\u06a7\u0697"+
		"\3\2\2\2\u06a7\u069b\3\2\2\2\u06a8\u019e\3\2\2\2\u06a9\u06b1\5\u01a1\u00d1"+
		"\2\u06aa\u06ac\5\u01a5\u00d3\2\u06ab\u06aa\3\2\2\2\u06ac\u06af\3\2\2\2"+
		"\u06ad\u06ab\3\2\2\2\u06ad\u06ae\3\2\2\2\u06ae\u06b0\3\2\2\2\u06af\u06ad"+
		"\3\2\2\2\u06b0\u06b2\5\u01a1\u00d1\2\u06b1\u06ad\3\2\2\2\u06b1\u06b2\3"+
		"\2\2\2\u06b2\u01a0\3\2\2\2\u06b3\u06b6\7\62\2\2\u06b4\u06b6\5\u01a3\u00d2"+
		"\2\u06b5\u06b3\3\2\2\2\u06b5\u06b4\3\2\2\2\u06b6\u01a2\3\2\2\2\u06b7\u06b8"+
		"\t\13\2\2\u06b8\u01a4\3\2\2\2\u06b9\u06bc\5\u01a1\u00d1\2\u06ba\u06bc"+
		"\7a\2\2\u06bb\u06b9\3\2\2\2\u06bb\u06ba\3\2\2\2\u06bc\u01a6\3\2\2\2\u06bd"+
		"\u06bf\7a\2\2\u06be\u06bd\3\2\2\2\u06bf\u06c0\3\2\2\2\u06c0\u06be\3\2"+
		"\2\2\u06c0\u06c1\3\2\2\2\u06c1\u01a8\3\2\2\2\u06c2\u06c3\7\62\2\2\u06c3"+
		"\u06c4\t\f\2\2\u06c4\u06c5\5\u01ab\u00d6\2\u06c5\u01aa\3\2\2\2\u06c6\u06ce"+
		"\5\u01ad\u00d7\2\u06c7\u06c9\5\u01af\u00d8\2\u06c8\u06c7\3\2\2\2\u06c9"+
		"\u06cc\3\2\2\2\u06ca\u06c8\3\2\2\2\u06ca\u06cb\3\2\2\2\u06cb\u06cd\3\2"+
		"\2\2\u06cc\u06ca\3\2\2\2\u06cd\u06cf\5\u01ad\u00d7\2\u06ce\u06ca\3\2\2"+
		"\2\u06ce\u06cf\3\2\2\2\u06cf\u01ac\3\2\2\2\u06d0\u06d1\t\r\2\2\u06d1\u01ae"+
		"\3\2\2\2\u06d2\u06d5\5\u01ad\u00d7\2\u06d3\u06d5\7a\2\2\u06d4\u06d2\3"+
		"\2\2\2\u06d4\u06d3\3\2\2\2\u06d5\u01b0\3\2\2\2\u06d6\u06d8\7\62\2\2\u06d7"+
		"\u06d9\5\u01a7\u00d4\2\u06d8\u06d7\3\2\2\2\u06d8\u06d9\3\2\2\2\u06d9\u06da"+
		"\3\2\2\2\u06da\u06db\5\u01b3\u00da\2\u06db\u01b2\3\2\2\2\u06dc\u06e4\5"+
		"\u01b5\u00db\2\u06dd\u06df\5\u01b7\u00dc\2\u06de\u06dd\3\2\2\2\u06df\u06e2"+
		"\3\2\2\2\u06e0\u06de\3\2\2\2\u06e0\u06e1\3\2\2\2\u06e1\u06e3\3\2\2\2\u06e2"+
		"\u06e0\3\2\2\2\u06e3\u06e5\5\u01b5\u00db\2\u06e4\u06e0\3\2\2\2\u06e4\u06e5"+
		"\3\2\2\2\u06e5\u01b4\3\2\2\2\u06e6\u06e7\t\16\2\2\u06e7\u01b6\3\2\2\2"+
		"\u06e8\u06eb\5\u01b5\u00db\2\u06e9\u06eb\7a\2\2\u06ea\u06e8\3\2\2\2\u06ea"+
		"\u06e9\3\2\2\2\u06eb\u01b8\3\2\2\2\u06ec\u06ed\7\62\2\2\u06ed\u06ee\t"+
		"\17\2\2\u06ee\u06ef\5\u01bb\u00de\2\u06ef\u01ba\3\2\2\2\u06f0\u06f8\5"+
		"\u01bd\u00df\2\u06f1\u06f3\5\u01bf\u00e0\2\u06f2\u06f1\3\2\2\2\u06f3\u06f6"+
		"\3\2\2\2\u06f4\u06f2\3\2\2\2\u06f4\u06f5\3\2\2\2\u06f5\u06f7\3\2\2\2\u06f6"+
		"\u06f4\3\2\2\2\u06f7\u06f9\5\u01bd\u00df\2\u06f8\u06f4\3\2\2\2\u06f8\u06f9"+
		"\3\2\2\2\u06f9\u01bc\3\2\2\2\u06fa\u06fb\t\20\2\2\u06fb\u01be\3\2\2\2"+
		"\u06fc\u06ff\5\u01bd\u00df\2\u06fd\u06ff\7a\2\2\u06fe\u06fc\3\2\2\2\u06fe"+
		"\u06fd\3\2\2\2\u06ff\u01c0\3\2\2\2\u0700\u0701\5\u019f\u00d0\2\u0701\u0703"+
		"\7\60\2\2\u0702\u0704\5\u019f\u00d0\2\u0703\u0702\3\2\2\2\u0703\u0704"+
		"\3\2\2\2\u0704\u0706\3\2\2\2\u0705\u0707\5\u01c3\u00e2\2\u0706\u0705\3"+
		"\2\2\2\u0706\u0707\3\2\2\2\u0707\u0709\3\2\2\2\u0708\u070a\5\u01cb\u00e6"+
		"\2\u0709\u0708\3\2\2\2\u0709\u070a\3\2\2\2\u070a\u071c\3\2\2\2\u070b\u070c"+
		"\7\60\2\2\u070c\u070e\5\u019f\u00d0\2\u070d\u070f\5\u01c3\u00e2\2\u070e"+
		"\u070d\3\2\2\2\u070e\u070f\3\2\2\2\u070f\u0711\3\2\2\2\u0710\u0712\5\u01cb"+
		"\u00e6\2\u0711\u0710\3\2\2\2\u0711\u0712\3\2\2\2\u0712\u071c\3\2\2\2\u0713"+
		"\u0714\5\u019f\u00d0\2\u0714\u0716\5\u01c3\u00e2\2\u0715\u0717\5\u01cb"+
		"\u00e6\2\u0716\u0715\3\2\2\2\u0716\u0717\3\2\2\2\u0717\u071c\3\2\2\2\u0718"+
		"\u0719\5\u019f\u00d0\2\u0719\u071a\5\u01cb\u00e6\2\u071a\u071c\3\2\2\2"+
		"\u071b\u0700\3\2\2\2\u071b\u070b\3\2\2\2\u071b\u0713\3\2\2\2\u071b\u0718"+
		"\3\2\2\2\u071c\u01c2\3\2\2\2\u071d\u071e\5\u01c5\u00e3\2\u071e\u071f\5"+
		"\u01c7\u00e4\2\u071f\u01c4\3\2\2\2\u0720\u0721\t\21\2\2\u0721\u01c6\3"+
		"\2\2\2\u0722\u0724\5\u01c9\u00e5\2\u0723\u0722\3\2\2\2\u0723\u0724\3\2"+
		"\2\2\u0724\u0725\3\2\2\2\u0725\u0726\5\u019f\u00d0\2\u0726\u01c8\3\2\2"+
		"\2\u0727\u0728\t\22\2\2\u0728\u01ca\3\2\2\2\u0729\u072a\t\23\2\2\u072a"+
		"\u01cc\3\2\2\2\u072b\u072c\5\u01cf\u00e8\2\u072c\u072e\5\u01d1\u00e9\2"+
		"\u072d\u072f\5\u01cb\u00e6\2\u072e\u072d\3\2\2\2\u072e\u072f\3\2\2\2\u072f"+
		"\u01ce\3\2\2\2\u0730\u0732\5\u01a9\u00d5\2\u0731\u0733\7\60\2\2\u0732"+
		"\u0731\3\2\2\2\u0732\u0733\3\2\2\2\u0733\u073c\3\2\2\2\u0734\u0735\7\62"+
		"\2\2\u0735\u0737\t\f\2\2\u0736\u0738\5\u01ab\u00d6\2\u0737\u0736\3\2\2"+
		"\2\u0737\u0738\3\2\2\2\u0738\u0739\3\2\2\2\u0739\u073a\7\60\2\2\u073a"+
		"\u073c\5\u01ab\u00d6\2\u073b\u0730\3\2\2\2\u073b\u0734\3\2\2\2\u073c\u01d0"+
		"\3\2\2\2\u073d\u073e\5\u01d3\u00ea\2\u073e\u073f\5\u01c7\u00e4\2\u073f"+
		"\u01d2\3\2\2\2\u0740\u0741\t\24\2\2\u0741\u01d4\3\2\2\2\67\2\u0617\u0619"+
		"\u0623\u0629\u062b\u0635\u0640\u0642\u064a\u064c\u0654\u0656\u0660\u0666"+
		"\u066d\u0671\u067c\u0687\u068b\u068f\u0693\u069b\u06a0\u06a5\u06a7\u06ad"+
		"\u06b1\u06b5\u06bb\u06c0\u06ca\u06ce\u06d4\u06d8\u06e0\u06e4\u06ea\u06f4"+
		"\u06f8\u06fe\u0703\u0706\u0709\u070e\u0711\u0716\u071b\u0723\u072e\u0732"+
		"\u0737\u073b\3\2\3\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}