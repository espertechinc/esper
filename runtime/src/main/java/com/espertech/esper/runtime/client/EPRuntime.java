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
package com.espertech.esper.runtime.client;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EPCompilerPathable;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.context.EPContextPartitionService;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowService;
import com.espertech.esper.common.client.metric.EPMetricsService;
import com.espertech.esper.common.client.render.EPRenderEventService;
import com.espertech.esper.common.client.variable.EPVariableService;

import javax.naming.Context;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * The runtime for deploying and executing EPL.
 */
public interface EPRuntime {
    /**
     * Returns the event service, for sending events to the runtime and for controlling time
     *
     * @return event service
     * @throws EPRuntimeDestroyedException thrown when the runtime has been destroyed
     */
    EPEventService getEventService() throws EPRuntimeDestroyedException;

    /**
     * Returns the data flow service, for managing dataflows
     *
     * @return data flow service
     * @throws EPRuntimeDestroyedException thrown when the runtime has been destroyed
     */
    EPDataFlowService getDataFlowService() throws EPRuntimeDestroyedException;

    /**
     * Returns the context partition service, for context partition information
     *
     * @return context partition service
     * @throws EPRuntimeDestroyedException thrown when the runtime has been destroyed
     */
    EPContextPartitionService getContextPartitionService() throws EPRuntimeDestroyedException;

    /**
     * Returns the variable service, for reading and writing variables
     *
     * @return variable service
     * @throws EPRuntimeDestroyedException thrown when the runtime has been destroyed
     */
    EPVariableService getVariableService() throws EPRuntimeDestroyedException;

    /**
     * Returns the metrics service, for managing runtime and statement metrics reporting
     *
     * @return metrics service
     * @throws EPRuntimeDestroyedException thrown when the runtime has been destroyed
     */
    EPMetricsService getMetricsService() throws EPRuntimeDestroyedException;

    /**
     * Returns the event type service, for obtaining information on event types
     *
     * @return event type service
     * @throws EPRuntimeDestroyedException thrown when the runtime has been destroyed
     */
    EPEventTypeService getEventTypeService() throws EPRuntimeDestroyedException;

    /**
     * Returns the event rendering service, for rendering events to JSON and XML
     *
     * @return render event service
     * @throws EPRuntimeDestroyedException thrown when the runtime has been destroyed
     */
    EPRenderEventService getRenderEventService() throws EPRuntimeDestroyedException;

    /**
     * Returns the fire-and-forget service, for executing fire-and-forget queries
     *
     * @return fire-and-forget service
     * @throws EPRuntimeDestroyedException thrown when the runtime has been destroyed
     */
    EPFireAndForgetService getFireAndForgetService() throws EPRuntimeDestroyedException;

    /**
     * Returns the deployment service, for deploying and undeploying compiled modules
     *
     * @return deployment service
     * @throws EPRuntimeDestroyedException thrown when the runtime has been destroyed
     */
    EPDeploymentService getDeploymentService() throws EPRuntimeDestroyedException;

    /**
     * Returns true if the runtime is in destroyed state, or false if not.
     *
     * @return indicator whether the runtime has been destroyed
     */
    boolean isDestroyed();

    /**
     * Frees any resources associated with this runtime instance, and leaves the runtime instance
     * ready for further use.
     * <p>
     * Do not use the {@link EPDeploymentService} administrative and {@link EPEventService} runtime instances obtained before the
     * initialize (including related services such as configuration, module management, etc.).
     * Your application must obtain new administrative and runtime instances.
     * <p>
     * Retains the existing configuration of the runtime instance but forgets any runtime configuration changes.
     * <p>
     * Stops and destroys any existing statement resources such as filters, patterns, expressions, views.
     */
    void initialize();

    /**
     * Returns the runtime URI, or "default" if this is the default runtime.
     *
     * @return runtime URI
     */
    String getURI();

    /**
     * Provides naming context for public named objects.
     * <p>
     * An extension point designed for use by input and output adapters as well as
     * other extension services.
     *
     * @return naming context providing name-to-object bindings
     * @throws EPRuntimeDestroyedException thrown when the runtime instance has been destroyed
     */
    Context getContext() throws EPRuntimeDestroyedException;

    /**
     * Destroys the runtime.
     * <p>
     * Releases any resources held by the runtime. The runtime enteres a state in
     * which operations provided by the runtime
     * are not guaranteed to operate properly.
     * <p>
     * Removes the runtime URI from the known URIs. Allows configuration to change for the instance.
     * <p>
     * When destroying a runtime your application must make sure that threads that are sending events into the runtime
     * have completed their work. More generally, the runtime should not be currently in use during or after the destroy operation.
     */
    void destroy();

    /**
     * Returns the runtime-instance global read-write lock.
     * The send-event methods takes a read lock.
     * The {@link EPDeploymentService#deploy(EPCompiled)} and {@link EPDeploymentService#undeploy(String)} methods take a write lock.
     *
     * @return runtime instance global read-write lock
     * @throws EPRuntimeDestroyedException thrown when the runtime has been destroyed
     */
    ReadWriteLock getRuntimeInstanceWideLock() throws EPRuntimeDestroyedException;

    /**
     * Add a listener to runtime state changes that receives a before-destroy event.
     * The listener collection applies set-semantics.
     *
     * @param listener to add
     */
    void addRuntimeStateListener(EPRuntimeStateListener listener);

    /**
     * Removate a listener to runtime state changes.
     *
     * @param listener to remove
     * @return true to indicate the listener was removed, or fals
     */
    boolean removeRuntimeStateListener(EPRuntimeStateListener listener);

    /**
     * Remove all listeners to runtime state changes.
     */
    void removeAllRuntimeStateListeners();

    /**
     * Returns a deep-copy of the configuration that is actively in use by the runtime.
     * <p>
     * Note: This can be an expensive operation.
     * </p>
     *
     * @return deep copy of the configuration
     */
    Configuration getConfigurationDeepCopy();

    /**
     * Returns the transient configuration, which are configuration values that are passed by reference (and not by value)
     *
     * @return transient configuration
     */
    Map<String, Object> getConfigurationTransient();

    /**
     * Returns a path object for use by the compiler that represents the EPL objects deployed into the runtime.
     *
     * @return path
     */
    EPCompilerPathable getRuntimePath();
}
