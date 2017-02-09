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
package com.espertech.esper.epl.declexpr;

import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.CreateExpressionDesc;
import com.espertech.esper.epl.spec.ExpressionDeclItem;
import com.espertech.esper.epl.spec.ExpressionScriptProvided;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExprDeclaredServiceImpl implements ExprDeclaredService {

    private final Map<String, ExpressionDeclItem> globalExpressions;
    private final Map<String, List<ExpressionScriptProvided>> globalScripts;

    public ExprDeclaredServiceImpl() {
        this.globalExpressions = new HashMap<String, ExpressionDeclItem>();
        this.globalScripts = new HashMap<String, List<ExpressionScriptProvided>>();
    }

    public synchronized String addExpressionOrScript(CreateExpressionDesc expressionDesc) throws ExprValidationException {
        if (expressionDesc.getExpression() != null) {
            ExpressionDeclItem expression = expressionDesc.getExpression();
            String name = expression.getName();
            if (globalExpressions.containsKey(name)) {
                throw new ExprValidationException("Expression '" + name + "' has already been declared");
            }
            globalExpressions.put(name, expression);
            return name;
        } else {
            ExpressionScriptProvided newScript = expressionDesc.getScript();
            String name = newScript.getName();

            List<ExpressionScriptProvided> scripts = globalScripts.get(name);
            if (scripts != null) {
                for (ExpressionScriptProvided script : scripts) {
                    if (script.getParameterNames().size() == newScript.getParameterNames().size()) {
                        throw new ExprValidationException("Script '" + name + "' that takes the same number of parameters has already been declared");
                    }
                }
            } else {
                scripts = new ArrayList<ExpressionScriptProvided>(2);
                globalScripts.put(name, scripts);
            }
            scripts.add(newScript);

            return name;
        }
    }

    public ExpressionDeclItem getExpression(String name) {
        return globalExpressions.get(name);
    }

    public List<ExpressionScriptProvided> getScriptsByName(String name) {
        return globalScripts.get(name);
    }

    public synchronized void destroyedExpression(CreateExpressionDesc expressionDesc) {
        if (expressionDesc.getExpression() != null) {
            globalExpressions.remove(expressionDesc.getExpression().getName());
        } else {
            globalScripts.remove(expressionDesc.getScript().getName());
        }
    }

    public void destroy() {
        globalExpressions.clear();
    }
}
