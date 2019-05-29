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

/**
 * Represents a member of a JSON object, a pair of a name and a value.
 */
public class Member {

    final String name;
    final JsonValue value;

    /**
     * Ctor.
     * @param name name
     * @param value value
     */
    public Member(String name, JsonValue value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Returns the name of this member.
     *
     * @return the name of this member, never <code>null</code>
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the value of this member.
     *
     * @return the value of this member, never <code>null</code>
     */
    public JsonValue getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + name.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }

    /**
     * Indicates whether a given object is "equal to" this JsonObject. An object is considered equal
     * if it is also a <code>JsonObject</code> and both objects contain the same members <em>in
     * the same order</em>.
     * <p>
     * If two JsonObjects are equal, they will also produce the same JSON output.
     * </p>
     *
     * @param object the object to be compared with this JsonObject
     * @return <tt>true</tt> if the specified object is equal to this JsonObject, <code>false</code>
     * otherwise
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null) {
            return false;
        }
        if (getClass() != object.getClass()) {
            return false;
        }
        Member other = (Member) object;
        return name.equals(other.name) && value.equals(other.value);
    }

}
