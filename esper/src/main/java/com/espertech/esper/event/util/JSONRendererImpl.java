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
package com.espertech.esper.event.util;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.util.EventPropertyRenderer;
import com.espertech.esper.client.util.EventPropertyRendererContext;
import com.espertech.esper.client.util.JSONEventRenderer;
import com.espertech.esper.client.util.JSONRenderingOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

/**
 * Render for the JSON format.
 */
public class JSONRendererImpl implements JSONEventRenderer {
    private static final Logger log = LoggerFactory.getLogger(JSONRendererImpl.class);

    private final static String NEWLINE = System.getProperty("line.separator");
    private final static String COMMA_DELIMITER_NEWLINE = "," + NEWLINE;

    private final RendererMeta meta;
    private final RendererMetaOptions rendererOptions;

    /**
     * Ctor.
     *
     * @param eventType type of event(s)
     * @param options   rendering options
     */
    public JSONRendererImpl(EventType eventType, JSONRenderingOptions options) {
        EventPropertyRenderer propertyRenderer = null;
        EventPropertyRendererContext propertyRendererContext = null;
        if (options.getRenderer() != null) {
            propertyRenderer = options.getRenderer();
            propertyRendererContext = new EventPropertyRendererContext(eventType, true);
        }

        rendererOptions = new RendererMetaOptions(options.isPreventLooping(), false, propertyRenderer, propertyRendererContext);
        meta = new RendererMeta(eventType, new Stack<EventTypePropertyPair>(), rendererOptions);
    }

    public String render(String title, EventBean theEvent) {
        StringBuilder buf = new StringBuilder();
        buf.append('{');
        buf.append(NEWLINE);

        ident(buf, 1);
        buf.append('\"');
        buf.append(title);
        buf.append("\": {");
        buf.append(NEWLINE);

        recursiveRender(theEvent, buf, 2, meta, rendererOptions);

        ident(buf, 1);
        buf.append('}');
        buf.append(NEWLINE);

        buf.append('}');
        buf.append(NEWLINE);

        return buf.toString();
    }

    public String render(EventBean theEvent) {
        StringBuilder buf = new StringBuilder();
        buf.append('{');
        recursiveRender(theEvent, buf, 2, meta, rendererOptions);
        buf.append('}');
        return buf.toString();
    }

    private static void ident(StringBuilder buf, int level) {
        for (int i = 0; i < level; i++) {
            indentChar(buf);
        }
    }

    private static void indentChar(StringBuilder buf) {
        buf.append(' ');
        buf.append(' ');
    }

    private static void recursiveRender(EventBean theEvent, StringBuilder buf, int level, RendererMeta meta, RendererMetaOptions rendererOptions) {
        String delimiter = "";

        // simple properties
        GetterPair[] simpleProps = meta.getSimpleProperties();
        if (rendererOptions.getRenderer() == null) {
            for (GetterPair simpleProp : simpleProps) {
                Object value = simpleProp.getGetter().get(theEvent);
                writeDelimitedIndentedProp(buf, delimiter, level, simpleProp.getName());
                simpleProp.getOutput().render(value, buf);
                delimiter = COMMA_DELIMITER_NEWLINE;
            }
        } else {
            EventPropertyRendererContext context = rendererOptions.getRendererContext();
            context.setStringBuilderAndReset(buf);
            for (GetterPair simpleProp : simpleProps) {
                Object value = simpleProp.getGetter().get(theEvent);
                writeDelimitedIndentedProp(buf, delimiter, level, simpleProp.getName());
                context.setDefaultRenderer(simpleProp.getOutput());
                context.setPropertyName(simpleProp.getName());
                context.setPropertyValue(value);
                rendererOptions.getRenderer().render(context);
                delimiter = COMMA_DELIMITER_NEWLINE;
            }
        }

        GetterPair[] indexProps = meta.getIndexProperties();
        for (GetterPair indexProp : indexProps) {
            Object value = indexProp.getGetter().get(theEvent);
            writeDelimitedIndentedProp(buf, delimiter, level, indexProp.getName());

            if (value == null) {
                buf.append("null");
            } else {
                if (!value.getClass().isArray()) {
                    buf.append("[]");
                } else {
                    buf.append('[');
                    String arrayDelimiter = "";

                    if (rendererOptions.getRenderer() == null) {
                        for (int i = 0; i < Array.getLength(value); i++) {
                            Object arrayItem = Array.get(value, i);
                            buf.append(arrayDelimiter);
                            indexProp.getOutput().render(arrayItem, buf);
                            arrayDelimiter = ", ";
                        }
                    } else {
                        EventPropertyRendererContext context = rendererOptions.getRendererContext();
                        context.setStringBuilderAndReset(buf);
                        for (int i = 0; i < Array.getLength(value); i++) {
                            Object arrayItem = Array.get(value, i);
                            buf.append(arrayDelimiter);
                            context.setPropertyName(indexProp.getName());
                            context.setPropertyValue(arrayItem);
                            context.setIndexedPropertyIndex(i);
                            context.setDefaultRenderer(indexProp.getOutput());
                            rendererOptions.getRenderer().render(context);
                            arrayDelimiter = ", ";
                        }
                    }
                    buf.append(']');
                }
            }
            delimiter = COMMA_DELIMITER_NEWLINE;
        }

        GetterPair[] mappedProps = meta.getMappedProperties();
        for (GetterPair mappedProp : mappedProps) {
            Object value = mappedProp.getGetter().get(theEvent);

            if ((value != null) && (!(value instanceof Map))) {
                log.warn("Property '" + mappedProp.getName() + "' expected to return Map and returned " + value.getClass() + " instead");
                continue;
            }

            writeDelimitedIndentedProp(buf, delimiter, level, mappedProp.getName());

            if (value == null) {
                buf.append("null");
                buf.append(NEWLINE);
            } else {
                Map<String, Object> map = (Map<String, Object>) value;
                if (map.isEmpty()) {
                    buf.append("{}");
                    buf.append(NEWLINE);
                } else {
                    buf.append('{');
                    buf.append(NEWLINE);

                    String localDelimiter = "";
                    Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
                    for (; it.hasNext(); ) {
                        Map.Entry<String, Object> entry = it.next();
                        if (entry.getKey() == null) {
                            continue;
                        }

                        buf.append(localDelimiter);
                        ident(buf, level + 1);
                        buf.append('\"');
                        buf.append(entry.getKey());
                        buf.append("\": ");

                        if (entry.getValue() == null) {
                            buf.append("null");
                        } else {
                            OutputValueRenderer outRenderer = OutputValueRendererFactory.getOutputValueRenderer(entry.getValue().getClass(), rendererOptions);
                            if (rendererOptions.getRenderer() == null) {
                                outRenderer.render(entry.getValue(), buf);
                            } else {
                                EventPropertyRendererContext context = rendererOptions.getRendererContext();
                                context.setStringBuilderAndReset(buf);
                                context.setPropertyName(mappedProp.getName());
                                context.setPropertyValue(entry.getValue());
                                context.setMappedPropertyKey(entry.getKey());
                                context.setDefaultRenderer(outRenderer);
                                rendererOptions.getRenderer().render(context);
                            }
                        }
                        localDelimiter = COMMA_DELIMITER_NEWLINE;
                    }

                    buf.append(NEWLINE);
                    ident(buf, level);
                    buf.append('}');
                }
            }

            delimiter = COMMA_DELIMITER_NEWLINE;
        }

        NestedGetterPair[] nestedProps = meta.getNestedProperties();
        for (NestedGetterPair nestedProp : nestedProps) {
            Object value = nestedProp.getGetter().getFragment(theEvent);

            writeDelimitedIndentedProp(buf, delimiter, level, nestedProp.getName());

            if (value == null) {
                buf.append("null");
            } else if (!nestedProp.isArray()) {
                if (!(value instanceof EventBean)) {
                    log.warn("Property '" + nestedProp.getName() + "' expected to return EventBean and returned " + value.getClass() + " instead");
                    buf.append("null");
                    continue;
                }
                EventBean nestedEventBean = (EventBean) value;
                buf.append('{');
                buf.append(NEWLINE);

                recursiveRender(nestedEventBean, buf, level + 1, nestedProp.getMetadata(), rendererOptions);

                ident(buf, level);
                buf.append('}');
            } else {
                if (!(value instanceof EventBean[])) {
                    log.warn("Property '" + nestedProp.getName() + "' expected to return EventBean[] and returned " + value.getClass() + " instead");
                    buf.append("null");
                    continue;
                }


                StringBuilder arrayDelimiterBuf = new StringBuilder();
                arrayDelimiterBuf.append(',');
                arrayDelimiterBuf.append(NEWLINE);
                ident(arrayDelimiterBuf, level + 1);

                EventBean[] nestedEventArray = (EventBean[]) value;
                String arrayDelimiter = "";
                buf.append('[');

                for (int i = 0; i < nestedEventArray.length; i++) {
                    EventBean arrayItem = nestedEventArray[i];
                    buf.append(arrayDelimiter);
                    arrayDelimiter = arrayDelimiterBuf.toString();

                    buf.append('{');
                    buf.append(NEWLINE);

                    recursiveRender(arrayItem, buf, level + 2, nestedProp.getMetadata(), rendererOptions);

                    ident(buf, level + 1);
                    buf.append('}');
                }
                buf.append(']');
            }
            delimiter = COMMA_DELIMITER_NEWLINE;
        }

        buf.append(NEWLINE);
    }

    private static void writeDelimitedIndentedProp(StringBuilder buf, String delimiter, int level, String name) {
        buf.append(delimiter);
        ident(buf, level);
        buf.append('\"');
        buf.append(name);
        buf.append("\": ");
    }
}
