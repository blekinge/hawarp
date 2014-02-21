droid-identify
==============

Introduction
------------

Hadoop Job for Identifying files using  DROID (Digital Record Object 
Identification), Version 6.1, http://digital-preservation.github.io/droid/. 
It reads a text file from an HDFS input path which contains a list of absolute 
file paths to file instances on network attached storage. This requires that all 
worker nodes of the Hadoop cluster can access the individual files at the
same path, for example by using the same mount points on all worker nodes.

Installation
------------

    cd droid-identify
    mvn install

Usage
-----

Execute hadoop job from the command line:

    hadoop jar target/droid-identify-1.0.jar-with-dependencies.jar 
      -d /hdfs/path/to/textfiles/with/absolutefilepaths/ -n job_name

In case the size of the text file is smaller than Hadoop’s default split size 
(default configuration: 64 Megabyte), only one single task – i.e. running on 
one single core – would be created for processing the complete list with no 
benefit of running tasks in parallel at all.
Usually, the number of records processed in one task can be controlled by the 
Hadoop parameter:  

    mapred.line.input.format.linespermap

If setting this parameter does not have the desired effect, it is possible to 
take advantage of Hadoop’s default behaviour to create at least one task per 
input file. Using the Unix command:

    split -a 4 -l NUMLINES absolute_file_paths.txt

the complete text file containing all paths can be split into different files 
with the desired number NUMLINES of file path lines per file which corresponds 
to the desired number of file identifications to be processed per task.
Generally, it’s important to keep an eye on the the number of records processed 
per task because this directly influences the task run time. As a rule of thumb 
it is recommended to ensure that “each task runs for at least 30-40 seconds” 
(http://blog.cloudera.com/2009/12/7-tips-for-improving-mapreduce-performance/).
The files can then be loaded into HDFS: 

    hadoop fs -copyFromLocal /local/directory/inputfiles/ /hdfs/parent/directory/

and the HDFS directory

    /hdfs/parent/directory/inputfiles

can then be defined as input directory for the hadoop job (parameter -d).