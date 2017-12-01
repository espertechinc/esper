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
package com.espertech.esper.epl.core.select;

import com.espertech.esper.client.ConfigurationInformation;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.util.EventUnderlyingType;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.CreateSchemaDesc;
import com.espertech.esper.epl.spec.InsertIntoDesc;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.table.mgmt.TableServiceUtil;
import com.espertech.esper.epl.util.EventRepresentationUtil;
import com.espertech.esper.event.EventAdapterException;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import com.espertech.esper.event.avro.AvroSchemaEventType;
import com.espertech.esper.event.map.MapEventType;
import com.espertech.esper.util.CollectionUtil;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class SelectExprJoinWildcardProcessorFactory {
    public static SelectExprProcessorForge create(Collection<Integer> assignedTypeNumberStack,
                                                  int statementId,
                                                  String statementName,
                                                  String[] streamNames,
                                                  EventType[] streamTypes,
                                                  EventAdapterService eventAdapterService,
                                                  InsertIntoDesc insertIntoDesc,
                                                  SelectExprEventTypeRegistry selectExprEventTypeRegistry,
                                                  EngineImportService engineImportService,
                                                  Annotation[] annotations,
                                                  ConfigurationInformation configuration,
                                                  TableService tableService,
                                                  String engineURI,
                                                  boolean isFireAndForget) throws ExprValidationException {
        if ((streamNames.length < 2) || (streamTypes.length < 2) || (streamNames.length != streamTypes.length)) {
            throw new IllegalArgumentException("Stream names and types parameter length is invalid, expected use of this class is for join statements");
        }

        // Create EventType of result join events
        Map<String, Object> selectProperties = new LinkedHashMap<String, Object>();
        EventType[] streamTypesWTables = new EventType[streamTypes.length];
        boolean hasTables = false;
        for (int i = 0; i < streamTypes.length; i++) {
            streamTypesWTables[i] = streamTypes[i];
            String tableName = TableServiceUtil.getTableNameFromEventType(streamTypesWTables[i]);
            if (tableName != null) {
                hasTables = true;
                streamTypesWTables[i] = tableService.getTableMetadata(tableName).getPublicEventType();
            }
            selectProperties.put(streamNames[i], streamTypesWTables[i]);
        }

        // If we have a name for this type, add it
        EventUnderlyingType representation = EventRepresentationUtil.getRepresentation(annotations, configuration, CreateSchemaDesc.AssignedType.NONE);
        EventType resultEventType;

        SelectExprProcessorForge processor = null;
        if (insertIntoDesc != null) {
            EventType existingType = eventAdapterService.getExistsTypeByName(insertIntoDesc.getEventTypeName());
            if (existingType != null) {
                processor = SelectExprInsertEventBeanFactory.getInsertUnderlyingJoinWildcard(eventAdapterService, existingType, streamNames, streamTypesWTables, engineImportService, statementName, engineURI, isFireAndForget);
            }
        }

        if (processor == null) {
            if (insertIntoDesc != null) {
                try {
                    if (representation == EventUnderlyingType.MAP) {
                        resultEventType = eventAdapterService.addNestableMapType(insertIntoDesc.getEventTypeName(), selectProperties, null, false, false, false, false, true);
                    } else if (representation == EventUnderlyingType.OBJECTARRAY) {
                        resultEventType = eventAdapterService.addNestableObjectArrayType(insertIntoDesc.getEventTypeName(), selectProperties, null, false, false, false, false, true, false, null);
                    } else if (representation == EventUnderlyingType.AVRO) {
                        resultEventType = eventAdapterService.addAvroType(insertIntoDesc.getEventTypeName(), selectProperties, false, false, false, false, true, annotations, null, statementName, engineURI);
                    } else {
                        throw new IllegalStateException("Unrecognized code " + representation);
                    }
                    selectExprEventTypeRegistry.add(resultEventType);
                } catch (EventAdapterException ex) {
                    throw new ExprValidationException(ex.getMessage(), ex);
                }
            } else {
                if (representation == EventUnderlyingType.MAP) {
                    resultEventType = eventAdapterService.createAnonymousMapType(statementId + "_join_" + CollectionUtil.toString(assignedTypeNumberStack, "_"), selectProperties, true);
                } else if (representation == EventUnderlyingType.OBJECTARRAY) {
                    resultEventType = eventAdapterService.createAnonymousObjectArrayType(statementId + "_join_" + CollectionUtil.toString(assignedTypeNumberStack, "_"), selectProperties);
                } else if (representation == EventUnderlyingType.AVRO) {
                    resultEventType = eventAdapterService.createAnonymousAvroType(statementId + "_join_" + CollectionUtil.toString(assignedTypeNumberStack, "_"), selectProperties, annotations, statementName, engineURI);
                } else {
                    throw new IllegalStateException("Unrecognized enum " + representation);
                }
            }
            if (resultEventType instanceof ObjectArrayEventType) {
                processor = new SelectExprJoinWildcardProcessorObjectArray(streamNames, resultEventType, eventAdapterService);
            } else if (resultEventType instanceof MapEventType) {
                processor = new SelectExprJoinWildcardProcessorMap(streamNames, resultEventType, eventAdapterService);
            } else if (resultEventType instanceof AvroSchemaEventType) {
                processor = eventAdapterService.getEventAdapterAvroHandler().getOutputFactory().makeJoinWildcard(streamNames, resultEventType, eventAdapterService);
            }
        }

        if (!hasTables) {
            return processor;
        }
        return new SelectExprJoinWildcardProcessorTableRows(streamTypes, processor, tableService);
    }
}
