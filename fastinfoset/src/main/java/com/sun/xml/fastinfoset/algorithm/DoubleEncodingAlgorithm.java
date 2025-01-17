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



public class DoubleEncodingAlgorithm extends IEEE754FloatingPointEncodingAlgorithm {

    public DoubleEncodingAlgorithm() {
    }

    @Override
    public final int getPrimtiveLengthFromOctetLength(int octetLength) throws EncodingAlgorithmException {
        if (octetLength % DOUBLE_SIZE != 0) {
            throw new EncodingAlgorithmException(CommonResourceBundle.getInstance().
                    getString("message.lengthIsNotMultipleOfDouble", new Object[]{DOUBLE_SIZE}));
        }
        
        return octetLength / DOUBLE_SIZE;
    }
    
    @Override
    public int getOctetLengthFromPrimitiveLength(int primitiveLength) {
        return primitiveLength * DOUBLE_SIZE;
    }
   
    @Override
    public final Object decodeFromBytes(byte[] b, int start, int length) throws EncodingAlgorithmException {
        double[] data = new double[getPrimtiveLengthFromOctetLength(length)];
        decodeFromBytesToDoubleArray(data, 0, b, start, length);
        
        return data;
    }
    
    @Override
    public final Object decodeFromInputStream(InputStream s) throws IOException {
        return decodeFromInputStreamToDoubleArray(s);
    }
    
    
    @Override
    public void encodeToOutputStream(Object data, OutputStream s) throws IOException {
        if (!(data instanceof double[])) {
            throw new IllegalArgumentException(CommonResourceBundle.getInstance().getString("message.dataNotDouble"));
        }
        
        final double[] fdata = (double[])data;
        
        encodeToOutputStreamFromDoubleArray(fdata, s);
    }
    
    @Override
    public final Object convertFromCharacters(char[] ch, int start, int length) {
        final CharBuffer cb = CharBuffer.wrap(ch, start, length);
        final List<Double> doubleList = new ArrayList<>();
        
        matchWhiteSpaceDelimnatedWords(cb, (int start1, int end) -> {
            String fStringValue = cb.subSequence(start1, end).toString();
            doubleList.add(Double.valueOf(fStringValue));
        });
        
        return generateArrayFromList(doubleList);
    }
    
    @Override
    public final void convertToCharacters(Object data, StringBuffer s) {
        if (!(data instanceof double[])) {
            throw new IllegalArgumentException(CommonResourceBundle.getInstance().getString("message.dataNotDouble"));
        }
        
        final double[] fdata = (double[])data;
        
        convertToCharactersFromDoubleArray(fdata, s);
    }
    
    
    public final void decodeFromBytesToDoubleArray(double[] data, int fstart, byte[] b, int start, int length) {
        final int size = length / DOUBLE_SIZE;
        for (int i = 0; i < size; i++) {
            final long bits =
                    ((long)(b[start++] & 0xFF) << 56) | 
                    ((long)(b[start++] & 0xFF) << 48) | 
                    ((long)(b[start++] & 0xFF) << 40) | 
                    ((long)(b[start++] & 0xFF) << 32) | 
                    ((long)(b[start++] & 0xFF) << 24) | 
                    ((long)(b[start++] & 0xFF) << 16) | 
                    ((long)(b[start++] & 0xFF) << 8) | 
                    (long)(b[start++] & 0xFF);
            data[fstart++] = Double.longBitsToDouble(bits);
        }
    }
    
    public final double[] decodeFromInputStreamToDoubleArray(InputStream s) throws IOException {
        final List<Double> doubleList = new ArrayList<>();
        final byte[] b = new byte[DOUBLE_SIZE];
        
        while (true) {
            int n = s.read(b);
            if (n != DOUBLE_SIZE) {
                if (n == -1) {
                    break;
                }
                
                while(n != DOUBLE_SIZE) {
                    final int m = s.read(b, n, DOUBLE_SIZE - n);
                    if (m == -1) {
                        throw new EOFException();
                    }
                    n += m;
                }
            }
            
            final long bits = 
                    ((long)(b[0] & 0xFF) << 56) | 
                    ((long)(b[1] & 0xFF) << 48) | 
                    ((long)(b[2] & 0xFF) << 40) | 
                    ((long)(b[3] & 0xFF) << 32) | 
                    ((b[4] & 0xFF) << 24) | 
                    ((b[5] & 0xFF) << 16) | 
                    ((b[6] & 0xFF) << 8) | 
                    (b[7] & 0xFF);
            
            doubleList.add(Double.longBitsToDouble(bits));
        }
        
        return generateArrayFromList(doubleList);
    }
    
    
    public final void encodeToOutputStreamFromDoubleArray(double[] fdata, OutputStream s) throws IOException {
        for (int i = 0; i < fdata.length; i++) {
            final long bits = Double.doubleToLongBits(fdata[i]);
            s.write((int)((bits >>> 56) & 0xFF));
            s.write((int)((bits >>> 48) & 0xFF));
            s.write((int)((bits >>> 40) & 0xFF));
            s.write((int)((bits >>> 32) & 0xFF));
            s.write((int)((bits >>> 24) & 0xFF));
            s.write((int)((bits >>> 16) & 0xFF));
            s.write((int)((bits >>>  8) & 0xFF));
            s.write((int)(bits & 0xFF));
        }
    }
    
    @Override
    public final void encodeToBytes(Object array, int astart, int alength, byte[] b, int start) {
        encodeToBytesFromDoubleArray((double[])array, astart, alength, b, start);
    }
    
    public final void encodeToBytesFromDoubleArray(double[] fdata, int fstart, int flength, byte[] b, int start) {
        final int fend = fstart + flength;
        for (int i = fstart; i < fend; i++) {
            final long bits = Double.doubleToLongBits(fdata[i]);
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
    
    
    public final void convertToCharactersFromDoubleArray(double[] fdata, StringBuffer s) {
        final int end = fdata.length - 1;
        for (int i = 0; i <= end; i++) {
            s.append(fdata[i]);
            if (i != end) {
                s.append(' ');
            }
        }
    }
    
    
    public final double[] generateArrayFromList(List<Double> array) {
        double[] fdata = new double[array.size()];
        for (int i = 0; i < fdata.length; i++) {
            fdata[i] = array.get(i);
        }
        
        return fdata;
    }
    
}
