package svsync;

/**
 * @author Sval
 */
public class Stat {
    
    private long sourceSize = 0;
    private long updateSize = 0;
    private long newSpaceSize = 0;
    private long transferSize = 0;
    
    public long getSourceSize() {
        return sourceSize;
    }

    public long getUpdateSize() {
        return updateSize;
    }

    public long getNewSpaceSize() {
        return newSpaceSize;
    }

    public long getTransferSize() {
        return transferSize;
    }    
    
    public Stat() {
    }
    
    public Stat(Snapshot source, Snapshot target, Diff diff) {
        
        for (FileSnapshot file : source.getFiles().values()) {
            sourceSize += file.getFileSize();
        }        
        
        for (FileSnapshot file : diff.getFileChanges(Diff.DiffType.CHANGED)) {
            updateSize += file.getFileSize();
        }
        
        long addedSize = 0;
        for (FileSnapshot file : diff.getFileChanges(Diff.DiffType.ADDED)) {
            addedSize += file.getFileSize();
        }
        
        long deletedSize = 0;
        for (FileSnapshot file : diff.getFileChanges(Diff.DiffType.DELETED)) {
            deletedSize += file.getFileSize();
        }
        
        long changeSize = 0;
        for (FileSnapshot file : diff.getFileChanges(Diff.DiffType.CHANGED)) {
            changeSize += target.getFiles().get(file.getRelativePath()).getFileSize();
        }        
        
        transferSize = addedSize + updateSize;
        newSpaceSize = addedSize - deletedSize + (updateSize - changeSize);
    }
    
    public Stat merge(Stat other) {
        
        Stat merged = new Stat();
        merged.sourceSize = this.sourceSize + other.sourceSize;
        merged.updateSize = this.updateSize + other.updateSize;
        merged.newSpaceSize = this.newSpaceSize + other.newSpaceSize;
        merged.transferSize = this.transferSize + other.transferSize;
        
        return merged;
    }
    
    @Override
    public String toString() {
        return "Stat{" 
                + "sourceSize=" + Utils.readableFileSize(sourceSize)
                + ", updateSize=" + Utils.readableFileSize(updateSize)
                + ", newSpaceSize=" + Utils.readableFileSize(newSpaceSize)
                + ", transferSize=" + Utils.readableFileSize(transferSize)
                + '}';
    }
    
}
