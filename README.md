# Svsync

This project provides a tool for one-way, efficient file synchronization from source to target. Unlike other file synchronization solutions, this tool supports multiple storage types (see below currently supported types) and can sync files between any combination of these types.

#Features
  * 100% cross-platform

  You can run it on Windows, Linux, Mac OSX or Raspberry Pi smoothly with exactly the same binaries.

  * Supports multiple storage types
    
	Currently supported
      * Local storage
	  * SMB (Windows) network share
	  * Amazon S3
	  * Microsoft Azure Blobs
	  
  * Declarative approach
    
	Just list your sources and targets (i.e. what needs to be synced and where) in XML file that defines a working profile. Svsync will take it from there.

  * Multithreading
	
	Svsync utilizes multithreading to perform some of its tasks in parallel.
  
  * Smart synchronization
    
	This tools uses a non-standard approach to file synchronization. Rather than immediately synchronizing files folder by folder, it first scans both source and target to get a full view of all the files. Then, it analyzes the changes minimizing the amount of data to be copied. For example, if you have already uploaded your 500GB video library to Azure blob, and then you accidentally change the name of the folder from "movies" to "videos", svsync will be smart enough to just move the files to a new location at the target, rather than re-uploading all the files again.
  
  * Advanced file-skipping capabilities
	
	Svsync provides an ability to skip any subfolders within a synced folder. In addition, you can use regular expressions on full file path to decide which files you want and which you don't. For example, you want to upload your photos library to S3, but you want to skip all the Thumbs.db files created by Windows, in order to save space at a target.  
  
#Use Scenarios
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

All dependencies will be added automatically if Maven is used. Otherwise, download the jars and add them to your build path.

##Usage

To run Svsync use the following command:

    svsync -profile <profile_path> [-analyze] [-restore <local_path>]

* -profile <profile_path> specifies path to a local synchronization profile to run
* -analyze is an optional flag that makes svsync to run analysis only and print report, rather than doing any actual synchronization.
* -restore <local_path> is an optional argument that makes svsync to download all files from target (defined inside profile) to a locally specified path.

##Profile Configuration
### Profile template

Svsync synchronization profile should look like the following template:

```xml
<?xml version="1.0"?>
<profile>
	<source name="source1" path="path_to_source1" />
	<source name="source2" path="path_to_source2" />
	<source name="source3" path="path_to_source3" />
	<target path="path_to_target "/>
</profile>
```

* Svsync support multiple sources of any type in a single profile, however only single target per profile is currently supported.
* All sources configured in profile will be copied to the target as its own subfolder with the name of the source. So, after synchronization, target will have 3 subfolders source1, source2, source3, etc.

### File skipping
Svsync supports skipping certain files from source while scanning. To skip files, use <exclude> tag inside <source>. There are 2 options available to skip files.

#### Option 1: Skip subpath
Use path attribute to skip subfolder or specific file within source. The path can be absolute or relative. Unix file separator is supported for windows.

```xml
    <source name="myphotos" path="D:\photos" >
        <exclude path="my_brother_trips/" />
        <exclude path="trip_to_japan/huge_video_file.avi" />
        <exclude path="ignore_me.txt" />
    </source>
```
	
#### Option 2: Skip files matched by regex
Use filter attribute with regex pattern. Files, which full path is matched with regex will be skipped.

```xml
    <source name="myphotos" path="D:\photos" >
		<exclude filter="(?:^|.*/)Thumbs\.db$" />
		<exclude filter="(?:^|.*/)Desktop\.ini$" />
    </source>
```
	
### How to define path to storage
The path to storage is specified as "path" atribute inside both <source> and <target> tags. Some storage types require additional attributes for authentication.

#### Local storage
Use absolute path to the folder that needs to be synced. Both Windows and Unix paths are supported.

    Example: <source name="my_photos" path="/home/user/photos" />

#### SMB storage
Use the following format for SMB shares:

    smb://<server_hostname>/path

Also, if SMB drive require authentication, add the following attributes: user, password.
	
	Example: <source name="my_photos" path="smb://192.168.0.1/photos" user="user1" password="password1" />
	
#### Amazon S3
Use the following format for Amazon S3:

    S3://<bucket_name>/optional_path

Also, use the following attributes for authentication: id, secret.
	
	Example: <source name="my_photos" path="S3://my_bucket/my_photos" id="my_id" secret="my_secret" />
	Example: <source name="my_photos2" path="S3://my_bucket2" id="my_id" secret="my_secret" />

Note: Svsync will not create new buckets, it assumes that all S3 buckets for sources and targets exist.
	
#### Azure Blobs
Use the following format for Azure blobs:

    azure://<storage_account_name>/container_name/optional_path

Also, use the following attributes for authentication: secret.
	
	Example: <source name="my_photos" path="azure://my_storage/my_photos" secret="my_secret" />
	Example: <source name="my_photos2" path="azure://my_storage/my_photos/trips" secret="my_secret" />

Note: Svsync will not create new storage accounts, it assumes that all storage accounts for sources and targets exist.

### Cache files map
Svsync scans both source and target on every run. If files doesn't change often at target, there is an option to cache the results of the scan for defined period of time.

```xml
	<target path="azure://mystorage" secret="my_secret" cache-days="7"/>
```
	
##Sample profile 1
```xml
<?xml version="1.0"?>
<profile>
	
	<source name="photos" path="smb://192.168.0.1/Storage/Photos" user="user1" password="12345">
		<exclude filter="(?:^|.*/)Thumbs\.db$" />
		<exclude filter="(?:^|.*/)Desktop\.ini$" />
	</source>


	<source name="videos" path="smb://192.168.0.1/Storage/Videos" user="user1" password="12345">
		<exclude filter="(?:^|.*/)Thumbs\.db$" />
		<exclude filter="(?:^|.*/)Desktop\.ini$" />
	</source>

		
	<source name="archived-documents" path="smb://192.168.0.1/Storage/Archive" user="user1" password="12345">
		<exclude filter="(?:^|.*/)Thumbs\.db$" />
		<exclude filter="(?:^|.*/)Desktop\.ini$" />
	</source>

	
	<source name="svsync-binaries" path="/home/user/svsync">
		<exclude filter=".+\.log$" />
		<exclude filter=".+\.snap$" />
	</source>
	
	<target path="azure://myazureaccount" secret="VeryVeryLongSecret" cache-days="7"/>

</profile>

```

##Sample profile 2
```xml
	<?xml version="1.0"?>
	<profile>
		<source name="my_s3_files" path="S3://my_bucket/my_files" id="my_amazon_id" secret="my_amazon_secret">
			<exclude path="test_ignore" />
			<exclude path="test_partial/ignore_me.txt" />
			<exclude path="ignore_me.txt" />
		</source>
		<target path="D:\files_from_s3" />
	</profile>
```