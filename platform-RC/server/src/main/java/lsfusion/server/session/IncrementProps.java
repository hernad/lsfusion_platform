package lsfusion.server.session;

import lsfusion.base.ConcurrentIdentityWeakHashSet;
import lsfusion.base.WeakIdentityHashSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.OverrideSessionModifier;
import lsfusion.server.logics.property.PropertyInterface;

public abstract class IncrementProps {

    // пока не concurrent, но в будущем возможно понадобится
    private WeakIdentityHashSet<OverrideSessionModifier> modifiers = new WeakIdentityHashSet<>();
    public void registerView(OverrideSessionModifier modifier) {
        modifiers.add(modifier);
        modifier.eventDataChanges(getProperties());
    }
    public void unregisterView(OverrideSessionModifier modifier) {
        modifiers.remove(modifier);
    }

    private WeakIdentityHashSet<OverrideIncrementProps> increments = new WeakIdentityHashSet<>();
    public void registerView(OverrideIncrementProps modifier) {
        increments.add(modifier);
    }
    public void unregisterView(OverrideIncrementProps modifier) {
        increments.remove(modifier);
    }

    public void eventChange(CalcProperty property, boolean sourceChanged) {
        eventChange(property, true, sourceChanged);
    }

    public void eventChange(CalcProperty property, boolean dataChanged, boolean sourceChanged) {
        for(OverrideIncrementProps increment : increments)
            increment.eventChange(property, dataChanged, sourceChanged);

         for(OverrideSessionModifier modifier : modifiers)
            modifier.eventIncrementChange(property, dataChanged, sourceChanged);
    }
    public void eventChanges(Iterable<? extends CalcProperty> properties) {
        for(CalcProperty property : properties)
            eventChange(property, true); // вызывается при clear, а значит все "источники" сбрасываются
    }
    
    public abstract <P extends PropertyInterface> PropertyChange<P> getPropertyChange(CalcProperty<P> property);
    public abstract ImSet<CalcProperty> getProperties();

    public abstract int getMaxCount(CalcProperty property);
    
    public abstract String out();
}