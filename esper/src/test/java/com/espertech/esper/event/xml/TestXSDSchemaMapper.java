/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.event.xml;

import java.net.URL;

import javax.xml.namespace.QName;

import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import com.espertech.esper.util.ResourceLoader;
import com.sun.org.apache.xerces.internal.dom.DOMXSImplementationSourceImpl;
import com.sun.org.apache.xerces.internal.impl.dv.XSSimpleType;
import com.sun.org.apache.xerces.internal.impl.dv.xs.XSSimpleTypeDecl;
import com.sun.org.apache.xerces.internal.xs.XSAttributeUse;
import com.sun.org.apache.xerces.internal.xs.XSComplexTypeDefinition;
import com.sun.org.apache.xerces.internal.xs.XSConstants;
import com.sun.org.apache.xerces.internal.xs.XSElementDeclaration;
import com.sun.org.apache.xerces.internal.xs.XSImplementation;
import com.sun.org.apache.xerces.internal.xs.XSLoader;
import com.sun.org.apache.xerces.internal.xs.XSModel;
import com.sun.org.apache.xerces.internal.xs.XSModelGroup;
import com.sun.org.apache.xerces.internal.xs.XSNamedMap;
import com.sun.org.apache.xerces.internal.xs.XSObject;
import com.sun.org.apache.xerces.internal.xs.XSObjectList;
import com.sun.org.apache.xerces.internal.xs.XSParticle;
import com.sun.org.apache.xerces.internal.xs.XSTypeDefinition;

import junit.framework.TestCase;

public class TestXSDSchemaMapper extends TestCase
{
    public void testMap() throws Exception
    {
        URL url = ResourceLoader.resolveClassPathOrURLResource("schema", "regression/simpleSchema.xsd");
        String schemaUri = url.toURI().toString();

        SchemaModel model = XSDSchemaMapper.loadAndMap(schemaUri, null);
        assertEquals(1, model.getComponents().size());

        SchemaElementComplex component = model.getComponents().get(0);
        assertEquals("simpleEvent", component.getName());
        assertEquals("samples:schemas:simpleSchema", component.getNamespace());
        assertEquals(0, component.getAttributes().size());
        assertEquals(0, component.getSimpleElements().size());
        assertEquals(3, component.getChildren().size());
        assertFalse(component.isArray());
        assertNull(component.getOptionalSimpleType());

        SchemaElementComplex nested1Element = component.getChildren().get(0);
        assertEquals("nested1", nested1Element.getName());
        assertEquals("samples:schemas:simpleSchema", nested1Element.getNamespace());
        assertEquals(1, nested1Element.getAttributes().size());
        assertEquals(2, nested1Element.getSimpleElements().size());
        assertEquals(1, nested1Element.getChildren().size());
        assertFalse(nested1Element.isArray());
        assertNull(nested1Element.getOptionalSimpleType());

        assertEquals("attr1", nested1Element.getAttributes().get(0).getName());
        assertEquals(null, nested1Element.getAttributes().get(0).getNamespace());
        assertEquals(XSSimpleType.PRIMITIVE_STRING, nested1Element.getAttributes().get(0).getXsSimpleType());
        assertEquals("prop1", nested1Element.getSimpleElements().get(0).getName());
        assertEquals(XSSimpleType.PRIMITIVE_STRING, nested1Element.getSimpleElements().get(0).getXsSimpleType());
        assertEquals("prop2", nested1Element.getSimpleElements().get(1).getName());
        assertEquals(XSSimpleType.PRIMITIVE_BOOLEAN, nested1Element.getSimpleElements().get(1).getXsSimpleType());

        SchemaElementComplex nested2Element = nested1Element.getChildren().get(0);
        assertEquals("nested2", nested2Element.getName());
        assertEquals("samples:schemas:simpleSchema", nested2Element.getNamespace());
        assertEquals(0, nested2Element.getAttributes().size());
        assertEquals(1, nested2Element.getSimpleElements().size());
        assertEquals(0, nested2Element.getChildren().size());
        assertFalse(nested2Element.isArray());
        assertNull(nested2Element.getOptionalSimpleType());

        verifySimpleElement(nested2Element.getSimpleElements().get(0), "prop3", XSSimpleType.PRIMITIVE_DECIMAL);

        SchemaElementComplex prop4Element = component.getChildren().get(1);
        assertEquals("prop4", prop4Element.getName());
        assertEquals("samples:schemas:simpleSchema", prop4Element.getNamespace());
        assertEquals(1, prop4Element.getAttributes().size());
        assertEquals(0, prop4Element.getSimpleElements().size());
        assertEquals(0, prop4Element.getChildren().size());
        assertEquals("attr2", prop4Element.getAttributes().get(0).getName());
        assertEquals(XSSimpleType.PRIMITIVE_BOOLEAN, prop4Element.getAttributes().get(0).getXsSimpleType());
        assertFalse(prop4Element.isArray());
        assertEquals(XSSimpleType.PRIMITIVE_STRING, (short) prop4Element.getOptionalSimpleType());

        SchemaElementComplex nested3Element = component.getChildren().get(2);
        assertEquals("nested3", nested3Element.getName());
        assertEquals("samples:schemas:simpleSchema", nested3Element.getNamespace());
        assertEquals(0, nested3Element.getAttributes().size());
        assertEquals(0, nested3Element.getSimpleElements().size());
        assertEquals(1, nested3Element.getChildren().size());
        assertFalse(nested3Element.isArray());
        assertNull(nested3Element.getOptionalSimpleType());

        SchemaElementComplex nested4Element = nested3Element.getChildren().get(0);
        assertEquals("nested4", nested4Element.getName());
        assertEquals("samples:schemas:simpleSchema", nested4Element.getNamespace());
        assertEquals(1, nested4Element.getAttributes().size());
        assertEquals(4, nested4Element.getSimpleElements().size());
        assertEquals(0, nested4Element.getChildren().size());
        assertEquals("id", nested4Element.getAttributes().get(0).getName());
        assertEquals(XSSimpleType.PRIMITIVE_STRING, nested4Element.getAttributes().get(0).getXsSimpleType());
        assertTrue(nested4Element.isArray());
        assertNull(nested4Element.getOptionalSimpleType());

        verifySimpleElement(nested4Element.getSimpleElements().get(0), "prop5", XSSimpleType.PRIMITIVE_STRING);
        verifySimpleElement(nested4Element.getSimpleElements().get(1), "prop6", XSSimpleType.PRIMITIVE_STRING);
        verifySimpleElement(nested4Element.getSimpleElements().get(2), "prop7", XSSimpleType.PRIMITIVE_STRING);
        verifySimpleElement(nested4Element.getSimpleElements().get(3), "prop8", XSSimpleType.PRIMITIVE_STRING);
    }

    public void testEvent() throws Exception
    {
        //URL url = ResourceLoader.resolveClassPathOrURLResource("schema", "regression/typeTestSchema.xsd");
        URL url = ResourceLoader.resolveClassPathOrURLResource("schema", "regression/simpleSchema.xsd");
        String uri = url.toURI().toString();

        DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
        registry.addSource(new DOMXSImplementationSourceImpl());
        XSImplementation impl =(XSImplementation) registry.getDOMImplementation("XS-Loader");
        XSLoader schemaLoader = impl.createXSLoader(null);
        XSModel xsModel = schemaLoader.loadURI(uri);

        XSNamedMap elements = xsModel.getComponents(XSConstants.ELEMENT_DECLARATION);
        for (int i = 0; i < elements.getLength(); i++)
        {
            XSObject object = elements.item(i);
            //System.out.println("name '" + object.getName() + "' namespace '" + object.getNamespace());
        }

        XSElementDeclaration dec = (XSElementDeclaration) elements.item(0);

        XSComplexTypeDefinition complexActualElement = (XSComplexTypeDefinition) dec.getTypeDefinition();
        printSchemaDef(complexActualElement, 2);
    }

    public void testExtendedElements() throws Exception
    {
        URL url = ResourceLoader.resolveClassPathOrURLResource("schema", "regression/schemaWithExtensions.xsd");
        String schemaUri = url.toURI().toString();
        SchemaModel model = XSDSchemaMapper.loadAndMap(schemaUri, null);

        SchemaElementComplex complexEvent = model.getComponents().get(0);
        verifyComplexElement(complexEvent, "complexEvent", XSSimpleType.COMPLEX_TYPE);
        verifySizes(complexEvent, 0, 0, 1);
        
        SchemaElementComplex mainElement = complexEvent.getChildren().get(0);
        verifyComplexElement(mainElement, "mainElement", XSSimpleType.COMPLEX_TYPE);
        verifySizes(mainElement, 0, 0, 4);
        
        SchemaElementComplex baseType4 = mainElement.getChildren().get(0);
        verifyComplexElement(baseType4, "baseType4", XSSimpleType.COMPLEX_TYPE);
        verifySizes(baseType4, 0, 0, 0);
        
        SchemaElementComplex aType2 = mainElement.getChildren().get(1);
        verifyComplexElement(aType2, "aType2", XSSimpleType.COMPLEX_TYPE);
        verifySizes(aType2, 0, 2, 1);
        
        SchemaElementComplex aType3 = mainElement.getChildren().get(2);
        verifyComplexElement(aType3, "aType3", XSSimpleType.COMPLEX_TYPE);
        verifySizes(aType3, 0, 1, 2);
        
        SchemaElementComplex aType3baseType4 = aType3.getChildren().get(0);
        verifyComplexElement(aType3baseType4, "baseType4", XSSimpleType.COMPLEX_TYPE);
        verifySizes(aType3baseType4, 0, 0, 0);
        
        SchemaElementComplex aType3type2 = aType3.getChildren().get(1);
        verifyComplexElement(aType3type2, "aType2", XSSimpleType.COMPLEX_TYPE);
        verifySizes(aType3type2, 0, 2, 1);
        
        SchemaElementComplex aType4 = mainElement.getChildren().get(3);
        verifyComplexElement(aType4, "aType4", XSSimpleType.COMPLEX_TYPE);
        verifySizes(aType4, 0, 0, 1);
     }

    private void printSchemaDef(XSComplexTypeDefinition complexActualElement, int indent)
    {
        XSObjectList attrs = complexActualElement.getAttributeUses();
        for(int i=0;i<attrs.getLength();i++)
        {
            XSAttributeUse attr = (XSAttributeUse)attrs.item(i);
            String name = attr.getAttrDeclaration().getName();
            QName qname = SchemaUtil.simpleTypeToQName(((XSSimpleTypeDecl) attr.getAttrDeclaration().getTypeDefinition()).getPrimitiveKind());
            //System.out.println(indent(indent) + "Attribute " + name + " qname " + qname.getLocalPart());
        }

        if ((complexActualElement.getContentType() == XSComplexTypeDefinition.CONTENTTYPE_ELEMENT) ||
            (complexActualElement.getContentType() == XSComplexTypeDefinition.CONTENTTYPE_MIXED))
        {
            // has children
            XSParticle particle = complexActualElement.getParticle();
            if (particle.getTerm() instanceof XSModelGroup )
            {
                XSModelGroup group = (XSModelGroup)particle.getTerm();
                XSObjectList particles = group.getParticles();
                for (int i=0;i<particles.getLength();i++)
                {
                    XSParticle childParticle = (XSParticle)particles.item(i);

                    if (childParticle.getTerm() instanceof XSElementDeclaration)
                    {
                        XSElementDeclaration decl = (XSElementDeclaration) childParticle.getTerm();

                        if (decl.getTypeDefinition().getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) {
                            XSSimpleTypeDecl simpleTypeDecl = (XSSimpleTypeDecl) decl.getTypeDefinition();
                            QName type = SchemaUtil.simpleTypeToQName(simpleTypeDecl.getPrimitiveKind());
                            //System.out.println(indent(indent) + "Simple particle " + childParticle.getTerm().getName() + " type " + type.getLocalPart() + " " + print(childParticle));
                        }

                        if (decl.getTypeDefinition().getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE)
                        {
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

    private String print(XSParticle particle)
    {
        String text = " min " + particle.getMinOccurs();
        if (particle.getMaxOccursUnbounded())
        {
            text += " unbounded";
        }
        else
        {
            text += " max " + particle.getMaxOccurs();
        }
        return text;
    }

    private String indent(int count)
    {
        return String.format("%" + count + "s", "");
    }
    

    private static void verifySimpleElement(SchemaElementSimple element, String name, short type){
        assertEquals(type, element.getXsSimpleType());
        verifyElement(element, name);
    }

    private static void verifyComplexElement(SchemaElementComplex element, String name, short type){
        assertNull(element.getOptionalSimpleType());
        verifyElement(element, name);
    }

    private static void verifyElement(SchemaElement element, String name){
        assertEquals(name, element.getName());
        assertEquals("samples:schemas:simpleSchema", element.getNamespace());
    }

    private static void verifySizes(SchemaElementComplex element, int expectedNumberOfAttributes, int expectedNumberOfSimpleElements, int expectedNumberOfChildren){
        assertEquals(expectedNumberOfAttributes, element.getAttributes().size());
        assertEquals(expectedNumberOfSimpleElements, element.getSimpleElements().size());
        assertEquals(expectedNumberOfChildren, element.getChildren().size());
    }
}
