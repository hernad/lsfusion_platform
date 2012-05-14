package platform.server.logics.property;

import platform.base.*;
import platform.interop.ClassViewType;
import platform.interop.PropertyEditType;
import platform.interop.form.ServerResponse;
import platform.server.Message;
import platform.server.Settings;
import platform.server.ThisMessage;
import platform.server.caches.IdentityLazy;
import platform.server.caches.ManualLazy;
import platform.server.caches.PackComplex;
import platform.server.classes.*;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.*;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.PullExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.expr.where.cases.CaseExpr;
import platform.server.data.expr.where.extra.CompareWhere;
import platform.server.data.query.IQuery;
import platform.server.data.query.Join;
import platform.server.data.query.MapKeysInterface;
import platform.server.data.query.Query;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.classes.AbstractClassWhere;
import platform.server.data.where.classes.ClassWhere;
import platform.server.form.entity.*;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.form.view.panellocation.PanelLocationView;
import platform.server.form.view.panellocation.ShortcutPanelLocationView;
import platform.server.logics.DataObject;
import platform.server.logics.linear.LP;
import platform.server.logics.panellocation.PanelLocation;
import platform.server.logics.panellocation.ShortcutPanelLocation;
import platform.server.logics.property.actions.edit.DefaultChangeActionProperty;
import platform.server.logics.property.change.PropertyChangeListener;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.property.group.AbstractNode;
import platform.server.logics.table.MapKeysTable;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;
import platform.server.session.*;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public abstract class Property<T extends PropertyInterface> extends AbstractNode implements MapKeysInterface<T>, ServerIdentitySerializable {
    private String sID;

    // вот отсюда идут свойства, которые отвечают за логику представлений и подставляются автоматически для PropertyDrawEntity и PropertyDrawView

    public String caption;
    public String toolTip;

    public int minimumCharWidth;
    public int maximumCharWidth;
    public int preferredCharWidth;

    public boolean loggable;
    private LP logProperty;
    public LP logFormProperty;

    public void setFixedCharWidth(int charWidth) {
        minimumCharWidth = charWidth;
        maximumCharWidth = charWidth;
        preferredCharWidth = charWidth;
    }

    public void inheritFixedCharWidth(Property property) {
        minimumCharWidth = property.minimumCharWidth;
        maximumCharWidth = property.maximumCharWidth;
        preferredCharWidth = property.preferredCharWidth;
    }

    private ImageIcon image;
    private String iconPath;

    public void inheritImage(Property property) {
        image = property.image;
        iconPath = property.iconPath;
    }

    public void setImage(String iconPath) {
        this.iconPath = iconPath;
        this.image = new ImageIcon(Property.class.getResource("/images/" + iconPath));
    }

    public KeyStroke editKey;
    public Boolean showEditKey;

    public String regexp;
    public String regexpMessage;
    public Boolean echoSymbols;

    public PanelLocation panelLocation;

    public Boolean shouldBeLast;

    public ClassViewType forceViewType;

    public Boolean askConfirm;

    public boolean autoset;

    public String toString() {
        return caption;
    }

    public int ID = 0;

    public String getCode() {
        return getSID();
    }

    public boolean isField() {
        return false;
    }

    public int getID() {
        return ID;
    }

    public void setID(int iID) {
        ID = iID;
    }

    public LP getLogProperty() {
        return logProperty;
    }

    public void setLogProperty(LP logProperty) {
        this.logProperty = logProperty;
    }

    public LP getLogFormProperty() {
        return logFormProperty;
    }

    public void setLogFormProperty(LP logFormProperty) {
        this.logFormProperty = logFormProperty;
    }

    public final Collection<T> interfaces;

    public boolean check() {
        return !getClassWhere().isFalse();
    }

    public <P extends PropertyInterface> boolean intersect(Property<P> property, Map<P, T> map) {
        return !getClassWhere().and(new ClassWhere<T>(property.getClassWhere(), map)).isFalse();
    }

    public boolean isInInterface(Map<T, ? extends AndClassSet> interfaceClasses, boolean isAny) {
        return isAny ? anyInInterface(interfaceClasses) : allInInterface(interfaceClasses);
    }

    @IdentityLazy
    public boolean allInInterface(Map<T, ? extends AndClassSet> interfaceClasses) {
        return new ClassWhere<T>(interfaceClasses).meansCompatible(getClassWhere());
    }

    @IdentityLazy
    public boolean anyInInterface(Map<T, ? extends AndClassSet> interfaceClasses) {
        return !getClassWhere().andCompatible(new ClassWhere<T>(interfaceClasses)).isFalse();
    }

    public boolean isFull(Collection<T> checkInterfaces) {
        ClassWhere<T> classWhere = getClassWhere();
        if(classWhere.isFalse())
            return false;
        for (AbstractClassWhere.And<T> where : classWhere.wheres) {
            for (T i : checkInterfaces)
                if(where.get(i)==null)
                    return false;
        }
        return true;
    }
    
    private boolean calculateIsFull() {
        return isFull(interfaces);
    }
    private Boolean isFull;
    private static ThreadLocal<Boolean> isFullRunning = new ThreadLocal<Boolean>();
    @ManualLazy
    public boolean isFull() {
        if(isFull==null) {
            if(isFullRunning.get()!=null)
                return false;
            isFullRunning.set(true);

            isFull = calculateIsFull();

            isFullRunning.set(null);
        }
        return isFull;
    }

    public Property(String sID, String caption, List<T> interfaces) {
        this.setSID(sID);
        this.caption = caption;
        this.interfaces = interfaces;
    }

    @IdentityLazy
    public Map<T, KeyExpr> getMapKeys() {
        return KeyExpr.getMapKeys(interfaces);
    }

    public static Modifier defaultModifier = new Modifier() {
        public PropertyChanges getPropertyChanges() {
            return PropertyChanges.EMPTY;
        }
    };

    public Expr getExpr(Map<T, ? extends Expr> joinImplement) {
        return getExpr(joinImplement, PropertyChanges.EMPTY);
    }

    public Expr getClassExpr(Map<T, ? extends Expr> joinImplement) {
        return getExpr(joinImplement, true, PropertyChanges.EMPTY, null);
    }

    public Expr getExpr(Map<T, ? extends Expr> joinImplement, Modifier modifier) {
        return getExpr(joinImplement, modifier.getPropertyChanges());
    }
    public Expr getExpr(Map<T, ? extends Expr> joinImplement, PropertyChanges propChanges) {
        return getExpr(joinImplement, propChanges, null);
    }

    public Expr aspectGetExpr(Map<T, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        assert joinImplement.size() == interfaces.size();

        return calculateExpr(joinImplement, propClasses, propChanges, changedWhere);
    }

    public IQuery<T, String> getQuery(PropertyChanges propChanges, PropertyQueryType queryType, Map<T, ? extends Expr> interfaceValues) {
        return getQuery(false, propChanges, queryType, interfaceValues);
    }

    @PackComplex
    @Message("message.core.property.get.expr")
    @ThisMessage
    public IQuery<T, String> getQuery(boolean propClasses, PropertyChanges propChanges, PropertyQueryType queryType, Map<T, ? extends Expr> interfaceValues) {
        if(queryType==PropertyQueryType.FULLCHANGED) {
            IQuery<T, String> query = getQuery(propClasses, propChanges, PropertyQueryType.RECURSIVE, interfaceValues);
            Query<T, String> fullQuery = new Query<T, String>(query.getMapKeys());
            Expr newExpr = query.getExpr("value");
            fullQuery.properties.put("value", newExpr);
            fullQuery.properties.put("changed", query.getExpr("changed").and(newExpr.getWhere().or(getExpr(fullQuery.mapKeys).getWhere())));
            return fullQuery;
        }
            
        Query<T, String> query = new Query<T,String>(BaseUtils.filterNotKeys(getMapKeys(), interfaceValues.keySet()));
        Map<T, Expr> allKeys = BaseUtils.merge(interfaceValues, query.mapKeys);
        WhereBuilder queryWheres = queryType.needChange() ? new WhereBuilder():null;
        query.properties.put("value", aspectGetExpr(allKeys, propClasses, propChanges, queryWheres));
        if(queryType.needChange())
            query.properties.put("changed", ValueExpr.get(queryWheres.toWhere()));
        return query;
    }

    public Expr getQueryExpr(Map<T, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWheres) {

        Map<T, Expr> interfaceValues = new HashMap<T, Expr>(); Map<T, Expr> interfaceExprs = new HashMap<T, Expr>();
        for(Map.Entry<T, ? extends Expr> entry : joinImplement.entrySet())
            if(entry.getValue().isValue())
                interfaceValues.put(entry.getKey(), entry.getValue());
            else
                interfaceExprs.put(entry.getKey(), entry.getValue());

        IQuery<T, String> query = getQuery(propClasses, propChanges, changedWheres!=null?PropertyQueryType.CHANGED:PropertyQueryType.NOCHANGE, interfaceValues);

        Join<String> queryJoin = query.join(interfaceExprs);
        if(changedWheres!=null)
            changedWheres.add(queryJoin.getExpr("changed").getWhere());
        return queryJoin.getExpr("value");
    }

    @Message("message.core.property.get.expr")
    @PackComplex
    @ThisMessage
    public Expr getJoinExpr(Map<T, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return aspectGetExpr(joinImplement, propClasses, propChanges, changedWhere);
    }

    public Expr getExpr(Map<T, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return getExpr(joinImplement, false, propChanges, changedWhere);
    }

    // в будущем propClasses можно заменить на PropertyTables propTables
    public Expr getExpr(Map<T, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if (isFull() && (Settings.instance.isUseQueryExpr() || Query.getMapKeys(joinImplement)!=null))
            return getQueryExpr(joinImplement, propClasses, propChanges, changedWhere);
        else
            return getJoinExpr(joinImplement, propClasses, propChanges, changedWhere);
    }

    public Expr calculateExpr(Map<T, ? extends Expr> joinImplement) {
        return calculateExpr(joinImplement, false, PropertyChanges.EMPTY, null);
    }

    public Expr calculateClassExpr(Map<T, ? extends Expr> joinImplement) { // вызывается до stored, поэтому чтобы не было проблем с кэшами, сделано так
        return calculateExpr(joinImplement, true, PropertyChanges.EMPTY, null);
    }

    protected abstract Expr calculateExpr(Map<T, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere);

    @IdentityLazy
    public ClassWhere<T> getClassWhere() {
        Map<T, KeyExpr> mapKeys = getMapKeys();
        return new Query<T, String>(mapKeys, getClassExpr(mapKeys), "value").getClassWhere(new ArrayList<String>());
    }

    @IdentityLazy
    public Type getInterfaceType(T propertyInterface) {
        Map<T, KeyExpr> mapKeys = getMapKeys();
        return mapKeys.get(propertyInterface).getType(getClassExpr(mapKeys).getWhere());
    }

    // возвращает от чего "зависят" изменения - с callback'ов
    protected abstract QuickSet<CalcProperty> calculateUsedChanges(StructChanges propChanges, boolean cascade);

    public QuickSet<CalcProperty> getUsedChanges(StructChanges propChanges) {
        return getUsedChanges(propChanges, false);
    }
    // 2-й параметр - "сверху" есть каскадная сессия, поэтому eventChange'ы надо проверять полностью, а не только на where
    public QuickSet<CalcProperty> getUsedChanges(StructChanges propChanges, boolean cascade) {
        if(this instanceof ActionProperty || propChanges.isEmpty()) // чтобы рекурсию разбить
            return QuickSet.EMPTY();

        QuickSet<CalcProperty> usedChanges;
        QuickSet<CalcProperty> modifyChanges = propChanges.getUsedChanges((CalcProperty) this, cascade);
        if(propChanges.hasChanges(modifyChanges) || (propChanges.hasChanges(usedChanges  = calculateUsedChanges(propChanges, cascade)) && !modifyChanges.isEmpty()))
            return modifyChanges;
        return usedChanges;
    }

    public PropertyChanges getUsedChanges(PropertyChanges propChanges) {
        return propChanges.filter(getUsedChanges(propChanges.getStruct()));
    }

    public String getSID() {
        return sID;
    }

    private boolean canChangeSID = true;

    public void setSID(String sID) {
        if (canChangeSID) {
            this.sID = sID;
        } else {
            throw new RuntimeException(String.format("Can't change property SID [%s] after freezing", sID));
        }
    }

    public void freezeSID() {     // todo [dale]: Отрефакторить установку SID
        canChangeSID = false;
    }

    public static class CommonClasses<T extends PropertyInterface> {
        public Map<T, ValueClass> interfaces;
        public ValueClass value;

        public CommonClasses(Map<T, ValueClass> interfaces, ValueClass value) {
            this.interfaces = interfaces;
            this.value = value;
        }
    }

    public Type getType() {
        return getCommonClasses().value.getType();
    }

    public Map<T, ValueClass> getMapClasses() {
        return getCommonClasses().interfaces;
    }

    @IdentityLazy
    public CommonClasses<T> getCommonClasses() {
        Map<Object, ValueClass> mapClasses = getClassValueWhere().getCommonParent(BaseUtils.<Object, T, String>merge(interfaces, Collections.singleton("value")));
        return new CommonClasses<T>(BaseUtils.filterKeys(mapClasses, interfaces), mapClasses.get("value"));
    }

    public ClassWhere<Field> getClassWhere(MapKeysTable<T> mapTable, PropertyField storedField) {
        return getClassValueWhere().remap(BaseUtils.<Object, T, String, Field>merge(mapTable.mapKeys, Collections.singletonMap("value", storedField)));

    }
    public abstract ClassWhere<Object> getClassValueWhere();

    public boolean cached = false;

    public ActionPropertyMapImplement<T> getEditAction(String editActionSID) {
        return getEditAction(editActionSID, null);
    }

    private Map<String, ActionPropertyMapImplement<T>> editActions = new HashMap<String, ActionPropertyMapImplement<T>>();
    public ActionPropertyMapImplement<T> getEditAction(String editActionSID, CalcProperty filterProperty) {
        ActionPropertyMapImplement<T> editAction = editActions.get(editActionSID);
        if(editAction!=null)
            return editAction;

        if(editActionSID.equals(ServerResponse.CHANGE_WYS)) {
            ActionPropertyMapImplement<T> customChangeEdit = editActions.get(ServerResponse.CHANGE);// если перегружен
            if(customChangeEdit!=null) // возвращаем customChangeEdit
                return customChangeEdit;
        }

        if(editActionSID.equals(ServerResponse.GROUP_CHANGE)) {
            ActionPropertyMapImplement<T> customChangeEdit = editActions.get(ServerResponse.CHANGE);// если перегружен
            if(customChangeEdit!=null) { // если перегружен, иначе пусть работает комбинаторная логика (в принципе можно потом по аналогии с PASTE делать)
                return null;
            }
        }

        if(editActionSID.equals(ServerResponse.PASTE)) {
            ActionPropertyMapImplement<T> customChangeWYSEdit = getEditAction(ServerResponse.CHANGE_WYS);// если перегружен
            return null;
        }

        return getDefaultEditAction(editActionSID, filterProperty);
    }

    public abstract ActionPropertyMapImplement<T> getDefaultEditAction(String editActionSID, CalcProperty filterProperty);

    public boolean checkEquals() {
        return this instanceof CalcProperty;
    }

    public Map<T, T> getIdentityInterfaces() {
        return BaseUtils.toMap(new HashSet<T>(interfaces));
    }

    // используется если создаваемый WhereBuilder нужен только если задан changed
    public static WhereBuilder cascadeWhere(WhereBuilder changed) {
        return changed == null ? null : new WhereBuilder();
    }

    // по умолчанию заполняет свойства
    // assert что entity этого свойства
    public void proceedDefaultDraw(PropertyDrawEntity<T> entity, FormEntity<?> form) {
        if (loggable && logFormProperty != null) {
            form.addPropertyDraw(logFormProperty, BaseUtils.orderMap(entity.propertyObject.mapping, interfaces).values().toArray(new PropertyObjectInterfaceEntity[0]));
            form.setForceViewType(logFormProperty, ClassViewType.PANEL);
        }

        if (shouldBeLast != null)
            entity.shouldBeLast = shouldBeLast;

        if (forceViewType != null)
            entity.forceViewType = forceViewType;

        //перемещаем свойство в контекстном меню в тот же groupObject, что и свойство, к которому оно привязано
        if (panelLocation != null && panelLocation.isShortcutLocation() && ((ShortcutPanelLocation) panelLocation).getOnlyProperty() != null) {
            Property onlyProperty = ((ShortcutPanelLocation) panelLocation).getOnlyProperty();
            for (PropertyDrawEntity drawEntity : form.getProperties(onlyProperty)) {
                if (drawEntity.toDraw != null) {
                    entity.toDraw = drawEntity.toDraw;
                }

                //добавляем в контекстное меню...
                drawEntity.setContextMenuEditAction(caption, getSID(), (ActionPropertyObjectEntity) entity.propertyObject);
            }
        }
    }

    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        if (iconPath != null) {
            propertyView.design.iconPath = iconPath;
            propertyView.design.setImage(image);
        }

        if (editKey != null)
            propertyView.editKey = editKey;
        if (showEditKey != null)
            propertyView.showEditKey = showEditKey;
        if (regexp != null)
            propertyView.regexp = regexp;
        if (regexpMessage != null)
            propertyView.regexpMessage = regexpMessage;
        if (echoSymbols != null)
            propertyView.echoSymbols = echoSymbols;
        if (askConfirm != null)
            propertyView.askConfirm = askConfirm;

        if (panelLocation != null) {
            PanelLocationView panelLocationView = panelLocation.convertToView();
            if (panelLocationView.isShortcutLocation()) {
                Property onlyProperty = ((ShortcutPanelLocation) panelLocation).getOnlyProperty();
                if (onlyProperty != null) {
                    for (PropertyDrawView prop : view.properties) {
                        if (prop.entity.propertyObject.property.equals(onlyProperty) &&
                        (view.getGroupObject(propertyView.entity.toDraw) == null || view.getGroupObject(propertyView.entity.toDraw).equals(view.getGroupObject(prop.entity.toDraw)))) {
                            ((ShortcutPanelLocationView) panelLocationView).setOnlyProperty(prop);
                            break;
                        }
                    }
                    if (((ShortcutPanelLocationView) panelLocationView).getOnlyProperty() == null)
                        panelLocationView = null;
                }
            }
            if (panelLocationView != null) {
                propertyView.entity.forceViewType = ClassViewType.PANEL;
                propertyView.setPanelLocation(panelLocationView);
            }
        }
        
        if(propertyView.getType() instanceof LogicalClass)
            propertyView.editOnSingleClick = Settings.instance.getEditLogicalOnSingleClick();
        if(propertyView.getType() instanceof ActionClass)
            propertyView.editOnSingleClick = Settings.instance.getEditActionClassOnSingleClick();

        if (loggable && logFormProperty != null) {
            PropertyDrawView logPropertyView = view.get(view.entity.getPropertyDraw(logFormProperty));
            GroupObjectEntity groupObject = propertyView.entity.getToDraw(view.entity);
            if (groupObject != null) {
                logPropertyView = BaseUtils.nvl(view.get(view.entity.getPropertyDraw(logFormProperty.property, groupObject)), logPropertyView);
            }
            if (logPropertyView != null) {
                logPropertyView.entity.setEditType(PropertyEditType.EDITABLE); //бывает, что проставляют READONLY для всего groupObject'а
                logPropertyView.setPanelLocation(new ShortcutPanelLocationView(propertyView));
            }
        }
    }

    public boolean hasChild(Property prop) {
        return prop.equals(this);
    }

    public List<Property> getProperties() {
        return Collections.singletonList((Property) this);
    }

    @Override
    public List<PropertyClassImplement> getProperties(Collection<List<ValueClassWrapper>> classLists, boolean anyInInterface) {
        List<PropertyClassImplement> resultList = new ArrayList<PropertyClassImplement>();
        if (isFull()) {
            for (List<ValueClassWrapper> classes : classLists) {
                if (interfaces.size() == classes.size()) {
                    for (List<T> mapping : new ListPermutations<T>(interfaces)) {
                        Map<T, AndClassSet> propertyInterface = new HashMap<T, AndClassSet>();
                        int interfaceCount = 0;
                        for (T iface : mapping) {
                            ValueClass propertyClass = classes.get(interfaceCount++).valueClass;
                            propertyInterface.put(iface, propertyClass.getUpSet());
                        }

                        if (isInInterface(propertyInterface, anyInInterface)) {
                            resultList.add(createClassImplement(classes, mapping));
                        }
                    }
                }
            }
        }
        return resultList;
    }
    
    protected abstract PropertyClassImplement<T, ?> createClassImplement(List<ValueClassWrapper> classes, List<T> mapping);

    @Override
    public Property getProperty(String sid) {
        return this.getSID().equals(sid) ? this : null;
    }

    public T getInterfaceById(int iID) {
        for (T inter : interfaces) {
            if (inter.getID() == iID) {
                return inter;
            }
        }

        return null;
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        outStream.writeUTF(getSID());
        outStream.writeUTF(caption);
        outStream.writeBoolean(toolTip != null);
        if (toolTip != null)
            outStream.writeUTF(toolTip);
        outStream.writeUTF(getCode());
        outStream.writeBoolean(isField());

        pool.serializeCollection(outStream, interfaces);
        pool.serializeObject(outStream, getParent());
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        //десериализация не нужна, т.к. вместо создания объекта, происходит поиск в BL
    }

    public <D extends PropertyInterface> void setEventAction(CalcPropertyMapImplement<?, T> whereImplement, int options) {
        assert this instanceof ActionProperty;
        setEvent(DerivedProperty.<T>createStatic(true, ActionClass.instance), whereImplement, options);
    }

    public <D extends PropertyInterface> void setEvent(boolean valueChanged, IncrementType incrementType, CalcPropertyImplement<D, CalcPropertyInterfaceImplement<T>> valueImplement, List<CalcPropertyMapImplement<?, T>> whereImplements, Collection<CalcPropertyMapImplement<?, T>> onChangeImplements) {
        // нужно onChange обернуть в getChange, and where, and change implement'ы
        if(!valueChanged)
            valueImplement = new CalcPropertyImplement<D, CalcPropertyInterfaceImplement<T>>(valueImplement.property.getOld(), valueImplement.mapping);

        List<CalcPropertyMapImplement<?, T>> onChangeWhereImplements = new ArrayList<CalcPropertyMapImplement<?, T>>();
        for(CalcPropertyMapImplement<?, T> onChangeImplement : onChangeImplements)
            onChangeWhereImplements.add(onChangeImplement.mapChanged(incrementType));
        for(CalcPropertyInterfaceImplement<T> mapping : valueImplement.mapping.values())
            if(mapping instanceof CalcPropertyMapImplement)
                onChangeWhereImplements.add(((CalcPropertyMapImplement<?, T>) mapping).mapChanged(IncrementType.CHANGE));

        CalcPropertyMapImplement<?, T> where;
        if(onChangeWhereImplements.size() > 0) {
            CalcPropertyMapImplement<?, T> onChangeWhere;
            if(onChangeWhereImplements.size()==1)
                where = BaseUtils.single(onChangeWhereImplements);
            else
                where = DerivedProperty.createUnion(interfaces, onChangeWhereImplements);
            if(whereImplements.size()>0)
                where = DerivedProperty.createAnd(interfaces, where, whereImplements);
        } else { // по сути новая ветка, assert что whereImplements > 0
            where = whereImplements.get(0);
            if(whereImplements.size() > 1)
                where = DerivedProperty.createAnd(interfaces, where, whereImplements.subList(1, whereImplements.size()));
        }
        setEvent(DerivedProperty.createJoin(valueImplement), where);
    }

    public <D extends PropertyInterface, W extends PropertyInterface> void setEvent(CalcPropertyInterfaceImplement<T> valueImplement, CalcPropertyMapImplement<W, T> whereImplement) {
        setEvent(valueImplement, whereImplement, 0);
    }

    private <D extends PropertyInterface, W extends PropertyInterface> void setEvent(CalcPropertyInterfaceImplement<T> valueImplement, CalcPropertyMapImplement<W, T> whereImplement, int options) {
        if(!((CalcProperty)whereImplement.property).noDB())
            whereImplement = whereImplement.mapChanged(IncrementType.SET);

        addEvent(valueImplement, whereImplement, options);
    }

    protected abstract <D extends PropertyInterface, W extends PropertyInterface> void addEvent(CalcPropertyInterfaceImplement<T> valueImplement, CalcPropertyMapImplement<W, T> whereImplement, int options);

    @Override
    public List<AbstractGroup> fillGroups(List<AbstractGroup> groupsList) {
        return groupsList;
    }

    protected boolean finalized = false;
    public void finalizeInit() {
        assert !finalized;
        finalized = true;
        if(this instanceof ActionProperty)
            for(CalcProperty<?> property : ((ActionProperty)this).getChangeProps()) // вообще говоря DataProperty и IsClassProperty
                property.actionChangeProps.add((ActionProperty) this);
    }

    @IdentityLazy
    public PropertyChange<T> getNoChange() {
        return new PropertyChange<T>(getMapKeys(), CaseExpr.NULL);
    }
    
    public void prereadCaches() {
        getClassWhere();
        if(isFull())
            getQuery(false, PropertyChanges.EMPTY, PropertyQueryType.FULLCHANGED, new HashMap<T, Expr>()).pack();
    }

    protected abstract Collection<Pair<Property<?>, LinkType>> calculateLinks();

    private Collection<Pair<Property<?>, LinkType>> links;
    @ManualLazy
    public Collection<Pair<Property<?>, LinkType>> getLinks() {
        if(links==null)
            links = calculateLinks();
        return links;
    }
}
