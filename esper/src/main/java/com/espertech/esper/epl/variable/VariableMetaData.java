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
package com.espertech.esper.epl.variable;

import com.espertech.esper.client.EventType;

public class VariableMetaData {
    private final String variableName;
    private final String contextPartitionName;
    private final int variableNumber;
    private final Class type;
    private final EventType eventType;
    private final boolean constant;
    private final VariableStateFactory variableStateFactory;

    public VariableMetaData(String variableName, String contextPartitionName, int variableNumber, Class type, EventType eventType, boolean constant, VariableStateFactory variableStateFactory) {
        this.variableName = variableName;
        this.contextPartitionName = contextPartitionName;
        this.variableNumber = variableNumber;
        this.type = type;
        this.eventType = eventType;
        this.constant = constant;
        this.variableStateFactory = variableStateFactory;
    }

    /**
     * Returns the variable name.
     *
     * @return variable name
     */
    public String getVariableName() {
        return variableName;
    }

    public String getContextPartitionName() {
        return contextPartitionName;
    }

    /**
     * Returns the variable number.
     *
     * @return variable index number
     */
    public int getVariableNumber() {
        return variableNumber;
    }

    /**
     * Returns the type of the variable.
     *
     * @return type
     */
    public Class getType() {
        return type;
    }

    /**
     * Returns the event type if the variable hold event(s).
     *
     * @return type
     */
    public EventType getEventType() {
        return eventType;
    }

    public boolean isConstant() {
        return constant;
    }

    public VariableStateFactory getVariableStateFactory() {
        return variableStateFactory;
    }
}
