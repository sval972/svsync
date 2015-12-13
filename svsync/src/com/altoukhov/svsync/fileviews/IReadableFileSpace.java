package com.altoukhov.svsync.fileviews;

import java.io.InputStream;

/**
 * @author Sval
 */
public interface IReadableFileSpace {
    InputStream readFile(String path);
}
