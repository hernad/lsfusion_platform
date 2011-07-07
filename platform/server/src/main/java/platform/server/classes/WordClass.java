package platform.server.classes;

import platform.interop.Data;
import platform.server.data.type.ParseException;
import platform.server.logics.ServerResourceBundle;

public class WordClass extends FileClass {

    public final static WordClass instance = new WordClass();
    private final static String sid = "WordClass";
    static {
        DataClass.storeClass(sid, instance);
    }

    protected WordClass() {}

    public String toString() {
        return ServerResourceBundle.getString("classes.word.file");
    }

    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof WordClass ? this : null;
    }

    public byte getTypeID() {
        return Data.WORD;
    }

    public String getSID() {
        return sid;
    }

    public String getExtensions() {
        return "doc,docx";
    }
}
