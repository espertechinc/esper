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
package com.espertech.esper.supportregression.event;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EventSender;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

public class SupportXML {
    private static final String XML =
            "<simpleEvent xmlns=\"samples:schemas:simpleSchema\" xmlns:ss=\"samples:schemas:simpleSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"samples:schemas:simpleSchema\n" +
                    "simpleSchema.xsd\">\n" +
                    "\t<nested1 attr1=\"SAMPLE_ATTR1\">\n" +
                    "\t\t<prop1>SAMPLE_V1</prop1>\n" +
                    "\t\t<prop2>true</prop2>\n" +
                    "\t\t<nested2>\n" +
                    "\t\t\t<prop3>3</prop3>\n" +
                    "\t\t\t<prop3>4</prop3>\n" +
                    "\t\t\t<prop3>5</prop3>\n" +
                    "\t\t</nested2>\n" +
                    "\t</nested1>\n" +
                    "\t<prop4 ss:attr2=\"true\">SAMPLE_V6</prop4>\n" +
                    "\t<nested3>\n" +
                    "\t\t<nested4 id=\"a\">\n" +
                    "\t\t\t<prop5>SAMPLE_V7</prop5>\n" +
                    "\t\t\t<prop5>SAMPLE_V8</prop5>\n" +
                    "\t\t</nested4>\n" +
                    "\t\t<nested4 id=\"b\">\n" +
                    "\t\t\t<prop5>SAMPLE_V9</prop5>\n" +
                    "\t\t</nested4>\n" +
                    "\t\t<nested4 id=\"c\">\n" +
                    "\t\t\t<prop5>SAMPLE_V10</prop5>\n" +
                    "\t\t\t<prop5>SAMPLE_V11</prop5>\n" +
                    "\t\t</nested4>\n" +
                    "\t</nested3>\n" +
                    "</simpleEvent>";

    public static Document sendDefaultEvent(EPRuntime runtime, String value) throws Exception {
        String xml = XML.replaceAll("VAL1", value);

        InputSource source = new InputSource(new StringReader(xml));
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        Document simpleDoc = builderFactory.newDocumentBuilder().parse(source);

        runtime.sendEvent(simpleDoc);

        return simpleDoc;
    }

    public static Document sendEvent(EventSender sender, String xml) throws Exception {
        Document simpleDoc = getDocument(xml);
        sender.sendEvent(simpleDoc);
        return simpleDoc;
    }

    public static Document getDocument() throws Exception {
        return getDocument(XML);
    }

    public static Document sendEvent(EPRuntime runtime, String xml) throws Exception {
        Document simpleDoc = getDocument(xml);
        runtime.sendEvent(simpleDoc);
        return simpleDoc;
    }

    public static Document getDocument(String xml) throws Exception {
        StringReader reader = new StringReader(xml);
        InputSource source = new InputSource(reader);
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        return builderFactory.newDocumentBuilder().parse(source);
    }

    public static Document getDocument(InputStream stream) throws EPException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;

        Document document = null;

        try {
            builder = factory.newDocumentBuilder();
            document = builder.parse(stream);
        } catch (ParserConfigurationException ex) {
            throw new EPException("Could not get a DOM parser", ex);
        } catch (SAXException ex) {
            throw new EPException("Could not parse", ex);
        } catch (IOException ex) {
            throw new EPException("Could not read", ex);
        } finally {
            try {
                stream.close();
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }

        return document;
    }

    public static String serialize(Document doc) throws TransformerException {
        javax.xml.transform.TransformerFactory transformerFactory =
                javax.xml.transform.TransformerFactory.newInstance();
        javax.xml.transform.Transformer transformer =
                transformerFactory.newTransformer();

        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        java.io.StringWriter xmlout = new java.io.StringWriter();
        javax.xml.transform.stream.StreamResult result = new
                javax.xml.transform.stream.StreamResult(xmlout);
        transformer.transform(new javax.xml.transform.dom.DOMSource(doc), result);
        return xmlout.getBuffer().toString();
    }
}
