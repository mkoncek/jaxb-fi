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

package com.sun.xml.fastinfoset.tools;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;
import com.sun.xml.fastinfoset.CommonResourceBundle;

public class SAXEventSerializer extends DefaultHandler
        implements LexicalHandler {

    private final Writer _writer; 
    private boolean _charactersAreCDATA;
    private StringBuffer _characters;
    
    private final Stack<AttributeValueHolder[]> _namespaceStack = new Stack<>();
    protected List<AttributeValueHolder> _namespaceAttributes;
    
    public SAXEventSerializer(OutputStream s) throws IOException {
        _writer = new OutputStreamWriter(s);
        _charactersAreCDATA = false;
    }
    
    // -- ContentHandler interface ---------------------------------------

    @Override
    public void startDocument() throws SAXException {
        try {
            _writer.write("<sax xmlns=\"http://www.sun.com/xml/sax-events\">\n");
            _writer.write("<startDocument/>\n");   
            _writer.flush();
        }
        catch (IOException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void endDocument() throws SAXException {
        try {
            _writer.write("<endDocument/>\n");  
            _writer.write("</sax>");
            _writer.flush();
            _writer.close();
        }
        catch (IOException e) {
            throw new SAXException(e);
        }
    }

    
    @Override
    public void startPrefixMapping(String prefix, String uri)
        throws SAXException
    {
        if (_namespaceAttributes == null) {
            _namespaceAttributes = new ArrayList<>();
        }
        
        String qName = (prefix.isEmpty()) ? "xmlns" : "xmlns" + prefix;
        AttributeValueHolder attribute = new AttributeValueHolder(
                qName,
                prefix, 
                uri, 
                null,
                null);
        _namespaceAttributes.add(attribute);            
    }
    
    @Override
    public void endPrefixMapping(String prefix)
        throws SAXException 
    {
        /*
        try {
            outputCharacters();

            _writer.write("<endPrefixMapping prefix=\"" +
                prefix + "\"/>\n");
            _writer.flush();
        }
        catch (IOException e) {
            throw new SAXException(e);
        }
         */
    }

    @Override
    public void startElement(String uri, String localName,
                             String qName, Attributes attributes)
        throws SAXException 
    {
        try {
            outputCharacters();

            if (_namespaceAttributes != null) {
                
                AttributeValueHolder[] attrsHolder = new AttributeValueHolder[0];
                attrsHolder = _namespaceAttributes.toArray(attrsHolder);
                        
                // Sort attributes
                quicksort(attrsHolder, 0, attrsHolder.length - 1);

                for (AttributeValueHolder attrsHolder1 : attrsHolder) {
                    _writer.write("<startPrefixMapping prefix=\""
                            + attrsHolder1.localName + "\" uri=\""
                            + attrsHolder1.uri + "\"/>\n");
                    _writer.flush();
                }
                        
                _namespaceStack.push(attrsHolder);
                _namespaceAttributes = null;
            } else {
                _namespaceStack.push(null);
            }
            
            AttributeValueHolder[] attrsHolder = 
                new AttributeValueHolder[attributes.getLength()];
            for (int i = 0; i < attributes.getLength(); i++) {
                attrsHolder[i] = new AttributeValueHolder(
                    attributes.getQName(i), 
                    attributes.getLocalName(i), 
                    attributes.getURI(i), 
                    attributes.getType(i),
                    attributes.getValue(i));
            }
            
            // Sort attributes
            quicksort(attrsHolder, 0, attrsHolder.length - 1);

            int attributeCount = 0;
            for (AttributeValueHolder attrsHolder1 : attrsHolder) {
                if (attrsHolder1.uri.equals("http://www.w3.org/2000/xmlns/")) {
                    // Ignore XMLNS attributes
                    continue;
                }
                attributeCount++;
            }
            
            if (attributeCount == 0) {
                _writer.write("<startElement uri=\"" + uri 
                    + "\" localName=\"" + localName + "\" qName=\""
                    + qName + "\"/>\n");
                return;
            }
            
            _writer.write("<startElement uri=\"" + uri 
                + "\" localName=\"" + localName + "\" qName=\""
                + qName + "\">\n");

            // Serialize attributes as children
            for (AttributeValueHolder attrsHolder1 : attrsHolder) {
                if (attrsHolder1.uri.equals("http://www.w3.org/2000/xmlns/")) {
                    // Ignore XMLNS attributes
                    continue;
                }
                _writer.write("  <attribute qName=\"" + attrsHolder1.qName
                        + "\" localName=\"" + attrsHolder1.localName
                        + "\" uri=\"" + attrsHolder1.uri
                        // + "\" type=\"" + attrsHolder[i].type
                        + "\" value=\"" + attrsHolder1.value + "\"/>\n");
            }
            
            _writer.write("</startElement>\n");
            _writer.flush();
        }
        catch (IOException e) {
            throw new SAXException(e);
        }       
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException 
    {
        try {
            outputCharacters();

            _writer.write("<endElement uri=\"" + uri 
                + "\" localName=\"" + localName + "\" qName=\""
                + qName + "\"/>\n");   
            _writer.flush();

            // Write out the end prefix here rather than waiting
            // for the explicit events
            AttributeValueHolder[] attrsHolder = _namespaceStack.pop();
            if (attrsHolder != null) {
                for (AttributeValueHolder attrsHolder1 : attrsHolder) {
                    _writer.write("<endPrefixMapping prefix=\""
                            + attrsHolder1.localName + "\"/>\n");
                    _writer.flush();
                }
            }
            
        }
        catch (IOException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException 
    {
        if (length == 0) {
            return;
        }
        
        if (_characters == null) {
            _characters = new StringBuffer();
        }
        
        // Coalesce multiple character events
        _characters.append(ch, start, length);
        
        /*
        try {
            _writer.write("<characters>" +
                (_charactersAreCDATA ? "<![CDATA[" : "") +
                new String(ch, start, length) +
                (_charactersAreCDATA ? "]]>" : "") +
                "</characters>\n");
            _writer.flush();
        }
        catch (IOException e) {
            throw new SAXException(e);
        }
         */
    }

    private void outputCharacters() throws SAXException {
        if (_characters == null) {
            return;
        }
        
        try {
            _writer.write("<characters>" +
                (_charactersAreCDATA ? "<![CDATA[" : "") +
                _characters +
                (_charactersAreCDATA ? "]]>" : "") +
                "</characters>\n");
            _writer.flush();

            _characters = null;
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }
    
    @Override
    public void ignorableWhitespace(char[] ch, int start, int length)
            throws SAXException 
    {
        // Report ignorable ws as characters (assumes validation off)
        characters(ch, start, length);
    }

    @Override
    public void processingInstruction(String target, String data)
            throws SAXException 
    {
        try {
            outputCharacters();

            _writer.write("<processingInstruction target=\"" + target 
                + "\" data=\"" + data + "\"/>\n");   
            _writer.flush();
        } 
        catch (IOException e) {
            throw new SAXException(e);
        }
    }
    
    // -- LexicalHandler interface ---------------------------------------

    @Override
    public void startDTD(String name, String publicId, String systemId)
            throws SAXException {
        // Not implemented
    }

    @Override
    public void endDTD()
            throws SAXException {
        // Not implemented
    }

    @Override
    public void startEntity(String name)
            throws SAXException {
        // Not implemented
    }

    @Override
    public void endEntity(String name)
            throws SAXException {
        // Not implemented
    }

    @Override
    public void startCDATA()
            throws SAXException {
        _charactersAreCDATA = true;
    }

    @Override
    public void endCDATA()
            throws SAXException {
        _charactersAreCDATA = false;
    }

    @Override
    public void comment(char[] ch, int start, int length)
            throws SAXException 
    {
        try {
            outputCharacters();

            _writer.write("<comment>" +
                new String(ch, start, length) +
                "</comment>\n");
            _writer.flush();
        }
        catch (IOException e) {
            throw new SAXException(e);
        }
    }
    
    // -- Utility methods ------------------------------------------------
    
    private void quicksort(AttributeValueHolder[] attrs, int p, int r) {
        while (p < r) {
            final int q = partition(attrs, p, r);
            quicksort(attrs, p, q);
            p = q + 1;
        }
    }
                                                                                                                         
    @SuppressWarnings("empty-statement")
    private int partition(AttributeValueHolder[] attrs, int p, int r) {
        AttributeValueHolder x = attrs[(p + r) >>> 1];
        int i = p - 1;
        int j = r + 1;
        while (true) {
            while (x.compareTo(attrs[--j]) < 0);
            while (x.compareTo(attrs[++i]) > 0);
            if (i < j) {
                final AttributeValueHolder t = attrs[i];
                attrs[i] = attrs[j];
                attrs[j] = t;
            }
            else {
                return j;
            }
        }
    }
    
    public static class AttributeValueHolder implements Comparable<AttributeValueHolder> {
        public final String qName;
        public final String localName;
        public final String uri;
        public final String type;
        public final String value;

        public AttributeValueHolder(String qName,
            String localName,
            String uri,
            String type,
            String value)
        {
            this.qName = qName;
            this.localName = localName;
            this.uri = uri;
            this.type = type;
            this.value = value;
        }

        @Override
        public int compareTo(AttributeValueHolder o) {
            try {
                return qName.compareTo(o.qName);
            } catch (Exception e) {
                throw new RuntimeException(CommonResourceBundle.getInstance().getString("message.AttributeValueHolderExpected"));
            }
        }                   

        @Override
        public boolean equals(Object o) {
            try {
                return (o instanceof AttributeValueHolder) &&
                        qName.equals(((AttributeValueHolder) o).qName);
            } catch (Exception e) {
                throw new RuntimeException(CommonResourceBundle.getInstance().getString("message.AttributeValueHolderExpected"));
            }
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + (this.qName != null ? this.qName.hashCode() : 0);
            return hash;
        }
    }

}

