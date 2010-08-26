package platform.client.form.grid;

import platform.client.form.ClientFormController;
import platform.client.form.ClientFormLayout;
import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.form.queries.FilterController;
import platform.client.form.queries.FindController;
import platform.client.logics.*;
import platform.interop.Order;
import platform.interop.form.screen.ExternalScreenComponent;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GridController {

    private final ClientGrid key;
    public ClientGrid getKey() {
        return key;
    }

    private final GridView view;
    public JComponent getView() {
        return view;
    }

    private final GridTable table;

    private final ClientFormController form;

    private final GroupObjectLogicsSupplier logicsSupplier;

    public GridController(ClientGrid key, GroupObjectLogicsSupplier ilogicsSupplier, ClientFormController iform) {

        this.key = key;
        logicsSupplier = ilogicsSupplier;
        form = iform;

        FindController findController = new FindController(logicsSupplier) {

            protected boolean queryChanged() {

                form.changeFind(getConditions());

                table.requestFocusInWindow();
                return true;
            }
        };

        FilterController filterController = new FilterController(logicsSupplier) {

            protected boolean queryChanged() {

                try {
                    form.changeFilter(logicsSupplier.getGroupObject(), getConditions());
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка при применении фильтра", e);
                }

                table.requestFocusInWindow();
                return true;
            }
        };

        view = new GridView(logicsSupplier, form, GridController.this.key.showFind ? findController.getView() : null, GridController.this.key.showFilter ? filterController.getView() : null, GridController.this.key.tabVertical) {

            protected void needToBeShown() {
                hidden = false;
                showViews();
            }

            protected void needToBeHidden() {
                hidden = true;
                hideViews();
            }
        };
        table = view.getTable();

        if (this.key.minRowCount > 0) { // вообще говоря, так делать неправильно, посколько и HeaderHeight и RowHeight могут изменяться во времени
            Dimension minSize = table.getMinimumSize();
            minSize.height = Math.max(minSize.height, (int) table.getTableHeader().getPreferredSize().getHeight() + this.key.minRowCount * table.getRowHeight());
            view.setMinimumSize(minSize);
        }
    }

    public void addView(ClientFormLayout formLayout) {
        formLayout.add(key, view);
        for (Map.Entry<ClientCell, ExternalScreenComponent> entry : extViews.entrySet()) {
            entry.getKey().externalScreen.add(form.getID(), entry.getValue(), entry.getKey().externalScreenConstraints);
        }
    }

    public void addGroupObjectCells() {
//                System.out.println("addGroupObjectCells");
        for (ClientObject object : logicsSupplier.getGroupObject()) {
            if (object.objectIDCell.show)
               table.addColumn(object.objectIDCell);
            if (object.classCell.show)
                table.addColumn(object.classCell);
        }

        // здесь еще добавить значения идентификаторов
        fillTableObjectID();

        table.updateTable();
    }

    public void removeGroupObjectCells() {
//                System.out.println("removeGroupObjectCells");
        for (ClientObject object : logicsSupplier.getGroupObject()) {
            if(object.objectIDCell.show)
                table.removeColumn(object.objectIDCell);
            table.removeColumn(object.classCell);
        }
        table.updateTable();
    }

    private Map<ClientCell, ExternalScreenComponent> extViews = new HashMap<ClientCell, ExternalScreenComponent>();

    private void addExternalScreenComponent(ClientCell key) {
        if (!extViews.containsKey(key)) {
            ExternalScreenComponent extView = new ExternalScreenComponent();
            extViews.put(key, extView);
        }
    }

    public void addProperty(ClientPropertyDraw property) {
//                System.out.println("addProperty " + property.toString());
        if (property.show) {
            if (table.addColumn(property)) {
                table.updateTable();
            }
        }

        if (property.externalScreen != null) {
            addExternalScreenComponent(property);
        }
    }

    public void removeProperty(ClientPropertyDraw property) {
//                System.out.println("removeProperty " + property.toString());
        if (property.show) {
            if (table.removeColumn(property)) {
                table.updateTable();
            }
        }
    }

    public void setGridObjects(List<ClientGroupObjectValue> gridObjects) {
        table.setGridObjects(gridObjects);

        //здесь еще добавить значения идентификаторов
        fillTableObjectID();
    }

    public void setGridClasses(List<ClientGroupObjectClass> gridClasses) {
        fillTableObjectClasses(gridClasses);
    }

    public void selectObject(ClientGroupObjectValue currentObject) {
        table.selectObject(currentObject);
    }

    public void setPropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values) {
        table.setColumnValues(property, values);
        if (extViews.containsKey(property)) {
            Object value = getSelectedValue(property);
            extViews.get(property).setValue((value == null) ? "" : value.toString());
            property.externalScreen.invalidate();
        }
    }

    public void changeGridOrder(ClientCell property, Order modiType) throws IOException {
        table.changeGridOrder(property, modiType);
    }

    public ClientCell getCurrentCell() {
        return table.getCurrentCell();
    }

    public Object getSelectedValue(ClientPropertyDraw cell) {
        return table.getSelectedValue(cell);
    }

    public boolean requestFocusInWindow() {
        return table.requestFocusInWindow();
    }

    boolean hidden = false;
    public void hideViews() {
        view.setVisible(false);
    }

    public void showViews() {
        if (!hidden)
            view.setVisible(true);
    }

    private void fillTableObjectID() {

        for (ClientObject object : logicsSupplier.getGroupObject())
            if(object.objectIDCell.show) {
                Map<ClientGroupObjectValue, Object> values = new HashMap<ClientGroupObjectValue, Object>();
                for (ClientGroupObjectValue value : table.getGridRows())
                    values.put(value, value.get(object));
                table.setColumnValues(object.objectIDCell, values);
            }
    }

    private void fillTableObjectClasses(List<ClientGroupObjectClass> classes) {

        for (ClientObject object : logicsSupplier.getGroupObject()) {

            Map<ClientGroupObjectValue, Object> cls = new HashMap<ClientGroupObjectValue, Object>();

            List<ClientGroupObjectValue> gridRows = table.getGridRows();
            for (int i = 0; i < gridRows.size(); i++) {
                cls.put(gridRows.get(i), classes.get(i).get(object));
            }

            table.setColumnValues(object.classCell, cls);
        }
    }
}
