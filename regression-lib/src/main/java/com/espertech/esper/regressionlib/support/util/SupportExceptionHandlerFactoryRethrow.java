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
package com.espertech.esper.regressionlib.support.util;

import com.espertech.esper.common.client.hook.exception.ExceptionHandler;
import com.espertech.esper.common.client.hook.exception.ExceptionHandlerContext;
import com.espertech.esper.common.client.hook.exception.ExceptionHandlerFactory;
import com.espertech.esper.common.client.hook.exception.ExceptionHandlerFactoryContext;

public class SupportExceptionHandlerFactoryRethrow implements ExceptionHandlerFactory {

    @Override
    public ExceptionHandler getHandler(ExceptionHandlerFactoryContext context) {
        return new SupportExceptionHandlerRethrow();
    }

    public static class SupportExceptionHandlerRethrow implements ExceptionHandler {
        public void handle(ExceptionHandlerContext context) {
            throw new RuntimeException("Unexpected exception in statement '" + context.getStatementName() +
                "': " + context.getThrowable().getMessage(), context.getThrowable());
        }
    }
}
