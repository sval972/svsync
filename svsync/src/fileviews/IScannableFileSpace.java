package svsync.fileviews;

import java.util.Collection;
import svsync.Snapshot;

/**
 * @author Sval
 */
public interface IScannableFileSpace {
    Snapshot scan(Collection<String> filters);
}
