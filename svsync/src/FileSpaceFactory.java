package svsync;

import svsync.fileviews.IReadableFileSpace;
import svsync.fileviews.IWriteableFileSpace;
import java.io.File;
import svsync.fileviews.AzureFileSpace;
import svsync.fileviews.LocalFileSpace;
import svsync.fileviews.S3FileSpace;
import svsync.fileviews.SmbFileSpace;

/**
 * @author Sval
 */
public class FileSpaceFactory {
    
    public static IReadableFileSpace create(SourceInfo source) {
                
        IReadableFileSpace fileSpace = null;
        
        if (isSmbPath(source.getPath())) {
            fileSpace = initSmbForRead(source);
        }
        else if (isS3Path(source.getPath())) {
            fileSpace = initS3ForRead(source);
        }
        else if (isAzurePath(source.getPath())) {
            fileSpace = initAzureForRead(source);
        }
        else if (isLocalPath(source.getPath())) {
            fileSpace = initLocalForRead(source);
        }
        
        return fileSpace;
    }

    public static IWriteableFileSpace create(TargetInfo target, String sourceName) {
        
        IWriteableFileSpace fileSpace = null;
        
        if (isSmbPath(target.getPath())) {
            fileSpace = initSmbForWrite(target, sourceName);
        }
        else if (isS3Path(target.getPath())) {
            fileSpace = initS3ForWrite(target, sourceName);
        }
        else if (isAzurePath(target.getPath())) {
            fileSpace = initAzureForWrite(target, sourceName);
        }
        else if (isLocalPath(target.getPath())) {
            fileSpace = initLocalForWrite(target, sourceName);
        }
        
        return fileSpace;
    }
    
    private static Boolean isS3Path(String path) {
        return path.toLowerCase().startsWith("s3://");
    }

    private static Boolean isAzurePath(String path) {
        return path.toLowerCase().startsWith("azure://");
    }
    
    private static Boolean isSmbPath(String path) {
        return path.toLowerCase().startsWith("smb://");
    }
    
    private static Boolean isLocalPath(String path) {
                
        for (File root : File.listRoots()) {
            if (path.toLowerCase().startsWith(root.getAbsolutePath().toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }

    private static IReadableFileSpace initLocalForRead(SourceInfo source) {
        
        LocalFileSpace localFileSpace = new LocalFileSpace(source.getPath(), source.getExcludes());
        
        if (localFileSpace.init()) {
            return localFileSpace;
        }
        
        return null;
    }
    
    private static IWriteableFileSpace initLocalForWrite(TargetInfo target, String sourceName) {
        
        LocalFileSpace localFileSpace = new LocalFileSpace(target.getPath(), sourceName);
        
        if (localFileSpace.init()) {
            return localFileSpace;
        }        
        
        return null;
    }
    
    private static IReadableFileSpace initS3ForRead(SourceInfo source) {
        
        String id = source.getParams().get("id");
        String secret = source.getParams().get("secret");
        S3FileSpace s3FileSpace = new S3FileSpace(source.getPath(), source.getExcludes(), id, secret);
        
        if (s3FileSpace.init()) {
            return s3FileSpace;
        }
        
        return null;
    }
    
    private static IWriteableFileSpace initS3ForWrite(TargetInfo target, String sourceName) {
        
        String id = target.getParams().get("id");
        String secret = target.getParams().get("secret");
        S3FileSpace s3FileSpace = new S3FileSpace(target.getPath(), sourceName, id, secret);
        
        if (s3FileSpace.init()) {
            return s3FileSpace;
        }
        
        return null;
    }

    private static IReadableFileSpace initAzureForRead(SourceInfo source) {

        String secret = source.getParams().get("secret");
        AzureFileSpace azureFileSpace = new AzureFileSpace(source.getPath(), source.getExcludes(), secret);
        
        if (azureFileSpace.init()) {
            return azureFileSpace;
        }
        
        return null;
    }
    
    private static IWriteableFileSpace initAzureForWrite(TargetInfo target, String sourceName) {

        String secret = target.getParams().get("secret");
        AzureFileSpace azureFileSpace = new AzureFileSpace(target.getPath(), sourceName, secret);
        
        if (azureFileSpace.init()) {
            return azureFileSpace;
        }
        
        return null;
    }    
    
    private static IReadableFileSpace initSmbForRead(SourceInfo source) {
        
        String user = source.getParams().get("user");
        String password = source.getParams().get("password");
        String domain = source.getParams().get("domain");
        SmbFileSpace smbFileSpace = new SmbFileSpace(source.getPath(), source.getExcludes(), domain, user, password);
        
        if (smbFileSpace.init()) {
            return smbFileSpace;
        }
        
        return null;
    }

    private static IWriteableFileSpace initSmbForWrite(TargetInfo target, String sourceName) {
        
        String user = target.getParams().get("user");
        String password = target.getParams().get("password");
        String domain = target.getParams().get("domain");
        SmbFileSpace smbFileSpace = new SmbFileSpace(target.getPath(), sourceName, domain, user, password);
        
        if (smbFileSpace.init()) {
            return smbFileSpace;
        }
        
        return null;
    }
    
}
