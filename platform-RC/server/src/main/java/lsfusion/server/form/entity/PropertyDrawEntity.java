package lsfusion.server.form.entity;

import lsfusion.base.OrderedMap;
import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.LongMutable;
import lsfusion.base.col.interfaces.mutable.MOrderExclSet;
import lsfusion.base.identity.IdentityObject;
import lsfusion.interop.ClassViewType;
import lsfusion.interop.PropertyEditType;
import lsfusion.interop.form.PropertyReadType;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.DataClass;
import lsfusion.server.classes.NumericClass;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.instance.InstanceFactory;
import lsfusion.server.form.instance.Instantiable;
import lsfusion.server.form.instance.PropertyDrawInstance;
import lsfusion.server.form.instance.PropertyType;
import lsfusion.server.form.view.DefaultFormView;
import lsfusion.server.form.view.PropertyDrawView;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.ActionPropertyMapImplement;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.ExplicitActionProperty;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lsfusion.interop.form.ServerResponse.*;

public class PropertyDrawEntity<P extends PropertyInterface> extends IdentityObject implements Instantiable<PropertyDrawInstance>, PropertyReaderEntity {

    private PropertyEditType editType = PropertyEditType.EDITABLE;

    public PropertyObjectEntity<P, ?> propertyObject;
    
    public GroupObjectEntity toDraw;

    private String mouseBinding;
    private Map<KeyStroke, String> keyBindings;
    private OrderedMap<String, LocalizedString> contextMenuBindings;
    private Map<String, ActionPropertyObjectEntity<?>> editActions;

    public boolean optimisticAsync;

    public Boolean askConfirm;
    public String askConfirmMessage;

    public Boolean shouldBeLast;
    public ClassViewType forceViewType;
    public String eventID;

    private String formPath;
    
    public LocalizedString initCaption = null; // чисто техническая особенность реализации
    
    // предполагается что propertyObject ссылается на все (хотя и не обязательно)
    public String columnsName;
    public Object columnGroupObjects = SetFact.mOrderExclSet();
    private boolean finalizedColumnGroupObjects;

    // предполагается что propertyCaption ссылается на все из propertyObject но без toDraw (хотя опять таки не обязательно)
    public CalcPropertyObjectEntity<?> propertyCaption;
    public CalcPropertyObjectEntity<?> propertyShowIf;
    public CalcPropertyObjectEntity<?> propertyReadOnly;
    public CalcPropertyObjectEntity<?> propertyFooter;
    public CalcPropertyObjectEntity<?> propertyBackground;
    public CalcPropertyObjectEntity<?> propertyForeground;

    public PropertyDrawEntity quickFilterProperty;

    public final PropertyReaderEntity captionReader = new PropertyReaderEntity() {
        @Override
        public byte getTypeID() {
            return PropertyReadType.CAPTION;
        }

        @Override
        public int getID() {
            return PropertyDrawEntity.this.getID();
        }

        @Override
        public PropertyType getPropertyType() {
            return null;
        }

        @Override
        public Object getProfiledObject() {
            return PropertyDrawEntity.this.propertyCaption;
        }

        @Override
        public String toString() {
            return ThreadLocalContext.localize("{logics.property.caption}") + "(" + PropertyDrawEntity.this.toString() + ")";
        }
    };
    
    
    public final PropertyReaderEntity footerReader = new PropertyReaderEntity() {
        @Override
        public byte getTypeID() {
            return PropertyReadType.FOOTER;
        }

        @Override
        public int getID() {
            return PropertyDrawEntity.this.getID();
        }

        @Override
        public PropertyType getPropertyType() {
            return null;
        }

        @Override
        public Object getProfiledObject() {
            return PropertyDrawEntity.this.propertyFooter;
        }

        @Override
        public String toString() {
            return ThreadLocalContext.localize("{logics.property.footer}") + "(" + PropertyDrawEntity.this.toString() + ")";
        }
    };


    public PropertyDrawEntity() {
    }

    public PropertyDrawEntity(int ID, PropertyObjectEntity<P, ?> propertyObject, GroupObjectEntity toDraw) {
        super(ID);
        setSID("propertyDraw" + ID);
        this.propertyObject = propertyObject;
        this.toDraw = toDraw;
    }

    public DataClass getRequestInputType(FormEntity form) {
        return getRequestInputType(CHANGE, form, optimisticAsync);
    }

    public DataClass getWYSRequestInputType(FormEntity form) {
        return getRequestInputType(CHANGE_WYS, form, false);
    }

    public DataClass getRequestInputType(String actionSID, FormEntity form, boolean optimistic) {
        Type type = null;
        if (propertyObject instanceof CalcPropertyObjectEntity) {
            ActionPropertyObjectEntity<?> changeAction = getEditAction(actionSID, form);

            if (changeAction != null) {
                type = changeAction.property.getSimpleRequestInputType(optimistic);
            }
        }

        assert type == null || type instanceof DataClass;

        return (DataClass) type;
    }

    public <A extends PropertyInterface> Pair<ObjectEntity, Boolean> getAddRemove(FormEntity form) {
        ActionPropertyObjectEntity<A> changeAction = (ActionPropertyObjectEntity<A>) getEditAction(CHANGE, form);
        if(changeAction!=null)
            return changeAction.getAddRemove(form);
        return null;
    }

    public boolean hasEditObjectAction() {
        return (editActions != null && editActions.containsKey(EDIT_OBJECT)) || propertyObject.property.isEditObjectActionDefined();
    }
    
    public ActionPropertyObjectEntity getChangeAction(FormEntity entity) {
        return getEditAction(CHANGE, entity);
    }

    public ActionPropertyObjectEntity<?> getEditAction(String actionId, FormEntity entity) {
        // ?? тут или нет
        if (isReadOnly() &&
                (CHANGE.equals(actionId)
                        || CHANGE_WYS.equals(actionId)
                        || EDIT_OBJECT.equals(actionId)
                        || GROUP_CHANGE.equals(actionId))) {
            return null;
        }

        if (editActions != null) {
            ActionPropertyObjectEntity editAction = editActions.get(actionId);
            if (editAction != null) {
                return editAction;
            }
        }

        Property<P> property = propertyObject.property;
        if (GROUP_CHANGE.equals(actionId)) {
            ActionPropertyObjectEntity<?> editAction = getEditAction(CHANGE, entity);
            if(editAction == null)
                return null;
            return editAction.getGroupChange();
        }

        if (isSelector() && !hasContextMenuBinding(actionId)) {
            return getSelectorAction(property, entity);
        }

        if (CHANGE_WYS.equals(actionId)) {
            //если CHANGE_WYS не переопределён на уровне Property и CHANGE request'ает DataClass, то возвращаем CHANGE
            if (!propertyObject.property.isChangeWYSOverriden() && getRequestInputType(entity) != null) {
                return getEditAction(CHANGE, entity);
            }
        }

        ActionPropertyMapImplement<?, P> editActionImplement = propertyObject.property.getEditAction(actionId);
        return editActionImplement == null ? null : editActionImplement.mapObjects(propertyObject.mapping);
    }

    private ActionPropertyObjectEntity<?> getSelectorAction(Property<P> property, FormEntity entity) {
        ImMap<P, ObjectEntity> groupObjects = propertyObject.mapping.filterValues(getToDraw(entity).getObjects()); // берем нижний объект в toDraw
        for (ObjectEntity objectInstance : groupObjects.valueIt()) {
            if (objectInstance.baseClass instanceof CustomClass) {
                ExplicitActionProperty dialogAction = objectInstance.getChangeAction(property);
                return new ActionPropertyObjectEntity<>(
                        dialogAction,
                        MapFact.singleton(dialogAction.interfaces.single(), (PropertyObjectInterfaceEntity) objectInstance)
                );
            }
        }
        return null;
    }

    public void setPropertyObject(PropertyObjectEntity<P, ?> propertyObject) {
        this.propertyObject = propertyObject;
    }

    public PropertyDrawInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public void setToDraw(GroupObjectEntity toDraw) {
        this.toDraw = toDraw;
    }

    public void setMouseAction(String actionSID) {
        mouseBinding = actionSID;
    }

    public void setKeyAction(KeyStroke ks, String actionSID) {
        if (keyBindings == null) {
            keyBindings = new HashMap<>();
        }
        keyBindings.put(ks, actionSID);
    }

    public void setContextMenuAction(String actionSID, LocalizedString caption) {
        if (contextMenuBindings == null) {
            contextMenuBindings = new OrderedMap<>();
        }
        contextMenuBindings.put(actionSID, caption);
    }

    public void setEditAction(String actionSID, ActionPropertyObjectEntity<?> editAction) {
        if(editActions==null) {
            editActions = new HashMap<>();
        }
        editActions.put(actionSID, editAction);
    }


    public OrderedMap<String, LocalizedString> getContextMenuBindings() {
        ImOrderMap<String, LocalizedString> propertyContextMenuBindings = propertyObject.property.getContextMenuBindings();
        if (propertyContextMenuBindings.isEmpty()) {
            return contextMenuBindings;
        }

        OrderedMap<String, LocalizedString> result = new OrderedMap<>();
        for (int i = 0; i < propertyContextMenuBindings.size(); ++i) {
            result.put(propertyContextMenuBindings.getKey(i), propertyContextMenuBindings.getValue(i));
        }

        if (contextMenuBindings == null) {
            return result;
        }

        result.putAll(contextMenuBindings);
        return result;
    }

    public boolean hasContextMenuBinding(String actionSid) {
        OrderedMap contextMenuBindings = getContextMenuBindings();
        return contextMenuBindings != null && contextMenuBindings.containsKey(actionSid);
    }

    public Map<KeyStroke, String> getKeyBindings() {
        ImMap<KeyStroke, String> propertyKeyBindings = propertyObject.property.getKeyBindings();
        if (propertyKeyBindings.isEmpty()) {
            return keyBindings;
        }

        if (keyBindings == null) {
            return propertyKeyBindings.toJavaMap();
        }
        Map<KeyStroke, String> result = propertyKeyBindings.toJavaMap();
        result.putAll(keyBindings);
        return result;
    }

    public String getMouseBinding() {
        return mouseBinding != null ? mouseBinding : propertyObject.property.getMouseBinding();
    }

    @LongMutable
    public ImOrderSet<GroupObjectEntity> getColumnGroupObjects() {
        if(!finalizedColumnGroupObjects) {
            finalizedColumnGroupObjects = true;
            columnGroupObjects = ((MOrderExclSet<GroupObjectEntity>)columnGroupObjects).immutableOrder();
        }

        return (ImOrderSet<GroupObjectEntity>)columnGroupObjects;
    }
    public void setColumnGroupObjects(String columnsName, ImOrderSet<GroupObjectEntity> columnGroupObjects) {
        assert !finalizedColumnGroupObjects;
        this.columnsName = columnsName;
        finalizedColumnGroupObjects = true;
        this.columnGroupObjects = columnGroupObjects;
    }

    public void addColumnGroupObject(GroupObjectEntity columnGroupObject) {
        assert !finalizedColumnGroupObjects;
        ((MOrderExclSet<GroupObjectEntity>)columnGroupObjects).exclAdd(columnGroupObject);
    }

    public void setPropertyCaption(CalcPropertyObjectEntity propertyCaption) {
        this.propertyCaption = propertyCaption;
    }

    public void setPropertyCaptionAndShowIf(CalcPropertyObjectEntity propertyCaptionAsShowIf) {
        this.propertyCaption = propertyCaptionAsShowIf;
        this.propertyShowIf = propertyCaptionAsShowIf;
    }

    public void setPropertyFooter(CalcPropertyObjectEntity propertyFooter) {
        this.propertyFooter = propertyFooter;
    }

    public void setPropertyBackground(CalcPropertyObjectEntity propertyBackground) {
        this.propertyBackground = propertyBackground;
    }

    public void setPropertyForeground(CalcPropertyObjectEntity propertyForeground) {
        this.propertyForeground = propertyForeground;
    }

    public PropertyEditType getEditType() {
        return editType;
    }

    public void setEditType(PropertyEditType editType) {
        this.editType = editType;
    }

    public boolean isSelector() {
        return editType == PropertyEditType.SELECTOR;
    }

    public boolean isReadOnly() {
        return editType == PropertyEditType.READONLY;
    }

    public boolean isEditable() {
        return editType == PropertyEditType.EDITABLE;
    }

    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView defaultView) {
        propertyObject.property.drawOptions.proceedDefaultDesign(propertyView);
    }

    @Override
    public String toString() {
        return (formPath == null ? "" : formPath) + " property:" + propertyObject.toString();
    }

    public GroupObjectEntity getToDraw(FormEntity form) {
        return toDraw==null?form.getApplyObject(propertyObject.getObjectInstances()):toDraw;        
    }

    public GroupObjectEntity getNFToDraw(FormEntity form, Version version) {
        return toDraw==null?form.getNFApplyObject(propertyObject.getObjectInstances(), version):toDraw;
    }

    public boolean isToolbar(FormEntity formEntity) {
        if (forceViewType != null)
            return forceViewType.isToolbar();

        GroupObjectEntity toDraw = getToDraw(formEntity);
        return toDraw != null && toDraw.initClassView.isToolbar();

    }

    public boolean isForcedPanel() {
        return forceViewType != null && forceViewType.isPanel();
    }

    static public String createSID(String name, List<String> mapping) {
        StringBuilder sidBuilder = new StringBuilder();
        sidBuilder.append(name);
        sidBuilder.append("(");
        for (int i = 0; i < mapping.size(); i++) {
            if (i > 0) {
                sidBuilder.append(",");
            }
            sidBuilder.append(mapping.get(i));
        }
        sidBuilder.append(")");
        return sidBuilder.toString();        
    }

    public static <P extends PropertyInterface> String createSID(PropertyObjectEntity<?, ?> property, ImOrderSet<P> interfaces) {
        assert property.property.isNamed();
        List<String> mapping = new ArrayList<>();
        for (P pi : interfaces) {
            PropertyObjectInterfaceEntity obj = property.mapping.getObject(pi);
            assert obj instanceof ObjectEntity;
            mapping.add(((ObjectEntity) obj).getSID());
        }
        return createSID(property.property.getName(), mapping);
    }

    public String getFormPath() {
        return formPath;
    }

    public void setFormPath(String formPath) {
        this.formPath = formPath;
    }

    public String getNamespace() {
        return propertyObject == null || propertyObject.property == null ? "" : propertyObject.property.getNamespace();
    }

    public CalcPropertyObjectEntity getDrawInstance() {
        return propertyObject.getDrawProperty();
    }

    @Override
    public Object getProfiledObject() {
        return this;
    }

    @Override
    public byte getTypeID() {
        return PropertyReadType.DRAW;
    }

    public Type getType() {
        return propertyObject.property.getType();
    }

    @Override
    public PropertyType getPropertyType() {
        Type type = getType();
        return new PropertyType(type.getSID(), getToDrawSID(), type.getCharLength().value, type instanceof NumericClass ? ((NumericClass) type).getPrecision() : 0);
    }

    private String getToDrawSID() {
        String result = null;
        if (toDraw != null) {
            result = toDraw.getSID();
        } else {
            for (ObjectEntity object : propertyObject.getMapObjectInstances().values()) {
                if (object != null && object.groupTo != null && object.groupTo.initClassView.isGrid())
                    result = object.getSID();
            }
        }
        return result == null ? "" : result;
    }
}