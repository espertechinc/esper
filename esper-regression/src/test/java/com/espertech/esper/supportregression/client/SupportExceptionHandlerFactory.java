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
package com.espertech.esper.supportregression.client;

import com.espertech.esper.client.hook.*;

import java.util.ArrayList;
import java.util.List;

public class SupportExceptionHandlerFactory implements ExceptionHandlerFactory {

    private static List<ExceptionHandlerFactoryContext> factoryContexts = new ArrayList<ExceptionHandlerFactoryContext>();
    private static List<SupportExceptionHandler> handlers = new ArrayList<SupportExceptionHandler>();

    @Override
    public ExceptionHandler getHandler(ExceptionHandlerFactoryContext context) {
        factoryContexts.add(context);
        SupportExceptionHandler handler = new SupportExceptionHandler();
        handlers.add(handler);
        return handler;
    }

    public static List<ExceptionHandlerFactoryContext> getFactoryContexts() {
        return factoryContexts;
    }

    public static List<SupportExceptionHandler> getHandlers() {
        return handlers;
    }

    public static class SupportExceptionHandler implements ExceptionHandlerInboundPool {
        private List<ExceptionHandlerContext> contexts = new ArrayList<ExceptionHandlerContext>();
        private List<ExceptionHandlerContextUnassociated> inboundPoolContexts = new ArrayList<ExceptionHandlerContextUnassociated>();

        public SupportExceptionHandler() {
        }

        public void handle(ExceptionHandlerContext context) {
            contexts.add(context);
        }

        public List<ExceptionHandlerContext> getContexts() {
            return contexts;
        }

        public List<ExceptionHandlerContextUnassociated> getInboundPoolContexts() {
            return inboundPoolContexts;
        }

        public void handleInboundPoolUnassociated(ExceptionHandlerContextUnassociated context) {
            inboundPoolContexts.add(context);
        }
    }
}
