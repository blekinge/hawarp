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
package eu.scape_project.cdx_creator;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvGenerator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.impl.CsvWriter;
import eu.scape_project.cdx_creator.cli.CDXCreatorConfig;
import eu.scape_project.hawarp.interfaces.ArchiveReader;
import eu.scape_project.hawarp.mapreduce.JwatArcReaderFactory;
import eu.scape_project.hawarp.utils.StringUtils;
import eu.scape_project.hawarp.webarchive.ArchiveReaderFactory;
import eu.scape_project.hawarp.webarchive.ArchiveRecord;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jwat.arc.ArcReader;
import org.jwat.arc.ArcRecordBase;

/**
 *
 * @author onbscs
 */
public class CDXCreationTask {

    private static final Log LOG = LogFactory.getLog(CDXCreationTask.class);

    private final CDXCreatorConfig config;

    private final File archiveFile;

    private final String cdxFileName;

    private final String cdxFilePath;

    public CDXCreationTask(CDXCreatorConfig config, File archiveFile) {
        this.config = config;
        this.archiveFile = archiveFile;
        if (config.isDirectoryInput()) {
            String inputFileName = archiveFile.getName();
            String warcExt = ".cdx.csv";
            cdxFileName = inputFileName + warcExt;
            cdxFilePath = StringUtils.ensureTrailSep(config.getOutputStr()) + cdxFileName;
        } else {
            if (config.getOutputStr() == null) {
                String inputFileName = archiveFile.getName();
                String warcExt = ".cdx.csv";
                cdxFileName = inputFileName + warcExt;
                cdxFilePath = StringUtils.ensureTrailSep(archiveFile.getAbsolutePath()) + cdxFileName;
            } else {
                cdxFilePath = config.getOutputStr();
                if (cdxFilePath.contains(File.separator)) {
                    cdxFileName = cdxFilePath.substring(cdxFilePath.lastIndexOf(File.separator) + 1);
                } else {
                    cdxFileName = cdxFilePath;
                }
            }
        }
    }

    public void createIndex() {
        FileInputStream fileInputStream = null;
        ArchiveReader reader = null;
        FileOutputStream outputStream = null;
        try {
            fileInputStream = new FileInputStream(archiveFile);
            reader = ArchiveReaderFactory.getReader(fileInputStream);

            List<ArchiveRecord> cdxArchRecords = new ArrayList<ArchiveRecord>();
            while (reader.hasNext()) {
                ArchiveRecord cdxArchRec = (ArchiveRecord) reader.next();
                cdxArchRecords.add(cdxArchRec);
            }

            CsvMapper mapper = new CsvMapper();
            CsvSchema schema = mapper.schemaFor(CdxArchiveRecord.class);
            
            schema = schema.withColumnSeparator('\t');

            ObjectWriter myObjectWriter = mapper.writer(schema);
            File tmpFile = new File("/home/onbscs/test.csv");
            FileOutputStream tempFileOutputStream = new FileOutputStream(tmpFile);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(tempFileOutputStream, 1024);
            OutputStreamWriter writerOutputStream = new OutputStreamWriter(bufferedOutputStream, "UTF-8");

            myObjectWriter.writeValue(writerOutputStream, cdxArchRecords);

            LOG.info("File processed: " + archiveFile.getAbsolutePath());
        } catch (FileNotFoundException ex) {
            LOG.error("File not found error", ex);
        } catch (IOException ex) {
            LOG.error("I/O Error", ex);
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }

            } catch (IOException ex) {
                LOG.error("I/O Error", ex);
            }
        }
    }

}
