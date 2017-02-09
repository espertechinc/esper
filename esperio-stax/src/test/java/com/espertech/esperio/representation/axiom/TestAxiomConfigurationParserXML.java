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
package com.espertech.esperio.representation.axiom;

import junit.framework.TestCase;

import javax.xml.xpath.XPathConstants;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;

public class TestAxiomConfigurationParserXML extends TestCase {
    public void testConfigureFromStream() throws Exception {
        String fileURL = "regression/esperio-axiom-test-readconfig.xml";
        URL url = this.getClass().getClassLoader().getResource(fileURL);
        if (url == null) {
            throw new RuntimeException("File by url '" + fileURL + "' could not be found in classpath");
        }
        File file = new File(url.getFile());
        String contents = readFile(file);

        ConfigurationEventTypeAxiom type = AxiomConfigurationParserXML.parse(contents);
        assertEquals("MySchemaEvent", type.getRootElementName());
        assertEquals("samples:schemas:simpleSchema", type.getRootElementNamespace());
        assertEquals("default-name-space", type.getDefaultNamespace());
        assertEquals("/myevent/element2", type.getXPathProperties().get("element2").getXpath());
        assertEquals(XPathConstants.STRING, type.getXPathProperties().get("element2").getType());
        assertEquals(Long.class, type.getXPathProperties().get("element2").getOptionalCastToType());
        assertEquals(1, type.getNamespacePrefixes().size());
        assertEquals("samples:schemas:simpleSchema", type.getNamespacePrefixes().get("ss"));
        assertFalse(type.isResolvePropertiesAbsolute());
    }

    private String readFile(File file) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        int x = fis.available();
        byte b[] = new byte[x];
        fis.read(b);
        fis.close();
        return new String(b);
    }
}
