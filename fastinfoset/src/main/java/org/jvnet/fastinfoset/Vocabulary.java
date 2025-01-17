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

package org.jvnet.fastinfoset;

import java.util.LinkedHashSet;
import java.util.Set;
import javax.xml.namespace.QName;

/**
 * A canonical representation of a vocabulary.
 * <p>
 * Each vocabulary table is represented as a Set. A vocabulary table entry is
 * represented as an item in the Set.
 * <p>
 * The 1st item contained in a Set is assigned the smallest index value,
 * n say (where n {@literal >}= 0). The 2nd item is assigned an index value of n + 1. The kth
 * item is assigned an index value of n + (k - 1).
 * <p>
 * A Fast Infoset parser/serializer implementation will tranform the canonical
 * representation of a Vocabulary instance into a more optimal form suitable
 * for the efficient usage according to the API implemented by the parsers and
 * serializers.
 */
public class Vocabulary {
    /**
     * The restricted alphabet table, containing String objects.
     */
    public final Set<String> restrictedAlphabets = new LinkedHashSet<>();

    /**
     * The encoding algorithm table, containing String objects.
     */
    public final Set<String> encodingAlgorithms = new LinkedHashSet<>();

    /**
     * The prefix table, containing String objects.
     */
    public final Set<String> prefixes = new LinkedHashSet<>();

    /**
     * The namespace name table, containing String objects.
     */
    public final Set<String> namespaceNames = new LinkedHashSet<>();

    /**
     * The local name table, containing String objects.
     */
    public final Set<String> localNames = new LinkedHashSet<>();

    /**
     * The "other NCName" table, containing String objects.
     */
    public final Set<String> otherNCNames = new LinkedHashSet<>();

    /**
     * The "other URI" table, containing String objects.
     */
    public final Set<String> otherURIs = new LinkedHashSet<>();

    /**
     * The "attribute value" table, containing String objects.
     */
    public final Set<String> attributeValues = new LinkedHashSet<>();

    /**
     * The "other string" table, containing String objects.
     */
    public final Set<String> otherStrings = new LinkedHashSet<>();

    /**
     * The "character content chunk" table, containing String objects.
     */
    public final Set<String> characterContentChunks = new LinkedHashSet<>();

    /**
     * The element table, containing QName objects.
     */
    public final Set<QName> elements = new LinkedHashSet<>();

    /**
     * The attribute table, containing QName objects.
     */
    public final Set<QName> attributes = new LinkedHashSet<>();

    public Vocabulary() {
    }
}
