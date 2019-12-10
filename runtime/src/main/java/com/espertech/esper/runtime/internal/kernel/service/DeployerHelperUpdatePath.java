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
package com.espertech.esper.runtime.internal.kernel.service;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.collection.PathException;
import com.espertech.esper.common.internal.collection.PathExceptionAlreadyRegistered;
import com.espertech.esper.common.internal.collection.PathRegistryObjectType;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionDeclItem;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionScriptProvided;
import com.espertech.esper.common.internal.context.compile.ContextMetaData;
import com.espertech.esper.common.internal.context.module.ModuleIndexMeta;
import com.espertech.esper.common.internal.epl.lookupplansubord.EventTableIndexMetadata;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.script.core.NameAndParamNum;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.event.core.EventTypeSPI;
import com.espertech.esper.common.internal.util.CRC32Util;
import com.espertech.esper.runtime.client.EPDeployPreconditionException;

import java.util.*;

public class DeployerHelperUpdatePath {
    public static DeployerModulePaths updatePath(int rolloutItemNumber, DeployerModuleEPLObjects eplObjects, String moduleName, String deploymentId, EPServicesContext services) throws PathException, EPDeployPreconditionException {
        // save path-visibility event types and named windows to the path
        long deploymentIdCrc32 = CRC32Util.computeCRC32(deploymentId);
        Map<Long, EventType> deploymentTypes = Collections.emptyMap();
        List<String> pathEventTypes = new ArrayList<>(eplObjects.getModuleEventTypes().size());
        List<String> pathNamedWindows = new ArrayList<>(eplObjects.getModuleNamedWindows().size());
        List<String> pathTables = new ArrayList<>(eplObjects.getModuleTables().size());
        List<String> pathContexts = new ArrayList<>(eplObjects.getModuleContexts().size());
        List<String> pathVariables = new ArrayList<>(eplObjects.getModuleVariables().size());
        List<String> pathExprDecl = new ArrayList<>(eplObjects.getModuleExpressions().size());
        List<NameAndParamNum> pathScripts = new ArrayList<>(eplObjects.getModuleScripts().size());

        try {
            for (Map.Entry<String, NamedWindowMetaData> entry : eplObjects.getModuleNamedWindows().entrySet()) {
                if (entry.getValue().getEventType().getMetadata().getAccessModifier().isNonPrivateNonTransient()) {
                    try {
                        services.getNamedWindowPathRegistry().add(entry.getKey(), moduleName, entry.getValue(), deploymentId);
                    } catch (PathExceptionAlreadyRegistered ex) {
                        throw new EPDeployPreconditionException(ex.getMessage(), ex, rolloutItemNumber);
                    }
                    pathNamedWindows.add(entry.getKey());
                }
            }
            for (Map.Entry<String, TableMetaData> entry : eplObjects.getModuleTables().entrySet()) {
                if (entry.getValue().getTableVisibility().isNonPrivateNonTransient()) {
                    try {
                        services.getTablePathRegistry().add(entry.getKey(), moduleName, entry.getValue(), deploymentId);
                    } catch (PathExceptionAlreadyRegistered ex) {
                        throw new EPDeployPreconditionException(ex.getMessage(), ex, rolloutItemNumber);
                    }
                    pathTables.add(entry.getKey());
                }
            }
            for (Map.Entry<String, EventType> entry : eplObjects.getModuleEventTypes().entrySet()) {
                EventTypeSPI eventTypeSPI = (EventTypeSPI) entry.getValue();
                long nameTypeId = CRC32Util.computeCRC32(eventTypeSPI.getName());
                EventTypeMetadata eventTypeMetadata = entry.getValue().getMetadata();
                if (eventTypeMetadata.getAccessModifier() == NameAccessModifier.PRECONFIGURED) {
                    // For XML all fragment event types are public
                    if (eventTypeMetadata.getApplicationType() != EventTypeApplicationType.XML) {
                        throw new IllegalStateException("Unrecognized public visibility type in deployment");
                    }
                } else if (eventTypeMetadata.getAccessModifier().isNonPrivateNonTransient()) {
                    if (eventTypeMetadata.getBusModifier() == EventTypeBusModifier.BUS) {
                        eventTypeSPI.setMetadataId(nameTypeId, -1);
                        services.getEventTypeRepositoryBus().addType(eventTypeSPI);
                    } else {
                        eventTypeSPI.setMetadataId(deploymentIdCrc32, nameTypeId);
                    }
                    try {
                        services.getEventTypePathRegistry().add(entry.getKey(), moduleName, entry.getValue(), deploymentId);
                    } catch (PathExceptionAlreadyRegistered ex) {
                        throw new EPDeployPreconditionException(ex.getMessage(), ex, rolloutItemNumber);
                    }
                } else {
                    eventTypeSPI.setMetadataId(deploymentIdCrc32, nameTypeId);
                }
                if (eventTypeMetadata.getAccessModifier().isNonPrivateNonTransient()) {
                    pathEventTypes.add(entry.getKey());
                }

                // we retain all types to enable variant-streams
                if (deploymentTypes.isEmpty()) {
                    deploymentTypes = new HashMap<>(4);
                }
                deploymentTypes.put(nameTypeId, eventTypeSPI);
            }

            // add serde information to event types
            services.getEventTypeSerdeRepository().addSerdes(deploymentId, eplObjects.getEventTypeSerdes(), eplObjects.getModuleEventTypes(), eplObjects.getBeanEventTypeFactory());

            for (Map.Entry<String, ContextMetaData> entry : eplObjects.getModuleContexts().entrySet()) {
                if (entry.getValue().getContextVisibility().isNonPrivateNonTransient()) {
                    try {
                        services.getContextPathRegistry().add(entry.getKey(), moduleName, entry.getValue(), deploymentId);
                    } catch (PathExceptionAlreadyRegistered ex) {
                        throw new EPDeployPreconditionException(ex.getMessage(), ex, rolloutItemNumber);
                    }
                    pathContexts.add(entry.getKey());
                }
            }
            for (Map.Entry<String, VariableMetaData> entry : eplObjects.getModuleVariables().entrySet()) {
                if (entry.getValue().getVariableVisibility().isNonPrivateNonTransient()) {
                    try {
                        services.getVariablePathRegistry().add(entry.getKey(), moduleName, entry.getValue(), deploymentId);
                    } catch (PathExceptionAlreadyRegistered ex) {
                        throw new EPDeployPreconditionException(ex.getMessage(), ex, rolloutItemNumber);
                    }
                    pathVariables.add(entry.getKey());
                }
            }
            for (Map.Entry<String, ExpressionDeclItem> entry : eplObjects.getModuleExpressions().entrySet()) {
                if (entry.getValue().getVisibility().isNonPrivateNonTransient()) {
                    try {
                        services.getExprDeclaredPathRegistry().add(entry.getKey(), moduleName, entry.getValue(), deploymentId);
                    } catch (PathExceptionAlreadyRegistered ex) {
                        throw new EPDeployPreconditionException(ex.getMessage(), ex, rolloutItemNumber);
                    }
                    pathExprDecl.add(entry.getKey());
                }
            }
            for (Map.Entry<NameAndParamNum, ExpressionScriptProvided> entry : eplObjects.getModuleScripts().entrySet()) {
                if (entry.getValue().getVisibility().isNonPrivateNonTransient()) {
                    try {
                        services.getScriptPathRegistry().add(entry.getKey(), moduleName, entry.getValue(), deploymentId);
                    } catch (PathExceptionAlreadyRegistered ex) {
                        throw new EPDeployPreconditionException(ex.getMessage(), ex, rolloutItemNumber);
                    }
                    pathScripts.add(entry.getKey());
                }
            }
            for (ModuleIndexMeta index : eplObjects.getModuleIndexes()) {
                if (index.isNamedWindow()) {
                    NamedWindowMetaData namedWindow = services.getNamedWindowPathRegistry().getWithModule(index.getInfraName(), index.getInfraModuleName());
                    if (namedWindow == null) {
                        throw new IllegalStateException("Failed to find named window '" + index.getInfraName() + "'");
                    }
                    validateIndexPrecondition(rolloutItemNumber, namedWindow.getIndexMetadata(), index);
                } else {
                    TableMetaData table = services.getTablePathRegistry().getWithModule(index.getInfraName(), index.getInfraModuleName());
                    if (table == null) {
                        throw new IllegalStateException("Failed to find table '" + index.getInfraName() + "'");
                    }
                    validateIndexPrecondition(rolloutItemNumber, table.getIndexMetadata(), index);
                }
            }
        } catch (Throwable t) {
            Undeployer.deleteFromEventTypeBus(services, deploymentTypes);
            Undeployer.deleteFromPathRegistries(services, deploymentId);
            throw t;
        }

        return new DeployerModulePaths(deploymentTypes, pathEventTypes, pathNamedWindows, pathTables, pathContexts,
            pathVariables, pathExprDecl, pathScripts);
    }

    private static void validateIndexPrecondition(int rolloutItemNumber, EventTableIndexMetadata indexMetadata, ModuleIndexMeta index) throws EPDeployPreconditionException {
        if (indexMetadata.getIndexByName(index.getIndexName()) != null) {
            PathExceptionAlreadyRegistered ex = new PathExceptionAlreadyRegistered(index.getIndexName(), PathRegistryObjectType.INDEX, index.getIndexModuleName());
            throw new EPDeployPreconditionException(ex.getMessage(), ex, rolloutItemNumber);
        }
    }
}
