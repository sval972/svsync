/*
Copyright 2015 Alex Altoukhov

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.altoukhov.svsync.fileviews;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import com.altoukhov.svsync.FileSnapshot;
import com.altoukhov.svsync.Snapshot;

/**
 * @author Alex Altoukhov
 */
public class LocalFileSpace extends FileSpace implements IScannableFileSpace, IReadableFileSpace, IWriteableFileSpace {
    
    private String rootPath;
    private String rootSuffix = "";
    
    private LocalFileSpace(String root) {
        rootPath = trimFilePath(root);
    }
    
    public LocalFileSpace(String root, String suffix) {
        this(root);
        
        rootPath = rootPath + File.separator + suffix;
        rootSuffix = suffix;
    }
    
    public LocalFileSpace(String root, List<String> excludes) {
        this(root);
        
        for (String exclude : excludes) {
            this.excludes.add(trimFilePath(toAbsolutePath(exclude)));
        }
    }
    
    public boolean init() {
        
        String rawRootPath = rootSuffix.isEmpty()? rootPath : rootPath.substring(0, rootPath.lastIndexOf(rootSuffix) -1);
        
        File rawRoot = new File(rawRootPath);
        
        if (rawRoot.exists() && rawRoot.isDirectory()) {
            
            File root = new File(rootPath);            
            
            if (!root.exists()) {
                root.mkdirs();
            }
            
            return true;
        }
        
        return false;
    }
    
    @Override
    protected Snapshot scan(List<Pattern> filters) {
        try {        
            Map<String, FileSnapshot> files = new LinkedHashMap<>();
            Set<String> dirs = new HashSet<>();

            File root = new File(rootPath);

            if (root.exists()) {

                Stack<File> stack = new Stack<>();
                stack.push(root);
                dirs.add("");

                while (!stack.isEmpty()) {
                    File currentFolder = stack.pop();

                    for (final File file : currentFolder.listFiles(filter)) {

                        if (file.isFile() && !isExcluded(trimFilePath(file.getAbsolutePath())) && !isFiltered(toRelativePath(file.getAbsolutePath()), filters)) {
                            FileSnapshot fileSnapshot = new FileSnapshot(file.getName(), file.length(), new DateTime(new Date(file.lastModified())), toRelativePath(file.getAbsolutePath()));
                            files.put(fileSnapshot.getRelativePath(), fileSnapshot);
                        }
                        else if (file.isDirectory() && !isExcluded(trimFilePath(file.getAbsolutePath())) && !isFiltered(toRelativePath(file.getAbsolutePath(), true), filters)) {
                            stack.push(file);
                            dirs.add(toRelativePath(file.getAbsolutePath()));
                            System.out.println("Scanning " + file.getAbsolutePath());
                        }
                    }
                }
            }        
            Snapshot snapshot = new Snapshot(files, dirs);
            return snapshot;
        }
        catch (SecurityException ex) {
            System.out.println("Failed to scan file space");
            System.out.println(ex.getMessage());
        }
        
        return null;
    }    
    
    @Override
    public InputStream readFile(String path){
        
        try {
            File file = new File(toAbsolutePath(path));
            return new FileInputStream(file);
        }
        catch (IOException ex) {
            System.out.println(ex.getMessage());
            return null;
        }
    }    
    
    // IWriteableFileSpace
    
    @Override
    public boolean deleteFile(String path) {
        try {
            return Files.deleteIfExists(Paths.get(toAbsolutePath(path)));
        }
        catch (IOException ex) {
            System.out.println("Failed to delete file: " + ex.getMessage());
        }
        
        return false;
    }
    
    private boolean setFileTimestamp(String path, DateTime timestamp) {
        
        try {
            File file = new File(toAbsolutePath(path));
            return file.setLastModified(timestamp.toDate().getTime());
        }
        catch (Exception ex) {
            System.out.println("Failed to set file's original timestamp: " + ex.getMessage());
            return false;
        }
    }

    private OutputStream getOutputFileStream(String path) {
        try {
            File file = new File(toAbsolutePath(path));
            return new FileOutputStream(file);
        }
        catch (IOException ex) {
            System.out.println(ex.getMessage());
            return null;
        }
    }

    @Override
    public boolean createDirectory(String path) {
        File dir = new File(toAbsolutePath(path));
        return dir.mkdir();
    }

    @Override
    public boolean deleteDirectory(String path) {
        File dir = new File(toAbsolutePath(path));
        return dir.delete();
    }

    @Override
    public boolean isMoveFileSupported() {
        return true;
    }

    @Override
    public boolean moveFile(String oldPath, String newPath) {
        File oldFile = new File(toAbsolutePath(oldPath));
        File newFile = new File(toAbsolutePath(newPath));
        return oldFile.renameTo(newFile);
    }
    
    private final FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                return !file.isHidden();
            }
        };
    
    @Override
    public boolean writeFile(InputStream fileStream, FileSnapshot file) {
        
        if (fileStream == null) return false;
        
        OutputStream out = null;
        long bytesWritten = 0;
        boolean setTimestamp = false;
        
        try {
            out = getOutputFileStream(file.getRelativePath());
            if (out == null) {
                System.out.println("Failed to open file for write: " + file.getRelativePath());
                return false;
            }
                
            if (file.isLargeFile()) {
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
        
        setTimestamp = setFileTimestamp(file.getRelativePath(), file.getModifiedTimestamp());
        return setTimestamp && (bytesWritten == file.getFileSize());
    }
    
    /*private boolean isExcluded(String file) {
        
        file = trimFilePath(file);
        
        for (String exclude : excludes) {
            if (file.startsWith(exclude)) {
                return true;
            }
        }
        
        return false;
    }*/

    private String trimFilePath(String path) {
        return path.endsWith(File.separator)? path.substring(0, path.length()-1) : path;
    }
    
    private String toAbsolutePath(String path) {
        return String.format("%s%s%s", rootPath, File.separator, path.replace("/", File.separator));
    }

    private String toRelativePath(String path) {
        return toRelativePath(path, false);
    }
        
    private String toRelativePath(String path, boolean withTrailingSlash) {
        if (!path.startsWith(rootPath)) return path;
        
        String relative = path.substring(rootPath.length());
        if (relative.startsWith(File.separator)) {
            relative = relative.substring(1);
        }
        
        if (withTrailingSlash && !relative.endsWith(File.separator)) {
            relative = relative + File.separator;
        }
        if (!withTrailingSlash && relative.endsWith(File.separator)) {
            relative = relative.substring(0, relative.length() - 1);
        }
        
        return relative.replace("\\", "/");
    }
}
