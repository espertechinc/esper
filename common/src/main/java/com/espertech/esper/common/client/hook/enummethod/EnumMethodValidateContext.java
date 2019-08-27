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
package com.espertech.esper.common.client.hook.enummethod;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.epl.enummethod.dot.EnumMethodEnum;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.methodbase.DotMethodFP;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;

import java.util.List;

/**
 * Context for use with the enumeration method extension API
 */
public class EnumMethodValidateContext {
    private final DotMethodFP footprintFound;
    private final EventType inputEventType;
    private final Class inputCollectionComponentType;
    private final StreamTypeService streamTypeService;
    private final EnumMethodEnum currentMethod;
    private final List<ExprNode> currentParameters;
    private final StatementRawInfo statementRawInfo;

    /**
     * Ctor.
     *
     * @param footprintFound               actual footprint chosen
     * @param inputEventType               input event type or null if the input is not a collection of events
     * @param inputCollectionComponentType type of scalar or object (non-event) input values, or null if the input is a collection of events
     * @param streamTypeService            event type information
     * @param currentMethod                information on the current method
     * @param currentParameters            parameters
     * @param statementRawInfo             EPL statement information
     */
    public EnumMethodValidateContext(DotMethodFP footprintFound, EventType inputEventType, Class inputCollectionComponentType, StreamTypeService streamTypeService, EnumMethodEnum currentMethod, List<ExprNode> currentParameters, StatementRawInfo statementRawInfo) {
        this.footprintFound = footprintFound;
        this.inputEventType = inputEventType;
        this.inputCollectionComponentType = inputCollectionComponentType;
        this.streamTypeService = streamTypeService;
        this.currentMethod = currentMethod;
        this.currentParameters = currentParameters;
        this.statementRawInfo = statementRawInfo;
    }

    /**
     * Returns the actual footprint chosen.
     *
     * @return footprint
     */
    public DotMethodFP getFootprintFound() {
        return footprintFound;
    }

    /**
     * Returns event type information.
     *
     * @return type info
     */
    public StreamTypeService getStreamTypeService() {
        return streamTypeService;
    }

    /**
     * Returns the enumeration method information
     *
     * @return current method
     */
    public EnumMethodEnum getCurrentMethod() {
        return currentMethod;
    }

    /**
     * Returns the parameters to the enumeration method.
     *
     * @return parameter expressions
     */
    public List<ExprNode> getCurrentParameters() {
        return currentParameters;
    }

    /**
     * Returns EPL statement information.
     *
     * @return statement info
     */
    public StatementRawInfo getStatementRawInfo() {
        return statementRawInfo;
    }

    /**
     * Returns the event type of the events that are the input of the enumeration method,
     * or null if the input to the enumeration method are scalar value input and not events
     *
     * @return input event type or null for scalar input
     */
    public EventType getInputEventType() {
        return inputEventType;
    }

    /**
     * Returns the component type of the values that are the input of the enumeration method,
     * or null if the input to the enumeration method are events and not scalar value input
     *
     * @return scalar value input type or null when the input is events
     */
    public Class getInputCollectionComponentType() {
        return inputCollectionComponentType;
    }
}
