package fileindexer;

public enum SkipReason {
    HIDDEN,
    EXT_NOT_ALLOWED,
    TOO_LARGE,
    IO_ERROR,
    NOT_A_FILE,
    DUPLICATE
}
