package lsfusion.server.logics.property.actions;

import com.google.common.base.Throwables;
import jasperapi.ClientReportData;
import jasperapi.ReportGenerator;
import jasperapi.ReportPropertyData;
import lsfusion.base.IOUtils;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.FormPrintType;
import lsfusion.interop.action.LogMessageClientAction;
import lsfusion.interop.action.ReportClientAction;
import lsfusion.interop.form.ReportGenerationData;
import lsfusion.interop.form.ReportGenerationDataType;
import lsfusion.server.SystemProperties;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormSelector;
import lsfusion.server.form.entity.ObjectSelector;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.*;
import net.sf.jasperreports.engine.JRException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class PrintActionProperty<O extends ObjectSelector> extends FormStaticActionProperty<O, FormPrintType> {

    private CalcPropertyInterfaceImplement<ClassPropertyInterface> printerProperty;

    private final LCP formPageCount;

    private final boolean syncType; // static interactive

    public PrintActionProperty(LocalizedString caption,
                               FormSelector<O> form,
                               final List<O> objectsToSet,
                               final List<Boolean> nulls,
                               FormPrintType staticType,
                               boolean syncType,
                               Integer top,
                               LCP formExportFile,
                               CalcPropertyMapImplement printer,
                               ImOrderSet<PropertyInterface> innerInterfaces,
                               LCP formPageCount) {
        super(caption, form, objectsToSet, nulls, staticType, formExportFile, top, printer == null ? null : printer.property);

        this.formPageCount = formPageCount;

        this.syncType = syncType;

        if (printer != null) {
            ImRevMap<PropertyInterface, ClassPropertyInterface> mapInterfaces = getMapInterfaces(innerInterfaces).reverse();
            this.printerProperty = printer.map(mapInterfaces);
        }
    }

    @Override
    protected Map<String, byte[]> exportPlain(ReportGenerationData reportData) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected byte[] exportHierarchical(ReportGenerationData reportData) throws JRException, IOException, ClassNotFoundException {
        return IOUtils.getFileBytes(ReportGenerator.exportToFile(reportData, (FormPrintType) staticType));
    }

    @Override
    protected void exportClient(ExecutionContext<ClassPropertyInterface> context, LocalizedString caption, ReportGenerationData reportData, Map<String, String> reportPath) throws SQLException, SQLHandledException {
        if (staticType == FormPrintType.MESSAGE) {
            printMessage(caption, context, reportData);
        } else {
            String pName = printerProperty == null ? null : (String) printerProperty.read(context, context.getKeys());
            Object pageCount = context.requestUserInteraction(new ReportClientAction(reportPath, syncType, reportData, (FormPrintType) staticType, pName, SystemProperties.isDebug));
            formPageCount.change(pageCount, context);
        }
    }

    private void printMessage(LocalizedString caption, ExecutionContext context, ReportGenerationData reportData) {

        try {

            Map<String, ClientReportData> data = ReportGenerator.retrieveReportSources(reportData, null, ReportGenerationDataType.PRINT).data;
            Map<String, Map<String, String>> propertyCaptionsMap = ReportGenerator.retrievePropertyCaptions(reportData);
            assert data.size() == 1;
            for (Map.Entry<String, ClientReportData> dataEntry : data.entrySet()) {
                String key = dataEntry.getKey();
                ClientReportData clientData = dataEntry.getValue();
                Map<String, String> propertyCaptions = propertyCaptionsMap.get(key);
                Map<String, ReportPropertyData> properties = clientData.getProperties();
                List<String> titles = getTitleRow(properties);
                List<String> titleRow = new ArrayList<>();
                for(String title : titles) {
                    titleRow.add(propertyCaptions.get(title));
                }
                List<List<String>> dataRows = new ArrayList();
                for (HashMap<Integer, Object> keyRow : clientData.getKeyRows()) {
                    Map<ReportPropertyData, Object> row = clientData.getRows().get(keyRow);
                    if(row != null) {
                        List<String> dataRow = new ArrayList<>();
                        for (int i = 0; i < titles.size(); i++) {
                            dataRow.add(String.valueOf(row.get(properties.get(titles.get(i)))));
                        }
                        dataRows.add(dataRow);
                    }
                }
                LogMessageClientAction action = new LogMessageClientAction(ThreadLocalContext.localize(caption), titleRow, dataRows, !context.getSession().isNoCancelInTransaction());
                if(syncType)
                    context.requestUserInteraction(action);
                else
                    context.delayUserInteraction(action);

            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private List<String> getTitleRow(Map<String, ReportPropertyData> properties) {
        List<String> titleRow = new ArrayList<>();
        for(Map.Entry<String, ReportPropertyData> property : properties.entrySet()) {
            if(!property.getValue().propertyType.equals("ActionClass"))
                titleRow.add(property.getKey());
        }
        return titleRow;
    }
}