package platform.gwt.form.shared.actions.form;

import platform.gwt.view.changes.dto.ObjectDTO;

public class ChangeClassView extends FormBoundAction<FormChangesResult> {
    public int groupObjectId;
    public ObjectDTO value;

    public ChangeClassView() {}

    public ChangeClassView(int groupObjectId, Object value) {
        this.groupObjectId = groupObjectId;
        this.value = new ObjectDTO(value);
    }
}
