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

import junit.framework.TestCase;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TestURIUtil extends TestCase {
    public void testSortRelevance() throws Exception {
        Object[][] uris = new Object[][]{
                {"a/relative/one", -1},
                {"other:mailto:test", 0},
                {"other://a", 1},
                {"type://a/b2/c1", 2},
                {"type://a/b3", 3},
                {"type://a/b2/c2", 4},
                {"type://a", 5},
                {"type://x?query#fragment&param", 6},
                {"type://a/b1/c1", 7},
                {"type://a/b1/c2", 8},
                {"type://a/b1/c2/d1", 9},
                {"type://a/b2", 10},
                {"type://x/a?query#fragment&param", 11},
                {"type://x/a/b?query#fragment&param", 12},
                {"/a/b/c", 13},
                {"/a", 14},
                {"//a/b/c", 15},
                {"//a", 16},
        };

        // setup input
        Map<URI, Object> input = new HashMap<URI, Object>();
        for (Object[] uri1 : uris) {
            URI uri = new URI((String) uri1[0]);
            input.put(uri, uri1[1]);
        }

        URI uri;
        Collection<Map.Entry<URI, Object>> result;
        String[] expected;

        uri = new URI("type://x/a/b?qqq");
        result = URIUtil.filterSort(uri, input);
        expected = new String[]{"type://x/a/b?query#fragment&param", "type://x/a?query#fragment&param", "type://x?query#fragment&param"};
        runAssertion(uri, input, result, expected);

        // unspecific child
        uri = new URI("type://a/b2");
        result = URIUtil.filterSort(uri, input);
        expected = new String[]{"type://a/b2", "type://a"};
        runAssertion(uri, input, result, expected);

        // very specific child
        uri = new URI("type://a/b2/c2/d/e");
        result = URIUtil.filterSort(uri, input);
        expected = new String[]{"type://a/b2/c2", "type://a/b2", "type://a"};
        runAssertion(uri, input, result, expected);

        // less specific child
        uri = new URI("type://a/b1/c2");
        result = URIUtil.filterSort(uri, input);
        expected = new String[]{"type://a/b1/c2", "type://a"};
        runAssertion(uri, input, result, expected);

        // unspecific child
        uri = new URI("type://a/b4");
        result = URIUtil.filterSort(uri, input);
        expected = new String[]{"type://a"};
        runAssertion(uri, input, result, expected);

        uri = new URI("type://b/b1");
        result = URIUtil.filterSort(uri, input);
        expected = new String[]{};
        runAssertion(uri, input, result, expected);

        uri = new URI("type://a/b1/c2/d1/e1/f1");
        result = URIUtil.filterSort(uri, input);
        expected = new String[]{"type://a/b1/c2/d1", "type://a/b1/c2", "type://a"};
        runAssertion(uri, input, result, expected);

        uri = new URI("other:mailto:test");
        result = URIUtil.filterSort(uri, input);
        expected = new String[]{"other:mailto:test"};
        runAssertion(uri, input, result, expected);

        uri = new URI("type://x/a?qqq");
        result = URIUtil.filterSort(uri, input);
        expected = new String[]{"type://x/a?query#fragment&param", "type://x?query#fragment&param"};
        runAssertion(uri, input, result, expected);

        uri = new URI("other://x/a?qqq");
        result = URIUtil.filterSort(uri, input);
        expected = new String[]{};
        runAssertion(uri, input, result, expected);

        // this is seen as relative, must be a full hit (no path checking)
        uri = new URI("/a/b");
        result = URIUtil.filterSort(uri, input);
        expected = new String[]{};
        runAssertion(uri, input, result, expected);

        // this is seen as relative
        uri = new URI("/a/b/c");
        result = URIUtil.filterSort(uri, input);
        expected = new String[]{"/a/b/c"};
        runAssertion(uri, input, result, expected);

        // this is seen as relative
        uri = new URI("//a/b");
        result = URIUtil.filterSort(uri, input);
        expected = new String[]{};
        runAssertion(uri, input, result, expected);

        // this is seen as relative
        uri = new URI("//a/b/c");
        result = URIUtil.filterSort(uri, input);
        expected = new String[]{"//a/b/c"};
        runAssertion(uri, input, result, expected);
    }

    private static void runAssertion(URI uriTested, Map<URI, Object> input, Collection<Map.Entry<URI, Object>> result, String[] expected)
            throws Exception {
        // assert
        assertEquals("found: " + result.toString() + " for URI " + uriTested, expected.length, result.size());
        int index = 0;
        for (Map.Entry<URI, Object> entry : result) {
            URI expectedUri = new URI(expected[index]);
            String message = "mismatch for line " + index;
            assertEquals(message, expectedUri, entry.getKey());
            assertEquals(message, input.get(expectedUri), entry.getValue());
            index++;
        }
    }
}
