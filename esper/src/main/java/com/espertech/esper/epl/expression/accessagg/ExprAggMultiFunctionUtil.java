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
package com.espertech.esper.epl.expression.accessagg;

import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprStreamUnderlyingNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;

public class ExprAggMultiFunctionUtil {
    public static int validateStreamWildcardGetStreamNum(ExprNode node)
            throws ExprValidationException {
        if (!(node instanceof ExprStreamUnderlyingNode)) {
            throw new IllegalStateException("Expression not stream-wildcard");
        }
        ExprStreamUnderlyingNode und = (ExprStreamUnderlyingNode) node;
        if (und.getStreamId() == -1) {
            throw new ExprValidationException("The expression does not resolve to a stream");
        }
        return und.getStreamId();
    }

    public static void validateWildcardStreamNumbers(StreamTypeService streamTypeService, String aggFuncName)
            throws ExprValidationException {
        checkWildcardNotJoinOrSubquery(streamTypeService, aggFuncName);
        checkWildcardHasStream(streamTypeService, aggFuncName);
    }

    public static void checkWildcardNotJoinOrSubquery(StreamTypeService streamTypeService, String aggFuncName)
            throws ExprValidationException {
        if (streamTypeService.getStreamNames().length > 1) {
            throw new ExprValidationException(getErrorPrefix(aggFuncName) + " requires that in joins or subqueries the stream-wildcard (stream-alias.*) syntax is used instead");
        }
    }

    private static void checkWildcardHasStream(StreamTypeService streamTypeService, String aggFuncName)
            throws ExprValidationException {
        if (streamTypeService.getStreamNames().length == 0) {    // could be the case for
            throw new ExprValidationException(getErrorPrefix(aggFuncName) + " requires that at least one stream is provided");
        }
    }

    public static String getErrorPrefix(String aggFuncName) {
        return "The '" + aggFuncName + "' aggregation function";
    }
}