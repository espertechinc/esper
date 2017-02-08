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
package com.espertech.esper.client.soda;

import java.io.StringWriter;
import java.util.List;

/**
 * Abstract base class for streams that can be projected via views providing data window, uniqueness or other projections
 * or deriving further information from streams.
 */
public abstract class ProjectedStream extends Stream {
    private static final long serialVersionUID = -8999968913067920696L;

    private List<View> views;
    private boolean unidirectional;
    private boolean retainUnion;
    private boolean retainIntersection;

    /**
     * Ctor.
     */
    public ProjectedStream() {
    }

    /**
     * Represent as textual.
     *
     * @param writer    to output to
     * @param formatter for newline-whitespace formatting
     */
    public abstract void toEPLProjectedStream(StringWriter writer, EPStatementFormatter formatter);

    /**
     * Represent type as textual non complete.
     *
     * @param writer to output to
     */
    public abstract void toEPLProjectedStreamType(StringWriter writer);

    /**
     * Ctor.
     *
     * @param views         is a list of views upon the stream
     * @param optStreamName is the stream as-name, or null if unnamed
     */
    protected ProjectedStream(List<View> views, String optStreamName) {
        super(optStreamName);
        this.views = views;
    }

    /**
     * Adds an un-parameterized view to the stream.
     *
     * @param namespace is the view namespace, for example "win" for most data windows
     * @param name      is the view name, for example "length" for a length window
     * @return stream
     */
    public ProjectedStream addView(String namespace, String name) {
        views.add(View.create(namespace, name));
        return this;
    }

    /**
     * Adds a parameterized view to the stream.
     *
     * @param namespace  is the view namespace, for example "win" for most data windows
     * @param name       is the view name, for example "length" for a length window
     * @param parameters is a list of view parameters
     * @return stream
     */
    public ProjectedStream addView(String namespace, String name, List<Expression> parameters) {
        views.add(View.create(namespace, name, parameters));
        return this;
    }

    /**
     * Adds a parameterized view to the stream.
     *
     * @param namespace  is the view namespace, for example "win" for most data windows
     * @param name       is the view name, for example "length" for a length window
     * @param parameters is a list of view parameters
     * @return stream
     */
    public ProjectedStream addView(String namespace, String name, Expression... parameters) {
        views.add(View.create(namespace, name, parameters));
        return this;
    }

    /**
     * Adds a parameterized view to the stream.
     *
     * @param name       is the view name, for example "length" for a length window
     * @param parameters is a list of view parameters
     * @return stream
     */
    public ProjectedStream addView(String name, Expression... parameters) {
        views.add(View.create(null, name, parameters));
        return this;
    }

    /**
     * Add a view to the stream.
     *
     * @param view to add
     * @return stream
     */
    public ProjectedStream addView(View view) {
        views.add(view);
        return this;
    }

    /**
     * Returns the list of views added to the stream.
     *
     * @return list of views
     */
    public List<View> getViews() {
        return views;
    }

    /**
     * Sets the list of views onto the stream.
     *
     * @param views list of views
     */
    public void setViews(List<View> views) {
        this.views = views;
    }

    /**
     * Renders the clause in textual representation.
     *
     * @param writer to output to
     */
    public void toEPLStream(StringWriter writer, EPStatementFormatter formatter) {
        toEPLProjectedStream(writer, formatter);
        toEPLViews(writer, views);
    }

    public void toEPLStreamType(StringWriter writer) {
        toEPLProjectedStreamType(writer);

        if ((views != null) && (views.size() != 0)) {
            writer.write('.');
            String delimiter = "";
            for (View view : views) {
                writer.write(delimiter);
                writer.append(view.getNamespace()).append(".").append(view.getName()).append("()");
                delimiter = ".";
            }
        }
    }

    /**
     * Returns true if the stream as unidirectional, for use in unidirectional joins.
     *
     * @return true for unidirectional stream, applicable only for joins
     */
    public boolean isUnidirectional() {
        return unidirectional;
    }

    /**
     * Set to true to indicate that a stream is unidirectional, for use in unidirectional joins.
     *
     * @param isUnidirectional true for unidirectional stream, applicable only for joins
     */
    public void setUnidirectional(boolean isUnidirectional) {
        this.unidirectional = isUnidirectional;
    }

    /**
     * Set to unidirectional.
     *
     * @param isUnidirectional try if unidirectional
     * @return stream
     */
    public ProjectedStream unidirectional(boolean isUnidirectional) {
        this.unidirectional = isUnidirectional;
        return this;
    }

    /**
     * Returns true if multiple data window shall be treated as a union.
     *
     * @return retain union
     */
    public boolean isRetainUnion() {
        return retainUnion;
    }

    /**
     * Set to true to have multiple data window be treated as a union.
     *
     * @param retainUnion indicator to union
     */
    public void setRetainUnion(boolean retainUnion) {
        this.retainUnion = retainUnion;
    }

    /**
     * Returns true if multiple data window shall be treated as an intersection.
     *
     * @return retain intersection
     */
    public boolean isRetainIntersection() {
        return retainIntersection;
    }

    /**
     * Set to true to have multiple data window be treated as a intersection.
     *
     * @param retainIntersection indicator to intersection
     */
    public void setRetainIntersection(boolean retainIntersection) {
        this.retainIntersection = retainIntersection;
    }

    /**
     * Renders the views onto the projected stream.
     *
     * @param writer to render to
     * @param views  to render
     */
    protected static void toEPLViews(StringWriter writer, List<View> views) {
        if ((views != null) && (views.size() != 0)) {
            if (views.iterator().next().getNamespace() == null) {
                writer.write('#');
                String delimiter = "";
                for (View view : views) {
                    writer.write(delimiter);
                    view.toEPLWithHash(writer);
                    delimiter = "#";
                }
            } else {
                writer.write('.');
                String delimiter = "";
                for (View view : views) {
                    writer.write(delimiter);
                    view.toEPL(writer);
                    delimiter = ".";
                }
            }
        }
    }

    public void toEPLStreamOptions(StringWriter writer) {
        if (unidirectional) {
            writer.write(" unidirectional");
        } else if (retainUnion) {
            writer.write(" retain-union");
        } else if (retainIntersection) {
            writer.write(" retain-intersection");
        }
    }
}
