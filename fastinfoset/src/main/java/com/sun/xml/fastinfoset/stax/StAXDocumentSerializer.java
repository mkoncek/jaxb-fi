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

package com.sun.xml.fastinfoset.stax;

import com.sun.xml.fastinfoset.Encoder;
import com.sun.xml.fastinfoset.EncodingConstants;
import com.sun.xml.fastinfoset.util.NamespaceContextImplementation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.EmptyStackException;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.jvnet.fastinfoset.EncodingAlgorithmIndexes;
import com.sun.xml.fastinfoset.CommonResourceBundle;
import com.sun.xml.fastinfoset.QualifiedName;
import com.sun.xml.fastinfoset.util.LocalNameQualifiedNamesMap;
import org.jvnet.fastinfoset.stax.LowLevelFastInfosetStreamWriter;

/**
 * The Fast Infoset StAX serializer.
 * <p>
 * Instantiate this serializer to serialize a fast infoset document in accordance 
 * with the StAX API.
 * 
 * <p>
 * More than one fast infoset document may be encoded to the 
 * {@link java.io.OutputStream}.
 */
public class StAXDocumentSerializer extends Encoder 
        implements XMLStreamWriter, LowLevelFastInfosetStreamWriter {
    protected StAXManager _manager;
    
    protected String _encoding;
    /**
     * Local name of current element.
     */
    protected String _currentLocalName;
    
    /**
     * Namespace of current element.
     */
    protected String _currentUri;
    
    /**
     * Prefix of current element.
     */
    protected String _currentPrefix;

   /**
     * This flag indicates when there is a pending start element event.
     */
    protected boolean _inStartElement = false;
    
    /**
     * This flag indicates if the current element is empty.
     */
    protected boolean _isEmptyElement = false;

    /**
     * List of attributes qnames and values defined in the current element.
     */
    protected String[] _attributesArray = new String[4 * 16];
    protected int _attributesArrayIndex = 0;
    
    protected boolean[] _nsSupportContextStack = new boolean[32];
    protected int _stackCount = -1;    
    
    /**
     * Mapping between uris and prefixes.
     */
    protected NamespaceContextImplementation _nsContext = 
            new NamespaceContextImplementation();

    /**
     * List of namespaces defined in the current element.
     */
    protected String[] _namespacesArray = new String[2 * 8];
    protected int _namespacesArrayIndex = 0;
    
    public StAXDocumentSerializer() {
        super(true);
        _manager = new StAXManager(StAXManager.CONTEXT_WRITER);
    }

    @SuppressWarnings({"this-escape"})
    public StAXDocumentSerializer(OutputStream outputStream) {
        super(true);
        setOutputStream(outputStream);
        _manager = new StAXManager(StAXManager.CONTEXT_WRITER);
    }

    @SuppressWarnings({"this-escape"})
    public StAXDocumentSerializer(OutputStream outputStream, StAXManager manager) {
        super(true);
        setOutputStream(outputStream);
        _manager = manager;
    }
    
    @Override
    public void reset() {
        super.reset();
        
        _attributesArrayIndex = 0;
        _namespacesArrayIndex = 0;
        
        _nsContext.reset();
        _stackCount = -1;
                
        _currentUri = _currentPrefix = null;
        _currentLocalName = null;
        
        _inStartElement = _isEmptyElement = false;
    }
    
    // -- XMLStreamWriter Interface -------------------------------------------
            
    @Override
    public void writeStartDocument() throws XMLStreamException {
        writeStartDocument("finf", "1.0");
    }
    
    @Override
    public void writeStartDocument(String version) throws XMLStreamException {
        writeStartDocument("finf", version);
    }
    
    @Override
    public void writeStartDocument(String encoding, String version)
        throws XMLStreamException
    {
        reset();
        
        try {
            encodeHeader(false);
            encodeInitialVocabulary();
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }
    
    @Override
    public void writeEndDocument() throws XMLStreamException {
        try {
            
            // terminate all elements not terminated
            // by writeEndElement
            for(;_stackCount >= 0; _stackCount--) {
                writeEndElement();
            }

            encodeDocumentTermination();
        } 
        catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }
    
    @Override
    public void close() throws XMLStreamException {
        reset();
    }
    
    @Override
    public void flush() throws XMLStreamException {
        try {
            _s.flush();
        }
        catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }
    
    @Override
    public void writeStartElement(String localName)
        throws XMLStreamException
    {
        // TODO is it necessary for FI to obtain the default namespace in scope?
        writeStartElement("", localName, "");
    }
    
    @Override
    public void writeStartElement(String namespaceURI, String localName)
        throws XMLStreamException
    {
        writeStartElement("", localName, namespaceURI);
    }
    
    @Override
    public void writeStartElement(String prefix, String localName,
        String namespaceURI) throws XMLStreamException
    {
        encodeTerminationAndCurrentElement(false);
              
        _inStartElement = true;
        _isEmptyElement = false;

        _currentLocalName = localName;
        _currentPrefix = prefix;
        _currentUri = namespaceURI;

        _stackCount++;
        if (_stackCount == _nsSupportContextStack.length) {
            boolean[] nsSupportContextStack = new boolean[_stackCount * 2];
            System.arraycopy(_nsSupportContextStack, 0, nsSupportContextStack, 0, _nsSupportContextStack.length);
            _nsSupportContextStack = nsSupportContextStack;
        }
        
        _nsSupportContextStack[_stackCount] = false;
    }
    
    @Override
    public void writeEmptyElement(String localName)
        throws XMLStreamException
    {
        writeEmptyElement("", localName, "");
    }
    
    @Override
    public void writeEmptyElement(String namespaceURI, String localName)
        throws XMLStreamException
    {
        writeEmptyElement("", localName, namespaceURI);
    }
    
    @Override
    public void writeEmptyElement(String prefix, String localName, 
        String namespaceURI) throws XMLStreamException
    {
        encodeTerminationAndCurrentElement(false);
        
        _isEmptyElement = _inStartElement = true;
        
        _currentLocalName = localName;
        _currentPrefix = prefix;
        _currentUri = namespaceURI;
        
        _stackCount++;
        if (_stackCount == _nsSupportContextStack.length) {
            boolean[] nsSupportContextStack = new boolean[_stackCount * 2];
            System.arraycopy(_nsSupportContextStack, 0, nsSupportContextStack, 0, _nsSupportContextStack.length);
            _nsSupportContextStack = nsSupportContextStack;
        }
        
        _nsSupportContextStack[_stackCount] = false;
    }
        
    @Override
    public void writeEndElement() throws XMLStreamException {
        if (_inStartElement) {
            encodeTerminationAndCurrentElement(false);
        }
            
        try {            
            encodeElementTermination();
            if (_nsSupportContextStack[_stackCount--]) {
                _nsContext.popContext();
            }
        }
        catch (IOException | EmptyStackException e) {
            throw new XMLStreamException(e);
        }
    }

    
    @Override
    public void writeAttribute(String localName, String value)
        throws XMLStreamException
    {
        writeAttribute("", "", localName, value);
    }
    
    @Override
    public void writeAttribute(String namespaceURI, String localName,
        String value) throws XMLStreamException
    {
        String prefix = "";
        
        // Find prefix for attribute, ignoring default namespace
        if (!namespaceURI.isEmpty()) {
            prefix = _nsContext.getNonDefaultPrefix(namespaceURI);

            // Undeclared prefix or ignorable default ns?
            if (prefix == null || prefix.isEmpty()) {
                // Workaround for BUG in SAX NamespaceSupport helper
                // which incorrectly defines namespace declaration URI
                if (namespaceURI == EncodingConstants.XMLNS_NAMESPACE_NAME || 
                        namespaceURI.equals(EncodingConstants.XMLNS_NAMESPACE_NAME)) {
                    // TODO
                    // Need to check carefully the rule for the writing of
                    // namespaces in StAX. Is it safe to ignore such 
                    // attributes, as declarations will be made using the
                    // writeNamespace method
                    return;
                }
                throw new XMLStreamException(CommonResourceBundle.getInstance().getString("message.URIUnbound", new Object[]{namespaceURI}));
            }
        }
        writeAttribute(prefix, namespaceURI, localName, value);
    }
        
    @Override
    public void writeAttribute(String prefix, String namespaceURI,
        String localName, String value) throws XMLStreamException
    {
        if (!_inStartElement) {
            throw new IllegalStateException(CommonResourceBundle.getInstance().getString("message.attributeWritingNotAllowed"));
        }

        // TODO
        // Need to check carefully the rule for the writing of
        // namespaces in StAX. Is it safe to ignore such 
        // attributes, as declarations will be made using the
        // writeNamespace method
        if (namespaceURI == EncodingConstants.XMLNS_NAMESPACE_NAME || 
                namespaceURI.equals(EncodingConstants.XMLNS_NAMESPACE_NAME)) {
            return;
        }

        if (_attributesArrayIndex == _attributesArray.length) {
            final String[] attributesArray = new String[_attributesArrayIndex * 2];
            System.arraycopy(_attributesArray, 0, attributesArray, 0, _attributesArrayIndex);
            _attributesArray = attributesArray;
        }
        
        _attributesArray[_attributesArrayIndex++] = namespaceURI;
        _attributesArray[_attributesArrayIndex++] = prefix;
        _attributesArray[_attributesArrayIndex++] = localName;
        _attributesArray[_attributesArrayIndex++] = value;
    }
    
    @Override
    public void writeNamespace(String prefix, String namespaceURI)
        throws XMLStreamException
    {
        if (prefix == null || prefix.isEmpty() || prefix.equals(EncodingConstants.XMLNS_NAMESPACE_PREFIX)) {
            writeDefaultNamespace(namespaceURI);
        }
        else {
            if (!_inStartElement) {
                throw new IllegalStateException(CommonResourceBundle.getInstance().getString("message.attributeWritingNotAllowed"));
            }
            
            if (_namespacesArrayIndex == _namespacesArray.length) {
                final String[] namespacesArray = new String[_namespacesArrayIndex * 2];
                System.arraycopy(_namespacesArray, 0, namespacesArray, 0, _namespacesArrayIndex);
                _namespacesArray = namespacesArray;
            }
            
            _namespacesArray[_namespacesArrayIndex++] = prefix;
            _namespacesArray[_namespacesArrayIndex++] = namespaceURI;
            setPrefix(prefix, namespaceURI);
        }
    }
    
    @Override
    public void writeDefaultNamespace(String namespaceURI)
        throws XMLStreamException
    {
        if (!_inStartElement) {
            throw new IllegalStateException(CommonResourceBundle.getInstance().getString("message.attributeWritingNotAllowed"));
        }
        
        if (_namespacesArrayIndex == _namespacesArray.length) {
            final String[] namespacesArray = new String[_namespacesArrayIndex * 2];
            System.arraycopy(_namespacesArray, 0, namespacesArray, 0, _namespacesArrayIndex);
            _namespacesArray = namespacesArray;
        }

        _namespacesArray[_namespacesArrayIndex++] = "";
        _namespacesArray[_namespacesArrayIndex++] = namespaceURI;
        setPrefix("", namespaceURI);
    }
    
    @Override
    public void writeComment(String data) throws XMLStreamException {
        try {
            if (getIgnoreComments()) return;
            
            encodeTerminationAndCurrentElement(true);

            // TODO: avoid array copy here
            encodeComment(data.toCharArray(), 0, data.length());
        }
        catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }
    
    @Override
    public void writeProcessingInstruction(String target)
        throws XMLStreamException
    {
        writeProcessingInstruction(target, "");
    }
    
    @Override
    public void writeProcessingInstruction(String target, String data)
        throws XMLStreamException
    {
        try {
            if (getIgnoreProcesingInstructions()) return;
            
            encodeTerminationAndCurrentElement(true);

            encodeProcessingInstruction(target, data);
        }
        catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }
    
    @Override
    public void writeCData(String text) throws XMLStreamException {
         try {
            final int length = text.length();
            if (length == 0) {
                return;
            } else if (length < _charBuffer.length) {
                if (getIgnoreWhiteSpaceTextContent() &&
                        isWhiteSpace(text)) return;

                // Warning: this method must be called before any state
                // is modified, such as the _charBuffer contents,
                // so the characters of text cannot be copied to _charBuffer
                // before this call
                encodeTerminationAndCurrentElement(true);

                text.getChars(0, length, _charBuffer, 0);
                encodeCIIBuiltInAlgorithmDataAsCDATA(_charBuffer, 0, length);
            } else {
                final char[] ch = text.toCharArray();
                if (getIgnoreWhiteSpaceTextContent() &&
                        isWhiteSpace(ch, 0, length)) return;

                encodeTerminationAndCurrentElement(true);

                encodeCIIBuiltInAlgorithmDataAsCDATA(ch, 0, length);
            }
        } catch (Exception e) {
            throw new XMLStreamException(e);
        }
    }
    
    @Override
    public void writeDTD(String dtd) throws XMLStreamException {
        throw new UnsupportedOperationException(CommonResourceBundle.getInstance().getString("message.notImplemented"));
    }
    
    @Override
    public void writeEntityRef(String name) throws XMLStreamException {
        throw new UnsupportedOperationException(CommonResourceBundle.getInstance().getString("message.notImplemented"));
    }
        
    @Override
    public void writeCharacters(String text) throws XMLStreamException {
         try {
            final int length = text.length();
            if (length == 0) {
                return;
            } else if (length < _charBuffer.length) {
                if (getIgnoreWhiteSpaceTextContent() && 
                        isWhiteSpace(text)) return;
                
                // Warning: this method must be called before any state
                // is modified, such as the _charBuffer contents,
                // so the characters of text cannot be copied to _charBuffer
                // before this call
                encodeTerminationAndCurrentElement(true);
                
                text.getChars(0, length, _charBuffer, 0);
                encodeCharacters(_charBuffer, 0, length);
            } else {
                final char[] ch = text.toCharArray();
                if (getIgnoreWhiteSpaceTextContent() && 
                        isWhiteSpace(ch, 0, length)) return;
                
                encodeTerminationAndCurrentElement(true);
                
                encodeCharactersNoClone(ch, 0, length);
            }
        }
        catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }
    
    @Override
    public void writeCharacters(char[] text, int start, int len)
        throws XMLStreamException
    {
         try {
            if (len <= 0) {
                return;
            }

            if (getIgnoreWhiteSpaceTextContent() && 
                    isWhiteSpace(text, start, len)) return;
            
            encodeTerminationAndCurrentElement(true);

            encodeCharacters(text, start, len);
        }
        catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    @Override
    public String getPrefix(String uri) throws XMLStreamException {
        return _nsContext.getPrefix(uri);
    }
    
    @Override
    public void setPrefix(String prefix, String uri) 
        throws XMLStreamException 
    {
        if (_stackCount > -1 && !_nsSupportContextStack[_stackCount]) {
            _nsSupportContextStack[_stackCount] = true;
            _nsContext.pushContext();
        }
        
        _nsContext.declarePrefix(prefix, uri);
    }
    
    @Override
    public void setDefaultNamespace(String uri) throws XMLStreamException {
        setPrefix("", uri);
    }
    
    /**
     * Sets the current namespace context for prefix and uri bindings.
     * This context becomes the root namespace context for writing and
     * will replace the current root namespace context.  Subsequent calls
     * to setPrefix and setDefaultNamespace will bind namespaces using
     * the context passed to the method as the root context for resolving
     * namespaces.  This method may only be called once at the start of
     * the document.  It does not cause the namespaces to be declared.
     * If a namespace URI to prefix mapping is found in the namespace
     * context it is treated as declared and the prefix may be used
     * by the StreamWriter.
     * @param context the namespace context to use for this writer, may not be null
     */
    @Override
    public void setNamespaceContext(NamespaceContext context)
        throws XMLStreamException 
    {           
        throw new UnsupportedOperationException("setNamespaceContext");
    }
    
    @Override
    public NamespaceContext getNamespaceContext() {
        return _nsContext;
    }
    
    @Override
    public Object getProperty(java.lang.String name) 
        throws IllegalArgumentException 
    {
        if (_manager != null) {
            return _manager.getProperty(name);
        }
        return null;
    }

    public void setManager(StAXManager manager) {
        _manager = manager;
    }
    
    public void setEncoding(String encoding) {
        _encoding = encoding;
    }
    

    public void writeOctets(byte[] b, int start, int len)
        throws XMLStreamException
    {
         try {
            if (len == 0) {
                return;
            }

            encodeTerminationAndCurrentElement(true);

            encodeCIIOctetAlgorithmData(EncodingAlgorithmIndexes.BASE64, b, start, len);
        }
        catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }
    
    protected void encodeTerminationAndCurrentElement(boolean terminateAfter) throws XMLStreamException {
        try {
            encodeTermination();
            
            if (_inStartElement) {

                _b = EncodingConstants.ELEMENT;
                if (_attributesArrayIndex > 0) {
                    _b |= EncodingConstants.ELEMENT_ATTRIBUTE_FLAG;
                }

                // Encode namespace decls associated with this element
                if (_namespacesArrayIndex > 0) {
                    write(_b | EncodingConstants.ELEMENT_NAMESPACES_FLAG);
                    for (int i = 0; i < _namespacesArrayIndex;) {
                        encodeNamespaceAttribute(_namespacesArray[i++], _namespacesArray[i++]);
                    }
                    _namespacesArrayIndex = 0;
                    
                    write(EncodingConstants.TERMINATOR);

                    _b = 0;
                }

                // If element's prefix is empty - apply default scope namespace
                if (_currentPrefix.isEmpty()) {
                    if (_currentUri.isEmpty()) {
                        _currentUri = _nsContext.getNamespaceURI("");
                    } else {
                        String tmpPrefix = getPrefix(_currentUri);
                        if (tmpPrefix != null) {
                            _currentPrefix = tmpPrefix;
                        }
                    }
                }
                
                encodeElementQualifiedNameOnThirdBit(_currentUri, _currentPrefix, _currentLocalName);
                
                for (int i = 0; i < _attributesArrayIndex;) {
                    encodeAttributeQualifiedNameOnSecondBit(
                            _attributesArray[i++], _attributesArray[i++], _attributesArray[i++]);

                    final String value = _attributesArray[i];
                    _attributesArray[i++] = null;
                    final boolean addToTable = isAttributeValueLengthMatchesLimit(value.length());
                    encodeNonIdentifyingStringOnFirstBit(value, _v.attributeValue, addToTable, false);
                    
                    _b = EncodingConstants.TERMINATOR;
                    _terminate = true;
                }
                _attributesArrayIndex = 0;
                _inStartElement = false;

                if (_isEmptyElement) {
                    encodeElementTermination();
                    if (_nsSupportContextStack[_stackCount--]) {
                        _nsContext.popContext();
                    }
                    
                    _isEmptyElement = false;
                }
                
                if (terminateAfter) {
                    encodeTermination();
                }
            }
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    
    // LowLevelFastInfosetSerializer
    
    @Override
    public final void initiateLowLevelWriting() throws XMLStreamException {
        encodeTerminationAndCurrentElement(false);
    }
        
    @Override
    public final int getNextElementIndex() {
        return _v.elementName.getNextIndex();
    }

    @Override
    public final int getNextAttributeIndex() {
        return _v.attributeName.getNextIndex();
    }
    
    @Override
    public final int getLocalNameIndex() {
        return _v.localName.getIndex();
    }
    
    @Override
    public final int getNextLocalNameIndex() {
        return _v.localName.getNextIndex();
    }

    @Override
    public final void writeLowLevelTerminationAndMark() throws IOException {
        encodeTermination();
        mark();
    }

    @Override
    public final void writeLowLevelStartElementIndexed(int type, int index) throws IOException {
        _b = type;
        encodeNonZeroIntegerOnThirdBit(index);
    }
    
    @Override
    public final boolean writeLowLevelStartElement(int type, String prefix, String localName,
            String namespaceURI) throws IOException {
        final boolean isIndexed = encodeElement(type, namespaceURI, prefix, localName);
        
        if (!isIndexed)
            encodeLiteral(type | EncodingConstants.ELEMENT_LITERAL_QNAME_FLAG,
                    namespaceURI, prefix, localName);
        
        return isIndexed;
    }
                
    @Override
    public final void writeLowLevelStartNamespaces() throws IOException {
        write(EncodingConstants.ELEMENT | EncodingConstants.ELEMENT_NAMESPACES_FLAG);
    }
    
    @Override
    public final void writeLowLevelNamespace(String prefix, String namespaceName) 
        throws IOException {
        encodeNamespaceAttribute(prefix, namespaceName);
    }
    
    @Override
    public final void writeLowLevelEndNamespaces() throws IOException {
        write(EncodingConstants.TERMINATOR);
    }
    
    @Override
    public final void writeLowLevelStartAttributes() throws IOException {
        if (hasMark()) {
            _octetBuffer[_markIndex] |= EncodingConstants.ELEMENT_ATTRIBUTE_FLAG;
            resetMark();
        }
    }
        
    @Override
    public final void writeLowLevelAttributeIndexed(int index) throws IOException {
        encodeNonZeroIntegerOnSecondBitFirstBitZero(index);
    }
    
    @Override
    public final boolean writeLowLevelAttribute(String prefix, String namespaceURI, String localName) throws IOException {
        final boolean isIndexed = encodeAttribute(namespaceURI, prefix, localName);

        if (!isIndexed)
            encodeLiteral(EncodingConstants.ATTRIBUTE_LITERAL_QNAME_FLAG, 
                    namespaceURI, prefix, localName);
        
        return isIndexed;
    }
                
    @Override
    public final void writeLowLevelAttributeValue(String value) throws IOException
    {
        final boolean addToTable = isAttributeValueLengthMatchesLimit(value.length());
        encodeNonIdentifyingStringOnFirstBit(value, _v.attributeValue, addToTable, false);
    }
    
    @Override
    public final void writeLowLevelStartNameLiteral(int type, String prefix, byte[] utf8LocalName, 
            String namespaceURI) throws IOException {
        encodeLiteralHeader(type, namespaceURI, prefix);
        encodeNonZeroOctetStringLengthOnSecondBit(utf8LocalName.length);
        write(utf8LocalName, 0, utf8LocalName.length);
    }
    
    @Override
    public final void writeLowLevelStartNameLiteral(int type, String prefix, int localNameIndex, 
            String namespaceURI) throws IOException {
        encodeLiteralHeader(type, namespaceURI, prefix);
        encodeNonZeroIntegerOnSecondBitFirstBitOne(localNameIndex);
    }
     
    @Override
    public final void writeLowLevelEndStartElement() throws IOException {
        if (hasMark()) {
            resetMark();
        } else {
            // Terminate the attributes
            _b = EncodingConstants.TERMINATOR;
            _terminate = true;
        }
    }
    
    @Override
    public final void writeLowLevelEndElement() throws IOException {
        encodeElementTermination();
    }
    
    @Override
    public final void writeLowLevelText(char[] text, int length) throws IOException {
        if (length == 0)
            return;
        
        encodeTermination();
        
        encodeCharacters(text, 0, length);
    }
    
    @Override
    public final void writeLowLevelText(String text) throws IOException {
        final int length = text.length();
        if (length == 0)
            return;
        
        encodeTermination();
        
        if (length < _charBuffer.length) {
            text.getChars(0, length, _charBuffer, 0);
            encodeCharacters(_charBuffer, 0, length);
        } else {
            final char[] ch = text.toCharArray();
            encodeCharactersNoClone(ch, 0, length);
        }
    }
    
    @Override
    public final void writeLowLevelOctets(byte[] octets, int length) throws IOException {
        if (length == 0)
            return;
        
        encodeTermination();
        
        encodeCIIOctetAlgorithmData(EncodingAlgorithmIndexes.BASE64, octets, 0, length);
    }

    private boolean encodeElement(int type, String namespaceURI, String prefix, String localName) throws IOException {
        final LocalNameQualifiedNamesMap.Entry entry = _v.elementName.obtainEntry(localName);
        for (int i = 0; i < entry._valueIndex; i++) {
            final QualifiedName name = entry._value[i];
            if ((prefix == name.prefix || prefix.equals(name.prefix))
                    && (namespaceURI == name.namespaceName || namespaceURI.equals(name.namespaceName))) {
                _b = type;
                encodeNonZeroIntegerOnThirdBit(name.index);
                return true;
            }
        }

        entry.addQualifiedName(new QualifiedName(prefix, namespaceURI, localName, "", _v.elementName.getNextIndex()));
        return false;
    }
        
    private boolean encodeAttribute(String namespaceURI, String prefix, String localName) throws IOException {
        final LocalNameQualifiedNamesMap.Entry entry = _v.attributeName.obtainEntry(localName);
        for (int i = 0; i < entry._valueIndex; i++) {
            final QualifiedName name = entry._value[i];
            if ((prefix == name.prefix || prefix.equals(name.prefix))
                    && (namespaceURI == name.namespaceName || namespaceURI.equals(name.namespaceName))) {
                encodeNonZeroIntegerOnSecondBitFirstBitZero(name.index);
                return true;
            }
        }

        entry.addQualifiedName(new QualifiedName(prefix, namespaceURI, localName, "", _v.attributeName.getNextIndex()));
        return false;
    }

    private void encodeLiteralHeader(int type, String namespaceURI, String prefix) throws IOException {
        if (namespaceURI != "") {
            type |= EncodingConstants.LITERAL_QNAME_NAMESPACE_NAME_FLAG;
            if (prefix != "")
                type |= EncodingConstants.LITERAL_QNAME_PREFIX_FLAG;
                        
            write(type);
            if (prefix != "")
                encodeNonZeroIntegerOnSecondBitFirstBitOne(_v.prefix.get(prefix));
            encodeNonZeroIntegerOnSecondBitFirstBitOne(_v.namespaceName.get(namespaceURI));
        } else
            write(type);
    }
        
    private void encodeLiteral(int type, String namespaceURI, String prefix, String localName) throws IOException {
        encodeLiteralHeader(type, namespaceURI, prefix);
        
        final int localNameIndex = _v.localName.obtainIndex(localName);
        if (localNameIndex == -1) {
            encodeNonEmptyOctetStringOnSecondBit(localName);
        } else
            encodeNonZeroIntegerOnSecondBitFirstBitOne(localNameIndex);
    }    
}
