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
package com.espertech.esper.event.xml;

import com.espertech.esper.core.support.SupportEngineImportServiceFactory;
import com.espertech.esper.util.ResourceLoader;
import com.sun.org.apache.xerces.internal.dom.DOMXSImplementationSourceImpl;
import com.sun.org.apache.xerces.internal.impl.dv.XSSimpleType;
import com.sun.org.apache.xerces.internal.impl.dv.xs.XSSimpleTypeDecl;
import com.sun.org.apache.xerces.internal.xs.*;
import junit.framework.TestCase;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import javax.xml.namespace.QName;
import java.net.URL;

public class TestXSDSchemaMapper extends TestCase {
    public void testMap() throws Exception {
        URL url = ResourceLoader.resolveClassPathOrURLResource("schema", "regression/simpleSchema.xsd", Thread.currentThread().getContextClassLoader());
        String schemaUri = url.toURI().toString();

        SchemaModel model = XSDSchemaMapper.loadAndMap(schemaUri, null, SupportEngineImportServiceFactory.make());
        assertEquals(1, model.getComponents().size());

        SchemaElementComplex simpleEvent = model.getComponents().get(0);
        verifyComplexElement(simpleEvent, "simpleEvent", XSSimpleType.COMPLEX_TYPE, false);
        verifySizes(simpleEvent, 0, 0, 3);

        SchemaElementComplex nested1 = simpleEvent.getChildren().get(0);
        verifyComplexElement(nested1, "nested1", XSSimpleType.COMPLEX_TYPE, false);
        verifySizes(nested1, 1, 2, 1);
        assertEquals("attr1", nested1.getAttributes().get(0).getName());
        assertEquals(null, nested1.getAttributes().get(0).getNamespace());
        assertEquals(XSSimpleType.PRIMITIVE_STRING, nested1.getAttributes().get(0).getXsSimpleType());
        assertEquals("prop1", nested1.getSimpleElements().get(0).getName());
        assertEquals(XSSimpleType.PRIMITIVE_STRING, nested1.getSimpleElements().get(0).getXsSimpleType());
        assertEquals("prop2", nested1.getSimpleElements().get(1).getName());
        assertEquals(XSSimpleType.PRIMITIVE_BOOLEAN, nested1.getSimpleElements().get(1).getXsSimpleType());

        SchemaElementComplex nested2 = nested1.getChildren().get(0);
        verifyComplexElement(nested2, "nested2", XSSimpleType.COMPLEX_TYPE, false);
        verifySizes(nested2, 0, 1, 0);
        verifySimpleElement(nested2.getSimpleElements().get(0), "prop3", XSSimpleType.PRIMITIVE_DECIMAL);

        SchemaElementComplex prop4 = simpleEvent.getChildren().get(1);
        verifyElement(prop4, "prop4");
        verifySizes(prop4, 1, 0, 0);
        assertEquals("attr2", prop4.getAttributes().get(0).getName());
        assertEquals(XSSimpleType.PRIMITIVE_BOOLEAN, prop4.getAttributes().get(0).getXsSimpleType());
        assertEquals(XSSimpleType.PRIMITIVE_STRING, (short) prop4.getOptionalSimpleType());

        SchemaElementComplex nested3 = simpleEvent.getChildren().get(2);
        verifyComplexElement(nested3, "nested3", XSSimpleType.COMPLEX_TYPE, false);
        verifySizes(nested3, 0, 0, 1);

        SchemaElementComplex nested4 = nested3.getChildren().get(0);
        verifyComplexElement(nested4, "nested4", XSSimpleType.COMPLEX_TYPE, true);
        verifySizes(nested4, 1, 4, 0);
        assertEquals("id", nested4.getAttributes().get(0).getName());
        assertEquals(XSSimpleType.PRIMITIVE_STRING, nested4.getAttributes().get(0).getXsSimpleType());
        verifySimpleElement(nested4.getSimpleElements().get(0), "prop5", XSSimpleType.PRIMITIVE_STRING);
        verifySimpleElement(nested4.getSimpleElements().get(1), "prop6", XSSimpleType.PRIMITIVE_STRING);
        verifySimpleElement(nested4.getSimpleElements().get(2), "prop7", XSSimpleType.PRIMITIVE_STRING);
        verifySimpleElement(nested4.getSimpleElements().get(3), "prop8", XSSimpleType.PRIMITIVE_STRING);
    }

    public void testEvent() throws Exception {
        //URL url = ResourceLoader.resolveClassPathOrURLResource("schema", "regression/typeTestSchema.xsd");
        URL url = ResourceLoader.resolveClassPathOrURLResource("schema", "regression/simpleSchema.xsd", this.getClass().getClassLoader());
        String uri = url.toURI().toString();

        DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
        registry.addSource(new DOMXSImplementationSourceImpl());
        XSImplementation impl = (XSImplementation) registry.getDOMImplementation("XS-Loader");
        XSLoader schemaLoader = impl.createXSLoader(null);
        XSModel xsModel = schemaLoader.loadURI(uri);

        XSNamedMap elements = xsModel.getComponents(XSConstants.ELEMENT_DECLARATION);
        for (int i = 0; i < elements.getLength(); i++) {
            XSObject object = elements.item(i);
            //System.out.println("name '" + object.getName() + "' namespace '" + object.getNamespace());
        }

        XSElementDeclaration dec = (XSElementDeclaration) elements.item(0);

        XSComplexTypeDefinition complexActualElement = (XSComplexTypeDefinition) dec.getTypeDefinition();
        printSchemaDef(complexActualElement, 2);
    }

    public void testExtendedElements() throws Exception {
        URL url = ResourceLoader.resolveClassPathOrURLResource("schema", "regression/schemaWithExtensions.xsd", this.getClass().getClassLoader());
        String schemaUri = url.toURI().toString();
        SchemaModel model = XSDSchemaMapper.loadAndMap(schemaUri, null, SupportEngineImportServiceFactory.make());

        SchemaElementComplex complexEvent = model.getComponents().get(0);
        verifyComplexElement(complexEvent, "complexEvent", XSSimpleType.COMPLEX_TYPE, false);
        verifySizes(complexEvent, 0, 0, 1);

        SchemaElementComplex mainElement = complexEvent.getChildren().get(0);
        verifyComplexElement(mainElement, "mainElement", XSSimpleType.COMPLEX_TYPE, false);
        verifySizes(mainElement, 0, 0, 4);

        SchemaElementComplex baseType4 = mainElement.getChildren().get(0);
        verifyComplexElement(baseType4, "baseType4", XSSimpleType.COMPLEX_TYPE, false);
        verifySizes(baseType4, 0, 0, 0);

        SchemaElementComplex aType2 = mainElement.getChildren().get(1);
        verifyComplexElement(aType2, "aType2", XSSimpleType.COMPLEX_TYPE, false);
        verifySizes(aType2, 0, 2, 1);

        SchemaElementComplex aType3 = mainElement.getChildren().get(2);
        verifyComplexElement(aType3, "aType3", XSSimpleType.COMPLEX_TYPE, false);
        verifySizes(aType3, 0, 1, 2);

        SchemaElementComplex aType3baseType4 = aType3.getChildren().get(0);
        verifyComplexElement(aType3baseType4, "baseType4", XSSimpleType.COMPLEX_TYPE, false);
        verifySizes(aType3baseType4, 0, 0, 0);

        SchemaElementComplex aType3type2 = aType3.getChildren().get(1);
        verifyComplexElement(aType3type2, "aType2", XSSimpleType.COMPLEX_TYPE, false);
        verifySizes(aType3type2, 0, 2, 1);

        SchemaElementComplex aType4 = mainElement.getChildren().get(3);
        verifyComplexElement(aType4, "aType4", XSSimpleType.COMPLEX_TYPE, false);
        verifySizes(aType4, 0, 0, 1);
    }

    private void printSchemaDef(XSComplexTypeDefinition complexActualElement, int indent) {
        XSObjectList attrs = complexActualElement.getAttributeUses();
        for (int i = 0; i < attrs.getLength(); i++) {
            XSAttributeUse attr = (XSAttributeUse) attrs.item(i);
            String name = attr.getAttrDeclaration().getName();
            QName qname = SchemaUtil.simpleTypeToQName(((XSSimpleTypeDecl) attr.getAttrDeclaration().getTypeDefinition()).getPrimitiveKind());
            //System.out.println(indent(indent) + "Attribute " + name + " qname " + qname.getLocalPart());
        }

        if ((complexActualElement.getContentType() == XSComplexTypeDefinition.CONTENTTYPE_ELEMENT) ||
                (complexActualElement.getContentType() == XSComplexTypeDefinition.CONTENTTYPE_MIXED)) {
            // has children
            XSParticle particle = complexActualElement.getParticle();
            if (particle.getTerm() instanceof XSModelGroup) {
                XSModelGroup group = (XSModelGroup) particle.getTerm();
                XSObjectList particles = group.getParticles();
                for (int i = 0; i < particles.getLength(); i++) {
                    XSParticle childParticle = (XSParticle) particles.item(i);

                    if (childParticle.getTerm() instanceof XSElementDeclaration) {
                        XSElementDeclaration decl = (XSElementDeclaration) childParticle.getTerm();

                        if (decl.getTypeDefinition().getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) {
                            XSSimpleTypeDecl simpleTypeDecl = (XSSimpleTypeDecl) decl.getTypeDefinition();
                            QName type = SchemaUtil.simpleTypeToQName(simpleTypeDecl.getPrimitiveKind());
                            //System.out.println(indent(indent) + "Simple particle " + childParticle.getTerm().getName() + " type " + type.getLocalPart() + " " + print(childParticle));
                        }

                        if (decl.getTypeDefinition().getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
                            //System.out.println(indent(indent) + "Complex particle " + childParticle.getTerm().getName()  + " " + print(childParticle));
                            complexActualElement = (XSComplexTypeDefinition) decl.getTypeDefinition();

                            if (complexActualElement.getSimpleType() != null) {
                                XSSimpleTypeDecl simpleTypeDecl = (XSSimpleTypeDecl) complexActualElement.getSimpleType();
                                QName type = SchemaUtil.simpleTypeToQName(simpleTypeDecl.getPrimitiveKind());
                                //System.out.println(indent(indent+1) + "Simple type" + childParticle.getTerm().getName() + " type " + type.getLocalPart() + " " + print(childParticle));
                            }

                            printSchemaDef(complexActualElement, indent + 4);
                        }
                    }
                }
            }
        }
    }

    private String print(XSParticle particle) {
        String text = " min " + particle.getMinOccurs();
        if (particle.getMaxOccursUnbounded()) {
            text += " unbounded";
        } else {
            text += " max " + particle.getMaxOccurs();
        }
        return text;
    }

    private String indent(int count) {
        return String.format("%" + count + "s", "");
    }


    private static void verifySimpleElement(SchemaElementSimple element, String name, short type) {
        assertEquals(type, element.getXsSimpleType());
        verifyElement(element, name);
    }

    private static void verifyComplexElement(SchemaElementComplex element, String name, short type, boolean isArray) {
        assertNull(element.getOptionalSimpleType());
        assertEquals(isArray, element.isArray());
        verifyElement(element, name);
    }

    private static void verifyElement(SchemaElement element, String name) {
        assertEquals(name, element.getName());
        assertEquals("samples:schemas:simpleSchema", element.getNamespace());
    }

    private static void verifySizes(SchemaElementComplex element, int expectedNumberOfAttributes, int expectedNumberOfSimpleElements, int expectedNumberOfChildren) {
        assertEquals(expectedNumberOfAttributes, element.getAttributes().size());
        assertEquals(expectedNumberOfSimpleElements, element.getSimpleElements().size());
        assertEquals(expectedNumberOfChildren, element.getChildren().size());
    }
}
