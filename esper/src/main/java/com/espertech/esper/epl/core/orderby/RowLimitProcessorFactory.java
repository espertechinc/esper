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
package com.espertech.esper.epl.core.orderby;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.RowLimitSpec;
import com.espertech.esper.epl.variable.VariableMetaData;
import com.espertech.esper.epl.variable.VariableReader;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.epl.variable.VariableServiceUtil;
import com.espertech.esper.util.JavaClassHelper;

/**
 * A factory for row-limit processor instances.
 */
public class RowLimitProcessorFactory {

    private final VariableMetaData numRowsVariableMetaData;
    private final VariableMetaData offsetVariableMetaData;
    private int currentRowLimit;
    private int currentOffset;

    /**
     * Ctor.
     *
     * @param rowLimitSpec        specification for row limit, or null if no row limit is defined
     * @param variableService     for retrieving variable state for use with row limiting
     * @param optionalContextName context name
     * @throws com.espertech.esper.epl.expression.core.ExprValidationException if row limit specification validation fails
     */
    public RowLimitProcessorFactory(RowLimitSpec rowLimitSpec, VariableService variableService, String optionalContextName)
            throws ExprValidationException {
        if (rowLimitSpec.getNumRowsVariable() != null) {
            numRowsVariableMetaData = variableService.getVariableMetaData(rowLimitSpec.getNumRowsVariable());
            if (numRowsVariableMetaData == null) {
                throw new ExprValidationException("Limit clause variable by name '" + rowLimitSpec.getNumRowsVariable() + "' has not been declared");
            }
            String message = VariableServiceUtil.checkVariableContextName(optionalContextName, numRowsVariableMetaData);
            if (message != null) {
                throw new ExprValidationException(message);
            }
            if (!JavaClassHelper.isNumeric(numRowsVariableMetaData.getType())) {
                throw new ExprValidationException("Limit clause requires a variable of numeric type");
            }
        } else {
            numRowsVariableMetaData = null;
            currentRowLimit = rowLimitSpec.getNumRows();

            if (currentRowLimit < 0) {
                currentRowLimit = Integer.MAX_VALUE;
            }
        }

        if (rowLimitSpec.getOptionalOffsetVariable() != null) {
            offsetVariableMetaData = variableService.getVariableMetaData(rowLimitSpec.getOptionalOffsetVariable());
            if (offsetVariableMetaData == null) {
                throw new ExprValidationException("Limit clause variable by name '" + rowLimitSpec.getOptionalOffsetVariable() + "' has not been declared");
            }
            String message = VariableServiceUtil.checkVariableContextName(optionalContextName, offsetVariableMetaData);
            if (message != null) {
                throw new ExprValidationException(message);
            }
            if (!JavaClassHelper.isNumeric(offsetVariableMetaData.getType())) {
                throw new ExprValidationException("Limit clause requires a variable of numeric type");
            }
        } else {
            offsetVariableMetaData = null;
            if (rowLimitSpec.getOptionalOffset() != null) {
                currentOffset = rowLimitSpec.getOptionalOffset();

                if (currentOffset <= 0) {
                    throw new ExprValidationException("Limit clause requires a positive offset");
                }
            } else {
                currentOffset = 0;
            }
        }
    }

    public RowLimitProcessor instantiate(AgentInstanceContext agentInstanceContext) {
        VariableReader numRowsVariableReader = null;
        if (numRowsVariableMetaData != null) {
            numRowsVariableReader = agentInstanceContext.getStatementContext().getVariableService().getReader(numRowsVariableMetaData.getVariableName(), agentInstanceContext.getAgentInstanceId());
        }

        VariableReader offsetVariableReader = null;
        if (offsetVariableMetaData != null) {
            offsetVariableReader = agentInstanceContext.getStatementContext().getVariableService().getReader(offsetVariableMetaData.getVariableName(), agentInstanceContext.getAgentInstanceId());
        }

        return new RowLimitProcessor(numRowsVariableReader, offsetVariableReader,
                currentRowLimit, currentOffset);
    }
}
