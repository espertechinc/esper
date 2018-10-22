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
package com.espertech.esper.compiler.internal.util;

import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.compiler.internal.generated.EsperEPL2GrammarLexer;
import com.espertech.esper.compiler.internal.parse.CaseInsensitiveInputStream;
import com.espertech.esper.compiler.internal.parse.ParseHelper;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.espertech.esper.common.internal.epl.historical.database.core.HistoricalEventViewableDatabaseForgeFactory.SAMPLE_WHERECLAUSE_PLACEHOLDER;

public class SQLLexer {
    private static final Logger log = LoggerFactory.getLogger(SQLLexer.class);

    /**
     * Lexes the sample SQL and inserts a "where 1=0" where-clause.
     *
     * @param querySQL to inspect using lexer
     * @return sample SQL with where-clause inserted
     * @throws ExprValidationException to indicate a lexer problem
     */
    public static String lexSampleSQL(String querySQL)
            throws ExprValidationException {
        querySQL = querySQL.replaceAll("\\s\\s+|\\n|\\r", " ");
        CharStream input = new CaseInsensitiveInputStream(querySQL);
        int whereIndex = -1;
        int groupbyIndex = -1;
        int havingIndex = -1;
        int orderByIndex = -1;
        List<Integer> unionIndexes = new ArrayList<Integer>();

        EsperEPL2GrammarLexer lex = ParseHelper.newLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lex);
        tokens.fill();
        List tokenList = tokens.getTokens();

        for (int i = 0; i < tokenList.size(); i++) {
            Token token = (Token) tokenList.get(i);
            if ((token == null) || token.getText() == null) {
                break;
            }
            String text = token.getText().toLowerCase(Locale.ENGLISH).trim();
            if (text.equals("where")) {
                whereIndex = token.getCharPositionInLine() + 1;
            }
            if (text.equals("group")) {
                groupbyIndex = token.getCharPositionInLine() + 1;
            }
            if (text.equals("having")) {
                havingIndex = token.getCharPositionInLine() + 1;
            }
            if (text.equals("order")) {
                orderByIndex = token.getCharPositionInLine() + 1;
            }
            if (text.equals("union")) {
                unionIndexes.add(token.getCharPositionInLine() + 1);
            }
        }

        // If we have a union, break string into subselects and process each
        if (unionIndexes.size() != 0) {
            StringWriter changedSQL = new StringWriter();
            int lastIndex = 0;
            for (int i = 0; i < unionIndexes.size(); i++) {
                int index = unionIndexes.get(i);
                String fragment;
                if (i > 0) {
                    fragment = querySQL.substring(lastIndex + 5, index - 1);
                } else {
                    fragment = querySQL.substring(lastIndex, index - 1);
                }
                String lexedFragment = lexSampleSQL(fragment);

                if (i > 0) {
                    changedSQL.append("union ");
                }
                changedSQL.append(lexedFragment);
                lastIndex = index - 1;
            }

            // last part after last union
            String fragment = querySQL.substring(lastIndex + 5, querySQL.length());
            String lexedFragment = lexSampleSQL(fragment);
            changedSQL.append("union ");
            changedSQL.append(lexedFragment);

            return changedSQL.toString();
        }

        // Found a where clause, simplest cases
        if (whereIndex != -1) {
            StringWriter changedSQL = new StringWriter();
            String prefix = querySQL.substring(0, whereIndex + 5);
            String suffix = querySQL.substring(whereIndex + 5, querySQL.length());
            changedSQL.write(prefix);
            changedSQL.write("1=0 and ");
            changedSQL.write(suffix);
            return changedSQL.toString();
        }

        // No where clause, find group-by
        int insertIndex;
        if (groupbyIndex != -1) {
            insertIndex = groupbyIndex;
        } else if (havingIndex != -1) {
            insertIndex = havingIndex;
        } else if (orderByIndex != -1) {
            insertIndex = orderByIndex;
        } else {
            StringWriter changedSQL = new StringWriter();
            changedSQL.write(querySQL);
            changedSQL.write(" where 1=0 ");
            return changedSQL.toString();
        }

        try {
            StringWriter changedSQL = new StringWriter();
            String prefix = querySQL.substring(0, insertIndex - 1);
            changedSQL.write(prefix);
            changedSQL.write("where 1=0 ");
            String suffix = querySQL.substring(insertIndex - 1, querySQL.length());
            changedSQL.write(suffix);
            return changedSQL.toString();
        } catch (Exception ex) {
            String text = "Error constructing sample SQL to retrieve metadata for JDBC-drivers that don't support metadata, consider using the " + SAMPLE_WHERECLAUSE_PLACEHOLDER + " placeholder or providing a sample SQL";
            log.error(text, ex);
            throw new ExprValidationException(text, ex);
        }
    }
}
