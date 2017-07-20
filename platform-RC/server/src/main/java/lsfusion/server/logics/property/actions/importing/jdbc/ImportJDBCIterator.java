package lsfusion.server.logics.property.actions.importing.jdbc;

import com.google.common.base.Throwables;
import com.sun.rowset.CachedRowSetImpl;
import lsfusion.server.classes.DateClass;
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassType;
import lsfusion.server.logics.property.actions.importing.ImportIterator;

import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ImportJDBCIterator extends ImportIterator {
    CachedRowSetImpl rs;
    List<Integer> sourceColumns;
    private final List<LCP> properties;

    public ImportJDBCIterator(CachedRowSetImpl rs, List<Integer> sourceColumns, List<LCP> properties) {
        this.rs = rs;
        this.sourceColumns = sourceColumns;
        this.properties = properties;
    }

    @Override
    public List<String> nextRow() {
        try {
            if (rs.next()) {
                List<String> listRow = new ArrayList<>();
                for (Integer column : sourceColumns) {
                    ValueClass valueClass = properties.get(sourceColumns.indexOf(column)).property.getValueClass(ClassType.valuePolicy);
                    if (valueClass instanceof DateClass) {
                        Date value = rs.getDate(column);
                        listRow.add(value == null ? null : DateClass.getDateFormat().format(value));
                    } else if (valueClass instanceof IntegerClass) {
                        Object value = rs.getObject(column);
                        String result = null;
                        if(value != null) {
                            if (value instanceof Boolean)
                                result = String.valueOf((Boolean) value ? 1 : 0);
                            else
                                result = String.valueOf(value);
                        }
                        listRow.add(result);
                    } else
                        listRow.add(rs.getString(column));
                }
                return listRow;
            }
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }
        return null;
    }

    @Override
    protected void release() {
        if (rs != null)
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
    }
}