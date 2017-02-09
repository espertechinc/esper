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
package com.espertech.esper.util;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utility for inspecting and comparing URI.
 */
public class URIUtil {
    /**
     * Given a child URI and a map of factory URIs, inspect the child URI against the factory URIs
     * and return a collection of entries for which the child URI falls within or is equals to the factory URI.
     *
     * @param child is the child URI to match against factory URIs
     * @param uris  is a map of factory URI and an object
     * @return matching factory URIs, if any
     */
    public static Collection<Map.Entry<URI, Object>> filterSort(URI child, Map<URI, Object> uris) {
        boolean childPathIsOpaque = child.isOpaque();
        boolean childPathIsRelative = !child.isAbsolute();
        String[] childPathElements = parsePathElements(child);

        TreeMap<Integer, Map.Entry<URI, Object>> result = new TreeMap<Integer, Map.Entry<URI, Object>>();
        for (Map.Entry<URI, Object> entry : uris.entrySet()) {
            URI facoryUri = entry.getKey();

            // handle opaque (mailto:) and relative (a/b) using equals
            if (childPathIsOpaque || childPathIsRelative || !facoryUri.isAbsolute() || facoryUri.isOpaque()) {
                if (facoryUri.equals(child)) {
                    result.put(Integer.MIN_VALUE, entry);   // Equals is a perfect match
                }
                continue;
            }

            // handle absolute URIs, compare scheme and authority if present
            if (((child.getScheme() != null) && (facoryUri.getScheme() == null)) ||
                    ((child.getScheme() == null) && (facoryUri.getScheme() != null))) {
                continue;
            }
            if ((child.getScheme() != null) && (!child.getScheme().equals(facoryUri.getScheme()))) {
                continue;
            }
            if (((child.getAuthority() != null) && (facoryUri.getAuthority() == null)) ||
                    ((child.getAuthority() == null) && (facoryUri.getAuthority() != null))) {
                continue;
            }
            if ((child.getAuthority() != null) && (!child.getAuthority().equals(facoryUri.getAuthority()))) {
                continue;
            }

            // Match the child
            String[] factoryPathElements = parsePathElements(facoryUri);
            int score = computeScore(childPathElements, factoryPathElements);
            if (score > 0) {
                result.put(score, entry);   // Partial match if score is positive
            }
        }

        return result.values();
    }

    public static String[] parsePathElements(URI uri) {

        String path = uri.getPath();
        if (path == null) {
            return new String[0];
        }
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        String[] split = path.split("/");
        if ((split.length > 0) && (split[0].length() == 0)) {
            return new String[0];
        }
        return split;
    }

    private static int computeScore(String[] childPathElements, String[] factoryPathElements) {
        int index = 0;

        if (factoryPathElements.length == 0) {
            return Integer.MAX_VALUE;    // the most general factory scores the lowest
        }

        while (true) {
            if ((childPathElements.length > index) &&
                    (factoryPathElements.length > index)) {
                if (!(childPathElements[index].equals(factoryPathElements[index]))) {
                    return 0;
                }
            } else {
                if (childPathElements.length <= index) {
                    if (factoryPathElements.length > index) {
                        return 0;
                    }
                    return Integer.MAX_VALUE - index - 1;
                }
            }

            if (factoryPathElements.length <= index) {
                break;
            }

            index++;
        }

        return Integer.MAX_VALUE - index - 1;
    }
}
