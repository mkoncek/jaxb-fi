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

package com.sun.xml.fastinfoset.algorithm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.jvnet.fastinfoset.EncodingAlgorithmException;
import com.sun.xml.fastinfoset.CommonResourceBundle;

public class BASE64EncodingAlgorithm extends BuiltInEncodingAlgorithm {
    
    /* package */ static final char[] encodeBase64 = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
        'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
        'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
    };

    /* package */ static final int[] decodeBase64 = {
        /*'+'*/ 62,
        -1, -1, -1,
        /*'/'*/ 63,
        /*'0'*/ 52,
        /*'1'*/ 53,
        /*'2'*/ 54,
        /*'3'*/ 55,
        /*'4'*/ 56,
        /*'5'*/ 57,
        /*'6'*/ 58,
        /*'7'*/ 59,
        /*'8'*/ 60,
        /*'9'*/ 61,
        -1, -1, -1, -1, -1, -1, -1,
        /*'A'*/ 0,
        /*'B'*/ 1,
        /*'C'*/ 2,
        /*'D'*/ 3,
        /*'E'*/ 4,
        /*'F'*/ 5,
        /*'G'*/ 6,
        /*'H'*/ 7,
        /*'I'*/ 8,
        /*'J'*/ 9,
        /*'K'*/ 10,
        /*'L'*/ 11,
        /*'M'*/ 12,
        /*'N'*/ 13,
        /*'O'*/ 14,
        /*'P'*/ 15,
        /*'Q'*/ 16,
        /*'R'*/ 17,
        /*'S'*/ 18,
        /*'T'*/ 19,
        /*'U'*/ 20,
        /*'V'*/ 21,
        /*'W'*/ 22,
        /*'X'*/ 23,
        /*'Y'*/ 24,
        /*'Z'*/ 25,
        -1, -1, -1, -1, -1, -1,
        /*'a'*/ 26,
        /*'b'*/ 27,
        /*'c'*/ 28,
        /*'d'*/ 29,
        /*'e'*/ 30,
        /*'f'*/ 31,
        /*'g'*/ 32,
        /*'h'*/ 33,
        /*'i'*/ 34,
        /*'j'*/ 35,
        /*'k'*/ 36,
        /*'l'*/ 37,
        /*'m'*/ 38,
        /*'n'*/ 39,
        /*'o'*/ 40,
        /*'p'*/ 41,
        /*'q'*/ 42,
        /*'r'*/ 43,
        /*'s'*/ 44,
        /*'t'*/ 45,
        /*'u'*/ 46,
        /*'v'*/ 47,
        /*'w'*/ 48,
        /*'x'*/ 49,
        /*'y'*/ 50,
        /*'z'*/ 51
    };

    public BASE64EncodingAlgorithm() {
    }

    @Override
    public final Object decodeFromBytes(byte[] b, int start, int length) throws EncodingAlgorithmException {
        final byte[] data = new byte[length];
        System.arraycopy(b, start, data, 0, length);
        return data;
    }
    
    @Override
    public final Object decodeFromInputStream(InputStream s) throws IOException {
        throw new UnsupportedOperationException(CommonResourceBundle.getInstance().getString("message.notImplemented"));
    }
    
    
    @Override
    public void encodeToOutputStream(Object data, OutputStream s) throws IOException {
        if (!(data instanceof byte[])) {
            throw new IllegalArgumentException(CommonResourceBundle.getInstance().getString("message.dataNotByteArray"));
        }
        
        s.write((byte[])data);
    }
    
    @Override
    public final Object convertFromCharacters(char[] ch, int start, int length) {
        if (length == 0) {
            return new byte[0];
        }
        
        StringBuilder encodedValue = removeWhitespace(ch, start, length);
        int encodedLength = encodedValue.length();
        if (encodedLength == 0) {
            return new byte[0];
        }
        
        int blockCount = encodedLength / 4;
        int partialBlockLength = 3;

        if (encodedValue.charAt(encodedLength - 1) == '=') {
            --partialBlockLength;
            if (encodedValue.charAt(encodedLength - 2) == '=') {
                --partialBlockLength;
            }
        }

        int valueLength = (blockCount - 1) * 3 + partialBlockLength;
        byte[] value = new byte[valueLength];

        int idx = 0;
        int encodedIdx = 0;
        for (int i = 0; i < blockCount; ++i) {
            int x1 = decodeBase64[encodedValue.charAt(encodedIdx++) - '+'];
            int x2 = decodeBase64[encodedValue.charAt(encodedIdx++) - '+'];
            int x3 = decodeBase64[encodedValue.charAt(encodedIdx++) - '+'];
            int x4 = decodeBase64[encodedValue.charAt(encodedIdx++) - '+'];

            value[idx++] = (byte) ((x1 << 2) | (x2 >> 4));
            if (idx < valueLength) {
                value[idx++] = (byte) (((x2 & 0x0f) << 4) | (x3 >> 2));
            }
            if (idx < valueLength) {
                value[idx++] = (byte) (((x3 & 0x03) << 6) | x4);
            }
        }

        return value;
    }
    
    @Override
    public final void convertToCharacters(Object data, StringBuffer s) {
        if (data == null) {
            return;
        }
        final byte[] value = (byte[]) data;
        
        convertToCharacters(value, 0, value.length, s);
    }
    
    @Override
    public final int getPrimtiveLengthFromOctetLength(int octetLength) throws EncodingAlgorithmException {
        return octetLength;
    }

    @Override
    public int getOctetLengthFromPrimitiveLength(int primitiveLength) {
        return primitiveLength;
    }
    
    @Override
    public final void encodeToBytes(Object array, int astart, int alength, byte[] b, int start) {
        System.arraycopy((byte[])array, astart, b, start, alength);
    }    

    public final void convertToCharacters(byte[] data, int offset, int length, StringBuffer s) {
        if (data == null) {
            return;
        }
        final byte[] value = data;
        if (length == 0) {
            return;
        }
        
        final int partialBlockLength = length % 3;
        final int blockCount = (partialBlockLength != 0) ? 
            length / 3 + 1 : 
            length / 3;

        final int encodedLength = blockCount * 4;
        final int originalBufferSize = s.length();
        s.ensureCapacity(encodedLength + originalBufferSize);

        int idx = offset;
        int lastIdx = offset + length;
        for (int i = 0; i < blockCount; ++i) {
            int b1 = value[idx++] & 0xFF;
            int b2 = (idx < lastIdx) ? value[idx++] & 0xFF : 0;
            int b3 = (idx < lastIdx) ? value[idx++] & 0xFF : 0;

            s.append(encodeBase64[b1 >> 2]);

            s.append(encodeBase64[((b1 & 0x03) << 4) | (b2 >> 4)]);

            s.append(encodeBase64[((b2 & 0x0f) << 2) | (b3 >> 6)]);

            s.append(encodeBase64[b3 & 0x3f]);
        }

        switch (partialBlockLength) {
            case 1 :
                s.setCharAt(originalBufferSize + encodedLength - 1, '=');
                s.setCharAt(originalBufferSize + encodedLength - 2, '=');
                break;
            case 2 :
                s.setCharAt(originalBufferSize + encodedLength - 1, '=');
                break;
        }
    }
}
