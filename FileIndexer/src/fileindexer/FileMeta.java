package fileindexer;

public class FileMeta {

    private final String absolutePath;
    private final long sizeBytes;
    private final long lastModifiedMillis;
    private final String extension;

    public FileMeta(String absolutePath, long sizeBytes, long lastModifiedMillis, String extension) {
        this.absolutePath = absolutePath;
        this.sizeBytes = sizeBytes;
        this.lastModifiedMillis = lastModifiedMillis;
        this.extension = extension;
    }

    public String getAbsolutePath() { return absolutePath; }
    public long getSizeBytes() { return sizeBytes; }
    public long getLastModifiedMillis() { return lastModifiedMillis; }
    public String getExtension() { return extension; }

    @Override
    public String toString() {
        return "FileMeta{" +
                "path='" + absolutePath + '\'' +
                ", size=" + sizeBytes +
                ", lastModified=" + lastModifiedMillis +
                ", ext='" + extension + '\'' +
                '}';
    }
}
