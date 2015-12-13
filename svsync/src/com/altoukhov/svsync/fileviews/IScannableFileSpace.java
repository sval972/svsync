package com.altoukhov.svsync.fileviews;

import java.util.Collection;
import com.altoukhov.svsync.Snapshot;

/**
 * @author Sval
 */
public interface IScannableFileSpace {
    Snapshot scan(Collection<String> filters);
}
