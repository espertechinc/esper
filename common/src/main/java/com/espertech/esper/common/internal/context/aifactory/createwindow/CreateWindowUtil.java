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
package com.espertech.esper.common.internal.context.aifactory.createwindow;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.FragmentEventType;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeAvro;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.EventUnderlyingType;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.compile.stage1.spec.*;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecCompiled;
import com.espertech.esper.common.internal.compile.stage2.SelectClauseElementCompiled;
import com.espertech.esper.common.internal.compile.stage2.SelectClauseExprCompiledSpec;
import com.espertech.esper.common.internal.compile.stage2.StreamSpecCompiler;
import com.espertech.esper.common.internal.compile.stage3.StatementBaseInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.event.avro.AvroSchemaEventType;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.core.*;
import com.espertech.esper.common.internal.event.json.compiletime.JsonEventTypeUtility;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.util.EventRepresentationUtil;

import java.util.*;
import java.util.function.Function;

public class CreateWindowUtil {
    // The create window command:
    //      create window windowName[.window_view_list] as [select properties from] type
    //
    // This section expected s single FilterStreamSpecCompiled representing the selected type.
    // It creates a new event type representing the window type and a sets the type selected on the filter stream spec.
    protected static CreateWindowCompileResult handleCreateWindow(StatementBaseInfo base,
                                                                  StatementCompileTimeServices services)
        throws ExprValidationException {

        CreateWindowDesc createWindowDesc = base.getStatementSpec().getRaw().getCreateWindowDesc();
        List<ColumnDesc> columns = createWindowDesc.getColumns();
        String typeName = createWindowDesc.getWindowName();
        EventType targetType;

        // determine that the window name is not already in use as an event type name
        EventType existingType = services.getEventTypeCompileTimeResolver().getTypeByName(typeName);
        if (existingType != null && existingType.getMetadata().getTypeClass() != EventTypeTypeClass.NAMED_WINDOW) {
            throw new ExprValidationException("Error starting statement: An event type or schema by name '" + typeName + "' already exists");
        }

        // Determine select-from
        SelectFromInfo optionalSelectFrom = CreateWindowUtil.getOptionalSelectFrom(createWindowDesc, services);

        // Create Map or Wrapper event type from the select clause of the window.
        // If no columns selected, simply create a wrapper type
        // Build a list of properties
        SelectClauseSpecRaw newSelectClauseSpecRaw = new SelectClauseSpecRaw();
        LinkedHashMap<String, Object> properties;
        boolean hasProperties = false;
        if ((columns != null) && (!columns.isEmpty())) {
            properties = EventTypeUtility.buildType(columns, null, services.getClasspathImportServiceCompileTime(), services.getEventTypeCompileTimeResolver());
            hasProperties = true;
        } else {
            if (optionalSelectFrom == null) {
                throw new IllegalStateException("Missing from-type information for create-window");
            }
            // Validate the select expressions which consists of properties only
            List<NamedWindowSelectedProps> select = compileLimitedSelect(optionalSelectFrom, base, services);

            properties = new LinkedHashMap<>();
            for (NamedWindowSelectedProps selectElement : select) {
                if (selectElement.getFragmentType() != null) {
                    properties.put(selectElement.getAssignedName(), selectElement.getFragmentType());
                } else {
                    properties.put(selectElement.getAssignedName(), selectElement.getSelectExpressionType());
                }

                // Add any properties to the new select clause for use by consumers to the statement itself
                newSelectClauseSpecRaw.add(new SelectClauseExprRawSpec(new ExprIdentNodeImpl(selectElement.getAssignedName()), null, false));
                hasProperties = true;
            }
        }

        // Create Map or Wrapper event type from the select clause of the window.
        // If no columns selected, simply create a wrapper type
        boolean isOnlyWildcard = base.getStatementSpec().getRaw().getSelectClauseSpec().isOnlyWildcard();
        boolean isWildcard = base.getStatementSpec().getRaw().getSelectClauseSpec().isUsingWildcard();
        NameAccessModifier namedWindowVisibility = services.getModuleVisibilityRules().getAccessModifierNamedWindow(base, typeName);
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(2);
        try {
            if (isWildcard && !isOnlyWildcard) {
                EventTypeMetadata metadata = new EventTypeMetadata(typeName, base.getModuleName(), EventTypeTypeClass.NAMED_WINDOW, EventTypeApplicationType.WRAPPER, namedWindowVisibility, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
                targetType = WrapperEventTypeUtil.makeWrapper(metadata, optionalSelectFrom.getEventType(), properties, null, services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());
            } else {
                // Some columns selected, use the types of the columns
                Function<EventTypeApplicationType, EventTypeMetadata> metadata = type -> new EventTypeMetadata(typeName, base.getModuleName(), EventTypeTypeClass.NAMED_WINDOW, type, namedWindowVisibility, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());

                if (hasProperties && !isOnlyWildcard) {
                    Map<String, Object> compiledProperties = EventTypeUtility.compileMapTypeProperties(properties, services.getEventTypeCompileTimeResolver());
                    EventUnderlyingType representation = EventRepresentationUtil.getRepresentation(base.getStatementSpec().getAnnotations(), services.getConfiguration(), CreateSchemaDesc.AssignedType.NONE);

                    if (representation == EventUnderlyingType.MAP) {
                        targetType = BaseNestableEventUtil.makeMapTypeCompileTime(metadata.apply(EventTypeApplicationType.MAP), compiledProperties, null, null, null, null, services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());
                    } else if (representation == EventUnderlyingType.OBJECTARRAY) {
                        targetType = BaseNestableEventUtil.makeOATypeCompileTime(metadata.apply(EventTypeApplicationType.OBJECTARR), compiledProperties, null, null, null, null, services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());
                    } else if (representation == EventUnderlyingType.AVRO) {
                        targetType = services.getEventTypeAvroHandler().newEventTypeFromNormalized(metadata.apply(EventTypeApplicationType.AVRO), services.getEventTypeCompileTimeResolver(), services.getBeanEventTypeFactoryPrivate().getEventBeanTypedEventFactory(), compiledProperties, base.getStatementRawInfo().getAnnotations(), null, null, null, base.getStatementName());
                    } else if (representation == EventUnderlyingType.JSON) {
                        EventTypeForgablesPair pair = JsonEventTypeUtility.makeJsonTypeCompileTimeNewType(metadata.apply(EventTypeApplicationType.JSON), compiledProperties, null, null, base.getStatementRawInfo(), services);
                        targetType = pair.getEventType();
                        additionalForgeables.addAll(pair.getAdditionalForgeables());
                    } else {
                        throw new IllegalStateException("Unrecognized representation " + representation);
                    }
                } else {
                    if (optionalSelectFrom == null) {
                        throw new IllegalStateException("Missing from-type information for create-window");
                    }
                    EventType selectFromType = optionalSelectFrom.getEventType();

                    // No columns selected, no wildcard, use the type as is or as a wrapped type
                    if (selectFromType instanceof ObjectArrayEventType) {
                        ObjectArrayEventType oaType = (ObjectArrayEventType) selectFromType;
                        targetType = BaseNestableEventUtil.makeOATypeCompileTime(metadata.apply(EventTypeApplicationType.OBJECTARR), oaType.getTypes(), null, null, oaType.getStartTimestampPropertyName(), oaType.getEndTimestampPropertyName(), services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());
                    } else if (selectFromType instanceof AvroSchemaEventType) {
                        AvroSchemaEventType avroSchemaEventType = (AvroSchemaEventType) selectFromType;
                        ConfigurationCommonEventTypeAvro avro = new ConfigurationCommonEventTypeAvro();
                        avro.setAvroSchema(avroSchemaEventType.getSchema());
                        targetType = services.getEventTypeAvroHandler().newEventTypeFromSchema(metadata.apply(EventTypeApplicationType.AVRO), services.getBeanEventTypeFactoryPrivate().getEventBeanTypedEventFactory(), avro, null, null);
                    } else if (selectFromType instanceof JsonEventType) {
                        JsonEventType jsonType = (JsonEventType) selectFromType;
                        targetType = JsonEventTypeUtility.makeJsonTypeCompileTimeExistingType(metadata.apply(EventTypeApplicationType.JSON), jsonType, services);
                    } else if (selectFromType instanceof MapEventType) {
                        MapEventType mapType = (MapEventType) selectFromType;
                        targetType = BaseNestableEventUtil.makeMapTypeCompileTime(metadata.apply(EventTypeApplicationType.MAP), mapType.getTypes(), null, null, mapType.getStartTimestampPropertyName(), mapType.getEndTimestampPropertyName(), services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());
                    } else if (selectFromType instanceof BeanEventType) {
                        BeanEventType beanType = (BeanEventType) selectFromType;
                        targetType = new BeanEventType(beanType.getStem(), metadata.apply(EventTypeApplicationType.CLASS), services.getBeanEventTypeFactoryPrivate(), null, null, beanType.getStartTimestampPropertyName(), beanType.getEndTimestampPropertyName());
                    } else {
                        targetType = WrapperEventTypeUtil.makeWrapper(metadata.apply(EventTypeApplicationType.WRAPPER), selectFromType, new HashMap<>(), null, services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());
                    }
                }
            }
            services.getEventTypeCompileTimeRegistry().newType(targetType);
        } catch (EPException ex) {
            throw new ExprValidationException(ex.getMessage(), ex);
        }

        FilterSpecCompiled filter = new FilterSpecCompiled(targetType, typeName, new List[0], null);
        return new CreateWindowCompileResult(filter, newSelectClauseSpecRaw, optionalSelectFrom == null ? null : optionalSelectFrom.getEventType(), additionalForgeables);
    }

    private static List<NamedWindowSelectedProps> compileLimitedSelect(SelectFromInfo selectFromInfo, StatementBaseInfo base, StatementCompileTimeServices compileTimeServices)
        throws ExprValidationException {
        List<NamedWindowSelectedProps> selectProps = new LinkedList<NamedWindowSelectedProps>();
        StreamTypeService streams = new StreamTypeServiceImpl(new EventType[]{selectFromInfo.getEventType()}, new String[]{"stream_0"}, new boolean[]{false}, false, false);

        ExprValidationContext validationContext = new ExprValidationContextBuilder(streams, base.getStatementRawInfo(), compileTimeServices).build();
        for (SelectClauseElementCompiled item : base.getStatementSpec().getSelectClauseCompiled().getSelectExprList()) {
            if (!(item instanceof SelectClauseExprCompiledSpec)) {
                continue;
            }
            SelectClauseExprCompiledSpec exprSpec = (SelectClauseExprCompiledSpec) item;
            ExprNode validatedExpression = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.SELECT, exprSpec.getSelectExpression(), validationContext);

            // determine an element name if none assigned
            String asName = exprSpec.getProvidedName();
            if (asName == null) {
                asName = ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(validatedExpression);
            }

            // check for fragments
            EventType fragmentType = null;
            if ((validatedExpression instanceof ExprIdentNode) && (!(selectFromInfo.getEventType() instanceof NativeEventType))) {
                ExprIdentNode identNode = (ExprIdentNode) validatedExpression;
                FragmentEventType fragmentEventType = selectFromInfo.getEventType().getFragmentType(identNode.getFullUnresolvedName());
                if ((fragmentEventType != null) && (!fragmentEventType.isNative())) {
                    fragmentType = fragmentEventType.getFragmentType();
                }
            }

            NamedWindowSelectedProps validatedElement = new NamedWindowSelectedProps(validatedExpression.getForge().getEvaluationType(), asName, fragmentType);
            selectProps.add(validatedElement);
        }

        return selectProps;
    }

    private static SelectFromInfo getOptionalSelectFrom(CreateWindowDesc createWindowDesc, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        if (createWindowDesc.getAsEventTypeName() == null) {
            return null;
        }
        EventType eventType = StreamSpecCompiler.resolveTypeName(createWindowDesc.getAsEventTypeName(), compileTimeServices.getEventTypeCompileTimeResolver());
        return new SelectFromInfo(eventType, createWindowDesc.getAsEventTypeName());
    }
}
