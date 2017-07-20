package lsfusion.server.logics.property.actions.file;

import lsfusion.base.BaseUtils;
import lsfusion.interop.action.SaveFileClientAction;
import lsfusion.server.classes.DynamicFormatFileClass;
import lsfusion.server.classes.FileClass;
import lsfusion.server.classes.StaticFormatFileClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ClassType;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.actions.SystemExplicitActionProperty;

import java.sql.SQLException;

public class SaveActionProperty extends FileActionProperty {
    private LCP<?> fileNameProp;

    public SaveActionProperty(LocalizedString caption, LCP fileProperty, LCP fileNameProp) {
        super(caption, fileProperty);

        this.fileNameProp = fileNameProp;

        drawOptions.setImage("save.png");
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ObjectValue[] objects = new ObjectValue[context.getKeyCount()];
        int i = 0;
        for (ClassPropertyInterface classInterface : interfaces) {
            objects[i++] = context.getKeyValue(classInterface);
        }

        String fileName = "new file";
        if (fileNameProp != null) {
            assert fileNameProp.property.interfaces.isEmpty(): "property expression after NAME should have no parameters";
            Object fileNameObject = fileNameProp.read(context);
            fileName = fileNameObject != null ? fileNameObject.toString().trim() : "";
        }

        FileClass fileClass = (FileClass) fileProperty.property.getType();
        for (byte[] file : fileClass.getFiles(fileProperty.read(context, objects))) {
            String extension;
            byte[] saveFile = file; 
            if (fileClass instanceof DynamicFormatFileClass) {
                extension = BaseUtils.getExtension(file);
                saveFile = BaseUtils.getFile(file);
            } else {
                extension = BaseUtils.firstWord(((StaticFormatFileClass) fileClass).getOpenExtension(file), ",");
            }
            context.delayUserInterfaction(new SaveFileClientAction(saveFile, fileName + "." + extension));
        }
    }
}