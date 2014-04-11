/*
 * Copyright 2012 The SCAPE Project Consortium.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * under the License.
 */
package eu.scape_project.arc2warc.warc;

import static eu.scape_project.hawarp.interfaces.Identifier.MIME_UNKNOWN;
import eu.scape_project.hawarp.mapreduce.HadoopWebArchiveRecord;
import static eu.scape_project.hawarp.utils.UUIDGenerator.getRecordID;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jwat.warc.WarcRecord;
import org.jwat.warc.WarcWriter;
import java.net.URISyntaxException;

/**
 * Creating WARC records using JWAT. This class creates WARC records using JWAT
 * ARC record writer, see https://sbforge.org/display/JWAT/JWAT-Tools.
 *
 * @author Sven Schlarb <https://github.com/shsdev>
 */
public class WarcCreator {

    private static final Log LOG = LogFactory.getLog(WarcCreator.class);

    protected WarcWriter writer;
    
    protected String fileName;

    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    
    private boolean isArcMetadataRecord;

    private boolean payloadIdentification;
    
    private String warcInfoId;

    private WarcCreator() {
    }

    public WarcCreator(WarcWriter writer, String fileName) {
        this.writer = writer;
        this.fileName = fileName;
        isArcMetadataRecord = true;
    }

    public void close() throws IOException {
        writer.close();
    }
    
    public void createWarcInfoRecord() throws IOException, URISyntaxException {
        WarcRecord record = WarcRecord.createRecord(writer);
        record.header.addHeader("WARC-Type", "warcinfo");
        record.header.addHeader("WARC-Date", sdf.format(Calendar.getInstance().getTime()));
        warcInfoId = getRecordID().toString();
        record.header.addHeader("WARC-Record-ID", warcInfoId);
        record.header.addHeader("WARC-Filename", fileName);
        record.header.addHeader("Content-Type", "application/warc-fields");
        String description = "software: JWAT Version 1.0.0 https://sbforge.org/display/JWAT/JWAT-Tools\n"
                + "description: migrated from ARC "
                + "format: WARC file version 1.0";
        byte[] descriptionBytes = description.getBytes();
        record.header.addHeader("Content-Length", Long.toString(descriptionBytes.length));
        writer.writeHeader(record);
        ByteArrayInputStream inBytes = new ByteArrayInputStream(descriptionBytes);
        writer.streamPayload(inBytes);
        writer.closeRecord();
    }

    public void createContentRecord(HadoopWebArchiveRecord arcRecord) throws IOException, URISyntaxException {
        WarcRecord record = WarcRecord.createRecord(writer);
        String recordId = getRecordID().toString();
        String arcRecordMime = arcRecord.getMimeType();
        String mimeType = (arcRecordMime != null) ? arcRecordMime : MIME_UNKNOWN;
        if(isArcMetadataRecord) mimeType = "text/xml";
        String type = (isArcMetadataRecord) ? "metadata" : "response";
        if(mimeType.equals("text/dns")) {
            type = "resource";
        }
        record.header.addHeader("WARC-Type", type);
        record.header.addHeader("WARC-Target-URI", arcRecord.getUrl());
        record.header.addHeader("WARC-Date", sdf.format(arcRecord.getDate()));
        record.header.addHeader("WARC-Record-ID", recordId);
        if(isArcMetadataRecord) {
            record.header.addHeader("WARC-Concurrent-To", warcInfoId);
        }
        record.header.addHeader("WARC-IP-Address", arcRecord.getIpAddress());
        record.header.addHeader("Content-Type", mimeType);
        byte[] contents = arcRecord.getContents();
        record.header.addHeader("WARC-Payload-Digest", arcRecord.getPayloadDigestStr());
        record.header.addHeader("WARC-Identified-Payload-Type", arcRecord.getIdentifiedPayloadType());
        record.header.addHeader("Content-Length", Long.toString(contents.length));
        writer.writeHeader(record);
        ByteArrayInputStream inBytes = new ByteArrayInputStream(contents);
        writer.streamPayload(inBytes);
        writer.closeRecord();
        isArcMetadataRecord = false;
    }

    public boolean isPayloadIdentification() {
        return payloadIdentification;
    }

    public void setPayloadIdentification(boolean payloadIdentification) {
        this.payloadIdentification = payloadIdentification;
    }

}
