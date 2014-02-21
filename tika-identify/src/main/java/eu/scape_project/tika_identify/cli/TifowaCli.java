/*
 *  Copyright 2011 The SCAPE Project Consortium.
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
package eu.scape_project.tika_identify.cli;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;

import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.mime.MediaType;

/**
 * Tika command line application
 *
 * @author shsdev https://github.com/shsdev
 * @version 0.2
 */
public class TifowaCli {

    // Logger instance
    private static final Log LOG = LogFactory.getLog(TifowaCli.class);
    // Statics to set up command line arguments
    private static final String HELP_FLG = "h";
    private static final String HELP_OPT = "help";
    private static final String HELP_OPT_DESC = "print this message.";
    private static final String DIR_FLG = "d";
    private static final String DIR_OPT = "dir";
    private static final String DIR_OPT_DESC = "directory containing files.";
    // Static for command line option parsing
    private static Options OPTIONS = new Options();
    static HashMap<String, Integer> myCollection = new HashMap<String, Integer>();
    static int countAllCalls = 0;
    static int countAllGoodItems = 0;
    static int countAllFailedItems = 0;
    private static Tika tika;
    private static DefaultDetector detector;

    static {
        OPTIONS.addOption(HELP_FLG, HELP_OPT, false, HELP_OPT_DESC);
        OPTIONS.addOption(DIR_FLG, DIR_OPT, true, DIR_OPT_DESC);
        tika = new Tika();
    }

    public static void main(String[] args) {
        // Static for command line option parsing
        TifowaCli tc = new TifowaCli();
        detector = new DefaultDetector();
        CommandLineParser cmdParser = new PosixParser();
        try {
            CommandLine cmd = cmdParser.parse(OPTIONS, args);
            if ((args.length == 0) || (cmd.hasOption(HELP_OPT))) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp(Constants.USAGE, OPTIONS, true);
                System.exit(0);
            } else {
                if (cmd.hasOption(DIR_OPT) && cmd.getOptionValue(DIR_OPT) != null) {
                    String dirStr = cmd.getOptionValue(DIR_OPT);
                    LOG.info("Directory: " + dirStr);

                    // *** start timer
                    long startClock = System.currentTimeMillis();

                    tc.processFiles(new File(dirStr));

                    // *** stop timer
                    long elapsedTimeMillis = System.currentTimeMillis() - startClock;


                    //  *** display the TYPE collection
                    displayMyTypes(myCollection, countAllCalls, countAllGoodItems, countAllFailedItems, elapsedTimeMillis);


                } else {
                    LOG.error("No directory given.");
                    HelpFormatter formatter = new HelpFormatter();
                    formatter.printHelp(Constants.USAGE, OPTIONS, true);
                    System.exit(1);
                }
            }
        } catch (ParseException ex) {
            LOG.error("Problem parsing command line arguments.", ex);
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(Constants.USAGE, OPTIONS, true);
            System.exit(1);
        }
    }

    public void checkDirectory(String dir) throws TifowaCliException {
        File file = new File(dir);
        if (file.isDirectory()) {
            if (file.list().length > 0) {
                LOG.info("Directory is not empty!");
            } else {
                throw new TifowaCliException("Directory \"" + dir + "\" is empty!");
            }
        } else {
            throw new TifowaCliException("\"" + dir + "\" is not a directory");
        }
    }

    private void processFiles(File path) {

        if (path.isDirectory()) {
            String[] children = path.list();
            for (int i = 0; i < children.length; i++) {
                processFiles(new File(path, children[i]));
            }
        } else {
            processFile(path);
        }
    }

    private synchronized void processFile(File path) {

        //  *** count calls
        countAllCalls++;

        //  *** count and display file types found during this run
        //createTypeDistributionContentType(path); //This is for reading "Content-Type" (e.g. added by the Crawler)
        createTypeDistributionDetect(path); // This is for detection          
    }

    private synchronized void createTypeDistributionDetect(File path) {
        try {
            Metadata met = new Metadata();

            met.set(Metadata.RESOURCE_NAME_KEY, path.getName());
            TikaInputStream stream = TikaInputStream.get(path, met);
            MediaType mediaType = detector.detect(stream, met);
            stream.close();

            String myType = mediaType.toString().intern();

            // Check for an existing key for the current type. Create it if it is not existing.
            try {
                int myGetCounter = myCollection.get(myType);
            } catch (Exception ex) {   //logger.debug("1st >" + myType + "< file type. Create NEW key for counter. ");
                myCollection.put(myType, 0);
            }

            // Read the counter for the current type and increase the type counter 
            myCollection.put(myType, myCollection.get(myType) + 1);

            // Increase overall item counter
            countAllGoodItems++;

        } catch (Exception ex) {
            countAllFailedItems++;
            LOG.warn("Exception processing file: \"" + path.getAbsolutePath() + "\"");
            LOG.warn("Exception: " + ex.getMessage());
        }
    }

    private synchronized void createTypeDistributionContentType(File path) {

        try {
            Metadata met = new Metadata();
            FileInputStream fileStr = new FileInputStream(path);
            tika.parse(fileStr, met);
            String myType = met.get("Content-Type").toLowerCase();
            myType = myType.replaceAll("([^A-Za-z-/;=0-9]|;$)", ""); //normalize tags
            myType = myType.replaceAll("text-html", "text/html"); //normalize tags
            //if(myType.indexOf("html") > 0 && (myType.indexOf("charset") > 0)) myType = "text/html"; //normalize txt formats
            fileStr.close();


            //printAllMetaData(met);

            // Check for an existing key for the current type. Create it if it is not existing.
            try {
                int myGetCounter = myCollection.get(myType);
            } catch (Exception ex) {   //logger.debug("1st >" + myType + "< file type. Create NEW key for counter. ");
                myCollection.put(myType, 0);
            }

            // Read the counter for the current type and increase the type counter 
            myCollection.put(myType, myCollection.get(myType) + 1);

            // Increase overall item counter
            countAllGoodItems++;

        } catch (Exception ex) {
            countAllFailedItems++;
            LOG.warn("Exception processing file: \"" + path.getAbsolutePath() + "\"");
            LOG.warn("Exception: " + ex.getMessage());
        }


    }

    private void printAllMetaData(Metadata met) {

        for (int i = 0; i < met.names().length; i++) {
            String name = met.names()[i];
            LOG.info("   *** " + name + ": " + met.get(name));
        }
    }

    private static void displayMyTypes(HashMap<String, Integer> myCollection, int countAllCalls, int countAllGoodItems, int countAllFailedItems, long elapsedTimeSec) {

        Iterator<String> it = myCollection.keySet().iterator();

        System.out.println("************************************");
        System.out.println("Total file processing time (sec): " + elapsedTimeSec / 1000F);
        System.out.println("************************************");
        System.out.println("Total number of TIKA calls      : " + countAllCalls);
        System.out.println("************************************");
        System.out.println("Total number of files analyzed  : " + countAllGoodItems);
        System.out.println("************************************");
        System.out.println("Total number of FAILED files    : " + countAllFailedItems);
        System.out.println("************************************");
        System.out.println("*** You can import the data below into a CSV. Use # as the separator. ***");
        System.out.println();
        System.out.println("TYPE#COUNT#PERCENTAGE");


        while (it.hasNext()) {

            String typeKey = it.next().toString();
            float typeValue = myCollection.get(typeKey);
            float myPerc = typeValue / countAllGoodItems * 100;
            //System.out.println(typeKey + " : " + typeValue + " => " + myPerc + "%");
            System.out.println(typeKey + "#" + (int) typeValue + "#" + myPerc);

        }

        //System.out.println("************************************");
        //System.out.println(myCollection);
        //System.out.println("************************************");
        System.out.println();


    }
}
