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

public final class RestrictedAlphabet {
    
    public static final String NUMERIC_CHARACTERS = "0123456789-+.E ";
    
    public static final int NUMERIC_CHARACTERS_INDEX = 0;
    
    public static final String DATE_TIME_CHARACTERS = "0123456789-:TZ ";

    public static final int DATE_TIME_CHARACTERS_INDEX = 1;

    private RestrictedAlphabet() {
    }
}
