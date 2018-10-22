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
package com.espertech.esperio.http.core;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

public class URIUtil {

    public static URI withQuery(URI uri, String... parameters) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        for (int i = 0; i < parameters.length; i += 2) {
            String key = parameters[i];
            String val = i + 1 < parameters.length ? parameters[i + 1] : "";
            map.put(key, val);
        }
        return withQuery(uri, map);
    }

    public static URI withQuery(URI uri, Map<String, String> parameters) {
        StringBuilder query = new StringBuilder();
        char separator = '?';
        for (Map.Entry<String, String> param : parameters.entrySet()) {
            query.append(separator);
            separator = '&';
            try {
                query.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                if (param.getValue().length() != 0) {
                    query.append('=');
                    query.append(URLEncoder.encode(param.getValue(), "UTF-8"));
                }
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        return URI.create(uri.toString() + query.toString());
    }
}
