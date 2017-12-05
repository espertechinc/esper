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
package com.espertech.esper.epl.spec;

import com.espertech.esper.epl.expression.core.ExprNode;

import java.io.Serializable;
import java.util.List;

/**
 * Specification object for historical data poll via database SQL statement.
 */
public class MethodStreamSpec extends StreamSpecBase implements StreamSpecRaw, StreamSpecCompiled, Serializable {
    private String ident;
    private String className;
    private String methodName;
    private List<ExprNode> expressions;
    private String eventTypeName;
    private static final long serialVersionUID = -5290682188045211532L;

    /**
     * Ctor.
     *
     * @param optionalStreamName is the stream name or null if none defined
     * @param viewSpecs          is an list of view specifications
     * @param ident              the prefix in the clause
     * @param className          the class name
     * @param methodName         the method name
     * @param expressions        the parameter expressions
     * @param eventTypeName      event type name if provided
     */
    public MethodStreamSpec(String optionalStreamName, ViewSpec[] viewSpecs, String ident, String className, String methodName, List<ExprNode> expressions, String eventTypeName) {
        super(optionalStreamName, viewSpecs, StreamSpecOptions.DEFAULT);
        this.ident = ident;
        this.className = className;
        this.methodName = methodName;
        this.expressions = expressions;
        this.eventTypeName = eventTypeName;
    }

    /**
     * Returns the prefix (method) for the method invocation syntax.
     *
     * @return identifier
     */
    public String getIdent() {
        return ident;
    }

    /**
     * Returns the class name.
     *
     * @return class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Returns the method name.
     *
     * @return method name
     */
    public String getMethodName() {
        return methodName;
    }

    public String getEventTypeName() {
        return eventTypeName;
    }

    /**
     * Returns the parameter expressions.
     *
     * @return parameter expressions
     */
    public List<ExprNode> getExpressions() {
        return expressions;
    }
}
