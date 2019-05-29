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
package com.espertech.esper.common.internal.epl.resultset.select.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.EventUnderlyingType;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.compile.stage1.spec.CreateSchemaDesc;
import com.espertech.esper.common.internal.compile.stage1.spec.InsertIntoDesc;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.resultset.select.eval.SelectEvalJoinWildcardProcessorJson;
import com.espertech.esper.common.internal.epl.resultset.select.eval.SelectEvalJoinWildcardProcessorMap;
import com.espertech.esper.common.internal.epl.resultset.select.eval.SelectEvalJoinWildcardProcessorObjectArray;
import com.espertech.esper.common.internal.epl.resultset.select.eval.SelectEvalJoinWildcardProcessorTableRows;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.event.avro.AvroSchemaEventType;
import com.espertech.esper.common.internal.event.core.BaseNestableEventUtil;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryCompileTime;
import com.espertech.esper.common.internal.event.core.EventTypeForgablesPair;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.event.json.compiletime.JsonEventTypeUtility;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.util.EventRepresentationUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.espertech.esper.common.client.meta.EventTypeApplicationType.*;

public class SelectExprJoinWildcardProcessorFactory {
    public static SelectExprProcessorForgeWForgables create(SelectProcessorArgs args, InsertIntoDesc insertIntoDesc, Function<String, String> eventTypeNamePostfix) throws ExprValidationException {
        String[] streamNames = args.getTypeService().getStreamNames();
        EventType[] streamTypes = args.getTypeService().getEventTypes();
        String moduleName = args.getModuleName();
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(2);
        if ((streamNames.length < 2) || (streamTypes.length < 2) || (streamNames.length != streamTypes.length)) {
            throw new IllegalArgumentException("Stream names and types parameter length is invalid, expected use of this class is for join statements");
        }

        // Create EventType of result join events
        LinkedHashMap<String, Object> selectProperties = new LinkedHashMap<>();
        EventType[] streamTypesWTables = new EventType[streamTypes.length];
        boolean hasTables = false;
        for (int i = 0; i < streamTypes.length; i++) {
            streamTypesWTables[i] = streamTypes[i];
            TableMetaData table = args.getTableCompileTimeResolver().resolveTableFromEventType(streamTypesWTables[i]);
            if (table != null) {
                hasTables = true;
                streamTypesWTables[i] = table.getPublicEventType();
            }
            selectProperties.put(streamNames[i], streamTypesWTables[i]);
        }

        // If we have a name for this type, add it
        EventUnderlyingType representation = EventRepresentationUtil.getRepresentation(args.getAnnotations(), args.getConfiguration(), CreateSchemaDesc.AssignedType.NONE);
        EventType resultEventType;

        SelectExprProcessorForge processor = null;
        if (insertIntoDesc != null) {
            EventType existingType = args.getEventTypeCompileTimeResolver().getTypeByName(insertIntoDesc.getEventTypeName());
            if (existingType != null) {
                processor = SelectExprInsertEventBeanFactory.getInsertUnderlyingJoinWildcard(existingType, streamNames, streamTypesWTables, args.getClasspathImportService(), args.getStatementName(), args.getEventTypeAvroHandler());
            }
        }

        if (processor == null) {
            if (insertIntoDesc != null) {
                String eventTypeName = eventTypeNamePostfix.apply(insertIntoDesc.getEventTypeName());
                NameAccessModifier visibility = args.getCompileTimeServices().getModuleVisibilityRules().getAccessModifierEventType(args.getStatementRawInfo(), eventTypeName);
                Function<EventTypeApplicationType, EventTypeMetadata> metadata = apptype -> new EventTypeMetadata(eventTypeName, moduleName, EventTypeTypeClass.STREAM, apptype, visibility, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
                if (representation == EventUnderlyingType.MAP) {
                    Map<String, Object> propertyTypes = EventTypeUtility.getPropertyTypesNonPrimitive(selectProperties);
                    resultEventType = BaseNestableEventUtil.makeMapTypeCompileTime(metadata.apply(MAP), propertyTypes, null, null, null, null, args.getBeanEventTypeFactoryPrivate(), args.getEventTypeCompileTimeResolver());
                } else if (representation == EventUnderlyingType.OBJECTARRAY) {
                    Map<String, Object> propertyTypes = EventTypeUtility.getPropertyTypesNonPrimitive(selectProperties);
                    resultEventType = BaseNestableEventUtil.makeOATypeCompileTime(metadata.apply(OBJECTARR), propertyTypes, null, null, null, null, args.getBeanEventTypeFactoryPrivate(), args.getEventTypeCompileTimeResolver());
                } else if (representation == EventUnderlyingType.AVRO) {
                    resultEventType = args.getEventTypeAvroHandler().newEventTypeFromNormalized(metadata.apply(AVRO), args.getEventTypeCompileTimeResolver(), EventBeanTypedEventFactoryCompileTime.INSTANCE, selectProperties, args.getAnnotations(), null, null, null, args.getStatementName());
                } else if (representation == EventUnderlyingType.JSON) {
                    EventTypeForgablesPair pair = JsonEventTypeUtility.makeJsonTypeCompileTimeNewType(metadata.apply(JSON), selectProperties, null, null, args.getStatementRawInfo(), args.getCompileTimeServices());
                    resultEventType = pair.getEventType();
                    additionalForgeables.addAll(pair.getAdditionalForgeables());
                } else {
                    throw new IllegalStateException("Unrecognized code " + representation);
                }
                args.getEventTypeCompileTimeRegistry().newType(resultEventType);
            } else {
                String eventTypeName = eventTypeNamePostfix.apply(args.getCompileTimeServices().getEventTypeNameGeneratorStatement().getAnonymousTypeName());
                Map<String, Object> propertyTypes = EventTypeUtility.getPropertyTypesNonPrimitive(selectProperties);
                Function<EventTypeApplicationType, EventTypeMetadata> metadata = type -> new EventTypeMetadata(eventTypeName, moduleName, EventTypeTypeClass.STATEMENTOUT, type, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
                if (representation == EventUnderlyingType.MAP) {
                    resultEventType = BaseNestableEventUtil.makeMapTypeCompileTime(metadata.apply(MAP), propertyTypes, null, null, null, null, args.getBeanEventTypeFactoryPrivate(), args.getEventTypeCompileTimeResolver());
                } else if (representation == EventUnderlyingType.OBJECTARRAY) {
                    resultEventType = BaseNestableEventUtil.makeOATypeCompileTime(metadata.apply(OBJECTARR), propertyTypes, null, null, null, null, args.getBeanEventTypeFactoryPrivate(), args.getEventTypeCompileTimeResolver());
                } else if (representation == EventUnderlyingType.AVRO) {
                    resultEventType = args.getEventTypeAvroHandler().newEventTypeFromNormalized(metadata.apply(AVRO), args.getEventTypeCompileTimeResolver(), args.getBeanEventTypeFactoryPrivate().getEventBeanTypedEventFactory(), selectProperties, args.getAnnotations(), null, null, null, args.getStatementName());
                } else if (representation == EventUnderlyingType.JSON) {
                    EventTypeForgablesPair pair = JsonEventTypeUtility.makeJsonTypeCompileTimeNewType(metadata.apply(JSON), propertyTypes, null, null, args.getStatementRawInfo(), args.getCompileTimeServices());
                    resultEventType = pair.getEventType();
                    additionalForgeables.addAll(pair.getAdditionalForgeables());
                } else {
                    throw new IllegalStateException("Unrecognized enum " + representation);
                }
                args.getEventTypeCompileTimeRegistry().newType(resultEventType);
            }

            /**
             * NOTE: Processors herein maintain their own result-event-type as they become inner types, for example "insert into VariantStream select * from A, B"
             */
            if (resultEventType instanceof ObjectArrayEventType) {
                processor = new SelectEvalJoinWildcardProcessorObjectArray(streamNames, resultEventType);
            } else if (resultEventType instanceof MapEventType) {
                processor = new SelectEvalJoinWildcardProcessorMap(streamNames, resultEventType);
            } else if (resultEventType instanceof AvroSchemaEventType) {
                processor = args.getEventTypeAvroHandler().getOutputFactory().makeJoinWildcard(streamNames, resultEventType);
            } else if (resultEventType instanceof JsonEventType) {
                processor = new SelectEvalJoinWildcardProcessorJson(streamNames, (JsonEventType) resultEventType);
            }
        }

        if (!hasTables) {
            return new SelectExprProcessorForgeWForgables(processor, additionalForgeables);
        }
        processor = new SelectEvalJoinWildcardProcessorTableRows(streamTypes, processor, args.getTableCompileTimeResolver());
        return new SelectExprProcessorForgeWForgables(processor, additionalForgeables);
    }
}
