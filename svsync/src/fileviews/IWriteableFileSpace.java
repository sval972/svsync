package svsync.fileviews;

import java.io.InputStream;
import svsync.FileSnapshot;

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
