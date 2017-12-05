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
package com.espertech.esper.core.start;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.factory.StatementAgentInstanceFactoryCreateTable;
import com.espertech.esper.core.context.factory.StatementAgentInstanceFactoryCreateTableResult;
import com.espertech.esper.core.context.mgr.ContextManagedStatementCreateAggregationVariableDesc;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.ContextMergeView;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.ExprEvaluatorContextStatement;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;
import com.espertech.esper.core.service.resource.StatementResourceHolder;
import com.espertech.esper.epl.agg.access.AggregationAccessor;
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.agg.service.common.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.common.AggregationStateFactory;
import com.espertech.esper.epl.agg.service.common.AggregationStateFactoryForge;
import com.espertech.esper.epl.annotation.AnnotationUtil;
import com.espertech.esper.epl.core.engineimport.EngineImportException;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.epl.spec.*;
import com.espertech.esper.epl.table.mgmt.*;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.epl.variable.VariableServiceUtil;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventTypeUtility;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.view.ViewProcessingException;
import com.espertech.esper.view.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Starts and provides the stop method for EPL statements.
 */
public class EPStatementStartMethodCreateTable extends EPStatementStartMethodBase {
    private static final Logger log = LoggerFactory.getLogger(EPStatementStartMethodCreateTable.class);

    public EPStatementStartMethodCreateTable(StatementSpecCompiled statementSpec) {
        super(statementSpec);
    }

    public EPStatementStartResult startInternal(final EPServicesContext services, final StatementContext statementContext, boolean isNewStatement, boolean isRecoveringStatement, boolean isRecoveringResilient) throws ExprValidationException, ViewProcessingException {
        final CreateTableDesc createDesc = statementSpec.getCreateTableDesc();

        // determine whether already declared
        VariableServiceUtil.checkAlreadyDeclaredVariable(createDesc.getTableName(), services.getVariableService());
        if (isNewStatement) {
            VariableServiceUtil.checkAlreadyDeclaredTable(createDesc.getTableName(), services.getTableService());
        }
        if (services.getEventAdapterService().getExistsTypeByName(createDesc.getTableName()) != null) {
            throw new ExprValidationException("An event type or schema by name '" + createDesc.getTableName() + "' already exists");
        }

        // Determine event type names
        String internalTypeName = "table_" + createDesc.getTableName() + "__internal";
        String publicTypeName = "table_" + createDesc.getTableName() + "__public";

        final TableMetadata metadata;
        try {
            // determine key types
            Class[] keyTypes = getKeyTypes(createDesc.getColumns(), services.getEngineImportService());

            // check column naming, interpret annotations
            List<TableColumnDesc> columnDescs = validateExpressions(createDesc.getColumns(), services, statementContext);

            // analyze and plan the state holders
            TableAccessAnalysisResult plan = analyzePlanAggregations(createDesc.getTableName(), statementContext, columnDescs, services, internalTypeName, publicTypeName);
            final TableStateRowFactory tableStateRowFactory = plan.getStateRowFactory();

            // register new table
            boolean queryPlanLogging = services.getConfigSnapshot().getEngineDefaults().getLogging().isEnableQueryPlan();
            metadata = services.getTableService().addTable(createDesc.getTableName(), statementContext.getExpression(), statementContext.getStatementName(), keyTypes, plan.getTableColumns(), tableStateRowFactory, plan.getNumberMethodAggregations(), statementContext, plan.getInternalEventType(),
                    plan.getPublicEventType(), plan.getEventToPublic(), queryPlanLogging);
        } catch (ExprValidationException ex) {
            services.getEventAdapterService().removeType(internalTypeName);
            services.getEventAdapterService().removeType(publicTypeName);
            throw ex;
        }

        // allocate context factory
        StatementAgentInstanceFactoryCreateTable contextFactory = new StatementAgentInstanceFactoryCreateTable(metadata);
        statementContext.setStatementAgentInstanceFactory(contextFactory);
        Viewable outputView;
        EPStatementStopMethod stopStatementMethod;
        EPStatementDestroyMethod destroyStatementMethod;

        if (statementSpec.getOptionalContextName() != null) {
            final String contextName = statementSpec.getOptionalContextName();
            ContextMergeView mergeView = new ContextMergeView(metadata.getPublicEventType());
            outputView = mergeView;
            ContextManagedStatementCreateAggregationVariableDesc statement = new ContextManagedStatementCreateAggregationVariableDesc(statementSpec, statementContext, mergeView, contextFactory);
            services.getContextManagementService().addStatement(statementSpec.getOptionalContextName(), statement, isRecoveringResilient);

            stopStatementMethod = new EPStatementStopMethod() {
                public void stop() {
                    services.getContextManagementService().stoppedStatement(contextName, statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getExpression(), statementContext.getExceptionHandlingService());
                }
            };

            destroyStatementMethod = new EPStatementDestroyMethod() {
                public void destroy() {
                    services.getContextManagementService().destroyedStatement(contextName, statementContext.getStatementName(), statementContext.getStatementId());
                    services.getStatementVariableRefService().removeReferencesStatement(statementContext.getStatementName());
                }
            };
        } else {
            AgentInstanceContext defaultAgentInstanceContext = getDefaultAgentInstanceContext(statementContext);
            StatementAgentInstanceFactoryCreateTableResult result = contextFactory.newContext(defaultAgentInstanceContext, false);
            if (statementContext.getStatementExtensionServicesContext() != null && statementContext.getStatementExtensionServicesContext().getStmtResources() != null) {
                StatementResourceHolder holder = statementContext.getStatementExtensionServicesContext().extractStatementResourceHolder(result);
                statementContext.getStatementExtensionServicesContext().getStmtResources().setUnpartitioned(holder);
            }
            outputView = result.getFinalView();

            stopStatementMethod = new EPStatementStopMethod() {
                public void stop() {
                }
            };
            destroyStatementMethod = new EPStatementDestroyMethod() {
                public void destroy() {
                    services.getStatementVariableRefService().removeReferencesStatement(statementContext.getStatementName());
                }
            };
        }

        services.getStatementVariableRefService().addReferences(statementContext.getStatementName(), createDesc.getTableName());

        return new EPStatementStartResult(outputView, stopStatementMethod, destroyStatementMethod);
    }

    private Class[] getKeyTypes(List<CreateTableColumn> columns, EngineImportService engineImportService)
            throws ExprValidationException {
        List<Class> keys = new ArrayList<Class>();
        for (CreateTableColumn col : columns) {
            if (col.getPrimaryKey() == null || !col.getPrimaryKey()) {
                continue;
            }
            String msg = "Column '" + col.getColumnName() + "' may not be tagged as primary key";
            if (col.getOptExpression() != null) {
                throw new ExprValidationException(msg + ", an expression cannot become a primary key column");
            }
            if (col.getOptTypeIsArray() != null && col.getOptTypeIsArray()) {
                throw new ExprValidationException(msg + ", an array-typed column cannot become a primary key column");
            }
            Object type = EventTypeUtility.buildType(new ColumnDesc(col.getColumnName(), col.getOptTypeName(), false, false), engineImportService);
            if (!(type instanceof Class)) {
                throw new ExprValidationException(msg + ", received unexpected event type '" + type + "'");
            }
            keys.add((Class) type);
        }
        return keys.toArray(new Class[keys.size()]);
    }

    private ExprAggregateNode validateAggregationExpr(ExprNode columnExpressionType, EventType optionalProvidedType, EPServicesContext services, StatementContext statementContext)
            throws ExprValidationException {
        // determine validation context types and istream/irstream
        EventType[] types;
        String[] streamNames;
        boolean[] istreamOnly;
        if (optionalProvidedType != null) {
            types = new EventType[]{optionalProvidedType};
            streamNames = new String[]{types[0].getName()};
            istreamOnly = new boolean[]{false}; // always false (expected to be bound by data window), use "ever"-aggregation functions otherwise
        } else {
            types = new EventType[0];
            streamNames = new String[0];
            istreamOnly = new boolean[0];
        }

        StreamTypeServiceImpl streamTypeService = new StreamTypeServiceImpl(types, streamNames, istreamOnly, services.getEngineURI(), false, false);
        ExprValidationContext validationContext = new ExprValidationContext(streamTypeService, statementContext.getEngineImportService(), statementContext.getStatementExtensionServicesContext(), null, statementContext.getSchedulingService(), statementContext.getVariableService(), statementContext.getTableService(), new ExprEvaluatorContextStatement(statementContext, false), statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), false, false, false, false, null, false);

        // substitute parameter nodes
        for (ExprNode childNode : columnExpressionType.getChildNodes()) {
            if (childNode instanceof ExprIdentNode) {
                ExprIdentNode identNode = (ExprIdentNode) childNode;
                String propname = identNode.getFullUnresolvedName().trim();
                Class clazz = JavaClassHelper.getClassForSimpleName(propname, services.getEngineImportService().getClassForNameProvider());
                if (propname.toLowerCase(Locale.ENGLISH).trim().equals("object")) {
                    clazz = Object.class;
                }
                EngineImportException ex = null;
                if (clazz == null) {
                    try {
                        clazz = services.getEngineImportService().resolveClass(propname, false);
                    } catch (EngineImportException e) {
                        ex = e;
                    }
                }
                if (clazz != null) {
                    ExprTypedNoEvalNode typeNode = new ExprTypedNoEvalNode(propname, clazz);
                    ExprNodeUtilityCore.replaceChildNode(columnExpressionType, identNode, typeNode);
                } else {
                    if (optionalProvidedType == null) {
                        if (ex != null) {
                            throw new ExprValidationException("Failed to resolve type '" + propname + "': " + ex.getMessage(), ex);
                        }
                        throw new ExprValidationException("Failed to resolve type '" + propname + "'");
                    }
                }
            }
        }

        // validate
        ExprNode validated = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.CREATETABLECOLUMN, columnExpressionType, validationContext);
        if (!(validated instanceof ExprAggregateNode)) {
            throw new ExprValidationException("Expression '" + ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(validated) + "' is not an aggregation");
        }

        return (ExprAggregateNode) validated;
    }

    private List<TableColumnDesc> validateExpressions(List<CreateTableColumn> columns, EPServicesContext services, StatementContext statementContext)
            throws ExprValidationException {
        Set<String> columnNames = new HashSet<String>();
        List<TableColumnDesc> descriptors = new ArrayList<TableColumnDesc>();

        int positionInDeclaration = 0;
        for (CreateTableColumn column : columns) {
            String msgprefix = "For column '" + column.getColumnName() + "'";

            // check duplicate name
            if (columnNames.contains(column.getColumnName())) {
                throw new ExprValidationException("Column '" + column.getColumnName() + "' is listed more than once");
            }
            columnNames.add(column.getColumnName());

            // determine presence of type annotation
            EventType optionalEventType = validateExpressionGetEventType(msgprefix, column.getAnnotations(), services.getEventAdapterService());

            // aggregation node
            TableColumnDesc descriptor;
            if (column.getOptExpression() != null) {
                ExprAggregateNode validated = validateAggregationExpr(column.getOptExpression(), optionalEventType, services, statementContext);
                descriptor = new TableColumnDescAgg(positionInDeclaration, column.getColumnName(), validated, optionalEventType);
            } else {
                Object unresolvedType = EventTypeUtility.buildType(new ColumnDesc(column.getColumnName(), column.getOptTypeName(), column.getOptTypeIsArray() == null ? false : column.getOptTypeIsArray(), column.getOptTypeIsPrimitiveArray() == null ? false : column.getOptTypeIsPrimitiveArray()),
                        services.getEngineImportService());
                descriptor = new TableColumnDescTyped(positionInDeclaration, column.getColumnName(), unresolvedType, column.getPrimaryKey() == null ? false : column.getPrimaryKey());
            }
            descriptors.add(descriptor);
            positionInDeclaration++;
        }

        return descriptors;
    }

    private static EventType validateExpressionGetEventType(String msgprefix, List<AnnotationDesc> annotations, EventAdapterService eventAdapterService)
            throws ExprValidationException {
        Map<String, List<AnnotationDesc>> annos = AnnotationUtil.mapByNameLowerCase(annotations);

        // check annotations used
        List<AnnotationDesc> typeAnnos = annos.remove("type");
        if (!annos.isEmpty()) {
            throw new ExprValidationException(msgprefix + " unrecognized annotation '" + annos.keySet().iterator().next() + "'");
        }

        // type determination
        EventType optionalType = null;
        if (typeAnnos != null) {
            String typeName = AnnotationUtil.getExpectSingleStringValue(msgprefix, typeAnnos);
            optionalType = eventAdapterService.getExistsTypeByName(typeName);
            if (optionalType == null) {
                throw new ExprValidationException(msgprefix + " failed to find event type '" + typeName + "'");
            }
        }

        return optionalType;
    }

    private TableAccessAnalysisResult analyzePlanAggregations(String tableName, StatementContext statementContext, List<TableColumnDesc> columns, EPServicesContext services, String internalTypeName, String publicTypeName)
            throws ExprValidationException {
        // once upfront: obtains aggregation factories for each aggregation
        // we do this once as a factory may be a heavier object
        Map<TableColumnDesc, AggregationMethodFactory> aggregationFactories = new HashMap<TableColumnDesc, AggregationMethodFactory>();
        for (TableColumnDesc column : columns) {
            if (column instanceof TableColumnDescAgg) {
                TableColumnDescAgg agg = (TableColumnDescAgg) column;
                AggregationMethodFactory factory = agg.getAggregation().getFactory();
                aggregationFactories.put(column, factory);
            }
        }

        // sort into these categories:
        // plain / method-agg / access-agg
        // compile all-column public types
        List<TableColumnDescTyped> plainColumns = new ArrayList<TableColumnDescTyped>();
        List<TableColumnDescAgg> methodAggColumns = new ArrayList<TableColumnDescAgg>();
        List<TableColumnDescAgg> accessAggColumns = new ArrayList<TableColumnDescAgg>();
        Map<String, Object> allColumnsPublicTypes = new LinkedHashMap<String, Object>();
        for (TableColumnDesc column : columns) {

            // handle plain types
            if (column instanceof TableColumnDescTyped) {
                TableColumnDescTyped typed = (TableColumnDescTyped) column;
                plainColumns.add(typed);
                allColumnsPublicTypes.put(column.getColumnName(), typed.getUnresolvedType());
                continue;
            }

            // handle aggs
            TableColumnDescAgg agg = (TableColumnDescAgg) column;
            AggregationMethodFactory aggFactory = aggregationFactories.get(agg);
            if (aggFactory.isAccessAggregation()) {
                accessAggColumns.add(agg);
            } else {
                methodAggColumns.add(agg);
            }
            allColumnsPublicTypes.put(column.getColumnName(), agg.getAggregation().getEvaluationType());
        }

        // determine column metadata
        //
        Map<String, TableMetadataColumn> columnMetadata = new LinkedHashMap<String, TableMetadataColumn>();

        // handle typed columns
        Map<String, Object> allColumnsInternalTypes = new LinkedHashMap<String, Object>();
        allColumnsInternalTypes.put(TableService.INTERNAL_RESERVED_PROPERTY, Object.class);
        int indexPlain = 1;
        List<Integer> groupKeyIndexes = new ArrayList<Integer>();
        TableMetadataColumnPairPlainCol[] assignPairsPlain = new TableMetadataColumnPairPlainCol[plainColumns.size()];
        for (TableColumnDescTyped typedColumn : plainColumns) {
            allColumnsInternalTypes.put(typedColumn.getColumnName(), typedColumn.getUnresolvedType());
            columnMetadata.put(typedColumn.getColumnName(), new TableMetadataColumnPlain(typedColumn.getColumnName(), typedColumn.isKey(), indexPlain));
            if (typedColumn.isKey()) {
                groupKeyIndexes.add(indexPlain);
            }
            assignPairsPlain[indexPlain - 1] = new TableMetadataColumnPairPlainCol(typedColumn.getPositionInDeclaration(), indexPlain);
            indexPlain++;
        }

        // determine internally-used event type
        // for use by indexes and lookups
        ObjectArrayEventType internalEventType;
        ObjectArrayEventType publicEventType;
        try {
            internalEventType = (ObjectArrayEventType) services.getEventAdapterService().addNestableObjectArrayType(internalTypeName, allColumnsInternalTypes, null, false, false, false, false, false, true, tableName);
            publicEventType = (ObjectArrayEventType) services.getEventAdapterService().addNestableObjectArrayType(publicTypeName, allColumnsPublicTypes, null, false, false, false, false, false, false, null);
        } catch (EPException ex) {
            throw new ExprValidationException("Invalid type information: " + ex.getMessage(), ex);
        }
        services.getStatementEventTypeRefService().addReferences(statementContext.getStatementName(), new String[]{internalTypeName, publicTypeName});

        // handle aggregation-methods single-func first.
        AggregationMethodFactory[] methodFactories = new AggregationMethodFactory[methodAggColumns.size()];
        int index = 0;
        TableMetadataColumnPairAggMethod[] assignPairsMethod = new TableMetadataColumnPairAggMethod[methodAggColumns.size()];
        for (TableColumnDescAgg column : methodAggColumns) {
            AggregationMethodFactory factory = aggregationFactories.get(column);
            EPType optionalEnumerationType = EPTypeHelper.optionalFromEnumerationExpr(statementContext.getStatementId(), statementContext.getEventAdapterService(), column.getAggregation());
            methodFactories[index] = factory;
            columnMetadata.put(column.getColumnName(), new TableMetadataColumnAggregation(column.getColumnName(), factory, index, null, optionalEnumerationType, column.getOptionalAssociatedType()));
            assignPairsMethod[index] = new TableMetadataColumnPairAggMethod(column.getPositionInDeclaration());
            index++;
        }

        // handle access-aggregation (sharable, multi-value) aggregations
        AggregationStateFactory[] stateFactories = new AggregationStateFactory[accessAggColumns.size()];
        TableMetadataColumnPairAggAccess[] assignPairsAccess = new TableMetadataColumnPairAggAccess[accessAggColumns.size()];
        index = 0;
        for (TableColumnDescAgg column : accessAggColumns) {
            AggregationMethodFactory factory = aggregationFactories.get(column);
            AggregationStateFactoryForge forge = factory.getAggregationStateFactory(false);
            stateFactories[index] = forge.makeFactory(statementContext.getEngineImportService(), false, statementContext.getStatementName());
            AggregationAccessor accessor = factory.getAccessorForge() == null ? null : factory.getAccessorForge().getAccessor(statementContext.getEngineImportService(), false, statementContext.getStatementName());
            AggregationAccessorSlotPair pair = new AggregationAccessorSlotPair(index, accessor);
            EPType optionalEnumerationType = EPTypeHelper.optionalFromEnumerationExpr(statementContext.getStatementId(), statementContext.getEventAdapterService(), column.getAggregation());
            columnMetadata.put(column.getColumnName(), new TableMetadataColumnAggregation(column.getColumnName(), factory, -1, pair, optionalEnumerationType, column.getOptionalAssociatedType()));
            assignPairsAccess[index] = new TableMetadataColumnPairAggAccess(column.getPositionInDeclaration(), accessor);
            index++;
        }

        // create state factory
        int[] groupKeyIndexesArr = CollectionUtil.intArray(groupKeyIndexes);
        TableStateRowFactory stateRowFactory = new TableStateRowFactory(internalEventType, statementContext.getEngineImportService(), methodFactories, stateFactories, groupKeyIndexesArr, services.getEventAdapterService());

        // create public event provision
        TableMetadataInternalEventToPublic eventToPublic = new TableMetadataInternalEventToPublic(publicEventType,
                assignPairsPlain, assignPairsMethod, assignPairsAccess, services.getEventAdapterService());

        return new TableAccessAnalysisResult(stateRowFactory, columnMetadata, methodAggColumns.size(), internalEventType, publicEventType, eventToPublic);
    }

    private static class TableAccessAnalysisResult {
        private final TableStateRowFactory stateRowFactory;
        private final Map<String, TableMetadataColumn> tableColumns;
        private final int numberMethodAggregations;
        private final ObjectArrayEventType internalEventType;
        private final ObjectArrayEventType publicEventType;
        private final TableMetadataInternalEventToPublic eventToPublic;

        private TableAccessAnalysisResult(TableStateRowFactory stateRowFactory, Map<String, TableMetadataColumn> tableColumns, int numberMethodAggregations, ObjectArrayEventType internalEventType, ObjectArrayEventType publicEventType, TableMetadataInternalEventToPublic eventToPublic) {
            this.stateRowFactory = stateRowFactory;
            this.tableColumns = tableColumns;
            this.numberMethodAggregations = numberMethodAggregations;
            this.internalEventType = internalEventType;
            this.publicEventType = publicEventType;
            this.eventToPublic = eventToPublic;
        }

        public TableStateRowFactory getStateRowFactory() {
            return stateRowFactory;
        }

        public Map<String, TableMetadataColumn> getTableColumns() {
            return tableColumns;
        }

        public int getNumberMethodAggregations() {
            return numberMethodAggregations;
        }

        public ObjectArrayEventType getInternalEventType() {
            return internalEventType;
        }

        public ObjectArrayEventType getPublicEventType() {
            return publicEventType;
        }

        public TableMetadataInternalEventToPublic getEventToPublic() {
            return eventToPublic;
        }
    }
}
