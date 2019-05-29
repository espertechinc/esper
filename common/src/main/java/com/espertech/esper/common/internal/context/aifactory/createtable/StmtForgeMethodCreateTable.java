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
package com.espertech.esper.common.internal.context.aifactory.createtable;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationIDGenerator;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlan;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlanner;
import com.espertech.esper.common.internal.compile.stage1.spec.AnnotationDesc;
import com.espertech.esper.common.internal.compile.stage1.spec.ColumnDesc;
import com.espertech.esper.common.internal.compile.stage1.spec.CreateTableColumn;
import com.espertech.esper.common.internal.compile.stage1.spec.CreateTableDesc;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.*;
import com.espertech.esper.common.internal.context.compile.ContextMetaData;
import com.espertech.esper.common.internal.context.module.StatementAIFactoryProvider;
import com.espertech.esper.common.internal.context.module.StatementFields;
import com.espertech.esper.common.internal.context.module.StatementInformationalsCompileTime;
import com.espertech.esper.common.internal.context.module.StatementProvider;
import com.espertech.esper.common.internal.epl.agg.core.*;
import com.espertech.esper.common.internal.epl.annotation.AnnotationUtil;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNode;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectSubscriberDescriptor;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.epl.table.compiletime.*;
import com.espertech.esper.common.internal.epl.util.EPLValidationUtil;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.event.core.BaseNestableEventUtil;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.core.EventTypeNameUtil;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.rettype.EPType;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;
import com.espertech.esper.common.internal.serde.compiletime.eventtype.SerdeEventPropertyDesc;
import com.espertech.esper.common.internal.serde.compiletime.eventtype.SerdeEventPropertyUtility;
import com.espertech.esper.common.internal.serde.compiletime.eventtype.SerdeEventTypeUtility;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.settings.ClasspathImportException;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;
import com.espertech.esper.common.internal.util.IntArrayUtil;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.*;

public class StmtForgeMethodCreateTable implements StmtForgeMethod {
    public final static String INTERNAL_RESERVED_PROPERTY = "internal-reserved";

    private final StatementBaseInfo base;

    public StmtForgeMethodCreateTable(StatementBaseInfo base) {
        this.base = base;
    }

    public StmtForgeMethodResult make(String packageName, String classPostfix, StatementCompileTimeServices services) throws ExprValidationException {
        try {
            return build(packageName, classPostfix, services);
        } catch (ExprValidationException ex) {
            throw ex;
        } catch (Throwable t) {
            throw new ExprValidationException("Unexpected exception creating table '" + base.getStatementSpec().getRaw().getCreateTableDesc().getTableName() + "': " + t.getMessage(), t);
        }
    }

    private StmtForgeMethodResult build(String packageName, String classPostfix, StatementCompileTimeServices services) throws ExprValidationException {
        CreateTableDesc createDesc = base.getStatementSpec().getRaw().getCreateTableDesc();
        String tableName = createDesc.getTableName();
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(2);

        // determine whether already declared as table or variable
        EPLValidationUtil.validateAlreadyExistsTableOrVariable(tableName, services.getVariableCompileTimeResolver(), services.getTableCompileTimeResolver(), services.getEventTypeCompileTimeResolver());

        // determine key types
        validateKeyTypes(createDesc.getColumns(), services.getClasspathImportServiceCompileTime());

        // check column naming, interpret annotations
        Pair<List<TableColumnDesc>, List<StmtClassForgeableFactory>> columnsValidated = validateExpressions(createDesc.getColumns(), services);
        List<TableColumnDesc> columnDescs = columnsValidated.getFirst();
        additionalForgeables.addAll(columnsValidated.getSecond());

        // analyze and plan the state holders
        TableAccessAnalysisResult plan = analyzePlanAggregations(createDesc.getTableName(), columnDescs, base.getStatementRawInfo(), services);
        additionalForgeables.addAll(plan.getAdditionalForgeables());
        NameAccessModifier visibility = plan.getPublicEventType().getMetadata().getAccessModifier();

        // determine context information
        String contextName = base.getStatementRawInfo().getContextName();
        NameAccessModifier contextVisibility = null;
        String contextModuleName = null;
        if (contextName != null) {
            ContextMetaData contextDetail = services.getContextCompileTimeResolver().getContextInfo(contextName);
            if (contextDetail == null) {
                throw new ExprValidationException("Failed to find context '" + contextName + "'");
            }
            contextVisibility = contextDetail.getContextVisibility();
            contextModuleName = contextDetail.getContextModuleName();
        }

        // add table
        TableMetaData tableMetaData = new TableMetaData(tableName, base.getModuleName(), visibility, contextName, contextVisibility, contextModuleName, plan.getInternalEventType(), plan.getPublicEventType(), plan.getPrimaryKeyColumns(), plan.getPrimaryKeyTypes(), plan.getPrimaryKeyColNums(), plan.getTableColumns(), plan.getColsAggMethod().length);
        services.getTableCompileTimeRegistry().newTable(tableMetaData);

        String aiFactoryProviderClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementAIFactoryProvider.class, classPostfix);
        String statementFieldsClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementFields.class, classPostfix);
        String statementProviderClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementProvider.class, classPostfix);

        StatementAgentInstanceFactoryCreateTableForge forge = new StatementAgentInstanceFactoryCreateTableForge(aiFactoryProviderClassName, tableMetaData.getTableName(), plan);

        // build forge list
        CodegenPackageScope packageScope = new CodegenPackageScope(packageName, statementFieldsClassName, services.isInstrumented());
        List<StmtClassForgeable> forgeables = new ArrayList<>(2);
        for (StmtClassForgeableFactory additional : additionalForgeables) {
            forgeables.add(additional.make(packageScope, classPostfix));
        }

        StmtClassForgeableAIFactoryProviderCreateTable aiFactoryForgeable = new StmtClassForgeableAIFactoryProviderCreateTable(aiFactoryProviderClassName, packageScope, forge, tableName);
        forgeables.add(aiFactoryForgeable);

        SelectSubscriberDescriptor selectSubscriberDescriptor = new SelectSubscriberDescriptor();
        StatementInformationalsCompileTime informationals = StatementInformationalsUtil.getInformationals(base, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), true, selectSubscriberDescriptor, packageScope, services);
        informationals.getProperties().put(StatementProperty.CREATEOBJECTNAME, createDesc.getTableName());
        forgeables.add(new StmtClassForgeableStmtProvider(aiFactoryProviderClassName, statementProviderClassName, informationals, packageScope));
        forgeables.add(new StmtClassForgeableStmtFields(statementFieldsClassName, packageScope, 1));

        return new StmtForgeMethodResult(forgeables, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    private void validateKeyTypes(List<CreateTableColumn> columns, ClasspathImportServiceCompileTime classpathImportService)
        throws ExprValidationException {
        for (CreateTableColumn col : columns) {
            if (col.getPrimaryKey() == null || !col.getPrimaryKey()) {
                continue;
            }
            String msg = "Column '" + col.getColumnName() + "' may not be tagged as primary key";
            if (col.getOptExpression() != null) {
                throw new ExprValidationException(msg + ", an expression cannot become a primary key column");
            }
            Object type = EventTypeUtility.buildType(new ColumnDesc(col.getColumnName(), col.getOptType().toEPL()), classpathImportService);
            if (!(type instanceof Class)) {
                throw new ExprValidationException(msg + ", received unexpected event type '" + type + "'");
            }
        }
    }

    private Pair<List<TableColumnDesc>, List<StmtClassForgeableFactory>> validateExpressions(List<CreateTableColumn> columns, StatementCompileTimeServices services)
        throws ExprValidationException {
        Set<String> columnNames = new HashSet<>();
        List<TableColumnDesc> descriptors = new ArrayList<>();

        int positionInDeclaration = 0;
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(2);
        for (CreateTableColumn column : columns) {
            String msgprefix = "For column '" + column.getColumnName() + "'";

            // check duplicate name
            if (columnNames.contains(column.getColumnName())) {
                throw new ExprValidationException("Column '" + column.getColumnName() + "' is listed more than once");
            }
            columnNames.add(column.getColumnName());

            // determine presence of type annotation
            EventType optionalEventType = validateExpressionGetEventType(msgprefix, column.getAnnotations(), services);

            // aggregation node
            TableColumnDesc descriptor;
            if (column.getOptExpression() != null) {
                Pair<ExprAggregateNode, List<StmtClassForgeableFactory>> pair = validateAggregationExpr(column.getOptExpression(), optionalEventType, services);
                descriptor = new TableColumnDescAgg(positionInDeclaration, column.getColumnName(), pair.getFirst(), optionalEventType);
                additionalForgeables.addAll(pair.getSecond());
            } else {
                Object unresolvedType = EventTypeUtility.buildType(new ColumnDesc(column.getColumnName(), column.getOptType().toEPL()),
                    services.getClasspathImportServiceCompileTime());
                descriptor = new TableColumnDescTyped(positionInDeclaration, column.getColumnName(), unresolvedType, column.getPrimaryKey() == null ? false : column.getPrimaryKey());
            }
            descriptors.add(descriptor);
            positionInDeclaration++;
        }

        return new Pair<>(descriptors, additionalForgeables);
    }

    private static EventType validateExpressionGetEventType(String msgprefix, List<AnnotationDesc> annotations, StatementCompileTimeServices services)
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
            optionalType = services.getEventTypeCompileTimeResolver().getTypeByName(typeName);
            if (optionalType == null) {
                throw new ExprValidationException(msgprefix + " failed to find event type '" + typeName + "'");
            }
        }

        return optionalType;
    }

    private Pair<ExprAggregateNode, List<StmtClassForgeableFactory>> validateAggregationExpr(ExprNode columnExpressionType, EventType optionalProvidedType, StatementCompileTimeServices services)
        throws ExprValidationException {
        ClasspathImportServiceCompileTime classpathImportService = services.getClasspathImportServiceCompileTime();

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

        StreamTypeServiceImpl streamTypeService = new StreamTypeServiceImpl(types, streamNames, istreamOnly, false, false);
        ExprValidationContext validationContext = new ExprValidationContextBuilder(streamTypeService, base.getStatementRawInfo(), services).build();

        // substitute parameter nodes
        for (ExprNode childNode : columnExpressionType.getChildNodes()) {
            if (childNode instanceof ExprIdentNode) {
                ExprIdentNode identNode = (ExprIdentNode) childNode;
                String propname = identNode.getFullUnresolvedName().trim();
                Class clazz = JavaClassHelper.getClassForSimpleName(propname, classpathImportService.getClassForNameProvider());
                if (propname.toLowerCase(Locale.ENGLISH).trim().equals("object")) {
                    clazz = Object.class;
                }
                ClasspathImportException ex = null;
                if (clazz == null) {
                    try {
                        clazz = classpathImportService.resolveClass(propname, false);
                    } catch (ClasspathImportException e) {
                        ex = e;
                    }
                }
                if (clazz != null) {
                    ExprTypedNoEvalNode typeNode = new ExprTypedNoEvalNode(propname, clazz);
                    ExprNodeUtilityModify.replaceChildNode(columnExpressionType, identNode, typeNode);
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
        ExprNode validated = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.CREATETABLECOLUMN, columnExpressionType, validationContext);
        if (!(validated instanceof ExprAggregateNode)) {
            throw new ExprValidationException("Expression '" + ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(validated) + "' is not an aggregation");
        }

        ExprAggregateNode aggregateNode = (ExprAggregateNode) validated;
        return new Pair<>(aggregateNode, validationContext.getAdditionalForgeables());
    }

    private TableAccessAnalysisResult analyzePlanAggregations(String tableName, List<TableColumnDesc> columns, StatementRawInfo statementRawInfo, StatementCompileTimeServices services)
        throws ExprValidationException {
        // once upfront: obtains aggregation factories for each aggregation
        // we do this once as a factory may be a heavier object
        Map<TableColumnDesc, AggregationForgeFactory> aggregationFactories = new HashMap<>();
        for (TableColumnDesc column : columns) {
            if (column instanceof TableColumnDescAgg) {
                TableColumnDescAgg agg = (TableColumnDescAgg) column;
                AggregationForgeFactory factory = agg.getAggregation().getFactory();
                aggregationFactories.put(column, factory);
            }
        }

        // sort into these categories:
        // plain / method-agg / access-agg
        // compile all-column public types
        List<TableColumnDescTyped> plainColumns = new ArrayList<>();
        List<TableColumnDescAgg> methodAggColumns = new ArrayList<>();
        List<TableColumnDescAgg> accessAggColumns = new ArrayList<>();
        Map<String, Object> allColumnsPublicTypes = new LinkedHashMap<>();
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
            AggregationForgeFactory aggFactory = aggregationFactories.get(agg);
            if (aggFactory.isAccessAggregation()) {
                accessAggColumns.add(agg);
            } else {
                methodAggColumns.add(agg);
            }
            allColumnsPublicTypes.put(column.getColumnName(), agg.getAggregation().getEvaluationType());
        }

        // determine column metadata
        //
        Map<String, TableMetadataColumn> columnMetadata = new LinkedHashMap<>();

        // handle typed columns
        Map<String, Object> allColumnsInternalTypes = new LinkedHashMap<>();
        allColumnsInternalTypes.put(INTERNAL_RESERVED_PROPERTY, Object.class);
        int indexPlain = 1;
        TableMetadataColumnPairPlainCol[] assignPairsPlain = new TableMetadataColumnPairPlainCol[plainColumns.size()];
        for (TableColumnDescTyped typedColumn : plainColumns) {
            allColumnsInternalTypes.put(typedColumn.getColumnName(), typedColumn.getUnresolvedType());
            columnMetadata.put(typedColumn.getColumnName(), new TableMetadataColumnPlain(typedColumn.getColumnName(), typedColumn.isKey(), indexPlain));
            assignPairsPlain[indexPlain - 1] = new TableMetadataColumnPairPlainCol(typedColumn.getPositionInDeclaration(), indexPlain);
            indexPlain++;
        }

        // determine internally-used event type
        NameAccessModifier visibility = services.getModuleVisibilityRules().getAccessModifierTable(base, tableName);
        String internalName = EventTypeNameUtil.getTableInternalTypeName(tableName);
        EventTypeMetadata internalMetadata = new EventTypeMetadata(internalName, base.getModuleName(), EventTypeTypeClass.TABLE_INTERNAL, EventTypeApplicationType.OBJECTARR, visibility, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        ObjectArrayEventType internalEventType = BaseNestableEventUtil.makeOATypeCompileTime(internalMetadata, allColumnsInternalTypes, null, null, null, null, services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());
        services.getEventTypeCompileTimeRegistry().newType(internalEventType);

        // for use by indexes and lookups
        String publicName = EventTypeNameUtil.getTablePublicTypeName(tableName);
        EventTypeMetadata publicMetadata = new EventTypeMetadata(publicName, base.getModuleName(), EventTypeTypeClass.TABLE_PUBLIC, EventTypeApplicationType.OBJECTARR, visibility, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        ObjectArrayEventType publicEventType = BaseNestableEventUtil.makeOATypeCompileTime(publicMetadata, allColumnsPublicTypes, null, null, null, null, services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());
        services.getEventTypeCompileTimeRegistry().newType(publicEventType);

        // handle aggregation-methods single-func first.
        AggregationForgeFactory[] methodFactories = new AggregationForgeFactory[methodAggColumns.size()];
        int index = 0;
        TableMetadataColumnPairAggMethod[] assignPairsMethod = new TableMetadataColumnPairAggMethod[methodAggColumns.size()];
        for (TableColumnDescAgg column : methodAggColumns) {
            AggregationForgeFactory factory = aggregationFactories.get(column);
            EPType optionalEnumerationType = EPTypeHelper.optionalFromEnumerationExpr(statementRawInfo, services, column.getAggregation());
            methodFactories[index] = factory;
            AggregationPortableValidation bindingInfo = factory.getAggregationPortableValidation();
            String expression = ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(factory.getAggregationExpression());
            columnMetadata.put(column.getColumnName(), new TableMetadataColumnAggregation(column.getColumnName(), false, index, bindingInfo, expression, true, optionalEnumerationType));
            assignPairsMethod[index] = new TableMetadataColumnPairAggMethod(column.getPositionInDeclaration());
            index++;
        }

        // handle access-aggregation (sharable, multi-value) aggregations
        AggregationStateFactoryForge[] stateFactories = new AggregationStateFactoryForge[accessAggColumns.size()];
        AggregationAccessorSlotPairForge[] accessAccessorForges = new AggregationAccessorSlotPairForge[accessAggColumns.size()];
        TableMetadataColumnPairAggAccess[] assignPairsAccess = new TableMetadataColumnPairAggAccess[accessAggColumns.size()];
        int accessNum = 0;
        for (TableColumnDescAgg column : accessAggColumns) {
            AggregationForgeFactory factory = aggregationFactories.get(column);
            AggregationStateFactoryForge forge = factory.getAggregationStateFactory(false);
            stateFactories[accessNum] = forge;
            AggregationAccessorForge accessor = factory.getAccessorForge();
            AggregationPortableValidation bindingInfo = factory.getAggregationPortableValidation();
            accessAccessorForges[accessNum] = new AggregationAccessorSlotPairForge(accessNum, accessor);
            String expression = ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(factory.getAggregationExpression());
            EPType optionalEnumerationType = EPTypeHelper.optionalFromEnumerationExpr(statementRawInfo, services, column.getAggregation());
            columnMetadata.put(column.getColumnName(), new TableMetadataColumnAggregation(column.getColumnName(), false, index, bindingInfo, expression, false, optionalEnumerationType));
            assignPairsAccess[accessNum] = new TableMetadataColumnPairAggAccess(column.getPositionInDeclaration(), accessor);
            index++;
            accessNum++;
        }

        // determine primary key index information
        List<String> primaryKeyColumns = new ArrayList<>();
        List<Class> primaryKeyTypes = new ArrayList<>();
        List<EventPropertyGetterSPI> primaryKeyGetters = new ArrayList<>();
        List<Integer> primaryKeyColNums = new ArrayList<>();
        int colNum = -1;
        for (TableColumnDescTyped typedColumn : plainColumns) {
            colNum++;
            if (typedColumn.isKey()) {
                primaryKeyColumns.add(typedColumn.getColumnName());
                primaryKeyTypes.add(internalEventType.getPropertyType(typedColumn.getColumnName()));
                primaryKeyGetters.add(internalEventType.getGetterSPI(typedColumn.getColumnName()));
                primaryKeyColNums.add(colNum + 1);
            }
        }
        String[] primaryKeyColumnArray = null;
        Class[] primaryKeyTypeArray = null;
        EventPropertyGetterSPI[] primaryKeyGetterArray = null;

        int[] primaryKeyColNumsArray = null;
        if (!primaryKeyColumns.isEmpty()) {
            primaryKeyColumnArray = primaryKeyColumns.toArray(new String[primaryKeyColumns.size()]);
            primaryKeyTypeArray = primaryKeyTypes.toArray(new Class[primaryKeyTypes.size()]);
            primaryKeyGetterArray = primaryKeyGetters.toArray(new EventPropertyGetterSPI[primaryKeyTypes.size()]);
            primaryKeyColNumsArray = IntArrayUtil.toArray(primaryKeyColNums);
        }

        AggregationRowStateForgeDesc forgeDesc = new AggregationRowStateForgeDesc(methodFactories, null, stateFactories, accessAccessorForges, new AggregationUseFlags(false, false, false));

        MultiKeyPlan multiKeyPlan = MultiKeyPlanner.planMultiKey(primaryKeyTypeArray, false, statementRawInfo, services.getSerdeResolver());

        DataInputOutputSerdeForge[] propertyForges = new DataInputOutputSerdeForge[internalEventType.getPropertyNames().length - 1];

        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(multiKeyPlan.getMultiKeyForgeables());
        for (int i = 1; i < internalEventType.getPropertyNames().length; i++) {
            String propertyName = internalEventType.getPropertyNames()[i];
            Object propertyType = internalEventType.getTypes().get(propertyName);
            SerdeEventPropertyDesc desc = SerdeEventPropertyUtility.forgeForEventProperty(publicEventType, propertyName, propertyType, statementRawInfo, services.getSerdeResolver());
            propertyForges[i - 1] = desc.getForge();

            // plan serdes for nested types
            for (EventType eventType : desc.getNestedTypes()) {
                List<StmtClassForgeableFactory> serdeForgeables = SerdeEventTypeUtility.plan(eventType, statementRawInfo, services.getSerdeEventTypeRegistry(), services.getSerdeResolver());
                additionalForgeables.addAll(serdeForgeables);
            }
        }

        return new TableAccessAnalysisResult(columnMetadata, internalEventType, propertyForges, publicEventType, assignPairsPlain, assignPairsMethod, assignPairsAccess, forgeDesc, primaryKeyColumnArray, primaryKeyGetterArray, primaryKeyTypeArray, primaryKeyColNumsArray, multiKeyPlan.getClassRef(), additionalForgeables);
    }
}
