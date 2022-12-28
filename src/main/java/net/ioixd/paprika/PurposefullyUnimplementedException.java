package net.ioixd.paprika;

public class PurposefullyUnimplementedException extends RuntimeException {
    public PurposefullyUnimplementedException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
    public PurposefullyUnimplementedException(Throwable err) {
        super("Does not need to be implemented. If the Lua interpreter encounters this error it should");
    }
}
