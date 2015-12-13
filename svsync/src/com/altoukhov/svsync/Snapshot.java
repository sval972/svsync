package com.altoukhov.svsync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import com.altoukhov.svsync.fileviews.IWriteableFileSpace;

/**
 * @author Sval
 */
public class Snapshot implements IWriteableFileSpace {
    
    private static final GsonBuilder gsonBuilder = new GsonBuilder();
    
    static {
        GsonSerializers serializers = new GsonSerializers();
        gsonBuilder.registerTypeAdapter(DateTime.class, serializers.new DateTimeSerializer());
        gsonBuilder.registerTypeAdapter(DateTime.class, serializers.new DateTimeDeserializer());
    }    
    
    private DateTime timestamp;
    private Map<String, FileSnapshot> files;
    private Set<String> directories;
    
    public Snapshot(Map<String, FileSnapshot> files, Set<String> dirs) {
        this.files = files;
        this.directories = dirs;
        this.timestamp = DateTime.now(DateTimeZone.UTC);
    }
    
    public DateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, FileSnapshot> getFiles() {
        return files;
    }

    public Set<String> getDirectories() {
        return directories;
    }
    
    public long size() {
        long size = 0;
        for (FileSnapshot file : files.values()) {
            size += file.getFileSize();
        }
        
        return size;
    }
    
    public static Diff diff(Snapshot source, Snapshot target, boolean detectMovedFiles) {
                
        Diff diff = new Diff();
        
        Collection<FileSnapshot> changed = diff.getFileChanges(Diff.DiffType.CHANGED);
        Collection<FileSnapshot> added = diff.getFileChanges(Diff.DiffType.ADDED);
        Collection<FileSnapshot> deleted = diff.getFileChanges(Diff.DiffType.DELETED);
        Collection<FileSnapshot> moved = diff.getFileChanges(Diff.DiffType.MOVED);
        
        Collection<String> dirAdded = diff.getDirectoryChanges(Diff.DiffType.ADDED);
        Collection<String> dirDeleted = diff.getDirectoryChanges(Diff.DiffType.DELETED);
        
        for (String filePath : target.files.keySet()) {
            if (source.files.containsKey(filePath)) {
                
                FileSnapshot fromFile = source.files.get(filePath);
                FileSnapshot toFile = target.files.get(filePath);
                
                if (!toFile.equals(fromFile)) {
                    changed.add(fromFile);
                }
            }
            else {
                deleted.add(target.files.get(filePath));
            }
        }
        
        for (String filePath : source.files.keySet()) {
            if (!target.files.containsKey(filePath)) {
                added.add(source.files.get(filePath));
            }
        }

        if (detectMovedFiles) {
            
            // Find moved items
            Set<FileSnapshot> movedFiles = new HashSet<>(added);
            movedFiles.retainAll(deleted);
                        
            if (!movedFiles.isEmpty()) {

                Map<FileSnapshot, List<FileSnapshot>> movedFromMap = new HashMap<>();
                Map<FileSnapshot, List<FileSnapshot>> movedToMap = new HashMap<>();
                
                for (FileSnapshot file : deleted) {
                    if (movedFiles.contains(file)) {
                        
                        if (!movedFromMap.containsKey(file)) {
                            movedFromMap.put(file, new ArrayList<FileSnapshot>());
                        }
                        
                        movedFromMap.get(file).add(file);
                    }
                }

                for (FileSnapshot file : added) {
                    if (movedFiles.contains(file)) {
                        
                        if (!movedToMap.containsKey(file)) {
                            movedToMap.put(file, new ArrayList<FileSnapshot>());
                        }
                        
                        movedToMap.get(file).add(file);
                    }
                }
                
                // Remove from added/deleted
                added.removeAll(movedFiles);
                deleted.removeAll(movedFiles);                
                
                for (FileSnapshot file : movedFiles) {
                    
                    List<FileSnapshot> fromList = movedFromMap.get(file);
                    List<FileSnapshot> toList = movedToMap.get(file);
                    
                    int i;
                    for (i=0; i<Math.min(fromList.size(), toList.size()); i++) {
                        FileSnapshot movedFile = toList.get(i);
                        movedFile.setPreviousPath(fromList.get(i).getRelativePath());
                        moved.add(movedFile);
                    }
                    
                    while (i<fromList.size()) {
                        deleted.add(fromList.get(i));
                        i++;
                    }
                    
                    while (i<toList.size()) {
                        added.add(toList.get(i));
                        i++;
                    }                    
                }
            }
        }
            
        // Find directory changes
        for (String dir : source.directories) {
            if (!target.directories.contains(dir)) {
                dirAdded.add(dir);
            }
        }

        for (String dir : target.directories) {
            if (!source.directories.contains(dir)) {
                dirDeleted.add(dir);
            }
        }
        
        diff.setStat(new Stat(source, target, diff));
        return diff;
    }
    
    private static Snapshot fromJson(String json) {
        Gson g = gsonBuilder.create();
        return g.fromJson(json, Snapshot.class);
    }
    
    public static Snapshot fromFile(String path) {
        try {            
            if (!(new File(path)).exists()) return null;
            String content = readFile(path, StandardCharsets.UTF_8);
            return fromJson(content);
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
            return null;
        }
    }
    
    private String toJson() {
        Gson g = gsonBuilder.setPrettyPrinting().create();
        return g.toJson(this);
    }
    
    public boolean toFile(String path) {
        Writer out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "UTF-8"));
            out.write(toJson());
        }
        catch (IOException ex) {
            System.out.println(ex.getMessage());
            return false;
        }
        finally {
            try {
                if (out != null) {
                    out.close();
                }
            }
            catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
        }
        
        return true;
    }
    
    private static String readFile(String path, Charset encoding) throws IOException 
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return encoding.decode(ByteBuffer.wrap(encoded)).toString();
    }

    @Override
    public boolean createDirectory(String path) {
        return directories.add(path);
    }

    @Override
    public boolean deleteDirectory(String path) {
        return directories.remove(path);
    }

    @Override
    public boolean deleteFile(String path) {
        files.remove(path);
        return true;
    }

    @Override
    public boolean writeFile(InputStream fileStream, FileSnapshot file) {
        files.put(file.getRelativePath(), file);
        return true;
    }

    @Override
    public boolean isMoveFileSupported() {
        return true;
    }

    @Override
    public boolean moveFile(String oldPath, String newPath) {
        
        if (files.containsKey(oldPath)) {
            FileSnapshot file = files.get(oldPath);
            FileSnapshot newFile = new FileSnapshot(file.getFileName(), file.getFileSize(), file.getModifiedTimestamp(), newPath);
            files.remove(oldPath);
            files.put(newPath, newFile);
            return true;
        }
        
        return false;
    }
}
