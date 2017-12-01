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

import com.espertech.esper.client.ConfigurationException;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.util.FileUtil;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.ResourceLoader;
import com.sun.org.apache.xerces.internal.dom.DOMXSImplementationSourceImpl;
import com.sun.org.apache.xerces.internal.impl.dv.XSSimpleType;
import com.sun.org.apache.xerces.internal.impl.dv.xs.XSSimpleTypeDecl;
import com.sun.org.apache.xerces.internal.xs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.LSInput;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for mapping a XSD schema model to an internal representation.
 */
public class XSDSchemaMapper {
    private static final Logger log = LoggerFactory.getLogger(XSDSchemaMapper.class);

    private static final int JAVA5_COMPLEX_TYPE = 13;
    private static final int JAVA5_SIMPLE_TYPE = 14;
    private static final int JAVA6_COMPLEX_TYPE = 15;
    private static final int JAVA6_SIMPLE_TYPE = 16;

    /**
     * Loading and mapping of the schema to the internal representation.
     *
     * @param schemaResource schema to load and map.
     * @param schemaText     schema
     * @param engineImportService engine imports
     * @return model
     */
    public static SchemaModel loadAndMap(String schemaResource, String schemaText, EngineImportService engineImportService) {
        // Load schema
        XSModel model;
        try {
            model = readSchemaInternal(schemaResource, schemaText, engineImportService);
        } catch (ConfigurationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ConfigurationException("Failed to read schema '" + schemaResource + "' : " + ex.getMessage(), ex);
        }

        // Map schema to internal representation
        return map(model);
    }

    private static XSModel readSchemaInternal(String schemaResource, String schemaText, EngineImportService engineImportService) throws IllegalAccessException, InstantiationException, ClassNotFoundException,
            ConfigurationException, URISyntaxException {
        LSInputImpl input = null;
        String baseURI = null;
        URL url = null;
        if (schemaResource != null) {
            try {
                url = ResourceLoader.resolveClassPathOrURLResource("schema", schemaResource, engineImportService.getClassLoader());
            } catch (FileNotFoundException e) {
                throw new ConfigurationException(e.getMessage(), e);
            }
            baseURI = url.toURI().toString();
        } else {
            input = new LSInputImpl(schemaText);
        }

        // Uses Xerxes internal classes
        DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
        registry.addSource(new DOMXSImplementationSourceImpl());
        Object xsImplementation = registry.getDOMImplementation("XS-Loader");
        if (xsImplementation == null) {
            throw new ConfigurationException("Failed to retrieve XS-Loader implementation from registry obtained via DOMImplementationRegistry.newInstance, please check that registry.getDOMImplementation(\"XS-Loader\") returns an instance");
        }
        if (!JavaClassHelper.isImplementsInterface(xsImplementation.getClass(), XSImplementation.class)) {
            String message = "The XS-Loader instance returned by the DOM registry class '" + xsImplementation.getClass().getName() + "' does not implement the interface '" + XSImplementation.class.getName() + "'; If you have a another Xerces distribution in your classpath please ensure the classpath order loads the JRE Xerces distribution or set the DOMImplementationRegistry.PROPERTY system property";
            throw new ConfigurationException(message);
        }
        XSImplementation impl = (XSImplementation) xsImplementation;
        XSLoader schemaLoader = impl.createXSLoader(null);
        schemaLoader.getConfig().setParameter("error-handler", new XSDSchemaMapperErrorHandler(schemaResource));
        XSModel xsModel;

        if (input != null) {
            xsModel = schemaLoader.load(input);
        } else {
            xsModel = schemaLoader.loadURI(baseURI);

            // If having trouble loading from the uri, try to attempt from file system.
            if (xsModel == null) {
                String schema;
                try {
                    schema = FileUtil.readTextFile(new File(url.toURI()));
                } catch (IOException e) {
                    throw new ConfigurationException("Failed to read file '" + url.toURI() + "':" + e.getMessage(), e);
                }

                log.debug("Found and obtained schema: " + schema);
                xsModel = schemaLoader.load(new LSInputImpl(schema));
                log.debug("Model for schema: " + xsModel);
            }
        }

        if (xsModel == null) {
            throw new ConfigurationException("Failed to read schema via URL '" + schemaResource + '\'');
        }

        return xsModel;
    }

    private static SchemaModel map(XSModel xsModel) {
        // get namespaces
        StringList namespaces = xsModel.getNamespaces();
        List<String> namesspaceList = new ArrayList<String>();
        for (int i = 0; i < namespaces.getLength(); i++) {
            namesspaceList.add(namespaces.item(i));
        }

        // get top-level complex elements
        XSNamedMap elements = xsModel.getComponents(XSConstants.ELEMENT_DECLARATION);
        List<SchemaElementComplex> components = new ArrayList<SchemaElementComplex>();

        for (int i = 0; i < elements.getLength(); i++) {
            XSObject object = elements.item(i);
            if (!(object instanceof XSElementDeclaration)) {
                continue;
            }

            XSElementDeclaration decl = (XSElementDeclaration) elements.item(i);
            if (!isComplexTypeCategory(decl.getTypeDefinition().getTypeCategory())) {
                continue;
            }

            XSComplexTypeDefinition complexActualElement = (XSComplexTypeDefinition) decl.getTypeDefinition();
            String name = object.getName();
            String namespace = object.getNamespace();

            ElementPathNode rootNode = new ElementPathNode(null, name);

            if (log.isDebugEnabled()) {
                log.debug("Processing component " + namespace + " " + name);
            }

            SchemaElementComplex complexElement = process(name, namespace, complexActualElement, false, rootNode);

            if (log.isDebugEnabled()) {
                log.debug("Adding component " + namespace + " " + name);
            }
            components.add(complexElement);
        }

        return new SchemaModel(components, namesspaceList);
    }

    private static boolean isComplexTypeCategory(short typeCategory) {
        return (typeCategory == XSTypeDefinition.COMPLEX_TYPE) || (typeCategory == JAVA5_COMPLEX_TYPE) || (typeCategory == JAVA6_COMPLEX_TYPE);
    }

    private static boolean isSimpleTypeCategory(short typeCategory) {
        return (typeCategory == XSTypeDefinition.SIMPLE_TYPE) || (typeCategory == JAVA5_SIMPLE_TYPE) || (typeCategory == JAVA6_SIMPLE_TYPE);
    }

    private static SchemaElementComplex process(String complexElementName, String complexElementNamespace, XSComplexTypeDefinition complexActualElement, boolean isArray, ElementPathNode node) {
        if (log.isDebugEnabled()) {
            log.debug("Processing complex " + complexElementNamespace + " " + complexElementName + " stack " + node.toString());
        }

        List<SchemaItemAttribute> attributes = new ArrayList<SchemaItemAttribute>();
        List<SchemaElementSimple> simpleElements = new ArrayList<SchemaElementSimple>();
        List<SchemaElementComplex> complexElements = new ArrayList<SchemaElementComplex>();

        Short optionalSimplyType = null;
        String optionalSimplyTypeName = null;
        if (complexActualElement.getSimpleType() != null) {
            XSSimpleTypeDecl simpleType = (XSSimpleTypeDecl) complexActualElement.getSimpleType();
            optionalSimplyType = simpleType.getPrimitiveKind();
            optionalSimplyTypeName = simpleType.getName();
        }

        SchemaElementComplex complexElement = new SchemaElementComplex(complexElementName, complexElementNamespace, attributes, complexElements, simpleElements, isArray, optionalSimplyType, optionalSimplyTypeName);

        // add attributes
        XSObjectList attrs = complexActualElement.getAttributeUses();
        for (int i = 0; i < attrs.getLength(); i++) {
            XSAttributeUse attr = (XSAttributeUse) attrs.item(i);
            String namespace = attr.getAttrDeclaration().getNamespace();
            String name = attr.getAttrDeclaration().getName();
            XSSimpleTypeDecl simpleType = (XSSimpleTypeDecl) attr.getAttrDeclaration().getTypeDefinition();
            attributes.add(new SchemaItemAttribute(namespace, name, simpleType.getPrimitiveKind(), simpleType.getName()));
        }

        if ((complexActualElement.getContentType() == XSComplexTypeDefinition.CONTENTTYPE_ELEMENT) ||
                (complexActualElement.getContentType() == XSComplexTypeDefinition.CONTENTTYPE_MIXED)) {
            // has children
            XSParticle particle = complexActualElement.getParticle();
            if (particle.getTerm() instanceof XSModelGroup) {
                return processModelGroup(particle, simpleElements, complexElements, node, complexActualElement, complexElement);
            }
        }

        return complexElement;
    }

    private static SchemaElementComplex processModelGroup(XSObject xsObject, List<SchemaElementSimple> simpleElements, List<SchemaElementComplex> complexElements, ElementPathNode node, XSComplexTypeDefinition complexActualElement, SchemaElementComplex complexElement) {
        XSTerm term = null;
        if (xsObject instanceof XSParticle) {
            term = ((XSParticle) xsObject).getTerm();
        } else {
            term = (XSTerm) xsObject;
        }

        if (term instanceof XSModelGroup) {
            XSModelGroup group = (XSModelGroup) term;
            XSObjectList particles = group.getParticles();
            for (int i = 0; i < particles.getLength(); i++) {
                XSParticle childParticle = (XSParticle) particles.item(i);

                if (childParticle.getTerm() instanceof XSElementDeclaration) {
                    XSElementDeclaration decl = (XSElementDeclaration) childParticle.getTerm();
                    boolean isArrayFlag = isArray(childParticle);

                    if (isSimpleTypeCategory(decl.getTypeDefinition().getTypeCategory())) {
                        XSSimpleTypeDecl simpleType = (XSSimpleTypeDecl) decl.getTypeDefinition();
                        Integer fractionDigits = getFractionRestriction(simpleType);
                        simpleElements.add(new SchemaElementSimple(decl.getName(), decl.getNamespace(), simpleType.getPrimitiveKind(), simpleType.getName(), isArrayFlag, fractionDigits));
                    }

                    if (isComplexTypeCategory(decl.getTypeDefinition().getTypeCategory())) {
                        String name = decl.getName();
                        String namespace = decl.getNamespace();
                        ElementPathNode newChild = node.addChild(name);

                        if (newChild.doesNameAlreadyExistInHierarchy()) {
                            continue;
                        }

                        complexActualElement = (XSComplexTypeDefinition) decl.getTypeDefinition();
                        SchemaElementComplex innerComplex = process(name, namespace, complexActualElement, isArrayFlag, newChild);

                        if (log.isDebugEnabled()) {
                            log.debug("Adding complex " + complexElement);
                        }
                        complexElements.add(innerComplex);
                    }
                }

                processModelGroup(childParticle.getTerm(), simpleElements, complexElements, node, complexActualElement, complexElement);
            }

        }
        return complexElement;
    }

    private static Integer getFractionRestriction(XSSimpleTypeDecl simpleType) {
        if ((simpleType.getDefinedFacets() & XSSimpleType.FACET_FRACTIONDIGITS) != 0) {
            XSObjectList facets = simpleType.getFacets();
            Integer digits = null;
            for (int f = 0; f < facets.getLength(); f++) {
                XSObject item = facets.item(f);
                if (item instanceof XSFacet) {
                    XSFacet facet = (XSFacet) item;
                    if (facet.getFacetKind() == XSSimpleType.FACET_FRACTIONDIGITS) {
                        try {
                            digits = Integer.parseInt(facet.getLexicalFacetValue());
                        } catch (RuntimeException ex) {
                            log.warn("Error parsing fraction facet value '" + facet.getLexicalFacetValue() + "' : " + ex.getMessage(), ex);
                        }
                    }
                }
            }
            return digits;
        }
        return null;
    }

    private static boolean isArray(XSParticle particle) {
        return particle.getMaxOccursUnbounded() || (particle.getMaxOccurs() > 1);
    }

    public static class LSInputImpl implements LSInput {

        private String stringData;

        public LSInputImpl(String stringData) {
            this.stringData = stringData;
        }

        @Override
        public Reader getCharacterStream() {
            return null;
        }

        @Override
        public void setCharacterStream(Reader characterStream) {
        }

        @Override
        public InputStream getByteStream() {
            return null;
        }

        @Override
        public void setByteStream(InputStream byteStream) {
        }

        @Override
        public String getStringData() {
            return stringData;
        }

        @Override
        public void setStringData(String stringData) {
            this.stringData = stringData;
        }

        @Override
        public String getSystemId() {
            return null;
        }

        @Override
        public void setSystemId(String systemId) {
        }

        @Override
        public String getPublicId() {
            return null;
        }

        @Override
        public void setPublicId(String publicId) {
        }

        @Override
        public String getBaseURI() {
            return null;
        }

        @Override
        public void setBaseURI(String baseURI) {
        }

        @Override
        public String getEncoding() {
            return null;
        }

        @Override
        public void setEncoding(String encoding) {
        }

        @Override
        public boolean getCertifiedText() {
            return false;
        }

        @Override
        public void setCertifiedText(boolean certifiedText) {
        }
    }

    public static class XSDSchemaMapperErrorHandler implements DOMErrorHandler {
        private final String schemaResource;

        public XSDSchemaMapperErrorHandler(String schemaResource) {
            this.schemaResource = schemaResource;
        }

        public boolean handleError(DOMError error) {
            String from = schemaResource != null ? schemaResource : "string";
            log.warn("DOM error reported loading schema from " + from + ":\n" +
                    "  message: " + error.getMessage() + "\n" +
                    "  type: " + error.getType() + "\n" +
                    "  related data: " + error.getRelatedData() + "\n" +
                    "  related exception: " + error.getRelatedException() + "\n" +
                    "  severity: " + error.getSeverity() + "\n" +
                    "  location: " + error.getLocation());

            if (error.getRelatedException() instanceof Throwable) {
                Throwable t = (Throwable) error.getRelatedException();
                log.warn("DOM error related exception: " + t.getMessage(), t);
            }

            return false;
        }
    }
}

