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
package com.espertech.esper.core.service;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.annotation.*;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.core.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

public class StatementSelectionUtil {
    private final static Logger log = LoggerFactory.getLogger(StatementSelectionUtil.class);

    private static final BeanEventType STATEMENT_META_EVENT_TYPE;

    static {
        STATEMENT_META_EVENT_TYPE = (BeanEventType) SupportEventAdapterService.getService().addBeanType("StatementRow", StatementRow.class, true, true, true);
    }

    public static void applyExpressionToStatements(EPServiceProviderSPI engine, String filter, BiConsumer<EPServiceProvider, EPStatement> consumer) {

        // compile filter
        ExprNode filterExpr = null;
        boolean isUseFilter = false;

        if ((filter != null) && (filter.trim().length() != 0)) {

            isUseFilter = true;
            Pair<ExprNode, String> statementExprNode = compileValidateStatementFilterExpr(engine, filter);
            if (statementExprNode.getSecond() == null) {
                filterExpr = statementExprNode.getFirst();
            }
        }

        String[] statementNames = engine.getEPAdministrator().getStatementNames();
        for (String statementName : statementNames) {
            EPStatement epStmt = engine.getEPAdministrator().getStatement(statementName);
            if (epStmt == null) {
                continue;
            }

            if (isUseFilter) {
                if (filterExpr != null) {
                    if (!evaluateStatement(filterExpr, epStmt)) {
                        continue;
                    }
                } else {
                    boolean match = false;
                    String searchString = filter.toLowerCase(Locale.ENGLISH);
                    if ((epStmt.getName() != null) && (epStmt.getName().toLowerCase(Locale.ENGLISH).contains(searchString))) {
                        match = true;
                    }
                    if (!match) {
                        if ((epStmt.getText() != null) && (epStmt.getText().toLowerCase(Locale.ENGLISH).contains(searchString))) {
                            match = true;
                        }
                        if ((epStmt.getState() != null) && (epStmt.getState().toString().toLowerCase(Locale.ENGLISH).contains(searchString))) {
                            match = true;
                        }
                    }
                    if (!match) {
                        continue;
                    }
                }
            }

            consumer.accept(engine, epStmt);
        }
    }

    public static boolean evaluateStatement(ExprNode expression, EPStatement stmt) {
        if (expression == null) {
            return true;
        }

        Class returnType = expression.getForge().getEvaluationType();
        if (JavaClassHelper.getBoxedType(returnType) != Boolean.class) {
            throw new EPException("Invalid expression, expected a boolean return type for expression and received '" +
                    JavaClassHelper.getClassNameFullyQualPretty(returnType) +
                    "' for expression '" + ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(expression) + "'");
        }
        ExprEvaluator evaluator = expression.getForge().getExprEvaluator();

        try {
            StatementRow row = getRow(stmt);
            EventBean rowBean = SupportEventAdapterService.getService().adapterForTypedBean(row, STATEMENT_META_EVENT_TYPE);

            Boolean pass = (Boolean) evaluator.evaluate(new EventBean[]{rowBean}, true, null);
            return !((pass == null) || (!pass));
        } catch (Exception ex) {
            log.error("Unexpected exception filtering statements by expression, skipping statement: " + ex.getMessage(), ex);
        }
        return false;
    }

    public static Pair<ExprNode, String> compileValidateStatementFilterExpr(EPServiceProviderSPI engine, String filterExpression) {
        ExprNode exprNode;
        try {
            EPAdministratorSPI spi = (EPAdministratorSPI) engine.getEPAdministrator();
            exprNode = spi.compileExpression(filterExpression);
        } catch (Exception ex) {
            return new Pair<ExprNode, String>(null, "Error compiling expression: " + ex.getMessage());
        }

        try {
            StreamTypeService streamTypeService = new StreamTypeServiceImpl(STATEMENT_META_EVENT_TYPE, null, true, engine.getURI());
            exprNode = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.SCRIPTPARAMS, exprNode, new ExprValidationContext(streamTypeService, engine.getEngineImportService(),
                    null, null, engine.getTimeProvider(), null, null, null, engine.getEventAdapterService(), "no-name-assigned", -1, null, null, true, false, false, false, null, true));
        } catch (Exception ex) {
            return new Pair<ExprNode, String>(null, "Error validating expression: " + ex.getMessage());
        }
        return new Pair<ExprNode, String>(exprNode, null);
    }

    // Predefined properties available:
    // - name (string)
    // - description (string)
    // - epl (string)
    // - each tag individually (string)
    // - priority
    // - drop (boolean)
    // - hint (string)
    private static StatementRow getRow(EPStatement statement) {
        String description = null;
        String hint = null;
        String hintDelimiter = "";
        int priority = 0;
        Map<String, String> tags = null;
        boolean drop = false;

        Annotation[] annotations = statement.getAnnotations();
        for (Annotation anno : annotations) {
            if (anno instanceof Hint) {
                if (hint == null) {
                    hint = "";
                }
                hint += hintDelimiter + ((Hint) anno).value();
                hintDelimiter = ",";
            } else if (anno instanceof Tag) {
                Tag tag = (Tag) anno;
                if (tags == null) {
                    tags = new HashMap<String, String>();
                }
                tags.put(tag.name(), tag.value());
            } else if (anno instanceof Priority) {
                Priority tag = (Priority) anno;
                priority = tag.value();
            } else if (anno instanceof Drop) {
                drop = true;
            } else if (anno instanceof Description) {
                description = ((Description) anno).value();
            }
        }

        String name = statement.getName();
        String text = statement.getText();
        String state = statement.getState().toString();
        Object userObject = statement.getUserObject();

        return new StatementRow(
                name,
                text,
                state,
                userObject,
                description,
                hint,
                priority,
                drop,
                tags
        );
    }

    public static class StatementRow {
        private String name;
        private String epl;
        private String state;
        private Object userObject;
        private String description;
        private String hint;
        private int priority;
        private Boolean drop;
        private Map<String, String> tag;

        public StatementRow(String name, String epl, String state, Object userObject, String description, String hint, int priority, Boolean drop, Map<String, String> tag) {
            this.name = name;
            this.epl = epl;
            this.state = state;
            this.userObject = userObject;
            this.description = description;
            this.hint = hint;
            this.priority = priority;
            this.drop = drop;
            this.tag = tag;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEpl() {
            return epl;
        }

        public void setEpl(String epl) {
            this.epl = epl;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public Object getUserObject() {
            return userObject;
        }

        public void setUserObject(Object userObject) {
            this.userObject = userObject;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getHint() {
            return hint;
        }

        public void setHint(String hint) {
            this.hint = hint;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        public Boolean isDrop() {
            return drop;
        }

        public Boolean getDrop() {
            return drop;
        }

        public void setDrop(Boolean drop) {
            this.drop = drop;
        }

        public Map<String, String> getTag() {
            return tag;
        }

        public void setTag(Map<String, String> tag) {
            this.tag = tag;
        }
    }
}
