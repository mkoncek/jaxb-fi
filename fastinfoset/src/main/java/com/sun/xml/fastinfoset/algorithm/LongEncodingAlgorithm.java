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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import org.jvnet.fastinfoset.EncodingAlgorithmException;
import com.sun.xml.fastinfoset.CommonResourceBundle;


public class LongEncodingAlgorithm extends IntegerEncodingAlgorithm {

    public LongEncodingAlgorithm() {
    }

    @Override
    public int getPrimtiveLengthFromOctetLength(int octetLength) throws EncodingAlgorithmException {
        if (octetLength % LONG_SIZE != 0) {
            throw new EncodingAlgorithmException(CommonResourceBundle.getInstance().
                    getString("message.lengthNotMultipleOfLong", new Object[]{LONG_SIZE}));
        }
        
        return octetLength / LONG_SIZE;
    }

    @Override
    public int getOctetLengthFromPrimitiveLength(int primitiveLength) {
        return primitiveLength * LONG_SIZE;
    }
    
    @Override
    public final Object decodeFromBytes(byte[] b, int start, int length) throws EncodingAlgorithmException {
        long[] data = new long[getPrimtiveLengthFromOctetLength(length)];
        decodeFromBytesToLongArray(data, 0, b, start, length);
        
        return data;
    }
    
    @Override
    public final Object decodeFromInputStream(InputStream s) throws IOException {
        return decodeFromInputStreamToIntArray(s);
    }
    
    
    @Override
    public void encodeToOutputStream(Object data, OutputStream s) throws IOException {
        if (!(data instanceof long[])) {
            throw new IllegalArgumentException(CommonResourceBundle.getInstance().getString("message.dataNotLongArray"));
        }
        
        final long[] ldata = (long[])data;
        
        encodeToOutputStreamFromLongArray(ldata, s);
    }
    
    
    @Override
    public Object convertFromCharacters(char[] ch, int start, int length) {
        final CharBuffer cb = CharBuffer.wrap(ch, start, length);
        final List<Long> longList = new ArrayList<>();
        
        matchWhiteSpaceDelimnatedWords(cb, (int start1, int end) -> {
            String lStringValue = cb.subSequence(start1, end).toString();
            longList.add(Long.valueOf(lStringValue));
        });
        
        return generateArrayFromList(longList);
    }
    
    @Override
    public void convertToCharacters(Object data, StringBuffer s) {
        if (!(data instanceof long[])) {
            throw new IllegalArgumentException(CommonResourceBundle.getInstance().getString("message.dataNotLongArray"));
        }
        
        final long[] ldata = (long[])data;
        
        convertToCharactersFromLongArray(ldata, s);
    }
    
    
    public final void decodeFromBytesToLongArray(long[] ldata, int istart, byte[] b, int start, int length) {
        final int size = length / LONG_SIZE;
        for (int i = 0; i < size; i++) {
            ldata[istart++] = 
                    ((long)(b[start++] & 0xFF) << 56) | 
                    ((long)(b[start++] & 0xFF) << 48) | 
                    ((long)(b[start++] & 0xFF) << 40) | 
                    ((long)(b[start++] & 0xFF) << 32) | 
                    ((long)(b[start++] & 0xFF) << 24) | 
                    ((long)(b[start++] & 0xFF) << 16) | 
                    ((long)(b[start++] & 0xFF) << 8) | 
                    (long)(b[start++] & 0xFF);
        }        
    }
    
    public final long[] decodeFromInputStreamToIntArray(InputStream s) throws IOException {
        final List<Long> longList = new ArrayList<>();
        final byte[] b = new byte[LONG_SIZE];
        
        while (true) {
            int n = s.read(b);
            if (n != LONG_SIZE) {
                if (n == -1) {
                    break;
                }
                
                while(n != LONG_SIZE) {
                    final int m = s.read(b, n, LONG_SIZE - n);
                    if (m == -1) {
                        throw new EOFException();
                    }
                    n += m;
                }
            }
            
            final long l =
                    (((long) b[0] << 56) +
                    ((long) (b[1] & 0xFF) << 48) +
                    ((long) (b[2] & 0xFF) << 40) +
                    ((long) (b[3] & 0xFF) << 32) +
                    ((long) (b[4] & 0xFF) << 24) +
                    ((b[5] & 0xFF) << 16) +
                    ((b[6] & 0xFF) << 8) +
                    ((b[7] & 0xFF)));

            longList.add(l);
        }
        
        return generateArrayFromList(longList);
    }
    
    
    public final void encodeToOutputStreamFromLongArray(long[] ldata, OutputStream s) throws IOException {
        for (int i = 0; i < ldata.length; i++) {
            final long bits = ldata[i];
            s.write((int)((bits >>> 56) & 0xFF));
            s.write((int)((bits >>> 48) & 0xFF));
            s.write((int)((bits >>> 40) & 0xFF));
            s.write((int)((bits >>> 32) & 0xFF));
            s.write((int)((bits >>> 24) & 0xFF));
            s.write((int)((bits >>> 16) & 0xFF));
            s.write((int)((bits >>> 8) & 0xFF));
            s.write((int)(bits & 0xFF));
        }
    }
    
    @Override
    public final void encodeToBytes(Object array, int astart, int alength, byte[] b, int start) {
        encodeToBytesFromLongArray((long[])array, astart, alength, b, start);
    }
    
    public final void encodeToBytesFromLongArray(long[] ldata, int lstart, int llength, byte[] b, int start) {
        final int lend = lstart + llength;
        for (int i = lstart; i < lend; i++) {
            final long bits = ldata[i];
            b[start++] = (byte)((bits >>> 56) & 0xFF);
            b[start++] = (byte)((bits >>> 48) & 0xFF);
            b[start++] = (byte)((bits >>> 40) & 0xFF);
            b[start++] = (byte)((bits >>> 32) & 0xFF);
            b[start++] = (byte)((bits >>> 24) & 0xFF);
            b[start++] = (byte)((bits >>> 16) & 0xFF);
            b[start++] = (byte)((bits >>>  8) & 0xFF);
            b[start++] = (byte)(bits & 0xFF);
        }
    }
    
    
    public final void convertToCharactersFromLongArray(long[] ldata, StringBuffer s) {
        final int end = ldata.length - 1;        
        for (int i = 0; i <= end; i++) {
            s.append(ldata[i]);
            if (i != end) {
                s.append(' ');
            }
        }
    }
    
    
    public final long[] generateArrayFromList(List<Long> array) {
        long[] ldata = new long[array.size()];
        for (int i = 0; i < ldata.length; i++) {
            ldata[i] = array.get(i);
        }
        
        return ldata;
    }
}
