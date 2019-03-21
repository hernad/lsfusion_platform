package lsfusion.gwt.client.form.object.table.grid.controller;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.Panel;
import lsfusion.gwt.client.base.view.ResizableSimplePanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.form.design.view.GFormLayoutImpl;
import lsfusion.gwt.client.form.object.table.grid.view.GGridTable;
import lsfusion.gwt.client.base.focus.DefaultFocusReceiver;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGridUserPreferences;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.table.grid.GGrid;
import lsfusion.gwt.client.form.order.user.GOrder;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.object.GGroupObjectValue;

import java.util.List;
import java.util.Map;

import static lsfusion.gwt.client.base.GwtClientUtils.setupFillParent;

public class GGridTableController {
    private static final GFormLayoutImpl layoutImpl = GFormLayoutImpl.get();
    
    private GGrid grid;
    private Panel gridView;
    private GGridTable table;
    private GGridController groupController;
    private boolean forceHidden = false;

    public GGridTableController(GGrid igrid, GFormController iformController, GGridController igroupObject, GGridUserPreferences[] userPreferences) {
        grid = igrid;
        groupController = igroupObject;

        table = new GGridTable(iformController, igroupObject, this, userPreferences, igrid.autoSize);

//        ResizableLayoutPanel panel = new ResizableLayoutPanel();
//        panel.setStyleName("gridResizePanel");
//        panel.setWidget(table);

        ResizableSimplePanel panel = new ResizableSimplePanel(table);
        panel.setStyleName("gridResizePanel");
        setupFillParent(panel.getElement(), table.getElement());
        if(grid.autoSize) { // убираем default'ый minHeight
            panel.getElement().getStyle().setProperty("minHeight", "0px");
            panel.getElement().getStyle().setProperty("minWidth", "0px");
        }

        gridView = layoutImpl.createGridView(grid, panel);
    }

    public int getHeaderHeight() {
        return grid.headerHeight;
    }

    public GGridTable getTable() {
        return table;
    }

    public GPropertyDraw getCurrentProperty() {
        return table.getCurrentProperty();
    }
    public GGroupObjectValue getCurrentColumn() {
        return table.getCurrentColumn();
    }

    public boolean containsProperty(GPropertyDraw property) {
        return table.containsProperty(property);
    }

    public Object getSelectedValue(GPropertyDraw property, GGroupObjectValue columnKey) {
        return table.getSelectedValue(property, columnKey);
    }

    public void setForceHidden(boolean hidden) {
        forceHidden = hidden;
    }
    
    public GFont getFont() {
        return grid.font;
    }

    public boolean isVisible() {
        return !forceHidden && groupController.isInGridClassView();
    }

    public void update() {
        table.update();
        boolean oldGridVisibilityState = gridView.isVisible();
        if (oldGridVisibilityState != isVisible()) {
            gridView.setVisible(isVisible());
        }
    }

    public void restoreScrollPosition() {
        table.restoreScrollPosition();
    }

    public void beforeHiding() {
        table.beforeHiding();
    }

    public void afterShowing() {
        table.afterShowing();
    }

    public void addToLayout(GFormLayout formLayout) {
        formLayout.add(grid, gridView, new DefaultFocusReceiver() {
            @Override
            public boolean focus() {
                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        table.setFocus(true);
                        groupController.scrollToTop();
                    }
                });
                return true;
            }
        });
    }

    public void updateCellBackgroundValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        table.updateCellBackgroundValues(propertyDraw, values);
    }

    public void updateCellForegroundValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        table.updateCellForegroundValues(propertyDraw, values);
    }

    public void updatePropertyCaptions(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        table.updatePropertyCaptions(propertyDraw, values);
    }

    public void updateShowIfValues(GPropertyDraw property, Map<GGroupObjectValue, Object> values) {
        table.updateShowIfValues(property, values);
    }

    public void updateReadOnlyValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        table.updateReadOnlyValues(propertyDraw, values);
    }

    public void updateRowBackgroundValues(Map<GGroupObjectValue, Object> values) {
        table.updateRowBackgroundValues(values);
    }

    public void updateRowForegroundValues(Map<GGroupObjectValue, Object> values) {
        table.updateRowForegroundValues(values);
    }

    public void modifyGridObject(GGroupObjectValue key, boolean add, int position) {
        table.modifyGroupObject(key, add, position);
    }

    public void updateColumnKeys(GPropertyDraw property, List<GGroupObjectValue> columnKeys) {
        table.updateColumnKeys(property, columnKeys);
    }

    public void changeOrder(GPropertyDraw property, GOrder modiType) {
        table.changeOrder(property, modiType);
    }

    public void selectProperty(GPropertyDraw propertyDraw) {
        table.selectProperty(propertyDraw);
    }

    public void clearGridOrders(GGroupObject groupObject) {
        table.clearGridOrders(groupObject);
    }
}