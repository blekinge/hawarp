/*
 * Copyright 2014 onbscs.
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
package eu.scape_project.hawarp.utils;

import com.google.common.io.Resources;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author onbscs
 */
public class ResourceUtils {

    private static final Log LOG = LogFactory.getLog(ResourceUtils.class);

    public static String getStringFromResource(URL resourceUrl) {
        InputStream resourceInputStream = null;
        StringWriter resContStrWr = null;
        try {
            resourceInputStream = resourceUrl.openStream();
            resContStrWr = new StringWriter();
            org.apache.commons.io.IOUtils.copy(resourceInputStream, resContStrWr, Charset.forName("UTF-8"));
            resourceInputStream.close();
            return resContStrWr.toString();
        } catch (IOException ex) {
            LOG.error("I/O error when reading resource", ex);
        } finally {
            try {
                if (resourceInputStream != null) {
                    resourceInputStream.close();
                }
                if (resContStrWr != null) {
                    resContStrWr.close();
                }
            } catch (IOException ex) {
                LOG.error("I/O error", ex);
            }
        }
        return null;
    }

}
