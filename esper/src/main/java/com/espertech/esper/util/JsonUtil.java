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
package com.espertech.esper.util;

import com.espertech.esper.epl.expression.core.ExprNodeOrigin;
import com.espertech.esper.epl.expression.core.ExprValidationContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.generated.EsperEPL2GrammarParser;
import com.espertech.esper.epl.parse.ASTJsonHelper;
import com.espertech.esper.epl.parse.ParseHelper;
import com.espertech.esper.epl.parse.ParseResult;
import com.espertech.esper.epl.parse.ParseRuleSelector;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.tree.Tree;

import java.util.Map;

public class JsonUtil {
    public static Object parsePopulate(String json, Class topClass, ExprNodeOrigin exprNodeOrigin, ExprValidationContext exprValidationContext) throws ExprValidationException {
        ParseRuleSelector startRuleSelector = new ParseRuleSelector() {
            public Tree invokeParseRule(EsperEPL2GrammarParser parser) throws RecognitionException {
                return parser.startJsonValueRule();
            }
        };
        ParseResult parseResult = ParseHelper.parse(json, json, true, startRuleSelector, false);
        EsperEPL2GrammarParser.StartJsonValueRuleContext tree = (EsperEPL2GrammarParser.StartJsonValueRuleContext) parseResult.getTree();
        Object parsed = ASTJsonHelper.walk(parseResult.getTokenStream(), tree.jsonvalue());

        if (!(parsed instanceof Map)) {
            throw new ExprValidationException("Failed to map value to object of type " + topClass.getName() + ", expected Json Map/Object format, received " + (parsed != null ? parsed.getClass().getSimpleName() : "null"));
        }
        Map<String, Object> objectProperties = (Map<String, Object>) parsed;
        return PopulateUtil.instantiatePopulateObject(objectProperties, topClass, exprNodeOrigin, exprValidationContext);
    }
}
