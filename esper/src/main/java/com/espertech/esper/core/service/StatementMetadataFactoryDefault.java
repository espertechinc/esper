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
package com.espertech.esper.core.service;

import com.espertech.esper.epl.spec.OnTriggerType;
import com.espertech.esper.epl.spec.StatementSpecRaw;

/**
 * Statement metadata.
 */
public class StatementMetadataFactoryDefault implements StatementMetadataFactory {
    public StatementMetadata create(StatementMetadataFactoryContext context) {
        return new StatementMetadata(getStatementType(context.getStatementSpec(), context.isPattern()));
    }

    public static StatementType getStatementType(StatementSpecRaw statementSpec, boolean pattern) {

        // determine statement type
        StatementType statementType = null;
        if (statementSpec.getCreateVariableDesc() != null) {
            statementType = StatementType.CREATE_VARIABLE;
        } else if (statementSpec.getCreateTableDesc() != null) {
            statementType = StatementType.CREATE_TABLE;
        } else if (statementSpec.getCreateWindowDesc() != null) {
            statementType = StatementType.CREATE_WINDOW;
        } else if (statementSpec.getOnTriggerDesc() != null) {
            if (statementSpec.getOnTriggerDesc().getOnTriggerType() == OnTriggerType.ON_DELETE) {
                statementType = StatementType.ON_DELETE;
            } else if (statementSpec.getOnTriggerDesc().getOnTriggerType() == OnTriggerType.ON_UPDATE) {
                statementType = StatementType.ON_UPDATE;
            } else if (statementSpec.getOnTriggerDesc().getOnTriggerType() == OnTriggerType.ON_SELECT) {
                if (statementSpec.getInsertIntoDesc() != null) {
                    statementType = StatementType.ON_INSERT;
                } else {
                    statementType = StatementType.ON_SELECT;
                }
            } else if (statementSpec.getOnTriggerDesc().getOnTriggerType() == OnTriggerType.ON_SET) {
                statementType = StatementType.ON_SET;
            } else if (statementSpec.getOnTriggerDesc().getOnTriggerType() == OnTriggerType.ON_MERGE) {
                statementType = StatementType.ON_MERGE;
            } else if (statementSpec.getOnTriggerDesc().getOnTriggerType() == OnTriggerType.ON_SPLITSTREAM) {
                statementType = StatementType.ON_SPLITSTREAM;
            }
        } else if (statementSpec.getInsertIntoDesc() != null) {
            statementType = StatementType.INSERT_INTO;
        } else if (pattern) {
            statementType = StatementType.PATTERN;
        } else if (statementSpec.getUpdateDesc() != null) {
            statementType = StatementType.UPDATE;
        } else if (statementSpec.getCreateIndexDesc() != null) {
            statementType = StatementType.CREATE_INDEX;
        } else if (statementSpec.getCreateContextDesc() != null) {
            statementType = StatementType.CREATE_CONTEXT;
        } else if (statementSpec.getCreateSchemaDesc() != null) {
            statementType = StatementType.CREATE_SCHEMA;
        } else if (statementSpec.getCreateDataFlowDesc() != null) {
            statementType = StatementType.CREATE_DATAFLOW;
        } else if (statementSpec.getCreateExpressionDesc() != null) {
            statementType = StatementType.CREATE_EXPRESSION;
        }
        if (statementType == null) {
            statementType = StatementType.SELECT;
        }
        return statementType;
    }
}
