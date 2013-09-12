Cipex - Container Item Property EXtraction
==========================================

Introduction
------------

Cipex is a tool to identify files packaged in container files.

The tool can be executed as a command line application or as a hadoop job 
(Hadoop 0.20 API) and provides a stack of identification tools which is 
executed on a set of container files (TAR, ZIP, ARC) in order to
extract identifying properties. Each tool can output a specific identification
property (e.g. Tika - mime type: image/png) or a list of properties
(e.g. Droid - mime type: image/png, droid puid: fmt/11). The output is in 
tabular form which allows importing the result as a comma separated file (CSV)
in spreadsheet applications or for large data sets in a HIVE table.

The number of elements packaged in a container file should not bee too high,
i.e. rather hundreds of thousands than millions, because, firstly, the file lists 
are internally managed as ArrayList and HashMap objects and, secondly, in the
Hadoop job, one container is processed in a map task.

The spring-based application configuration allows easily adding new 
identification tools by extending the abstract class 'Identification' and
adapting the spring configuration file accordingly.

Also the output of the application can be adapted by either changing the 
output syntax as documented in the spring configuration or by writing a custom 
class which has to implement the 'Reportable' interface where the command line 
application output and the hadoop job key-value pair output have to be
defined separately.

In order to give a simple example, let us assume we have the following 
container aggregating an HTML file and a picture in PNG format:

    containerfile.zip
        - index.html
        - picture.png

Using three different identification tools, the following tabular output of 
properties is produced:

    Identifier                       Tool       Property    Value
    -----------------------------------------------------------------
    containerfile.zip/index.html     droid      mime        text/html
    containerfile.zip/index.html     droid      puid        fmt/96
    containerfile.zip/picture.png    droid      mime        image/png
    containerfile.zip/picture.png    droid      puid        fmt/11
    containerfile.zip/index.html     tika       mime        text/html
    containerfile.zip/picture.png    tika       mime        image/png
    containerfile.zip/index.html     unixfile   mime        text/html
    containerfile.zip/picture.png    unixfile   mime        image/png

In this example, the tool droid is the only tool which outputs two different
properties, the mime type of the file and the puid (pronom unique identifier),
the other tools only return the mime type of the file. 

Installation
------------

    cd cipex
    mvn install

[Travis CI](http://travis-ci.org/) is used to verify the build: 
[![Build Status](https://api.travis-ci.org/shsdev/cipex.png)](https://travis-ci.org/shsdev/cipex) 

Configuration
-------------

The spring configuration is added as a classpath resource at

    src/main/resources/eu/scape_project/tb/cipex/spring-config.xml

and a copy is available locally in the project root directory.

Usage
-----

The command line application is executed by typing

    java -jar
      target/cipex-1.0-SNAPSHOT-jar-with-dependencies.jar 
      -d /path/to/local/filesystem/directory/

where

    -d,--dir <arg>    Local file system directory containing the ARC files.

and the hadoop job is be executed by typing

    hadoop jar
      target/cipex-1.0-SNAPSHOT-jar-with-dependencies.jar 
      -d /path/to/hdfs/directory/

where

    -d,--dir <arg>    HDFS directory containing (the) text file(s) listing HDFS 
                      paths to container files.

The HDFS directory must point to a directory containing (the) text file(s)
listing HDFS paths to container files. If the file size is smaller than the
default split size (64MB), then the maximum number of records per tasks 
should be configured. If this is not possible for some reason, Hadoop can be 
forced to create a specific number of records per task by splitting the input 
text file.

For example, let us assume a textfile named zipcontainerhdfspaths.txt 
which is input for the Hadoop job:

    /user/name/input/000.zip
    /user/name/input/001.zip
    /user/name/input/002.zip
    /user/name/input/003.zip
    /user/name/input/004.zip
    /user/name/input/005.zip
    /user/name/input/006.zip
    /user/name/input/007.zip
    /user/name/input/008.zip
    /user/name/input/009.zip

The size of this textfile is far below the 64MB default split size, therefore
Hadoop would process all files in one single map task. 

If you want to force Hadoop to create 10 tasks, you can split the textfile:

    split -a 4 -l 1 zipcontainerhdfspaths.txt

and load the splitted files into HDFS as input. Hadoop will create at least 
one map task per input file.

The optional parameter -s allows defining a spring configuration file
available on the local file system instead of the one available as a resource
in the packaged jar file. This option only works in command line execution
mode, the hadoop job always reads the spring configuration as a file resource
from the classpath.

Additional hadoop parameters must be defined after the jar parameter, e.g.
setting the maximum number of tasks that should run in parallel:

    hadoop jar
      target/cipex-1.0-SNAPSHOT-jar-with-dependencies.jar
      -Dmapred.tasktracker.map.tasks.maximum=2
      -d /path/to/hdfs/input/directory
