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
package com.espertech.esper.common.client.hook.datetimemethod;

import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.epl.datetime.eval.DatetimeMethodDesc;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.methodbase.DotMethodFP;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;

import java.util.List;

/**
 * Context for use with the date-time method extension API
 */
public class DateTimeMethodValidateContext {
    private final DotMethodFP footprintFound;
    private final StreamTypeService streamTypeService;
    private final DatetimeMethodDesc currentMethod;
    private final List<ExprNode> currentParameters;
    private final StatementRawInfo statementRawInfo;

    /**
     * Ctor.
     * @param footprintFound actual footprint chosen
     * @param streamTypeService event type information
     * @param currentMethod information on the current method
     * @param currentParameters parameters
     * @param statementRawInfo EPL statement information
     */
    public DateTimeMethodValidateContext(DotMethodFP footprintFound, StreamTypeService streamTypeService, DatetimeMethodDesc currentMethod, List<ExprNode> currentParameters, StatementRawInfo statementRawInfo) {
        this.footprintFound = footprintFound;
        this.streamTypeService = streamTypeService;
        this.currentMethod = currentMethod;
        this.currentParameters = currentParameters;
        this.statementRawInfo = statementRawInfo;
    }

    /**
     * Returns the actual footprint chosen.
     * @return footprint
     */
    public DotMethodFP getFootprintFound() {
        return footprintFound;
    }

    /**
     * Returns event type information.
     * @return type info
     */
    public StreamTypeService getStreamTypeService() {
        return streamTypeService;
    }

    /**
     * Returns the date-time method information
     * @return current method
     */
    public DatetimeMethodDesc getCurrentMethod() {
        return currentMethod;
    }

    /**
     * Returns the parameters to the date-time method.
     * @return parameter expressions
     */
    public List<ExprNode> getCurrentParameters() {
        return currentParameters;
    }

    /**
     * Returns EPL statement information.
     * @return statement info
     */
    public StatementRawInfo getStatementRawInfo() {
        return statementRawInfo;
    }
}
