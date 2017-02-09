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

import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;

import java.util.Collection;
import java.util.Set;

/**
 * An uncompiled, unoptimize for of stream specification created by a parser.
 */
public interface StreamSpecRaw extends StreamSpec {
    /**
     * Compiles a raw stream specification consisting event type information and filter expressions
     * to an validated, optimized form for use with filter service
     *
     * @param statementContext        statement-level services
     * @param eventTypeReferences     event type names used by the statement
     * @param isInsertInto            true for insert-into
     * @param isJoin                  indicates whether a join or not a join
     * @param isContextDeclaration    indicates whether declared as part of the context declarations, if any
     * @param isOnTrigger             indicator for on-trigger
     * @param optionalStreamName      stream name
     * @param assignedTypeNumberStack for assigning nested type numbers
     * @return compiled stream
     * @throws ExprValidationException to indicate validation errors
     */
    public StreamSpecCompiled compile(StatementContext statementContext,
                                      Set<String> eventTypeReferences,
                                      boolean isInsertInto,
                                      Collection<Integer> assignedTypeNumberStack,
                                      boolean isJoin,
                                      boolean isContextDeclaration,
                                      boolean isOnTrigger,
                                      String optionalStreamName)
            throws ExprValidationException;

}
