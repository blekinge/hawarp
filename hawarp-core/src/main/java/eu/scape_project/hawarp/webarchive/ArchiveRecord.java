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
package eu.scape_project.hawarp.webarchive;

import static eu.scape_project.hawarp.utils.DateUtils.GMTUTCUnixTsFormat;
import eu.scape_project.hawarp.utils.DigestUtils;
import static eu.scape_project.hawarp.utils.IOUtils.BUFFER_SIZE;
import eu.scape_project.hawarp.utils.StringUtils;
import eu.scape_project.hawarp.utils.UrlUtils;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jwat.arc.ArcRecordBase;
import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.common.HeaderLine;
import org.jwat.common.HttpHeader;
import org.jwat.common.Payload;
import org.jwat.warc.WarcRecord;

/**
 * ArchiveRecord is a flat properties representation of a web archive record. It
 * is build from ARC or WARC records, the properties are initialised in the
 * contructors for ARC and WARC records respectively.
 *
 * @author onbscs
 */
public class ArchiveRecord extends ArchiveRecordBase {
    
    

    public ArchiveRecord() {

    }

    ArchiveRecord(ArcRecordBase arcRecord, boolean computePayloadDigest) {

        this.url = UrlUtils.canonicalize(arcRecord.getUrlStr());
        this.origUrl = arcRecord.getUrlStr();
        if (!arcRecord.header.ipAddressStr.isEmpty()) {
            this.ipAddress = arcRecord.header.ipAddressStr;
        }
        if (!arcRecord.header.contentTypeStr.isEmpty()) {
            this.mimeType = StringUtils.normaliseMimetype(arcRecord.header.contentTypeStr);
        } else {
            this.mimeType = eu.scape_project.hawarp.interfaces.Identifier.MIME_UNKNOWN;
        }
        this.type = "response";
        this.date = arcRecord.getArchiveDate();
        if (arcRecord.getResultCode() != null) {
            this.httpReturnCode = arcRecord.getResultCode();
        } else {
            if (arcRecord.getHttpHeader() != null && arcRecord.getHttpHeader().statusCode != null) {
                this.httpReturnCode = arcRecord.getHttpHeader().statusCode;
            }

        }

        this.payloadDigestOldStr = "-";

        this.redirectUrl = "-";

        this.offsetCompressedStr = Long.toString(arcRecord.getStartOffset());
        this.offsetUncompressedStr = "-";

        this.compressedDatFileOffset = "-";
        this.uncompressedDatFileOffset = "-";

        this.metaTags = "-";

        // Some ARC records do not have HTTP Response metadata in the ARC header
        // but these metadata are part of the payload content. One possible reason
        // for why this could have happened is a malformed request URL breaking
        // the metadata creation during the crawl. In such a case the original 
        // response was stored together with the response metadata as payload.
        // For this reason HTTP Response metadata that are part of the payload
        // must be read here.
        Payload pl = null;
        ByteCountingPushBackInputStream pbin = null;
        if (arcRecord.getHttpHeader() == null) {
            pl = arcRecord.getPayload();
            long length = pl.getTotalLength();
            try {
                pbin = new ByteCountingPushBackInputStream(arcRecord.getPayloadContent(), BUFFER_SIZE);
                long consumed = length - pbin.getConsumed();
                HttpHeader httpHeader = HttpHeader.processPayload(HttpHeader.HT_RESPONSE, pbin, length, "SHA1");
                if (httpHeader != null && httpHeader.statusCode != null) {
                    this.httpReturnCode = httpHeader.statusCode;
                }
                if (httpHeader != null && httpHeader.contentType != null) {
                    // take only mime type part, discard extended mime type information
                    this.mimeType = StringUtils.normaliseMimetype(httpHeader.contentType);
                }
                // long to int conversion safe, payload header size must not exceed buffer size
                pbin.unread((int) consumed);

            } catch (IOException ex) {
                //LOG.warn("Unable to process HTTP metadata", ex);
            }
        }
        // compute payload digest
        if (computePayloadDigest) {
            try {
                pbin = new ByteCountingPushBackInputStream(arcRecord.getPayloadContent(), BUFFER_SIZE);
                pl = arcRecord.getPayload();
                PayloadContent pc = new PayloadContent(pbin);
                pc.readPayloadContent();
                byte[] payloadBytes = pc.getPayloadBytes();
                long length = pl.getTotalLength();
                long consumed = length - pbin.getConsumed();
                pbin.unread((int) consumed);
                this.payloadDigestStr = DigestUtils.SHAsum(payloadBytes, false, true);
                if (this.payloadDigestStr == null || this.payloadDigestStr.isEmpty()) {
                    this.payloadDigestStr = "-";
                }
            } catch (IOException ex) {

            }
        }
    }

    ArchiveRecord(WarcRecord warcRecord, boolean computePayloadDigest) {
        boolean isResponseType = false;

        if (warcRecord.getHeaderList() != null) {
            HeaderLine warcTypeHl = warcRecord.getHeader("WARC-Type");
            if (warcTypeHl != null && warcTypeHl.value.equals("warcinfo")) {

            } else if (warcTypeHl != null && warcTypeHl.value.equals("response")) {
                isResponseType = true;
            }
            HeaderLine ipAddressHl = warcRecord.getHeader("WARC-IP-Address");
            if (ipAddressHl != null) {
                this.ipAddress = ipAddressHl.value;
            }
            HeaderLine contentTypeHl = warcRecord.getHeader("Content-Type");
            if (contentTypeHl != null) {
                this.mimeType = StringUtils.normaliseMimetype(contentTypeHl.value);
            }
            HeaderLine contentLengthHl = warcRecord.getHeader("Content-Length");
            if (contentLengthHl != null) {
                try {
                    this.contentLengthLong = Long.parseLong(contentLengthHl.value);
                } catch (NumberFormatException ex) {
                    this.contentLengthLong = -1;
                }
            }
            HeaderLine paloadDigestHl = warcRecord.getHeader("WARC-Payload-Digest");
            if (paloadDigestHl != null) {
                this.payloadDigestStr = paloadDigestHl.value;
            }

            HeaderLine targetUriHl = warcRecord.getHeader("WARC-Target-URI");
            if (targetUriHl != null) {
                this.url = targetUriHl.value;
                this.url = UrlUtils.canonicalize(this.url);
                this.origUrl = targetUriHl.value;
            }
            HeaderLine dateHl = warcRecord.getHeader("WARC-Date");
            if (dateHl != null) {
                try {
                    this.date = GMTUTCUnixTsFormat.parse(dateHl.value);
                } catch (ParseException ex) {
                    this.date = new Date(0);
                }
            }

            this.payloadDigestStr = "-";
            this.payloadDigestOldStr = "-";

            this.redirectUrl = "-";

            // http header
            try {
                Payload pl = warcRecord.getPayload();
                long length = pl.getTotalLength();
                ByteCountingPushBackInputStream pbin = new ByteCountingPushBackInputStream(warcRecord.getPayloadContent(), BUFFER_SIZE);

                long consumed = length - pbin.getConsumed();
                HttpHeader httpHeader = HttpHeader.processPayload(HttpHeader.HT_RESPONSE, pbin, length, "SHA1");
                if (httpHeader != null && httpHeader.statusCode != null) {
                    this.httpReturnCode = httpHeader.statusCode;
                }
                // long to int conversion safe, payload header size must not exceed buffer size
                pbin.unread((int) consumed);

            } catch (IOException ex) {
                //LOG.warn("Unable to process HTTP metadata", ex);
            }

            // compute payload digest
            if (computePayloadDigest) {
                try {
                    ByteCountingPushBackInputStream pbin = new ByteCountingPushBackInputStream(warcRecord.getPayloadContent(), BUFFER_SIZE);
                    Payload pl = warcRecord.getPayload();
                    PayloadContent pc = new PayloadContent(pbin);
                    pc.readPayloadContent();
                    byte[] payloadBytes = pc.getPayloadBytes();
                    long length = pl.getTotalLength();
                    long consumed = length - pbin.getConsumed();
                    pbin.unread((int) consumed);
                    this.payloadDigestStr = DigestUtils.SHAsum(payloadBytes, false, true);
                    if (this.payloadDigestStr == null || this.payloadDigestStr.isEmpty()) {
                        this.payloadDigestStr = "-";
                    }
                } catch (IOException ex) {

                }
            }

            this.offsetCompressedStr = Long.toString(warcRecord.getStartOffset());
            this.offsetUncompressedStr = "-";

            this.compressedDatFileOffset = "-";
            this.uncompressedDatFileOffset = "-";

            this.metaTags = "-";

        }

    }

}
