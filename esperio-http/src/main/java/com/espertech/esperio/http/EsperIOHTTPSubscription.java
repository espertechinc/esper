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

import com.espertech.esper.adapter.BaseSubscription;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.event.EventBeanReader;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.filter.FilterHandleCallback;
import com.espertech.esper.util.PlaceholderParseException;
import com.espertech.esper.util.PlaceholderParser;
import com.espertech.esperio.http.core.URIUtil;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.ConfigurationException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class EsperIOHTTPSubscription extends BaseSubscription {
    private final static Logger log = LoggerFactory.getLogger(EsperIOHTTPSubscription.class);

    private String uuid = UUID.randomUUID().toString();

    private final String stream;
    private final URI uriPrecompiled;
    private final HttpClient httpclient;
    private final ResponseHandler<String> responseHandler;
    private final List<PlaceholderParser.Fragment> fragments;

    public EsperIOHTTPSubscription(String stream, String uriWithReplacements) throws URISyntaxException, ConfigurationException {
        this.httpclient = new DefaultHttpClient();
        this.responseHandler = new BasicResponseHandler();
        this.stream = stream;

        if (uriWithReplacements.indexOf("${") == -1) {
            uriPrecompiled = new URI(uriWithReplacements);
            fragments = null;
        } else {
            try {
                fragments = PlaceholderParser.parsePlaceholder(uriWithReplacements);
            } catch (PlaceholderParseException e) {
                throw new ConfigurationException("URI with placeholders '" + uriWithReplacements + "' could not be parsed");
            }
            uriPrecompiled = null;
        }
    }

    public void matchFound(EventBean theEvent, Collection<FilterHandleCallback> allStmtMatches) {
        EventTypeSPI spi = (EventTypeSPI) theEvent.getEventType();
        EventBeanReader reader = spi.getReader();
        Object[] props = reader.read(theEvent);
        String[] names = spi.getPropertyNames();
        Map<String, String> parameters = formPairs(names, props, "stream", stream);

        URI requestURI;
        if (uriPrecompiled != null) {
            requestURI = URIUtil.withQuery(uriPrecompiled, parameters);
        } else {
            String uri = formURI(parameters, fragments);
            try {
                requestURI = new URI(uri);
            } catch (URISyntaxException e) {
                log.error("Incorrect URI generated: " + e.getMessage(), e);
                return;
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Requesting from URI " + requestURI);
        }

        HttpGet httpget = new HttpGet(requestURI);

        try {
            httpclient.execute(httpget, responseHandler);
        } catch (IOException e) {
            log.warn("Error executing request:" + e.getMessage());
        }
    }

    private String formURI(Map<String, String> parameters, List<PlaceholderParser.Fragment> fragments) {
        StringBuilder buf = new StringBuilder();
        for (PlaceholderParser.Fragment fragment : fragments) {
            if (fragment instanceof PlaceholderParser.ParameterFragment) {
                PlaceholderParser.ParameterFragment param = (PlaceholderParser.ParameterFragment) fragment;
                String val = parameters.get(param.getValue());
                if (val == null) {
                    buf.append("null");
                } else {
                    buf.append(val);
                }
            } else {
                PlaceholderParser.TextFragment param = (PlaceholderParser.TextFragment) fragment;
                buf.append(param.getValue());
            }
        }
        return buf.toString();
    }

    public boolean isSubSelect() {
        return false;
    }

    public int getStatementId() {
        return -1;
    }

    private static Map<String, String> formPairs(String[] properties, Object[] values, String... additional) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        for (int i = 0; i < additional.length; i += 2) {
            String key = additional[i];
            String val = i + 1 < additional.length ? additional[i + 1] : "";
            map.put(key, val);
        }
        for (int i = 0; i < properties.length; i++) {
            String key = properties[i];
            Object value = values[i];
            if (value == null) {
                map.put(key, "");
            } else {
                map.put(key, value.toString());
            }
        }
        return map;
    }
}
