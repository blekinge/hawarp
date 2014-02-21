/*
 *  Copyright 2012 The SCAPE Project Consortium.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package eu.scape_project.droid_identify.droid;

import eu.scape_project.droid_identify.hadoop.DroidIdentifyHadoopJob;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.gov.nationalarchives.droid.core.SignatureParseException;
import uk.gov.nationalarchives.droid.core.BinarySignatureIdentifier;

import uk.gov.nationalarchives.droid.core.interfaces.IdentificationRequest;
import uk.gov.nationalarchives.droid.core.interfaces.IdentificationResult;
import uk.gov.nationalarchives.droid.core.interfaces.IdentificationResultCollection;
import uk.gov.nationalarchives.droid.core.interfaces.RequestIdentifier;
import uk.gov.nationalarchives.droid.core.interfaces.resource.FileSystemIdentificationRequest;
import uk.gov.nationalarchives.droid.core.interfaces.resource.RequestMetaData;

/**
 * Droid File Format Identification. File format identification using the Droid
 * version 6.1 API. A DroidIdentification object can be initialised with a
 * default signature file version 67 using the default constructor or with
 * another signature file using the constructor that allows to give a path to
 * another signature file. Identification can be performed using a file or an
 * input stream.
 *
 * @author Sven Schlarb https://github.com/shsdev
 * @version 0.1
 */
public class DroidIdentification {

    private static final Log LOG = LogFactory.getLog(DroidIdentification.class);
    public static final String SIGNATURE_FILE_V67_URL = "http://www.nationalarchives.gov.uk/documents/DROID_SignatureFile_V67.xml";
    private String sigFilePath;
    private BinarySignatureIdentifier bsi;
    // Singleton Instance
    private static DroidIdentification instance = null;

    /**
     * Get instance with default signature file
     *
     * @return DroidIdentification instance
     * @throws IOException
     * @throws SignatureParseException
     */
    public static DroidIdentification getInstance() throws IOException, SignatureParseException {
        if (instance == null) {
            instance = new DroidIdentification();
        }
        return instance;
    }

    /**
     * Get instance with path to signature file
     *
     * @param sigFilePath Path to signature file
     * @return DroidIdentification instance
     * @throws IOException
     * @throws SignatureParseException
     */
    public static DroidIdentification getInstance(String sigFilePath) throws IOException, SignatureParseException {
        // reset instance if new signature file is used
        if (instance != null && !instance.sigFilePath.equals(sigFilePath)) {
            instance = null;
        }
        if (instance == null) {
            instance = new DroidIdentification(sigFilePath);
        }
        return instance;
    }

    /**
     * Constructor which initialises a default signature file (v67)
     *
     * @throws IOException
     * @throws SignatureParseException
     */
    public DroidIdentification() throws IOException, SignatureParseException {
        URL sigFileV67Url = new URL(SIGNATURE_FILE_V67_URL);
        InputStream sigFileStream = sigFileV67Url.openStream();
        File tmpSigFile = File.createTempFile("tmpsigfile", ".xml");
        FileOutputStream fos = new FileOutputStream(tmpSigFile);
        IOUtils.copy(sigFileStream, fos);
        fos.close();
        sigFilePath = tmpSigFile.getAbsolutePath();
        this.init();
    }

    /**
     * Constructor which initialises a given specified signature file
     *
     * @param sigFilePath
     * @throws SignatureParseException
     */
    private DroidIdentification(String sigFilePath) throws SignatureParseException {
        this.sigFilePath = sigFilePath;
        this.init();
    }

    /**
     * Initialise signature parser
     *
     * @throws SignatureParseException
     */
    private void init() throws SignatureParseException {
        bsi = new BinarySignatureIdentifier();
        bsi.setSignatureFile(sigFilePath);
        bsi.init();
    }

    /**
     * Run droid identification on file
     *
     * @param filePath Absolute file path
     * @return Result list
     * @throws FileNotFoundException
     * @throws IOException
     */
    public String identify(String filePath)  {
        InputStream in = null;
        String puid = "fmt/0";
        IdentificationRequest request = null;
        try {
            
            File file = new File(filePath);
            URI resourceUri = file.toURI();
            in = new FileInputStream(file);
            LOG.debug("Identification of resource: " + resourceUri.toString());
            RequestMetaData metaData = new RequestMetaData(file.length(), file.lastModified(), file.getName());
            LOG.debug("File length: " + file.length());
            LOG.debug("File modified: " + file.lastModified());
            LOG.debug("File name: " + file.getName());
            RequestIdentifier identifier = new RequestIdentifier(resourceUri);
            request = new FileSystemIdentificationRequest(metaData, identifier);
            request.open(in);
            IdentificationResultCollection results = bsi.matchBinarySignatures(request);
            bsi.removeLowerPriorityHits(results);
            if (results == null || results.getResults() == null || results.getResults().isEmpty()) {
                LOG.warn("No identification result");
            } else {
                List<IdentificationResult> result = results.getResults();
                if (result != null && !result.isEmpty()) {
                    for (IdentificationResult ir : result) {
                        String id = ir.getPuid();
                        if (id != null && !id.isEmpty()) {
                            // take first puid, ignore others
                            puid = id;
                            break;
                        }
                    }
                }
                if (puid.isEmpty()) {
                    puid = "fmt/0"; // unknown
                }
            }
             request.close();
        } catch (IOException ex) {
            LOG.error("I/O Exception", ex);
        } finally {
            try {
                if(request != null) {
                    request.close();
                }
                if(in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                LOG.warn("I/O error when closing stream",ex);
            }
        }
        
        return puid;
    }
}
