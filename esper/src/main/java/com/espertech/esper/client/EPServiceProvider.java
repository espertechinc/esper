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
package com.espertech.esper.client;

import javax.naming.Context;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * This class provides access to the EPRuntime and EPAdministrator implementations.
 */
public interface EPServiceProvider {
    /**
     * Returns a class instance of EPRuntime.
     * <p>
     * If the engine instance is destroyed, the behavior is undefined and a NullPointerException is possible.
     *
     * @return an instance of EPRuntime
     * @throws EPServiceDestroyedException thrown when the engine instance has been destroyed
     */
    public EPRuntime getEPRuntime() throws EPServiceDestroyedException;

    /**
     * Returns a class instance of EPAdministrator.
     * <p>
     * If the engine instance is destroyed, the behavior is undefined and a NullPointerException is possible.
     *
     * @return an instance of EPAdministrator
     * @throws EPServiceDestroyedException thrown when the engine instance has been destroyed
     */
    public EPAdministrator getEPAdministrator() throws EPServiceDestroyedException;

    /**
     * Provides naming context for public named objects.
     * <p>
     * An extension point designed for use by input and output adapters as well as
     * other extension services.
     *
     * @return naming context providing name-to-object bindings
     * @throws EPServiceDestroyedException thrown when the engine instance has been destroyed
     */
    public Context getContext() throws EPServiceDestroyedException;

    /**
     * Frees any resources associated with this engine instance, and leaves the engine instance
     * ready for further use.
     * <p>
     * Do not use the {@link EPAdministrator} administrative and {@link EPRuntime} runtime instances obtained before the
     * initialize (including related services such as configuration, module management, etc.).
     * Your application must obtain new administrative and runtime instances.
     * <p>
     * Retains the existing configuration of the engine instance but forgets any runtime configuration changes.
     * <p>
     * Stops and destroys any existing statement resources such as filters, patterns, expressions, views.
     */
    public void initialize();

    /**
     * Returns the provider URI, or "default" if this is the default provider.
     *
     * @return provider URI
     */
    public String getURI();

    /**
     * Destroys the service.
     * <p>
     * Releases any resources held by the service. The service enteres a state in
     * which operations provided by administrative and runtime interfaces originiated by the service
     * are not guaranteed to operate properly.
     * <p>
     * Removes the service URI from the known URIs. Allows configuration to change for the instance.
     * <p>
     * When destroying a service instance your application must make sure that threads that are sending events into the service
     * have completed their work. More generally, the service should not be currently in use during or after the destroy operation.
     */
    public void destroy();

    /**
     * Returns true if the service is in destroyed state, or false if not.
     *
     * @return indicator whether the service has been destroyed
     */
    public boolean isDestroyed();

    /**
     * Add a listener to service provider state changes that receives a before-destroy event.
     * The listener collection applies set-semantics.
     *
     * @param listener to add
     */
    public void addServiceStateListener(EPServiceStateListener listener);

    /**
     * Removate a listener to service provider state changes.
     *
     * @param listener to remove
     * @return true to indicate the listener was removed, or fals
     */
    public boolean removeServiceStateListener(EPServiceStateListener listener);

    /**
     * Remove all listeners to service provider state changes.
     */
    public void removeAllServiceStateListeners();

    /**
     * Add a listener to statement state changes that receives statement-level events.
     * The listener collection applies set-semantics.
     *
     * @param listener to add
     */
    public void addStatementStateListener(EPStatementStateListener listener);

    /**
     * Removate a listener to statement state changes.
     *
     * @param listener to remove
     * @return true to indicate the listener was removed, or fals
     */
    public boolean removeStatementStateListener(EPStatementStateListener listener);

    /**
     * Remove all listeners to statement state changes.
     */
    public void removeAllStatementStateListeners();

    /**
     * Returns the isolated service provider for that name,
     * creating an isolated service if the name is a new name, or
     * returning an existing isolated service for an existing name.
     * <p>
     * Note: Requires configuration setting.
     * </p>
     *
     * @param name to return isolated service for
     * @return isolated service
     * @throws EPServiceDestroyedException  thrown when the engine instance has been destroyed
     * @throws EPServiceNotAllowedException thrown when the engine configuration does not allow isolated service providers
     */
    public EPServiceProviderIsolated getEPServiceIsolated(String name) throws EPServiceDestroyedException, EPServiceNotAllowedException;

    /**
     * Returns the names of isolated service providers currently allocated.
     *
     * @return isolated service provider names
     */
    public String[] getEPServiceIsolatedNames();

    /**
     * Returns the engine-instance global read-write lock.
     * The {@link com.espertech.esper.client.EPRuntime#sendEvent} method takes a read lock.
     * The {@link com.espertech.esper.client.EPAdministrator#createEPL} methods take a write lock.
     *
     * @return engine instance global read-write lock
     */
    public ReadWriteLock getEngineInstanceWideLock();
}
