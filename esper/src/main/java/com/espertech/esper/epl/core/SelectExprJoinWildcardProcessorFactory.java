/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.core;

import com.espertech.esper.client.ConfigurationInformation;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.CreateSchemaDesc;
import com.espertech.esper.epl.spec.InsertIntoDesc;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.table.mgmt.TableServiceUtil;
import com.espertech.esper.event.EventAdapterException;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.util.EventRepresentationUtil;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class SelectExprJoinWildcardProcessorFactory
{
    /**
     * Ctor.
     * @param streamNames - name of each stream
     * @param streamTypes - type of each stream
     * @param eventAdapterService - service for generating events and handling event types
     * @param insertIntoDesc - describes the insert-into clause
     * @param selectExprEventTypeRegistry - registry for event type to statements
     * @throws com.espertech.esper.epl.expression.core.ExprValidationException if the expression validation failed
     */
    public static SelectExprProcessor create(Collection<Integer> assignedTypeNumberStack,
                                                  int statementId,
                                                  String[] streamNames,
                                                  EventType[] streamTypes,
                                                  EventAdapterService eventAdapterService,
                                                  InsertIntoDesc insertIntoDesc,
                                                  SelectExprEventTypeRegistry selectExprEventTypeRegistry,
                                                  EngineImportService engineImportService,
                                                  Annotation[] annotations,
                                                  ConfigurationInformation configuration,
                                                  TableService tableService) throws ExprValidationException
    {
        if ((streamNames.length < 2) || (streamTypes.length < 2) || (streamNames.length != streamTypes.length))
        {
            throw new IllegalArgumentException("Stream names and types parameter length is invalid, expected use of this class is for join statements");
        }

        // Create EventType of result join events
        Map<String, Object> eventTypeMap = new LinkedHashMap<String, Object>();
        EventType[] streamTypesWTables = new EventType[streamTypes.length];
        boolean hasTables = false;
        for (int i = 0; i < streamTypes.length; i++)
        {
            streamTypesWTables[i] = streamTypes[i];
            String tableName = TableServiceUtil.getTableNameFromEventType(streamTypesWTables[i]);
            if (tableName != null) {
                hasTables = true;
                streamTypesWTables[i] = tableService.getTableMetadata(tableName).getPublicEventType();
            }
            eventTypeMap.put(streamNames[i], streamTypesWTables[i]);
        }

        // If we have a name for this type, add it
        boolean useMap = EventRepresentationUtil.isMap(annotations, configuration, CreateSchemaDesc.AssignedType.NONE);
        EventType resultEventType;

        SelectExprProcessor processor = null;
        if (insertIntoDesc != null) {
            EventType existingType = eventAdapterService.getExistsTypeByName(insertIntoDesc.getEventTypeName());
            if (existingType != null) {
                processor = SelectExprInsertEventBeanFactory.getInsertUnderlyingJoinWildcard(eventAdapterService, existingType, streamNames, streamTypesWTables, engineImportService);
            }
        }

        if (processor == null) {
            if (insertIntoDesc != null) {
                try {
                    if (useMap) {
                        resultEventType = eventAdapterService.addNestableMapType(insertIntoDesc.getEventTypeName(), eventTypeMap, null, false, false, false, false, true);
                    }
                    else {
                        resultEventType = eventAdapterService.addNestableObjectArrayType(insertIntoDesc.getEventTypeName(), eventTypeMap, null, false, false, false, false, true, false, null);
                    }
                    selectExprEventTypeRegistry.add(resultEventType);
                }
                catch (EventAdapterException ex) {
                    throw new ExprValidationException(ex.getMessage());
                }
            }
            else {
                if (useMap) {
                    resultEventType = eventAdapterService.createAnonymousMapType(statementId + "_join_" + CollectionUtil.toString(assignedTypeNumberStack, "_"), eventTypeMap, true);
                }
                else {
                    resultEventType = eventAdapterService.createAnonymousObjectArrayType(statementId + "_join_" + CollectionUtil.toString(assignedTypeNumberStack, "_"), eventTypeMap);
                }
            }
            if (resultEventType instanceof ObjectArrayEventType) {
                processor = new SelectExprJoinWildcardProcessorObjectArray(streamNames, resultEventType, eventAdapterService);
            }
            else {
                processor = new SelectExprJoinWildcardProcessorMap(streamNames, resultEventType, eventAdapterService);
            }
        }

        if (!hasTables) {
            return processor;
        }
        return new SelectExprJoinWildcardProcessorTableRows(streamTypes, processor, tableService);
    }
}
