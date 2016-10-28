// Generated from EsperEPL2Grammar.g by ANTLR 4.5.3

  package com.espertech.esper.epl.generated;

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
	static { RuntimeMetaData.checkVersion("4.5.3", RuntimeMetaData.VERSION); }

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
		TIMEPERIOD_MILLISECOND=102, TIMEPERIOD_MILLISECONDS=103, BOOLEAN_TRUE=104, 
		BOOLEAN_FALSE=105, VALUE_NULL=106, ROW_LIMIT_EXPR=107, OFFSET=108, UPDATE=109, 
		MATCH_RECOGNIZE=110, MATCH_RECOGNIZE_PERMUTE=111, MEASURES=112, DEFINE=113, 
		PARTITION=114, MATCHES=115, AFTER=116, FOR=117, WHILE=118, USING=119, 
		MERGE=120, MATCHED=121, EXPRESSIONDECL=122, NEWKW=123, START=124, CONTEXT=125, 
		INITIATED=126, TERMINATED=127, DATAFLOW=128, CUBE=129, ROLLUP=130, GROUPING=131, 
		GROUPING_ID=132, SETS=133, FOLLOWMAX_BEGIN=134, FOLLOWMAX_END=135, FOLLOWED_BY=136, 
		GOES=137, EQUALS=138, SQL_NE=139, QUESTION=140, LPAREN=141, RPAREN=142, 
		LBRACK=143, RBRACK=144, LCURLY=145, RCURLY=146, COLON=147, COMMA=148, 
		EQUAL=149, LNOT=150, BNOT=151, NOT_EQUAL=152, DIV=153, DIV_ASSIGN=154, 
		PLUS=155, PLUS_ASSIGN=156, INC=157, MINUS=158, MINUS_ASSIGN=159, DEC=160, 
		STAR=161, STAR_ASSIGN=162, MOD=163, MOD_ASSIGN=164, GE=165, GT=166, LE=167, 
		LT=168, BXOR=169, BXOR_ASSIGN=170, BOR=171, BOR_ASSIGN=172, LOR=173, BAND=174, 
		BAND_ASSIGN=175, LAND=176, SEMI=177, DOT=178, NUM_LONG=179, NUM_DOUBLE=180, 
		NUM_FLOAT=181, ESCAPECHAR=182, ESCAPEBACKTICK=183, ATCHAR=184, HASHCHAR=185, 
		WS=186, SL_COMMENT=187, ML_COMMENT=188, TICKED_STRING_LITERAL=189, QUOTED_STRING_LITERAL=190, 
		STRING_LITERAL=191, IDENT=192, IntegerLiteral=193, FloatingPointLiteral=194;
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
		"TIMEPERIOD_MILLISECONDS", "BOOLEAN_TRUE", "BOOLEAN_FALSE", "VALUE_NULL", 
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
		"'true'", "'false'", "'null'", "'limit'", "'offset'", "'update'", "'match_recognize'", 
		"'match_recognize_permute'", "'measures'", "'define'", "'partition'", 
		"'matches'", "'after'", "'for'", "'while'", "'using'", "'merge'", "'matched'", 
		"'expression'", "'new'", "'start'", "'context'", "'initiated'", "'terminated'", 
		"'dataflow'", "'cube'", "'rollup'", "'grouping'", "'grouping_id'", "'sets'", 
		"'-['", "']>'", "'->'", "'=>'", "'='", "'<>'", "'?'", "'('", "')'", "'['", 
		"']'", "'{'", "'}'", "':'", "','", "'=='", "'!'", "'~'", "'!='", "'/'", 
		"'/='", "'+'", "'+='", "'++'", "'-'", "'-='", "'--'", "'*'", "'*='", "'%'", 
		"'%='", "'>='", "'>'", "'<='", "'<'", "'^'", "'^='", "'|'", "'|='", "'||'", 
		"'&'", "'&='", "'&&'", "';'", "'.'", "'\\u18FF'", "'\\u18FE'", "'\\u18FD'", 
		"'\\'", "'`'", "'@'", "'#'"
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
		"TIMEPERIOD_MILLISECONDS", "BOOLEAN_TRUE", "BOOLEAN_FALSE", "VALUE_NULL", 
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
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\u00c4\u071e\b\1\4"+
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
		"\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\4\3\4\3\4\3"+
		"\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\6\3\6\3\6\3\6\3\6\3\7\3\7\3\7\3\7\3\7"+
		"\3\7\3\7\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\t\3\t\3\t\3\n\3\n\3\n\3\n\3\13"+
		"\3\13\3\13\3\13\3\f\3\f\3\f\3\f\3\f\3\f\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3"+
		"\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\16\3\16\3\16\3\16\3\16\3\16\3\17\3\17"+
		"\3\17\3\20\3\20\3\20\3\20\3\21\3\21\3\21\3\21\3\22\3\22\3\22\3\22\3\23"+
		"\3\23\3\23\3\23\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\25\3\25"+
		"\3\25\3\25\3\25\3\25\3\25\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\27\3\27"+
		"\3\27\3\27\3\27\3\27\3\27\3\30\3\30\3\30\3\30\3\30\3\30\3\31\3\31\3\31"+
		"\3\31\3\31\3\31\3\31\3\32\3\32\3\32\3\32\3\32\3\33\3\33\3\33\3\33\3\33"+
		"\3\34\3\34\3\34\3\34\3\34\3\35\3\35\3\35\3\35\3\35\3\36\3\36\3\36\3\36"+
		"\3\37\3\37\3\37\3\37\3\37\3 \3 \3 \3 \3 \3 \3!\3!\3!\3!\3!\3!\3\"\3\""+
		"\3\"\3\"\3\"\3#\3#\3#\3#\3#\3$\3$\3$\3$\3$\3$\3%\3%\3%\3%\3%\3&\3&\3&"+
		"\3\'\3\'\3\'\3(\3(\3(\3)\3)\3)\3)\3)\3)\3*\3*\3*\3*\3*\3*\3*\3+\3+\3+"+
		"\3+\3+\3+\3+\3+\3+\3,\3,\3,\3,\3-\3-\3-\3-\3.\3.\3.\3.\3.\3/\3/\3/\3/"+
		"\3/\3/\3/\3\60\3\60\3\60\3\60\3\60\3\60\3\60\3\61\3\61\3\61\3\61\3\61"+
		"\3\61\3\62\3\62\3\62\3\62\3\62\3\63\3\63\3\63\3\63\3\63\3\63\3\63\3\64"+
		"\3\64\3\64\3\64\3\64\3\65\3\65\3\65\3\65\3\65\3\65\3\65\3\66\3\66\3\66"+
		"\3\66\3\66\3\66\3\67\3\67\3\67\3\67\38\38\38\38\38\39\39\39\39\39\39\3"+
		"9\39\3:\3:\3:\3:\3:\3:\3:\3:\3;\3;\3;\3;\3;\3;\3;\3;\3;\3<\3<\3<\3<\3"+
		"<\3<\3<\3=\3=\3=\3=\3=\3=\3=\3=\3=\3=\3=\3=\3=\3=\3=\3>\3>\3>\3>\3>\3"+
		">\3>\3>\3>\3>\3>\3>\3>\3?\3?\3?\3?\3?\3?\3?\3?\3?\3?\3?\3?\3?\3?\3?\3"+
		"?\3?\3?\3?\3?\3@\3@\3@\3@\3@\3@\3@\3@\3A\3A\3A\3A\3B\3B\3B\3B\3B\3B\3"+
		"B\3B\3B\3B\3B\3B\3C\3C\3C\3C\3C\3D\3D\3D\3D\3D\3D\3D\3D\3D\3E\3E\3E\3"+
		"E\3E\3E\3E\3E\3E\3E\3F\3F\3F\3F\3F\3F\3F\3F\3F\3F\3F\3G\3G\3G\3G\3G\3"+
		"G\3H\3H\3H\3H\3H\3H\3H\3I\3I\3I\3I\3I\3I\3I\3I\3J\3J\3J\3J\3J\3J\3J\3"+
		"J\3J\3J\3J\3J\3K\3K\3K\3K\3K\3K\3K\3K\3K\3K\3K\3L\3L\3L\3L\3L\3L\3L\3"+
		"M\3M\3M\3M\3M\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3"+
		"O\3O\3O\3O\3O\3O\3O\3P\3P\3P\3P\3P\3P\3P\3P\3P\3Q\3Q\3Q\3Q\3R\3R\3R\3"+
		"R\3R\3R\3R\3R\3R\3S\3S\3S\3S\3S\3S\3T\3T\3T\3T\3T\3T\3U\3U\3U\3V\3V\3"+
		"V\3V\3V\3V\3W\3W\3W\3W\3W\3X\3X\3X\3X\3X\3X\3Y\3Y\3Y\3Y\3Y\3Y\3Z\3Z\3"+
		"Z\3Z\3Z\3Z\3Z\3[\3[\3[\3[\3[\3\\\3\\\3\\\3\\\3\\\3\\\3]\3]\3]\3]\3^\3"+
		"^\3^\3^\3^\3_\3_\3_\3_\3_\3`\3`\3`\3`\3`\3`\3a\3a\3a\3a\3a\3a\3a\3b\3"+
		"b\3b\3b\3b\3b\3b\3b\3c\3c\3c\3c\3d\3d\3d\3d\3d\3d\3d\3e\3e\3e\3e\3e\3"+
		"e\3e\3e\3f\3f\3f\3f\3f\3g\3g\3g\3g\3g\3g\3g\3g\3g\3g\3g\3g\3h\3h\3h\3"+
		"h\3h\3h\3h\3h\3h\3h\3h\3h\3h\3i\3i\3i\3i\3i\3j\3j\3j\3j\3j\3j\3k\3k\3"+
		"k\3k\3k\3l\3l\3l\3l\3l\3l\3m\3m\3m\3m\3m\3m\3m\3n\3n\3n\3n\3n\3n\3n\3"+
		"o\3o\3o\3o\3o\3o\3o\3o\3o\3o\3o\3o\3o\3o\3o\3o\3p\3p\3p\3p\3p\3p\3p\3"+
		"p\3p\3p\3p\3p\3p\3p\3p\3p\3p\3p\3p\3p\3p\3p\3p\3p\3q\3q\3q\3q\3q\3q\3"+
		"q\3q\3q\3r\3r\3r\3r\3r\3r\3r\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3t\3t\3t\3"+
		"t\3t\3t\3t\3t\3u\3u\3u\3u\3u\3u\3v\3v\3v\3v\3w\3w\3w\3w\3w\3w\3x\3x\3"+
		"x\3x\3x\3x\3y\3y\3y\3y\3y\3y\3z\3z\3z\3z\3z\3z\3z\3z\3{\3{\3{\3{\3{\3"+
		"{\3{\3{\3{\3{\3{\3|\3|\3|\3|\3}\3}\3}\3}\3}\3}\3~\3~\3~\3~\3~\3~\3~\3"+
		"~\3\177\3\177\3\177\3\177\3\177\3\177\3\177\3\177\3\177\3\177\3\u0080"+
		"\3\u0080\3\u0080\3\u0080\3\u0080\3\u0080\3\u0080\3\u0080\3\u0080\3\u0080"+
		"\3\u0080\3\u0081\3\u0081\3\u0081\3\u0081\3\u0081\3\u0081\3\u0081\3\u0081"+
		"\3\u0081\3\u0082\3\u0082\3\u0082\3\u0082\3\u0082\3\u0083\3\u0083\3\u0083"+
		"\3\u0083\3\u0083\3\u0083\3\u0083\3\u0084\3\u0084\3\u0084\3\u0084\3\u0084"+
		"\3\u0084\3\u0084\3\u0084\3\u0084\3\u0085\3\u0085\3\u0085\3\u0085\3\u0085"+
		"\3\u0085\3\u0085\3\u0085\3\u0085\3\u0085\3\u0085\3\u0085\3\u0086\3\u0086"+
		"\3\u0086\3\u0086\3\u0086\3\u0087\3\u0087\3\u0087\3\u0088\3\u0088\3\u0088"+
		"\3\u0089\3\u0089\3\u0089\3\u008a\3\u008a\3\u008a\3\u008b\3\u008b\3\u008c"+
		"\3\u008c\3\u008c\3\u008d\3\u008d\3\u008e\3\u008e\3\u008f\3\u008f\3\u0090"+
		"\3\u0090\3\u0091\3\u0091\3\u0092\3\u0092\3\u0093\3\u0093\3\u0094\3\u0094"+
		"\3\u0095\3\u0095\3\u0096\3\u0096\3\u0096\3\u0097\3\u0097\3\u0098\3\u0098"+
		"\3\u0099\3\u0099\3\u0099\3\u009a\3\u009a\3\u009b\3\u009b\3\u009b\3\u009c"+
		"\3\u009c\3\u009d\3\u009d\3\u009d\3\u009e\3\u009e\3\u009e\3\u009f\3\u009f"+
		"\3\u00a0\3\u00a0\3\u00a0\3\u00a1\3\u00a1\3\u00a1\3\u00a2\3\u00a2\3\u00a3"+
		"\3\u00a3\3\u00a3\3\u00a4\3\u00a4\3\u00a5\3\u00a5\3\u00a5\3\u00a6\3\u00a6"+
		"\3\u00a6\3\u00a7\3\u00a7\3\u00a8\3\u00a8\3\u00a8\3\u00a9\3\u00a9\3\u00aa"+
		"\3\u00aa\3\u00ab\3\u00ab\3\u00ab\3\u00ac\3\u00ac\3\u00ad\3\u00ad\3\u00ad"+
		"\3\u00ae\3\u00ae\3\u00ae\3\u00af\3\u00af\3\u00b0\3\u00b0\3\u00b0\3\u00b1"+
		"\3\u00b1\3\u00b1\3\u00b2\3\u00b2\3\u00b3\3\u00b3\3\u00b4\3\u00b4\3\u00b5"+
		"\3\u00b5\3\u00b6\3\u00b6\3\u00b7\3\u00b7\3\u00b8\3\u00b8\3\u00b9\3\u00b9"+
		"\3\u00ba\3\u00ba\3\u00bb\6\u00bb\u05f4\n\u00bb\r\u00bb\16\u00bb\u05f5"+
		"\3\u00bb\3\u00bb\3\u00bc\3\u00bc\3\u00bc\3\u00bc\7\u00bc\u05fe\n\u00bc"+
		"\f\u00bc\16\u00bc\u0601\13\u00bc\3\u00bc\3\u00bc\3\u00bc\5\u00bc\u0606"+
		"\n\u00bc\5\u00bc\u0608\n\u00bc\3\u00bc\3\u00bc\3\u00bd\3\u00bd\3\u00bd"+
		"\3\u00bd\7\u00bd\u0610\n\u00bd\f\u00bd\16\u00bd\u0613\13\u00bd\3\u00bd"+
		"\3\u00bd\3\u00bd\3\u00bd\3\u00bd\3\u00be\3\u00be\3\u00be\7\u00be\u061d"+
		"\n\u00be\f\u00be\16\u00be\u0620\13\u00be\3\u00be\3\u00be\3\u00bf\3\u00bf"+
		"\3\u00bf\7\u00bf\u0627\n\u00bf\f\u00bf\16\u00bf\u062a\13\u00bf\3\u00bf"+
		"\3\u00bf\3\u00c0\3\u00c0\3\u00c0\7\u00c0\u0631\n\u00c0\f\u00c0\16\u00c0"+
		"\u0634\13\u00c0\3\u00c0\3\u00c0\3\u00c1\3\u00c1\3\u00c1\3\u00c1\3\u00c1"+
		"\5\u00c1\u063d\n\u00c1\3\u00c2\3\u00c2\7\u00c2\u0641\n\u00c2\f\u00c2\16"+
		"\u00c2\u0644\13\u00c2\3\u00c3\3\u00c3\3\u00c3\3\u00c3\5\u00c3\u064a\n"+
		"\u00c3\3\u00c4\3\u00c4\5\u00c4\u064e\n\u00c4\3\u00c5\3\u00c5\3\u00c5\3"+
		"\u00c5\3\u00c5\3\u00c5\3\u00c5\3\u00c5\3\u00c5\5\u00c5\u0659\n\u00c5\3"+
		"\u00c6\3\u00c6\3\u00c6\3\u00c6\3\u00c6\3\u00c6\3\u00c6\3\u00c7\3\u00c7"+
		"\5\u00c7\u0664\n\u00c7\3\u00c8\3\u00c8\5\u00c8\u0668\n\u00c8\3\u00c9\3"+
		"\u00c9\5\u00c9\u066c\n\u00c9\3\u00ca\3\u00ca\5\u00ca\u0670\n\u00ca\3\u00cb"+
		"\3\u00cb\3\u00cc\3\u00cc\7\u00cc\u0676\n\u00cc\f\u00cc\16\u00cc\u0679"+
		"\13\u00cc\3\u00cc\3\u00cc\5\u00cc\u067d\n\u00cc\3\u00cc\3\u00cc\3\u00cc"+
		"\5\u00cc\u0682\n\u00cc\5\u00cc\u0684\n\u00cc\3\u00cd\3\u00cd\7\u00cd\u0688"+
		"\n\u00cd\f\u00cd\16\u00cd\u068b\13\u00cd\3\u00cd\5\u00cd\u068e\n\u00cd"+
		"\3\u00ce\3\u00ce\5\u00ce\u0692\n\u00ce\3\u00cf\3\u00cf\3\u00d0\3\u00d0"+
		"\5\u00d0\u0698\n\u00d0\3\u00d1\6\u00d1\u069b\n\u00d1\r\u00d1\16\u00d1"+
		"\u069c\3\u00d2\3\u00d2\3\u00d2\3\u00d2\3\u00d3\3\u00d3\7\u00d3\u06a5\n"+
		"\u00d3\f\u00d3\16\u00d3\u06a8\13\u00d3\3\u00d3\5\u00d3\u06ab\n\u00d3\3"+
		"\u00d4\3\u00d4\3\u00d5\3\u00d5\5\u00d5\u06b1\n\u00d5\3\u00d6\3\u00d6\5"+
		"\u00d6\u06b5\n\u00d6\3\u00d6\3\u00d6\3\u00d7\3\u00d7\7\u00d7\u06bb\n\u00d7"+
		"\f\u00d7\16\u00d7\u06be\13\u00d7\3\u00d7\5\u00d7\u06c1\n\u00d7\3\u00d8"+
		"\3\u00d8\3\u00d9\3\u00d9\5\u00d9\u06c7\n\u00d9\3\u00da\3\u00da\3\u00da"+
		"\3\u00da\3\u00db\3\u00db\7\u00db\u06cf\n\u00db\f\u00db\16\u00db\u06d2"+
		"\13\u00db\3\u00db\5\u00db\u06d5\n\u00db\3\u00dc\3\u00dc\3\u00dd\3\u00dd"+
		"\5\u00dd\u06db\n\u00dd\3\u00de\3\u00de\3\u00de\5\u00de\u06e0\n\u00de\3"+
		"\u00de\5\u00de\u06e3\n\u00de\3\u00de\5\u00de\u06e6\n\u00de\3\u00de\3\u00de"+
		"\3\u00de\5\u00de\u06eb\n\u00de\3\u00de\5\u00de\u06ee\n\u00de\3\u00de\3"+
		"\u00de\3\u00de\5\u00de\u06f3\n\u00de\3\u00de\3\u00de\3\u00de\5\u00de\u06f8"+
		"\n\u00de\3\u00df\3\u00df\3\u00df\3\u00e0\3\u00e0\3\u00e1\5\u00e1\u0700"+
		"\n\u00e1\3\u00e1\3\u00e1\3\u00e2\3\u00e2\3\u00e3\3\u00e3\3\u00e4\3\u00e4"+
		"\3\u00e4\5\u00e4\u070b\n\u00e4\3\u00e5\3\u00e5\5\u00e5\u070f\n\u00e5\3"+
		"\u00e5\3\u00e5\3\u00e5\5\u00e5\u0714\n\u00e5\3\u00e5\3\u00e5\5\u00e5\u0718"+
		"\n\u00e5\3\u00e6\3\u00e6\3\u00e6\3\u00e7\3\u00e7\3\u0611\2\u00e8\3\3\5"+
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
		"\u00bf\u017d\u00c0\u017f\u00c1\u0181\2\u0183\u00c2\u0185\u00c3\u0187\u00c4"+
		"\u0189\2\u018b\2\u018d\2\u018f\2\u0191\2\u0193\2\u0195\2\u0197\2\u0199"+
		"\2\u019b\2\u019d\2\u019f\2\u01a1\2\u01a3\2\u01a5\2\u01a7\2\u01a9\2\u01ab"+
		"\2\u01ad\2\u01af\2\u01b1\2\u01b3\2\u01b5\2\u01b7\2\u01b9\2\u01bb\2\u01bd"+
		"\2\u01bf\2\u01c1\2\u01c3\2\u01c5\2\u01c7\2\u01c9\2\u01cb\2\u01cd\2\3\2"+
		"\25\5\2\13\f\16\17\"\"\4\2\f\f\17\17\4\2^^bb\4\2))^^\4\2$$^^\n\2$$))^"+
		"^ddhhppttvv\5\2&&aac|\6\2&&\62;aac|\4\2NNnn\3\2\63;\4\2ZZzz\5\2\62;CH"+
		"ch\3\2\629\4\2DDdd\3\2\62\63\4\2GGgg\4\2--//\6\2FFHHffhh\4\2RRrr\u0734"+
		"\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2"+
		"\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2"+
		"\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2"+
		"\2\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2"+
		"\2\2\61\3\2\2\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3"+
		"\2\2\2\2=\3\2\2\2\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2\2\2G\3\2\2"+
		"\2\2I\3\2\2\2\2K\3\2\2\2\2M\3\2\2\2\2O\3\2\2\2\2Q\3\2\2\2\2S\3\2\2\2\2"+
		"U\3\2\2\2\2W\3\2\2\2\2Y\3\2\2\2\2[\3\2\2\2\2]\3\2\2\2\2_\3\2\2\2\2a\3"+
		"\2\2\2\2c\3\2\2\2\2e\3\2\2\2\2g\3\2\2\2\2i\3\2\2\2\2k\3\2\2\2\2m\3\2\2"+
		"\2\2o\3\2\2\2\2q\3\2\2\2\2s\3\2\2\2\2u\3\2\2\2\2w\3\2\2\2\2y\3\2\2\2\2"+
		"{\3\2\2\2\2}\3\2\2\2\2\177\3\2\2\2\2\u0081\3\2\2\2\2\u0083\3\2\2\2\2\u0085"+
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
		"\2\2\u0179\3\2\2\2\2\u017b\3\2\2\2\2\u017d\3\2\2\2\2\u017f\3\2\2\2\2\u0183"+
		"\3\2\2\2\2\u0185\3\2\2\2\2\u0187\3\2\2\2\3\u01cf\3\2\2\2\5\u01d6\3\2\2"+
		"\2\7\u01dd\3\2\2\2\t\u01e0\3\2\2\2\13\u01e8\3\2\2\2\r\u01ed\3\2\2\2\17"+
		"\u01f4\3\2\2\2\21\u01fb\3\2\2\2\23\u01fe\3\2\2\2\25\u0202\3\2\2\2\27\u0206"+
		"\3\2\2\2\31\u020c\3\2\2\2\33\u021b\3\2\2\2\35\u0221\3\2\2\2\37\u0224\3"+
		"\2\2\2!\u0228\3\2\2\2#\u022c\3\2\2\2%\u0230\3\2\2\2\'\u0234\3\2\2\2)\u023d"+
		"\3\2\2\2+\u0244\3\2\2\2-\u024b\3\2\2\2/\u0252\3\2\2\2\61\u0258\3\2\2\2"+
		"\63\u025f\3\2\2\2\65\u0264\3\2\2\2\67\u0269\3\2\2\29\u026e\3\2\2\2;\u0273"+
		"\3\2\2\2=\u0277\3\2\2\2?\u027c\3\2\2\2A\u0282\3\2\2\2C\u0288\3\2\2\2E"+
		"\u028d\3\2\2\2G\u0292\3\2\2\2I\u0298\3\2\2\2K\u029d\3\2\2\2M\u02a0\3\2"+
		"\2\2O\u02a3\3\2\2\2Q\u02a6\3\2\2\2S\u02ac\3\2\2\2U\u02b3\3\2\2\2W\u02bc"+
		"\3\2\2\2Y\u02c0\3\2\2\2[\u02c4\3\2\2\2]\u02c9\3\2\2\2_\u02d0\3\2\2\2a"+
		"\u02d7\3\2\2\2c\u02dd\3\2\2\2e\u02e2\3\2\2\2g\u02e9\3\2\2\2i\u02ee\3\2"+
		"\2\2k\u02f5\3\2\2\2m\u02fb\3\2\2\2o\u02ff\3\2\2\2q\u0304\3\2\2\2s\u030c"+
		"\3\2\2\2u\u0314\3\2\2\2w\u031d\3\2\2\2y\u0324\3\2\2\2{\u0333\3\2\2\2}"+
		"\u0340\3\2\2\2\177\u0354\3\2\2\2\u0081\u035c\3\2\2\2\u0083\u0360\3\2\2"+
		"\2\u0085\u036c\3\2\2\2\u0087\u0371\3\2\2\2\u0089\u037a\3\2\2\2\u008b\u0384"+
		"\3\2\2\2\u008d\u038f\3\2\2\2\u008f\u0395\3\2\2\2\u0091\u039c\3\2\2\2\u0093"+
		"\u03a4\3\2\2\2\u0095\u03b0\3\2\2\2\u0097\u03bb\3\2\2\2\u0099\u03c2\3\2"+
		"\2\2\u009b\u03c7\3\2\2\2\u009d\u03d9\3\2\2\2\u009f\u03e0\3\2\2\2\u00a1"+
		"\u03e9\3\2\2\2\u00a3\u03ed\3\2\2\2\u00a5\u03f6\3\2\2\2\u00a7\u03fc\3\2"+
		"\2\2\u00a9\u0402\3\2\2\2\u00ab\u0405\3\2\2\2\u00ad\u040b\3\2\2\2\u00af"+
		"\u0410\3\2\2\2\u00b1\u0416\3\2\2\2\u00b3\u041c\3\2\2\2\u00b5\u0423\3\2"+
		"\2\2\u00b7\u0428\3\2\2\2\u00b9\u042e\3\2\2\2\u00bb\u0432\3\2\2\2\u00bd"+
		"\u0437\3\2\2\2\u00bf\u043c\3\2\2\2\u00c1\u0442\3\2\2\2\u00c3\u0449\3\2"+
		"\2\2\u00c5\u0451\3\2\2\2\u00c7\u0455\3\2\2\2\u00c9\u045c\3\2\2\2\u00cb"+
		"\u0464\3\2\2\2\u00cd\u0469\3\2\2\2\u00cf\u0475\3\2\2\2\u00d1\u0482\3\2"+
		"\2\2\u00d3\u0487\3\2\2\2\u00d5\u048d\3\2\2\2\u00d7\u0492\3\2\2\2\u00d9"+
		"\u0498\3\2\2\2\u00db\u049f\3\2\2\2\u00dd\u04a6\3\2\2\2\u00df\u04b6\3\2"+
		"\2\2\u00e1\u04ce\3\2\2\2\u00e3\u04d7\3\2\2\2\u00e5\u04de\3\2\2\2\u00e7"+
		"\u04e8\3\2\2\2\u00e9\u04f0\3\2\2\2\u00eb\u04f6\3\2\2\2\u00ed\u04fa\3\2"+
		"\2\2\u00ef\u0500\3\2\2\2\u00f1\u0506\3\2\2\2\u00f3\u050c\3\2\2\2\u00f5"+
		"\u0514\3\2\2\2\u00f7\u051f\3\2\2\2\u00f9\u0523\3\2\2\2\u00fb\u0529\3\2"+
		"\2\2\u00fd\u0531\3\2\2\2\u00ff\u053b\3\2\2\2\u0101\u0546\3\2\2\2\u0103"+
		"\u054f\3\2\2\2\u0105\u0554\3\2\2\2\u0107\u055b\3\2\2\2\u0109\u0564\3\2"+
		"\2\2\u010b\u0570\3\2\2\2\u010d\u0575\3\2\2\2\u010f\u0578\3\2\2\2\u0111"+
		"\u057b\3\2\2\2\u0113\u057e\3\2\2\2\u0115\u0581\3\2\2\2\u0117\u0583\3\2"+
		"\2\2\u0119\u0586\3\2\2\2\u011b\u0588\3\2\2\2\u011d\u058a\3\2\2\2\u011f"+
		"\u058c\3\2\2\2\u0121\u058e\3\2\2\2\u0123\u0590\3\2\2\2\u0125\u0592\3\2"+
		"\2\2\u0127\u0594\3\2\2\2\u0129\u0596\3\2\2\2\u012b\u0598\3\2\2\2\u012d"+
		"\u059b\3\2\2\2\u012f\u059d\3\2\2\2\u0131\u059f\3\2\2\2\u0133\u05a2\3\2"+
		"\2\2\u0135\u05a4\3\2\2\2\u0137\u05a7\3\2\2\2\u0139\u05a9\3\2\2\2\u013b"+
		"\u05ac\3\2\2\2\u013d\u05af\3\2\2\2\u013f\u05b1\3\2\2\2\u0141\u05b4\3\2"+
		"\2\2\u0143\u05b7\3\2\2\2\u0145\u05b9\3\2\2\2\u0147\u05bc\3\2\2\2\u0149"+
		"\u05be\3\2\2\2\u014b\u05c1\3\2\2\2\u014d\u05c4\3\2\2\2\u014f\u05c6\3\2"+
		"\2\2\u0151\u05c9\3\2\2\2\u0153\u05cb\3\2\2\2\u0155\u05cd\3\2\2\2\u0157"+
		"\u05d0\3\2\2\2\u0159\u05d2\3\2\2\2\u015b\u05d5\3\2\2\2\u015d\u05d8\3\2"+
		"\2\2\u015f\u05da\3\2\2\2\u0161\u05dd\3\2\2\2\u0163\u05e0\3\2\2\2\u0165"+
		"\u05e2\3\2\2\2\u0167\u05e4\3\2\2\2\u0169\u05e6\3\2\2\2\u016b\u05e8\3\2"+
		"\2\2\u016d\u05ea\3\2\2\2\u016f\u05ec\3\2\2\2\u0171\u05ee\3\2\2\2\u0173"+
		"\u05f0\3\2\2\2\u0175\u05f3\3\2\2\2\u0177\u05f9\3\2\2\2\u0179\u060b\3\2"+
		"\2\2\u017b\u0619\3\2\2\2\u017d\u0623\3\2\2\2\u017f\u062d\3\2\2\2\u0181"+
		"\u0637\3\2\2\2\u0183\u063e\3\2\2\2\u0185\u0649\3\2\2\2\u0187\u064d\3\2"+
		"\2\2\u0189\u0658\3\2\2\2\u018b\u065a\3\2\2\2\u018d\u0661\3\2\2\2\u018f"+
		"\u0665\3\2\2\2\u0191\u0669\3\2\2\2\u0193\u066d\3\2\2\2\u0195\u0671\3\2"+
		"\2\2\u0197\u0683\3\2\2\2\u0199\u0685\3\2\2\2\u019b\u0691\3\2\2\2\u019d"+
		"\u0693\3\2\2\2\u019f\u0697\3\2\2\2\u01a1\u069a\3\2\2\2\u01a3\u069e\3\2"+
		"\2\2\u01a5\u06a2\3\2\2\2\u01a7\u06ac\3\2\2\2\u01a9\u06b0\3\2\2\2\u01ab"+
		"\u06b2\3\2\2\2\u01ad\u06b8\3\2\2\2\u01af\u06c2\3\2\2\2\u01b1\u06c6\3\2"+
		"\2\2\u01b3\u06c8\3\2\2\2\u01b5\u06cc\3\2\2\2\u01b7\u06d6\3\2\2\2\u01b9"+
		"\u06da\3\2\2\2\u01bb\u06f7\3\2\2\2\u01bd\u06f9\3\2\2\2\u01bf\u06fc\3\2"+
		"\2\2\u01c1\u06ff\3\2\2\2\u01c3\u0703\3\2\2\2\u01c5\u0705\3\2\2\2\u01c7"+
		"\u0707\3\2\2\2\u01c9\u0717\3\2\2\2\u01cb\u0719\3\2\2\2\u01cd\u071c\3\2"+
		"\2\2\u01cf\u01d0\7e\2\2\u01d0\u01d1\7t\2\2\u01d1\u01d2\7g\2\2\u01d2\u01d3"+
		"\7c\2\2\u01d3\u01d4\7v\2\2\u01d4\u01d5\7g\2\2\u01d5\4\3\2\2\2\u01d6\u01d7"+
		"\7y\2\2\u01d7\u01d8\7k\2\2\u01d8\u01d9\7p\2\2\u01d9\u01da\7f\2\2\u01da"+
		"\u01db\7q\2\2\u01db\u01dc\7y\2\2\u01dc\6\3\2\2\2\u01dd\u01de\7k\2\2\u01de"+
		"\u01df\7p\2\2\u01df\b\3\2\2\2\u01e0\u01e1\7d\2\2\u01e1\u01e2\7g\2\2\u01e2"+
		"\u01e3\7v\2\2\u01e3\u01e4\7y\2\2\u01e4\u01e5\7g\2\2\u01e5\u01e6\7g\2\2"+
		"\u01e6\u01e7\7p\2\2\u01e7\n\3\2\2\2\u01e8\u01e9\7n\2\2\u01e9\u01ea\7k"+
		"\2\2\u01ea\u01eb\7m\2\2\u01eb\u01ec\7g\2\2\u01ec\f\3\2\2\2\u01ed\u01ee"+
		"\7t\2\2\u01ee\u01ef\7g\2\2\u01ef\u01f0\7i\2\2\u01f0\u01f1\7g\2\2\u01f1"+
		"\u01f2\7z\2\2\u01f2\u01f3\7r\2\2\u01f3\16\3\2\2\2\u01f4\u01f5\7g\2\2\u01f5"+
		"\u01f6\7u\2\2\u01f6\u01f7\7e\2\2\u01f7\u01f8\7c\2\2\u01f8\u01f9\7r\2\2"+
		"\u01f9\u01fa\7g\2\2\u01fa\20\3\2\2\2\u01fb\u01fc\7q\2\2\u01fc\u01fd\7"+
		"t\2\2\u01fd\22\3\2\2\2\u01fe\u01ff\7c\2\2\u01ff\u0200\7p\2\2\u0200\u0201"+
		"\7f\2\2\u0201\24\3\2\2\2\u0202\u0203\7p\2\2\u0203\u0204\7q\2\2\u0204\u0205"+
		"\7v\2\2\u0205\26\3\2\2\2\u0206\u0207\7g\2\2\u0207\u0208\7x\2\2\u0208\u0209"+
		"\7g\2\2\u0209\u020a\7t\2\2\u020a\u020b\7{\2\2\u020b\30\3\2\2\2\u020c\u020d"+
		"\7g\2\2\u020d\u020e\7x\2\2\u020e\u020f\7g\2\2\u020f\u0210\7t\2\2\u0210"+
		"\u0211\7{\2\2\u0211\u0212\7/\2\2\u0212\u0213\7f\2\2\u0213\u0214\7k\2\2"+
		"\u0214\u0215\7u\2\2\u0215\u0216\7v\2\2\u0216\u0217\7k\2\2\u0217\u0218"+
		"\7p\2\2\u0218\u0219\7e\2\2\u0219\u021a\7v\2\2\u021a\32\3\2\2\2\u021b\u021c"+
		"\7y\2\2\u021c\u021d\7j\2\2\u021d\u021e\7g\2\2\u021e\u021f\7t\2\2\u021f"+
		"\u0220\7g\2\2\u0220\34\3\2\2\2\u0221\u0222\7c\2\2\u0222\u0223\7u\2\2\u0223"+
		"\36\3\2\2\2\u0224\u0225\7u\2\2\u0225\u0226\7w\2\2\u0226\u0227\7o\2\2\u0227"+
		" \3\2\2\2\u0228\u0229\7c\2\2\u0229\u022a\7x\2\2\u022a\u022b\7i\2\2\u022b"+
		"\"\3\2\2\2\u022c\u022d\7o\2\2\u022d\u022e\7c\2\2\u022e\u022f\7z\2\2\u022f"+
		"$\3\2\2\2\u0230\u0231\7o\2\2\u0231\u0232\7k\2\2\u0232\u0233\7p\2\2\u0233"+
		"&\3\2\2\2\u0234\u0235\7e\2\2\u0235\u0236\7q\2\2\u0236\u0237\7c\2\2\u0237"+
		"\u0238\7n\2\2\u0238\u0239\7g\2\2\u0239\u023a\7u\2\2\u023a\u023b\7e\2\2"+
		"\u023b\u023c\7g\2\2\u023c(\3\2\2\2\u023d\u023e\7o\2\2\u023e\u023f\7g\2"+
		"\2\u023f\u0240\7f\2\2\u0240\u0241\7k\2\2\u0241\u0242\7c\2\2\u0242\u0243"+
		"\7p\2\2\u0243*\3\2\2\2\u0244\u0245\7u\2\2\u0245\u0246\7v\2\2\u0246\u0247"+
		"\7f\2\2\u0247\u0248\7f\2\2\u0248\u0249\7g\2\2\u0249\u024a\7x\2\2\u024a"+
		",\3\2\2\2\u024b\u024c\7c\2\2\u024c\u024d\7x\2\2\u024d\u024e\7g\2\2\u024e"+
		"\u024f\7f\2\2\u024f\u0250\7g\2\2\u0250\u0251\7x\2\2\u0251.\3\2\2\2\u0252"+
		"\u0253\7e\2\2\u0253\u0254\7q\2\2\u0254\u0255\7w\2\2\u0255\u0256\7p\2\2"+
		"\u0256\u0257\7v\2\2\u0257\60\3\2\2\2\u0258\u0259\7u\2\2\u0259\u025a\7"+
		"g\2\2\u025a\u025b\7n\2\2\u025b\u025c\7g\2\2\u025c\u025d\7e\2\2\u025d\u025e"+
		"\7v\2\2\u025e\62\3\2\2\2\u025f\u0260\7e\2\2\u0260\u0261\7c\2\2\u0261\u0262"+
		"\7u\2\2\u0262\u0263\7g\2\2\u0263\64\3\2\2\2\u0264\u0265\7g\2\2\u0265\u0266"+
		"\7n\2\2\u0266\u0267\7u\2\2\u0267\u0268\7g\2\2\u0268\66\3\2\2\2\u0269\u026a"+
		"\7y\2\2\u026a\u026b\7j\2\2\u026b\u026c\7g\2\2\u026c\u026d\7p\2\2\u026d"+
		"8\3\2\2\2\u026e\u026f\7v\2\2\u026f\u0270\7j\2\2\u0270\u0271\7g\2\2\u0271"+
		"\u0272\7p\2\2\u0272:\3\2\2\2\u0273\u0274\7g\2\2\u0274\u0275\7p\2\2\u0275"+
		"\u0276\7f\2\2\u0276<\3\2\2\2\u0277\u0278\7h\2\2\u0278\u0279\7t\2\2\u0279"+
		"\u027a\7q\2\2\u027a\u027b\7o\2\2\u027b>\3\2\2\2\u027c\u027d\7q\2\2\u027d"+
		"\u027e\7w\2\2\u027e\u027f\7v\2\2\u027f\u0280\7g\2\2\u0280\u0281\7t\2\2"+
		"\u0281@\3\2\2\2\u0282\u0283\7k\2\2\u0283\u0284\7p\2\2\u0284\u0285\7p\2"+
		"\2\u0285\u0286\7g\2\2\u0286\u0287\7t\2\2\u0287B\3\2\2\2\u0288\u0289\7"+
		"l\2\2\u0289\u028a\7q\2\2\u028a\u028b\7k\2\2\u028b\u028c\7p\2\2\u028cD"+
		"\3\2\2\2\u028d\u028e\7n\2\2\u028e\u028f\7g\2\2\u028f\u0290\7h\2\2\u0290"+
		"\u0291\7v\2\2\u0291F\3\2\2\2\u0292\u0293\7t\2\2\u0293\u0294\7k\2\2\u0294"+
		"\u0295\7i\2\2\u0295\u0296\7j\2\2\u0296\u0297\7v\2\2\u0297H\3\2\2\2\u0298"+
		"\u0299\7h\2\2\u0299\u029a\7w\2\2\u029a\u029b\7n\2\2\u029b\u029c\7n\2\2"+
		"\u029cJ\3\2\2\2\u029d\u029e\7q\2\2\u029e\u029f\7p\2\2\u029fL\3\2\2\2\u02a0"+
		"\u02a1\7k\2\2\u02a1\u02a2\7u\2\2\u02a2N\3\2\2\2\u02a3\u02a4\7d\2\2\u02a4"+
		"\u02a5\7{\2\2\u02a5P\3\2\2\2\u02a6\u02a7\7i\2\2\u02a7\u02a8\7t\2\2\u02a8"+
		"\u02a9\7q\2\2\u02a9\u02aa\7w\2\2\u02aa\u02ab\7r\2\2\u02abR\3\2\2\2\u02ac"+
		"\u02ad\7j\2\2\u02ad\u02ae\7c\2\2\u02ae\u02af\7x\2\2\u02af\u02b0\7k\2\2"+
		"\u02b0\u02b1\7p\2\2\u02b1\u02b2\7i\2\2\u02b2T\3\2\2\2\u02b3\u02b4\7f\2"+
		"\2\u02b4\u02b5\7k\2\2\u02b5\u02b6\7u\2\2\u02b6\u02b7\7v\2\2\u02b7\u02b8"+
		"\7k\2\2\u02b8\u02b9\7p\2\2\u02b9\u02ba\7e\2\2\u02ba\u02bb\7v\2\2\u02bb"+
		"V\3\2\2\2\u02bc\u02bd\7c\2\2\u02bd\u02be\7n\2\2\u02be\u02bf\7n\2\2\u02bf"+
		"X\3\2\2\2\u02c0\u02c1\7c\2\2\u02c1\u02c2\7p\2\2\u02c2\u02c3\7{\2\2\u02c3"+
		"Z\3\2\2\2\u02c4\u02c5\7u\2\2\u02c5\u02c6\7q\2\2\u02c6\u02c7\7o\2\2\u02c7"+
		"\u02c8\7g\2\2\u02c8\\\3\2\2\2\u02c9\u02ca\7q\2\2\u02ca\u02cb\7w\2\2\u02cb"+
		"\u02cc\7v\2\2\u02cc\u02cd\7r\2\2\u02cd\u02ce\7w\2\2\u02ce\u02cf\7v\2\2"+
		"\u02cf^\3\2\2\2\u02d0\u02d1\7g\2\2\u02d1\u02d2\7x\2\2\u02d2\u02d3\7g\2"+
		"\2\u02d3\u02d4\7p\2\2\u02d4\u02d5\7v\2\2\u02d5\u02d6\7u\2\2\u02d6`\3\2"+
		"\2\2\u02d7\u02d8\7h\2\2\u02d8\u02d9\7k\2\2\u02d9\u02da\7t\2\2\u02da\u02db"+
		"\7u\2\2\u02db\u02dc\7v\2\2\u02dcb\3\2\2\2\u02dd\u02de\7n\2\2\u02de\u02df"+
		"\7c\2\2\u02df\u02e0\7u\2\2\u02e0\u02e1\7v\2\2\u02e1d\3\2\2\2\u02e2\u02e3"+
		"\7k\2\2\u02e3\u02e4\7p\2\2\u02e4\u02e5\7u\2\2\u02e5\u02e6\7g\2\2\u02e6"+
		"\u02e7\7t\2\2\u02e7\u02e8\7v\2\2\u02e8f\3\2\2\2\u02e9\u02ea\7k\2\2\u02ea"+
		"\u02eb\7p\2\2\u02eb\u02ec\7v\2\2\u02ec\u02ed\7q\2\2\u02edh\3\2\2\2\u02ee"+
		"\u02ef\7x\2\2\u02ef\u02f0\7c\2\2\u02f0\u02f1\7n\2\2\u02f1\u02f2\7w\2\2"+
		"\u02f2\u02f3\7g\2\2\u02f3\u02f4\7u\2\2\u02f4j\3\2\2\2\u02f5\u02f6\7q\2"+
		"\2\u02f6\u02f7\7t\2\2\u02f7\u02f8\7f\2\2\u02f8\u02f9\7g\2\2\u02f9\u02fa"+
		"\7t\2\2\u02fal\3\2\2\2\u02fb\u02fc\7c\2\2\u02fc\u02fd\7u\2\2\u02fd\u02fe"+
		"\7e\2\2\u02fen\3\2\2\2\u02ff\u0300\7f\2\2\u0300\u0301\7g\2\2\u0301\u0302"+
		"\7u\2\2\u0302\u0303\7e\2\2\u0303p\3\2\2\2\u0304\u0305\7t\2\2\u0305\u0306"+
		"\7u\2\2\u0306\u0307\7v\2\2\u0307\u0308\7t\2\2\u0308\u0309\7g\2\2\u0309"+
		"\u030a\7c\2\2\u030a\u030b\7o\2\2\u030br\3\2\2\2\u030c\u030d\7k\2\2\u030d"+
		"\u030e\7u\2\2\u030e\u030f\7v\2\2\u030f\u0310\7t\2\2\u0310\u0311\7g\2\2"+
		"\u0311\u0312\7c\2\2\u0312\u0313\7o\2\2\u0313t\3\2\2\2\u0314\u0315\7k\2"+
		"\2\u0315\u0316\7t\2\2\u0316\u0317\7u\2\2\u0317\u0318\7v\2\2\u0318\u0319"+
		"\7t\2\2\u0319\u031a\7g\2\2\u031a\u031b\7c\2\2\u031b\u031c\7o\2\2\u031c"+
		"v\3\2\2\2\u031d\u031e\7u\2\2\u031e\u031f\7e\2\2\u031f\u0320\7j\2\2\u0320"+
		"\u0321\7g\2\2\u0321\u0322\7o\2\2\u0322\u0323\7c\2\2\u0323x\3\2\2\2\u0324"+
		"\u0325\7w\2\2\u0325\u0326\7p\2\2\u0326\u0327\7k\2\2\u0327\u0328\7f\2\2"+
		"\u0328\u0329\7k\2\2\u0329\u032a\7t\2\2\u032a\u032b\7g\2\2\u032b\u032c"+
		"\7e\2\2\u032c\u032d\7v\2\2\u032d\u032e\7k\2\2\u032e\u032f\7q\2\2\u032f"+
		"\u0330\7p\2\2\u0330\u0331\7c\2\2\u0331\u0332\7n\2\2\u0332z\3\2\2\2\u0333"+
		"\u0334\7t\2\2\u0334\u0335\7g\2\2\u0335\u0336\7v\2\2\u0336\u0337\7c\2\2"+
		"\u0337\u0338\7k\2\2\u0338\u0339\7p\2\2\u0339\u033a\7/\2\2\u033a\u033b"+
		"\7w\2\2\u033b\u033c\7p\2\2\u033c\u033d\7k\2\2\u033d\u033e\7q\2\2\u033e"+
		"\u033f\7p\2\2\u033f|\3\2\2\2\u0340\u0341\7t\2\2\u0341\u0342\7g\2\2\u0342"+
		"\u0343\7v\2\2\u0343\u0344\7c\2\2\u0344\u0345\7k\2\2\u0345\u0346\7p\2\2"+
		"\u0346\u0347\7/\2\2\u0347\u0348\7k\2\2\u0348\u0349\7p\2\2\u0349\u034a"+
		"\7v\2\2\u034a\u034b\7g\2\2\u034b\u034c\7t\2\2\u034c\u034d\7u\2\2\u034d"+
		"\u034e\7g\2\2\u034e\u034f\7e\2\2\u034f\u0350\7v\2\2\u0350\u0351\7k\2\2"+
		"\u0351\u0352\7q\2\2\u0352\u0353\7p\2\2\u0353~\3\2\2\2\u0354\u0355\7r\2"+
		"\2\u0355\u0356\7c\2\2\u0356\u0357\7v\2\2\u0357\u0358\7v\2\2\u0358\u0359"+
		"\7g\2\2\u0359\u035a\7t\2\2\u035a\u035b\7p\2\2\u035b\u0080\3\2\2\2\u035c"+
		"\u035d\7u\2\2\u035d\u035e\7s\2\2\u035e\u035f\7n\2\2\u035f\u0082\3\2\2"+
		"\2\u0360\u0361\7o\2\2\u0361\u0362\7g\2\2\u0362\u0363\7v\2\2\u0363\u0364"+
		"\7c\2\2\u0364\u0365\7f\2\2\u0365\u0366\7c\2\2\u0366\u0367\7v\2\2\u0367"+
		"\u0368\7c\2\2\u0368\u0369\7u\2\2\u0369\u036a\7s\2\2\u036a\u036b\7n\2\2"+
		"\u036b\u0084\3\2\2\2\u036c\u036d\7r\2\2\u036d\u036e\7t\2\2\u036e\u036f"+
		"\7g\2\2\u036f\u0370\7x\2\2\u0370\u0086\3\2\2\2\u0371\u0372\7r\2\2\u0372"+
		"\u0373\7t\2\2\u0373\u0374\7g\2\2\u0374\u0375\7x\2\2\u0375\u0376\7v\2\2"+
		"\u0376\u0377\7c\2\2\u0377\u0378\7k\2\2\u0378\u0379\7n\2\2\u0379\u0088"+
		"\3\2\2\2\u037a\u037b\7r\2\2\u037b\u037c\7t\2\2\u037c\u037d\7g\2\2\u037d"+
		"\u037e\7x\2\2\u037e\u037f\7e\2\2\u037f\u0380\7q\2\2\u0380\u0381\7w\2\2"+
		"\u0381\u0382\7p\2\2\u0382\u0383\7v\2\2\u0383\u008a\3\2\2\2\u0384\u0385"+
		"\7r\2\2\u0385\u0386\7t\2\2\u0386\u0387\7g\2\2\u0387\u0388\7x\2\2\u0388"+
		"\u0389\7y\2\2\u0389\u038a\7k\2\2\u038a\u038b\7p\2\2\u038b\u038c\7f\2\2"+
		"\u038c\u038d\7q\2\2\u038d\u038e\7y\2\2\u038e\u008c\3\2\2\2\u038f\u0390"+
		"\7r\2\2\u0390\u0391\7t\2\2\u0391\u0392\7k\2\2\u0392\u0393\7q\2\2\u0393"+
		"\u0394\7t\2\2\u0394\u008e\3\2\2\2\u0395\u0396\7g\2\2\u0396\u0397\7z\2"+
		"\2\u0397\u0398\7k\2\2\u0398\u0399\7u\2\2\u0399\u039a\7v\2\2\u039a\u039b"+
		"\7u\2\2\u039b\u0090\3\2\2\2\u039c\u039d\7y\2\2\u039d\u039e\7g\2\2\u039e"+
		"\u039f\7g\2\2\u039f\u03a0\7m\2\2\u03a0\u03a1\7f\2\2\u03a1\u03a2\7c\2\2"+
		"\u03a2\u03a3\7{\2\2\u03a3\u0092\3\2\2\2\u03a4\u03a5\7n\2\2\u03a5\u03a6"+
		"\7c\2\2\u03a6\u03a7\7u\2\2\u03a7\u03a8\7v\2\2\u03a8\u03a9\7y\2\2\u03a9"+
		"\u03aa\7g\2\2\u03aa\u03ab\7g\2\2\u03ab\u03ac\7m\2\2\u03ac\u03ad\7f\2\2"+
		"\u03ad\u03ae\7c\2\2\u03ae\u03af\7{\2\2\u03af\u0094\3\2\2\2\u03b0\u03b1"+
		"\7k\2\2\u03b1\u03b2\7p\2\2\u03b2\u03b3\7u\2\2\u03b3\u03b4\7v\2\2\u03b4"+
		"\u03b5\7c\2\2\u03b5\u03b6\7p\2\2\u03b6\u03b7\7e\2\2\u03b7\u03b8\7g\2\2"+
		"\u03b8\u03b9\7q\2\2\u03b9\u03ba\7h\2\2\u03ba\u0096\3\2\2\2\u03bb\u03bc"+
		"\7v\2\2\u03bc\u03bd\7{\2\2\u03bd\u03be\7r\2\2\u03be\u03bf\7g\2\2\u03bf"+
		"\u03c0\7q\2\2\u03c0\u03c1\7h\2\2\u03c1\u0098\3\2\2\2\u03c2\u03c3\7e\2"+
		"\2\u03c3\u03c4\7c\2\2\u03c4\u03c5\7u\2\2\u03c5\u03c6\7v\2\2\u03c6\u009a"+
		"\3\2\2\2\u03c7\u03c8\7e\2\2\u03c8\u03c9\7w\2\2\u03c9\u03ca\7t\2\2\u03ca"+
		"\u03cb\7t\2\2\u03cb\u03cc\7g\2\2\u03cc\u03cd\7p\2\2\u03cd\u03ce\7v\2\2"+
		"\u03ce\u03cf\7a\2\2\u03cf\u03d0\7v\2\2\u03d0\u03d1\7k\2\2\u03d1\u03d2"+
		"\7o\2\2\u03d2\u03d3\7g\2\2\u03d3\u03d4\7u\2\2\u03d4\u03d5\7v\2\2\u03d5"+
		"\u03d6\7c\2\2\u03d6\u03d7\7o\2\2\u03d7\u03d8\7r\2\2\u03d8\u009c\3\2\2"+
		"\2\u03d9\u03da\7f\2\2\u03da\u03db\7g\2\2\u03db\u03dc\7n\2\2\u03dc\u03dd"+
		"\7g\2\2\u03dd\u03de\7v\2\2\u03de\u03df\7g\2\2\u03df\u009e\3\2\2\2\u03e0"+
		"\u03e1\7u\2\2\u03e1\u03e2\7p\2\2\u03e2\u03e3\7c\2\2\u03e3\u03e4\7r\2\2"+
		"\u03e4\u03e5\7u\2\2\u03e5\u03e6\7j\2\2\u03e6\u03e7\7q\2\2\u03e7\u03e8"+
		"\7v\2\2\u03e8\u00a0\3\2\2\2\u03e9\u03ea\7u\2\2\u03ea\u03eb\7g\2\2\u03eb"+
		"\u03ec\7v\2\2\u03ec\u00a2\3\2\2\2\u03ed\u03ee\7x\2\2\u03ee\u03ef\7c\2"+
		"\2\u03ef\u03f0\7t\2\2\u03f0\u03f1\7k\2\2\u03f1\u03f2\7c\2\2\u03f2\u03f3"+
		"\7d\2\2\u03f3\u03f4\7n\2\2\u03f4\u03f5\7g\2\2\u03f5\u00a4\3\2\2\2\u03f6"+
		"\u03f7\7v\2\2\u03f7\u03f8\7c\2\2\u03f8\u03f9\7d\2\2\u03f9\u03fa\7n\2\2"+
		"\u03fa\u03fb\7g\2\2\u03fb\u00a6\3\2\2\2\u03fc\u03fd\7w\2\2\u03fd\u03fe"+
		"\7p\2\2\u03fe\u03ff\7v\2\2\u03ff\u0400\7k\2\2\u0400\u0401\7n\2\2\u0401"+
		"\u00a8\3\2\2\2\u0402\u0403\7c\2\2\u0403\u0404\7v\2\2\u0404\u00aa\3\2\2"+
		"\2\u0405\u0406\7k\2\2\u0406\u0407\7p\2\2\u0407\u0408\7f\2\2\u0408\u0409"+
		"\7g\2\2\u0409\u040a\7z\2\2\u040a\u00ac\3\2\2\2\u040b\u040c\7{\2\2\u040c"+
		"\u040d\7g\2\2\u040d\u040e\7c\2\2\u040e\u040f\7t\2\2\u040f\u00ae\3\2\2"+
		"\2\u0410\u0411\7{\2\2\u0411\u0412\7g\2\2\u0412\u0413\7c\2\2\u0413\u0414"+
		"\7t\2\2\u0414\u0415\7u\2\2\u0415\u00b0\3\2\2\2\u0416\u0417\7o\2\2\u0417"+
		"\u0418\7q\2\2\u0418\u0419\7p\2\2\u0419\u041a\7v\2\2\u041a\u041b\7j\2\2"+
		"\u041b\u00b2\3\2\2\2\u041c\u041d\7o\2\2\u041d\u041e\7q\2\2\u041e\u041f"+
		"\7p\2\2\u041f\u0420\7v\2\2\u0420\u0421\7j\2\2\u0421\u0422\7u\2\2\u0422"+
		"\u00b4\3\2\2\2\u0423\u0424\7y\2\2\u0424\u0425\7g\2\2\u0425\u0426\7g\2"+
		"\2\u0426\u0427\7m\2\2\u0427\u00b6\3\2\2\2\u0428\u0429\7y\2\2\u0429\u042a"+
		"\7g\2\2\u042a\u042b\7g\2\2\u042b\u042c\7m\2\2\u042c\u042d\7u\2\2\u042d"+
		"\u00b8\3\2\2\2\u042e\u042f\7f\2\2\u042f\u0430\7c\2\2\u0430\u0431\7{\2"+
		"\2\u0431\u00ba\3\2\2\2\u0432\u0433\7f\2\2\u0433\u0434\7c\2\2\u0434\u0435"+
		"\7{\2\2\u0435\u0436\7u\2\2\u0436\u00bc\3\2\2\2\u0437\u0438\7j\2\2\u0438"+
		"\u0439\7q\2\2\u0439\u043a\7w\2\2\u043a\u043b\7t\2\2\u043b\u00be\3\2\2"+
		"\2\u043c\u043d\7j\2\2\u043d\u043e\7q\2\2\u043e\u043f\7w\2\2\u043f\u0440"+
		"\7t\2\2\u0440\u0441\7u\2\2\u0441\u00c0\3\2\2\2\u0442\u0443\7o\2\2\u0443"+
		"\u0444\7k\2\2\u0444\u0445\7p\2\2\u0445\u0446\7w\2\2\u0446\u0447\7v\2\2"+
		"\u0447\u0448\7g\2\2\u0448\u00c2\3\2\2\2\u0449\u044a\7o\2\2\u044a\u044b"+
		"\7k\2\2\u044b\u044c\7p\2\2\u044c\u044d\7w\2\2\u044d\u044e\7v\2\2\u044e"+
		"\u044f\7g\2\2\u044f\u0450\7u\2\2\u0450\u00c4\3\2\2\2\u0451\u0452\7u\2"+
		"\2\u0452\u0453\7g\2\2\u0453\u0454\7e\2\2\u0454\u00c6\3\2\2\2\u0455\u0456"+
		"\7u\2\2\u0456\u0457\7g\2\2\u0457\u0458\7e\2\2\u0458\u0459\7q\2\2\u0459"+
		"\u045a\7p\2\2\u045a\u045b\7f\2\2\u045b\u00c8\3\2\2\2\u045c\u045d\7u\2"+
		"\2\u045d\u045e\7g\2\2\u045e\u045f\7e\2\2\u045f\u0460\7q\2\2\u0460\u0461"+
		"\7p\2\2\u0461\u0462\7f\2\2\u0462\u0463\7u\2\2\u0463\u00ca\3\2\2\2\u0464"+
		"\u0465\7o\2\2\u0465\u0466\7u\2\2\u0466\u0467\7g\2\2\u0467\u0468\7e\2\2"+
		"\u0468\u00cc\3\2\2\2\u0469\u046a\7o\2\2\u046a\u046b\7k\2\2\u046b\u046c"+
		"\7n\2\2\u046c\u046d\7n\2\2\u046d\u046e\7k\2\2\u046e\u046f\7u\2\2\u046f"+
		"\u0470\7g\2\2\u0470\u0471\7e\2\2\u0471\u0472\7q\2\2\u0472\u0473\7p\2\2"+
		"\u0473\u0474\7f\2\2\u0474\u00ce\3\2\2\2\u0475\u0476\7o\2\2\u0476\u0477"+
		"\7k\2\2\u0477\u0478\7n\2\2\u0478\u0479\7n\2\2\u0479\u047a\7k\2\2\u047a"+
		"\u047b\7u\2\2\u047b\u047c\7g\2\2\u047c\u047d\7e\2\2\u047d\u047e\7q\2\2"+
		"\u047e\u047f\7p\2\2\u047f\u0480\7f\2\2\u0480\u0481\7u\2\2\u0481\u00d0"+
		"\3\2\2\2\u0482\u0483\7v\2\2\u0483\u0484\7t\2\2\u0484\u0485\7w\2\2\u0485"+
		"\u0486\7g\2\2\u0486\u00d2\3\2\2\2\u0487\u0488\7h\2\2\u0488\u0489\7c\2"+
		"\2\u0489\u048a\7n\2\2\u048a\u048b\7u\2\2\u048b\u048c\7g\2\2\u048c\u00d4"+
		"\3\2\2\2\u048d\u048e\7p\2\2\u048e\u048f\7w\2\2\u048f\u0490\7n\2\2\u0490"+
		"\u0491\7n\2\2\u0491\u00d6\3\2\2\2\u0492\u0493\7n\2\2\u0493\u0494\7k\2"+
		"\2\u0494\u0495\7o\2\2\u0495\u0496\7k\2\2\u0496\u0497\7v\2\2\u0497\u00d8"+
		"\3\2\2\2\u0498\u0499\7q\2\2\u0499\u049a\7h\2\2\u049a\u049b\7h\2\2\u049b"+
		"\u049c\7u\2\2\u049c\u049d\7g\2\2\u049d\u049e\7v\2\2\u049e\u00da\3\2\2"+
		"\2\u049f\u04a0\7w\2\2\u04a0\u04a1\7r\2\2\u04a1\u04a2\7f\2\2\u04a2\u04a3"+
		"\7c\2\2\u04a3\u04a4\7v\2\2\u04a4\u04a5\7g\2\2\u04a5\u00dc\3\2\2\2\u04a6"+
		"\u04a7\7o\2\2\u04a7\u04a8\7c\2\2\u04a8\u04a9\7v\2\2\u04a9\u04aa\7e\2\2"+
		"\u04aa\u04ab\7j\2\2\u04ab\u04ac\7a\2\2\u04ac\u04ad\7t\2\2\u04ad\u04ae"+
		"\7g\2\2\u04ae\u04af\7e\2\2\u04af\u04b0\7q\2\2\u04b0\u04b1\7i\2\2\u04b1"+
		"\u04b2\7p\2\2\u04b2\u04b3\7k\2\2\u04b3\u04b4\7|\2\2\u04b4\u04b5\7g\2\2"+
		"\u04b5\u00de\3\2\2\2\u04b6\u04b7\7o\2\2\u04b7\u04b8\7c\2\2\u04b8\u04b9"+
		"\7v\2\2\u04b9\u04ba\7e\2\2\u04ba\u04bb\7j\2\2\u04bb\u04bc\7a\2\2\u04bc"+
		"\u04bd\7t\2\2\u04bd\u04be\7g\2\2\u04be\u04bf\7e\2\2\u04bf\u04c0\7q\2\2"+
		"\u04c0\u04c1\7i\2\2\u04c1\u04c2\7p\2\2\u04c2\u04c3\7k\2\2\u04c3\u04c4"+
		"\7|\2\2\u04c4\u04c5\7g\2\2\u04c5\u04c6\7a\2\2\u04c6\u04c7\7r\2\2\u04c7"+
		"\u04c8\7g\2\2\u04c8\u04c9\7t\2\2\u04c9\u04ca\7o\2\2\u04ca\u04cb\7w\2\2"+
		"\u04cb\u04cc\7v\2\2\u04cc\u04cd\7g\2\2\u04cd\u00e0\3\2\2\2\u04ce\u04cf"+
		"\7o\2\2\u04cf\u04d0\7g\2\2\u04d0\u04d1\7c\2\2\u04d1\u04d2\7u\2\2\u04d2"+
		"\u04d3\7w\2\2\u04d3\u04d4\7t\2\2\u04d4\u04d5\7g\2\2\u04d5\u04d6\7u\2\2"+
		"\u04d6\u00e2\3\2\2\2\u04d7\u04d8\7f\2\2\u04d8\u04d9\7g\2\2\u04d9\u04da"+
		"\7h\2\2\u04da\u04db\7k\2\2\u04db\u04dc\7p\2\2\u04dc\u04dd\7g\2\2\u04dd"+
		"\u00e4\3\2\2\2\u04de\u04df\7r\2\2\u04df\u04e0\7c\2\2\u04e0\u04e1\7t\2"+
		"\2\u04e1\u04e2\7v\2\2\u04e2\u04e3\7k\2\2\u04e3\u04e4\7v\2\2\u04e4\u04e5"+
		"\7k\2\2\u04e5\u04e6\7q\2\2\u04e6\u04e7\7p\2\2\u04e7\u00e6\3\2\2\2\u04e8"+
		"\u04e9\7o\2\2\u04e9\u04ea\7c\2\2\u04ea\u04eb\7v\2\2\u04eb\u04ec\7e\2\2"+
		"\u04ec\u04ed\7j\2\2\u04ed\u04ee\7g\2\2\u04ee\u04ef\7u\2\2\u04ef\u00e8"+
		"\3\2\2\2\u04f0\u04f1\7c\2\2\u04f1\u04f2\7h\2\2\u04f2\u04f3\7v\2\2\u04f3"+
		"\u04f4\7g\2\2\u04f4\u04f5\7t\2\2\u04f5\u00ea\3\2\2\2\u04f6\u04f7\7h\2"+
		"\2\u04f7\u04f8\7q\2\2\u04f8\u04f9\7t\2\2\u04f9\u00ec\3\2\2\2\u04fa\u04fb"+
		"\7y\2\2\u04fb\u04fc\7j\2\2\u04fc\u04fd\7k\2\2\u04fd\u04fe\7n\2\2\u04fe"+
		"\u04ff\7g\2\2\u04ff\u00ee\3\2\2\2\u0500\u0501\7w\2\2\u0501\u0502\7u\2"+
		"\2\u0502\u0503\7k\2\2\u0503\u0504\7p\2\2\u0504\u0505\7i\2\2\u0505\u00f0"+
		"\3\2\2\2\u0506\u0507\7o\2\2\u0507\u0508\7g\2\2\u0508\u0509\7t\2\2\u0509"+
		"\u050a\7i\2\2\u050a\u050b\7g\2\2\u050b\u00f2\3\2\2\2\u050c\u050d\7o\2"+
		"\2\u050d\u050e\7c\2\2\u050e\u050f\7v\2\2\u050f\u0510\7e\2\2\u0510\u0511"+
		"\7j\2\2\u0511\u0512\7g\2\2\u0512\u0513\7f\2\2\u0513\u00f4\3\2\2\2\u0514"+
		"\u0515\7g\2\2\u0515\u0516\7z\2\2\u0516\u0517\7r\2\2\u0517\u0518\7t\2\2"+
		"\u0518\u0519\7g\2\2\u0519\u051a\7u\2\2\u051a\u051b\7u\2\2\u051b\u051c"+
		"\7k\2\2\u051c\u051d\7q\2\2\u051d\u051e\7p\2\2\u051e\u00f6\3\2\2\2\u051f"+
		"\u0520\7p\2\2\u0520\u0521\7g\2\2\u0521\u0522\7y\2\2\u0522\u00f8\3\2\2"+
		"\2\u0523\u0524\7u\2\2\u0524\u0525\7v\2\2\u0525\u0526\7c\2\2\u0526\u0527"+
		"\7t\2\2\u0527\u0528\7v\2\2\u0528\u00fa\3\2\2\2\u0529\u052a\7e\2\2\u052a"+
		"\u052b\7q\2\2\u052b\u052c\7p\2\2\u052c\u052d\7v\2\2\u052d\u052e\7g\2\2"+
		"\u052e\u052f\7z\2\2\u052f\u0530\7v\2\2\u0530\u00fc\3\2\2\2\u0531\u0532"+
		"\7k\2\2\u0532\u0533\7p\2\2\u0533\u0534\7k\2\2\u0534\u0535\7v\2\2\u0535"+
		"\u0536\7k\2\2\u0536\u0537\7c\2\2\u0537\u0538\7v\2\2\u0538\u0539\7g\2\2"+
		"\u0539\u053a\7f\2\2\u053a\u00fe\3\2\2\2\u053b\u053c\7v\2\2\u053c\u053d"+
		"\7g\2\2\u053d\u053e\7t\2\2\u053e\u053f\7o\2\2\u053f\u0540\7k\2\2\u0540"+
		"\u0541\7p\2\2\u0541\u0542\7c\2\2\u0542\u0543\7v\2\2\u0543\u0544\7g\2\2"+
		"\u0544\u0545\7f\2\2\u0545\u0100\3\2\2\2\u0546\u0547\7f\2\2\u0547\u0548"+
		"\7c\2\2\u0548\u0549\7v\2\2\u0549\u054a\7c\2\2\u054a\u054b\7h\2\2\u054b"+
		"\u054c\7n\2\2\u054c\u054d\7q\2\2\u054d\u054e\7y\2\2\u054e\u0102\3\2\2"+
		"\2\u054f\u0550\7e\2\2\u0550\u0551\7w\2\2\u0551\u0552\7d\2\2\u0552\u0553"+
		"\7g\2\2\u0553\u0104\3\2\2\2\u0554\u0555\7t\2\2\u0555\u0556\7q\2\2\u0556"+
		"\u0557\7n\2\2\u0557\u0558\7n\2\2\u0558\u0559\7w\2\2\u0559\u055a\7r\2\2"+
		"\u055a\u0106\3\2\2\2\u055b\u055c\7i\2\2\u055c\u055d\7t\2\2\u055d\u055e"+
		"\7q\2\2\u055e\u055f\7w\2\2\u055f\u0560\7r\2\2\u0560\u0561\7k\2\2\u0561"+
		"\u0562\7p\2\2\u0562\u0563\7i\2\2\u0563\u0108\3\2\2\2\u0564\u0565\7i\2"+
		"\2\u0565\u0566\7t\2\2\u0566\u0567\7q\2\2\u0567\u0568\7w\2\2\u0568\u0569"+
		"\7r\2\2\u0569\u056a\7k\2\2\u056a\u056b\7p\2\2\u056b\u056c\7i\2\2\u056c"+
		"\u056d\7a\2\2\u056d\u056e\7k\2\2\u056e\u056f\7f\2\2\u056f\u010a\3\2\2"+
		"\2\u0570\u0571\7u\2\2\u0571\u0572\7g\2\2\u0572\u0573\7v\2\2\u0573\u0574"+
		"\7u\2\2\u0574\u010c\3\2\2\2\u0575\u0576\7/\2\2\u0576\u0577\7]\2\2\u0577"+
		"\u010e\3\2\2\2\u0578\u0579\7_\2\2\u0579\u057a\7@\2\2\u057a\u0110\3\2\2"+
		"\2\u057b\u057c\7/\2\2\u057c\u057d\7@\2\2\u057d\u0112\3\2\2\2\u057e\u057f"+
		"\7?\2\2\u057f\u0580\7@\2\2\u0580\u0114\3\2\2\2\u0581\u0582\7?\2\2\u0582"+
		"\u0116\3\2\2\2\u0583\u0584\7>\2\2\u0584\u0585\7@\2\2\u0585\u0118\3\2\2"+
		"\2\u0586\u0587\7A\2\2\u0587\u011a\3\2\2\2\u0588\u0589\7*\2\2\u0589\u011c"+
		"\3\2\2\2\u058a\u058b\7+\2\2\u058b\u011e\3\2\2\2\u058c\u058d\7]\2\2\u058d"+
		"\u0120\3\2\2\2\u058e\u058f\7_\2\2\u058f\u0122\3\2\2\2\u0590\u0591\7}\2"+
		"\2\u0591\u0124\3\2\2\2\u0592\u0593\7\177\2\2\u0593\u0126\3\2\2\2\u0594"+
		"\u0595\7<\2\2\u0595\u0128\3\2\2\2\u0596\u0597\7.\2\2\u0597\u012a\3\2\2"+
		"\2\u0598\u0599\7?\2\2\u0599\u059a\7?\2\2\u059a\u012c\3\2\2\2\u059b\u059c"+
		"\7#\2\2\u059c\u012e\3\2\2\2\u059d\u059e\7\u0080\2\2\u059e\u0130\3\2\2"+
		"\2\u059f\u05a0\7#\2\2\u05a0\u05a1\7?\2\2\u05a1\u0132\3\2\2\2\u05a2\u05a3"+
		"\7\61\2\2\u05a3\u0134\3\2\2\2\u05a4\u05a5\7\61\2\2\u05a5\u05a6\7?\2\2"+
		"\u05a6\u0136\3\2\2\2\u05a7\u05a8\7-\2\2\u05a8\u0138\3\2\2\2\u05a9\u05aa"+
		"\7-\2\2\u05aa\u05ab\7?\2\2\u05ab\u013a\3\2\2\2\u05ac\u05ad\7-\2\2\u05ad"+
		"\u05ae\7-\2\2\u05ae\u013c\3\2\2\2\u05af\u05b0\7/\2\2\u05b0\u013e\3\2\2"+
		"\2\u05b1\u05b2\7/\2\2\u05b2\u05b3\7?\2\2\u05b3\u0140\3\2\2\2\u05b4\u05b5"+
		"\7/\2\2\u05b5\u05b6\7/\2\2\u05b6\u0142\3\2\2\2\u05b7\u05b8\7,\2\2\u05b8"+
		"\u0144\3\2\2\2\u05b9\u05ba\7,\2\2\u05ba\u05bb\7?\2\2\u05bb\u0146\3\2\2"+
		"\2\u05bc\u05bd\7\'\2\2\u05bd\u0148\3\2\2\2\u05be\u05bf\7\'\2\2\u05bf\u05c0"+
		"\7?\2\2\u05c0\u014a\3\2\2\2\u05c1\u05c2\7@\2\2\u05c2\u05c3\7?\2\2\u05c3"+
		"\u014c\3\2\2\2\u05c4\u05c5\7@\2\2\u05c5\u014e\3\2\2\2\u05c6\u05c7\7>\2"+
		"\2\u05c7\u05c8\7?\2\2\u05c8\u0150\3\2\2\2\u05c9\u05ca\7>\2\2\u05ca\u0152"+
		"\3\2\2\2\u05cb\u05cc\7`\2\2\u05cc\u0154\3\2\2\2\u05cd\u05ce\7`\2\2\u05ce"+
		"\u05cf\7?\2\2\u05cf\u0156\3\2\2\2\u05d0\u05d1\7~\2\2\u05d1\u0158\3\2\2"+
		"\2\u05d2\u05d3\7~\2\2\u05d3\u05d4\7?\2\2\u05d4\u015a\3\2\2\2\u05d5\u05d6"+
		"\7~\2\2\u05d6\u05d7\7~\2\2\u05d7\u015c\3\2\2\2\u05d8\u05d9\7(\2\2\u05d9"+
		"\u015e\3\2\2\2\u05da\u05db\7(\2\2\u05db\u05dc\7?\2\2\u05dc\u0160\3\2\2"+
		"\2\u05dd\u05de\7(\2\2\u05de\u05df\7(\2\2\u05df\u0162\3\2\2\2\u05e0\u05e1"+
		"\7=\2\2\u05e1\u0164\3\2\2\2\u05e2\u05e3\7\60\2\2\u05e3\u0166\3\2\2\2\u05e4"+
		"\u05e5\7\u1901\2\2\u05e5\u0168\3\2\2\2\u05e6\u05e7\7\u1900\2\2\u05e7\u016a"+
		"\3\2\2\2\u05e8\u05e9\7\u18ff\2\2\u05e9\u016c\3\2\2\2\u05ea\u05eb\7^\2"+
		"\2\u05eb\u016e\3\2\2\2\u05ec\u05ed\7b\2\2\u05ed\u0170\3\2\2\2\u05ee\u05ef"+
		"\7B\2\2\u05ef\u0172\3\2\2\2\u05f0\u05f1\7%\2\2\u05f1\u0174\3\2\2\2\u05f2"+
		"\u05f4\t\2\2\2\u05f3\u05f2\3\2\2\2\u05f4\u05f5\3\2\2\2\u05f5\u05f3\3\2"+
		"\2\2\u05f5\u05f6\3\2\2\2\u05f6\u05f7\3\2\2\2\u05f7\u05f8\b\u00bb\2\2\u05f8"+
		"\u0176\3\2\2\2\u05f9\u05fa\7\61\2\2\u05fa\u05fb\7\61\2\2\u05fb\u05ff\3"+
		"\2\2\2\u05fc\u05fe\n\3\2\2\u05fd\u05fc\3\2\2\2\u05fe\u0601\3\2\2\2\u05ff"+
		"\u05fd\3\2\2\2\u05ff\u0600\3\2\2\2\u0600\u0607\3\2\2\2\u0601\u05ff\3\2"+
		"\2\2\u0602\u0608\7\f\2\2\u0603\u0605\7\17\2\2\u0604\u0606\7\f\2\2\u0605"+
		"\u0604\3\2\2\2\u0605\u0606\3\2\2\2\u0606\u0608\3\2\2\2\u0607\u0602\3\2"+
		"\2\2\u0607\u0603\3\2\2\2\u0607\u0608\3\2\2\2\u0608\u0609\3\2\2\2\u0609"+
		"\u060a\b\u00bc\2\2\u060a\u0178\3\2\2\2\u060b\u060c\7\61\2\2\u060c\u060d"+
		"\7,\2\2\u060d\u0611\3\2\2\2\u060e\u0610\13\2\2\2\u060f\u060e\3\2\2\2\u0610"+
		"\u0613\3\2\2\2\u0611\u0612\3\2\2\2\u0611\u060f\3\2\2\2\u0612\u0614\3\2"+
		"\2\2\u0613\u0611\3\2\2\2\u0614\u0615\7,\2\2\u0615\u0616\7\61\2\2\u0616"+
		"\u0617\3\2\2\2\u0617\u0618\b\u00bd\2\2\u0618\u017a\3\2\2\2\u0619\u061e"+
		"\7b\2\2\u061a\u061d\5\u0181\u00c1\2\u061b\u061d\n\4\2\2\u061c\u061a\3"+
		"\2\2\2\u061c\u061b\3\2\2\2\u061d\u0620\3\2\2\2\u061e\u061c\3\2\2\2\u061e"+
		"\u061f\3\2\2\2\u061f\u0621\3\2\2\2\u0620\u061e\3\2\2\2\u0621\u0622\7b"+
		"\2\2\u0622\u017c\3\2\2\2\u0623\u0628\7)\2\2\u0624\u0627\5\u0181\u00c1"+
		"\2\u0625\u0627\n\5\2\2\u0626\u0624\3\2\2\2\u0626\u0625\3\2\2\2\u0627\u062a"+
		"\3\2\2\2\u0628\u0626\3\2\2\2\u0628\u0629\3\2\2\2\u0629\u062b\3\2\2\2\u062a"+
		"\u0628\3\2\2\2\u062b\u062c\7)\2\2\u062c\u017e\3\2\2\2\u062d\u0632\7$\2"+
		"\2\u062e\u0631\5\u0181\u00c1\2\u062f\u0631\n\6\2\2\u0630\u062e\3\2\2\2"+
		"\u0630\u062f\3\2\2\2\u0631\u0634\3\2\2\2\u0632\u0630\3\2\2\2\u0632\u0633"+
		"\3\2\2\2\u0633\u0635\3\2\2\2\u0634\u0632\3\2\2\2\u0635\u0636\7$\2\2\u0636"+
		"\u0180\3\2\2\2\u0637\u063c\7^\2\2\u0638\u063d\t\7\2\2\u0639\u063d\5\u018b"+
		"\u00c6\2\u063a\u063d\5\u0189\u00c5\2\u063b\u063d\13\2\2\2\u063c\u0638"+
		"\3\2\2\2\u063c\u0639\3\2\2\2\u063c\u063a\3\2\2\2\u063c\u063b\3\2\2\2\u063d"+
		"\u0182\3\2\2\2\u063e\u0642\t\b\2\2\u063f\u0641\t\t\2\2\u0640\u063f\3\2"+
		"\2\2\u0641\u0644\3\2\2\2\u0642\u0640\3\2\2\2\u0642\u0643\3\2\2\2\u0643"+
		"\u0184\3\2\2\2\u0644\u0642\3\2\2\2\u0645\u064a\5\u018d\u00c7\2\u0646\u064a"+
		"\5\u018f\u00c8\2\u0647\u064a\5\u0191\u00c9\2\u0648\u064a\5\u0193\u00ca"+
		"\2\u0649\u0645\3\2\2\2\u0649\u0646\3\2\2\2\u0649\u0647\3\2\2\2\u0649\u0648"+
		"\3\2\2\2\u064a\u0186\3\2\2\2\u064b\u064e\5\u01bb\u00de\2\u064c\u064e\5"+
		"\u01c7\u00e4\2\u064d\u064b\3\2\2\2\u064d\u064c\3\2\2\2\u064e\u0188\3\2"+
		"\2\2\u064f\u0650\7^\2\2\u0650\u0651\4\62\65\2\u0651\u0652\4\629\2\u0652"+
		"\u0659\4\629\2\u0653\u0654\7^\2\2\u0654\u0655\4\629\2\u0655\u0659\4\62"+
		"9\2\u0656\u0657\7^\2\2\u0657\u0659\4\629\2\u0658\u064f\3\2\2\2\u0658\u0653"+
		"\3\2\2\2\u0658\u0656\3\2\2\2\u0659\u018a\3\2\2\2\u065a\u065b\7^\2\2\u065b"+
		"\u065c\7w\2\2\u065c\u065d\5\u01a7\u00d4\2\u065d\u065e\5\u01a7\u00d4\2"+
		"\u065e\u065f\5\u01a7\u00d4\2\u065f\u0660\5\u01a7\u00d4\2\u0660\u018c\3"+
		"\2\2\2\u0661\u0663\5\u0197\u00cc\2\u0662\u0664\5\u0195\u00cb\2\u0663\u0662"+
		"\3\2\2\2\u0663\u0664\3\2\2\2\u0664\u018e\3\2\2\2\u0665\u0667\5\u01a3\u00d2"+
		"\2\u0666\u0668\5\u0195\u00cb\2\u0667\u0666\3\2\2\2\u0667\u0668\3\2\2\2"+
		"\u0668\u0190\3\2\2\2\u0669\u066b\5\u01ab\u00d6\2\u066a\u066c\5\u0195\u00cb"+
		"\2\u066b\u066a\3\2\2\2\u066b\u066c\3\2\2\2\u066c\u0192\3\2\2\2\u066d\u066f"+
		"\5\u01b3\u00da\2\u066e\u0670\5\u0195\u00cb\2\u066f\u066e\3\2\2\2\u066f"+
		"\u0670\3\2\2\2\u0670\u0194\3\2\2\2\u0671\u0672\t\n\2\2\u0672\u0196\3\2"+
		"\2\2\u0673\u0684\7\62\2\2\u0674\u0676\7\62\2\2\u0675\u0674\3\2\2\2\u0676"+
		"\u0679\3\2\2\2\u0677\u0675\3\2\2\2\u0677\u0678\3\2\2\2\u0678\u067a\3\2"+
		"\2\2\u0679\u0677\3\2\2\2\u067a\u0681\5\u019d\u00cf\2\u067b\u067d\5\u0199"+
		"\u00cd\2\u067c\u067b\3\2\2\2\u067c\u067d\3\2\2\2\u067d\u0682\3\2\2\2\u067e"+
		"\u067f\5\u01a1\u00d1\2\u067f\u0680\5\u0199\u00cd\2\u0680\u0682\3\2\2\2"+
		"\u0681\u067c\3\2\2\2\u0681\u067e\3\2\2\2\u0682\u0684\3\2\2\2\u0683\u0673"+
		"\3\2\2\2\u0683\u0677\3\2\2\2\u0684\u0198\3\2\2\2\u0685\u068d\5\u019b\u00ce"+
		"\2\u0686\u0688\5\u019f\u00d0\2\u0687\u0686\3\2\2\2\u0688\u068b\3\2\2\2"+
		"\u0689\u0687\3\2\2\2\u0689\u068a\3\2\2\2\u068a\u068c\3\2\2\2\u068b\u0689"+
		"\3\2\2\2\u068c\u068e\5\u019b\u00ce\2\u068d\u0689\3\2\2\2\u068d\u068e\3"+
		"\2\2\2\u068e\u019a\3\2\2\2\u068f\u0692\7\62\2\2\u0690\u0692\5\u019d\u00cf"+
		"\2\u0691\u068f\3\2\2\2\u0691\u0690\3\2\2\2\u0692\u019c\3\2\2\2\u0693\u0694"+
		"\t\13\2\2\u0694\u019e\3\2\2\2\u0695\u0698\5\u019b\u00ce\2\u0696\u0698"+
		"\7a\2\2\u0697\u0695\3\2\2\2\u0697\u0696\3\2\2\2\u0698\u01a0\3\2\2\2\u0699"+
		"\u069b\7a\2\2\u069a\u0699\3\2\2\2\u069b\u069c\3\2\2\2\u069c\u069a\3\2"+
		"\2\2\u069c\u069d\3\2\2\2\u069d\u01a2\3\2\2\2\u069e\u069f\7\62\2\2\u069f"+
		"\u06a0\t\f\2\2\u06a0\u06a1\5\u01a5\u00d3\2\u06a1\u01a4\3\2\2\2\u06a2\u06aa"+
		"\5\u01a7\u00d4\2\u06a3\u06a5\5\u01a9\u00d5\2\u06a4\u06a3\3\2\2\2\u06a5"+
		"\u06a8\3\2\2\2\u06a6\u06a4\3\2\2\2\u06a6\u06a7\3\2\2\2\u06a7\u06a9\3\2"+
		"\2\2\u06a8\u06a6\3\2\2\2\u06a9\u06ab\5\u01a7\u00d4\2\u06aa\u06a6\3\2\2"+
		"\2\u06aa\u06ab\3\2\2\2\u06ab\u01a6\3\2\2\2\u06ac\u06ad\t\r\2\2\u06ad\u01a8"+
		"\3\2\2\2\u06ae\u06b1\5\u01a7\u00d4\2\u06af\u06b1\7a\2\2\u06b0\u06ae\3"+
		"\2\2\2\u06b0\u06af\3\2\2\2\u06b1\u01aa\3\2\2\2\u06b2\u06b4\7\62\2\2\u06b3"+
		"\u06b5\5\u01a1\u00d1\2\u06b4\u06b3\3\2\2\2\u06b4\u06b5\3\2\2\2\u06b5\u06b6"+
		"\3\2\2\2\u06b6\u06b7\5\u01ad\u00d7\2\u06b7\u01ac\3\2\2\2\u06b8\u06c0\5"+
		"\u01af\u00d8\2\u06b9\u06bb\5\u01b1\u00d9\2\u06ba\u06b9\3\2\2\2\u06bb\u06be"+
		"\3\2\2\2\u06bc\u06ba\3\2\2\2\u06bc\u06bd\3\2\2\2\u06bd\u06bf\3\2\2\2\u06be"+
		"\u06bc\3\2\2\2\u06bf\u06c1\5\u01af\u00d8\2\u06c0\u06bc\3\2\2\2\u06c0\u06c1"+
		"\3\2\2\2\u06c1\u01ae\3\2\2\2\u06c2\u06c3\t\16\2\2\u06c3\u01b0\3\2\2\2"+
		"\u06c4\u06c7\5\u01af\u00d8\2\u06c5\u06c7\7a\2\2\u06c6\u06c4\3\2\2\2\u06c6"+
		"\u06c5\3\2\2\2\u06c7\u01b2\3\2\2\2\u06c8\u06c9\7\62\2\2\u06c9\u06ca\t"+
		"\17\2\2\u06ca\u06cb\5\u01b5\u00db\2\u06cb\u01b4\3\2\2\2\u06cc\u06d4\5"+
		"\u01b7\u00dc\2\u06cd\u06cf\5\u01b9\u00dd\2\u06ce\u06cd\3\2\2\2\u06cf\u06d2"+
		"\3\2\2\2\u06d0\u06ce\3\2\2\2\u06d0\u06d1\3\2\2\2\u06d1\u06d3\3\2\2\2\u06d2"+
		"\u06d0\3\2\2\2\u06d3\u06d5\5\u01b7\u00dc\2\u06d4\u06d0\3\2\2\2\u06d4\u06d5"+
		"\3\2\2\2\u06d5\u01b6\3\2\2\2\u06d6\u06d7\t\20\2\2\u06d7\u01b8\3\2\2\2"+
		"\u06d8\u06db\5\u01b7\u00dc\2\u06d9\u06db\7a\2\2\u06da\u06d8\3\2\2\2\u06da"+
		"\u06d9\3\2\2\2\u06db\u01ba\3\2\2\2\u06dc\u06dd\5\u0199\u00cd\2\u06dd\u06df"+
		"\7\60\2\2\u06de\u06e0\5\u0199\u00cd\2\u06df\u06de\3\2\2\2\u06df\u06e0"+
		"\3\2\2\2\u06e0\u06e2\3\2\2\2\u06e1\u06e3\5\u01bd\u00df\2\u06e2\u06e1\3"+
		"\2\2\2\u06e2\u06e3\3\2\2\2\u06e3\u06e5\3\2\2\2\u06e4\u06e6\5\u01c5\u00e3"+
		"\2\u06e5\u06e4\3\2\2\2\u06e5\u06e6\3\2\2\2\u06e6\u06f8\3\2\2\2\u06e7\u06e8"+
		"\7\60\2\2\u06e8\u06ea\5\u0199\u00cd\2\u06e9\u06eb\5\u01bd\u00df\2\u06ea"+
		"\u06e9\3\2\2\2\u06ea\u06eb\3\2\2\2\u06eb\u06ed\3\2\2\2\u06ec\u06ee\5\u01c5"+
		"\u00e3\2\u06ed\u06ec\3\2\2\2\u06ed\u06ee\3\2\2\2\u06ee\u06f8\3\2\2\2\u06ef"+
		"\u06f0\5\u0199\u00cd\2\u06f0\u06f2\5\u01bd\u00df\2\u06f1\u06f3\5\u01c5"+
		"\u00e3\2\u06f2\u06f1\3\2\2\2\u06f2\u06f3\3\2\2\2\u06f3\u06f8\3\2\2\2\u06f4"+
		"\u06f5\5\u0199\u00cd\2\u06f5\u06f6\5\u01c5\u00e3\2\u06f6\u06f8\3\2\2\2"+
		"\u06f7\u06dc\3\2\2\2\u06f7\u06e7\3\2\2\2\u06f7\u06ef\3\2\2\2\u06f7\u06f4"+
		"\3\2\2\2\u06f8\u01bc\3\2\2\2\u06f9\u06fa\5\u01bf\u00e0\2\u06fa\u06fb\5"+
		"\u01c1\u00e1\2\u06fb\u01be\3\2\2\2\u06fc\u06fd\t\21\2\2\u06fd\u01c0\3"+
		"\2\2\2\u06fe\u0700\5\u01c3\u00e2\2\u06ff\u06fe\3\2\2\2\u06ff\u0700\3\2"+
		"\2\2\u0700\u0701\3\2\2\2\u0701\u0702\5\u0199\u00cd\2\u0702\u01c2\3\2\2"+
		"\2\u0703\u0704\t\22\2\2\u0704\u01c4\3\2\2\2\u0705\u0706\t\23\2\2\u0706"+
		"\u01c6\3\2\2\2\u0707\u0708\5\u01c9\u00e5\2\u0708\u070a\5\u01cb\u00e6\2"+
		"\u0709\u070b\5\u01c5\u00e3\2\u070a\u0709\3\2\2\2\u070a\u070b\3\2\2\2\u070b"+
		"\u01c8\3\2\2\2\u070c\u070e\5\u01a3\u00d2\2\u070d\u070f\7\60\2\2\u070e"+
		"\u070d\3\2\2\2\u070e\u070f\3\2\2\2\u070f\u0718\3\2\2\2\u0710\u0711\7\62"+
		"\2\2\u0711\u0713\t\f\2\2\u0712\u0714\5\u01a5\u00d3\2\u0713\u0712\3\2\2"+
		"\2\u0713\u0714\3\2\2\2\u0714\u0715\3\2\2\2\u0715\u0716\7\60\2\2\u0716"+
		"\u0718\5\u01a5\u00d3\2\u0717\u070c\3\2\2\2\u0717\u0710\3\2\2\2\u0718\u01ca"+
		"\3\2\2\2\u0719\u071a\5\u01cd\u00e7\2\u071a\u071b\5\u01c1\u00e1\2\u071b"+
		"\u01cc\3\2\2\2\u071c\u071d\t\24\2\2\u071d\u01ce\3\2\2\2\67\2\u05f3\u05f5"+
		"\u05ff\u0605\u0607\u0611\u061c\u061e\u0626\u0628\u0630\u0632\u063c\u0642"+
		"\u0649\u064d\u0658\u0663\u0667\u066b\u066f\u0677\u067c\u0681\u0683\u0689"+
		"\u068d\u0691\u0697\u069c\u06a6\u06aa\u06b0\u06b4\u06bc\u06c0\u06c6\u06d0"+
		"\u06d4\u06da\u06df\u06e2\u06e5\u06ea\u06ed\u06f2\u06f7\u06ff\u070a\u070e"+
		"\u0713\u0717\3\2\3\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}