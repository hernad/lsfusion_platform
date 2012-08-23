package platform.gwt.form2.shared.view;

import com.google.gwt.user.cellview.client.Column;
import platform.gwt.form2.client.form.ui.GFormController;
import platform.gwt.form2.shared.view.changes.GGroupObjectValue;
import platform.gwt.form2.shared.view.classes.GType;
import platform.gwt.form2.shared.view.grid.EditManager;
import platform.gwt.form2.shared.view.logics.FormLogicsProvider;
import platform.gwt.form2.shared.view.logics.GGroupObjectLogicsSupplier;
import platform.gwt.form2.shared.view.panel.PanelRenderer;
import platform.gwt.form2.shared.view.reader.*;

import java.util.Map;

public class GPropertyDraw extends GComponent implements GPropertyReader {
    public static final String CAPTION_ORIGINAL = "CAPTION_ORIGINAL";

    public int ID;
    public GGroupObject groupObject;
    public String sID;
    public String caption;
    public GType baseType;
    public GType changeType;
    public String iconPath;
    public Boolean focusable;
    public boolean checkEquals;
    public GPropertyEditType editType = GPropertyEditType.EDITABLE;

    public GCaptionReader captionReader;
    public GFooterReader footerReader;
    public GBackgroundReader backgroundReader;
    public GForegroundReader foregroundReader;

    public GPropertyDraw(){}

    public void update(GGroupObjectLogicsSupplier controller, Map<GGroupObjectValue, Object> values) {
        controller.updatePropertyDrawValues(this, values);
    }

    public Column<GridDataRecord, Object> createGridColumn(EditManager editManager, FormLogicsProvider form) {
        return baseType.createGridColumn(editManager, form, this);
    }

    public PanelRenderer createPanelRenderer(GFormController form) {
        return baseType.createPanelRenderer(form, this);
    }

    @Override
    public int getGroupObjectID() {
        return groupObject != null ? groupObject.ID : -1;
    }

    public String getCaptionOrEmpty() {
        return caption == null ? "" : caption;
    }

    public String getDynamicCaption(String caption) {
        return CAPTION_ORIGINAL.equals(caption) ? this.caption : caption;
    }

    public String getIconPath(boolean enabled) {
        if (!enabled && iconPath != null) {
            int dotInd = iconPath.lastIndexOf(".");
            if (dotInd != -1) {
                return iconPath.substring(0, dotInd) + "_Disabled" + iconPath.substring(dotInd);
            }
        }

        return iconPath;
    }

    @Override
    public String toString() {
        return "GPropertyDraw{" +
                ", sID='" + sID + '\'' +
                ", caption='" + caption + '\'' +
                ", baseType=" + baseType +
                ", changeType=" + changeType +
                ", iconPath='" + iconPath + '\'' +
                ", focusable=" + focusable +
                ", checkEquals=" + checkEquals +
                ", editType=" + editType +
                '}';
    }
}
