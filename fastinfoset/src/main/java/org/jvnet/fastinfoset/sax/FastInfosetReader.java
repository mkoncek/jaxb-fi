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

package org.jvnet.fastinfoset.sax;

import java.io.IOException;
import java.io.InputStream;
import org.jvnet.fastinfoset.FastInfosetException;
import org.jvnet.fastinfoset.FastInfosetParser;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.LexicalHandler;

/**
 * Interface for reading an Fast Infoset document using callbacks.
 *
 * <p>FastInfosetReader is the interface that a Fast Infoset parser's
 * SAX2 driver must implement. This interface allows an application to
 * to register Fast Infoset specific event handlers for encoding algorithms.</p>
 *
 * <p>The reception of encoding algorithm events is determined by
 * the registration of:
 * <ul>
 *    <li>A {@link PrimitiveTypeContentHandler}, for the recieving of events,
 *        associated with built-in encoding algorithms, for decoded data that
 *        can be reported as Java primitive types.</li>
 *    <li>A {@link EncodingAlgorithmContentHandler}, for the recieving of events,
 *        associated with built-in and application-defined encoding algorithms, for
 *        decoded data that can be reported as an array of octets or as a Java
 *        Object.</li>
 *    <li>{@link org.jvnet.fastinfoset.EncodingAlgorithm} implementations, for
 *        the receiving of events, associated with application defined algorithms.
 *        for decoded data that shall be reported as a Java Object by way of the
 *        registered EncodingAlgorithmContentHandler.</li>
 * </ul>
 *
 * <p>The reporting of element content events for built-in algorithms
 *    is determimed by the following:
 * <ul>
 *    <li>If a PrimitiveContentHandler is registered then decoded data is reported
 *        as Java primitive types using the corresponding methods on the PrimitiveContentHandler
 *        interface.</li>
 *    <li>If a PrimitiveContentHandler is not registered and a
 *        EncodingAlgorithmContentHandler is registered then decoded data is reported
 *        as Java Objects using {@link EncodingAlgorithmContentHandler#object(String, int, Object)}.
 *        An Object shall correspond to the Java primitive type that
 *        would otherwise be reported using the PrimitiveContentHandler.</li>
 *    <li>If neither is registered then then decoded data is reported as characters.</li>
 * </ul>
 *
 * <p>The reporting of element content events for application-defined algorithms
 *    is determimed by the following:
 * <ul>
 *    <li>If an EncodingAlgorithmContentHandler is registered and there is no
 *        EncodingAlgorithm registered for an application-defined encoding algorithm
 *        then decoded data for such an algoroithm is reported as an array of octets
 *        using {@link EncodingAlgorithmContentHandler#octets(String, int, byte[], int, int)};
 *        otherwise</li>
 *    <li>If there is an EncodingAlgorithm registered for the application-defined
 *        encoding algorithm then the decoded data is reported as a Java Object,
 *        returned by decoding according to the EncodingAlgorithm, using
 *        {@link EncodingAlgorithmContentHandler#object(String, int, Object)}.</li>
 * </ul>
 *
 * <p>The reporting of attribute values for encoding algorithms is achieved using
 * {@link EncodingAlgorithmAttributes} that extends {@link org.xml.sax.Attributes}.
 * The registered ContentHandler may cast the attr paramter of the
 * {@link org.xml.sax.ContentHandler#startElement(String, String, String, org.xml.sax.Attributes)}
 * to the EncodingAlgorithmAttributes interface to access to encoding algorithm information.
 *
 * <p>The reporting of attribute values for built-in algorithms
 *    is determimed by the following:
 * <ul>
 *    <li>If a PrimitiveContentHandler or EncodingAlgorithmContentHandler is
 *        registered then decoded data is reported as Java Objects corresponding
 *        to the Java primitive types. The Java Objects may be obtained using
 *        {@link EncodingAlgorithmAttributes#getAlgorithmData(int)}.
 *    <li>If neither is registered then then decoded data is reported as characters.</li>
 * </ul>
 *
 * <p>The reporting of attribute values for application-defined algorithms
 *    is determimed by the following:
 * <ul>
 *    <li>If an EncodingAlgorithmContentHandler is registered and there is no
 *        EncodingAlgorithm registered for an application-defined encoding algorithm
 *        then decoded data for such an algoroithm is reported as Java Object,
 *        that is an instance of {@code byte[]},
 *        using {@link EncodingAlgorithmAttributes#getAlgorithmData(int)};
 *        otherwise</li>
 *    <li>If there is an EncodingAlgorithm registered for the application-defined
 *        encoding algorithm then the decoded data is reported as a Java Object,
 *        returned by decoding according to the EncodingAlgorithm, using
 *        {@link EncodingAlgorithmAttributes#getAlgorithmData(int)}.</li>
 * </ul>
 *
 * @see org.jvnet.fastinfoset.sax.PrimitiveTypeContentHandler
 * @see org.jvnet.fastinfoset.sax.EncodingAlgorithmContentHandler
 * @see org.xml.sax.XMLReader
 * @see org.xml.sax.ContentHandler
 */
public interface FastInfosetReader extends XMLReader, FastInfosetParser {
    /**
     * The property name to be used for getting and setting the
     * EncodingAlgorithmContentHandler.
     *
     */
    String ENCODING_ALGORITHM_CONTENT_HANDLER_PROPERTY =
            "http://jvnet.org/fastinfoset/sax/properties/encoding-algorithm-content-handler";

    /**
     * The property name to be used for getting and setting the
     * PrimtiveTypeContentHandler.
     *
     */
    String PRIMITIVE_TYPE_CONTENT_HANDLER_PROPERTY =
            "http://jvnet.org/fastinfoset/sax/properties/primitive-type-content-handler";

    /**
     * Parse a fast infoset document from an InputStream.
     *
     * <p>The application can use this method to instruct the Fast Infoset
     * reader to begin parsing a fast infoset document from a byte stream.</p>
     *
     * <p>Applications may not invoke this method while a parse is in progress
     * (they should create a new XMLReader instead for each nested XML document).
     * Once a parse is complete, an application may reuse the same
     * FastInfosetReader object, possibly with a different byte stream.</p>
     *
     * <p>During the parse, the FastInfosetReader will provide information about
     * the fast infoset document through the registered event handlers.</p>
     *
     * <p> This method is synchronous: it will not return until parsing has ended.
     * If a client application wants to terminate parsing early, it should throw
     * an exception.</p>
     *
     * @param s The byte stream to parse from.
     */
    void parse(InputStream s) throws IOException, FastInfosetException, SAXException;

    /**
     * Allow an application to register a lexical handler.
     *
     * <p>Applications may register a new or different handler in the
     * middle of a parse, and the SAX parser must begin using the new
     * handler immediately.</p>
     *
     * @param handler The lexical handler.
     * @see #getLexicalHandler
     */
    void setLexicalHandler(LexicalHandler handler);

    /**
     * Return the current lexical handler.
     *
     * @return The current lexical handler, or null if none
     *         has been registered.
     * @see #setLexicalHandler
     */
    LexicalHandler getLexicalHandler();

    /**
     * Allow an application to register a DTD declaration handler.
     *
     * <p>Applications may register a new or different handler in the
     * middle of a parse, and the SAX parser must begin using the new
     * handler immediately.</p>
     *
     * @param handler The DTD declaration handler.
     * @see #getLexicalHandler
     */
    void setDeclHandler(DeclHandler handler);

    /**
     * Return the current DTD declaration handler.
     *
     * @return The current DTD declaration handler, or null if none
     *         has been registered.
     * @see #setLexicalHandler
     */
    DeclHandler getDeclHandler();

    /**
     * Allow an application to register an encoding algorithm handler.
     *
     * <p>Applications may register a new or different handler in the
     * middle of a parse, and the SAX parser must begin using the new
     * handler immediately.</p>
     *
     * @param handler The encoding algorithm handler.
     * @see #getEncodingAlgorithmContentHandler
     */
    void setEncodingAlgorithmContentHandler(EncodingAlgorithmContentHandler handler);

    /**
     * Return the current encoding algorithm handler.
     *
     * @return The current encoding algorithm handler, or null if none
     *         has been registered.
     * @see #setEncodingAlgorithmContentHandler
     */
    EncodingAlgorithmContentHandler getEncodingAlgorithmContentHandler();

    /**
     * Allow an application to register a primitive type handler.
     *
     * <p>Applications may register a new or different handler in the
     * middle of a parse, and the SAX parser must begin using the new
     * handler immediately.</p>
     *
     * @param handler The primitive type handler.
     * @see #getPrimitiveTypeContentHandler
     */
    void setPrimitiveTypeContentHandler(PrimitiveTypeContentHandler handler);


    /**
     * Return the current primitive type handler.
     *
     * @return The current primitive type handler, or null if none
     *         has been registered.
     * @see #setPrimitiveTypeContentHandler
     */
    PrimitiveTypeContentHandler getPrimitiveTypeContentHandler();
}
