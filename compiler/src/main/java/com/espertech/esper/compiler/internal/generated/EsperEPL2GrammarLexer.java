// Generated from EsperEPL2Grammar.g by ANTLR 4.9.3

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
package com.espertech.esper.compiler.internal.generated;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class EsperEPL2GrammarLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.9.3", RuntimeMetaData.VERSION); }

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
		MERGE=123, MATCHED=124, EXPRESSIONDECL=125, CLASSDECL=126, NEWKW=127, 
		START=128, CONTEXT=129, INITIATED=130, TERMINATED=131, DATAFLOW=132, CUBE=133, 
		ROLLUP=134, GROUPING=135, GROUPING_ID=136, SETS=137, EVENTPRECEDENCE=138, 
		FOLLOWMAX_BEGIN=139, FOLLOWED_BY=140, GOES=141, EQUALS=142, SQL_NE=143, 
		QUESTION=144, LPAREN=145, RPAREN=146, LBRACK=147, RBRACK=148, LCURLY=149, 
		RCURLY=150, COLON=151, COMMA=152, EQUAL=153, LNOT=154, BNOT=155, NOT_EQUAL=156, 
		DIV=157, DIV_ASSIGN=158, PLUS=159, PLUS_ASSIGN=160, INC=161, MINUS=162, 
		MINUS_ASSIGN=163, DEC=164, STAR=165, STAR_ASSIGN=166, MOD=167, MOD_ASSIGN=168, 
		GE=169, GT=170, LE=171, LT=172, BXOR=173, BXOR_ASSIGN=174, BOR=175, BOR_ASSIGN=176, 
		LOR=177, BAND=178, BAND_ASSIGN=179, LAND=180, SEMI=181, DOT=182, NUM_LONG=183, 
		NUM_DOUBLE=184, NUM_FLOAT=185, ESCAPECHAR=186, ESCAPEBACKTICK=187, ATCHAR=188, 
		HASHCHAR=189, WS=190, SL_COMMENT=191, ML_COMMENT=192, TICKED_STRING_LITERAL=193, 
		QUOTED_STRING_LITERAL=194, STRING_LITERAL=195, TRIPLEQUOTE=196, IDENT=197, 
		IntegerLiteral=198, FloatingPointLiteral=199;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"CREATE", "WINDOW", "IN_SET", "BETWEEN", "LIKE", "REGEXP", "ESCAPE", 
			"OR_EXPR", "AND_EXPR", "NOT_EXPR", "EVERY_EXPR", "EVERY_DISTINCT_EXPR", 
			"WHERE", "AS", "SUM", "AVG", "MAX", "MIN", "COALESCE", "MEDIAN", "STDDEV", 
			"AVEDEV", "COUNT", "SELECT", "CASE", "ELSE", "WHEN", "THEN", "END", "FROM", 
			"OUTER", "INNER", "JOIN", "LEFT", "RIGHT", "FULL", "ON", "IS", "BY", 
			"GROUP", "HAVING", "DISTINCT", "ALL", "ANY", "SOME", "OUTPUT", "EVENTS", 
			"FIRST", "LAST", "INSERT", "INTO", "VALUES", "ORDER", "ASC", "DESC", 
			"RSTREAM", "ISTREAM", "IRSTREAM", "SCHEMA", "UNIDIRECTIONAL", "RETAINUNION", 
			"RETAININTERSECTION", "PATTERN", "SQL", "METADATASQL", "PREVIOUS", "PREVIOUSTAIL", 
			"PREVIOUSCOUNT", "PREVIOUSWINDOW", "PRIOR", "EXISTS", "WEEKDAY", "LW", 
			"INSTANCEOF", "TYPEOF", "CAST", "CURRENT_TIMESTAMP", "DELETE", "SNAPSHOT", 
			"SET", "VARIABLE", "TABLE", "UNTIL", "AT", "INDEX", "TIMEPERIOD_YEAR", 
			"TIMEPERIOD_YEARS", "TIMEPERIOD_MONTH", "TIMEPERIOD_MONTHS", "TIMEPERIOD_WEEK", 
			"TIMEPERIOD_WEEKS", "TIMEPERIOD_DAY", "TIMEPERIOD_DAYS", "TIMEPERIOD_HOUR", 
			"TIMEPERIOD_HOURS", "TIMEPERIOD_MINUTE", "TIMEPERIOD_MINUTES", "TIMEPERIOD_SEC", 
			"TIMEPERIOD_SECOND", "TIMEPERIOD_SECONDS", "TIMEPERIOD_MILLISEC", "TIMEPERIOD_MILLISECOND", 
			"TIMEPERIOD_MILLISECONDS", "TIMEPERIOD_MICROSEC", "TIMEPERIOD_MICROSECOND", 
			"TIMEPERIOD_MICROSECONDS", "BOOLEAN_TRUE", "BOOLEAN_FALSE", "VALUE_NULL", 
			"ROW_LIMIT_EXPR", "OFFSET", "UPDATE", "MATCH_RECOGNIZE", "MATCH_RECOGNIZE_PERMUTE", 
			"MEASURES", "DEFINE", "PARTITION", "MATCHES", "AFTER", "FOR", "WHILE", 
			"USING", "MERGE", "MATCHED", "EXPRESSIONDECL", "CLASSDECL", "NEWKW", 
			"START", "CONTEXT", "INITIATED", "TERMINATED", "DATAFLOW", "CUBE", "ROLLUP", 
			"GROUPING", "GROUPING_ID", "SETS", "EVENTPRECEDENCE", "FOLLOWMAX_BEGIN", 
			"FOLLOWED_BY", "GOES", "EQUALS", "SQL_NE", "QUESTION", "LPAREN", "RPAREN", 
			"LBRACK", "RBRACK", "LCURLY", "RCURLY", "COLON", "COMMA", "EQUAL", "LNOT", 
			"BNOT", "NOT_EQUAL", "DIV", "DIV_ASSIGN", "PLUS", "PLUS_ASSIGN", "INC", 
			"MINUS", "MINUS_ASSIGN", "DEC", "STAR", "STAR_ASSIGN", "MOD", "MOD_ASSIGN", 
			"GE", "GT", "LE", "LT", "BXOR", "BXOR_ASSIGN", "BOR", "BOR_ASSIGN", "LOR", 
			"BAND", "BAND_ASSIGN", "LAND", "SEMI", "DOT", "NUM_LONG", "NUM_DOUBLE", 
			"NUM_FLOAT", "ESCAPECHAR", "ESCAPEBACKTICK", "ATCHAR", "HASHCHAR", "WS", 
			"SL_COMMENT", "ML_COMMENT", "TICKED_STRING_LITERAL", "QUOTED_STRING_LITERAL", 
			"STRING_LITERAL", "TRIPLEQUOTE", "EscapeSequence", "IDENT", "IntegerLiteral", 
			"FloatingPointLiteral", "OctalEscape", "UnicodeEscape", "DecimalIntegerLiteral", 
			"HexIntegerLiteral", "OctalIntegerLiteral", "BinaryIntegerLiteral", "IntegerTypeSuffix", 
			"DecimalNumeral", "Digits", "Digit", "NonZeroDigit", "DigitOrUnderscore", 
			"Underscores", "HexNumeral", "HexDigits", "HexDigit", "HexDigitOrUnderscore", 
			"OctalNumeral", "OctalDigits", "OctalDigit", "OctalDigitOrUnderscore", 
			"BinaryNumeral", "BinaryDigits", "BinaryDigit", "BinaryDigitOrUnderscore", 
			"DecimalFloatingPointLiteral", "ExponentPart", "ExponentIndicator", "SignedInteger", 
			"Sign", "FloatTypeSuffix", "HexadecimalFloatingPointLiteral", "HexSignificand", 
			"BinaryExponent", "BinaryExponentIndicator"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'create'", "'window'", "'in'", "'between'", "'like'", "'regexp'", 
			"'escape'", "'or'", "'and'", "'not'", "'every'", "'every-distinct'", 
			"'where'", "'as'", "'sum'", "'avg'", "'max'", "'min'", "'coalesce'", 
			"'median'", "'stddev'", "'avedev'", "'count'", "'select'", "'case'", 
			"'else'", "'when'", "'then'", "'end'", "'from'", "'outer'", "'inner'", 
			"'join'", "'left'", "'right'", "'full'", "'on'", "'is'", "'by'", "'group'", 
			"'having'", "'distinct'", "'all'", "'any'", "'some'", "'output'", "'events'", 
			"'first'", "'last'", "'insert'", "'into'", "'values'", "'order'", "'asc'", 
			"'desc'", "'rstream'", "'istream'", "'irstream'", "'schema'", "'unidirectional'", 
			"'retain-union'", "'retain-intersection'", "'pattern'", "'sql'", "'metadatasql'", 
			"'prev'", "'prevtail'", "'prevcount'", "'prevwindow'", "'prior'", "'exists'", 
			"'weekday'", "'lastweekday'", "'instanceof'", "'typeof'", "'cast'", "'current_timestamp'", 
			"'delete'", "'snapshot'", "'set'", "'variable'", "'table'", "'until'", 
			"'at'", "'index'", "'year'", "'years'", "'month'", "'months'", "'week'", 
			"'weeks'", "'day'", "'days'", "'hour'", "'hours'", "'minute'", "'minutes'", 
			"'sec'", "'second'", "'seconds'", "'msec'", "'millisecond'", "'milliseconds'", 
			"'usec'", "'microsecond'", "'microseconds'", "'true'", "'false'", "'null'", 
			"'limit'", "'offset'", "'update'", "'match_recognize'", "'match_recognize_permute'", 
			"'measures'", "'define'", "'partition'", "'matches'", "'after'", "'for'", 
			"'while'", "'using'", "'merge'", "'matched'", "'expression'", "'inlined_class'", 
			"'new'", "'start'", "'context'", "'initiated'", "'terminated'", "'dataflow'", 
			"'cube'", "'rollup'", "'grouping'", "'grouping_id'", "'sets'", "'event-precedence'", 
			"'-['", "'->'", "'=>'", "'='", "'<>'", "'?'", "'('", "')'", "'['", "']'", 
			"'{'", "'}'", "':'", "','", "'=='", "'!'", "'~'", "'!='", "'/'", "'/='", 
			"'+'", "'+='", "'++'", "'-'", "'-='", "'--'", "'*'", "'*='", "'%'", "'%='", 
			"'>='", "'>'", "'<='", "'<'", "'^'", "'^='", "'|'", "'|='", "'||'", "'&'", 
			"'&='", "'&&'", "';'", "'.'", "'\u18FF'", "'\u18FE'", "'\u18FD'", "'\\'", 
			"'`'", "'@'", "'#'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "CREATE", "WINDOW", "IN_SET", "BETWEEN", "LIKE", "REGEXP", "ESCAPE", 
			"OR_EXPR", "AND_EXPR", "NOT_EXPR", "EVERY_EXPR", "EVERY_DISTINCT_EXPR", 
			"WHERE", "AS", "SUM", "AVG", "MAX", "MIN", "COALESCE", "MEDIAN", "STDDEV", 
			"AVEDEV", "COUNT", "SELECT", "CASE", "ELSE", "WHEN", "THEN", "END", "FROM", 
			"OUTER", "INNER", "JOIN", "LEFT", "RIGHT", "FULL", "ON", "IS", "BY", 
			"GROUP", "HAVING", "DISTINCT", "ALL", "ANY", "SOME", "OUTPUT", "EVENTS", 
			"FIRST", "LAST", "INSERT", "INTO", "VALUES", "ORDER", "ASC", "DESC", 
			"RSTREAM", "ISTREAM", "IRSTREAM", "SCHEMA", "UNIDIRECTIONAL", "RETAINUNION", 
			"RETAININTERSECTION", "PATTERN", "SQL", "METADATASQL", "PREVIOUS", "PREVIOUSTAIL", 
			"PREVIOUSCOUNT", "PREVIOUSWINDOW", "PRIOR", "EXISTS", "WEEKDAY", "LW", 
			"INSTANCEOF", "TYPEOF", "CAST", "CURRENT_TIMESTAMP", "DELETE", "SNAPSHOT", 
			"SET", "VARIABLE", "TABLE", "UNTIL", "AT", "INDEX", "TIMEPERIOD_YEAR", 
			"TIMEPERIOD_YEARS", "TIMEPERIOD_MONTH", "TIMEPERIOD_MONTHS", "TIMEPERIOD_WEEK", 
			"TIMEPERIOD_WEEKS", "TIMEPERIOD_DAY", "TIMEPERIOD_DAYS", "TIMEPERIOD_HOUR", 
			"TIMEPERIOD_HOURS", "TIMEPERIOD_MINUTE", "TIMEPERIOD_MINUTES", "TIMEPERIOD_SEC", 
			"TIMEPERIOD_SECOND", "TIMEPERIOD_SECONDS", "TIMEPERIOD_MILLISEC", "TIMEPERIOD_MILLISECOND", 
			"TIMEPERIOD_MILLISECONDS", "TIMEPERIOD_MICROSEC", "TIMEPERIOD_MICROSECOND", 
			"TIMEPERIOD_MICROSECONDS", "BOOLEAN_TRUE", "BOOLEAN_FALSE", "VALUE_NULL", 
			"ROW_LIMIT_EXPR", "OFFSET", "UPDATE", "MATCH_RECOGNIZE", "MATCH_RECOGNIZE_PERMUTE", 
			"MEASURES", "DEFINE", "PARTITION", "MATCHES", "AFTER", "FOR", "WHILE", 
			"USING", "MERGE", "MATCHED", "EXPRESSIONDECL", "CLASSDECL", "NEWKW", 
			"START", "CONTEXT", "INITIATED", "TERMINATED", "DATAFLOW", "CUBE", "ROLLUP", 
			"GROUPING", "GROUPING_ID", "SETS", "EVENTPRECEDENCE", "FOLLOWMAX_BEGIN", 
			"FOLLOWED_BY", "GOES", "EQUALS", "SQL_NE", "QUESTION", "LPAREN", "RPAREN", 
			"LBRACK", "RBRACK", "LCURLY", "RCURLY", "COLON", "COMMA", "EQUAL", "LNOT", 
			"BNOT", "NOT_EQUAL", "DIV", "DIV_ASSIGN", "PLUS", "PLUS_ASSIGN", "INC", 
			"MINUS", "MINUS_ASSIGN", "DEC", "STAR", "STAR_ASSIGN", "MOD", "MOD_ASSIGN", 
			"GE", "GT", "LE", "LT", "BXOR", "BXOR_ASSIGN", "BOR", "BOR_ASSIGN", "LOR", 
			"BAND", "BAND_ASSIGN", "LAND", "SEMI", "DOT", "NUM_LONG", "NUM_DOUBLE", 
			"NUM_FLOAT", "ESCAPECHAR", "ESCAPEBACKTICK", "ATCHAR", "HASHCHAR", "WS", 
			"SL_COMMENT", "ML_COMMENT", "TICKED_STRING_LITERAL", "QUOTED_STRING_LITERAL", 
			"STRING_LITERAL", "TRIPLEQUOTE", "IDENT", "IntegerLiteral", "FloatingPointLiteral"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
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
		parserTokenParaphases.put(CLASSDECL, "'inlined_class'");
		parserTokenParaphases.put(NEWKW, "'new'");
		parserTokenParaphases.put(DATAFLOW, "'dataflow'");
		parserTokenParaphases.put(VALUES, "'values'");
		parserTokenParaphases.put(CUBE, "'cube'");
		parserTokenParaphases.put(ROLLUP, "'rollup'");
		parserTokenParaphases.put(GROUPING, "'grouping'");
		parserTokenParaphases.put(GROUPING_ID, "'grouping_id'");
		parserTokenParaphases.put(SETS, "'sets'");
		parserTokenParaphases.put(EVENTPRECEDENCE, "'event-precedence'");

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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\u00c9\u0766\b\1\4"+
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
		"\4\u00e8\t\u00e8\4\u00e9\t\u00e9\4\u00ea\t\u00ea\4\u00eb\t\u00eb\4\u00ec"+
		"\t\u00ec\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\4\3"+
		"\4\3\4\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\6\3\6\3\6\3\6\3\6\3\7\3\7\3\7"+
		"\3\7\3\7\3\7\3\7\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\t\3\t\3\t\3\n\3\n\3\n\3"+
		"\n\3\13\3\13\3\13\3\13\3\f\3\f\3\f\3\f\3\f\3\f\3\r\3\r\3\r\3\r\3\r\3\r"+
		"\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\16\3\16\3\16\3\16\3\16\3\16\3\17"+
		"\3\17\3\17\3\20\3\20\3\20\3\20\3\21\3\21\3\21\3\21\3\22\3\22\3\22\3\22"+
		"\3\23\3\23\3\23\3\23\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\25"+
		"\3\25\3\25\3\25\3\25\3\25\3\25\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\27"+
		"\3\27\3\27\3\27\3\27\3\27\3\27\3\30\3\30\3\30\3\30\3\30\3\30\3\31\3\31"+
		"\3\31\3\31\3\31\3\31\3\31\3\32\3\32\3\32\3\32\3\32\3\33\3\33\3\33\3\33"+
		"\3\33\3\34\3\34\3\34\3\34\3\34\3\35\3\35\3\35\3\35\3\35\3\36\3\36\3\36"+
		"\3\36\3\37\3\37\3\37\3\37\3\37\3 \3 \3 \3 \3 \3 \3!\3!\3!\3!\3!\3!\3\""+
		"\3\"\3\"\3\"\3\"\3#\3#\3#\3#\3#\3$\3$\3$\3$\3$\3$\3%\3%\3%\3%\3%\3&\3"+
		"&\3&\3\'\3\'\3\'\3(\3(\3(\3)\3)\3)\3)\3)\3)\3*\3*\3*\3*\3*\3*\3*\3+\3"+
		"+\3+\3+\3+\3+\3+\3+\3+\3,\3,\3,\3,\3-\3-\3-\3-\3.\3.\3.\3.\3.\3/\3/\3"+
		"/\3/\3/\3/\3/\3\60\3\60\3\60\3\60\3\60\3\60\3\60\3\61\3\61\3\61\3\61\3"+
		"\61\3\61\3\62\3\62\3\62\3\62\3\62\3\63\3\63\3\63\3\63\3\63\3\63\3\63\3"+
		"\64\3\64\3\64\3\64\3\64\3\65\3\65\3\65\3\65\3\65\3\65\3\65\3\66\3\66\3"+
		"\66\3\66\3\66\3\66\3\67\3\67\3\67\3\67\38\38\38\38\38\39\39\39\39\39\3"+
		"9\39\39\3:\3:\3:\3:\3:\3:\3:\3:\3;\3;\3;\3;\3;\3;\3;\3;\3;\3<\3<\3<\3"+
		"<\3<\3<\3<\3=\3=\3=\3=\3=\3=\3=\3=\3=\3=\3=\3=\3=\3=\3=\3>\3>\3>\3>\3"+
		">\3>\3>\3>\3>\3>\3>\3>\3>\3?\3?\3?\3?\3?\3?\3?\3?\3?\3?\3?\3?\3?\3?\3"+
		"?\3?\3?\3?\3?\3?\3@\3@\3@\3@\3@\3@\3@\3@\3A\3A\3A\3A\3B\3B\3B\3B\3B\3"+
		"B\3B\3B\3B\3B\3B\3B\3C\3C\3C\3C\3C\3D\3D\3D\3D\3D\3D\3D\3D\3D\3E\3E\3"+
		"E\3E\3E\3E\3E\3E\3E\3E\3F\3F\3F\3F\3F\3F\3F\3F\3F\3F\3F\3G\3G\3G\3G\3"+
		"G\3G\3H\3H\3H\3H\3H\3H\3H\3I\3I\3I\3I\3I\3I\3I\3I\3J\3J\3J\3J\3J\3J\3"+
		"J\3J\3J\3J\3J\3J\3K\3K\3K\3K\3K\3K\3K\3K\3K\3K\3K\3L\3L\3L\3L\3L\3L\3"+
		"L\3M\3M\3M\3M\3M\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3"+
		"N\3O\3O\3O\3O\3O\3O\3O\3P\3P\3P\3P\3P\3P\3P\3P\3P\3Q\3Q\3Q\3Q\3R\3R\3"+
		"R\3R\3R\3R\3R\3R\3R\3S\3S\3S\3S\3S\3S\3T\3T\3T\3T\3T\3T\3U\3U\3U\3V\3"+
		"V\3V\3V\3V\3V\3W\3W\3W\3W\3W\3X\3X\3X\3X\3X\3X\3Y\3Y\3Y\3Y\3Y\3Y\3Z\3"+
		"Z\3Z\3Z\3Z\3Z\3Z\3[\3[\3[\3[\3[\3\\\3\\\3\\\3\\\3\\\3\\\3]\3]\3]\3]\3"+
		"^\3^\3^\3^\3^\3_\3_\3_\3_\3_\3`\3`\3`\3`\3`\3`\3a\3a\3a\3a\3a\3a\3a\3"+
		"b\3b\3b\3b\3b\3b\3b\3b\3c\3c\3c\3c\3d\3d\3d\3d\3d\3d\3d\3e\3e\3e\3e\3"+
		"e\3e\3e\3e\3f\3f\3f\3f\3f\3g\3g\3g\3g\3g\3g\3g\3g\3g\3g\3g\3g\3h\3h\3"+
		"h\3h\3h\3h\3h\3h\3h\3h\3h\3h\3h\3i\3i\3i\3i\3i\3j\3j\3j\3j\3j\3j\3j\3"+
		"j\3j\3j\3j\3j\3k\3k\3k\3k\3k\3k\3k\3k\3k\3k\3k\3k\3k\3l\3l\3l\3l\3l\3"+
		"m\3m\3m\3m\3m\3m\3n\3n\3n\3n\3n\3o\3o\3o\3o\3o\3o\3p\3p\3p\3p\3p\3p\3"+
		"p\3q\3q\3q\3q\3q\3q\3q\3r\3r\3r\3r\3r\3r\3r\3r\3r\3r\3r\3r\3r\3r\3r\3"+
		"r\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3"+
		"s\3s\3t\3t\3t\3t\3t\3t\3t\3t\3t\3u\3u\3u\3u\3u\3u\3u\3v\3v\3v\3v\3v\3"+
		"v\3v\3v\3v\3v\3w\3w\3w\3w\3w\3w\3w\3w\3x\3x\3x\3x\3x\3x\3y\3y\3y\3y\3"+
		"z\3z\3z\3z\3z\3z\3{\3{\3{\3{\3{\3{\3|\3|\3|\3|\3|\3|\3}\3}\3}\3}\3}\3"+
		"}\3}\3}\3~\3~\3~\3~\3~\3~\3~\3~\3~\3~\3~\3\177\3\177\3\177\3\177\3\177"+
		"\3\177\3\177\3\177\3\177\3\177\3\177\3\177\3\177\3\177\3\u0080\3\u0080"+
		"\3\u0080\3\u0080\3\u0081\3\u0081\3\u0081\3\u0081\3\u0081\3\u0081\3\u0082"+
		"\3\u0082\3\u0082\3\u0082\3\u0082\3\u0082\3\u0082\3\u0082\3\u0083\3\u0083"+
		"\3\u0083\3\u0083\3\u0083\3\u0083\3\u0083\3\u0083\3\u0083\3\u0083\3\u0084"+
		"\3\u0084\3\u0084\3\u0084\3\u0084\3\u0084\3\u0084\3\u0084\3\u0084\3\u0084"+
		"\3\u0084\3\u0085\3\u0085\3\u0085\3\u0085\3\u0085\3\u0085\3\u0085\3\u0085"+
		"\3\u0085\3\u0086\3\u0086\3\u0086\3\u0086\3\u0086\3\u0087\3\u0087\3\u0087"+
		"\3\u0087\3\u0087\3\u0087\3\u0087\3\u0088\3\u0088\3\u0088\3\u0088\3\u0088"+
		"\3\u0088\3\u0088\3\u0088\3\u0088\3\u0089\3\u0089\3\u0089\3\u0089\3\u0089"+
		"\3\u0089\3\u0089\3\u0089\3\u0089\3\u0089\3\u0089\3\u0089\3\u008a\3\u008a"+
		"\3\u008a\3\u008a\3\u008a\3\u008b\3\u008b\3\u008b\3\u008b\3\u008b\3\u008b"+
		"\3\u008b\3\u008b\3\u008b\3\u008b\3\u008b\3\u008b\3\u008b\3\u008b\3\u008b"+
		"\3\u008b\3\u008b\3\u008c\3\u008c\3\u008c\3\u008d\3\u008d\3\u008d\3\u008e"+
		"\3\u008e\3\u008e\3\u008f\3\u008f\3\u0090\3\u0090\3\u0090\3\u0091\3\u0091"+
		"\3\u0092\3\u0092\3\u0093\3\u0093\3\u0094\3\u0094\3\u0095\3\u0095\3\u0096"+
		"\3\u0096\3\u0097\3\u0097\3\u0098\3\u0098\3\u0099\3\u0099\3\u009a\3\u009a"+
		"\3\u009a\3\u009b\3\u009b\3\u009c\3\u009c\3\u009d\3\u009d\3\u009d\3\u009e"+
		"\3\u009e\3\u009f\3\u009f\3\u009f\3\u00a0\3\u00a0\3\u00a1\3\u00a1\3\u00a1"+
		"\3\u00a2\3\u00a2\3\u00a2\3\u00a3\3\u00a3\3\u00a4\3\u00a4\3\u00a4\3\u00a5"+
		"\3\u00a5\3\u00a5\3\u00a6\3\u00a6\3\u00a7\3\u00a7\3\u00a7\3\u00a8\3\u00a8"+
		"\3\u00a9\3\u00a9\3\u00a9\3\u00aa\3\u00aa\3\u00aa\3\u00ab\3\u00ab\3\u00ac"+
		"\3\u00ac\3\u00ac\3\u00ad\3\u00ad\3\u00ae\3\u00ae\3\u00af\3\u00af\3\u00af"+
		"\3\u00b0\3\u00b0\3\u00b1\3\u00b1\3\u00b1\3\u00b2\3\u00b2\3\u00b2\3\u00b3"+
		"\3\u00b3\3\u00b4\3\u00b4\3\u00b4\3\u00b5\3\u00b5\3\u00b5\3\u00b6\3\u00b6"+
		"\3\u00b7\3\u00b7\3\u00b8\3\u00b8\3\u00b9\3\u00b9\3\u00ba\3\u00ba\3\u00bb"+
		"\3\u00bb\3\u00bc\3\u00bc\3\u00bd\3\u00bd\3\u00be\3\u00be\3\u00bf\6\u00bf"+
		"\u0638\n\u00bf\r\u00bf\16\u00bf\u0639\3\u00bf\3\u00bf\3\u00c0\3\u00c0"+
		"\3\u00c0\3\u00c0\7\u00c0\u0642\n\u00c0\f\u00c0\16\u00c0\u0645\13\u00c0"+
		"\3\u00c0\3\u00c0\3\u00c0\5\u00c0\u064a\n\u00c0\5\u00c0\u064c\n\u00c0\3"+
		"\u00c0\3\u00c0\3\u00c1\3\u00c1\3\u00c1\3\u00c1\7\u00c1\u0654\n\u00c1\f"+
		"\u00c1\16\u00c1\u0657\13\u00c1\3\u00c1\3\u00c1\3\u00c1\3\u00c1\3\u00c1"+
		"\3\u00c2\3\u00c2\3\u00c2\7\u00c2\u0661\n\u00c2\f\u00c2\16\u00c2\u0664"+
		"\13\u00c2\3\u00c2\3\u00c2\3\u00c3\3\u00c3\3\u00c3\7\u00c3\u066b\n\u00c3"+
		"\f\u00c3\16\u00c3\u066e\13\u00c3\3\u00c3\3\u00c3\3\u00c4\3\u00c4\3\u00c4"+
		"\7\u00c4\u0675\n\u00c4\f\u00c4\16\u00c4\u0678\13\u00c4\3\u00c4\3\u00c4"+
		"\3\u00c5\3\u00c5\3\u00c5\3\u00c5\3\u00c6\3\u00c6\3\u00c6\3\u00c6\3\u00c6"+
		"\5\u00c6\u0685\n\u00c6\3\u00c7\3\u00c7\7\u00c7\u0689\n\u00c7\f\u00c7\16"+
		"\u00c7\u068c\13\u00c7\3\u00c8\3\u00c8\3\u00c8\3\u00c8\5\u00c8\u0692\n"+
		"\u00c8\3\u00c9\3\u00c9\5\u00c9\u0696\n\u00c9\3\u00ca\3\u00ca\3\u00ca\3"+
		"\u00ca\3\u00ca\3\u00ca\3\u00ca\3\u00ca\3\u00ca\5\u00ca\u06a1\n\u00ca\3"+
		"\u00cb\3\u00cb\3\u00cb\3\u00cb\3\u00cb\3\u00cb\3\u00cb\3\u00cc\3\u00cc"+
		"\5\u00cc\u06ac\n\u00cc\3\u00cd\3\u00cd\5\u00cd\u06b0\n\u00cd\3\u00ce\3"+
		"\u00ce\5\u00ce\u06b4\n\u00ce\3\u00cf\3\u00cf\5\u00cf\u06b8\n\u00cf\3\u00d0"+
		"\3\u00d0\3\u00d1\3\u00d1\7\u00d1\u06be\n\u00d1\f\u00d1\16\u00d1\u06c1"+
		"\13\u00d1\3\u00d1\3\u00d1\5\u00d1\u06c5\n\u00d1\3\u00d1\3\u00d1\3\u00d1"+
		"\5\u00d1\u06ca\n\u00d1\5\u00d1\u06cc\n\u00d1\3\u00d2\3\u00d2\7\u00d2\u06d0"+
		"\n\u00d2\f\u00d2\16\u00d2\u06d3\13\u00d2\3\u00d2\5\u00d2\u06d6\n\u00d2"+
		"\3\u00d3\3\u00d3\5\u00d3\u06da\n\u00d3\3\u00d4\3\u00d4\3\u00d5\3\u00d5"+
		"\5\u00d5\u06e0\n\u00d5\3\u00d6\6\u00d6\u06e3\n\u00d6\r\u00d6\16\u00d6"+
		"\u06e4\3\u00d7\3\u00d7\3\u00d7\3\u00d7\3\u00d8\3\u00d8\7\u00d8\u06ed\n"+
		"\u00d8\f\u00d8\16\u00d8\u06f0\13\u00d8\3\u00d8\5\u00d8\u06f3\n\u00d8\3"+
		"\u00d9\3\u00d9\3\u00da\3\u00da\5\u00da\u06f9\n\u00da\3\u00db\3\u00db\5"+
		"\u00db\u06fd\n\u00db\3\u00db\3\u00db\3\u00dc\3\u00dc\7\u00dc\u0703\n\u00dc"+
		"\f\u00dc\16\u00dc\u0706\13\u00dc\3\u00dc\5\u00dc\u0709\n\u00dc\3\u00dd"+
		"\3\u00dd\3\u00de\3\u00de\5\u00de\u070f\n\u00de\3\u00df\3\u00df\3\u00df"+
		"\3\u00df\3\u00e0\3\u00e0\7\u00e0\u0717\n\u00e0\f\u00e0\16\u00e0\u071a"+
		"\13\u00e0\3\u00e0\5\u00e0\u071d\n\u00e0\3\u00e1\3\u00e1\3\u00e2\3\u00e2"+
		"\5\u00e2\u0723\n\u00e2\3\u00e3\3\u00e3\3\u00e3\5\u00e3\u0728\n\u00e3\3"+
		"\u00e3\5\u00e3\u072b\n\u00e3\3\u00e3\5\u00e3\u072e\n\u00e3\3\u00e3\3\u00e3"+
		"\3\u00e3\5\u00e3\u0733\n\u00e3\3\u00e3\5\u00e3\u0736\n\u00e3\3\u00e3\3"+
		"\u00e3\3\u00e3\5\u00e3\u073b\n\u00e3\3\u00e3\3\u00e3\3\u00e3\5\u00e3\u0740"+
		"\n\u00e3\3\u00e4\3\u00e4\3\u00e4\3\u00e5\3\u00e5\3\u00e6\5\u00e6\u0748"+
		"\n\u00e6\3\u00e6\3\u00e6\3\u00e7\3\u00e7\3\u00e8\3\u00e8\3\u00e9\3\u00e9"+
		"\3\u00e9\5\u00e9\u0753\n\u00e9\3\u00ea\3\u00ea\5\u00ea\u0757\n\u00ea\3"+
		"\u00ea\3\u00ea\3\u00ea\5\u00ea\u075c\n\u00ea\3\u00ea\3\u00ea\5\u00ea\u0760"+
		"\n\u00ea\3\u00eb\3\u00eb\3\u00eb\3\u00ec\3\u00ec\3\u0655\2\u00ed\3\3\5"+
		"\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21"+
		"!\22#\23%\24\'\25)\26+\27-\30/\31\61\32\63\33\65\34\67\359\36;\37= ?!"+
		"A\"C#E$G%I&K\'M(O)Q*S+U,W-Y.[/]\60_\61a\62c\63e\64g\65i\66k\67m8o9q:s"+
		";u<w=y>{?}@\177A\u0081B\u0083C\u0085D\u0087E\u0089F\u008bG\u008dH\u008f"+
		"I\u0091J\u0093K\u0095L\u0097M\u0099N\u009bO\u009dP\u009fQ\u00a1R\u00a3"+
		"S\u00a5T\u00a7U\u00a9V\u00abW\u00adX\u00afY\u00b1Z\u00b3[\u00b5\\\u00b7"+
		"]\u00b9^\u00bb_\u00bd`\u00bfa\u00c1b\u00c3c\u00c5d\u00c7e\u00c9f\u00cb"+
		"g\u00cdh\u00cfi\u00d1j\u00d3k\u00d5l\u00d7m\u00d9n\u00dbo\u00ddp\u00df"+
		"q\u00e1r\u00e3s\u00e5t\u00e7u\u00e9v\u00ebw\u00edx\u00efy\u00f1z\u00f3"+
		"{\u00f5|\u00f7}\u00f9~\u00fb\177\u00fd\u0080\u00ff\u0081\u0101\u0082\u0103"+
		"\u0083\u0105\u0084\u0107\u0085\u0109\u0086\u010b\u0087\u010d\u0088\u010f"+
		"\u0089\u0111\u008a\u0113\u008b\u0115\u008c\u0117\u008d\u0119\u008e\u011b"+
		"\u008f\u011d\u0090\u011f\u0091\u0121\u0092\u0123\u0093\u0125\u0094\u0127"+
		"\u0095\u0129\u0096\u012b\u0097\u012d\u0098\u012f\u0099\u0131\u009a\u0133"+
		"\u009b\u0135\u009c\u0137\u009d\u0139\u009e\u013b\u009f\u013d\u00a0\u013f"+
		"\u00a1\u0141\u00a2\u0143\u00a3\u0145\u00a4\u0147\u00a5\u0149\u00a6\u014b"+
		"\u00a7\u014d\u00a8\u014f\u00a9\u0151\u00aa\u0153\u00ab\u0155\u00ac\u0157"+
		"\u00ad\u0159\u00ae\u015b\u00af\u015d\u00b0\u015f\u00b1\u0161\u00b2\u0163"+
		"\u00b3\u0165\u00b4\u0167\u00b5\u0169\u00b6\u016b\u00b7\u016d\u00b8\u016f"+
		"\u00b9\u0171\u00ba\u0173\u00bb\u0175\u00bc\u0177\u00bd\u0179\u00be\u017b"+
		"\u00bf\u017d\u00c0\u017f\u00c1\u0181\u00c2\u0183\u00c3\u0185\u00c4\u0187"+
		"\u00c5\u0189\u00c6\u018b\2\u018d\u00c7\u018f\u00c8\u0191\u00c9\u0193\2"+
		"\u0195\2\u0197\2\u0199\2\u019b\2\u019d\2\u019f\2\u01a1\2\u01a3\2\u01a5"+
		"\2\u01a7\2\u01a9\2\u01ab\2\u01ad\2\u01af\2\u01b1\2\u01b3\2\u01b5\2\u01b7"+
		"\2\u01b9\2\u01bb\2\u01bd\2\u01bf\2\u01c1\2\u01c3\2\u01c5\2\u01c7\2\u01c9"+
		"\2\u01cb\2\u01cd\2\u01cf\2\u01d1\2\u01d3\2\u01d5\2\u01d7\2\3\2\25\5\2"+
		"\13\f\16\17\"\"\4\2\f\f\17\17\4\2^^bb\4\2))^^\4\2$$^^\n\2$$))^^ddhhpp"+
		"ttvv\5\2&&aac|\6\2&&\62;aac|\4\2NNnn\3\2\63;\4\2ZZzz\5\2\62;CHch\3\2\62"+
		"9\4\2DDdd\3\2\62\63\4\2GGgg\4\2--//\6\2FFHHffhh\4\2RRrr\2\u077c\2\3\3"+
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
		"\3\2\2\2\2\u0183\3\2\2\2\2\u0185\3\2\2\2\2\u0187\3\2\2\2\2\u0189\3\2\2"+
		"\2\2\u018d\3\2\2\2\2\u018f\3\2\2\2\2\u0191\3\2\2\2\3\u01d9\3\2\2\2\5\u01e0"+
		"\3\2\2\2\7\u01e7\3\2\2\2\t\u01ea\3\2\2\2\13\u01f2\3\2\2\2\r\u01f7\3\2"+
		"\2\2\17\u01fe\3\2\2\2\21\u0205\3\2\2\2\23\u0208\3\2\2\2\25\u020c\3\2\2"+
		"\2\27\u0210\3\2\2\2\31\u0216\3\2\2\2\33\u0225\3\2\2\2\35\u022b\3\2\2\2"+
		"\37\u022e\3\2\2\2!\u0232\3\2\2\2#\u0236\3\2\2\2%\u023a\3\2\2\2\'\u023e"+
		"\3\2\2\2)\u0247\3\2\2\2+\u024e\3\2\2\2-\u0255\3\2\2\2/\u025c\3\2\2\2\61"+
		"\u0262\3\2\2\2\63\u0269\3\2\2\2\65\u026e\3\2\2\2\67\u0273\3\2\2\29\u0278"+
		"\3\2\2\2;\u027d\3\2\2\2=\u0281\3\2\2\2?\u0286\3\2\2\2A\u028c\3\2\2\2C"+
		"\u0292\3\2\2\2E\u0297\3\2\2\2G\u029c\3\2\2\2I\u02a2\3\2\2\2K\u02a7\3\2"+
		"\2\2M\u02aa\3\2\2\2O\u02ad\3\2\2\2Q\u02b0\3\2\2\2S\u02b6\3\2\2\2U\u02bd"+
		"\3\2\2\2W\u02c6\3\2\2\2Y\u02ca\3\2\2\2[\u02ce\3\2\2\2]\u02d3\3\2\2\2_"+
		"\u02da\3\2\2\2a\u02e1\3\2\2\2c\u02e7\3\2\2\2e\u02ec\3\2\2\2g\u02f3\3\2"+
		"\2\2i\u02f8\3\2\2\2k\u02ff\3\2\2\2m\u0305\3\2\2\2o\u0309\3\2\2\2q\u030e"+
		"\3\2\2\2s\u0316\3\2\2\2u\u031e\3\2\2\2w\u0327\3\2\2\2y\u032e\3\2\2\2{"+
		"\u033d\3\2\2\2}\u034a\3\2\2\2\177\u035e\3\2\2\2\u0081\u0366\3\2\2\2\u0083"+
		"\u036a\3\2\2\2\u0085\u0376\3\2\2\2\u0087\u037b\3\2\2\2\u0089\u0384\3\2"+
		"\2\2\u008b\u038e\3\2\2\2\u008d\u0399\3\2\2\2\u008f\u039f\3\2\2\2\u0091"+
		"\u03a6\3\2\2\2\u0093\u03ae\3\2\2\2\u0095\u03ba\3\2\2\2\u0097\u03c5\3\2"+
		"\2\2\u0099\u03cc\3\2\2\2\u009b\u03d1\3\2\2\2\u009d\u03e3\3\2\2\2\u009f"+
		"\u03ea\3\2\2\2\u00a1\u03f3\3\2\2\2\u00a3\u03f7\3\2\2\2\u00a5\u0400\3\2"+
		"\2\2\u00a7\u0406\3\2\2\2\u00a9\u040c\3\2\2\2\u00ab\u040f\3\2\2\2\u00ad"+
		"\u0415\3\2\2\2\u00af\u041a\3\2\2\2\u00b1\u0420\3\2\2\2\u00b3\u0426\3\2"+
		"\2\2\u00b5\u042d\3\2\2\2\u00b7\u0432\3\2\2\2\u00b9\u0438\3\2\2\2\u00bb"+
		"\u043c\3\2\2\2\u00bd\u0441\3\2\2\2\u00bf\u0446\3\2\2\2\u00c1\u044c\3\2"+
		"\2\2\u00c3\u0453\3\2\2\2\u00c5\u045b\3\2\2\2\u00c7\u045f\3\2\2\2\u00c9"+
		"\u0466\3\2\2\2\u00cb\u046e\3\2\2\2\u00cd\u0473\3\2\2\2\u00cf\u047f\3\2"+
		"\2\2\u00d1\u048c\3\2\2\2\u00d3\u0491\3\2\2\2\u00d5\u049d\3\2\2\2\u00d7"+
		"\u04aa\3\2\2\2\u00d9\u04af\3\2\2\2\u00db\u04b5\3\2\2\2\u00dd\u04ba\3\2"+
		"\2\2\u00df\u04c0\3\2\2\2\u00e1\u04c7\3\2\2\2\u00e3\u04ce\3\2\2\2\u00e5"+
		"\u04de\3\2\2\2\u00e7\u04f6\3\2\2\2\u00e9\u04ff\3\2\2\2\u00eb\u0506\3\2"+
		"\2\2\u00ed\u0510\3\2\2\2\u00ef\u0518\3\2\2\2\u00f1\u051e\3\2\2\2\u00f3"+
		"\u0522\3\2\2\2\u00f5\u0528\3\2\2\2\u00f7\u052e\3\2\2\2\u00f9\u0534\3\2"+
		"\2\2\u00fb\u053c\3\2\2\2\u00fd\u0547\3\2\2\2\u00ff\u0555\3\2\2\2\u0101"+
		"\u0559\3\2\2\2\u0103\u055f\3\2\2\2\u0105\u0567\3\2\2\2\u0107\u0571\3\2"+
		"\2\2\u0109\u057c\3\2\2\2\u010b\u0585\3\2\2\2\u010d\u058a\3\2\2\2\u010f"+
		"\u0591\3\2\2\2\u0111\u059a\3\2\2\2\u0113\u05a6\3\2\2\2\u0115\u05ab\3\2"+
		"\2\2\u0117\u05bc\3\2\2\2\u0119\u05bf\3\2\2\2\u011b\u05c2\3\2\2\2\u011d"+
		"\u05c5\3\2\2\2\u011f\u05c7\3\2\2\2\u0121\u05ca\3\2\2\2\u0123\u05cc\3\2"+
		"\2\2\u0125\u05ce\3\2\2\2\u0127\u05d0\3\2\2\2\u0129\u05d2\3\2\2\2\u012b"+
		"\u05d4\3\2\2\2\u012d\u05d6\3\2\2\2\u012f\u05d8\3\2\2\2\u0131\u05da\3\2"+
		"\2\2\u0133\u05dc\3\2\2\2\u0135\u05df\3\2\2\2\u0137\u05e1\3\2\2\2\u0139"+
		"\u05e3\3\2\2\2\u013b\u05e6\3\2\2\2\u013d\u05e8\3\2\2\2\u013f\u05eb\3\2"+
		"\2\2\u0141\u05ed\3\2\2\2\u0143\u05f0\3\2\2\2\u0145\u05f3\3\2\2\2\u0147"+
		"\u05f5\3\2\2\2\u0149\u05f8\3\2\2\2\u014b\u05fb\3\2\2\2\u014d\u05fd\3\2"+
		"\2\2\u014f\u0600\3\2\2\2\u0151\u0602\3\2\2\2\u0153\u0605\3\2\2\2\u0155"+
		"\u0608\3\2\2\2\u0157\u060a\3\2\2\2\u0159\u060d\3\2\2\2\u015b\u060f\3\2"+
		"\2\2\u015d\u0611\3\2\2\2\u015f\u0614\3\2\2\2\u0161\u0616\3\2\2\2\u0163"+
		"\u0619\3\2\2\2\u0165\u061c\3\2\2\2\u0167\u061e\3\2\2\2\u0169\u0621\3\2"+
		"\2\2\u016b\u0624\3\2\2\2\u016d\u0626\3\2\2\2\u016f\u0628\3\2\2\2\u0171"+
		"\u062a\3\2\2\2\u0173\u062c\3\2\2\2\u0175\u062e\3\2\2\2\u0177\u0630\3\2"+
		"\2\2\u0179\u0632\3\2\2\2\u017b\u0634\3\2\2\2\u017d\u0637\3\2\2\2\u017f"+
		"\u063d\3\2\2\2\u0181\u064f\3\2\2\2\u0183\u065d\3\2\2\2\u0185\u0667\3\2"+
		"\2\2\u0187\u0671\3\2\2\2\u0189\u067b\3\2\2\2\u018b\u067f\3\2\2\2\u018d"+
		"\u0686\3\2\2\2\u018f\u0691\3\2\2\2\u0191\u0695\3\2\2\2\u0193\u06a0\3\2"+
		"\2\2\u0195\u06a2\3\2\2\2\u0197\u06a9\3\2\2\2\u0199\u06ad\3\2\2\2\u019b"+
		"\u06b1\3\2\2\2\u019d\u06b5\3\2\2\2\u019f\u06b9\3\2\2\2\u01a1\u06cb\3\2"+
		"\2\2\u01a3\u06cd\3\2\2\2\u01a5\u06d9\3\2\2\2\u01a7\u06db\3\2\2\2\u01a9"+
		"\u06df\3\2\2\2\u01ab\u06e2\3\2\2\2\u01ad\u06e6\3\2\2\2\u01af\u06ea\3\2"+
		"\2\2\u01b1\u06f4\3\2\2\2\u01b3\u06f8\3\2\2\2\u01b5\u06fa\3\2\2\2\u01b7"+
		"\u0700\3\2\2\2\u01b9\u070a\3\2\2\2\u01bb\u070e\3\2\2\2\u01bd\u0710\3\2"+
		"\2\2\u01bf\u0714\3\2\2\2\u01c1\u071e\3\2\2\2\u01c3\u0722\3\2\2\2\u01c5"+
		"\u073f\3\2\2\2\u01c7\u0741\3\2\2\2\u01c9\u0744\3\2\2\2\u01cb\u0747\3\2"+
		"\2\2\u01cd\u074b\3\2\2\2\u01cf\u074d\3\2\2\2\u01d1\u074f\3\2\2\2\u01d3"+
		"\u075f\3\2\2\2\u01d5\u0761\3\2\2\2\u01d7\u0764\3\2\2\2\u01d9\u01da\7e"+
		"\2\2\u01da\u01db\7t\2\2\u01db\u01dc\7g\2\2\u01dc\u01dd\7c\2\2\u01dd\u01de"+
		"\7v\2\2\u01de\u01df\7g\2\2\u01df\4\3\2\2\2\u01e0\u01e1\7y\2\2\u01e1\u01e2"+
		"\7k\2\2\u01e2\u01e3\7p\2\2\u01e3\u01e4\7f\2\2\u01e4\u01e5\7q\2\2\u01e5"+
		"\u01e6\7y\2\2\u01e6\6\3\2\2\2\u01e7\u01e8\7k\2\2\u01e8\u01e9\7p\2\2\u01e9"+
		"\b\3\2\2\2\u01ea\u01eb\7d\2\2\u01eb\u01ec\7g\2\2\u01ec\u01ed\7v\2\2\u01ed"+
		"\u01ee\7y\2\2\u01ee\u01ef\7g\2\2\u01ef\u01f0\7g\2\2\u01f0\u01f1\7p\2\2"+
		"\u01f1\n\3\2\2\2\u01f2\u01f3\7n\2\2\u01f3\u01f4\7k\2\2\u01f4\u01f5\7m"+
		"\2\2\u01f5\u01f6\7g\2\2\u01f6\f\3\2\2\2\u01f7\u01f8\7t\2\2\u01f8\u01f9"+
		"\7g\2\2\u01f9\u01fa\7i\2\2\u01fa\u01fb\7g\2\2\u01fb\u01fc\7z\2\2\u01fc"+
		"\u01fd\7r\2\2\u01fd\16\3\2\2\2\u01fe\u01ff\7g\2\2\u01ff\u0200\7u\2\2\u0200"+
		"\u0201\7e\2\2\u0201\u0202\7c\2\2\u0202\u0203\7r\2\2\u0203\u0204\7g\2\2"+
		"\u0204\20\3\2\2\2\u0205\u0206\7q\2\2\u0206\u0207\7t\2\2\u0207\22\3\2\2"+
		"\2\u0208\u0209\7c\2\2\u0209\u020a\7p\2\2\u020a\u020b\7f\2\2\u020b\24\3"+
		"\2\2\2\u020c\u020d\7p\2\2\u020d\u020e\7q\2\2\u020e\u020f\7v\2\2\u020f"+
		"\26\3\2\2\2\u0210\u0211\7g\2\2\u0211\u0212\7x\2\2\u0212\u0213\7g\2\2\u0213"+
		"\u0214\7t\2\2\u0214\u0215\7{\2\2\u0215\30\3\2\2\2\u0216\u0217\7g\2\2\u0217"+
		"\u0218\7x\2\2\u0218\u0219\7g\2\2\u0219\u021a\7t\2\2\u021a\u021b\7{\2\2"+
		"\u021b\u021c\7/\2\2\u021c\u021d\7f\2\2\u021d\u021e\7k\2\2\u021e\u021f"+
		"\7u\2\2\u021f\u0220\7v\2\2\u0220\u0221\7k\2\2\u0221\u0222\7p\2\2\u0222"+
		"\u0223\7e\2\2\u0223\u0224\7v\2\2\u0224\32\3\2\2\2\u0225\u0226\7y\2\2\u0226"+
		"\u0227\7j\2\2\u0227\u0228\7g\2\2\u0228\u0229\7t\2\2\u0229\u022a\7g\2\2"+
		"\u022a\34\3\2\2\2\u022b\u022c\7c\2\2\u022c\u022d\7u\2\2\u022d\36\3\2\2"+
		"\2\u022e\u022f\7u\2\2\u022f\u0230\7w\2\2\u0230\u0231\7o\2\2\u0231 \3\2"+
		"\2\2\u0232\u0233\7c\2\2\u0233\u0234\7x\2\2\u0234\u0235\7i\2\2\u0235\""+
		"\3\2\2\2\u0236\u0237\7o\2\2\u0237\u0238\7c\2\2\u0238\u0239\7z\2\2\u0239"+
		"$\3\2\2\2\u023a\u023b\7o\2\2\u023b\u023c\7k\2\2\u023c\u023d\7p\2\2\u023d"+
		"&\3\2\2\2\u023e\u023f\7e\2\2\u023f\u0240\7q\2\2\u0240\u0241\7c\2\2\u0241"+
		"\u0242\7n\2\2\u0242\u0243\7g\2\2\u0243\u0244\7u\2\2\u0244\u0245\7e\2\2"+
		"\u0245\u0246\7g\2\2\u0246(\3\2\2\2\u0247\u0248\7o\2\2\u0248\u0249\7g\2"+
		"\2\u0249\u024a\7f\2\2\u024a\u024b\7k\2\2\u024b\u024c\7c\2\2\u024c\u024d"+
		"\7p\2\2\u024d*\3\2\2\2\u024e\u024f\7u\2\2\u024f\u0250\7v\2\2\u0250\u0251"+
		"\7f\2\2\u0251\u0252\7f\2\2\u0252\u0253\7g\2\2\u0253\u0254\7x\2\2\u0254"+
		",\3\2\2\2\u0255\u0256\7c\2\2\u0256\u0257\7x\2\2\u0257\u0258\7g\2\2\u0258"+
		"\u0259\7f\2\2\u0259\u025a\7g\2\2\u025a\u025b\7x\2\2\u025b.\3\2\2\2\u025c"+
		"\u025d\7e\2\2\u025d\u025e\7q\2\2\u025e\u025f\7w\2\2\u025f\u0260\7p\2\2"+
		"\u0260\u0261\7v\2\2\u0261\60\3\2\2\2\u0262\u0263\7u\2\2\u0263\u0264\7"+
		"g\2\2\u0264\u0265\7n\2\2\u0265\u0266\7g\2\2\u0266\u0267\7e\2\2\u0267\u0268"+
		"\7v\2\2\u0268\62\3\2\2\2\u0269\u026a\7e\2\2\u026a\u026b\7c\2\2\u026b\u026c"+
		"\7u\2\2\u026c\u026d\7g\2\2\u026d\64\3\2\2\2\u026e\u026f\7g\2\2\u026f\u0270"+
		"\7n\2\2\u0270\u0271\7u\2\2\u0271\u0272\7g\2\2\u0272\66\3\2\2\2\u0273\u0274"+
		"\7y\2\2\u0274\u0275\7j\2\2\u0275\u0276\7g\2\2\u0276\u0277\7p\2\2\u0277"+
		"8\3\2\2\2\u0278\u0279\7v\2\2\u0279\u027a\7j\2\2\u027a\u027b\7g\2\2\u027b"+
		"\u027c\7p\2\2\u027c:\3\2\2\2\u027d\u027e\7g\2\2\u027e\u027f\7p\2\2\u027f"+
		"\u0280\7f\2\2\u0280<\3\2\2\2\u0281\u0282\7h\2\2\u0282\u0283\7t\2\2\u0283"+
		"\u0284\7q\2\2\u0284\u0285\7o\2\2\u0285>\3\2\2\2\u0286\u0287\7q\2\2\u0287"+
		"\u0288\7w\2\2\u0288\u0289\7v\2\2\u0289\u028a\7g\2\2\u028a\u028b\7t\2\2"+
		"\u028b@\3\2\2\2\u028c\u028d\7k\2\2\u028d\u028e\7p\2\2\u028e\u028f\7p\2"+
		"\2\u028f\u0290\7g\2\2\u0290\u0291\7t\2\2\u0291B\3\2\2\2\u0292\u0293\7"+
		"l\2\2\u0293\u0294\7q\2\2\u0294\u0295\7k\2\2\u0295\u0296\7p\2\2\u0296D"+
		"\3\2\2\2\u0297\u0298\7n\2\2\u0298\u0299\7g\2\2\u0299\u029a\7h\2\2\u029a"+
		"\u029b\7v\2\2\u029bF\3\2\2\2\u029c\u029d\7t\2\2\u029d\u029e\7k\2\2\u029e"+
		"\u029f\7i\2\2\u029f\u02a0\7j\2\2\u02a0\u02a1\7v\2\2\u02a1H\3\2\2\2\u02a2"+
		"\u02a3\7h\2\2\u02a3\u02a4\7w\2\2\u02a4\u02a5\7n\2\2\u02a5\u02a6\7n\2\2"+
		"\u02a6J\3\2\2\2\u02a7\u02a8\7q\2\2\u02a8\u02a9\7p\2\2\u02a9L\3\2\2\2\u02aa"+
		"\u02ab\7k\2\2\u02ab\u02ac\7u\2\2\u02acN\3\2\2\2\u02ad\u02ae\7d\2\2\u02ae"+
		"\u02af\7{\2\2\u02afP\3\2\2\2\u02b0\u02b1\7i\2\2\u02b1\u02b2\7t\2\2\u02b2"+
		"\u02b3\7q\2\2\u02b3\u02b4\7w\2\2\u02b4\u02b5\7r\2\2\u02b5R\3\2\2\2\u02b6"+
		"\u02b7\7j\2\2\u02b7\u02b8\7c\2\2\u02b8\u02b9\7x\2\2\u02b9\u02ba\7k\2\2"+
		"\u02ba\u02bb\7p\2\2\u02bb\u02bc\7i\2\2\u02bcT\3\2\2\2\u02bd\u02be\7f\2"+
		"\2\u02be\u02bf\7k\2\2\u02bf\u02c0\7u\2\2\u02c0\u02c1\7v\2\2\u02c1\u02c2"+
		"\7k\2\2\u02c2\u02c3\7p\2\2\u02c3\u02c4\7e\2\2\u02c4\u02c5\7v\2\2\u02c5"+
		"V\3\2\2\2\u02c6\u02c7\7c\2\2\u02c7\u02c8\7n\2\2\u02c8\u02c9\7n\2\2\u02c9"+
		"X\3\2\2\2\u02ca\u02cb\7c\2\2\u02cb\u02cc\7p\2\2\u02cc\u02cd\7{\2\2\u02cd"+
		"Z\3\2\2\2\u02ce\u02cf\7u\2\2\u02cf\u02d0\7q\2\2\u02d0\u02d1\7o\2\2\u02d1"+
		"\u02d2\7g\2\2\u02d2\\\3\2\2\2\u02d3\u02d4\7q\2\2\u02d4\u02d5\7w\2\2\u02d5"+
		"\u02d6\7v\2\2\u02d6\u02d7\7r\2\2\u02d7\u02d8\7w\2\2\u02d8\u02d9\7v\2\2"+
		"\u02d9^\3\2\2\2\u02da\u02db\7g\2\2\u02db\u02dc\7x\2\2\u02dc\u02dd\7g\2"+
		"\2\u02dd\u02de\7p\2\2\u02de\u02df\7v\2\2\u02df\u02e0\7u\2\2\u02e0`\3\2"+
		"\2\2\u02e1\u02e2\7h\2\2\u02e2\u02e3\7k\2\2\u02e3\u02e4\7t\2\2\u02e4\u02e5"+
		"\7u\2\2\u02e5\u02e6\7v\2\2\u02e6b\3\2\2\2\u02e7\u02e8\7n\2\2\u02e8\u02e9"+
		"\7c\2\2\u02e9\u02ea\7u\2\2\u02ea\u02eb\7v\2\2\u02ebd\3\2\2\2\u02ec\u02ed"+
		"\7k\2\2\u02ed\u02ee\7p\2\2\u02ee\u02ef\7u\2\2\u02ef\u02f0\7g\2\2\u02f0"+
		"\u02f1\7t\2\2\u02f1\u02f2\7v\2\2\u02f2f\3\2\2\2\u02f3\u02f4\7k\2\2\u02f4"+
		"\u02f5\7p\2\2\u02f5\u02f6\7v\2\2\u02f6\u02f7\7q\2\2\u02f7h\3\2\2\2\u02f8"+
		"\u02f9\7x\2\2\u02f9\u02fa\7c\2\2\u02fa\u02fb\7n\2\2\u02fb\u02fc\7w\2\2"+
		"\u02fc\u02fd\7g\2\2\u02fd\u02fe\7u\2\2\u02fej\3\2\2\2\u02ff\u0300\7q\2"+
		"\2\u0300\u0301\7t\2\2\u0301\u0302\7f\2\2\u0302\u0303\7g\2\2\u0303\u0304"+
		"\7t\2\2\u0304l\3\2\2\2\u0305\u0306\7c\2\2\u0306\u0307\7u\2\2\u0307\u0308"+
		"\7e\2\2\u0308n\3\2\2\2\u0309\u030a\7f\2\2\u030a\u030b\7g\2\2\u030b\u030c"+
		"\7u\2\2\u030c\u030d\7e\2\2\u030dp\3\2\2\2\u030e\u030f\7t\2\2\u030f\u0310"+
		"\7u\2\2\u0310\u0311\7v\2\2\u0311\u0312\7t\2\2\u0312\u0313\7g\2\2\u0313"+
		"\u0314\7c\2\2\u0314\u0315\7o\2\2\u0315r\3\2\2\2\u0316\u0317\7k\2\2\u0317"+
		"\u0318\7u\2\2\u0318\u0319\7v\2\2\u0319\u031a\7t\2\2\u031a\u031b\7g\2\2"+
		"\u031b\u031c\7c\2\2\u031c\u031d\7o\2\2\u031dt\3\2\2\2\u031e\u031f\7k\2"+
		"\2\u031f\u0320\7t\2\2\u0320\u0321\7u\2\2\u0321\u0322\7v\2\2\u0322\u0323"+
		"\7t\2\2\u0323\u0324\7g\2\2\u0324\u0325\7c\2\2\u0325\u0326\7o\2\2\u0326"+
		"v\3\2\2\2\u0327\u0328\7u\2\2\u0328\u0329\7e\2\2\u0329\u032a\7j\2\2\u032a"+
		"\u032b\7g\2\2\u032b\u032c\7o\2\2\u032c\u032d\7c\2\2\u032dx\3\2\2\2\u032e"+
		"\u032f\7w\2\2\u032f\u0330\7p\2\2\u0330\u0331\7k\2\2\u0331\u0332\7f\2\2"+
		"\u0332\u0333\7k\2\2\u0333\u0334\7t\2\2\u0334\u0335\7g\2\2\u0335\u0336"+
		"\7e\2\2\u0336\u0337\7v\2\2\u0337\u0338\7k\2\2\u0338\u0339\7q\2\2\u0339"+
		"\u033a\7p\2\2\u033a\u033b\7c\2\2\u033b\u033c\7n\2\2\u033cz\3\2\2\2\u033d"+
		"\u033e\7t\2\2\u033e\u033f\7g\2\2\u033f\u0340\7v\2\2\u0340\u0341\7c\2\2"+
		"\u0341\u0342\7k\2\2\u0342\u0343\7p\2\2\u0343\u0344\7/\2\2\u0344\u0345"+
		"\7w\2\2\u0345\u0346\7p\2\2\u0346\u0347\7k\2\2\u0347\u0348\7q\2\2\u0348"+
		"\u0349\7p\2\2\u0349|\3\2\2\2\u034a\u034b\7t\2\2\u034b\u034c\7g\2\2\u034c"+
		"\u034d\7v\2\2\u034d\u034e\7c\2\2\u034e\u034f\7k\2\2\u034f\u0350\7p\2\2"+
		"\u0350\u0351\7/\2\2\u0351\u0352\7k\2\2\u0352\u0353\7p\2\2\u0353\u0354"+
		"\7v\2\2\u0354\u0355\7g\2\2\u0355\u0356\7t\2\2\u0356\u0357\7u\2\2\u0357"+
		"\u0358\7g\2\2\u0358\u0359\7e\2\2\u0359\u035a\7v\2\2\u035a\u035b\7k\2\2"+
		"\u035b\u035c\7q\2\2\u035c\u035d\7p\2\2\u035d~\3\2\2\2\u035e\u035f\7r\2"+
		"\2\u035f\u0360\7c\2\2\u0360\u0361\7v\2\2\u0361\u0362\7v\2\2\u0362\u0363"+
		"\7g\2\2\u0363\u0364\7t\2\2\u0364\u0365\7p\2\2\u0365\u0080\3\2\2\2\u0366"+
		"\u0367\7u\2\2\u0367\u0368\7s\2\2\u0368\u0369\7n\2\2\u0369\u0082\3\2\2"+
		"\2\u036a\u036b\7o\2\2\u036b\u036c\7g\2\2\u036c\u036d\7v\2\2\u036d\u036e"+
		"\7c\2\2\u036e\u036f\7f\2\2\u036f\u0370\7c\2\2\u0370\u0371\7v\2\2\u0371"+
		"\u0372\7c\2\2\u0372\u0373\7u\2\2\u0373\u0374\7s\2\2\u0374\u0375\7n\2\2"+
		"\u0375\u0084\3\2\2\2\u0376\u0377\7r\2\2\u0377\u0378\7t\2\2\u0378\u0379"+
		"\7g\2\2\u0379\u037a\7x\2\2\u037a\u0086\3\2\2\2\u037b\u037c\7r\2\2\u037c"+
		"\u037d\7t\2\2\u037d\u037e\7g\2\2\u037e\u037f\7x\2\2\u037f\u0380\7v\2\2"+
		"\u0380\u0381\7c\2\2\u0381\u0382\7k\2\2\u0382\u0383\7n\2\2\u0383\u0088"+
		"\3\2\2\2\u0384\u0385\7r\2\2\u0385\u0386\7t\2\2\u0386\u0387\7g\2\2\u0387"+
		"\u0388\7x\2\2\u0388\u0389\7e\2\2\u0389\u038a\7q\2\2\u038a\u038b\7w\2\2"+
		"\u038b\u038c\7p\2\2\u038c\u038d\7v\2\2\u038d\u008a\3\2\2\2\u038e\u038f"+
		"\7r\2\2\u038f\u0390\7t\2\2\u0390\u0391\7g\2\2\u0391\u0392\7x\2\2\u0392"+
		"\u0393\7y\2\2\u0393\u0394\7k\2\2\u0394\u0395\7p\2\2\u0395\u0396\7f\2\2"+
		"\u0396\u0397\7q\2\2\u0397\u0398\7y\2\2\u0398\u008c\3\2\2\2\u0399\u039a"+
		"\7r\2\2\u039a\u039b\7t\2\2\u039b\u039c\7k\2\2\u039c\u039d\7q\2\2\u039d"+
		"\u039e\7t\2\2\u039e\u008e\3\2\2\2\u039f\u03a0\7g\2\2\u03a0\u03a1\7z\2"+
		"\2\u03a1\u03a2\7k\2\2\u03a2\u03a3\7u\2\2\u03a3\u03a4\7v\2\2\u03a4\u03a5"+
		"\7u\2\2\u03a5\u0090\3\2\2\2\u03a6\u03a7\7y\2\2\u03a7\u03a8\7g\2\2\u03a8"+
		"\u03a9\7g\2\2\u03a9\u03aa\7m\2\2\u03aa\u03ab\7f\2\2\u03ab\u03ac\7c\2\2"+
		"\u03ac\u03ad\7{\2\2\u03ad\u0092\3\2\2\2\u03ae\u03af\7n\2\2\u03af\u03b0"+
		"\7c\2\2\u03b0\u03b1\7u\2\2\u03b1\u03b2\7v\2\2\u03b2\u03b3\7y\2\2\u03b3"+
		"\u03b4\7g\2\2\u03b4\u03b5\7g\2\2\u03b5\u03b6\7m\2\2\u03b6\u03b7\7f\2\2"+
		"\u03b7\u03b8\7c\2\2\u03b8\u03b9\7{\2\2\u03b9\u0094\3\2\2\2\u03ba\u03bb"+
		"\7k\2\2\u03bb\u03bc\7p\2\2\u03bc\u03bd\7u\2\2\u03bd\u03be\7v\2\2\u03be"+
		"\u03bf\7c\2\2\u03bf\u03c0\7p\2\2\u03c0\u03c1\7e\2\2\u03c1\u03c2\7g\2\2"+
		"\u03c2\u03c3\7q\2\2\u03c3\u03c4\7h\2\2\u03c4\u0096\3\2\2\2\u03c5\u03c6"+
		"\7v\2\2\u03c6\u03c7\7{\2\2\u03c7\u03c8\7r\2\2\u03c8\u03c9\7g\2\2\u03c9"+
		"\u03ca\7q\2\2\u03ca\u03cb\7h\2\2\u03cb\u0098\3\2\2\2\u03cc\u03cd\7e\2"+
		"\2\u03cd\u03ce\7c\2\2\u03ce\u03cf\7u\2\2\u03cf\u03d0\7v\2\2\u03d0\u009a"+
		"\3\2\2\2\u03d1\u03d2\7e\2\2\u03d2\u03d3\7w\2\2\u03d3\u03d4\7t\2\2\u03d4"+
		"\u03d5\7t\2\2\u03d5\u03d6\7g\2\2\u03d6\u03d7\7p\2\2\u03d7\u03d8\7v\2\2"+
		"\u03d8\u03d9\7a\2\2\u03d9\u03da\7v\2\2\u03da\u03db\7k\2\2\u03db\u03dc"+
		"\7o\2\2\u03dc\u03dd\7g\2\2\u03dd\u03de\7u\2\2\u03de\u03df\7v\2\2\u03df"+
		"\u03e0\7c\2\2\u03e0\u03e1\7o\2\2\u03e1\u03e2\7r\2\2\u03e2\u009c\3\2\2"+
		"\2\u03e3\u03e4\7f\2\2\u03e4\u03e5\7g\2\2\u03e5\u03e6\7n\2\2\u03e6\u03e7"+
		"\7g\2\2\u03e7\u03e8\7v\2\2\u03e8\u03e9\7g\2\2\u03e9\u009e\3\2\2\2\u03ea"+
		"\u03eb\7u\2\2\u03eb\u03ec\7p\2\2\u03ec\u03ed\7c\2\2\u03ed\u03ee\7r\2\2"+
		"\u03ee\u03ef\7u\2\2\u03ef\u03f0\7j\2\2\u03f0\u03f1\7q\2\2\u03f1\u03f2"+
		"\7v\2\2\u03f2\u00a0\3\2\2\2\u03f3\u03f4\7u\2\2\u03f4\u03f5\7g\2\2\u03f5"+
		"\u03f6\7v\2\2\u03f6\u00a2\3\2\2\2\u03f7\u03f8\7x\2\2\u03f8\u03f9\7c\2"+
		"\2\u03f9\u03fa\7t\2\2\u03fa\u03fb\7k\2\2\u03fb\u03fc\7c\2\2\u03fc\u03fd"+
		"\7d\2\2\u03fd\u03fe\7n\2\2\u03fe\u03ff\7g\2\2\u03ff\u00a4\3\2\2\2\u0400"+
		"\u0401\7v\2\2\u0401\u0402\7c\2\2\u0402\u0403\7d\2\2\u0403\u0404\7n\2\2"+
		"\u0404\u0405\7g\2\2\u0405\u00a6\3\2\2\2\u0406\u0407\7w\2\2\u0407\u0408"+
		"\7p\2\2\u0408\u0409\7v\2\2\u0409\u040a\7k\2\2\u040a\u040b\7n\2\2\u040b"+
		"\u00a8\3\2\2\2\u040c\u040d\7c\2\2\u040d\u040e\7v\2\2\u040e\u00aa\3\2\2"+
		"\2\u040f\u0410\7k\2\2\u0410\u0411\7p\2\2\u0411\u0412\7f\2\2\u0412\u0413"+
		"\7g\2\2\u0413\u0414\7z\2\2\u0414\u00ac\3\2\2\2\u0415\u0416\7{\2\2\u0416"+
		"\u0417\7g\2\2\u0417\u0418\7c\2\2\u0418\u0419\7t\2\2\u0419\u00ae\3\2\2"+
		"\2\u041a\u041b\7{\2\2\u041b\u041c\7g\2\2\u041c\u041d\7c\2\2\u041d\u041e"+
		"\7t\2\2\u041e\u041f\7u\2\2\u041f\u00b0\3\2\2\2\u0420\u0421\7o\2\2\u0421"+
		"\u0422\7q\2\2\u0422\u0423\7p\2\2\u0423\u0424\7v\2\2\u0424\u0425\7j\2\2"+
		"\u0425\u00b2\3\2\2\2\u0426\u0427\7o\2\2\u0427\u0428\7q\2\2\u0428\u0429"+
		"\7p\2\2\u0429\u042a\7v\2\2\u042a\u042b\7j\2\2\u042b\u042c\7u\2\2\u042c"+
		"\u00b4\3\2\2\2\u042d\u042e\7y\2\2\u042e\u042f\7g\2\2\u042f\u0430\7g\2"+
		"\2\u0430\u0431\7m\2\2\u0431\u00b6\3\2\2\2\u0432\u0433\7y\2\2\u0433\u0434"+
		"\7g\2\2\u0434\u0435\7g\2\2\u0435\u0436\7m\2\2\u0436\u0437\7u\2\2\u0437"+
		"\u00b8\3\2\2\2\u0438\u0439\7f\2\2\u0439\u043a\7c\2\2\u043a\u043b\7{\2"+
		"\2\u043b\u00ba\3\2\2\2\u043c\u043d\7f\2\2\u043d\u043e\7c\2\2\u043e\u043f"+
		"\7{\2\2\u043f\u0440\7u\2\2\u0440\u00bc\3\2\2\2\u0441\u0442\7j\2\2\u0442"+
		"\u0443\7q\2\2\u0443\u0444\7w\2\2\u0444\u0445\7t\2\2\u0445\u00be\3\2\2"+
		"\2\u0446\u0447\7j\2\2\u0447\u0448\7q\2\2\u0448\u0449\7w\2\2\u0449\u044a"+
		"\7t\2\2\u044a\u044b\7u\2\2\u044b\u00c0\3\2\2\2\u044c\u044d\7o\2\2\u044d"+
		"\u044e\7k\2\2\u044e\u044f\7p\2\2\u044f\u0450\7w\2\2\u0450\u0451\7v\2\2"+
		"\u0451\u0452\7g\2\2\u0452\u00c2\3\2\2\2\u0453\u0454\7o\2\2\u0454\u0455"+
		"\7k\2\2\u0455\u0456\7p\2\2\u0456\u0457\7w\2\2\u0457\u0458\7v\2\2\u0458"+
		"\u0459\7g\2\2\u0459\u045a\7u\2\2\u045a\u00c4\3\2\2\2\u045b\u045c\7u\2"+
		"\2\u045c\u045d\7g\2\2\u045d\u045e\7e\2\2\u045e\u00c6\3\2\2\2\u045f\u0460"+
		"\7u\2\2\u0460\u0461\7g\2\2\u0461\u0462\7e\2\2\u0462\u0463\7q\2\2\u0463"+
		"\u0464\7p\2\2\u0464\u0465\7f\2\2\u0465\u00c8\3\2\2\2\u0466\u0467\7u\2"+
		"\2\u0467\u0468\7g\2\2\u0468\u0469\7e\2\2\u0469\u046a\7q\2\2\u046a\u046b"+
		"\7p\2\2\u046b\u046c\7f\2\2\u046c\u046d\7u\2\2\u046d\u00ca\3\2\2\2\u046e"+
		"\u046f\7o\2\2\u046f\u0470\7u\2\2\u0470\u0471\7g\2\2\u0471\u0472\7e\2\2"+
		"\u0472\u00cc\3\2\2\2\u0473\u0474\7o\2\2\u0474\u0475\7k\2\2\u0475\u0476"+
		"\7n\2\2\u0476\u0477\7n\2\2\u0477\u0478\7k\2\2\u0478\u0479\7u\2\2\u0479"+
		"\u047a\7g\2\2\u047a\u047b\7e\2\2\u047b\u047c\7q\2\2\u047c\u047d\7p\2\2"+
		"\u047d\u047e\7f\2\2\u047e\u00ce\3\2\2\2\u047f\u0480\7o\2\2\u0480\u0481"+
		"\7k\2\2\u0481\u0482\7n\2\2\u0482\u0483\7n\2\2\u0483\u0484\7k\2\2\u0484"+
		"\u0485\7u\2\2\u0485\u0486\7g\2\2\u0486\u0487\7e\2\2\u0487\u0488\7q\2\2"+
		"\u0488\u0489\7p\2\2\u0489\u048a\7f\2\2\u048a\u048b\7u\2\2\u048b\u00d0"+
		"\3\2\2\2\u048c\u048d\7w\2\2\u048d\u048e\7u\2\2\u048e\u048f\7g\2\2\u048f"+
		"\u0490\7e\2\2\u0490\u00d2\3\2\2\2\u0491\u0492\7o\2\2\u0492\u0493\7k\2"+
		"\2\u0493\u0494\7e\2\2\u0494\u0495\7t\2\2\u0495\u0496\7q\2\2\u0496\u0497"+
		"\7u\2\2\u0497\u0498\7g\2\2\u0498\u0499\7e\2\2\u0499\u049a\7q\2\2\u049a"+
		"\u049b\7p\2\2\u049b\u049c\7f\2\2\u049c\u00d4\3\2\2\2\u049d\u049e\7o\2"+
		"\2\u049e\u049f\7k\2\2\u049f\u04a0\7e\2\2\u04a0\u04a1\7t\2\2\u04a1\u04a2"+
		"\7q\2\2\u04a2\u04a3\7u\2\2\u04a3\u04a4\7g\2\2\u04a4\u04a5\7e\2\2\u04a5"+
		"\u04a6\7q\2\2\u04a6\u04a7\7p\2\2\u04a7\u04a8\7f\2\2\u04a8\u04a9\7u\2\2"+
		"\u04a9\u00d6\3\2\2\2\u04aa\u04ab\7v\2\2\u04ab\u04ac\7t\2\2\u04ac\u04ad"+
		"\7w\2\2\u04ad\u04ae\7g\2\2\u04ae\u00d8\3\2\2\2\u04af\u04b0\7h\2\2\u04b0"+
		"\u04b1\7c\2\2\u04b1\u04b2\7n\2\2\u04b2\u04b3\7u\2\2\u04b3\u04b4\7g\2\2"+
		"\u04b4\u00da\3\2\2\2\u04b5\u04b6\7p\2\2\u04b6\u04b7\7w\2\2\u04b7\u04b8"+
		"\7n\2\2\u04b8\u04b9\7n\2\2\u04b9\u00dc\3\2\2\2\u04ba\u04bb\7n\2\2\u04bb"+
		"\u04bc\7k\2\2\u04bc\u04bd\7o\2\2\u04bd\u04be\7k\2\2\u04be\u04bf\7v\2\2"+
		"\u04bf\u00de\3\2\2\2\u04c0\u04c1\7q\2\2\u04c1\u04c2\7h\2\2\u04c2\u04c3"+
		"\7h\2\2\u04c3\u04c4\7u\2\2\u04c4\u04c5\7g\2\2\u04c5\u04c6\7v\2\2\u04c6"+
		"\u00e0\3\2\2\2\u04c7\u04c8\7w\2\2\u04c8\u04c9\7r\2\2\u04c9\u04ca\7f\2"+
		"\2\u04ca\u04cb\7c\2\2\u04cb\u04cc\7v\2\2\u04cc\u04cd\7g\2\2\u04cd\u00e2"+
		"\3\2\2\2\u04ce\u04cf\7o\2\2\u04cf\u04d0\7c\2\2\u04d0\u04d1\7v\2\2\u04d1"+
		"\u04d2\7e\2\2\u04d2\u04d3\7j\2\2\u04d3\u04d4\7a\2\2\u04d4\u04d5\7t\2\2"+
		"\u04d5\u04d6\7g\2\2\u04d6\u04d7\7e\2\2\u04d7\u04d8\7q\2\2\u04d8\u04d9"+
		"\7i\2\2\u04d9\u04da\7p\2\2\u04da\u04db\7k\2\2\u04db\u04dc\7|\2\2\u04dc"+
		"\u04dd\7g\2\2\u04dd\u00e4\3\2\2\2\u04de\u04df\7o\2\2\u04df\u04e0\7c\2"+
		"\2\u04e0\u04e1\7v\2\2\u04e1\u04e2\7e\2\2\u04e2\u04e3\7j\2\2\u04e3\u04e4"+
		"\7a\2\2\u04e4\u04e5\7t\2\2\u04e5\u04e6\7g\2\2\u04e6\u04e7\7e\2\2\u04e7"+
		"\u04e8\7q\2\2\u04e8\u04e9\7i\2\2\u04e9\u04ea\7p\2\2\u04ea\u04eb\7k\2\2"+
		"\u04eb\u04ec\7|\2\2\u04ec\u04ed\7g\2\2\u04ed\u04ee\7a\2\2\u04ee\u04ef"+
		"\7r\2\2\u04ef\u04f0\7g\2\2\u04f0\u04f1\7t\2\2\u04f1\u04f2\7o\2\2\u04f2"+
		"\u04f3\7w\2\2\u04f3\u04f4\7v\2\2\u04f4\u04f5\7g\2\2\u04f5\u00e6\3\2\2"+
		"\2\u04f6\u04f7\7o\2\2\u04f7\u04f8\7g\2\2\u04f8\u04f9\7c\2\2\u04f9\u04fa"+
		"\7u\2\2\u04fa\u04fb\7w\2\2\u04fb\u04fc\7t\2\2\u04fc\u04fd\7g\2\2\u04fd"+
		"\u04fe\7u\2\2\u04fe\u00e8\3\2\2\2\u04ff\u0500\7f\2\2\u0500\u0501\7g\2"+
		"\2\u0501\u0502\7h\2\2\u0502\u0503\7k\2\2\u0503\u0504\7p\2\2\u0504\u0505"+
		"\7g\2\2\u0505\u00ea\3\2\2\2\u0506\u0507\7r\2\2\u0507\u0508\7c\2\2\u0508"+
		"\u0509\7t\2\2\u0509\u050a\7v\2\2\u050a\u050b\7k\2\2\u050b\u050c\7v\2\2"+
		"\u050c\u050d\7k\2\2\u050d\u050e\7q\2\2\u050e\u050f\7p\2\2\u050f\u00ec"+
		"\3\2\2\2\u0510\u0511\7o\2\2\u0511\u0512\7c\2\2\u0512\u0513\7v\2\2\u0513"+
		"\u0514\7e\2\2\u0514\u0515\7j\2\2\u0515\u0516\7g\2\2\u0516\u0517\7u\2\2"+
		"\u0517\u00ee\3\2\2\2\u0518\u0519\7c\2\2\u0519\u051a\7h\2\2\u051a\u051b"+
		"\7v\2\2\u051b\u051c\7g\2\2\u051c\u051d\7t\2\2\u051d\u00f0\3\2\2\2\u051e"+
		"\u051f\7h\2\2\u051f\u0520\7q\2\2\u0520\u0521\7t\2\2\u0521\u00f2\3\2\2"+
		"\2\u0522\u0523\7y\2\2\u0523\u0524\7j\2\2\u0524\u0525\7k\2\2\u0525\u0526"+
		"\7n\2\2\u0526\u0527\7g\2\2\u0527\u00f4\3\2\2\2\u0528\u0529\7w\2\2\u0529"+
		"\u052a\7u\2\2\u052a\u052b\7k\2\2\u052b\u052c\7p\2\2\u052c\u052d\7i\2\2"+
		"\u052d\u00f6\3\2\2\2\u052e\u052f\7o\2\2\u052f\u0530\7g\2\2\u0530\u0531"+
		"\7t\2\2\u0531\u0532\7i\2\2\u0532\u0533\7g\2\2\u0533\u00f8\3\2\2\2\u0534"+
		"\u0535\7o\2\2\u0535\u0536\7c\2\2\u0536\u0537\7v\2\2\u0537\u0538\7e\2\2"+
		"\u0538\u0539\7j\2\2\u0539\u053a\7g\2\2\u053a\u053b\7f\2\2\u053b\u00fa"+
		"\3\2\2\2\u053c\u053d\7g\2\2\u053d\u053e\7z\2\2\u053e\u053f\7r\2\2\u053f"+
		"\u0540\7t\2\2\u0540\u0541\7g\2\2\u0541\u0542\7u\2\2\u0542\u0543\7u\2\2"+
		"\u0543\u0544\7k\2\2\u0544\u0545\7q\2\2\u0545\u0546\7p\2\2\u0546\u00fc"+
		"\3\2\2\2\u0547\u0548\7k\2\2\u0548\u0549\7p\2\2\u0549\u054a\7n\2\2\u054a"+
		"\u054b\7k\2\2\u054b\u054c\7p\2\2\u054c\u054d\7g\2\2\u054d\u054e\7f\2\2"+
		"\u054e\u054f\7a\2\2\u054f\u0550\7e\2\2\u0550\u0551\7n\2\2\u0551\u0552"+
		"\7c\2\2\u0552\u0553\7u\2\2\u0553\u0554\7u\2\2\u0554\u00fe\3\2\2\2\u0555"+
		"\u0556\7p\2\2\u0556\u0557\7g\2\2\u0557\u0558\7y\2\2\u0558\u0100\3\2\2"+
		"\2\u0559\u055a\7u\2\2\u055a\u055b\7v\2\2\u055b\u055c\7c\2\2\u055c\u055d"+
		"\7t\2\2\u055d\u055e\7v\2\2\u055e\u0102\3\2\2\2\u055f\u0560\7e\2\2\u0560"+
		"\u0561\7q\2\2\u0561\u0562\7p\2\2\u0562\u0563\7v\2\2\u0563\u0564\7g\2\2"+
		"\u0564\u0565\7z\2\2\u0565\u0566\7v\2\2\u0566\u0104\3\2\2\2\u0567\u0568"+
		"\7k\2\2\u0568\u0569\7p\2\2\u0569\u056a\7k\2\2\u056a\u056b\7v\2\2\u056b"+
		"\u056c\7k\2\2\u056c\u056d\7c\2\2\u056d\u056e\7v\2\2\u056e\u056f\7g\2\2"+
		"\u056f\u0570\7f\2\2\u0570\u0106\3\2\2\2\u0571\u0572\7v\2\2\u0572\u0573"+
		"\7g\2\2\u0573\u0574\7t\2\2\u0574\u0575\7o\2\2\u0575\u0576\7k\2\2\u0576"+
		"\u0577\7p\2\2\u0577\u0578\7c\2\2\u0578\u0579\7v\2\2\u0579\u057a\7g\2\2"+
		"\u057a\u057b\7f\2\2\u057b\u0108\3\2\2\2\u057c\u057d\7f\2\2\u057d\u057e"+
		"\7c\2\2\u057e\u057f\7v\2\2\u057f\u0580\7c\2\2\u0580\u0581\7h\2\2\u0581"+
		"\u0582\7n\2\2\u0582\u0583\7q\2\2\u0583\u0584\7y\2\2\u0584\u010a\3\2\2"+
		"\2\u0585\u0586\7e\2\2\u0586\u0587\7w\2\2\u0587\u0588\7d\2\2\u0588\u0589"+
		"\7g\2\2\u0589\u010c\3\2\2\2\u058a\u058b\7t\2\2\u058b\u058c\7q\2\2\u058c"+
		"\u058d\7n\2\2\u058d\u058e\7n\2\2\u058e\u058f\7w\2\2\u058f\u0590\7r\2\2"+
		"\u0590\u010e\3\2\2\2\u0591\u0592\7i\2\2\u0592\u0593\7t\2\2\u0593\u0594"+
		"\7q\2\2\u0594\u0595\7w\2\2\u0595\u0596\7r\2\2\u0596\u0597\7k\2\2\u0597"+
		"\u0598\7p\2\2\u0598\u0599\7i\2\2\u0599\u0110\3\2\2\2\u059a\u059b\7i\2"+
		"\2\u059b\u059c\7t\2\2\u059c\u059d\7q\2\2\u059d\u059e\7w\2\2\u059e\u059f"+
		"\7r\2\2\u059f\u05a0\7k\2\2\u05a0\u05a1\7p\2\2\u05a1\u05a2\7i\2\2\u05a2"+
		"\u05a3\7a\2\2\u05a3\u05a4\7k\2\2\u05a4\u05a5\7f\2\2\u05a5\u0112\3\2\2"+
		"\2\u05a6\u05a7\7u\2\2\u05a7\u05a8\7g\2\2\u05a8\u05a9\7v\2\2\u05a9\u05aa"+
		"\7u\2\2\u05aa\u0114\3\2\2\2\u05ab\u05ac\7g\2\2\u05ac\u05ad\7x\2\2\u05ad"+
		"\u05ae\7g\2\2\u05ae\u05af\7p\2\2\u05af\u05b0\7v\2\2\u05b0\u05b1\7/\2\2"+
		"\u05b1\u05b2\7r\2\2\u05b2\u05b3\7t\2\2\u05b3\u05b4\7g\2\2\u05b4\u05b5"+
		"\7e\2\2\u05b5\u05b6\7g\2\2\u05b6\u05b7\7f\2\2\u05b7\u05b8\7g\2\2\u05b8"+
		"\u05b9\7p\2\2\u05b9\u05ba\7e\2\2\u05ba\u05bb\7g\2\2\u05bb\u0116\3\2\2"+
		"\2\u05bc\u05bd\7/\2\2\u05bd\u05be\7]\2\2\u05be\u0118\3\2\2\2\u05bf\u05c0"+
		"\7/\2\2\u05c0\u05c1\7@\2\2\u05c1\u011a\3\2\2\2\u05c2\u05c3\7?\2\2\u05c3"+
		"\u05c4\7@\2\2\u05c4\u011c\3\2\2\2\u05c5\u05c6\7?\2\2\u05c6\u011e\3\2\2"+
		"\2\u05c7\u05c8\7>\2\2\u05c8\u05c9\7@\2\2\u05c9\u0120\3\2\2\2\u05ca\u05cb"+
		"\7A\2\2\u05cb\u0122\3\2\2\2\u05cc\u05cd\7*\2\2\u05cd\u0124\3\2\2\2\u05ce"+
		"\u05cf\7+\2\2\u05cf\u0126\3\2\2\2\u05d0\u05d1\7]\2\2\u05d1\u0128\3\2\2"+
		"\2\u05d2\u05d3\7_\2\2\u05d3\u012a\3\2\2\2\u05d4\u05d5\7}\2\2\u05d5\u012c"+
		"\3\2\2\2\u05d6\u05d7\7\177\2\2\u05d7\u012e\3\2\2\2\u05d8\u05d9\7<\2\2"+
		"\u05d9\u0130\3\2\2\2\u05da\u05db\7.\2\2\u05db\u0132\3\2\2\2\u05dc\u05dd"+
		"\7?\2\2\u05dd\u05de\7?\2\2\u05de\u0134\3\2\2\2\u05df\u05e0\7#\2\2\u05e0"+
		"\u0136\3\2\2\2\u05e1\u05e2\7\u0080\2\2\u05e2\u0138\3\2\2\2\u05e3\u05e4"+
		"\7#\2\2\u05e4\u05e5\7?\2\2\u05e5\u013a\3\2\2\2\u05e6\u05e7\7\61\2\2\u05e7"+
		"\u013c\3\2\2\2\u05e8\u05e9\7\61\2\2\u05e9\u05ea\7?\2\2\u05ea\u013e\3\2"+
		"\2\2\u05eb\u05ec\7-\2\2\u05ec\u0140\3\2\2\2\u05ed\u05ee\7-\2\2\u05ee\u05ef"+
		"\7?\2\2\u05ef\u0142\3\2\2\2\u05f0\u05f1\7-\2\2\u05f1\u05f2\7-\2\2\u05f2"+
		"\u0144\3\2\2\2\u05f3\u05f4\7/\2\2\u05f4\u0146\3\2\2\2\u05f5\u05f6\7/\2"+
		"\2\u05f6\u05f7\7?\2\2\u05f7\u0148\3\2\2\2\u05f8\u05f9\7/\2\2\u05f9\u05fa"+
		"\7/\2\2\u05fa\u014a\3\2\2\2\u05fb\u05fc\7,\2\2\u05fc\u014c\3\2\2\2\u05fd"+
		"\u05fe\7,\2\2\u05fe\u05ff\7?\2\2\u05ff\u014e\3\2\2\2\u0600\u0601\7\'\2"+
		"\2\u0601\u0150\3\2\2\2\u0602\u0603\7\'\2\2\u0603\u0604\7?\2\2\u0604\u0152"+
		"\3\2\2\2\u0605\u0606\7@\2\2\u0606\u0607\7?\2\2\u0607\u0154\3\2\2\2\u0608"+
		"\u0609\7@\2\2\u0609\u0156\3\2\2\2\u060a\u060b\7>\2\2\u060b\u060c\7?\2"+
		"\2\u060c\u0158\3\2\2\2\u060d\u060e\7>\2\2\u060e\u015a\3\2\2\2\u060f\u0610"+
		"\7`\2\2\u0610\u015c\3\2\2\2\u0611\u0612\7`\2\2\u0612\u0613\7?\2\2\u0613"+
		"\u015e\3\2\2\2\u0614\u0615\7~\2\2\u0615\u0160\3\2\2\2\u0616\u0617\7~\2"+
		"\2\u0617\u0618\7?\2\2\u0618\u0162\3\2\2\2\u0619\u061a\7~\2\2\u061a\u061b"+
		"\7~\2\2\u061b\u0164\3\2\2\2\u061c\u061d\7(\2\2\u061d\u0166\3\2\2\2\u061e"+
		"\u061f\7(\2\2\u061f\u0620\7?\2\2\u0620\u0168\3\2\2\2\u0621\u0622\7(\2"+
		"\2\u0622\u0623\7(\2\2\u0623\u016a\3\2\2\2\u0624\u0625\7=\2\2\u0625\u016c"+
		"\3\2\2\2\u0626\u0627\7\60\2\2\u0627\u016e\3\2\2\2\u0628\u0629\7\u1901"+
		"\2\2\u0629\u0170\3\2\2\2\u062a\u062b\7\u1900\2\2\u062b\u0172\3\2\2\2\u062c"+
		"\u062d\7\u18ff\2\2\u062d\u0174\3\2\2\2\u062e\u062f\7^\2\2\u062f\u0176"+
		"\3\2\2\2\u0630\u0631\7b\2\2\u0631\u0178\3\2\2\2\u0632\u0633\7B\2\2\u0633"+
		"\u017a\3\2\2\2\u0634\u0635\7%\2\2\u0635\u017c\3\2\2\2\u0636\u0638\t\2"+
		"\2\2\u0637\u0636\3\2\2\2\u0638\u0639\3\2\2\2\u0639\u0637\3\2\2\2\u0639"+
		"\u063a\3\2\2\2\u063a\u063b\3\2\2\2\u063b\u063c\b\u00bf\2\2\u063c\u017e"+
		"\3\2\2\2\u063d\u063e\7\61\2\2\u063e\u063f\7\61\2\2\u063f\u0643\3\2\2\2"+
		"\u0640\u0642\n\3\2\2\u0641\u0640\3\2\2\2\u0642\u0645\3\2\2\2\u0643\u0641"+
		"\3\2\2\2\u0643\u0644\3\2\2\2\u0644\u064b\3\2\2\2\u0645\u0643\3\2\2\2\u0646"+
		"\u064c\7\f\2\2\u0647\u0649\7\17\2\2\u0648\u064a\7\f\2\2\u0649\u0648\3"+
		"\2\2\2\u0649\u064a\3\2\2\2\u064a\u064c\3\2\2\2\u064b\u0646\3\2\2\2\u064b"+
		"\u0647\3\2\2\2\u064b\u064c\3\2\2\2\u064c\u064d\3\2\2\2\u064d\u064e\b\u00c0"+
		"\2\2\u064e\u0180\3\2\2\2\u064f\u0650\7\61\2\2\u0650\u0651\7,\2\2\u0651"+
		"\u0655\3\2\2\2\u0652\u0654\13\2\2\2\u0653\u0652\3\2\2\2\u0654\u0657\3"+
		"\2\2\2\u0655\u0656\3\2\2\2\u0655\u0653\3\2\2\2\u0656\u0658\3\2\2\2\u0657"+
		"\u0655\3\2\2\2\u0658\u0659\7,\2\2\u0659\u065a\7\61\2\2\u065a\u065b\3\2"+
		"\2\2\u065b\u065c\b\u00c1\2\2\u065c\u0182\3\2\2\2\u065d\u0662\7b\2\2\u065e"+
		"\u0661\5\u018b\u00c6\2\u065f\u0661\n\4\2\2\u0660\u065e\3\2\2\2\u0660\u065f"+
		"\3\2\2\2\u0661\u0664\3\2\2\2\u0662\u0660\3\2\2\2\u0662\u0663\3\2\2\2\u0663"+
		"\u0665\3\2\2\2\u0664\u0662\3\2\2\2\u0665\u0666\7b\2\2\u0666\u0184\3\2"+
		"\2\2\u0667\u066c\7)\2\2\u0668\u066b\5\u018b\u00c6\2\u0669\u066b\n\5\2"+
		"\2\u066a\u0668\3\2\2\2\u066a\u0669\3\2\2\2\u066b\u066e\3\2\2\2\u066c\u066a"+
		"\3\2\2\2\u066c\u066d\3\2\2\2\u066d\u066f\3\2\2\2\u066e\u066c\3\2\2\2\u066f"+
		"\u0670\7)\2\2\u0670\u0186\3\2\2\2\u0671\u0676\7$\2\2\u0672\u0675\5\u018b"+
		"\u00c6\2\u0673\u0675\n\6\2\2\u0674\u0672\3\2\2\2\u0674\u0673\3\2\2\2\u0675"+
		"\u0678\3\2\2\2\u0676\u0674\3\2\2\2\u0676\u0677\3\2\2\2\u0677\u0679\3\2"+
		"\2\2\u0678\u0676\3\2\2\2\u0679\u067a\7$\2\2\u067a\u0188\3\2\2\2\u067b"+
		"\u067c\7$\2\2\u067c\u067d\7$\2\2\u067d\u067e\7$\2\2\u067e\u018a\3\2\2"+
		"\2\u067f\u0684\7^\2\2\u0680\u0685\t\7\2\2\u0681\u0685\5\u0195\u00cb\2"+
		"\u0682\u0685\5\u0193\u00ca\2\u0683\u0685\13\2\2\2\u0684\u0680\3\2\2\2"+
		"\u0684\u0681\3\2\2\2\u0684\u0682\3\2\2\2\u0684\u0683\3\2\2\2\u0685\u018c"+
		"\3\2\2\2\u0686\u068a\t\b\2\2\u0687\u0689\t\t\2\2\u0688\u0687\3\2\2\2\u0689"+
		"\u068c\3\2\2\2\u068a\u0688\3\2\2\2\u068a\u068b\3\2\2\2\u068b\u018e\3\2"+
		"\2\2\u068c\u068a\3\2\2\2\u068d\u0692\5\u0197\u00cc\2\u068e\u0692\5\u0199"+
		"\u00cd\2\u068f\u0692\5\u019b\u00ce\2\u0690\u0692\5\u019d\u00cf\2\u0691"+
		"\u068d\3\2\2\2\u0691\u068e\3\2\2\2\u0691\u068f\3\2\2\2\u0691\u0690\3\2"+
		"\2\2\u0692\u0190\3\2\2\2\u0693\u0696\5\u01c5\u00e3\2\u0694\u0696\5\u01d1"+
		"\u00e9\2\u0695\u0693\3\2\2\2\u0695\u0694\3\2\2\2\u0696\u0192\3\2\2\2\u0697"+
		"\u0698\7^\2\2\u0698\u0699\4\62\65\2\u0699\u069a\4\629\2\u069a\u06a1\4"+
		"\629\2\u069b\u069c\7^\2\2\u069c\u069d\4\629\2\u069d\u06a1\4\629\2\u069e"+
		"\u069f\7^\2\2\u069f\u06a1\4\629\2\u06a0\u0697\3\2\2\2\u06a0\u069b\3\2"+
		"\2\2\u06a0\u069e\3\2\2\2\u06a1\u0194\3\2\2\2\u06a2\u06a3\7^\2\2\u06a3"+
		"\u06a4\7w\2\2\u06a4\u06a5\5\u01b1\u00d9\2\u06a5\u06a6\5\u01b1\u00d9\2"+
		"\u06a6\u06a7\5\u01b1\u00d9\2\u06a7\u06a8\5\u01b1\u00d9\2\u06a8\u0196\3"+
		"\2\2\2\u06a9\u06ab\5\u01a1\u00d1\2\u06aa\u06ac\5\u019f\u00d0\2\u06ab\u06aa"+
		"\3\2\2\2\u06ab\u06ac\3\2\2\2\u06ac\u0198\3\2\2\2\u06ad\u06af\5\u01ad\u00d7"+
		"\2\u06ae\u06b0\5\u019f\u00d0\2\u06af\u06ae\3\2\2\2\u06af\u06b0\3\2\2\2"+
		"\u06b0\u019a\3\2\2\2\u06b1\u06b3\5\u01b5\u00db\2\u06b2\u06b4\5\u019f\u00d0"+
		"\2\u06b3\u06b2\3\2\2\2\u06b3\u06b4\3\2\2\2\u06b4\u019c\3\2\2\2\u06b5\u06b7"+
		"\5\u01bd\u00df\2\u06b6\u06b8\5\u019f\u00d0\2\u06b7\u06b6\3\2\2\2\u06b7"+
		"\u06b8\3\2\2\2\u06b8\u019e\3\2\2\2\u06b9\u06ba\t\n\2\2\u06ba\u01a0\3\2"+
		"\2\2\u06bb\u06cc\7\62\2\2\u06bc\u06be\7\62\2\2\u06bd\u06bc\3\2\2\2\u06be"+
		"\u06c1\3\2\2\2\u06bf\u06bd\3\2\2\2\u06bf\u06c0\3\2\2\2\u06c0\u06c2\3\2"+
		"\2\2\u06c1\u06bf\3\2\2\2\u06c2\u06c9\5\u01a7\u00d4\2\u06c3\u06c5\5\u01a3"+
		"\u00d2\2\u06c4\u06c3\3\2\2\2\u06c4\u06c5\3\2\2\2\u06c5\u06ca\3\2\2\2\u06c6"+
		"\u06c7\5\u01ab\u00d6\2\u06c7\u06c8\5\u01a3\u00d2\2\u06c8\u06ca\3\2\2\2"+
		"\u06c9\u06c4\3\2\2\2\u06c9\u06c6\3\2\2\2\u06ca\u06cc\3\2\2\2\u06cb\u06bb"+
		"\3\2\2\2\u06cb\u06bf\3\2\2\2\u06cc\u01a2\3\2\2\2\u06cd\u06d5\5\u01a5\u00d3"+
		"\2\u06ce\u06d0\5\u01a9\u00d5\2\u06cf\u06ce\3\2\2\2\u06d0\u06d3\3\2\2\2"+
		"\u06d1\u06cf\3\2\2\2\u06d1\u06d2\3\2\2\2\u06d2\u06d4\3\2\2\2\u06d3\u06d1"+
		"\3\2\2\2\u06d4\u06d6\5\u01a5\u00d3\2\u06d5\u06d1\3\2\2\2\u06d5\u06d6\3"+
		"\2\2\2\u06d6\u01a4\3\2\2\2\u06d7\u06da\7\62\2\2\u06d8\u06da\5\u01a7\u00d4"+
		"\2\u06d9\u06d7\3\2\2\2\u06d9\u06d8\3\2\2\2\u06da\u01a6\3\2\2\2\u06db\u06dc"+
		"\t\13\2\2\u06dc\u01a8\3\2\2\2\u06dd\u06e0\5\u01a5\u00d3\2\u06de\u06e0"+
		"\7a\2\2\u06df\u06dd\3\2\2\2\u06df\u06de\3\2\2\2\u06e0\u01aa\3\2\2\2\u06e1"+
		"\u06e3\7a\2\2\u06e2\u06e1\3\2\2\2\u06e3\u06e4\3\2\2\2\u06e4\u06e2\3\2"+
		"\2\2\u06e4\u06e5\3\2\2\2\u06e5\u01ac\3\2\2\2\u06e6\u06e7\7\62\2\2\u06e7"+
		"\u06e8\t\f\2\2\u06e8\u06e9\5\u01af\u00d8\2\u06e9\u01ae\3\2\2\2\u06ea\u06f2"+
		"\5\u01b1\u00d9\2\u06eb\u06ed\5\u01b3\u00da\2\u06ec\u06eb\3\2\2\2\u06ed"+
		"\u06f0\3\2\2\2\u06ee\u06ec\3\2\2\2\u06ee\u06ef\3\2\2\2\u06ef\u06f1\3\2"+
		"\2\2\u06f0\u06ee\3\2\2\2\u06f1\u06f3\5\u01b1\u00d9\2\u06f2\u06ee\3\2\2"+
		"\2\u06f2\u06f3\3\2\2\2\u06f3\u01b0\3\2\2\2\u06f4\u06f5\t\r\2\2\u06f5\u01b2"+
		"\3\2\2\2\u06f6\u06f9\5\u01b1\u00d9\2\u06f7\u06f9\7a\2\2\u06f8\u06f6\3"+
		"\2\2\2\u06f8\u06f7\3\2\2\2\u06f9\u01b4\3\2\2\2\u06fa\u06fc\7\62\2\2\u06fb"+
		"\u06fd\5\u01ab\u00d6\2\u06fc\u06fb\3\2\2\2\u06fc\u06fd\3\2\2\2\u06fd\u06fe"+
		"\3\2\2\2\u06fe\u06ff\5\u01b7\u00dc\2\u06ff\u01b6\3\2\2\2\u0700\u0708\5"+
		"\u01b9\u00dd\2\u0701\u0703\5\u01bb\u00de\2\u0702\u0701\3\2\2\2\u0703\u0706"+
		"\3\2\2\2\u0704\u0702\3\2\2\2\u0704\u0705\3\2\2\2\u0705\u0707\3\2\2\2\u0706"+
		"\u0704\3\2\2\2\u0707\u0709\5\u01b9\u00dd\2\u0708\u0704\3\2\2\2\u0708\u0709"+
		"\3\2\2\2\u0709\u01b8\3\2\2\2\u070a\u070b\t\16\2\2\u070b\u01ba\3\2\2\2"+
		"\u070c\u070f\5\u01b9\u00dd\2\u070d\u070f\7a\2\2\u070e\u070c\3\2\2\2\u070e"+
		"\u070d\3\2\2\2\u070f\u01bc\3\2\2\2\u0710\u0711\7\62\2\2\u0711\u0712\t"+
		"\17\2\2\u0712\u0713\5\u01bf\u00e0\2\u0713\u01be\3\2\2\2\u0714\u071c\5"+
		"\u01c1\u00e1\2\u0715\u0717\5\u01c3\u00e2\2\u0716\u0715\3\2\2\2\u0717\u071a"+
		"\3\2\2\2\u0718\u0716\3\2\2\2\u0718\u0719\3\2\2\2\u0719\u071b\3\2\2\2\u071a"+
		"\u0718\3\2\2\2\u071b\u071d\5\u01c1\u00e1\2\u071c\u0718\3\2\2\2\u071c\u071d"+
		"\3\2\2\2\u071d\u01c0\3\2\2\2\u071e\u071f\t\20\2\2\u071f\u01c2\3\2\2\2"+
		"\u0720\u0723\5\u01c1\u00e1\2\u0721\u0723\7a\2\2\u0722\u0720\3\2\2\2\u0722"+
		"\u0721\3\2\2\2\u0723\u01c4\3\2\2\2\u0724\u0725\5\u01a3\u00d2\2\u0725\u0727"+
		"\7\60\2\2\u0726\u0728\5\u01a3\u00d2\2\u0727\u0726\3\2\2\2\u0727\u0728"+
		"\3\2\2\2\u0728\u072a\3\2\2\2\u0729\u072b\5\u01c7\u00e4\2\u072a\u0729\3"+
		"\2\2\2\u072a\u072b\3\2\2\2\u072b\u072d\3\2\2\2\u072c\u072e\5\u01cf\u00e8"+
		"\2\u072d\u072c\3\2\2\2\u072d\u072e\3\2\2\2\u072e\u0740\3\2\2\2\u072f\u0730"+
		"\7\60\2\2\u0730\u0732\5\u01a3\u00d2\2\u0731\u0733\5\u01c7\u00e4\2\u0732"+
		"\u0731\3\2\2\2\u0732\u0733\3\2\2\2\u0733\u0735\3\2\2\2\u0734\u0736\5\u01cf"+
		"\u00e8\2\u0735\u0734\3\2\2\2\u0735\u0736\3\2\2\2\u0736\u0740\3\2\2\2\u0737"+
		"\u0738\5\u01a3\u00d2\2\u0738\u073a\5\u01c7\u00e4\2\u0739\u073b\5\u01cf"+
		"\u00e8\2\u073a\u0739\3\2\2\2\u073a\u073b\3\2\2\2\u073b\u0740\3\2\2\2\u073c"+
		"\u073d\5\u01a3\u00d2\2\u073d\u073e\5\u01cf\u00e8\2\u073e\u0740\3\2\2\2"+
		"\u073f\u0724\3\2\2\2\u073f\u072f\3\2\2\2\u073f\u0737\3\2\2\2\u073f\u073c"+
		"\3\2\2\2\u0740\u01c6\3\2\2\2\u0741\u0742\5\u01c9\u00e5\2\u0742\u0743\5"+
		"\u01cb\u00e6\2\u0743\u01c8\3\2\2\2\u0744\u0745\t\21\2\2\u0745\u01ca\3"+
		"\2\2\2\u0746\u0748\5\u01cd\u00e7\2\u0747\u0746\3\2\2\2\u0747\u0748\3\2"+
		"\2\2\u0748\u0749\3\2\2\2\u0749\u074a\5\u01a3\u00d2\2\u074a\u01cc\3\2\2"+
		"\2\u074b\u074c\t\22\2\2\u074c\u01ce\3\2\2\2\u074d\u074e\t\23\2\2\u074e"+
		"\u01d0\3\2\2\2\u074f\u0750\5\u01d3\u00ea\2\u0750\u0752\5\u01d5\u00eb\2"+
		"\u0751\u0753\5\u01cf\u00e8\2\u0752\u0751\3\2\2\2\u0752\u0753\3\2\2\2\u0753"+
		"\u01d2\3\2\2\2\u0754\u0756\5\u01ad\u00d7\2\u0755\u0757\7\60\2\2\u0756"+
		"\u0755\3\2\2\2\u0756\u0757\3\2\2\2\u0757\u0760\3\2\2\2\u0758\u0759\7\62"+
		"\2\2\u0759\u075b\t\f\2\2\u075a\u075c\5\u01af\u00d8\2\u075b\u075a\3\2\2"+
		"\2\u075b\u075c\3\2\2\2\u075c\u075d\3\2\2\2\u075d\u075e\7\60\2\2\u075e"+
		"\u0760\5\u01af\u00d8\2\u075f\u0754\3\2\2\2\u075f\u0758\3\2\2\2\u0760\u01d4"+
		"\3\2\2\2\u0761\u0762\5\u01d7\u00ec\2\u0762\u0763\5\u01cb\u00e6\2\u0763"+
		"\u01d6\3\2\2\2\u0764\u0765\t\24\2\2\u0765\u01d8\3\2\2\2\67\2\u0637\u0639"+
		"\u0643\u0649\u064b\u0655\u0660\u0662\u066a\u066c\u0674\u0676\u0684\u068a"+
		"\u0691\u0695\u06a0\u06ab\u06af\u06b3\u06b7\u06bf\u06c4\u06c9\u06cb\u06d1"+
		"\u06d5\u06d9\u06df\u06e4\u06ee\u06f2\u06f8\u06fc\u0704\u0708\u070e\u0718"+
		"\u071c\u0722\u0727\u072a\u072d\u0732\u0735\u073a\u073f\u0747\u0752\u0756"+
		"\u075b\u075f\3\2\3\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}