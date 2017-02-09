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

import com.espertech.esper.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.epl.spec.FilterStreamSpecRaw;
import com.espertech.esper.epl.spec.StatementSpecRaw;
import com.espertech.esper.epl.spec.StreamSpecRaw;
import com.espertech.esper.epl.table.mgmt.TableService;

import java.util.List;

public class StatementContextFactoryUtil {
    public static boolean determineHasTableAccess(List<ExprSubselectNode> subselectNodes, StatementSpecRaw statementSpecRaw, EPServicesContext engineServices) {
        boolean hasTableAccess = (statementSpecRaw.getTableExpressions() != null && !statementSpecRaw.getTableExpressions().isEmpty()) ||
                statementSpecRaw.getIntoTableSpec() != null;
        hasTableAccess = hasTableAccess || isJoinWithTable(statementSpecRaw, engineServices.getTableService()) || isSubqueryWithTable(subselectNodes, engineServices.getTableService()) || isInsertIntoTable(statementSpecRaw, engineServices.getTableService());
        return hasTableAccess;
    }

    private static boolean isInsertIntoTable(StatementSpecRaw statementSpecRaw, TableService tableService) {
        if (statementSpecRaw.getInsertIntoDesc() == null) {
            return false;
        }
        return tableService.getTableMetadata(statementSpecRaw.getInsertIntoDesc().getEventTypeName()) != null;
    }

    private static boolean isSubqueryWithTable(List<ExprSubselectNode> subselectNodes, TableService tableService) {
        for (ExprSubselectNode node : subselectNodes) {
            FilterStreamSpecRaw spec = (FilterStreamSpecRaw) node.getStatementSpecRaw().getStreamSpecs().get(0);
            if (tableService.getTableMetadata(spec.getRawFilterSpec().getEventTypeName()) != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean isJoinWithTable(StatementSpecRaw statementSpecRaw, TableService tableService) {
        for (StreamSpecRaw stream : statementSpecRaw.getStreamSpecs()) {
            if (stream instanceof FilterStreamSpecRaw) {
                FilterStreamSpecRaw filter = (FilterStreamSpecRaw) stream;
                if (tableService.getTableMetadata(filter.getRawFilterSpec().getEventTypeName()) != null) {
                    return true;
                }
            }
        }
        return false;
    }
}
