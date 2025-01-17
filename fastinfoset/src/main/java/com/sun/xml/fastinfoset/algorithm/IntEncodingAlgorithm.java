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



public class IntEncodingAlgorithm extends IntegerEncodingAlgorithm {

    public IntEncodingAlgorithm() {
    }

    @Override
    public final int getPrimtiveLengthFromOctetLength(int octetLength) throws EncodingAlgorithmException {
        if (octetLength % INT_SIZE != 0) {
            throw new EncodingAlgorithmException(CommonResourceBundle.getInstance().
                    getString("message.lengthNotMultipleOfInt", new Object[]{INT_SIZE}));
        }
        
        return octetLength / INT_SIZE;
    }

    @Override
    public int getOctetLengthFromPrimitiveLength(int primitiveLength) {
        return primitiveLength * INT_SIZE;
    }
    
    @Override
    public final Object decodeFromBytes(byte[] b, int start, int length) throws EncodingAlgorithmException {
        int[] data = new int[getPrimtiveLengthFromOctetLength(length)];
        decodeFromBytesToIntArray(data, 0, b, start, length);
        
        return data;
    }
    
    @Override
    public final Object decodeFromInputStream(InputStream s) throws IOException {
        return decodeFromInputStreamToIntArray(s);
    }
    
    
    @Override
    public void encodeToOutputStream(Object data, OutputStream s) throws IOException {
        if (!(data instanceof int[])) {
            throw new IllegalArgumentException(CommonResourceBundle.getInstance().getString("message.dataNotIntArray"));
        }
        
        final int[] idata = (int[])data;
        
        encodeToOutputStreamFromIntArray(idata, s);
    }
    
    
    @Override
    public final Object convertFromCharacters(char[] ch, int start, int length) {
        final CharBuffer cb = CharBuffer.wrap(ch, start, length);
        final List<Integer> integerList = new ArrayList<>();
        
        matchWhiteSpaceDelimnatedWords(cb, (int start1, int end) -> {
            String iStringValue = cb.subSequence(start1, end).toString();
            integerList.add(Integer.valueOf(iStringValue));
        });
        
        return generateArrayFromList(integerList);
    }
    
    @Override
    public final void convertToCharacters(Object data, StringBuffer s) {
        if (!(data instanceof int[])) {
            throw new IllegalArgumentException(CommonResourceBundle.getInstance().getString("message.dataNotIntArray"));
        }
        
        final int[] idata = (int[])data;
        
        convertToCharactersFromIntArray(idata, s);
    }
    
    
    public final void decodeFromBytesToIntArray(int[] idata, int istart, byte[] b, int start, int length) {
        final int size = length / INT_SIZE;
        for (int i = 0; i < size; i++) {
            idata[istart++] = ((b[start++] & 0xFF) << 24) | 
                    ((b[start++] & 0xFF) << 16) | 
                    ((b[start++] & 0xFF) << 8) | 
                    (b[start++] & 0xFF);
        }        
    }
    
    public final int[] decodeFromInputStreamToIntArray(InputStream s) throws IOException {
        final List<Integer> integerList = new ArrayList<>();
        final byte[] b = new byte[INT_SIZE];
        
        while (true) {
            int n = s.read(b);
            if (n != 4) {
                if (n == -1) {
                    break;
                }
                
                while(n != 4) {
                    final int m = s.read(b, n, INT_SIZE - n);
                    if (m == -1) {
                        throw new EOFException();
                    }
                    n += m;
                }
            }
            
            final int i = ((b[0] & 0xFF) << 24) | 
                    ((b[1] & 0xFF) << 16) | 
                    ((b[2] & 0xFF) << 8) | 
                    (b[3] & 0xFF);
            integerList.add(i);
        }
        
        return generateArrayFromList(integerList);
    }
    
    
    public final void encodeToOutputStreamFromIntArray(int[] idata, OutputStream s) throws IOException {
        for (int i = 0; i < idata.length; i++) {
            final int bits = idata[i];
            s.write((bits >>> 24) & 0xFF);
            s.write((bits >>> 16) & 0xFF);
            s.write((bits >>> 8) & 0xFF);
            s.write(bits & 0xFF);
        }
    }
    
    @Override
    public final void encodeToBytes(Object array, int astart, int alength, byte[] b, int start) {
        encodeToBytesFromIntArray((int[])array, astart, alength, b, start);
    }
    
    public final void encodeToBytesFromIntArray(int[] idata, int istart, int ilength, byte[] b, int start) {
        final int iend = istart + ilength;
        for (int i = istart; i < iend; i++) {
            final int bits = idata[i];
            b[start++] = (byte)((bits >>> 24) & 0xFF);
            b[start++] = (byte)((bits >>> 16) & 0xFF);
            b[start++] = (byte)((bits >>>  8) & 0xFF);
            b[start++] = (byte)(bits & 0xFF);
        }
    }
    
    
    public final void convertToCharactersFromIntArray(int[] idata, StringBuffer s) {
        final int end = idata.length - 1;
        for (int i = 0; i <= end; i++) {
            s.append(idata[i]);
            if (i != end) {
                s.append(' ');
            }
        }
    }
    
    
    public final int[] generateArrayFromList(List<Integer> array) {
        int[] idata = new int[array.size()];
        for (int i = 0; i < idata.length; i++) {
            idata[i] = array.get(i);
        }
        
        return idata;
    }
}
