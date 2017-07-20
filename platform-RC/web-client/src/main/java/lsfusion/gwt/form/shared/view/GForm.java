package lsfusion.gwt.form.shared.view;

import lsfusion.gwt.base.shared.GwtSharedUtils;
import lsfusion.gwt.form.client.MainFrame;
import lsfusion.gwt.form.shared.view.changes.dto.GFormChangesDTO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class GForm implements Serializable {
    public String sessionID;

    public String sID;
    
    public String canonicalName;

    public String caption;
    public String creationPath;

    public int autoRefresh;

    public GContainer mainContainer;
    public ArrayList<GTreeGroup> treeGroups = new ArrayList<>();
    public ArrayList<GGroupObject> groupObjects = new ArrayList<>();
    public ArrayList<GPropertyDraw> propertyDraws = new ArrayList<>();
    public ArrayList<GRegularFilterGroup> regularFilterGroups = new ArrayList<>();
    public LinkedHashMap<GPropertyDraw, Boolean> defaultOrders = new LinkedHashMap<>();

    private transient HashMap<Integer, GPropertyDraw> idProps;
    private transient HashMap<Integer, GObject> idObjects;

    public GFormChangesDTO initialFormChanges;
    public GFormUserPreferences userPreferences;

    public ArrayList<GFont> usedFonts = new ArrayList<>();
    
    public GGroupObject getGroupObject(int id) {
        for (GGroupObject groupObject : groupObjects) {
            if (groupObject.ID == id) {
                return groupObject;
            }
        }
        return null;
    }

    public GGroupObject getGroupObject(String sID) {
        for (GGroupObject groupObject : groupObjects) {
            if (groupObject.getSID().equals(sID)) {
                return groupObject;
            }
        }
        return null;
    }

    public GObject getObject(int id) {
        GObject obj;
        if (idObjects == null) {
            idObjects = new HashMap<>();
            obj = null;
        } else {
            obj = idObjects.get(id);
        }

        if (obj == null) {
            OBJECTS:
            for (GGroupObject groupObject : groupObjects) {
                for (GObject object : groupObject.objects) {
                    if (object.ID == id) {
                        obj = object;
                        idObjects.put(id, object);
                        break OBJECTS;
                    }
                }
            }
        }
        return obj;
    }

    public GPropertyDraw getProperty(int id) {
        GPropertyDraw prop;
        if (idProps == null) {
            idProps = new HashMap<>();
            prop = null;
        } else {
            prop = idProps.get(id);
        }

        if (prop == null) {
            for (GPropertyDraw property : propertyDraws) {
                if (property.ID == id) {
                    prop = property;
                    break;
                }
            }
        }
        return prop;
    }

    public GPropertyDraw getProperty(String sid) {
        for (GPropertyDraw property : propertyDraws) {
            if (property.sID.equals(sid)) {
                return property;
            }
        }
        return null;
    }

    public LinkedHashMap<GPropertyDraw, Boolean> getDefaultOrders(GGroupObject group) {
        LinkedHashMap<GPropertyDraw, Boolean> result = new LinkedHashMap<>();
        for (Map.Entry<GPropertyDraw, Boolean> entry : defaultOrders.entrySet()) {
            if (GwtSharedUtils.nullEquals(entry.getKey().groupObject, group)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public void addFont(GFont font) {
        if (!usedFonts.contains(font)) {
            usedFonts.add(font);
        }
    }

    public String getTooltip() {
        return MainFrame.configurationAccessAllowed ?
                GwtSharedUtils.stringFormat("<html><body bgcolor=#FFFFE1>" +
                        "<b>%s</b><br/><hr>" +
                        "<b>sID:</b> %s<br/>" +
                        "<b>Путь:</b> %s<br/>" +
                        "</body></html>", caption, canonicalName, creationPath) :
                GwtSharedUtils.stringFormat("<html><body bgcolor=#FFFFE1><b>%s</b></body></html>", caption);
    }
}