package svsync.fileviews;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileFilter;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import svsync.FileSnapshot;
import svsync.Snapshot;

/**
 * @author Sval
 */
public class SmbFileSpace extends FileSpace implements IScannableFileSpace, IReadableFileSpace, IWriteableFileSpace {

    private final NtlmPasswordAuthentication auth;
    private String rootPath;
    private String rootSuffix = "";
        
    private SmbFileSpace(String root, String domain, String user, String password) {
        
        if (domain == null) domain = "";
        if (user == null) user = "";
        if (password == null) password = "";
        auth = new NtlmPasswordAuthentication(domain, user, password);
        rootPath = root.endsWith("/")? root : root + "/";
    }
    
    public SmbFileSpace(String root, String suffix, String domain, String user, String password) {
        this(root, domain, user, password);
        
        rootPath = rootPath + suffix;
        rootPath = rootPath.endsWith("/")? rootPath : rootPath + "/";
        
        rootSuffix = suffix;
    }
    
    public SmbFileSpace(String root, List<String> excludes, String domain, String user, String password) {
        this(root, domain, user, password);

        for (String exclude : excludes) {
            this.excludes.add(toAbsoluteFilePath(exclude));
        }
    }    
       
    public boolean init() {
        
        jcifs.Config.setProperty("jcifs.smb.client.responseTimeout", "60000");
        
        try {
            String rawRootPath = rootSuffix.isEmpty()? rootPath : rootPath.substring(0, rootPath.lastIndexOf(rootSuffix));

            SmbFile rawRoot = new SmbFile(rawRootPath, auth);

            if (rawRoot.exists() && rawRoot.isDirectory()) {

                SmbFile root = new SmbFile(rootPath, auth);
                if (!root.exists()) {
                    root.mkdirs();
                }

                return true;
            }
        }
        catch (MalformedURLException | SmbException ex) {
            System.out.println(ex.getMessage());
        }
        
        return false;
    }
    
    @Override
    protected Snapshot scan(List<Pattern> filters) {
        try {
            Map<String, FileSnapshot> files = new LinkedHashMap<>();
            Set<String> dirs = new HashSet<>();

            SmbFile root = new SmbFile(rootPath, auth);
            if (root.exists()) {

                Stack<SmbFile> stack = new Stack<>();
                stack.push(root);
                dirs.add("");

                while (!stack.isEmpty()) {
                    SmbFile currentFolder = stack.pop();
                    
                    for (final SmbFile file : listFiles(currentFolder)) {
                        
                        String path = file.getPath();
                        
                        boolean isFile = isFile(file);
                        boolean isDirectory = isDirectory(file);
                        
                        if (isFile && !isExcluded(path) && !isFiltered(toRelativePath(path), filters)) {
                            FileSnapshot fileSnapshot = createFileSnapshot(file, path);
                            files.put(fileSnapshot.getRelativePath(), fileSnapshot);
                        }
                        else if (isDirectory && !isExcluded(path) && !isFiltered(toRelativePath(path, true), filters)) {
                            stack.push(file);
                            dirs.add(toRelativePath(path));
                            System.out.println("Scanning " + path);
                        }
                    }
                }
            }        
            Snapshot snapshot = new Snapshot(files, dirs);
            return snapshot;
        }
        catch (MalformedURLException | SmbException ex) {
            System.out.println("Failed to scan file space");
            System.out.println(ex.getMessage());
            System.out.println(ex);
        }
        
        return null;
    }
    
    @Override
    public InputStream readFile(String path) {
        
        try {
            SmbFile file = new SmbFile(toAbsoluteFilePath(path), auth);
            return file.getInputStream();
        }
        catch (IOException ex) {
            System.out.println(ex.getMessage());
            return null;
        }
    }

    @Override
    public boolean createDirectory(String path) {
        
        try {
            SmbFile file = new SmbFile(toAbsoluteDirPath(path), auth);
            file.mkdir();
            return true;
        }
        catch (IOException ex) {
            System.out.println(ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean deleteDirectory(String path) {
        
        try {
            SmbFile file = new SmbFile(toAbsoluteDirPath(path), auth);
            file.delete();
            return true;
        }
        catch (IOException ex) {
            System.out.println(ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean deleteFile(String path) {

        try {
            SmbFile file = new SmbFile(toAbsoluteFilePath(path), auth);
            file.delete();
            return true;
        }
        catch (IOException ex) {
            System.out.println(ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean isMoveFileSupported() {
        return true;
    }

    @Override
    public boolean moveFile(String oldPath, String newPath) {
        
        try {
            SmbFile oldFile = new SmbFile(toAbsoluteFilePath(oldPath), auth);
            SmbFile newFile = new SmbFile(toAbsoluteFilePath(newPath), auth);
            oldFile.renameTo(newFile);
        }
        catch (IOException ex) {
            System.out.println(ex.getMessage());
            return false;
        }
        
        return true;
    }

    @Override
    public boolean writeFile(InputStream fileStream, FileSnapshot fileInfo) {
        
        if (fileStream == null) return false;
        
        OutputStream out = null;
        long bytesWritten = 0;
        boolean setTimestamp = false;
        
        try {
            SmbFile file = new SmbFile(toAbsoluteFilePath(fileInfo.getRelativePath()), auth);
            out = file.getOutputStream();
            
            if (out == null) {
                System.out.println("Failed to open file for write: " + fileInfo.getRelativePath());
            }
                
            if (fileInfo.isLargeFile()) {
                bytesWritten = IOUtils.copyLarge(fileStream, out);
            }
            else {
                bytesWritten = IOUtils.copy(fileStream, out);
            }            
        }
        catch (IOException ex) {
            System.out.println("Failed to copy file: " + ex.getMessage());
        }
        finally {
            try {
                if (fileStream != null) fileStream.close();
                if (out != null) out.close();
            }
            catch (IOException ex) {
                System.out.println("Failed to close stream: " + ex.getMessage());
            }
        }
                
        try {
            SmbFile file = new SmbFile(toAbsoluteFilePath(fileInfo.getRelativePath()), auth);
            file.setLastModified(fileInfo.getModifiedTimestamp().toDate().getTime());
            setTimestamp = true;
        }
        catch (IOException ex) {
            System.out.println("Failed to copy file: " + ex.getMessage());
        }
                
        return setTimestamp && (bytesWritten == fileInfo.getFileSize());        
    }

    private final static SmbFileFilter filter = new SmbFileFilter() {

        @Override
        public boolean accept(SmbFile file) throws SmbException {
            return !file.isHidden();
        }
    };
    
    private SmbFile[] listFiles(SmbFile folder) throws SmbException {
        
        SmbFile[] files = null;
        int attemptCount = 0;
        
        while ((files == null) && (attemptCount < 3)) {
            try {
                attemptCount++;
                files = folder.listFiles(filter);
            }
            catch (SmbException ex) {
                if (attemptCount < 3) {
                    System.out.println("Failed to list files for " + folder.getPath() + ", retrying.");
                }
                else {
                    throw ex;
                }
            }
        }
        
        return files;
    }
    
    private Boolean isFile(final SmbFile file) throws SmbException {
        
        Boolean isFile = null;        
        int attemptCount = 0;
        
        while ((isFile == null) && (attemptCount < 3)) {
            try {
                isFile = file.isFile();
                attemptCount++;
            }
            catch (SmbException ex) {
                if (attemptCount < 3) {
                    System.out.println("Failed to query file " + file.getPath() + ", retrying.");
                }
                else {
                    throw ex;
                }
            }
        }
        
        return isFile;
    }

    private Boolean isDirectory(final SmbFile file) throws SmbException {
        
        Boolean isDirectory = null;
        int attemptCount = 0;
        
        while ((isDirectory == null) && (attemptCount < 3)) {
            try {
                isDirectory = file.isDirectory();
                attemptCount++;
            }
            catch (SmbException ex) {
                if (attemptCount < 3) {
                    System.out.println("Failed to query file " + file.getPath() + ", retrying.");
                }
                else {
                    throw ex;
                }
            }
        }
        
        return isDirectory;
    }

    private FileSnapshot createFileSnapshot(final SmbFile file, String path) throws SmbException {
        
        Long modifiedDate = null;
        Long length = null;
        
        int attemptCount = 0;
        
        while (((modifiedDate == null) || (length == null)) && (attemptCount < 3)) {
            try {
                
                if (modifiedDate == null) {
                    modifiedDate = file.lastModified();
                    attemptCount = 0;
                }
                if (length == null) {
                    length = file.length();
                }
                
                attemptCount++;
            }
            catch (SmbException ex) {
                if (attemptCount < 3) {
                    System.out.println("Failed to query file " + file.getPath() + ", retrying.");
                }
                else {
                    throw ex;
                }
            }
        }
        
        return new FileSnapshot(file.getName(), length, new DateTime(new Date(modifiedDate)), toRelativePath(path));
    }
    
    private String toRelativePath(String absolutePath) {
        return toRelativePath(absolutePath, false);
    }
    
    private String toRelativePath(String absolutePath, boolean withTrailingSlash) {
        if (!absolutePath.startsWith(rootPath)) return absolutePath;
        
        String relative = absolutePath.substring(rootPath.length());
        
        if (withTrailingSlash && !relative.endsWith("/")) {
            relative = relative + "/";
        }
        if (!withTrailingSlash && relative.endsWith("/")) {
            relative = relative.substring(0, relative.length() - 1);
        }
        
        return relative;
    }
        
    private String toAbsoluteDirPath(String path) {
        return String.format("%s%s/", rootPath, path.endsWith("/")? path.substring(0, path.length() - 1) : path);
    }
    
    private String toAbsoluteFilePath(String relativePath) {
        return String.format("%s%s", rootPath, relativePath);
    }
}
