# Svsync

This project provides a tool for one way, efficient file synchronization from source to target. Unlike other file synchronization solutions, this tool supports multiple storage types (see below currently supported types) and can sync files between any combination of these types.

#Features
  * 100% Cross-platform solution

  You can run it on Windows, Linux, Mac OSX or Raspberry Pi smoothly with exactly the same binaries

  * Supports multiple storage types
    
	Currently supported
      * Local storage
	  * SMB (Windows) network share
	  * Amazon S3
	  * Microsoft Azure Blobs
	  
  * Declarative approach
    
	Tool's input is a profile XML file, that lists your sources and targets, i.e. what needs to be synced and where

  * Multithreading
	
	The tools is utilizing multithreading to perform some of its tasks in parallel
  
  * Smart sync
    
	This tools uses a non-standard approach to file synchronization. Rather than immediately synchronizing files folder by folder, it first scans both source and target to get a full view of all the files. Then, it analyzes the changes minimizing the amount of data to be synchronized. For example, if you have already uploaded your 500GB video library to Azure blob, and then you accidentally change the name of the folder from "movies" to "videos", svsync will be smart enough to just move the files to a new location at the target, rather than re-uploading all the files again.
  
  * Advanced file-skipping capabilities
	
	Svsync provides an ability to skip any subfolders within a synced folder. In addition, you can use regular expressions on full file path to decide which files you want and which you don't. For example, you want to upload your photos library to S3, but you want to skip all the Thumbs.db files created by Windows, in order to save space at a target.  
  
#Possible Use
  * Sync your stuff from home NAS to Amazon S3 once a day for disaster recovery.
    
	You can choose only critical stuff, rather than all your files, paying only for storage taken at S3.
  
  * Sync large amount of data from your pc to an external drive once in a while.
    
	No need to copy all data over and over again, just sync the differences from the last time.
  
  * Sync up your work files at the end of the day to a network share for backup.
    
	You can use Windows scheduled tasks or cron job to automate the runs.
	
	
#Getting Started

##Download
###Option 1: Source Via Git

To get the source code of the SDK via git just type:

    git clone git://github.com/sval972/svsync.git
    cd ./svsync
    mvn compile

###Option 2: Source Zip

To download a copy of the source code, click "Download ZIP" on the right side of the page or click [here](https://github.com/sval972/svsync/archive/master.zip). Unzip and navigate to the svsync folder.

##Minimum Requirements

* Java 1.7+
* [Gson](https://github.com/google/gson) is used for JSON parsing.
* [Joda-Time](https://github.com/JodaOrg/joda-time) is used for Date/Time related operations.
* [Guava](https://github.com/google/guava) is used for miscellaneous purposes.
* [Commons-io](https://github.com/apache/commons-io) is used for advanced operations with the local storage.
* [Azure-Storage](https://github.com/Azure/azure-storage-java) is used for accessing Microsoft Azure blobs.
* [Aws-Java-Sdk](https://github.com/aws/aws-sdk-java) is used for accessing Amazon S3.
* [Jcifs](https://github.com/kohsuke/jcifs) is used for accessing SMB network shares.
* (Optional) Maven

All dependencies will be added automatically if Maven is used. Otherwise, please download the jars and add them to your build path.

##Usage


##Sample Profile Configuration
