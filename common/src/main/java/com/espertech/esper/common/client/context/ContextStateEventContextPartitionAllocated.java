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
package com.espertech.esper.common.client.context;

/**
 * Context partition state event indicating a context partition allocated.
 */
public class ContextStateEventContextPartitionAllocated extends ContextStateEventContextPartition {

    private final ContextPartitionIdentifier identifier;

    /**
     * Ctor.
     *
     * @param runtimeURI          runtime URI
     * @param contextDeploymentId deployment id of create-context statement
     * @param contextName         context name
     * @param id                  context partition id
     * @param identifier          identifier
     */
    public ContextStateEventContextPartitionAllocated(String runtimeURI, String contextDeploymentId, String contextName, int id, ContextPartitionIdentifier identifier) {
        super(runtimeURI, contextDeploymentId, contextName, id);
        this.identifier = identifier;
    }

    /**
     * Returns the identifier; For nested context the identifier is the identifier of the last or innermost context.
     *
     * @return identifier
     */
    public ContextPartitionIdentifier getIdentifier() {
        return identifier;
    }
}
