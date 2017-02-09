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
package com.espertech.esper.client.soda;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.List;

/**
 * Object model of an EPL statement.
 * <p>
 * Applications can create an object model by instantiating this class and then setting the various clauses.
 * When done, use {@link com.espertech.esper.client.EPAdministrator} to create a statement from the model.
 * <p>
 * Alternativly, a given textual EPL can be compiled into an object model representation via the compile method on
 * {@link com.espertech.esper.client.EPAdministrator}.
 * <p>
 * Use the toEPL method to generate a textual EPL from an object model.
 * <p>
 * Minimally, and EPL statement consists of the select-clause and the where-clause. These are represented by {@link SelectClause}
 * and {@link FromClause} respectively.
 * <p>
 * Here is a short example that create a simple EPL statement such as "select page, responseTime from PageLoad" :
 * <pre>
 * EPStatementObjectModel model = new EPStatementObjectModel();
 * model.setSelectClause(SelectClause.create("page", "responseTime"));
 * model.setPropertyEvalSpec(FromClause.create(FilterStream.create("PageLoad")));
 * </pre>
 * <p>
 * The select-clause and from-clause must be set for the statement object model to be useable by the
 * administrative API. All other clauses a optional.
 * <p>
 * Please see the documentation set for further examples.
 */
public class EPStatementObjectModel implements Serializable {
    private static final long serialVersionUID = 0L;

    private List<AnnotationPart> annotations;
    private List<ExpressionDeclaration> expressionDeclarations;
    private List<ScriptExpression> scriptExpressions;
    private String contextName;
    private UpdateClause updateClause;
    private CreateVariableClause createVariable;
    private CreateTableClause createTable;
    private CreateWindowClause createWindow;
    private CreateIndexClause createIndex;
    private CreateSchemaClause createSchema;
    private CreateContextClause createContext;
    private CreateDataFlowClause createDataFlow;
    private CreateExpressionClause createExpression;
    private OnClause onExpr;
    private InsertIntoClause insertInto;
    private SelectClause selectClause;
    private FromClause fromClause;
    private Expression whereClause;
    private GroupByClause groupByClause;
    private Expression havingClause;
    private OutputLimitClause outputLimitClause;
    private OrderByClause orderByClause;
    private RowLimitClause rowLimitClause;
    private MatchRecognizeClause matchRecognizeClause;
    private ForClause forClause;
    private String treeObjectName;
    private FireAndForgetClause fireAndForgetClause;
    private IntoTableClause intoTableClause;

    /**
     * Ctor.
     */
    public EPStatementObjectModel() {
    }

    /**
     * Specify an insert-into-clause.
     *
     * @param insertInto specifies the insert-into-clause, or null to indicate that the clause is absent
     */
    public void setInsertInto(InsertIntoClause insertInto) {
        this.insertInto = insertInto;
    }

    /**
     * Specify an insert-into-clause.
     *
     * @param insertInto specifies the insert-into-clause, or null to indicate that the clause is absent
     * @return model
     */
    public EPStatementObjectModel insertInto(InsertIntoClause insertInto) {
        this.insertInto = insertInto;
        return this;
    }

    /**
     * Return the insert-into-clause, or null to indicate that the clause is absent.
     *
     * @return specification of the insert-into-clause, or null if none present
     */
    public InsertIntoClause getInsertInto() {
        return insertInto;
    }

    /**
     * Specify a select-clause.
     *
     * @param selectClause specifies the select-clause, the select-clause cannot be null and must be set
     */
    public void setSelectClause(SelectClause selectClause) {
        this.selectClause = selectClause;
    }

    /**
     * Specify a select-clause.
     *
     * @param selectClause specifies the select-clause, the select-clause cannot be null and must be set
     * @return model
     */
    public EPStatementObjectModel selectClause(SelectClause selectClause) {
        this.selectClause = selectClause;
        return this;
    }

    /**
     * Return the select-clause.
     *
     * @return specification of the select-clause
     */
    public SelectClause getSelectClause() {
        return selectClause;
    }

    /**
     * Specify a from-clause.
     *
     * @param fromClause specifies the from-clause, the from-clause cannot be null and must be set
     */
    public void setFromClause(FromClause fromClause) {
        this.fromClause = fromClause;
    }

    /**
     * Specify a from-clause.
     *
     * @param fromClause specifies the from-clause, the from-clause cannot be null and must be set
     * @return model
     */
    public EPStatementObjectModel fromClause(FromClause fromClause) {
        this.fromClause = fromClause;
        return this;
    }

    /**
     * Return the where-clause, or null to indicate that the clause is absent.
     *
     * @return specification of the where-clause, or null if none present
     */
    public Expression getWhereClause() {
        return whereClause;
    }

    /**
     * Specify a where-clause.
     *
     * @param whereClause specifies the where-clause, which is optional and can be null
     */
    public void setWhereClause(Expression whereClause) {
        this.whereClause = whereClause;
    }

    /**
     * Specify a where-clause.
     *
     * @param whereClause specifies the where-clause, which is optional and can be null
     * @return model
     */
    public EPStatementObjectModel whereClause(Expression whereClause) {
        this.whereClause = whereClause;
        return this;
    }

    /**
     * Return the from-clause.
     *
     * @return specification of the from-clause
     */
    public FromClause getFromClause() {
        return fromClause;
    }

    /**
     * Return the group-by-clause, or null to indicate that the clause is absent.
     *
     * @return specification of the group-by-clause, or null if none present
     */
    public GroupByClause getGroupByClause() {
        return groupByClause;
    }

    /**
     * Specify a group-by-clause.
     *
     * @param groupByClause specifies the group-by-clause, which is optional and can be null
     */
    public void setGroupByClause(GroupByClause groupByClause) {
        this.groupByClause = groupByClause;
    }

    /**
     * Specify a group-by-clause.
     *
     * @param groupByClause specifies the group-by-clause, which is optional and can be null
     * @return model
     */
    public EPStatementObjectModel groupByClause(GroupByClause groupByClause) {
        this.groupByClause = groupByClause;
        return this;
    }

    /**
     * Return the having-clause, or null to indicate that the clause is absent.
     *
     * @return specification of the having-clause, or null if none present
     */
    public Expression getHavingClause() {
        return havingClause;
    }

    /**
     * Specify a having-clause.
     *
     * @param havingClause specifies the having-clause, which is optional and can be null
     */
    public void setHavingClause(Expression havingClause) {
        this.havingClause = havingClause;
    }

    /**
     * Specify a having-clause.
     *
     * @param havingClause specifies the having-clause, which is optional and can be null
     * @return model
     */
    public EPStatementObjectModel havingClause(Expression havingClause) {
        this.havingClause = havingClause;
        return this;
    }

    /**
     * Return the order-by-clause, or null to indicate that the clause is absent.
     *
     * @return specification of the order-by-clause, or null if none present
     */
    public OrderByClause getOrderByClause() {
        return orderByClause;
    }

    /**
     * Specify an order-by-clause.
     *
     * @param orderByClause specifies the order-by-clause, which is optional and can be null
     */
    public void setOrderByClause(OrderByClause orderByClause) {
        this.orderByClause = orderByClause;
    }

    /**
     * Specify an order-by-clause.
     *
     * @param orderByClause specifies the order-by-clause, which is optional and can be null
     * @return model
     */
    public EPStatementObjectModel orderByClause(OrderByClause orderByClause) {
        this.orderByClause = orderByClause;
        return this;
    }

    /**
     * Return the output-rate-limiting-clause, or null to indicate that the clause is absent.
     *
     * @return specification of the output-rate-limiting-clause, or null if none present
     */
    public OutputLimitClause getOutputLimitClause() {
        return outputLimitClause;
    }

    /**
     * Specify an output-rate-limiting-clause.
     *
     * @param outputLimitClause specifies the output-rate-limiting-clause, which is optional and can be null
     */
    public void setOutputLimitClause(OutputLimitClause outputLimitClause) {
        this.outputLimitClause = outputLimitClause;
    }

    /**
     * Specify an output-rate-limiting-clause.
     *
     * @param outputLimitClause specifies the output-rate-limiting-clause, which is optional and can be null
     * @return model
     */
    public EPStatementObjectModel outputLimitClause(OutputLimitClause outputLimitClause) {
        this.outputLimitClause = outputLimitClause;
        return this;
    }

    /**
     * Renders the object model in it's EPL syntax textual representation.
     *
     * @return EPL representing the statement object model
     * @throws IllegalStateException if required clauses do not exist
     */
    public String toEPL() {
        StringWriter writer = new StringWriter();
        toEPL(new EPStatementFormatter(false), writer);
        return writer.toString();
    }

    /**
     * Rendering using the provided writer.
     *
     * @param writer to use
     */
    public void toEPL(StringWriter writer) {
        toEPL(new EPStatementFormatter(false), writer);
    }

    /**
     * Rendering using the provided formatter.
     *
     * @param formatter to use
     * @return rendered string
     */
    public String toEPL(EPStatementFormatter formatter) {
        StringWriter writer = new StringWriter();
        toEPL(formatter, writer);
        return writer.toString();
    }

    /**
     * Renders the object model in it's EPL syntax textual representation, using a whitespace-formatter as provided.
     *
     * @param formatter the formatter to use
     * @param writer    writer to use
     * @throws IllegalStateException if required clauses do not exist
     */
    public void toEPL(EPStatementFormatter formatter, StringWriter writer) {
        AnnotationPart.toEPL(writer, annotations, formatter);
        ExpressionDeclaration.toEPL(writer, expressionDeclarations, formatter);
        ScriptExpression.toEPL(writer, scriptExpressions, formatter);

        if (contextName != null) {
            formatter.beginContext(writer);
            writer.append("context ");
            writer.append(contextName);
        }

        if (createIndex != null) {
            formatter.beginCreateIndex(writer);
            createIndex.toEPL(writer);
            return;
        } else if (createSchema != null) {
            formatter.beginCreateSchema(writer);
            createSchema.toEPL(writer);
            return;
        } else if (createExpression != null) {
            formatter.beginCreateExpression(writer);
            createExpression.toEPL(writer);
            return;
        } else if (createContext != null) {
            formatter.beginCreateContext(writer);
            createContext.toEPL(writer, formatter);
            return;
        } else if (createWindow != null) {
            formatter.beginCreateWindow(writer);
            createWindow.toEPL(writer);

            if (fromClause != null) {
                FilterStream fs = (FilterStream) fromClause.getStreams().get(0);
                if (fs.isRetainUnion()) {
                    writer.write(" retain-union");
                }
            }

            writer.write(" as ");
            if ((selectClause == null) || (selectClause.getSelectList().isEmpty()) && !createWindow.getColumns().isEmpty()) {
                createWindow.toEPLCreateTablePart(writer);
            } else {
                selectClause.toEPL(writer, formatter, false, false);
                fromClause.toEPL(writer, formatter);
                createWindow.toEPLInsertPart(writer);
            }
            return;
        } else if (createVariable != null) {
            formatter.beginCreateVariable(writer);
            createVariable.toEPL(writer);
            return;
        } else if (createTable != null) {
            formatter.beginCreateTable(writer);
            createTable.toEPL(writer);
            return;
        } else if (createDataFlow != null) {
            formatter.beginCreateDataFlow(writer);
            createDataFlow.toEPL(writer, formatter);
            return;
        }

        boolean displayWhereClause = true;
        if (updateClause != null) {
            formatter.beginUpdate(writer);
            updateClause.toEPL(writer);
        } else if (onExpr != null) {
            formatter.beginOnTrigger(writer);
            writer.write("on ");
            fromClause.getStreams().get(0).toEPL(writer, formatter);

            if (onExpr instanceof OnDeleteClause) {
                formatter.beginOnDelete(writer);
                writer.write("delete from ");
                ((OnDeleteClause) onExpr).toEPL(writer);
            } else if (onExpr instanceof OnUpdateClause) {
                formatter.beginOnUpdate(writer);
                writer.write("update ");
                ((OnUpdateClause) onExpr).toEPL(writer);
            } else if (onExpr instanceof OnSelectClause) {
                OnSelectClause onSelect = (OnSelectClause) onExpr;
                if (insertInto != null) {
                    insertInto.toEPL(writer, formatter, true);
                }
                selectClause.toEPL(writer, formatter, true, onSelect.isDeleteAndSelect());
                writer.write(" from ");
                onSelect.toEPL(writer);
            } else if (onExpr instanceof OnSetClause) {
                OnSetClause onSet = (OnSetClause) onExpr;
                onSet.toEPL(writer, formatter);
            } else if (onExpr instanceof OnMergeClause) {
                OnMergeClause merge = (OnMergeClause) onExpr;
                merge.toEPL(writer, whereClause, formatter);
                displayWhereClause = false;
            } else {
                OnInsertSplitStreamClause split = (OnInsertSplitStreamClause) onExpr;
                insertInto.toEPL(writer, formatter, true);
                selectClause.toEPL(writer, formatter, true, false);
                if (whereClause != null) {
                    writer.write(" where ");
                    whereClause.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
                }
                split.toEPL(writer, formatter);
                displayWhereClause = false;
            }
        } else {
            if (intoTableClause != null) {
                intoTableClause.toEPL(writer);
            }

            if (selectClause == null) {
                throw new IllegalStateException("Select-clause has not been defined");
            }
            if (fromClause == null) {
                throw new IllegalStateException("From-clause has not been defined");
            }

            if (fireAndForgetClause instanceof FireAndForgetUpdate) {
                FireAndForgetUpdate update = (FireAndForgetUpdate) fireAndForgetClause;
                writer.append("update ");
                fromClause.toEPLOptions(writer, formatter, false);
                writer.append(" ");
                UpdateClause.renderEPLAssignments(writer, update.getAssignments());
            } else if (fireAndForgetClause instanceof FireAndForgetInsert) {
                FireAndForgetInsert insert = (FireAndForgetInsert) fireAndForgetClause;
                insertInto.toEPL(writer, formatter, true);
                if (insert.isUseValuesKeyword()) {
                    writer.append(" values (");
                    String delimiter = "";
                    for (SelectClauseElement element : selectClause.getSelectList()) {
                        writer.write(delimiter);
                        element.toEPLElement(writer);
                        delimiter = ", ";
                    }
                    writer.append(")");
                } else {
                    selectClause.toEPL(writer, formatter, true, false);
                }
            } else if (fireAndForgetClause instanceof FireAndForgetDelete) {
                writer.append("delete ");
                fromClause.toEPLOptions(writer, formatter, true);
            } else {
                if (insertInto != null) {
                    insertInto.toEPL(writer, formatter, true);
                }
                selectClause.toEPL(writer, formatter, true, false);
                fromClause.toEPLOptions(writer, formatter, true);
            }
        }

        if (matchRecognizeClause != null) {
            matchRecognizeClause.toEPL(writer);
        }
        if (whereClause != null && displayWhereClause) {
            formatter.beginWhere(writer);
            writer.write("where ");
            whereClause.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        }
        if (groupByClause != null) {
            formatter.beginGroupBy(writer);
            writer.write("group by ");
            groupByClause.toEPL(writer);
        }
        if (havingClause != null) {
            formatter.beginHaving(writer);
            writer.write("having ");
            havingClause.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        }
        if (outputLimitClause != null) {
            formatter.beginOutput(writer);
            writer.write("output ");
            outputLimitClause.toEPL(writer);
        }
        if (orderByClause != null) {
            formatter.beginOrderBy(writer);
            writer.write("order by ");
            orderByClause.toEPL(writer);
        }
        if (rowLimitClause != null) {
            formatter.beginLimit(writer);
            writer.write("limit ");
            rowLimitClause.toEPL(writer);
        }
        if (forClause != null) {
            formatter.beginFor(writer);
            forClause.toEPL(writer);
        }
    }

    /**
     * Returns the create-window clause for creating named windows, or null if this statement does not
     * create a named window.
     *
     * @return named window creation clause
     */
    public CreateWindowClause getCreateWindow() {
        return createWindow;
    }

    /**
     * Sets the create-window clause for creating named windows, or null if this statement does not
     * create a named window.
     *
     * @param createWindow is the named window creation clause
     */
    public void setCreateWindow(CreateWindowClause createWindow) {
        this.createWindow = createWindow;
    }

    /**
     * Returns the on-delete clause for deleting from named windows, or null if this statement
     * does not delete from a named window
     *
     * @return on delete clause
     */
    public OnClause getOnExpr() {
        return onExpr;
    }

    /**
     * Sets the on-delete or on-select clause for selecting or deleting from named windows, or null if this statement
     * does not on-select or on-delete from a named window
     *
     * @param onExpr is the on-expression (on-select and on-delete) clause to set
     */
    public void setOnExpr(OnClause onExpr) {
        this.onExpr = onExpr;
    }

    /**
     * Returns the create-variable clause if this is a statement creating a variable, or null if not.
     *
     * @return create-variable clause
     */
    public CreateVariableClause getCreateVariable() {
        return createVariable;
    }

    /**
     * Sets the create-variable clause if this is a statement creating a variable, or null if not.
     *
     * @param createVariable create-variable clause
     */
    public void setCreateVariable(CreateVariableClause createVariable) {
        this.createVariable = createVariable;
    }

    /**
     * Returns the row limit specification, or null if none supplied.
     *
     * @return row limit spec if any
     */
    public RowLimitClause getRowLimitClause() {
        return rowLimitClause;
    }

    /**
     * Sets the row limit specification, or null if none applicable.
     *
     * @param rowLimitClause row limit spec if any
     */
    public void setRowLimitClause(RowLimitClause rowLimitClause) {
        this.rowLimitClause = rowLimitClause;
    }

    /**
     * Returns the update specification.
     *
     * @return update spec if defined
     */
    public UpdateClause getUpdateClause() {
        return updateClause;
    }

    /**
     * Sets the update specification.
     *
     * @param updateClause update spec if defined
     */
    public void setUpdateClause(UpdateClause updateClause) {
        this.updateClause = updateClause;
    }

    /**
     * Returns annotations.
     *
     * @return annotations
     */
    public List<AnnotationPart> getAnnotations() {
        return annotations;
    }

    /**
     * Sets annotations.
     *
     * @param annotations to set
     */
    public void setAnnotations(List<AnnotationPart> annotations) {
        this.annotations = annotations;
    }

    /**
     * Match-recognize clause.
     *
     * @return clause
     */
    public MatchRecognizeClause getMatchRecognizeClause() {
        return matchRecognizeClause;
    }

    /**
     * Sets match-recognize clause.
     *
     * @param clause to set
     */
    public void setMatchRecognizeClause(MatchRecognizeClause clause) {
        this.matchRecognizeClause = clause;
    }

    /**
     * Returns create-index clause.
     *
     * @return clause
     */
    public CreateIndexClause getCreateIndex() {
        return createIndex;
    }

    /**
     * Sets create-index clause.
     *
     * @param createIndex to set
     */
    public void setCreateIndex(CreateIndexClause createIndex) {
        this.createIndex = createIndex;
    }

    /**
     * Returns the create-schema clause.
     *
     * @return clause
     */
    public CreateSchemaClause getCreateSchema() {
        return createSchema;
    }

    /**
     * Sets the create-schema clause.
     *
     * @param createSchema clause to set
     */
    public void setCreateSchema(CreateSchemaClause createSchema) {
        this.createSchema = createSchema;
    }

    /**
     * Returns the create-context clause.
     *
     * @return clause
     */
    public CreateContextClause getCreateContext() {
        return createContext;
    }

    /**
     * Sets the create-context clause.
     *
     * @param createContext clause to set
     */
    public void setCreateContext(CreateContextClause createContext) {
        this.createContext = createContext;
    }

    /**
     * Returns the for-clause.
     *
     * @return for-clause
     */
    public ForClause getForClause() {
        return forClause;
    }

    /**
     * Sets the for-clause.
     *
     * @param forClause for-clause
     */
    public void setForClause(ForClause forClause) {
        this.forClause = forClause;
    }

    /**
     * Returns the expression declarations, if any.
     *
     * @return expression declarations
     */
    public List<ExpressionDeclaration> getExpressionDeclarations() {
        return expressionDeclarations;
    }

    /**
     * Sets the expression declarations, if any.
     *
     * @param expressionDeclarations expression declarations to set
     */
    public void setExpressionDeclarations(List<ExpressionDeclaration> expressionDeclarations) {
        this.expressionDeclarations = expressionDeclarations;
    }

    /**
     * Returns the context name if context dimensions apply to statement.
     *
     * @return context name
     */
    public String getContextName() {
        return contextName;
    }

    /**
     * Sets the context name if context dimensions apply to statement.
     *
     * @param contextName context name
     */
    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    /**
     * Returns the scripts defined.
     *
     * @return scripts
     */
    public List<ScriptExpression> getScriptExpressions() {
        return scriptExpressions;
    }

    /**
     * Sets the scripts.
     *
     * @param scriptExpressions to set
     */
    public void setScriptExpressions(List<ScriptExpression> scriptExpressions) {
        this.scriptExpressions = scriptExpressions;
    }

    /**
     * Returns the "create dataflow" part, if present.
     *
     * @return create dataflow clause
     */
    public CreateDataFlowClause getCreateDataFlow() {
        return createDataFlow;
    }

    /**
     * Sets the "create dataflow" part,.
     *
     * @param createDataFlow create dataflow clause
     */
    public void setCreateDataFlow(CreateDataFlowClause createDataFlow) {
        this.createDataFlow = createDataFlow;
    }

    /**
     * Returns the internal expression id assigned for tools to identify the expression.
     *
     * @return object name
     */
    public String getTreeObjectName() {
        return treeObjectName;
    }

    /**
     * Sets an internal expression id assigned for tools to identify the expression.
     *
     * @param treeObjectName object name
     */
    public void setTreeObjectName(String treeObjectName) {
        this.treeObjectName = treeObjectName;
    }

    /**
     * Returns the create-expression clause, if any
     *
     * @return clause
     */
    public CreateExpressionClause getCreateExpression() {
        return createExpression;
    }

    /**
     * Sets the create-expression clause, if any
     *
     * @param createExpression clause
     */
    public void setCreateExpression(CreateExpressionClause createExpression) {
        this.createExpression = createExpression;
    }

    /**
     * Returns fire-and-forget (on-demand) query information for FAF select, insert, update and delete.
     *
     * @return fire and forget query information
     */
    public FireAndForgetClause getFireAndForgetClause() {
        return fireAndForgetClause;
    }

    /**
     * Sets fire-and-forget (on-demand) query information for FAF select, insert, update and delete.
     *
     * @param fireAndForgetClause fire and forget query information
     */
    public void setFireAndForgetClause(FireAndForgetClause fireAndForgetClause) {
        this.fireAndForgetClause = fireAndForgetClause;
    }

    /**
     * Returns the into-table clause, or null if none found.
     *
     * @return into-table clause
     */
    public IntoTableClause getIntoTableClause() {
        return intoTableClause;
    }

    /**
     * Sets the into-table clause, or null if none found.
     *
     * @param intoTableClause into-table clause
     */
    public void setIntoTableClause(IntoTableClause intoTableClause) {
        this.intoTableClause = intoTableClause;
    }

    /**
     * Returns the create-table clause if present or null if not present
     *
     * @return create-table clause
     */
    public CreateTableClause getCreateTable() {
        return createTable;
    }

    /**
     * Sets the create-table clause if present or null if not present
     *
     * @param createTable create-table clause
     */
    public void setCreateTable(CreateTableClause createTable) {
        this.createTable = createTable;
    }
}
