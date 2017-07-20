package lsfusion.server.logics.property.actions.importing.mdb;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.server.classes.DateTimeClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.importing.ImportDataActionProperty;
import lsfusion.server.logics.property.actions.importing.ImportIterator;

import java.io.IOException;
import java.util.*;

public class ImportMDBDataActionProperty extends ImportDataActionProperty {
    public ImportMDBDataActionProperty(ValueClass valueClass, List<String> ids, List<LCP> properties, BaseLogicsModule baseLM) {
        super(new ValueClass[] {valueClass}, ids, properties, baseLM);
    }

    @Override
    public ImportIterator getIterator(byte[] file) {

        try {
            List<Map<String, Object>> rows = (List<Map<String, Object>>) BaseUtils.deserializeCustomObject(file);
            List<List<String>> rowsList = new ArrayList<>();

            Map<String, Integer> fieldMapping = new HashMap<>();
            int i = 0;
            if(!rows.isEmpty()) {
                for (Map.Entry<String, Object> entry : rows.get(0).entrySet()) {
                    fieldMapping.put(entry.getKey(), i);
                    i++;
                }
            }

            for (Map<String, Object> row : rows) {
                List<String> entryList = new ArrayList<>();
                for (Object entry : row.values()) {
                    if(entry instanceof Date)
                        entryList.add(DateTimeClass.getDateTimeFormat().format(entry));
                    else
                        entryList.add(entry == null ? null : String.valueOf(entry));
                }
                rowsList.add(entryList);
            }

            List<Integer> sourceColumns = getSourceColumns(fieldMapping);

            return new ImportMDBIterator(rowsList, sourceColumns);
            
        } catch (IOException | ClassNotFoundException e) {
            throw Throwables.propagate(e);
        }
    }
}