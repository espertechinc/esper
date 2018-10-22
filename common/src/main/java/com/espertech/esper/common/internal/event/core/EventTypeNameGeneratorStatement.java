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
package com.espertech.esper.common.internal.event.core;

public class EventTypeNameGeneratorStatement {
    private final int statementNumber;
    private int seqNum;

    public EventTypeNameGeneratorStatement(int statementNumber) {
        this.statementNumber = statementNumber;
    }

    public String getAnonymousTypeName() {
        return formatSingleConst("out");
    }

    public String getAnonymousTypeNameWithInner(int expressionNum) {
        return formatNV("eval", Integer.toString(expressionNum));
    }

    public String getAnonymousDBHistorical(int streamNum) {
        return formatNV("dbpoll", Integer.toString(streamNum));
    }

    public String getAnonymousMethodHistorical(int streamNum) {
        return formatNV("methodpoll", Integer.toString(streamNum));
    }

    public String getAnonymousRowrecogCompositeName() {
        return formatSingleConst("mrcomp");
    }

    public String getAnonymousRowrecogRowName() {
        return formatSingleConst("mrrow");
    }

    public String getAnonymousRowrecogMultimatchDefineName(int defineNum) {
        return formatNV("mrmd", Integer.toString(defineNum));
    }

    public String getAnonymousRowrecogMultimatchAllName() {
        return formatSingleConst("mrma");
    }

    public String getPatternTypeName(int stream) {
        return formatNV("pat", Integer.toString(stream));
    }

    public String getViewDerived(String name, int streamNum) {
        return formatNV("view", name + "(" + streamNum + ")");
    }

    public String getViewExpr(int streamNumber) {
        return formatNV("expr", Integer.toString(streamNumber));
    }

    public String getViewGroup(int streamNum) {
        return formatNV("grp", Integer.toString(streamNum));
    }

    public String getAnonymousTypeNameEnumMethod(String enumMethod, String propertyName) {
        return formatNV("enum", enumMethod + "(" + propertyName + ")");
    }

    public String getAnonymousTypeSubselectMultirow(int subselectNumber) {
        return formatNV("subq", Integer.toString(subselectNumber));
    }

    public String getAnonymousTypeNameUDFMethod(String methodName, String typeName) {
        return formatNV("mth", methodName + "(" + typeName + ")");
    }

    public String getAnonymousPatternName(int streamNum, short factoryNodeId) {
        return formatNV("pan", streamNum + "(" + factoryNodeId + ")");
    }

    public String getAnonymousPatternNameWTag(int streamNum, short factoryNodeId, String tag) {
        return formatNV("pwt", streamNum + "(" + factoryNodeId + "_" + tag + ")");
    }

    public String getContextPropertyTypeName(String contextName) {
        return formatNV("ctx", contextName);
    }

    public String getContextStatementTypeName(String contextName) {
        return formatNV("ctxout", contextName);
    }

    public String getDataflowOperatorTypeName(int operatorNumber) {
        return formatNV("df", "op_" + operatorNumber);
    }

    private String formatSingleConst(String postfixConst) {
        StringBuilder builder = new StringBuilder();
        builder.append("stmt");
        builder.append(statementNumber);
        builder.append("_");
        builder.append(postfixConst);
        builder.append(seqNum++);
        return builder.toString();
    }

    private String formatNV(String postfixNameOne, String postfixValueOne) {
        StringBuilder builder = new StringBuilder();
        builder.append("stmt");
        builder.append(statementNumber);
        builder.append("_");
        builder.append(postfixNameOne);
        builder.append("_");
        builder.append(postfixValueOne);
        builder.append("_");
        builder.append(seqNum++);
        return builder.toString();
    }
}
