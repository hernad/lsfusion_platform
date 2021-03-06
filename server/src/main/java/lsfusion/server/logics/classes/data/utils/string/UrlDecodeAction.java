package lsfusion.server.logics.classes.data.utils.string;

import com.google.common.base.Throwables;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.net.URLDecoder;
import java.util.Iterator;

public class UrlDecodeAction extends InternalAction {
    private final ClassPropertyInterface stringInterface;
    private final ClassPropertyInterface encodingInterface;

    public UrlDecodeAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        stringInterface = i.next();
        encodingInterface = i.next();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        try {
            String string = (String) context.getKeyValue(stringInterface).getValue();
            String encoding = (String) context.getKeyValue(encodingInterface).getValue();
            String encoded = URLDecoder.decode(string, encoding);
            findProperty("urlDecoded[]").change(encoded, context);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}