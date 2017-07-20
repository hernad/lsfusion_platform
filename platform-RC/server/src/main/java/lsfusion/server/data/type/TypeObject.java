package lsfusion.server.data.type;

import lsfusion.server.data.Field;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.logics.DataObject;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TypeObject extends AbstractParseInterface {

    private final Object object;
    private final Type type;

    public TypeObject(Object object, Type type) {
        this.object = object;
        this.type = type;

        assert this.object !=null;
    }

    public TypeObject(Object object, Type type, SQLSyntax syntax, boolean cast) {
        this(type.castValue(object, type, syntax),type);
        assert cast;
    }

    public TypeObject(DataObject dataObject, Field fieldTo, SQLSyntax syntax) {
        this(fieldTo.type.castValue(dataObject.object, dataObject.getType(), syntax),fieldTo.type);
    }

    public boolean isSafeString() {
        return type.isSafeString(object);
    }

    // нужно ли делать явный type (для дат важно)
    public boolean isSafeType() {
        return type.isSafeType();
    }

    public Type getType() {
        return type;
    }

    public String getString(SQLSyntax syntax, StringBuilder envString, boolean usedRecursion) {
        return type.getString(object, syntax);
    }

    public void writeParam(PreparedStatement statement, SQLSession.ParamNum paramNum, SQLSyntax syntax) throws SQLException {
        type.writeParam(statement, paramNum, object, syntax);
    }

    public void writeNullParam(PreparedStatement statement, SQLSession.ParamNum paramNum, SQLSyntax syntax, TypeEnvironment env) throws SQLException {
        type.writeParam(statement, paramNum, object, syntax);
    }

    public ConcatenateType getConcType() {
        if(type instanceof ConcatenateType)
            return (ConcatenateType) type;
        return null;
    }
}