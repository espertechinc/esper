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
package com.espertech.esper.epl.spec;

import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprSubstitutionNode;
import com.espertech.esper.epl.expression.table.ExprTableAccessNode;

import java.io.Serializable;
import java.util.*;

/**
 * Specification object representing a complete EPL statement including all EPL constructs.
 */
public class StatementSpecRaw implements Serializable {
    private ExpressionDeclDesc expressionDeclDesc;
    private OnTriggerDesc onTriggerDesc;
    private UpdateDesc updateDesc;
    private CreateWindowDesc createWindowDesc;
    private CreateVariableDesc createVariableDesc;
    private CreateTableDesc createTableDesc;
    private CreateIndexDesc createIndexDesc;
    private CreateSchemaDesc createSchemaDesc;
    private InsertIntoDesc insertIntoDesc;
    private SelectClauseStreamSelectorEnum selectStreamDirEnum;
    private SelectClauseSpecRaw selectClauseSpec = new SelectClauseSpecRaw();
    private List<StreamSpecRaw> streamSpecs = new LinkedList<StreamSpecRaw>();
    private List<OuterJoinDesc> outerJoinDescList = new LinkedList<OuterJoinDesc>();
    private ExprNode filterExprRootNode;
    private List<GroupByClauseElement> groupByExpressions = new ArrayList<GroupByClauseElement>(2);
    private ExprNode havingExprRootNode;
    private OutputLimitSpec outputLimitSpec;
    private RowLimitSpec rowLimitSpec;
    private List<OrderByItem> orderByList = new LinkedList<OrderByItem>();
    private boolean hasVariables;
    private List<AnnotationDesc> annotations = new ArrayList<AnnotationDesc>(1);
    private String expressionNoAnnotations;
    private MatchRecognizeSpec matchRecognizeSpec;
    private Set<String> referencedVariables;
    private ForClauseSpec forClauseSpec;
    private Map<Integer, List<ExprNode>> sqlParameters;
    private List<ExprSubstitutionNode> substitutionParameters;
    private CreateContextDesc createContextDesc;
    private String optionalContextName;
    private List<ExpressionScriptProvided> scriptExpressions;
    private CreateDataFlowDesc createDataFlowDesc;
    private CreateExpressionDesc createExpressionDesc;
    private FireAndForgetSpec fireAndForgetSpec;
    private IntoTableSpec intoTableSpec;
    private Set<ExprTableAccessNode> tableExpressions;

    private static final long serialVersionUID = 5390766716794133693L;

    /**
     * Ctor.
     *
     * @param defaultStreamSelector stream selection for the statement
     */
    public StatementSpecRaw(SelectClauseStreamSelectorEnum defaultStreamSelector) {
        selectStreamDirEnum = defaultStreamSelector;
    }

    /**
     * Returns the FROM-clause stream definitions.
     *
     * @return list of stream specifications
     */
    public List<StreamSpecRaw> getStreamSpecs() {
        return streamSpecs;
    }

    /**
     * Returns SELECT-clause list of expressions.
     *
     * @return list of expressions and optional name
     */
    public SelectClauseSpecRaw getSelectClauseSpec() {
        return selectClauseSpec;
    }

    /**
     * Returns the WHERE-clause root node of filter expression.
     *
     * @return filter expression root node
     */
    public ExprNode getFilterRootNode() {
        return filterExprRootNode;
    }

    /**
     * Returns the LEFT/RIGHT/FULL OUTER JOIN-type and property name descriptor, if applicable. Returns null if regular join.
     *
     * @return outer join type, stream names and property names
     */
    public List<OuterJoinDesc> getOuterJoinDescList() {
        return outerJoinDescList;
    }

    /**
     * Returns list of group-by expressions.
     *
     * @return group-by expression nodes as specified in group-by clause
     */
    public List<GroupByClauseElement> getGroupByExpressions() {
        return groupByExpressions;
    }

    /**
     * Returns expression root node representing the having-clause, if present, or null if no having clause was supplied.
     *
     * @return having-clause expression top node
     */
    public ExprNode getHavingExprRootNode() {
        return havingExprRootNode;
    }

    /**
     * Returns the output limit definition, if any.
     *
     * @return output limit spec
     */
    public OutputLimitSpec getOutputLimitSpec() {
        return outputLimitSpec;
    }

    /**
     * Return a descriptor with the insert-into event name and optional list of columns.
     *
     * @return insert into specification
     */
    public InsertIntoDesc getInsertIntoDesc() {
        return insertIntoDesc;
    }

    /**
     * Returns the list of order-by expression as specified in the ORDER BY clause.
     *
     * @return Returns the orderByList.
     */
    public List<OrderByItem> getOrderByList() {
        return orderByList;
    }

    /**
     * Returns the stream selector (rstream/istream).
     *
     * @return stream selector
     */
    public SelectClauseStreamSelectorEnum getSelectStreamSelectorEnum() {
        return selectStreamDirEnum;
    }

    /**
     * Sets the output limiting definition.
     *
     * @param outputLimitSpec defines the rules for output limiting
     */
    public void setOutputLimitSpec(OutputLimitSpec outputLimitSpec) {
        this.outputLimitSpec = outputLimitSpec;
    }

    /**
     * Sets the having-clause filter expression node.
     *
     * @param havingExprRootNode the having-clause expression
     */
    public void setHavingExprRootNode(ExprNode havingExprRootNode) {
        this.havingExprRootNode = havingExprRootNode;
    }

    /**
     * Sets the definition for any insert-into clause.
     *
     * @param insertIntoDesc is the descriptor for insert-into rules
     */
    public void setInsertIntoDesc(InsertIntoDesc insertIntoDesc) {
        this.insertIntoDesc = insertIntoDesc;
    }

    /**
     * Sets the stream selector (rstream/istream/both etc).
     *
     * @param selectStreamDirEnum to be set
     */
    public void setSelectStreamDirEnum(SelectClauseStreamSelectorEnum selectStreamDirEnum) {
        this.selectStreamDirEnum = selectStreamDirEnum;
    }

    /**
     * Sets the select clause.
     *
     * @param selectClauseSpec is the new select clause specification
     */
    public void setSelectClauseSpec(SelectClauseSpecRaw selectClauseSpec) {
        this.selectClauseSpec = selectClauseSpec;
    }

    /**
     * Returns the create-window specification.
     *
     * @return descriptor for creating a named window
     */
    public CreateWindowDesc getCreateWindowDesc() {
        return createWindowDesc;
    }

    /**
     * Sets the create-window specification.
     *
     * @param createWindowDesc descriptor for creating a named window
     */
    public void setCreateWindowDesc(CreateWindowDesc createWindowDesc) {
        this.createWindowDesc = createWindowDesc;
    }

    /**
     * Returns the on-delete statement specification.
     *
     * @return descriptor for creating a an on-delete statement
     */
    public OnTriggerDesc getOnTriggerDesc() {
        return onTriggerDesc;
    }

    /**
     * Sets the on-delete statement specification.
     *
     * @param onTriggerDesc descriptor for creating an on-delete statement
     */
    public void setOnTriggerDesc(OnTriggerDesc onTriggerDesc) {
        this.onTriggerDesc = onTriggerDesc;
    }

    /**
     * Gets the where clause.
     *
     * @return where clause or null if none
     */
    public ExprNode getFilterExprRootNode() {
        return filterExprRootNode;
    }

    /**
     * Sets the where clause or null if none
     *
     * @param filterExprRootNode where clause expression
     */
    public void setFilterExprRootNode(ExprNode filterExprRootNode) {
        this.filterExprRootNode = filterExprRootNode;
    }

    /**
     * Returns true if a statement (or subquery sub-statements) use variables.
     *
     * @return indicator if variables are used
     */
    public boolean isHasVariables() {
        return hasVariables;
    }

    /**
     * Sets the flag indicating the statement uses variables.
     *
     * @param hasVariables true if variables are used
     */
    public void setHasVariables(boolean hasVariables) {
        this.hasVariables = hasVariables;
    }

    /**
     * Returns the descriptor for create-variable statements.
     *
     * @return create-variable info
     */
    public CreateVariableDesc getCreateVariableDesc() {
        return createVariableDesc;
    }

    /**
     * Sets the descriptor for create-variable statements, if this is one.
     *
     * @param createVariableDesc create-variable info
     */
    public void setCreateVariableDesc(CreateVariableDesc createVariableDesc) {
        this.createVariableDesc = createVariableDesc;
    }

    /**
     * Returns the row limit, or null if none.
     *
     * @return row limit
     */
    public RowLimitSpec getRowLimitSpec() {
        return rowLimitSpec;
    }

    /**
     * Sets the row limit, or null if none.
     *
     * @param rowLimitSpec row limit
     */
    public void setRowLimitSpec(RowLimitSpec rowLimitSpec) {
        this.rowLimitSpec = rowLimitSpec;
    }

    /**
     * Returns a list of annotation descriptors.
     *
     * @return annotation descriptors
     */
    public List<AnnotationDesc> getAnnotations() {
        return annotations;
    }

    /**
     * Sets a list of annotation descriptors.
     *
     * @param annotations annotation descriptors
     */
    public void setAnnotations(List<AnnotationDesc> annotations) {
        this.annotations = annotations;
    }

    /**
     * Sets the update specification.
     *
     * @param updateDesc update spec
     */
    public void setUpdateDesc(UpdateDesc updateDesc) {
        this.updateDesc = updateDesc;
    }

    /**
     * Returns the update spec.
     *
     * @return update spec
     */
    public UpdateDesc getUpdateDesc() {
        return updateDesc;
    }

    /**
     * Sets the expression text without annotations.
     *
     * @param expressionNoAnnotations text
     */
    public void setExpressionNoAnnotations(String expressionNoAnnotations) {
        this.expressionNoAnnotations = expressionNoAnnotations;
    }

    /**
     * Returns the expression text without annotations.
     *
     * @return expressionNoAnnotations text
     */
    public String getExpressionNoAnnotations() {
        return expressionNoAnnotations;
    }

    /**
     * Returns the match recognize spec.
     *
     * @return spec
     */
    public MatchRecognizeSpec getMatchRecognizeSpec() {
        return matchRecognizeSpec;
    }

    /**
     * Sets the match recognize spec
     *
     * @param matchRecognizeSpec spec
     */
    public void setMatchRecognizeSpec(MatchRecognizeSpec matchRecognizeSpec) {
        this.matchRecognizeSpec = matchRecognizeSpec;
    }

    /**
     * Set variables referenced
     *
     * @param referencedVariables vars
     */
    public void setReferencedVariables(Set<String> referencedVariables) {
        this.referencedVariables = referencedVariables;
    }

    /**
     * Returns variables referenced
     *
     * @return vars
     */
    public Set<String> getReferencedVariables() {
        return referencedVariables;
    }

    /**
     * Returns create-index if any.
     *
     * @return index create
     */
    public CreateIndexDesc getCreateIndexDesc() {
        return createIndexDesc;
    }

    /**
     * Set create-index if any.
     *
     * @param createIndexDesc index create
     */
    public void setCreateIndexDesc(CreateIndexDesc createIndexDesc) {
        this.createIndexDesc = createIndexDesc;
    }

    public CreateSchemaDesc getCreateSchemaDesc() {
        return createSchemaDesc;
    }

    public void setCreateSchemaDesc(CreateSchemaDesc createSchemaDesc) {
        this.createSchemaDesc = createSchemaDesc;
    }

    public ForClauseSpec getForClauseSpec() {
        return forClauseSpec;
    }

    public void setForClauseSpec(ForClauseSpec forClauseSpec) {
        this.forClauseSpec = forClauseSpec;
    }

    public Map<Integer, List<ExprNode>> getSqlParameters() {
        return sqlParameters;
    }

    public void setSqlParameters(Map<Integer, List<ExprNode>> sqlParameters) {
        this.sqlParameters = sqlParameters;
    }

    public List<ExprSubstitutionNode> getSubstitutionParameters() {
        return substitutionParameters;
    }

    public void setSubstitutionParameters(List<ExprSubstitutionNode> substitutionParameters) {
        this.substitutionParameters = substitutionParameters;
    }

    public ExpressionDeclDesc getExpressionDeclDesc() {
        return expressionDeclDesc;
    }

    public void setExpressionDeclDesc(ExpressionDeclDesc expressionDeclDesc) {
        this.expressionDeclDesc = expressionDeclDesc;
    }

    public CreateContextDesc getCreateContextDesc() {
        return createContextDesc;
    }

    public void setCreateContextDesc(CreateContextDesc createContextDesc) {
        this.createContextDesc = createContextDesc;
    }

    public String getOptionalContextName() {
        return optionalContextName;
    }

    public void setOptionalContextName(String optionalContextName) {
        this.optionalContextName = optionalContextName;
    }

    public List<ExpressionScriptProvided> getScriptExpressions() {
        return scriptExpressions;
    }

    public void setScriptExpressions(List<ExpressionScriptProvided> scriptExpressions) {
        this.scriptExpressions = scriptExpressions;
    }

    public CreateDataFlowDesc getCreateDataFlowDesc() {
        return createDataFlowDesc;
    }

    public void setCreateDataFlowDesc(CreateDataFlowDesc createDataFlowDesc) {
        this.createDataFlowDesc = createDataFlowDesc;
    }

    public CreateExpressionDesc getCreateExpressionDesc() {
        return createExpressionDesc;
    }

    public void setCreateExpressionDesc(CreateExpressionDesc createExpressionDesc) {
        this.createExpressionDesc = createExpressionDesc;
    }

    public FireAndForgetSpec getFireAndForgetSpec() {
        return fireAndForgetSpec;
    }

    public void setFireAndForgetSpec(FireAndForgetSpec fireAndForgetSpec) {
        this.fireAndForgetSpec = fireAndForgetSpec;
    }

    public IntoTableSpec getIntoTableSpec() {
        return intoTableSpec;
    }

    public void setIntoTableSpec(IntoTableSpec intoTableSpec) {
        this.intoTableSpec = intoTableSpec;
    }

    public Set<ExprTableAccessNode> getTableExpressions() {
        return tableExpressions;
    }

    public void setTableExpressions(Set<ExprTableAccessNode> tableExpressions) {
        this.tableExpressions = tableExpressions;
    }

    public CreateTableDesc getCreateTableDesc() {
        return createTableDesc;
    }

    public void setCreateTableDesc(CreateTableDesc createTableDesc) {
        this.createTableDesc = createTableDesc;
    }
}
