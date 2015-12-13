package com.altoukhov.svsync.fileviews;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.UploadPartRequest;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.joda.time.DateTime;
import com.altoukhov.svsync.FileSnapshot;
import com.altoukhov.svsync.Snapshot;
import com.altoukhov.svsync.Utils;

/**
 * @author Sval
 */
public class S3FileSpace extends FileSpace implements IScannableFileSpace, IReadableFileSpace, IWriteableFileSpace {

    private final AmazonS3Client s3;
    private String bucketName;
    private String rootPath = "";
    private String rootSuffix = "";
    
    private S3FileSpace(String root, String id, String secret) {
        
        s3 = new AmazonS3Client(wrapCredentials(id, secret));
                
        root = root.substring("s3://".length());
        root = root.endsWith("/")? root.substring(0, root.length() - 1) : root;
        bucketName = root;
        
        if (root.contains("/")) {
            int splitIndex = root.indexOf("/");
            bucketName = root.substring(0, splitIndex);
            rootPath = root.substring(splitIndex+1);
        }
    }
    
    public S3FileSpace(String root, String suffix, String id, String secret) {
        this(root, id, secret);
        rootPath = rootPath.isEmpty()? suffix : rootPath + "/" + suffix;
        rootSuffix = suffix;
    }
    
    public S3FileSpace(String root, List<String> excludes, String id, String secret) {
        this(root, id, secret);
        
        for (String exclude : excludes) {
            this.excludes.add(toAbsoluteFilePath(exclude));
        }
    }
    
    public boolean init() {
        
        if (!s3.doesBucketExist(bucketName)) {
            return false;
        }
        
        String rawRootPath = rootPath.equals(rootSuffix)?
                "" :
                rootSuffix.isEmpty()? rootPath : rootPath.substring(0, rootPath.lastIndexOf(rootSuffix) -1);
        
        if (!rawRootPath.isEmpty()) {
            try {
                ObjectMetadata rawRootMeta = s3.getObjectMetadata(bucketName, rawRootPath + "/");
            }
            catch (AmazonClientException ex) {
                return false;
            }
        }
        
        if (!rawRootPath.equals(rootPath)) {
            
            try {
                ObjectMetadata rootMeta = s3.getObjectMetadata(bucketName, rootPath + "/");
            }
            catch (AmazonClientException ex) {
                ObjectMetadata meta = new ObjectMetadata();
                meta.setContentLength(0);
                s3.putObject(bucketName, rootPath + "/", new ByteArrayInputStream(new byte[0]), meta);
            }
        }
        
        return true;
    }

    @Override
    protected Snapshot scan(List<Pattern> filters) {
        try {
            Map<String, FileSnapshot> files = new LinkedHashMap<>();
            Set<String> dirs = new HashSet<>();

            ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
            .withBucketName(bucketName)
            .withPrefix(rootPath.isEmpty()? "" : rootPath + "/");

            ObjectListing objectListing;

            do {
                objectListing = listObjects(listObjectsRequest);
                for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {

                    if (isExcluded(objectSummary.getKey()) || isFiltered(objectSummary.getKey(), filters)) continue;

                    if (objectSummary.getKey().endsWith("/")) {
                        String filePath = trimPath(objectSummary.getKey());
                        filePath = filePath.equals(rootPath)? "" : filePath.substring(rootPath.length() + (rootPath.isEmpty()? 0 : 1 ));
                        dirs.add(filePath);
                        System.out.println(String.format("Scanning s3://%s/%s", bucketName, objectSummary.getKey()));
                    }
                    else {
                        String fileName = objectSummary.getKey();
                        String filePath = "";

                        if (fileName.contains("/")) {
                            int fileNameSplitIndex = fileName.lastIndexOf("/");
                            filePath = fileName.substring(0, fileNameSplitIndex);
                            fileName = fileName.substring(fileNameSplitIndex + 1);

                            filePath = filePath.equals(rootPath)? "" : filePath.substring(rootPath.length() + (rootPath.isEmpty()? 0 : 1 ));
                        }

                        if (filePath.equals("")) {
                            filePath = fileName;
                        }
                        else {
                            filePath = filePath + "/" + fileName;
                        }

                        ObjectMetadata meta = getObjectInfo(objectSummary);
                        String lmd = meta.getUserMetaDataOf("lmd");

                        Date lastModified = (lmd == null)? objectSummary.getLastModified() : new Date(Long.parseLong(lmd));

                        FileSnapshot file = new FileSnapshot(fileName, objectSummary.getSize(), new DateTime(lastModified), filePath);
                        files.put(filePath, file);
                    }
                }
                listObjectsRequest.setMarker(objectListing.getNextMarker());
            } while (objectListing.isTruncated());        

            Snapshot snapshot = new Snapshot(files, dirs);
            return snapshot;
        }
        catch (AmazonClientException ex) {
            System.out.println("Failed to scan file space");
            System.out.println(ex.getMessage());
        }
        
        return null;
    }

    @Override
    public boolean createDirectory(String path) {
        path = trimPath(path);
        
        try {
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(0);
            meta.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
            s3.putObject(bucketName, toAbsoluteDirPath(path), new ByteArrayInputStream(new byte[0]), meta);
        }
        catch (AmazonClientException ex) {
            return false;
        }
        return true;
    }

    @Override
    public boolean deleteDirectory(String path) {
        path = trimPath(path);
        
        try {
            s3.deleteObject(bucketName, toAbsoluteDirPath(path));
        }
        catch (AmazonClientException ex) {
            return false;
        }
        return true;
    }

    @Override
    public boolean writeFile(InputStream fileStream, FileSnapshot file) {
        if (fileStream == null) return false;
        
        if (file.isLargeFile()) {
            return writeLargeFile(fileStream, file);
        }
        
        try {
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(file.getFileSize());
            meta.getUserMetadata().put("lmd", file.getModifiedTimestamp().toDate().getTime() + "");
            meta.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
            s3.putObject(bucketName, toAbsoluteFilePath(file.getRelativePath()), fileStream, meta);
        }
        catch (AmazonClientException ex) {
            return false;
        }
        return true;
    }    
    
    public boolean writeLargeFile(InputStream fileStream, FileSnapshot file) {
        if (fileStream == null) return false;
        
        try {
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(file.getFileSize());
            meta.getUserMetadata().put("lmd", file.getModifiedTimestamp().toDate().getTime() + "");
            meta.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
            
            List<PartETag> partTags = new ArrayList<>();
            String fileKey = toAbsoluteFilePath(file.getRelativePath());
            
            InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, fileKey, meta);
            InitiateMultipartUploadResult result = s3.initiateMultipartUpload(request);
            
            long contentLength = file.getFileSize();
            long partSize = 256 * 1024 * 1024;

            try {
                // Uploading the file, part by part.
                long filePosition = 0;

                for (int i = 1; filePosition < contentLength; i++) {
                    
                    partSize = Math.min(partSize, (contentLength - filePosition));

                    // Creating the request for a part upload
                    UploadPartRequest uploadRequest = new UploadPartRequest()
                            .withBucketName(bucketName).withKey(fileKey)
                            .withUploadId(result.getUploadId()).withPartNumber(i)
                            .withInputStream(fileStream)
                            .withPartSize(partSize);

                    // Upload part and add response to the result list.
                    partTags.add(s3.uploadPart(uploadRequest).getPartETag());
                    filePosition += partSize;
                    
                    System.out.println("Uploaded " + Utils.readableFileSize(filePosition) + " out of " + Utils.readableFileSize(contentLength));
                }
            }         
            catch (Exception e) {
                System.out.println("UploadPartRequest failed: " + e.getMessage());
                s3.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, fileKey, result.getUploadId()));
                return false;
            }

            s3.completeMultipartUpload(new CompleteMultipartUploadRequest(bucketName, fileKey, result.getUploadId(), partTags));
        }
        catch (AmazonClientException ex) {
            System.out.println("Upload failed: " + ex.getMessage());
            return false;

        }
        return true;
    }       
    
    @Override
    public boolean deleteFile(String path) {
        
        try {
            s3.deleteObject(bucketName, toAbsoluteFilePath(path));
        }
        catch (AmazonClientException ex) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public boolean isMoveFileSupported() {
        return true;
    }

    @Override
    public boolean moveFile(String oldPath, String newPath) {
        
        try {
            ObjectMetadata meta = s3.getObjectMetadata(bucketName, toAbsoluteFilePath(oldPath));            
            CopyObjectRequest copyRequest = new CopyObjectRequest(bucketName, toAbsoluteFilePath(oldPath), bucketName, toAbsoluteFilePath(newPath));
            copyRequest.setNewObjectMetadata(meta);
            s3.copyObject(copyRequest);
            s3.deleteObject(bucketName, toAbsoluteFilePath(oldPath));
        }
        catch (AmazonClientException ex) {
            return false;
        }
        
        return true;
    }
 
    private AWSCredentials wrapCredentials(final String id, final String secret) {
        return new AWSCredentials() {

            @Override
            public String getAWSAccessKeyId() {
                return id;
            }

            @Override
            public String getAWSSecretKey() {
                return secret;
            }
        };
    }
    
    @Override
    public InputStream readFile(String path) {
        
        try {
            S3Object s3File = s3.getObject(bucketName, toAbsoluteFilePath(path));
            return s3File.getObjectContent();            
        }
        catch (AmazonClientException ex) {
            System.out.println(ex.getMessage());
            return null;
        }
    }
    
    private String trimPath(String path) {
        return (path.endsWith("\\") || path.endsWith("/")) ? path.substring(0, path.length()-1) : path;
    }

    private String toAbsoluteDirPath(String path) {
        return rootPath.isEmpty()? path +"/" : rootPath + "/" + path + "/";
    }
    
    private String toAbsoluteFilePath(String path) {
        return rootPath.isEmpty()? path : rootPath + "/" + path;
    }
    
    private ObjectListing listObjects(ListObjectsRequest listObjectsRequest) throws AmazonClientException {
        
        ObjectListing objectListing = null;
        int attemptCount = 0;
        
        while ((objectListing == null) && (attemptCount < 3)) {
            try {
                objectListing = s3.listObjects(listObjectsRequest);
                attemptCount++;
            }
            catch (AmazonClientException ex) {
                if (attemptCount < 3) {
                    System.out.println(String.format("Failed to list files for s3://%s/%s, retrying.", bucketName, listObjectsRequest.getPrefix()));
                }
                else {
                    throw ex;
                }
            }
        }
        
        return objectListing;
    }

    private ObjectMetadata getObjectInfo(S3ObjectSummary objectSummary) throws AmazonClientException {
        
        ObjectMetadata meta = null;
        int attemptCount = 0;
        
        while ((meta == null) && (attemptCount < 3)) {
            try {
                meta = s3.getObjectMetadata(bucketName, objectSummary.getKey());
                attemptCount++;
            }
            catch (AmazonClientException ex) {
                if (attemptCount < 3) {
                    System.out.println(String.format("Failed to get metadata for s3://%s/%s, retrying.", bucketName, objectSummary.getKey()));
                }
                else {
                    throw ex;
                }
            }
        }
        
        return meta;
    }
}
