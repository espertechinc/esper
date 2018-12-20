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
package com.espertech.esperio.http;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.event.core.*;
import com.espertech.esper.common.internal.util.SimpleTypeParser;
import com.espertech.esper.common.internal.util.SimpleTypeParserFactory;
import com.espertech.esper.runtime.internal.kernel.service.EPRuntimeSPI;
import org.apache.http.*;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

public class EsperHttpRequestHandler implements HttpRequestHandler {

    private final static Logger log = LoggerFactory.getLogger(EsperHttpRequestHandler.class);

    private final EPRuntimeSPI runtime;
    private final Map<String, EsperHttpRequestCacheEntry> streamCache = new HashMap<String, EsperHttpRequestCacheEntry>();

    public EsperHttpRequestHandler(final EPRuntimeSPI runtime) {
        super();
        this.runtime = runtime;
    }

    public void handle(
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context) throws HttpException, IOException {

        String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
        if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("POST")) {
            throw new MethodNotSupportedException(method + " method not supported");
        }

        response.setStatusCode(HttpStatus.SC_OK);

        String target = request.getRequestLine().getUri();
        try {
            Map<String, String> parameters = parseTarget(target);
            handle(parameters);
        } catch (Throwable t) {
            log.error("Error processing Http GET request target '" + target + "' :" + t.getMessage(), t);
        }
    }

    private void handle(Map<String, String> parameters) {

        String eventTypeName = parameters.get("stream");

        EsperHttpRequestCacheEntry cacheEntry = streamCache.get(eventTypeName);
        if (cacheEntry == null) {
            cacheEntry = makeCacheEntry(eventTypeName);
            streamCache.put(eventTypeName, cacheEntry);
        }

        if (cacheEntry == null) {
            return;
        }

        Object[] values = new Object[cacheEntry.getParsers().length];
        for (int i = 0; i < cacheEntry.getParsers().length; i++) {
            String value = parameters.get(cacheEntry.getWritableProperties()[i].getPropertyName());
            if (value == null) {
                continue;
            }
            values[i] = cacheEntry.getParsers()[i].parse(value);
        }

        EventBean theEvent = cacheEntry.getEventBeanManufacturer().make(values);
        runtime.getEventServiceSPI().processWrappedEvent(theEvent);
    }

    private EsperHttpRequestCacheEntry makeCacheEntry(String eventTypeName) {

        EventType eventType = runtime.getServicesContext().getEventTypeRepositoryBus().getNameToTypeMap().get(eventTypeName);
        if (eventType == null) {
            log.info("Event type by name '" + eventTypeName + "' not found.");
            return null;
        }

        if (!(eventType instanceof EventTypeSPI)) {
            log.info("Event type by name '" + eventTypeName + "' is not writable.");
            return null;
        }

        EventTypeSPI eventTypeSPI = (EventTypeSPI) eventType;

        Set<WriteablePropertyDescriptor> writablesSet = EventTypeUtility.getWriteableProperties(eventTypeSPI, false, false);
        List<WriteablePropertyDescriptor> writablePropertiesList = new ArrayList<WriteablePropertyDescriptor>();
        List<SimpleTypeParser> parserList = new ArrayList<SimpleTypeParser>();

        for (WriteablePropertyDescriptor writableDesc : writablesSet) {
            SimpleTypeParser parser = SimpleTypeParserFactory.getParser(writableDesc.getType());
            if (parser == null) {
                log.debug("No parser found for type '" + writableDesc.getType() + "'");
                continue;
            }

            writablePropertiesList.add(writableDesc);
            parserList.add(parser);
        }

        WriteablePropertyDescriptor[] writableProperties = writablePropertiesList.toArray(new WriteablePropertyDescriptor[writablePropertiesList.size()]);
        SimpleTypeParser[] parsers = parserList.toArray(new SimpleTypeParser[parserList.size()]);

        EventBeanManufacturer eventBeanManufacturer;
        try {
            eventBeanManufacturer = EventTypeUtility.getManufacturer(eventType, writableProperties, runtime.getServicesContext().getClasspathImportServiceRuntime(), false, runtime.getServicesContext().getEventTypeAvroHandler()).getManufacturer(runtime.getServicesContext().getEventBeanTypedEventFactory());
        } catch (EventBeanManufactureException e) {
            log.info("Unable to create manufacturer for event type: " + e.getMessage(), e);
            return null;
        }

        return new EsperHttpRequestCacheEntry(eventBeanManufacturer, writableProperties, parsers);
    }

    private HashMap<String, String> parseTarget(String search) throws UnsupportedEncodingException {

        int start = search.indexOf('?');
        if (start == -1) {
            return new HashMap<String, String>();
        }
        String parameters = search.substring(start + 1, search.length());
        return splitParams(parameters);
    }

    private HashMap<String, String> splitParams(String search) throws UnsupportedEncodingException {
        HashMap<String, String> map = new HashMap<String, String>();
        String[] parameters = search.split("&");

        for (String param : parameters) {
            String[] temp = param.split("=");
            if (temp.length != 2) {
                continue;
            }
            map.put(temp[0], java.net.URLDecoder.decode(temp[1], "UTF-8"));
        }
        return map;
    }
}
