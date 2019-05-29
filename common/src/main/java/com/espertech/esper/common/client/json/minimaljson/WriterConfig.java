/*******************************************************************************
 * Copyright (c) 2015, 2016 EclipseSource.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

/**
 * ATTRIBUTION NOTICE
 * ==================
 * MinimalJson is a fast and minimal JSON parser and writer for Java. It's not an object mapper, but a bare-bones library that aims at being:
 * - minimal: no dependencies, single package with just a few classes, small download size (< 35kB)
 * - fast: high performance comparable with other state-of-the-art parsers (see below)
 * - lightweight: object representation with minimal memory footprint (e.g. no HashMaps)
 * - simple: reading, writing and modifying JSON with minimal code (short names, fluent style)
 *
 * Minimal JSON can be found at https://github.com/ralfstx/minimal-json.
 *
 * Minimal JSON is licensed under the MIT License, see https://github.com/ralfstx/minimal-json/blob/master/LICENSE
 *
 */
package com.espertech.esper.common.client.json.minimaljson;

import java.io.Writer;


/**
 * Controls the formatting of the JSON output. Use one of the available constants.
 */
public abstract class WriterConfig {

    /**
     * Write JSON in its minimal form, without any additional whitespace. This is the default.
     */
    public final static WriterConfig MINIMAL = new WriterConfig() {
        @Override
        public JsonWriter createWriter(Writer writer) {
            return new JsonWriter(writer);
        }
    };

    /**
     * Write JSON in pretty-print, with each value on a separate line and an indentation of two
     * spaces.
     */
    public final static WriterConfig PRETTY_PRINT = PrettyPrint.indentWithSpaces(2);

    public abstract JsonWriter createWriter(Writer writer);

}
