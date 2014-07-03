/*
 * Copyright 2014 scape.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.scape_project.hawarp.interfaces;

import java.io.IOException;

/**
 * Identifier. Interface to be implemented by identifiers.
 *
 * @author shsdev https://github.com/shsdev
 */
public interface Identifier {
    
    public static final String MIME_UNKNOWN = "no-type";
    
    public String identify(byte[] prefix);
    
    public String identify(String filePath) throws IOException;
    
    public String getCurrentItemId();

    public void setCurrentItemId(String currentItemId);
    
    
}
