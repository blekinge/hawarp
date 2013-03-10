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
package eu.scape_project.pc.droid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
import org.junit.*;
import static org.junit.Assert.*;
import uk.gov.nationalarchives.droid.core.interfaces.IdentificationResult;

/**
 * Test class for the Droid File Format Identification Hadoop Job.
 *
 * @author Sven Schlarb https://github.com/shsdev
 * @version 0.1
 */
public class DroidIdentificationTest {
    
    public static final String SIGNATURE_FILE_V67_URL = "http://www.nationalarchives.gov.uk/documents/DROID_SignatureFile_V67.xml";

    private static DroidIdentification dihj;

    /**
     * Set up.
     * @throws Exception 
     */
    @BeforeClass
    public static void setUpClass() throws Exception {
        URL sigFileV67Url = new URL(SIGNATURE_FILE_V67_URL);
        InputStream sigFileStream = sigFileV67Url.openStream();
        File tmpSigFile = File.createTempFile("tmpsigfile", ".xml");
        FileOutputStream fos = new FileOutputStream(tmpSigFile);
        IOUtils.copy(sigFileStream, fos);
        fos.close();
        dihj = new DroidIdentification(tmpSigFile.getAbsolutePath());
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * 
     * @throws IOException 
     */
    @Test
    public void testIdentifyOdt() throws IOException {
        InputStream odtTestFileStream = DroidIdentificationTest.class.getResourceAsStream("testfile.odt");
        File tmpOdtTestFile = File.createTempFile("odttestfile", ".odt");
        FileOutputStream fos = new FileOutputStream(tmpOdtTestFile);
        IOUtils.copy(odtTestFileStream, fos);
        fos.close();
        IdentificationResult result = dihj.identify(tmpOdtTestFile.getAbsolutePath());
        assertEquals("fmt/290",result.getPuid());
        assertEquals("application/vnd.oasis.opendocument.text",result.getMimeType()); 
        assertEquals("OpenDocument Text",result.getName()); 
    }
    
    /**
     * 
     * @throws IOException 
     */
    @Test
    public void testIdentifyPdf() throws IOException {
        InputStream odtTestFileStream = DroidIdentificationTest.class.getResourceAsStream("testfile.pdf");
        File tmpOdtTestFile = File.createTempFile("pdftestfile", ".pdf");
        FileOutputStream fos = new FileOutputStream(tmpOdtTestFile);
        IOUtils.copy(odtTestFileStream, fos);
        fos.close();
        IdentificationResult result = dihj.identify(tmpOdtTestFile.getAbsolutePath());
        assertEquals("fmt/18",result.getPuid());
        assertEquals("application/pdf",result.getMimeType()); 
        assertEquals("Acrobat PDF 1.4 - Portable Document Format",result.getName()); 
    }
    
}