/*
 * Copyright 2012 The SCAPE Project Consortium.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License. under the License.
 */
package eu.scape_project.spacip.unpacker;

import static eu.scape_project.spacip.Spacip.DEFAULT_ITEMS_PER_INVOCATION;
import static eu.scape_project.spacip.Spacip.DEFAULT_OUTPUT_FILE_SUFFIX;
import static eu.scape_project.spacip.Spacip.DEFAULT_SCAPE_PLATFORM_INVOKE;
import static eu.scape_project.spacip.Spacip.DEFAULT_UNPACK_HDFS_PATH;
import static eu.scape_project.spacip.Spacip.DEFAULT_TOOLOUTPUT_HDFS_PATH;
import eu.scape_project.spacip.utils.IOUtils;
import eu.scape_project.spacip.utils.StrUt;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveReaderFactory;
import org.archive.io.ArchiveRecord;
import org.archive.io.ArchiveRecordHeader;
import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCRecordMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ContainerItemPreparation
 *
 * @author Sven Schlarb https://github.com/shsdev
 * @version 0.1
 */
public class ContainerItemPreparation {

    private static Logger logger = LoggerFactory.getLogger(ContainerItemPreparation.class.getName());
    public static final int BUFFER_SIZE = 8192;

    private MultipleOutputs mos;
    private Mapper.Context context;
    private Configuration conf;

    /**
     * Constructor
     *
     * @param mos
     * @param context
     */
    public ContainerItemPreparation(MultipleOutputs mos, Mapper.Context context, Configuration conf) {
        this.mos = mos;
        this.context = context;
        this.conf = conf;
    }

    /**
     * Constructor
     */
    private ContainerItemPreparation() {

    }

    /**
     * Write ARC file content to output stream
     *
     * @param arcRecord ARC record
     * @param outputStream Output stream
     * @throws IOException
     */
    public static void arcToOutputStream(ARCRecord arcRecord, OutputStream outputStream) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(outputStream);
        ARCRecordMetaData metaData = arcRecord.getMetaData();
        long contentBegin = metaData.getContentBegin();
        byte[] tempBuffer = new byte[BUFFER_SIZE];
        int bytesRead;
        BufferedInputStream bis = new BufferedInputStream(arcRecord);
        bis.skip(contentBegin);
        while ((bytesRead = bis.read(tempBuffer)) != -1) {
            bos.write(tempBuffer, 0, bytesRead);
        }
        bos.flush();
        bis.close();
        bos.close();
    }

    /**
     * Prepare input
     *
     * @param pt
     * @param fs
     * @param mos
     * @param itemsPerInvocation
     * @throws IOException IO Error
     * @throws java.lang.InterruptedException
     */
    public void prepareInput(Path pt) throws IOException, InterruptedException {
        FileSystem fs = FileSystem.get(context.getConfiguration());
        InputStream containerFileStream = fs.open(pt);
        String containerFileName = pt.getName();
        ArchiveReader reader = ArchiveReaderFactory.get(containerFileName, containerFileStream, true);
        long currTM = System.currentTimeMillis();
        String unpackHdfsPath = conf.get("unpack_hdfs_path", DEFAULT_UNPACK_HDFS_PATH);
        String hdfsUnpackDirStr = StrUt.normdir(unpackHdfsPath) + currTM + "/";
        String hdfsJoboutputPath = conf.get("tooloutput_hdfs_path", DEFAULT_TOOLOUTPUT_HDFS_PATH);
        String hdfsOutputDirStr = StrUt.normdir(hdfsJoboutputPath) + currTM + "/";
        Iterator<ArchiveRecord> recordIterator = reader.iterator();
        int numItemsPerInvocation = conf.getInt("num_items_per_task", DEFAULT_ITEMS_PER_INVOCATION);
        int numItemCounter = numItemsPerInvocation;
        String inliststr = "";
        String outliststr = "";
        try {
            // K: Record key V: Temporary file
            while (recordIterator.hasNext()) {
                ArchiveRecord nativeArchiveRecord = recordIterator.next();
                ArchiveRecordHeader header = nativeArchiveRecord.getHeader();
                String mimeSuffix = header.getMimetype().replaceAll("/", "-");
                String readerIdentifier = nativeArchiveRecord.getHeader().getReaderIdentifier();
                String recordIdentifier = nativeArchiveRecord.getHeader().getRecordIdentifier();
                ARCRecord arcRecord = (ARCRecord) nativeArchiveRecord;
                String recordKey = readerIdentifier + "/" + recordIdentifier;
                String fileName = RandomStringUtils.randomAlphabetic(20) + "." + mimeSuffix;
                String hdfsPathStr = hdfsUnpackDirStr + fileName;
                Path hdfsPath = new Path(hdfsPathStr);
                String outputFileSuffix = conf.get("output_file_suffix", DEFAULT_OUTPUT_FILE_SUFFIX);
                String hdfsOutPathStr = hdfsOutputDirStr + fileName + outputFileSuffix;
                FSDataOutputStream hdfsOutStream = fs.create(hdfsPath);
                ContainerItemPreparation.arcToOutputStream(arcRecord, hdfsOutStream);
                Text key = new Text(recordKey);
                Text value = new Text(fs.getHomeDirectory() + File.separator + hdfsPath.toString());
                mos.write("keyfilmapping", key, value);
                String scapePlatformInvoke = conf.get("scape_platform_invoke", DEFAULT_SCAPE_PLATFORM_INVOKE);
                Text ptmrkey = new Text(scapePlatformInvoke);
                // for the configured number of items per invokation, add the 
                // files to the input and output list of the command.
                if (numItemCounter > 0) {
                    inliststr += "," + fs.getHomeDirectory() + File.separator + hdfsPathStr;
                    outliststr += "," + fs.getHomeDirectory() + File.separator + hdfsOutPathStr;
                    numItemCounter--;
                } else if (numItemCounter == 0) {
                    inliststr = inliststr.substring(1);
                    outliststr = outliststr.substring(1);
                    String ptMrStr = "--input=\"hdfs:///./\" "
                            + "--inputlist=\"" + inliststr + "\" "
                            + "--output=\"hdfs:///./\" "
                            + "--outputlist=\"" + outliststr + "\"";
                    Text ptmrvalue = new Text(ptMrStr);
                    mos.write("ptmapredinput", ptmrkey, ptmrvalue);
                    numItemCounter = numItemsPerInvocation;
                    inliststr = "";
                    outliststr = "";
                }
            }
        } catch (RuntimeException ex) {
            logger.error("ARC reader error, skipped.", ex);
        }
    }
}
