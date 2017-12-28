package lsfusion.server.data;

import lsfusion.base.BaseUtils;
import lsfusion.base.ExtInt;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.MOrderExclSet;
import lsfusion.base.col.interfaces.mutable.add.MAddCol;
import lsfusion.server.classes.*;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.*;
import lsfusion.server.logics.i18n.LocalizedString;

import java.io.*;
import java.sql.*;

public class JDBCTable {
    public final boolean singleRow;
    public final ImOrderSet<String> fields;
    public final ImMap<String, Type> fieldTypes;
    public final ImList<ImMap<String, Object>> set;

    public JDBCTable(boolean singleRow, ImOrderSet<String> fields, ImMap<String, Type> fieldTypes, ImList<ImMap<String, Object>> set) {
        this.singleRow = singleRow;
        this.fields = fields;
        this.fieldTypes = fieldTypes;
        this.set = set;
    }

    private static class JDBCDataClass extends DataClass<Object> {
        private final int sqlType;
        private final String sqlName; // тут конечно стремновато, в разных СУБД могут разные имена, но чудес не бывает
        
        public JDBCDataClass(int sqlType, String sqlName) {
            super(LocalizedString.create("JDBC"));
            this.sqlType = sqlType;
            this.sqlName = sqlName;
        }

        public DataClass getCompatible(DataClass compClass, boolean or) {
            throw new UnsupportedOperationException();
        }
        public Object getDefaultValue() {
            throw new UnsupportedOperationException();
       }
        public byte getTypeID() {
            throw new UnsupportedOperationException();
        }
        protected Class getReportJavaClass() {
            throw new UnsupportedOperationException();
        }
        public String getString(Object value, SQLSyntax syntax) {
            throw new UnsupportedOperationException();
        }
        protected int getBaseDotNetSize() {
            throw new UnsupportedOperationException();
        }
        public String getDotNetType(SQLSyntax syntax, TypeEnvironment typeEnv) {
            throw new UnsupportedOperationException();
        }
        public String getDotNetRead(String reader) {
            throw new UnsupportedOperationException();
        }
        public String getDotNetWrite(String writer, String value) {
            throw new UnsupportedOperationException();
        }
        public Object parseString(String s) throws ParseException {
            throw new UnsupportedOperationException();
        }
        public Object format(Object value) {
            throw new UnsupportedOperationException();
        }
        public String getSID() {
            throw new UnsupportedOperationException();
        }

        public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
            return sqlName;
        }
        public int getSQL(SQLSyntax syntax) {
            return sqlType;
        }
        public boolean isSafeString(Object value) {
            return false;
        }
        protected void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
            statement.setObject(num, value, sqlType);
        }
        public Object read(Object value) {
            return value;
        }
    }

    private static Type getType(ResultSetMetaData metaData, int column) throws SQLException {
        int sqlType = metaData.getColumnType(column);
        
        switch (sqlType) {
            case Types.BIT:
            case Types.BOOLEAN:
                return LogicalClass.instance;
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
                return IntegerClass.instance;
            case Types.BIGINT:
                return LongClass.instance;
            case Types.FLOAT:
            case Types.REAL:
            case Types.DOUBLE:
                return DoubleClass.instance;
            case Types.NUMERIC:
            case Types.DECIMAL:
                int precision = metaData.getPrecision(column);
                return NumericClass.get(metaData.getScale(column) + precision, precision);
            case Types.CHAR:
            case Types.NCHAR:
                precision = metaData.getPrecision(column);
                if(precision <= 0)
                    return StringClass.get(ExtInt.UNLIMITED);
                return StringClass.get(precision);
            case Types.VARCHAR:
            case Types.NVARCHAR:
            case Types.LONGVARCHAR:
            case Types.LONGNVARCHAR:
                precision = metaData.getPrecision(column);
                if(precision <= 0)
                    return StringClass.text;
                return StringClass.getv(precision);
            case Types.DATE:
                return DateClass.instance;
            case Types.TIME:
                return TimeClass.instance;
            case Types.TIMESTAMP:
                return DateTimeClass.instance;
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                return ByteArrayClass.instance;
            case Types.NULL:
                return NullReader.typeInstance;
//            case Types.ARRAY:
//                return ArrayClass.get;
        }
        return new JDBCDataClass(sqlType, metaData.getColumnTypeName(column));
    }

    public static byte[] serialize(boolean singleRow, ImOrderSet<String> fields, Type.Getter<String> fieldTypes, ImList<ImMap<String, Object>> set) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream o = new DataOutputStream(b);
        o.writeBoolean(singleRow); //singleRow
        if(singleRow)
            set = set.subList(0, Math.min(set.size(), 1));
        o.writeInt(fields.size());
        for(String field : fields) {
            BaseUtils.serializeString(o, field);
            TypeSerializer.serializeType(o, fieldTypes.getType(field));
        }
        o.writeBoolean(true); // fixed size
        o.writeInt(set.size());
        for(ImMap<String, Object> row : set) {
            for (String field : fields)
                BaseUtils.serializeObject(o, row.get(field));
        }
        return b.toByteArray();
    }

    // для оптимизации не будем готоваить сначала set'ы / map'ы а сразу сериализуем в результат
    public static byte[] serialize(ResultSet set) throws IOException, SQLException {
        ResultSetMetaData metaData = set.getMetaData();

        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream o = new DataOutputStream(b);
        o.writeBoolean(false); //singleRow

        int cc = metaData.getColumnCount();
        MAddCol<Type> types = ListFact.mAddCol(cc);
        o.writeInt(cc);
        for(int i=1;i<=cc;i++) {
            String field = metaData.getColumnName(i);
            BaseUtils.serializeString(o, field);
            Type type = getType(metaData, i);
            types.add(type);
            TypeSerializer.serializeType(o, type);
        }
        o.writeBoolean(false); // dynamic size
        while(set.next()) {
            o.writeBoolean(true);
            for(int i=0;i<cc;i++) {
                Object value = set.getObject(i + 1);
                Type type = types.get(i);
                if(type instanceof IntegerClass && value instanceof Boolean) // для tinyint выставляется IntegerClass, а jdbc driver для tinyint(1) может вернуть Boolean, а в read это пока не кладем 
                    value = (Boolean) value ? 1 : 0;
                BaseUtils.serializeObject(o, type.read(value));
            }
        }
        o.writeBoolean(false);
        return b.toByteArray();
    }

    public static JDBCTable deserializeJDBC(byte[] array) throws IOException {
        ByteArrayInputStream b = new ByteArrayInputStream(array);
        DataInputStream in = new DataInputStream(b);

        boolean singleRow = in.readBoolean();
        int fieldCount = in.readInt();
        MOrderExclSet<String> mFields = SetFact.mOrderExclSet(fieldCount);
        MExclMap<String, Type> mFieldTypes = MapFact.mExclMap(fieldCount);
        for (int i = 0; i < fieldCount; i++) {
            String field = BaseUtils.deserializeString(in);
            mFields.exclAdd(field);
            mFieldTypes.exclAdd(field, TypeSerializer.deserializeType(in));
        }
        ImOrderSet<String> fields = mFields.immutableOrder();

        MList<ImMap<String, Object>> mList;
        if(in.readBoolean()) { // fixed size 
            int rowCount = in.readInt();
            mList = ListFact.mList(rowCount);
            for (int i = 0; i < rowCount; i++)
                mList.add(deserializeRow(in, fieldCount, fields));
        } else {
            mList = ListFact.mList();
            while (in.readBoolean())
                mList.add(deserializeRow(in, fieldCount, fields));
        }        

        return new JDBCTable(singleRow, fields, mFieldTypes.immutable(), mList.immutableList());
    }

    public static ImMap<String, Object> deserializeRow(DataInputStream in, int fieldCount, ImOrderSet<String> fields) throws IOException {
        MExclMap<String, Object> mRow = MapFact.mExclMap(fieldCount);
        for (int j = 0; j < fieldCount; j++)
            mRow.exclAdd(fields.get(j), BaseUtils.deserializeObject(in));
        return mRow.immutable();
    }
}
