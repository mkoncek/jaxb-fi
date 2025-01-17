/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2004, 2023 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.xml.fastinfoset.dom;

import com.sun.xml.fastinfoset.Encoder;
import com.sun.xml.fastinfoset.EncodingConstants;
import com.sun.xml.fastinfoset.QualifiedName;
import com.sun.xml.fastinfoset.util.NamespaceContextImplementation;
import com.sun.xml.fastinfoset.util.LocalNameQualifiedNamesMap;
import java.io.IOException;
import org.jvnet.fastinfoset.FastInfosetException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The Fast Infoset DOM serializer.
 * <p>
 * Instantiate this serializer to serialize a fast infoset document in accordance
 * with the DOM API.
 *
 */
public class DOMDocumentSerializer extends Encoder {

    public DOMDocumentSerializer() {
        super();
    }

    public DOMDocumentSerializer(boolean useLocalNameAsKeyForQualifiedNameLookup) {
        super(useLocalNameAsKeyForQualifiedNameLookup);
    }

    /**
     * Serialize a {@link Node}.
     *
     * @param n the node to serialize.
     */
    public final void serialize(Node n) throws IOException {
        switch (n.getNodeType()) {
            case Node.DOCUMENT_NODE:
                serialize((Document)n);
                break;
            case Node.ELEMENT_NODE:
                serializeElementAsDocument(n);
                break;
            case Node.COMMENT_NODE:
                serializeComment(n);
                break;
            case Node.PROCESSING_INSTRUCTION_NODE:
                serializeProcessingInstruction(n);
                break;
        }
    }
    
    /**
     * Serialize a {@link Document}.
     *
     * @param d the document to serialize.
     */
    public final void serialize(Document d) throws IOException {
        reset();
        encodeHeader(false);
        encodeInitialVocabulary();
        
        final NodeList nl = d.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            final Node n = nl.item(i);
            switch (n.getNodeType()) {
                case Node.ELEMENT_NODE:
                    serializeElement(n);
                    break;
                case Node.COMMENT_NODE:
                    serializeComment(n);
                    break;
                case Node.PROCESSING_INSTRUCTION_NODE:
                    serializeProcessingInstruction(n);
                    break;
            }
        }
        encodeDocumentTermination();
    }
    
    protected final void serializeElementAsDocument(Node e) throws IOException {
        reset();
        encodeHeader(false);
        encodeInitialVocabulary();
        
        serializeElement(e);
        
        encodeDocumentTermination();
    }
    
//    protected Node[] _namespaceAttributes = new Node[4];
    
    // map which will hold all current scope prefixes and associated attributes
    // Collection of populated namespace available for current scope
    protected NamespaceContextImplementation _namespaceScopeContext = new NamespaceContextImplementation();
    protected Node[] _attributes = new Node[32];
    
    protected final void serializeElement(Node e) throws IOException {
        encodeTermination();
        
        int attributesSize = 0;
        
        _namespaceScopeContext.pushContext();
        
        if (e.hasAttributes()) {
            /*
             * Split the attribute nodes into namespace attributes
             * or normal attributes.
             */
            final NamedNodeMap nnm = e.getAttributes();
            for (int i = 0; i < nnm.getLength(); i++) {
                final Node a = nnm.item(i);
                final String namespaceURI = a.getNamespaceURI();
                if (namespaceURI != null && namespaceURI.equals("http://www.w3.org/2000/xmlns/")) {
                    String attrPrefix = a.getLocalName();
                    String attrNamespace = a.getNodeValue();
                    if (attrPrefix == "xmlns" || attrPrefix.equals("xmlns")) {
                        attrPrefix = "";
                    }
                    _namespaceScopeContext.declarePrefix(attrPrefix, attrNamespace);
                } else {
                    if (attributesSize == _attributes.length) {
                        final Node[] attributes = new Node[attributesSize * 3 / 2 + 1];
                        System.arraycopy(_attributes, 0, attributes, 0, attributesSize);
                        _attributes = attributes;
                    }
                    _attributes[attributesSize++] = a;
                    
                    String attrNamespaceURI = a.getNamespaceURI();
                    String attrPrefix = a.getPrefix();
                    if (attrPrefix != null && !_namespaceScopeContext.getNamespaceURI(attrPrefix).equals(attrNamespaceURI)) {
                        _namespaceScopeContext.declarePrefix(attrPrefix, attrNamespaceURI);
                    }
                }
            }
        }
        
        String elementNamespaceURI = e.getNamespaceURI();
        String elementPrefix = e.getPrefix();
        if (elementPrefix == null)  elementPrefix = "";
        if (elementNamespaceURI != null &&
                !_namespaceScopeContext.getNamespaceURI(elementPrefix).equals(elementNamespaceURI)) {
            _namespaceScopeContext.declarePrefix(elementPrefix, elementNamespaceURI);
        }
        
        if (!_namespaceScopeContext.isCurrentContextEmpty()) {
            if (attributesSize > 0) {
                write(EncodingConstants.ELEMENT | EncodingConstants.ELEMENT_NAMESPACES_FLAG |
                        EncodingConstants.ELEMENT_ATTRIBUTE_FLAG);
            } else {
                write(EncodingConstants.ELEMENT | EncodingConstants.ELEMENT_NAMESPACES_FLAG);
            }
            
            for (int i = _namespaceScopeContext.getCurrentContextStartIndex();
                     i < _namespaceScopeContext.getCurrentContextEndIndex(); i++) {

                String prefix = _namespaceScopeContext.getPrefix(i);
                String uri = _namespaceScopeContext.getNamespaceURI(i);
                encodeNamespaceAttribute(prefix, uri);
            }
            write(EncodingConstants.TERMINATOR);
            _b = 0;
        } else {
            _b = (attributesSize > 0) ? EncodingConstants.ELEMENT | EncodingConstants.ELEMENT_ATTRIBUTE_FLAG :
                EncodingConstants.ELEMENT;
        }
        
        String namespaceURI = elementNamespaceURI;
//        namespaceURI = (namespaceURI == null) ? _namespaceScopeContext.getNamespaceURI("") : namespaceURI;
        namespaceURI = (namespaceURI == null) ? "" : namespaceURI;
        encodeElement(namespaceURI, e.getNodeName(), e.getLocalName());
        
        if (attributesSize > 0) {
            // Serialize the attributes
            for (int i = 0; i < attributesSize; i++) {
                final Node a = _attributes[i];
                _attributes[i] = null;
                namespaceURI = a.getNamespaceURI();
                namespaceURI = (namespaceURI == null) ? "" : namespaceURI;
                encodeAttribute(namespaceURI, a.getNodeName(), a.getLocalName());
                
                final String value = a.getNodeValue();
                final boolean addToTable = isAttributeValueLengthMatchesLimit(value.length());
                encodeNonIdentifyingStringOnFirstBit(value, _v.attributeValue, addToTable, false);
            }
            
            _b = EncodingConstants.TERMINATOR;
            _terminate = true;
        }
        
        if (e.hasChildNodes()) {
            // Serialize the children
            final NodeList nl = e.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                final Node n = nl.item(i);
                switch (n.getNodeType()) {
                    case Node.ELEMENT_NODE:
                        serializeElement(n);
                        break;
                    case Node.TEXT_NODE:
                        serializeText(n);
                        break;
                    case Node.CDATA_SECTION_NODE:
                        serializeCDATA(n);
                        break;
                    case Node.COMMENT_NODE:
                        serializeComment(n);
                        break;
                    case Node.PROCESSING_INSTRUCTION_NODE:
                        serializeProcessingInstruction(n);
                        break;
                }
            }
        }
        encodeElementTermination();
        _namespaceScopeContext.popContext();
    }
    
    
    protected final void serializeText(Node t) throws IOException {
        final String text = t.getNodeValue();
        
        final int length = (text != null) ? text.length() : 0;
        if (length == 0) {
            return;
        } else if (length < _charBuffer.length) {
            text.getChars(0, length, _charBuffer, 0);
            if (getIgnoreWhiteSpaceTextContent() &&
                    isWhiteSpace(_charBuffer, 0, length)) return;
            
            encodeTermination();
            encodeCharacters(_charBuffer, 0, length);
        } else {
            final char[] ch = text.toCharArray();
            if (getIgnoreWhiteSpaceTextContent() &&
                    isWhiteSpace(ch, 0, length)) return;
            
            encodeTermination();
            encodeCharactersNoClone(ch, 0, length);
        }
    }
    
    protected final void serializeCDATA(Node t) throws IOException {
        final String text = t.getNodeValue();
        
        final int length = (text != null) ? text.length() : 0;
        if (length == 0) {
            return;
        } else {
            final char[] ch = text.toCharArray();
            if (getIgnoreWhiteSpaceTextContent() &&
                    isWhiteSpace(ch, 0, length)) return;
            
            encodeTermination();
            try {
                encodeCIIBuiltInAlgorithmDataAsCDATA(ch, 0, length);
            } catch (FastInfosetException e) {
                throw new IOException("");
            }
        }
    }
    
    protected final void serializeComment(Node c) throws IOException {
        if (getIgnoreComments()) return;
        
        encodeTermination();
        
        final String comment = c.getNodeValue();
        
        final int length = (comment != null) ? comment.length() : 0;
        if (length == 0) {
            encodeComment(_charBuffer, 0, 0);
        } else if (length < _charBuffer.length) {
            comment.getChars(0, length, _charBuffer, 0);
            encodeComment(_charBuffer, 0, length);
        } else {
            final char[] ch = comment.toCharArray();
            encodeCommentNoClone(ch, 0, length);
        }
    }
    
    protected final void serializeProcessingInstruction(Node pi) throws IOException {
        if (getIgnoreProcesingInstructions()) return;
        
        encodeTermination();
        
        final String target = pi.getNodeName();
        final String data = pi.getNodeValue();
        encodeProcessingInstruction(target, data);
    }
    
    protected final void encodeElement(String namespaceURI, String qName, String localName) throws IOException {
        LocalNameQualifiedNamesMap.Entry entry = _v.elementName.obtainEntry(qName);
        if (entry._valueIndex > 0) {
            final QualifiedName[] names = entry._value;
            for (int i = 0; i < entry._valueIndex; i++) {
                if ((namespaceURI == names[i].namespaceName || namespaceURI.equals(names[i].namespaceName))) {
                    encodeNonZeroIntegerOnThirdBit(names[i].index);
                    return;
                }
            }
        }
        
        // Was DOM node created using an NS-aware call?
        if (localName != null) {
            encodeLiteralElementQualifiedNameOnThirdBit(namespaceURI, getPrefixFromQualifiedName(qName),
                    localName, entry);
        } else {
            encodeLiteralElementQualifiedNameOnThirdBit(namespaceURI, "", qName, entry);
        }
    }
    
    protected final void encodeAttribute(String namespaceURI, String qName, String localName) throws IOException {
        LocalNameQualifiedNamesMap.Entry entry = _v.attributeName.obtainEntry(qName);
        if (entry._valueIndex > 0) {
            final QualifiedName[] names = entry._value;
            for (int i = 0; i < entry._valueIndex; i++) {
                if ((namespaceURI == names[i].namespaceName || namespaceURI.equals(names[i].namespaceName))) {
                    encodeNonZeroIntegerOnSecondBitFirstBitZero(names[i].index);
                    return;
                }
            }
        }
        
        // Was DOM node created using an NS-aware call?
        if (localName != null) {
            encodeLiteralAttributeQualifiedNameOnSecondBit(namespaceURI, getPrefixFromQualifiedName(qName),
                    localName, entry);
        } else {
            encodeLiteralAttributeQualifiedNameOnSecondBit(namespaceURI, "", qName, entry);
        }
    }
}
