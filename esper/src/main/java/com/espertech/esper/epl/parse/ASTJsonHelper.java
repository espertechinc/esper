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

import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.generated.EsperEPL2GrammarParser;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Walker to annotation stuctures.
 */
public class ASTJsonHelper {
    public static Object walk(CommonTokenStream tokenStream, EsperEPL2GrammarParser.JsonvalueContext node) throws ASTWalkException {
        if (node.constant() != null) {
            EsperEPL2GrammarParser.ConstantContext constCtx = node.constant();
            if (constCtx.stringconstant() != null) {
                return extractString(constCtx.stringconstant().getText());
            } else {
                return ASTConstantHelper.parse(constCtx.getChild(0));
            }
        } else if (node.jsonobject() != null) {
            return walkObject(tokenStream, node.jsonobject());
        } else if (node.jsonarray() != null) {
            return walkArray(tokenStream, node.jsonarray());
        }
        throw ASTWalkException.from("Encountered unexpected node type in json tree", tokenStream, node);
    }

    public static Map<String, Object> walkObject(CommonTokenStream tokenStream, EsperEPL2GrammarParser.JsonobjectContext ctx) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        List<EsperEPL2GrammarParser.JsonpairContext> pairs = ctx.jsonmembers().jsonpair();
        for (EsperEPL2GrammarParser.JsonpairContext pair : pairs) {
            Pair<String, Object> value = walkJSONField(tokenStream, pair);
            map.put(value.getFirst(), value.getSecond());
        }
        return map;
    }

    public static List<Object> walkArray(CommonTokenStream tokenStream, EsperEPL2GrammarParser.JsonarrayContext ctx) {
        List<Object> list = new ArrayList<Object>();
        if (ctx.jsonelements() == null) {
            return list;
        }
        List<EsperEPL2GrammarParser.JsonvalueContext> values = ctx.jsonelements().jsonvalue();
        for (EsperEPL2GrammarParser.JsonvalueContext value : values) {
            Object val = walk(tokenStream, value);
            list.add(val);
        }
        return list;
    }

    private static Pair<String, Object> walkJSONField(CommonTokenStream tokenStream, EsperEPL2GrammarParser.JsonpairContext ctx) {
        String label;
        if (ctx.stringconstant() != null) {
            label = extractString(ctx.stringconstant().getText());
        } else {
            label = ctx.keywordAllowedIdent().getText();
        }
        Object value = walk(tokenStream, ctx.jsonvalue());
        return new Pair<String, Object>(label, value);
    }

    private static String extractString(String text) {
        StringBuffer sb = new StringBuffer(text);
        int startPoint = 1;
        for (;;) {
            int slashIndex = sb.indexOf("\\", startPoint);
            if (slashIndex == -1) {
                break;
            }
            char escapeType = sb.charAt(slashIndex + 1);
            switch (escapeType) {
                case 'u':
                    String unicode = extractUnicode(sb, slashIndex);
                    sb.replace(slashIndex, slashIndex + 6, unicode); // backspace
                    break; // back to the loop

                // note: Java's character escapes match JSON's, which is why it looks like we're replacing
                // "\b" with "\b". We're actually replacing 2 characters (slash-b) with one (backspace).
                case 'b':
                    sb.replace(slashIndex, slashIndex + 2, "\b");
                    break;
                case 't':
                    sb.replace(slashIndex, slashIndex + 2, "\t");
                    break;
                case 'n':
                    sb.replace(slashIndex, slashIndex + 2, "\n");
                    break;
                case 'f':
                    sb.replace(slashIndex, slashIndex + 2, "\f");
                    break;
                case 'r':
                    sb.replace(slashIndex, slashIndex + 2, "\r");
                    break;
                case '\'':
                    sb.replace(slashIndex, slashIndex + 2, "\'");
                    break;
                case '\"':
                    sb.replace(slashIndex, slashIndex + 2, "\"");
                    break;
                case '\\':
                    sb.replace(slashIndex, slashIndex + 2, "\\");
                    break;
                case '/':
                    sb.replace(slashIndex, slashIndex + 2, "/");
                    break;
                default:
                    break;
            }
            startPoint = slashIndex + 1;
        }
        sb.deleteCharAt(0);
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private static String extractUnicode(StringBuffer sb, int slashIndex) {
        String result;
        String code = sb.substring(slashIndex + 2, slashIndex + 6);
        int charNum = Integer.parseInt(code, 16); // hex to integer
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(baos, "UTF-8");
            osw.write(charNum);
            osw.flush();
            result = baos.toString("UTF-8"); // Thanks to Silvester Pozarnik for the tip about adding "UTF-8" here
        } catch (Exception e) {
            throw ASTWalkException.from("Failed to obtain for unicode '" + charNum + "'", e);
        }
        return result;
    }
}
