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

/**
 * @author Paul Sandoz
 * @author Alexey Stashok
 */
public interface OctetBufferListener {
    /**
     * Callback method that will be called before the
     * (@link Decoder) octet buffer content is going to be changed.
     * So it will be possible to preserve a read data by
     * cloning, or perform other actions.
     */
    void onBeforeOctetBufferOverwrite();
}
