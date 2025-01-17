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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.sun.xml.fastinfoset.CommonResourceBundle;
import java.net.URI;
import java.net.URISyntaxException;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public abstract class TransformInputOutput {
    
    /** Creates a new instance of TransformInputOutput */
    protected TransformInputOutput() {
    }
    
    public void parse(String[] args) throws Exception {
        InputStream in = null;
        OutputStream out = null;
        if (args.length == 0) {
            in = new BufferedInputStream(System.in);
            out = new BufferedOutputStream(System.out);
        } else if (args.length == 1) {
            in = new BufferedInputStream(new FileInputStream(args[0]));
            out = new BufferedOutputStream(System.out);
        } else if (args.length == 2) {
            in = new BufferedInputStream(new FileInputStream(args[0]));
            out = new BufferedOutputStream(new FileOutputStream(args[1]));
        } else {
            throw new IllegalArgumentException(CommonResourceBundle.getInstance().getString("message.optinalFileNotSpecified"));
        }
        
        parse(in, out);
    }
    
    abstract public void parse(InputStream in, OutputStream out) throws Exception;
    
    // parse alternative with current working directory parameter
    // is used to process xml documents, which have external imported entities
    public void parse(InputStream in, OutputStream out, String workingDirectory) throws Exception {
        throw new UnsupportedOperationException();
    }
        
    private static URI currentJavaWorkingDirectory;
    static {
        currentJavaWorkingDirectory = new File(System.getProperty("user.dir")).toURI();
    }

    protected static EntityResolver createRelativePathResolver(final String workingDirectory) {
        return new EntityResolver() {
            @Override
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                if (systemId != null && systemId.startsWith("file:/")) {
                    URI workingDirectoryURI = new File(workingDirectory).toURI();
                    URI workingFile;
                    try {
                        // Construction new File(new URI(String)).toURI() is used to be sure URI has correct representation without redundant '/'
                        workingFile = convertToNewWorkingDirectory(currentJavaWorkingDirectory, workingDirectoryURI, new File(new URI(systemId)).toURI());
                        return new InputSource(workingFile.toString());
                    } catch (URISyntaxException ex) {
                        //Should not get here
                    }
                }
                return null;
            }
        };
    }
    
    private static URI convertToNewWorkingDirectory(URI oldwd, URI newwd, URI file) throws IOException, URISyntaxException {
        String oldwdStr = oldwd.toString();
        String newwdStr = newwd.toString();
        String fileStr = file.toString();
        
        String cmpStr = null;
        // In simpliest case <user.dir>/file.xml - do it faster
        if (fileStr.startsWith(oldwdStr) && (cmpStr = fileStr.substring(oldwdStr.length())).indexOf('/') == -1) {
            return new URI(newwdStr + '/' + cmpStr);
        }
        
        String[] oldwdSplit = oldwdStr.split("/");
        String[] newwdSplit = newwdStr.split("/");
        String[] fileSplit = fileStr.split("/");
        
        int diff;
        for (diff = 0; diff < oldwdSplit.length && diff < fileSplit.length; diff++) {
            if (!oldwdSplit[diff].equals(fileSplit[diff])) {
                break;
            }
        }
        
        int diffNew;
        for(diffNew=0; diffNew<newwdSplit.length && diffNew<fileSplit.length; diffNew++) {
            if (!newwdSplit[diffNew].equals(fileSplit[diffNew])) {
                break;
            }
        }
        
        //Workaround for case, when extrnal imported entity has imports other entity
        //in that case systemId has correct path, not based on user.dir
        if (diffNew > diff) {
            return file;
        }

        int elemsToSub = oldwdSplit.length - diff;
        StringBuilder resultStr = new StringBuilder(100);
        for(int i=0; i<newwdSplit.length - elemsToSub; i++) {
            resultStr.append(newwdSplit[i]);
            resultStr.append('/');
        }
                
        for(int i=diff; i<fileSplit.length; i++) {
            resultStr.append(fileSplit[i]);
            if (i < fileSplit.length - 1) resultStr.append('/');
        }
        
        return new URI(resultStr.toString());
    }
}
