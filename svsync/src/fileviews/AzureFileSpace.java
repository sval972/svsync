package svsync.fileviews;

import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;
import svsync.FileSnapshot;
import svsync.Snapshot;
import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.blob.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.joda.time.DateTime;
import static svsync.fileviews.FileSpace.isFiltered;

/**
 * @author Sval
 */
public class AzureFileSpace extends FileSpace implements IScannableFileSpace, IReadableFileSpace, IWriteableFileSpace {

    private String connectionString;
    
    private String containerName = "";    
    private String rootPath = "";
    private String rootSuffix = "";
    
    private boolean isReadMode;
    private boolean writeDirectories = true;
    
    private CloudStorageAccount storageAccount;
    private CloudBlobClient blobClient;
    private CloudBlobContainer container;
    
    private AzureFileSpace(String root, String secret, boolean isReadMode) {

        this.isReadMode = isReadMode;
        
        root = root.substring("azure://".length());
        root = root.endsWith("/")? root.substring(0, root.length() - 1) : root;
        
        String storageAccountName = root;
        
        if (root.contains("/")) {
            String[] splits = root.split("/");
            
            storageAccountName = splits[0];
            containerName = splits[1];
            
            if (splits.length > 2) {
                rootPath = root.substring(storageAccountName.length() + containerName.length() + 2);
            }
        }
        
        connectionString = wrapCredentials(storageAccountName, secret);
    }
    
    // Write constructor
    // azure://{account}
    // azure://{account}/{container}
    public AzureFileSpace(String root, String suffix, String secret) {
        this(root, secret, false);
        
        if (containerName.isEmpty()) {
            containerName = suffix;
        }
        else {
            rootPath = rootPath.isEmpty()? suffix : rootPath + "/" + suffix;
            rootSuffix = suffix;
        }
    }
    
    // Read constructor
    // azure://{account}/{container}
    // azure://{account}/{container}/{path}
    public AzureFileSpace(String root, List<String> excludes, String secret) {
        this(root, secret, true);
        
        for (String exclude : excludes) {
            this.excludes.add(toAbsoluteFilePath(exclude));
        }
    }
    
    public boolean init() {
        try {
            storageAccount = CloudStorageAccount.parse(connectionString);
            blobClient = storageAccount.createCloudBlobClient();
            container = blobClient.getContainerReference(containerName);
        
            if (!container.exists()) {
                if (isReadMode) {
                    return false;
                }
                else {
                    container.create();
                }
            }
        }
        catch (StorageException | URISyntaxException | InvalidKeyException ex) {
            return false;
        }
        
        if (!writeDirectories) return true;

        String rawRootPath = rootPath.equals(rootSuffix)?
                "" :
                rootSuffix.isEmpty()? rootPath : rootPath.substring(0, rootPath.lastIndexOf(rootSuffix) -1);

        if (!rawRootPath.isEmpty()) {
            try {
                CloudBlockBlob blob = container.getBlockBlobReference(rawRootPath + "/");
                if (!blob.exists()) return false;
            }
            catch (URISyntaxException | StorageException ex) {
                return false;
            }
        }

        if (!rawRootPath.equals(rootPath)) {
            try {
                CloudBlockBlob blob = container.getBlockBlobReference(rootPath + "/");
                if (!blob.exists()) {
                    HashMap<String, String> meta = new HashMap<>();
                    meta.put("type", "directory");
                    blob.setMetadata(meta);
                    blob.upload(emptyStream, 0);
                }
            }
            catch (URISyntaxException | StorageException | IOException ex) {
                return false;
            }
        }             
        
        return true;
    }    
    
    @Override
    protected Snapshot scan(List<Pattern> filters) {
        try {
            Map<String, FileSnapshot> files = new LinkedHashMap<>();
            Set<String> dirs = new HashSet<>();
            dirs.add("");
            
            for (ListBlobItem blobItem : container.listBlobs(rootPath.isEmpty()? "" : rootPath + "/", true, null, null, null)) {
                CloudBlockBlob blob = (CloudBlockBlob)blobItem;
                
                if (isExcluded(blob.getName()) || isFiltered(blob.getName(), filters)) continue;
                
                blob.downloadAttributes();
                HashMap<String, String> meta = blob.getMetadata();
                
                String type = meta.get("type");
                
                if (type == null) {
                    type = (blob.getName().endsWith("/") && (blob.getProperties().getLength() == 0))? "directory" : "file";
                }
                
               if (type.equals("directory") && writeDirectories) {
                    String filePath = blob.getName().substring(0, blob.getName().lastIndexOf("/"));
                    filePath = filePath.equals(rootPath)? "" : filePath.substring(rootPath.length() + (rootPath.isEmpty()? 0 : 1 ));
                    dirs.add(filePath);
                    System.out.println(String.format("Scanning azure://%s/%s", containerName, filePath));
                }
                else {
                    String fileName = blob.getName();
                    String filePath = "";
                    
                    if (!writeDirectories) {
                        String dirPath = fileName.equals(rootPath)? "" : fileName.substring(rootPath.length() + (rootPath.isEmpty()? 0 : 1 ));
                        List<String> dirsFromFile = parseDirs(dirPath);
                        for (String dir : dirsFromFile) {
                            boolean added = dirs.add(dir);
                            if (added) {
                                System.out.println(String.format("Scanning azure://%s/%s/%s", storageAccount.getCredentials().getAccountName(), containerName, dir));
                            }
                        }
                    }
                    
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

                    String lastModifiedProp = meta.get("lastModified");
                    Date lastModified = (lastModifiedProp == null)? blob.getProperties().getLastModified() : new Date(Long.parseLong(lastModifiedProp));

                    FileSnapshot file = new FileSnapshot(fileName, blob.getProperties().getLength(), new DateTime(lastModified), filePath);
                    files.put(filePath, file);
                }
            }
            
            Snapshot snapshot = new Snapshot(files, dirs);
            return snapshot;
        }
        catch (Exception ex) {
            System.out.println("Failed to scan file space");
            System.out.println(ex.getMessage());
        }
        
        return null;        
    }

    private List<String> parseDirs(String filePath) {
        List<String> dirs = new ArrayList<>();
        StringBuilder path = new StringBuilder();
        
        String[] pathSplits = filePath.split("/");
        if (pathSplits.length > 1) {
            for (int i=0; i<pathSplits.length-1; i++) {
                
                if (!path.toString().equals("")) path.append("/");
                path.append(pathSplits[i]);
                dirs.add(path.toString());
            }
        }
        
        return dirs;
    }    
    
    @Override
    public InputStream readFile(String path) {
        try {
            CloudBlockBlob blob = container.getBlockBlobReference(toAbsoluteFilePath(path));
            return blob.openInputStream();
        }
        catch (StorageException | URISyntaxException ex) {
            System.out.println(ex.getMessage());
            return null;
        }
    }

    @Override
    public boolean createDirectory(String path) {
        if (!writeDirectories) return true;
        
        try {
            CloudBlockBlob blob = container.getBlockBlobReference(toAbsoluteDirPath(path));
            HashMap<String, String> meta = new HashMap<>();
            
            meta.put("type", "directory");
            blob.setMetadata(meta);
            blob.upload(emptyStream, 0);
        }
        catch (URISyntaxException | StorageException | IOException ex) {
            return false;
        }
        
        return true;
    }

    @Override
    public boolean deleteDirectory(String path) {
        if (!writeDirectories) return true;
        
        try {
            CloudBlockBlob blob = container.getBlockBlobReference(toAbsoluteDirPath(path));
            blob.delete();
        }
        catch (StorageException | URISyntaxException ex) {
            return false;
        }
        return true;
    }

    @Override
    public boolean deleteFile(String path) {
        try {
            CloudBlockBlob blob = container.getBlockBlobReference(toAbsoluteFilePath(path));
            blob.delete();
        }
        catch (StorageException | URISyntaxException ex) {
            return false;
        }
        return true;
    }

    @Override
    public boolean writeFile(InputStream fileStream, FileSnapshot file) {
        if (fileStream == null) return false;
        
        try {
            CloudBlockBlob blob = container.getBlockBlobReference(toAbsoluteFilePath(file.getRelativePath()));
            HashMap<String, String> meta = new HashMap<>();
            meta.put("type", "file");
            meta.put("lastModified", file.getModifiedTimestamp().toDate().getTime() + "");
            blob.setMetadata(meta);
            blob.upload(fileStream, file.getFileSize());
        }
        catch (StorageException | IOException | URISyntaxException ex) {
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
            CloudBlockBlob fromBlob = container.getBlockBlobReference(toAbsoluteFilePath(oldPath));
            CloudBlockBlob toBlob = container.getBlockBlobReference(toAbsoluteFilePath(newPath));
            toBlob.startCopyFromBlob(fromBlob);
            
            toBlob.downloadAttributes();
            CopyState state = toBlob.getProperties().getCopyState();
            while(state.getStatus().equals(CopyStatus.PENDING))
            {
                System.out.println("Progress is " + state.getBytesCopied()/state.getTotalBytes());
                Thread.sleep(500);

                toBlob.downloadAttributes();
                state = toBlob.getProperties().getCopyState();
            }            
            
            fromBlob.delete();
        }
        catch (StorageException | URISyntaxException | InterruptedException ex) {
            return false;
        }
        return true;
    }
        
    private String wrapCredentials(final String storageAccount, final String secret) {
        return String.format("DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s", storageAccount, secret);
    }
    
    private String toAbsoluteFilePath(String path) {
        return rootPath.isEmpty()? path : rootPath + "/" + path;
    }
    
    private String toAbsoluteDirPath(String path) {
        return rootPath.isEmpty()? path +"/" : rootPath + "/" + path + "/";
    }
    
    private final static InputStream emptyStream = 
            new InputStream() {
                    @Override
                    public int read() throws IOException {
                        return 0;
                    }
                };
}
