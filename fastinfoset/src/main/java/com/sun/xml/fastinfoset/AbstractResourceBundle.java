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

package com.sun.xml.fastinfoset;

import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;


/**
 * This class contains methods common to all *ResourceBundle classes
 *
 * @author FastInfoset team
 */
public abstract class AbstractResourceBundle extends ResourceBundle {
        
    public static final String LOCALE = "com.sun.xml.fastinfoset.locale";

    protected AbstractResourceBundle() {
    }

    /**
     * Gets 'key' from ResourceBundle and format message using 'args'.
     *
     * @param key String key for message.
     * @param args Array of arguments for message.
     * @return String formatted message.
     */
    public String getString(String key, Object[] args) {
        String pattern = getBundle().getString(key);
        return MessageFormat.format(pattern, args);
    }

    /**
     * Parse a locale string, return corresponding Locale instance.
     *
     * @param localeString
     * Name for the locale of interest.  If null, use VM default locale.
     * @return New Locale instance.
     */
    public static Locale parseLocale(String localeString) {        
        Locale locale = null;
        if (localeString == null) {
            locale = Locale.getDefault();
        } else {
            try {
                String[] args = localeString.split("_");
                switch (args.length) {
                    case 1:
                        locale = new Locale.Builder().setLanguage(args[0]).build();
                        break;
                    case 2:
                        locale = new Locale.Builder().setLanguage(args[0]).setRegion(args[1]).build();
                        break;
                    case 3:
                        locale = new Locale.Builder().setLanguage(args[0]).setRegion(args[1]).setVariant(args[2]).build();
                        break;
                    default:
                        break;
                }
            } catch (Throwable t) {
                locale = Locale.getDefault();
            }
        }
        return locale;
    }
    
    /**
     * Subclasses of this class must implement this method so that the 
     * correct resource bundle is passed to methods in this class
     *
     * @return
     *  A java.util.ResourceBundle from the subclass. Methods in this class
     *  will use this reference.
     */
    public abstract ResourceBundle getBundle();
    

    /**
     * Since we are changing the ResourceBundle extension point, must implement
     * handleGetObject() using delegate getBundle().Uses getObject() call to
     * work around protected access to ResourceBundle.handleGetObject().
     * Happily, this means parent tree of delegate bundle is searched for a
     * match.
     * <p>
     * Implements java.util.ResourceBundle.handleGetObject; inherits that
     * javadoc information.
     */
    @Override
    protected Object handleGetObject(String key) {
       return getBundle().getObject(key);
    }

    /**
     * Since we are changing the ResourceBundle extension point, must implement
     * getKeys() using delegate getBundle().Implements
     * java.util.ResourceBundle.getKeys; inherits that javadoc information.
     */
    @Override
    public final Enumeration<String> getKeys() {
       return getBundle().getKeys();
    }
}
