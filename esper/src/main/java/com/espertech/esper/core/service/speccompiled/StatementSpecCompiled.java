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
package com.espertech.esper.core.service.speccompiled;

import com.espertech.esper.epl.declexpr.ExprDeclaredNode;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.spec.*;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.filterspec.FilterSpecCompiled;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Specification object representing a complete EPL statement including all EPL constructs.
 */
public class StatementSpecCompiled {
    public static final StatementSpecCompiled DEFAULT_SELECT_ALL_EMPTY;

    static {
        DEFAULT_SELECT_ALL_EMPTY = new StatementSpecCompiled();
        DEFAULT_SELECT_ALL_EMPTY.getSelectClauseSpec().setSelectExprList(new SelectClauseElementWildcard());
    }

    private final OnTriggerDesc onTriggerDesc;
    private final CreateWindowDesc createWindowDesc;
    private final CreateIndexDesc createIndexDesc;
    private final CreateVariableDesc createVariableDesc;
    private final CreateTableDesc createTableDesc;
    private final CreateSchemaDesc createSchemaDesc;
    private InsertIntoDesc insertIntoDesc;
    private SelectClauseStreamSelectorEnum selectStreamDirEnum;
    private SelectClauseSpecCompiled selectClauseSpec;
    private StreamSpecCompiled[] streamSpecs;
    private final OuterJoinDesc[] outerJoinDescList;
    private ExprNode filterExprRootNode;
    private ExprNode havingExprRootNode;
    private final OutputLimitSpec outputLimitSpec;
    private final OrderByItem[] orderByList;
    private final ExprSubselectNode[] subSelectExpressions;
    private final ExprDeclaredNode[] declaredExpressions;
    private final ExpressionScriptProvided[] scripts;
    private final Set<String> variableReferences;
    private final RowLimitSpec rowLimitSpec;
    private final String[] eventTypeReferences;
    private final Annotation[] annotations;
    private final UpdateDesc updateSpec;
    private final MatchRecognizeSpec matchRecognizeSpec;
    private final ForClauseSpec forClauseSpec;
    private final Map<Integer, List<ExprNode>> sqlParameters;
    private final CreateContextDesc contextDesc;
    private final String optionalContextName;
    private final CreateDataFlowDesc createGraphDesc;
    private final CreateExpressionDesc createExpressionDesc;
    private final FireAndForgetSpec fireAndForgetSpec;
    private final GroupByClauseExpressions groupByExpressions;
    private final IntoTableSpec intoTableSpec;
    private final ExprTableAccessNode[] tableNodes;
    private FilterSpecCompiled[] filterSpecsOverall;
    private NamedWindowConsumerStreamSpec[] namedWindowConsumersAll;

    public StatementSpecCompiled(OnTriggerDesc onTriggerDesc,
                                 CreateWindowDesc createWindowDesc,
                                 CreateIndexDesc createIndexDesc,
                                 CreateVariableDesc createVariableDesc,
                                 CreateTableDesc createTableDesc,
                                 CreateSchemaDesc createSchemaDesc,
                                 InsertIntoDesc insertIntoDesc,
                                 SelectClauseStreamSelectorEnum selectClauseStreamSelectorEnum,
                                 SelectClauseSpecCompiled selectClauseSpec,
                                 StreamSpecCompiled[] streamSpecs,
                                 OuterJoinDesc[] outerJoinDescList,
                                 ExprNode filterExprRootNode,
                                 ExprNode havingExprRootNode,
                                 OutputLimitSpec outputLimitSpec,
                                 OrderByItem[] orderByList,
                                 ExprSubselectNode[] subSelectExpressions,
                                 ExprDeclaredNode[] declaredExpressions,
                                 ExpressionScriptProvided[] scripts,
                                 Set<String> variableReferences,
                                 RowLimitSpec rowLimitSpec,
                                 String[] eventTypeReferences,
                                 Annotation[] annotations,
                                 UpdateDesc updateSpec,
                                 MatchRecognizeSpec matchRecognizeSpec,
                                 ForClauseSpec forClauseSpec,
                                 Map<Integer, List<ExprNode>> sqlParameters,
                                 CreateContextDesc contextDesc,
                                 String optionalContextName,
                                 CreateDataFlowDesc createGraphDesc,
                                 CreateExpressionDesc createExpressionDesc,
                                 FireAndForgetSpec fireAndForgetSpec,
                                 GroupByClauseExpressions groupByExpressions,
                                 IntoTableSpec intoTableSpec,
                                 ExprTableAccessNode[] tableNodes) {
        this.onTriggerDesc = onTriggerDesc;
        this.createWindowDesc = createWindowDesc;
        this.createIndexDesc = createIndexDesc;
        this.createVariableDesc = createVariableDesc;
        this.createTableDesc = createTableDesc;
        this.createSchemaDesc = createSchemaDesc;
        this.insertIntoDesc = insertIntoDesc;
        this.selectStreamDirEnum = selectClauseStreamSelectorEnum;
        this.selectClauseSpec = selectClauseSpec;
        this.streamSpecs = streamSpecs;
        this.outerJoinDescList = outerJoinDescList;
        this.filterExprRootNode = filterExprRootNode;
        this.havingExprRootNode = havingExprRootNode;
        this.outputLimitSpec = outputLimitSpec;
        this.orderByList = orderByList;
        this.subSelectExpressions = subSelectExpressions;
        this.declaredExpressions = declaredExpressions;
        this.scripts = scripts;
        this.variableReferences = variableReferences;
        this.rowLimitSpec = rowLimitSpec;
        this.eventTypeReferences = eventTypeReferences;
        this.annotations = annotations;
        this.updateSpec = updateSpec;
        this.matchRecognizeSpec = matchRecognizeSpec;
        this.forClauseSpec = forClauseSpec;
        this.sqlParameters = sqlParameters;
        this.contextDesc = contextDesc;
        this.optionalContextName = optionalContextName;
        this.createGraphDesc = createGraphDesc;
        this.createExpressionDesc = createExpressionDesc;
        this.fireAndForgetSpec = fireAndForgetSpec;
        this.groupByExpressions = groupByExpressions;
        this.intoTableSpec = intoTableSpec;
        this.tableNodes = tableNodes;
    }

    /**
     * Ctor.
     */
    public StatementSpecCompiled() {
        onTriggerDesc = null;
        createWindowDesc = null;
        createIndexDesc = null;
        createVariableDesc = null;
        createTableDesc = null;
        createSchemaDesc = null;
        insertIntoDesc = null;
        selectStreamDirEnum = SelectClauseStreamSelectorEnum.RSTREAM_ISTREAM_BOTH;
        selectClauseSpec = new SelectClauseSpecCompiled(false);
        streamSpecs = StreamSpecCompiled.EMPTY_STREAM_ARRAY;
        outerJoinDescList = OuterJoinDesc.EMPTY_OUTERJOIN_ARRAY;
        filterExprRootNode = null;
        havingExprRootNode = null;
        outputLimitSpec = null;
        orderByList = OrderByItem.EMPTY_ORDERBY_ARRAY;
        subSelectExpressions = ExprSubselectNode.EMPTY_SUBSELECT_ARRAY;
        declaredExpressions = ExprNodeUtilityRich.EMPTY_DECLARED_ARR;
        scripts = ExprNodeUtilityRich.EMPTY_SCRIPTS;
        variableReferences = new HashSet<String>();
        rowLimitSpec = null;
        eventTypeReferences = new String[0];
        annotations = new Annotation[0];
        updateSpec = null;
        matchRecognizeSpec = null;
        forClauseSpec = null;
        sqlParameters = null;
        contextDesc = null;
        optionalContextName = null;
        createGraphDesc = null;
        createExpressionDesc = null;
        fireAndForgetSpec = null;
        groupByExpressions = null;
        intoTableSpec = null;
        tableNodes = null;
    }

    /**
     * Returns the specification for an create-window statement.
     *
     * @return create-window spec, or null if not such a statement
     */
    public CreateWindowDesc getCreateWindowDesc() {
        return createWindowDesc;
    }

    /**
     * Returns the create-variable statement descriptor.
     *
     * @return create-variable spec
     */
    public CreateVariableDesc getCreateVariableDesc() {
        return createVariableDesc;
    }

    /**
     * Returns the FROM-clause stream definitions.
     *
     * @return list of stream specifications
     */
    public StreamSpecCompiled[] getStreamSpecs() {
        return streamSpecs;
    }

    /**
     * Sets the FROM-clause stream definitions.
     *
     * @param streamSpecs list of stream specifications
     */
    public void setStreamSpecs(StreamSpecCompiled[] streamSpecs) {
        this.streamSpecs = streamSpecs;
    }

    /**
     * Returns SELECT-clause list of expressions.
     *
     * @return list of expressions and optional name
     */
    public SelectClauseSpecCompiled getSelectClauseSpec() {
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
    public OuterJoinDesc[] getOuterJoinDescList() {
        return outerJoinDescList;
    }

    /**
     * Returns expression root node representing the having-clause, if present, or null if no having clause was supplied.
     *
     * @return having-clause expression top node
     */
    public ExprNode getHavingExprRootNode() {
        return havingExprRootNode;
    }

    public void setHavingExprRootNode(ExprNode havingExprRootNode) {
        this.havingExprRootNode = havingExprRootNode;
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
    public OrderByItem[] getOrderByList() {
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
     * Set the where clause filter node.
     *
     * @param optionalFilterNode is the where-clause filter node
     */
    public void setFilterExprRootNode(ExprNode optionalFilterNode) {
        filterExprRootNode = optionalFilterNode;
    }

    /**
     * Returns the list of lookup expression nodes.
     *
     * @return lookup nodes
     */
    public ExprSubselectNode[] getSubSelectExpressions() {
        return subSelectExpressions;
    }

    /**
     * Returns the specification for an on-delete or on-select statement.
     *
     * @return on-trigger spec, or null if not such a statement
     */
    public OnTriggerDesc getOnTriggerDesc() {
        return onTriggerDesc;
    }

    /**
     * Returns true to indicate the statement has variables.
     *
     * @return true for statements that use variables
     */
    public boolean isHasVariables() {
        return variableReferences != null && !variableReferences.isEmpty();
    }

    /**
     * Sets the stream selection.
     *
     * @param selectStreamDirEnum stream selection
     */
    public void setSelectStreamDirEnum(SelectClauseStreamSelectorEnum selectStreamDirEnum) {
        this.selectStreamDirEnum = selectStreamDirEnum;
    }

    /**
     * Returns the row limit specification, or null if none supplied.
     *
     * @return row limit spec if any
     */
    public RowLimitSpec getRowLimitSpec() {
        return rowLimitSpec;
    }

    /**
     * Returns the event type name in used by the statement.
     *
     * @return set of event type name
     */
    public String[] getEventTypeReferences() {
        return eventTypeReferences;
    }

    /**
     * Returns annotations or empty array if none.
     *
     * @return annotations
     */
    public Annotation[] getAnnotations() {
        return annotations;
    }

    /**
     * Sets the insert-into clause.
     *
     * @param insertIntoDesc insert-into clause.
     */
    public void setInsertIntoDesc(InsertIntoDesc insertIntoDesc) {
        this.insertIntoDesc = insertIntoDesc;
    }

    /**
     * Sets the select clause.
     *
     * @param selectClauseSpec select clause
     */
    public void setSelectClauseSpec(SelectClauseSpecCompiled selectClauseSpec) {
        this.selectClauseSpec = selectClauseSpec;
    }

    /**
     * Returns the update spec if update clause is used.
     *
     * @return update desc
     */
    public UpdateDesc getUpdateSpec() {
        return updateSpec;
    }

    /**
     * Returns the match recognize spec, if used
     *
     * @return match recognize spec
     */
    public MatchRecognizeSpec getMatchRecognizeSpec() {
        return matchRecognizeSpec;
    }

    /**
     * Return variables referenced.
     *
     * @return variables
     */
    public Set<String> getVariableReferences() {
        return variableReferences;
    }

    /**
     * Returns create index
     *
     * @return create index
     */
    public CreateIndexDesc getCreateIndexDesc() {
        return createIndexDesc;
    }

    public CreateSchemaDesc getCreateSchemaDesc() {
        return createSchemaDesc;
    }

    public ForClauseSpec getForClauseSpec() {
        return forClauseSpec;
    }

    public Map<Integer, List<ExprNode>> getSqlParameters() {
        return sqlParameters;
    }

    public ExprDeclaredNode[] getDeclaredExpressions() {
        return declaredExpressions;
    }

    public CreateContextDesc getContextDesc() {
        return contextDesc;
    }

    public String getOptionalContextName() {
        return optionalContextName;
    }

    public CreateDataFlowDesc getCreateGraphDesc() {
        return createGraphDesc;
    }

    public CreateExpressionDesc getCreateExpressionDesc() {
        return createExpressionDesc;
    }

    public FireAndForgetSpec getFireAndForgetSpec() {
        return fireAndForgetSpec;
    }

    public GroupByClauseExpressions getGroupByExpressions() {
        return groupByExpressions;
    }

    public IntoTableSpec getIntoTableSpec() {
        return intoTableSpec;
    }

    public ExprTableAccessNode[] getTableNodes() {
        return tableNodes;
    }

    public CreateTableDesc getCreateTableDesc() {
        return createTableDesc;
    }

    public void setFilterSpecsOverall(FilterSpecCompiled[] filterSpecsOverall) {
        this.filterSpecsOverall = filterSpecsOverall;
    }

    public FilterSpecCompiled[] getFilterSpecsOverall() {
        return filterSpecsOverall;
    }

    public NamedWindowConsumerStreamSpec[] getNamedWindowConsumersAll() {
        return namedWindowConsumersAll;
    }

    public void setNamedWindowConsumersAll(NamedWindowConsumerStreamSpec[] namedWindowConsumersAll) {
        this.namedWindowConsumersAll = namedWindowConsumersAll;
    }

    public ExpressionScriptProvided[] getScripts() {
        return scripts;
    }
}
