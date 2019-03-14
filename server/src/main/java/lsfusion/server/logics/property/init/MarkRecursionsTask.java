package lsfusion.server.logics.property.init;

import lsfusion.server.logics.property.ActionProperty;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.cases.CaseUnionProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.action.flow.ListCaseActionProperty;

public class MarkRecursionsTask extends GroupPropertiesTask {

    public String getCaption() {
        return "Looking for recursions in abstract actions / properties";
    }

    protected void runTask(Property property) {
        if (property instanceof ActionProperty) {
            if (property instanceof ListCaseActionProperty && ((ListCaseActionProperty) property).isAbstract()) {
                ((ListCaseActionProperty) property).markRecursions();
            }
        }

        if (property instanceof CalcProperty) {
            if (property instanceof CaseUnionProperty && ((CaseUnionProperty) property).isAbstract()) {
                ((CaseUnionProperty) property).checkRecursions();
            }
        }
    }

}
