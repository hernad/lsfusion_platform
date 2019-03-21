package lsfusion.client.form.property.cell.controller;

import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.classes.ClientType;

public interface EditPropertyHandler {
    boolean requestValue(ClientType valueType, Object oldValue);
    ClientFormController getForm();
    
    void updateEditValue(Object value);
}