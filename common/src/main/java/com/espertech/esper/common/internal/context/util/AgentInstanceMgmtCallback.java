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
package com.espertech.esper.common.internal.context.util;

public interface AgentInstanceMgmtCallback {
    AgentInstanceMgmtCallback INSTANCE_NO_ACTION = new AgentInstanceMgmtCallback() {
        public void stop(AgentInstanceStopServices services) {
            // no action
        }

        public void transfer(AgentInstanceTransferServices services) {
            // no action
        }
    };

    void stop(AgentInstanceStopServices services);

    default void transfer(AgentInstanceTransferServices services) {
        // no action
    }
}
