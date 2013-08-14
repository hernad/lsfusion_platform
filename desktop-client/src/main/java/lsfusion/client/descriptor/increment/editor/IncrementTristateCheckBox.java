package lsfusion.client.descriptor.increment.editor;

import lsfusion.base.ReflectionUtils;
import lsfusion.base.context.ApplicationContextProvider;
import lsfusion.base.context.IncrementView;
import lsfusion.client.descriptor.editor.base.TristateCheckBox;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class IncrementTristateCheckBox extends TristateCheckBox implements IncrementView, ItemListener, ChangeListener {

    private final Object object;
    private final String field;

    public IncrementTristateCheckBox(String title, ApplicationContextProvider object, String field) {
        super(title);
        this.object = object;
        this.field = field;

        addChangeListener(this);
        addItemListener(this);

        object.getContext().addDependency(object, field, this);
    }

    public void itemStateChanged(ItemEvent e) {
        ReflectionUtils.invokeSetter(object, field, getStateAsBoolean());
    }

    public void stateChanged(ChangeEvent e) {
        ReflectionUtils.invokeSetter(object, field, getStateAsBoolean());
    }

    @Override
    protected void onChange() {
        super.onChange();
    }

    public void update(Object updateObject, String updateField) {
        setStateFromBoolean((Boolean) ReflectionUtils.invokeGetter(object, field));
    }
}
