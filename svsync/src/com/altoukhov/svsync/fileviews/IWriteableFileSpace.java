package com.altoukhov.svsync.fileviews;

import java.io.InputStream;
import com.altoukhov.svsync.FileSnapshot;

/**
 * @author Sval
 */
public interface IWriteableFileSpace {
    boolean createDirectory(String path);
    boolean deleteDirectory(String path);
    
    boolean deleteFile(String path);
    boolean writeFile(InputStream fileStream, FileSnapshot file);
    
    boolean isMoveFileSupported();
    boolean moveFile(String oldPath, String newPath);
}
