package lsfusion.server.classes.link;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.classes.CustomStaticFormatFileClass;
import lsfusion.server.classes.DataClass;
import lsfusion.server.classes.StaticFormatFileClass;

public abstract class StaticFormatLinkClass extends LinkClass {
    
    protected StaticFormatLinkClass(boolean multiple) {
        super(multiple);
    }

    public String getDefaultCastExtension() {
        return null;
    }

    protected ImSet<String> getExtensions() {
        return SetFact.singleton(getDefaultCastExtension());
    }

    @Override
    public DataClass getCompatible(DataClass compClass, boolean or) {
        if(!(compClass instanceof StaticFormatLinkClass))
            return null;

        StaticFormatLinkClass staticFileClass = (StaticFormatLinkClass)compClass;
        if(!(multiple == staticFileClass.multiple))
            return null;

        if(equals(staticFileClass))
            return this;

//        ImSet<String> mergedExtensions = getExtensions().merge(staticFileClass.getExtensions());
        return CustomStaticFormatLinkClass.get(multiple);
    }
}