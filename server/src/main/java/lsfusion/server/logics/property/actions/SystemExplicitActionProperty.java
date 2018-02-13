package lsfusion.server.logics.property.actions;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.i18n.LocalizedString;

// usually are created from parser
public abstract class SystemExplicitActionProperty extends ExplicitActionProperty {


    protected SystemExplicitActionProperty() {
        super();
    }
    
    protected SystemExplicitActionProperty(LocalizedString caption) {
        super(caption, new ValueClass[]{});
    }
    
    protected SystemExplicitActionProperty(ValueClass... classes) {
        super(classes);
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }

    protected SystemExplicitActionProperty(LocalizedString caption, ValueClass... classes) {
        super(caption, classes);
    }
}
