package lsfusion.server.form.entity;

import lsfusion.server.classes.CustomClass;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;

public class ListFormEntity<T extends BusinessLogics<T>> extends BaseClassFormEntity<T> {

    protected ListFormEntity(BaseLogicsModule<T> LM, CustomClass cls, String sID, String caption) {
        super(LM, cls, sID, caption);

        LM.addObjectActions(this, object);

        finalizeInit(LM.getVersion());
    }

    public ListFormEntity(BaseLogicsModule<T> LM, CustomClass cls) {
        this(LM, cls, "listForm_" + cls.getSID(), cls.caption);
    }
}
