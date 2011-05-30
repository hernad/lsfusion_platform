package platform.server.logics;

import net.sf.jasperreports.engine.JRException;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.Result;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.server.Settings;
import platform.server.caches.IdentityLazy;
import platform.server.classes.*;
import platform.server.data.Time;
import platform.server.data.Union;
import platform.server.data.expr.query.OrderType;
import platform.server.data.where.classes.ClassWhere;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.FilterEntity;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.*;
import platform.server.logics.property.derived.AggregateGroupProperty;
import platform.server.logics.property.derived.ConcatenateProperty;
import platform.server.logics.property.derived.CycleGroupProperty;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.mail.EmailActionProperty;

import java.io.FileNotFoundException;
import java.util.*;

import static platform.base.BaseUtils.consecutiveInts;
import static platform.base.BaseUtils.toPrimitive;
import static platform.server.logics.PropertyUtils.*;

/**
 * User: DAle
 * Date: 16.05.11
 * Time: 17:37
 */

public abstract class LogicsModule {
    public abstract void initClasses();
    public abstract void initTables();
    public abstract void initGroups();
    public abstract void initProperties();
    public abstract void initIndexes();
    public abstract void initNavigators() throws JRException, FileNotFoundException;

    public BaseLogicsModule<?> baseLM;

    // aliases для использования внутри иерархии логических модулей
    protected BaseClass baseClass;

    protected AbstractGroup rootGroup;
    protected AbstractGroup publicGroup;
    protected AbstractGroup privateGroup;
    protected AbstractGroup baseGroup;
    protected AbstractGroup idGroup;
    protected AbstractGroup actionGroup;
    protected AbstractGroup sessionGroup;
    protected AbstractGroup recognizeGroup;

    protected void setBaseLogicsModule(BaseLogicsModule<?> baseLM) {
        this.baseLM = baseLM;
    }

    protected void initBaseGroupAliases() {
        this.rootGroup = baseLM.rootGroup;
        this.publicGroup = baseLM.publicGroup;
        this.privateGroup = baseLM.privateGroup;
        this.baseGroup = baseLM.baseGroup;
        this.idGroup = baseLM.idGroup;
        this.actionGroup = baseLM.actionGroup;
        this.sessionGroup = baseLM.sessionGroup;
        this.recognizeGroup = baseLM.recognizeGroup;
    }

    protected void initBaseClassAliases() {
        this.baseClass = baseLM.baseClass;
    }

    protected void storeCustomClass(CustomClass customClass) {
        assert !baseLM.sidToClass.containsKey(customClass.getSID());
        baseLM.sidToClass.put(customClass.getSID(), customClass);
    }

    protected ConcreteCustomClass addConcreteClass(AbstractGroup group, String sID, String caption, CustomClass... parents) {
        ConcreteCustomClass customClass = new ConcreteCustomClass(sID, caption, parents);
        group.add(customClass);
        storeCustomClass(customClass);
        return customClass;
    }

    protected BaseClass addBaseClass(String sID, String caption) {
        BaseClass baseClass = new BaseClass(sID, caption);
        storeCustomClass(baseClass);
        storeCustomClass(baseClass.named);
        return baseClass;
    }

    protected AbstractCustomClass addAbstractClass(AbstractGroup group, String sID, String caption, CustomClass... parents) {
        AbstractCustomClass customClass = new AbstractCustomClass(sID, caption, parents);
        group.add(customClass);
        storeCustomClass(customClass);
        return customClass;
    }

    protected CustomClass findCustomClass(String sid) {
        return baseLM.sidToClass.get(sid);
    }

    protected ValueClass findValueClass(String sid) {
        ValueClass valueClass = findCustomClass(sid);
        if (valueClass == null) {
            valueClass = DataClass.findDataClass(sid);
        }
        return valueClass;
    }

    protected ConcreteCustomClass addConcreteClass(String sID, String caption, CustomClass... parents) {
        return addConcreteClass(baseLM.baseGroup, sID, caption, parents);
    }

    protected AbstractCustomClass addAbstractClass(String sID, String caption, CustomClass... parents) {
        return addAbstractClass(baseLM.baseGroup, sID, caption, parents);
    }

    protected StaticCustomClass addStaticClass(String sID, String caption, String[] sids, String[] names) {
        StaticCustomClass customClass = new StaticCustomClass(sID, caption, baseLM.baseClass.sidClass, sids, names);
        storeCustomClass(customClass);
        customClass.dialogReadOnly = true;
        return customClass;
    }

    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////
    /// Properties
    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////

    public String genSID() {
        String id = "property" + baseLM.idSet.size();
        baseLM.idSet.add(id);
        return id;
    }

    protected LP addDProp(String sID, String caption, ValueClass value, ValueClass... params) {
        return addDProp(null, sID, false, caption, value, params);
    }

    protected LP addDProp(AbstractGroup group, String sID, String caption, ValueClass value, ValueClass... params) {
        return addDProp(group, sID, false, caption, value, params);
    }

    protected LP[] addDProp(AbstractGroup group, String paramID, String[] sIDs, String[] captions, ValueClass[] values, ValueClass... params) {
        LP[] result = new LP[sIDs.length];
        for (int i = 0; i < sIDs.length; i++)
            result[i] = addDProp(group, sIDs[i] + paramID, captions[i], values[i], params);
        return result;
    }

    protected LP addDProp(AbstractGroup group, String sID, boolean persistent, String caption, ValueClass value, ValueClass... params) {
        StoredDataProperty dataProperty = new StoredDataProperty(sID, caption, params, value);
        LP lp = addProperty(group, persistent, new LP<ClassPropertyInterface>(dataProperty));
        dataProperty.markStored(baseLM.tableFactory);
        return lp;
    }

    protected LP addGDProp(AbstractGroup group, String paramID, String sID, String caption, ValueClass[] values, CustomClass[]... params) {
        CustomClass[][] listParams = new CustomClass[params[0].length][]; //
        for (int i = 0; i < listParams.length; i++) {
            listParams[i] = new CustomClass[params.length];
            for (int j = 0; j < params.length; j++)
                listParams[i][j] = params[j][i];
        }
        params = listParams;

        LP[] genProps = new LP[params.length];
        for (int i = 0; i < params.length; i++) {
            String genID = "";
            String genCaption = "";
            for (int j = 0; j < params[i].length; j++) {
                genID += params[i][j].getSID();
                genCaption = (genCaption.length() == 0 ? "" : genCaption) + params[i][j].caption;
            }
            genProps[i] = addDProp(sID + genID, caption + " (" + genCaption + ")", values[i], params[i]);
        }

        return addCUProp(group, sID + paramID, caption, genProps);
    }

    protected LP[] addGDProp(AbstractGroup group, String paramID, String[] sIDs, String[] captions, ValueClass[][] values, CustomClass[]... params) {
        LP[] result = new LP[values.length];
        for (int i = 0; i < values.length; i++)
            result[i] = addGDProp(group, paramID, sIDs[i], captions[i], values[i], params);
        return result;
    }

    protected <D extends PropertyInterface> LP addDCProp(String sID, String caption, LP<D> derivedProp, Object... params) {
        return addDCProp(null, sID, caption, derivedProp, params);
    }

    protected <D extends PropertyInterface> LP addDCProp(String sID, boolean persistent, String caption, LP<D> derivedProp, Object... params) {
        return addDCProp(null, sID, persistent, caption, false, derivedProp, params);
    }

    protected <D extends PropertyInterface> LP addDCProp(AbstractGroup group, String sID, String caption, LP<D> derivedProp, Object... params) {
        return addDCProp(group, sID, false, caption, false, derivedProp, params);
    }

    protected <D extends PropertyInterface> LP addDCProp(String sID, String caption, boolean forced, LP<D> derivedProp, Object... params) {
        return addDCProp(null, sID, caption, forced, derivedProp, params);
    }

    protected <D extends PropertyInterface> LP addDCProp(AbstractGroup group, String sID, String caption, boolean forced, LP<D> derivedProp, Object... params) {
        return addDCProp(group, sID, false, caption, forced, derivedProp, params);
    }

    protected <D extends PropertyInterface> LP addDCProp(AbstractGroup group, String sID, boolean persistent, String caption, boolean forced, LP<D> derivedProp, Object... params) {

        // считываем override'ы с конца
        List<ValueClass> backClasses = new ArrayList<ValueClass>();
        int i = params.length - 1;
        while (i > 0 && (params[i] == null || params[i] instanceof ValueClass))
            backClasses.add((ValueClass) params[i--]);
        params = Arrays.copyOfRange(params, 0, i + 1);
        ValueClass[] overrideClasses = BaseUtils.reverse(backClasses).toArray(new ValueClass[1]);

        boolean defaultChanged = false;
        if (params[0] instanceof Boolean) {
            defaultChanged = (Boolean) params[0];
            params = Arrays.copyOfRange(params, 1, params.length);
        }

        // придется создавать Join свойство чтобы считать его класс
        List<PropertyUtils.LI> list = readLI(params);

        int propsize = derivedProp.listInterfaces.size();
        int dersize = getIntNum(params);
        JoinProperty<AndFormulaProperty.Interface> joinProperty = new JoinProperty<AndFormulaProperty.Interface>(sID, caption, dersize, false);
        LP<JoinProperty.Interface> listProperty = new LP<JoinProperty.Interface>(joinProperty);

        AndFormulaProperty andProperty = new AndFormulaProperty(genSID(), new boolean[list.size() - propsize]);
        Map<AndFormulaProperty.Interface, PropertyInterfaceImplement<JoinProperty.Interface>> mapImplement = new HashMap<AndFormulaProperty.Interface, PropertyInterfaceImplement<JoinProperty.Interface>>();
        mapImplement.put(andProperty.objectInterface, DerivedProperty.createJoin(mapImplement(derivedProp, mapLI(list.subList(0, propsize), listProperty.listInterfaces))));
        Iterator<AndFormulaProperty.AndInterface> itAnd = andProperty.andInterfaces.iterator();
        for (PropertyInterfaceImplement<JoinProperty.Interface> partProperty : mapLI(list.subList(propsize, list.size()), listProperty.listInterfaces))
            mapImplement.put(itAnd.next(), partProperty);

        joinProperty.implement = new PropertyImplement<AndFormulaProperty.Interface, PropertyInterfaceImplement<JoinProperty.Interface>>(andProperty, mapImplement);

        // получаем классы
        Result<ValueClass> value = new Result<ValueClass>();
        ValueClass[] commonClasses = listProperty.getCommonClasses(value);

        // override'им классы
        ValueClass valueClass;
        if (overrideClasses.length > dersize) {
            valueClass = overrideClasses[dersize];
            assert !overrideClasses[dersize].isCompatibleParent(value.result);
            overrideClasses = Arrays.copyOfRange(params, 0, dersize, ValueClass[].class);
        } else
            valueClass = value.result;

        // выполняем само создание свойства
        LP derDataProp = addDProp(group, sID, persistent, caption, valueClass, overrideClasses(commonClasses, overrideClasses));
        if (forced)
            derDataProp.setDerivedForcedChange(defaultChanged, derivedProp, params);
        else
            derDataProp.setDerivedChange(defaultChanged, derivedProp, params);
        return derDataProp;
    }

    protected LP addSDProp(String caption, ValueClass value, ValueClass... params) {
        return addSDProp((AbstractGroup) null, caption, value, params);
    }

    protected LP addSDProp(AbstractGroup group, String caption, ValueClass value, ValueClass... params) {
        return addSDProp(group, genSID(), caption, value, params);
    }

    protected LP addSDProp(String sID, String caption, ValueClass value, ValueClass... params) {
        return addSDProp(null, sID, caption, value, params);
    }

    protected LP addSDProp(String sID, boolean persistent, String caption, ValueClass value, ValueClass... params) {
        return addSDProp(null, sID, persistent, caption, value, params);
    }

    protected LP addSDProp(AbstractGroup group, String sID, String caption, ValueClass value, ValueClass... params) {
        return addSDProp(group, sID, false, caption, value, params);
    }

    protected LP addSDProp(AbstractGroup group, String sID, boolean persistent, String caption, ValueClass value, ValueClass... params) {
        return addProperty(group, persistent, new LP<ClassPropertyInterface>(new SessionDataProperty(sID, caption, params, value)));
    }

    protected LP addFAProp(String caption, FormEntity form, ObjectEntity... params) {
        return addFAProp(null, caption, form, params, new PropertyObjectEntity[0], new PropertyObjectEntity[0], false, false);
    }

    protected LP addFAProp(AbstractGroup group, FormEntity form, ObjectEntity... params) {
        return addFAProp(group, form.caption, form, params);
    }

    protected LP addFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity... params) {
        return addFAProp(group, caption, form, params, new PropertyObjectEntity[0], new PropertyObjectEntity[0], false, false);
    }

    protected LP addMFAProp(String caption, FormEntity form, ObjectEntity... params) {
        return addMFAProp(null, caption, form, params, new PropertyObjectEntity[0], new PropertyObjectEntity[0]);
    }

    protected LP addMFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, PropertyObjectEntity... setProperties) {
        return addMFAProp(group, caption, form, objectsToSet, false, setProperties);
    }

    protected LP addMFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, boolean newSession, PropertyObjectEntity... setProperties) {
        // во все setProperties просто будут записаны null'ы
        return addMFAProp(group, caption, form, objectsToSet, setProperties, new PropertyObjectEntity[setProperties.length], newSession);
    }

    protected LP addMFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, PropertyObjectEntity[] setProperties, PropertyObjectEntity[] getProperties) {
        return addMFAProp(group, caption, form, objectsToSet, setProperties, getProperties, true);
    }

    protected LP addMFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, PropertyObjectEntity[] setProperties, PropertyObjectEntity[] getProperties, boolean newSession) {
        return addFAProp(group, caption, form, objectsToSet, setProperties, getProperties, newSession, true);
    }

    protected LP addFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, PropertyObjectEntity[] setProperties, PropertyObjectEntity[] getProperties, boolean newSession, boolean isModal) {
        return addProperty(group, new LP<ClassPropertyInterface>(new FormActionProperty(genSID(), caption, form, objectsToSet, setProperties, getProperties, newSession, isModal)));
    }

    protected LP addSelectFromListAction(AbstractGroup group, String caption, LP selectionProperty, ValueClass selectionClass, ValueClass... baseClasses) {
        return addSelectFromListAction(group, caption, null, new FilterEntity[0], selectionProperty, selectionClass, baseClasses);
    }

    protected LP addSelectFromListAction(AbstractGroup group, String caption, ObjectEntity remapObject, FilterEntity[] remapFilters, LP selectionProperty, ValueClass selectionClass, ValueClass... baseClasses) {
        return addSelectFromListAction(group, caption, remapObject, remapFilters, selectionProperty, false, selectionClass, baseClasses);
    }

    protected LP addSelectFromListAction(AbstractGroup group, String caption, ObjectEntity remapObject, FilterEntity[] remapFilters, LP selectionProperty, boolean isSelectionClassFirstParam, ValueClass selectionClass, ValueClass... baseClasses) {
        BaseLogicsModule.SelectFromListFormEntity selectFromListForm = baseLM.new SelectFromListFormEntity(remapObject, remapFilters, selectionProperty, isSelectionClassFirstParam, selectionClass, baseClasses);
        return addMFAProp(group, caption, selectFromListForm, selectFromListForm.mainObjects, false);
    }

    protected LP addStopActionProp(String caption, String header) {
        return addAProp(new StopActionProperty(genSID(), caption, header));
    }

    protected LP addEAProp(ValueClass... params) {
        return addEAProp(null, params);
    }

    protected LP addEAProp(String subject, ValueClass... params) {
        return addEAProp(subject, baseLM.fromAddress, params);

    }

    protected LP addEAProp(String subject, LP fromAddress, ValueClass... params) {
        return addEAProp(null, genSID(), "email", subject, fromAddress, params);
    }

    protected LP addEAProp(AbstractGroup group, String sID, String caption, String subject, LP fromAddress, ValueClass... params) {
        return addProperty(group, new LP<ClassPropertyInterface>(new EmailActionProperty(sID, caption, subject, fromAddress, baseLM.BL, params)));
    }

    protected <X extends PropertyInterface> void addEARecepient(LP<ClassPropertyInterface> eaProp, LP<X> emailProp, Integer... params) {
        Map<X, ClassPropertyInterface> mapInterfaces = new HashMap<X, ClassPropertyInterface>();
        for (int i = 0; i < emailProp.listInterfaces.size(); i++)
            mapInterfaces.put(emailProp.listInterfaces.get(i), eaProp.listInterfaces.get(params[i] - 1));
        ((EmailActionProperty) eaProp.property).addRecepient(new PropertyMapImplement<X, ClassPropertyInterface>(emailProp.property, mapInterfaces));
    }

    protected void addInlineEAForm(LP<ClassPropertyInterface> eaProp, FormEntity form, Object... params) {
        Map<ObjectEntity, ClassPropertyInterface> mapObjects = new HashMap<ObjectEntity, ClassPropertyInterface>();
        for (int i = 0; i < params.length / 2; i++)
            mapObjects.put((ObjectEntity) params[2 * i], eaProp.listInterfaces.get((Integer) params[2 * i + 1] - 1));
        ((EmailActionProperty) eaProp.property).addInlineForm(form, mapObjects);
    }

    protected void addAttachEAForm(LP<ClassPropertyInterface> eaProp, FormEntity form, EmailActionProperty.Format format, Object... params) {
        LP attachmentName = null;
        if (params.length > 0 && params[0] instanceof LP) {
            attachmentName = (LP) params[0];
            params = Arrays.copyOfRange(params, 1, params.length);
        }
        Map<ObjectEntity, ClassPropertyInterface> mapObjects = new HashMap<ObjectEntity, ClassPropertyInterface>();
        for (int i = 0; i < params.length / 2; i++)
            mapObjects.put((ObjectEntity) params[2 * i], eaProp.listInterfaces.get((Integer) params[2 * i + 1] - 1));
        ((EmailActionProperty) eaProp.property).addAttachmentForm(form, format, mapObjects, attachmentName);
    }

    protected LP addTAProp(LP sourceProperty, LP targetProperty) {
        return addProperty(null, new LP<ClassPropertyInterface>(new TranslateActionProperty(genSID(), "translate", baseLM.translationDictionaryTerm, sourceProperty, targetProperty, baseLM.dictionary)));
    }

    protected <P extends PropertyInterface> LP addSCProp(LP<P> lp) {
        return addSCProp(baseLM.privateGroup, "sys", lp);
    }

    protected <P extends PropertyInterface> LP addSCProp(AbstractGroup group, String caption, LP<P> lp) {
        return addProperty(group, new LP<ShiftChangeProperty.Interface<P>>(new ShiftChangeProperty<P, PropertyInterface>(genSID(), caption, lp.property, new PropertyMapImplement<PropertyInterface, P>(baseLM.reverseBarcode.property))));
    }

    // добавляет свойство с бесконечным значением
    protected LP addICProp(DataClass valueClass, ValueClass... params) {
        return addProperty(baseLM.privateGroup, false, new LP<ClassPropertyInterface>(new InfiniteClassProperty(genSID(), "Беск.", params, valueClass)));
    }

    protected LP addCProp(StaticClass valueClass, Object value, ValueClass... params) {
        return addCProp("sys", valueClass, value, params);
    }

    protected LP addCProp(String caption, StaticClass valueClass, Object value, ValueClass... params) {
        return addCProp(null, false, caption, valueClass, value, params);
    }

    protected LP addCProp(AbstractGroup group, boolean persistent, String caption, StaticClass valueClass, Object value, ValueClass... params) {
        return addCProp(group, persistent, caption, valueClass, value, Arrays.asList(params));
    }

    // только для того, чтобы обернуть все в IdentityLazy, так как только для List нормально сделан equals
    @IdentityLazy
    protected LP addCProp(AbstractGroup group, boolean persistent, String caption, StaticClass valueClass, Object value, List<ValueClass> params) {
        return addCProp(group, genSID(), persistent, caption, valueClass, value, params.toArray(new ValueClass[]{}));
    }

    protected LP addCProp(AbstractGroup group, String sID, boolean persistent, String caption, StaticClass valueClass, Object value, ValueClass... params) {
        return addProperty(group, persistent, new LP<ClassPropertyInterface>(new ValueClassProperty(sID, caption, params, valueClass, value)));
    }

    protected LP addTProp(Time time) {
        return addProperty(null, new LP<PropertyInterface>(new TimeFormulaProperty(genSID(), time)));
    }

    protected <P extends PropertyInterface> LP addTCProp(Time time, String sID, String caption, LP<P> changeProp, ValueClass... classes) {
        return addTCProp(null, time, sID, caption, changeProp, classes);
    }

    protected <P extends PropertyInterface> LP addTCProp(Time time, String sID, boolean isStored, String caption, LP<P> changeProp, ValueClass... classes) {
        return addTCProp(null, time, sID, isStored, caption, changeProp, classes);
    }

    protected <P extends PropertyInterface> LP addTCProp(AbstractGroup group, Time time, String sID, String caption, LP<P> changeProp, ValueClass... classes) {
        return addTCProp(group, time, sID, false, caption, changeProp, classes);
    }

    protected <P extends PropertyInterface> LP addTCProp(AbstractGroup group, Time time, String sID, boolean isStored, String caption, LP<P> changeProp, ValueClass... classes) {
        TimePropertyChange<P> timeProperty = new TimePropertyChange<P>(isStored, time, sID, caption, overrideClasses(changeProp.getMapClasses(), classes), changeProp.listInterfaces);

        changeProp.property.timeChanges.put(time, timeProperty);

        if (isStored) {
            timeProperty.property.markStored(baseLM.tableFactory);
        }

        return addProperty(group, false, new LP<ClassPropertyInterface>(timeProperty.property));
    }

    protected LP addSFProp(String formula, ConcreteValueClass value, int paramCount) {
        return addProperty(null, new LP<StringFormulaProperty.Interface>(new StringFormulaProperty(genSID(), value, formula, paramCount)));
    }

    protected LP addCFProp(Compare compare) {
        return addProperty(null, new LP<CompareFormulaProperty.Interface>(new CompareFormulaProperty(genSID(), compare)));
    }

    protected <P extends PropertyInterface> LP addSProp(int intNum) {
        return addProperty(null, new LP<StringConcatenateProperty.Interface>(new StringConcatenateProperty(genSID(), "Объед.", intNum, " ")));
    }

    protected <P extends PropertyInterface> LP addSProp(int intNum, String separator) {
        return addProperty(null, new LP<StringConcatenateProperty.Interface>(new StringConcatenateProperty(genSID(), "Объед.", intNum, separator)));
    }

    protected <P extends PropertyInterface> LP addInsensitiveSProp(int intNum) {
        return addProperty(null, new LP<StringConcatenateProperty.Interface>(new StringConcatenateProperty(genSID(), "Объед.", intNum, " ", false)));
    }

    protected <P extends PropertyInterface> LP addInsensitiveSProp(int intNum, String separator) {
        return addProperty(null, new LP<StringConcatenateProperty.Interface>(new StringConcatenateProperty(genSID(), "Объед.", intNum, separator, false)));
    }

    protected LP addMFProp(ConcreteValueClass value, int paramCount) {
        return addProperty(null, new LP<StringFormulaProperty.Interface>(new MultiplyFormulaProperty(genSID(), value, paramCount)));
    }

    protected LP addAFProp(boolean... nots) {
        return addAFProp((AbstractGroup) null, nots);
    }

    protected LP addAFProp(String sID, boolean... nots) {
        return addAFProp(null, sID, nots);
    }

    protected LP addAFProp(AbstractGroup group, boolean... nots) {
        return addAFProp(group, genSID(), nots);
    }

    protected LP addAFProp(AbstractGroup group, String sID, boolean... nots) {
        return addProperty(group, new LP<AndFormulaProperty.Interface>(new AndFormulaProperty(sID, nots)));
    }

    protected LP addCCProp(int paramCount) {
        return addProperty(null, new LP<ConcatenateProperty.Interface>(new ConcatenateProperty(paramCount)));
    }

    protected LP addJProp(LP mainProp, Object... params) {
        return addJProp(baseLM.privateGroup, "sys", mainProp, params);
    }

    protected LP addJProp(String caption, LP mainProp, Object... params) {
        return addJProp((AbstractGroup) null, caption, mainProp, params);
    }

    protected LP addJProp(String sID, String caption, LP mainProp, Object... params) {
        return addJProp(sID, false, caption, mainProp, params);
    }

    protected LP addJProp(String sID, boolean persistent, String caption, LP mainProp, Object... params) {
        return addJProp(null, sID, persistent, caption, mainProp, params);
    }

    protected LP addJProp(AbstractGroup group, String caption, LP mainProp, Object... params) {
        return addJProp(group, genSID(), caption, mainProp, params);
    }

    protected LP addJProp(boolean implementChange, String caption, LP mainProp, Object... params) {
        return addJProp((AbstractGroup) null, implementChange, caption, mainProp, params);
    }

    protected LP addJProp(boolean implementChange, String sID, String caption, LP mainProp, Object... params) {
        return addJProp((AbstractGroup) null, implementChange, sID, caption, mainProp, params);
    }

    protected LP addJProp(AbstractGroup group, boolean implementChange, String caption, LP mainProp, Object... params) {
        return addJProp(group, implementChange, genSID(), caption, mainProp, params);
    }

    protected LP addJProp(AbstractGroup group, String sID, String caption, LP mainProp, Object... params) {
        return addJProp(group, mainProp.property instanceof ActionProperty, sID, caption, mainProp, params);
    }

    protected LP addJProp(AbstractGroup group, String sID, boolean persistent, String caption, LP mainProp, Object... params) {
        return addJProp(group, mainProp.property instanceof ActionProperty, sID, persistent, caption, mainProp, params);
    }

    protected LP addJProp(boolean implementChange, LP mainProp, Object... params) {
        return addJProp(baseLM.privateGroup, implementChange, genSID(), "sys", mainProp, params);
    }

    protected LP addJProp(AbstractGroup group, boolean implementChange, String sID, String caption, LP mainProp, Object... params) {
        return addJProp(group, implementChange, sID, false, caption, mainProp, params);
    }

    protected LP addJProp(AbstractGroup group, boolean implementChange, String sID, boolean persistent, String caption, LP mainProp, Object... params) {

        JoinProperty<?> property = new JoinProperty(sID, caption, getIntNum(params), implementChange);
        property.inheritFixedCharWidth(mainProp.property);

        LP listProperty = new LP<JoinProperty.Interface>(property);
        property.implement = mapImplement(mainProp, readImplements(listProperty.listInterfaces, params));

        return addProperty(group, persistent, listProperty);
    }

    protected LP[] addJProp(AbstractGroup group, boolean implementChange, String paramID, LP[] props, String caption, Object... params) {
        LP[] result = new LP[props.length];
        for (int i = 0; i < props.length; i++)
            result[i] = addJProp(group, implementChange, props[i].property.sID + paramID, props[i].property.caption + (caption.length() == 0 ? "" : (" " + caption)), props[i], params);
        return result;
    }

    protected LP[] addJProp(AbstractGroup group, boolean implementChange, String paramID, LP[] props, Object... params) {
        return addJProp(group, implementChange, paramID, props, "", params);
    }

    /**
     * Создаёт свойство для группового изменения, при этом итерация идёт по всем интерфейсам, и мэппинг интерфейсов происходит по порядку
     */
    protected LP addGCAProp(AbstractGroup group, String sID, String caption, GroupObjectEntity groupObject, LP mainProperty, LP getterProperty) {
        int groupInts[] = consecutiveInts(mainProperty.listInterfaces.size());
        int getterInts[] = consecutiveInts(getterProperty.listInterfaces.size());

        return addGCAProp(group, sID, caption, groupObject, mainProperty, groupInts, getterProperty, getterInts);
    }

    /**
     * Создаёт свойство для группового изменения
     * Пример:
     * <pre>
     *   LP ценаПоставкиТовара = Свойство(Товар)
     *   LP ценаПродажиТовара = Свойство(Магазин, Товар)
     *
     *   Тогда, чтобы установить цену для всех товаров в магазине, равной цене поставки товара, создаём свойство
     *
     *   addGCAProp(..., ценаПродажиТовара, 2, ценаПоставкиТовара, 1)
     * </pre>
     *
     * @param groupObject используется для получения фильтров на набор, для которого будут происходить изменения
     * @param params сначала идут номера интерфейсов для группировки, затем getterProperty, затем мэппинг интерфейсов getterProperty
     */
    protected LP addGCAProp(AbstractGroup group, String sID, String caption, GroupObjectEntity groupObject, LP mainProperty, Object... params) {
        assert params.length > 0;

        List<Integer> groupInts = new ArrayList<Integer>();
        int i = 0;
        while (!(params[i] instanceof LP)) {
            groupInts.add((Integer) params[i++] - 1);
        }

        LP getterProperty = (LP) params[i++];

        List<Integer> getterInts = new ArrayList<Integer>();
        while (i < params.length) {
            getterInts.add((Integer) params[i++] - 1);
        }

        return addGCAProp(group, sID, caption, groupObject, mainProperty, toPrimitive(groupInts), getterProperty, toPrimitive(getterInts));
    }

    private LP addGCAProp(AbstractGroup group, String sID, String caption, GroupObjectEntity groupObject, LP mainProperty, int[] groupInts, LP getterProperty, int[] getterInts) {
        return addProperty(group, new LP<ClassPropertyInterface>(
                new GroupChangeActionProperty(sID, caption, groupObject,
                        mainProperty, groupInts,
                        getterProperty, getterInts)));
    }

    private <T extends PropertyInterface> LP addGProp(AbstractGroup group, String sID, boolean persistent, String caption, LP<T> groupProp, List<PropertyInterfaceImplement<T>> listImplements) {
        GroupProperty<T> property = new SumGroupProperty<T>(sID, caption, listImplements, groupProp.property);
        return mapLGProp(group, persistent, property, listImplements);
    }

    private <P extends PropertyInterface, L extends PropertyInterface> LP mapLProp(AbstractGroup group, boolean persistent, PropertyMapImplement<L, P> implement, LP<P> property) {
        return addProperty(group, persistent, new LP<L>(implement.property, BaseUtils.mapList(property.listInterfaces, BaseUtils.reverse(implement.mapping))));
    }

    private <P extends PropertyInterface, L extends PropertyInterface> LP mapLGProp(AbstractGroup group, PropertyImplement<L, PropertyInterfaceImplement<P>> implement, List<PropertyInterfaceImplement<P>> listImplements) {
        return mapLGProp(group, false, implement, listImplements);
    }

    private <P extends PropertyInterface, L extends PropertyInterface> LP mapLGProp(AbstractGroup group, boolean persistent, PropertyImplement<L, PropertyInterfaceImplement<P>> implement, List<PropertyInterfaceImplement<P>> listImplements) {
        return addProperty(group, persistent, new LP<L>(implement.property, BaseUtils.mapList(listImplements, BaseUtils.reverse(implement.mapping))));
    }

    private <P extends PropertyInterface> LP mapLGProp(AbstractGroup group, boolean persistent, GroupProperty<P> property, List<PropertyInterfaceImplement<P>> listImplements) {
        return mapLGProp(group, persistent, new PropertyImplement<GroupProperty.Interface<P>, PropertyInterfaceImplement<P>>(property, property.getMapInterfaces()), listImplements);
    }

    protected <P extends PropertyInterface> LP addOProp(String caption, OrderType orderType, LP<P> sum, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addOProp(genSID(), caption, orderType, sum, ascending, includeLast, partNum, params);
    }

    protected <P extends PropertyInterface> LP addOProp(String sID, String caption, OrderType orderType, LP<P> sum, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addOProp((AbstractGroup) null, sID, caption, orderType, sum, ascending, includeLast, partNum, params);
    }

    protected <P extends PropertyInterface> LP addOProp(AbstractGroup group, String caption, OrderType orderType, LP<P> sum, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addOProp(group, genSID(), caption, orderType, sum, ascending, includeLast, partNum, params);
    }

    protected <P extends PropertyInterface> LP addOProp(AbstractGroup group, String sID, String caption, OrderType orderType, LP<P> sum, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addOProp(group, sID, false, caption, sum, false, orderType, ascending, includeLast, partNum, params);
    }

    // проценты
    protected <P extends PropertyInterface> LP addPOProp(AbstractGroup group, String caption, LP<P> sum, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addPOProp(group, genSID(), false, caption, sum, ascending, includeLast, partNum, params);
    }

    protected <P extends PropertyInterface> LP addPOProp(AbstractGroup group, String sID, boolean persistent, String caption, LP<P> sum, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addOProp(group, sID, persistent, caption, sum, true, null, ascending, includeLast, partNum, params);
    }

    private <P extends PropertyInterface> LP addOProp(AbstractGroup group, String sID, boolean persistent, String caption, LP<P> sum, boolean percent, OrderType orderType, boolean ascending, boolean includeLast, int partNum, Object... params) {
        List<LI> li = readLI(params);

        Collection<PropertyInterfaceImplement<P>> partitions = mapLI(li.subList(0, partNum), sum.listInterfaces);
        OrderedMap<PropertyInterfaceImplement<P>, Boolean> orders = new OrderedMap<PropertyInterfaceImplement<P>, Boolean>(mapLI(li.subList(partNum, li.size()), sum.listInterfaces), !ascending);

        PropertyMapImplement<?, P> orderProperty;
        if (percent)
            orderProperty = DerivedProperty.createPOProp(sID, caption, sum.property, partitions, orders, includeLast);
        else
            orderProperty = DerivedProperty.createOProp(sID, caption, orderType, sum.property, partitions, orders, includeLast);

        return mapLProp(group, persistent, orderProperty, sum);
    }

    protected <R extends PropertyInterface, L extends PropertyInterface> LP addUGProp(AbstractGroup group, String caption, boolean ascending, LP<R> restriction, LP<L> ungroup, Object... params) {
        return addUGProp(group, genSID(), caption, ascending, restriction, ungroup, params);
    }

    protected <R extends PropertyInterface, L extends PropertyInterface> LP addUGProp(AbstractGroup group, String sID, String caption, boolean ascending, LP<R> restriction, LP<L> ungroup, Object... params) {
        return addUGProp(group, sID, false, caption, ascending, restriction, ungroup, params);
    }

    protected <R extends PropertyInterface, L extends PropertyInterface> LP addUGProp(AbstractGroup group, String sID, boolean persistent, String caption, boolean ascending, LP<R> restriction, LP<L> ungroup, Object... params) {
        List<LI> li = readLI(params);

        Map<L, PropertyInterfaceImplement<R>> groupImplement = new HashMap<L, PropertyInterfaceImplement<R>>();
        for (int i = 0; i < ungroup.listInterfaces.size(); i++)
            groupImplement.put(ungroup.listInterfaces.get(i), li.get(i).map(restriction.listInterfaces));
        OrderedMap<PropertyInterfaceImplement<R>, Boolean> orders = new OrderedMap<PropertyInterfaceImplement<R>, Boolean>(mapLI(li.subList(ungroup.listInterfaces.size(), li.size()), restriction.listInterfaces), ascending);
        return mapLProp(group, persistent, DerivedProperty.createUGProp(sID, caption, new PropertyImplement<L, PropertyInterfaceImplement<R>>(ungroup.property, groupImplement), orders, restriction.property), restriction);
    }

    protected <R extends PropertyInterface, L extends PropertyInterface> LP addPGProp(AbstractGroup group, String sID, boolean persistent, int roundlen, boolean roundfirst, String caption, LP<R> proportion, LP<L> ungroup, Object... params) {
        List<LI> li = readLI(params);

        Map<L, PropertyInterfaceImplement<R>> groupImplement = new HashMap<L, PropertyInterfaceImplement<R>>();
        for (int i = 0; i < ungroup.listInterfaces.size(); i++)
            groupImplement.put(ungroup.listInterfaces.get(i), li.get(i).map(proportion.listInterfaces));
        return mapLProp(group, persistent, DerivedProperty.createPGProp(sID, caption, roundlen, roundfirst, baseLM.baseClass, new PropertyImplement<L, PropertyInterfaceImplement<R>>(ungroup.property, groupImplement), proportion.property), proportion);
    }

    /*
      // свойство обратное группируещему - для этого задается ограничивающее свойство, результирующее св-во с группировочными, порядковое св-во
      protected LF addUGProp(AbstractGroup group, String title, LF maxGroupProp, LF unGroupProp, Object... params) {
          List<LI> lParams = readLI(params);
          List<LI> lUnGroupParams = lParams.subList(0,unGroupProp.listInterfaces.size());
          List<LI> orderParams = lParams.subList(unGroupProp.listInterfaces.size(),lParams.size());

          int intNum = maxGroupProp.listInterfaces.size();

          // "двоим" интерфейсы, для результ. св-ва
          // ставим equals'ы на группировочные свойства (раздвоенные)
          List<Object[]> groupParams = new ArrayList<Object[]>();
          groupParams.add(directLI(maxGroupProp));
          for(LI li : lUnGroupParams)
              groupParams.add(li.compare(equals2, this, intNum));

          boolean[] andParams = new boolean[groupParams.size()-1];
          for(int i=0;i<andParams.length;i++)
              andParams[i] = false;
          LF groupPropSet = addJProp(addAFProp(andParams),BaseUtils.add(groupParams));

          for(int i=0;i<intNum;i++) { // докинем не достающие порядки
              boolean found = false;
              for(LI order : orderParams)
                  if(order instanceof LII && ((LII)order).intNum==i+1) {
                      found = true;
                      break;
                  }
              if(!found)
                  orderParams.add(new LII(i+1));
          }

          // ставим на предшествие сначала order'а, потом всех интерфейсов
          LF[] orderProps = new LF[orderParams.size()];
          for(int i=0;i<orderParams.size();i++) {
              orderProps[i] = (addJProp(and1, BaseUtils.add(directLI(groupPropSet),orderParams.get(i).compare(greater2, this, intNum))));
              groupPropSet = addJProp(and1, BaseUtils.add(directLI(groupPropSet),orderParams.get(i).compare(equals2, this, intNum)));
          }
          LF groupPropPrev = addSUProp(Union.OVERRIDE, orderProps);

          // группируем суммируя по "задвоенным" св-вам maxGroup
          Object[] remainParams = new Object[intNum];
          for(int i=1;i<=intNum;i++)
              remainParams[i-1] = i+intNum;
          LF remainPrev = addSGProp(groupPropPrev, remainParams);

          // создадим группировочное св-во с маппом на общий интерфейс, нужно поубирать "дырки"


          // возвращаем MIN2(unGroup-MU(prevGroup,0(maxGroup)),maxGroup) и не unGroup<=prevGroup
          LF zeroQuantity = addJProp(and1, BaseUtils.add(new Object[]{vzero},directLI(maxGroupProp)));
          LF zeroRemainPrev = addSUProp(Union.OVERRIDE , zeroQuantity, remainPrev);
          LF calc = addSFProp("prm3+prm1-prm2-GREATEST(prm3,prm1-prm2)",DoubleClass.instance,3);
          LF maxRestRemain = addJProp(calc, BaseUtils.add(BaseUtils.add(unGroupProp.write(),directLI(zeroRemainPrev)),directLI(maxGroupProp)));
          LF exceed = addJProp(groeq2, BaseUtils.add(directLI(remainPrev),unGroupProp.write()));
          return addJProp(group, title, andNot1, BaseUtils.add(directLI(maxRestRemain),directLI(exceed)));
      }
    */

    protected LP addSGProp(LP groupProp, Object... params) {
        return addSGProp(baseLM.privateGroup, "sys", groupProp, params);
    }

    protected LP addSGProp(String caption, LP groupProp, Object... params) {
        return addSGProp((AbstractGroup) null, caption, groupProp, params);
    }

    protected LP addSGProp(AbstractGroup group, String caption, LP groupProp, Object... params) {
        return addSGProp(group, genSID(), caption, groupProp, params);
    }

    protected LP addSGProp(String sID, String caption, LP groupProp, Object... params) {
        return addSGProp(sID, false, caption, groupProp, params);
    }

    protected LP addSGProp(String sID, boolean persistent, String caption, LP groupProp, Object... params) {
        return addSGProp(null, sID, persistent, caption, groupProp, params);
    }

    protected LP addSGProp(AbstractGroup group, String sID, String caption, LP groupProp, Object... params) {
        return addSGProp(group, sID, false, caption, groupProp, params);
    }

    protected LP addSGProp(AbstractGroup group, String sID, boolean persistent, String caption, LP groupProp, Object... params) {
        return addSGProp(group, sID, persistent, false, caption, groupProp, params);
    }

    protected <T extends PropertyInterface> LP addSGProp(AbstractGroup group, String sID, boolean persistent, boolean notZero, String caption, LP<T> groupProp, Object... params) {
        return addSGProp(group, sID, persistent, notZero, caption, groupProp, readImplements(groupProp.listInterfaces, params));
    }

    private <T extends PropertyInterface> LP addSGProp(AbstractGroup group, String sID, boolean persistent, boolean notZero, String caption, LP<T> groupProp, List<PropertyInterfaceImplement<T>> listImplements) {
        boolean wrapNotZero = persistent && (notZero || !Settings.instance.isDisableSumGroupNotZero());
        SumGroupProperty<T> property = new SumGroupProperty<T>(wrapNotZero ? genSID() : sID, caption, listImplements, groupProp.property);

        LP result;
        if (wrapNotZero)
            result = addJProp(group, sID, persistent, caption, baseLM.onlyNotZero, directLI(mapLGProp(null, false, property, listImplements)));
        else
            result = mapLGProp(group, persistent, property, listImplements);

        result.sumGroup = property; // так как может wrap'ся, использование - setDG
        result.groupProperty = groupProp; // для порядка параметров, использование - setDG

        return result;
    }

    protected LP addMGProp(LP groupProp, Object... params) {
        return addMGProp(baseLM.privateGroup, genSID(), "sys", groupProp, params);
    }

    protected LP addMGProp(String sID, String caption, LP groupProp, Object... params) {
        return addMGProp(null, sID, caption, groupProp, params);
    }

    protected LP addMGProp(AbstractGroup group, String sID, String caption, LP groupProp, Object... params) {
        return addMGProp(group, sID, false, caption, groupProp, params);
    }

    protected LP addMGProp(AbstractGroup group, String sID, boolean persist, String caption, LP groupProp, Object... params) {
        return addMGProp(group, persist, new String[]{sID}, new String[]{caption}, 0, groupProp, params)[0];
    }

    protected <T extends PropertyInterface> LP[] addMGProp(AbstractGroup group, String[] ids, String[] captions, int extra, LP<T> groupProp, Object... params) {
        return addMGProp(group, false, ids, captions, extra, groupProp, params);
    }

    protected <T extends PropertyInterface> LP[] addMGProp(AbstractGroup group, boolean persist, String[] ids, String[] captions, int extra, LP<T> groupProp, Object... params) {
        LP[] result = new LP[extra + 1];

        Collection<Property> suggestPersist = new ArrayList<Property>();

        List<PropertyInterfaceImplement<T>> listImplements = readImplements(groupProp.listInterfaces, params);
        List<PropertyInterfaceImplement<T>> groupImplements = listImplements.subList(extra, listImplements.size());
        List<PropertyImplement<?, PropertyInterfaceImplement<T>>> mgProps = DerivedProperty.createMGProp(ids, captions, groupProp.property, baseLM.baseClass,
                listImplements.subList(0, extra), new HashSet<PropertyInterfaceImplement<T>>(groupImplements), suggestPersist);

        if (persist)
            for (Property property : suggestPersist)
                addPersistent(addProperty(null, new LP(property)));

        for (int i = 0; i < mgProps.size(); i++)
            result[i] = mapLGProp(group, mgProps.get(i), groupImplements);
        return result;

        /*
        List<LI> li = readLI(params);
        Object[] interfaces = writeLI(li.subList(extra,li.size())); // "вырежем" группировочные интерфейсы

        LF[] result = new LF[extra+1];
        int i = 0;
        do {
            result[i] = addGProp(group,ids[i],captions[i],groupProp,false,interfaces);
            if(i<extra) // если не последняя
                groupProp = addJProp(and1, BaseUtils.add(li.get(i).write(),directLI( // само свойство
                        addJProp(equals2, BaseUtils.add(directLI(groupProp),directLI( // только те кто дает предыдущий максимум
                        addJProp(result[i], interfaces))))))); // предыдущий максимум
        } while (i++<extra);
        return result;*/
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LP addCGProp(AbstractGroup group, String sID, String caption, LP<T> groupProp, LP<P> dataProp, Object... params) {
        return addCGProp(group, true, sID, caption, groupProp, dataProp, params);
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LP addCGProp(AbstractGroup group, String sID, boolean persistent, String caption, LP<T> groupProp, LP<P> dataProp, Object... params) {
        return addCGProp(group, true, sID, persistent, caption, groupProp, dataProp, params);
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LP addCGProp(AbstractGroup group, boolean checkChange, String sID, String caption, LP<T> groupProp, LP<P> dataProp, Object... params) {
        return addCGProp(group, checkChange, sID, false, caption, groupProp, dataProp, params);
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LP addCGProp(AbstractGroup group, boolean checkChange, String sID, boolean persistent, String caption, LP<T> groupProp, LP<P> dataProp, Object... params) {
        List<PropertyInterfaceImplement<T>> listImplements = readImplements(groupProp.listInterfaces, params);
        CycleGroupProperty<T, P> property = new CycleGroupProperty<T, P>(sID, caption, listImplements, groupProp.property, dataProp.property);

        // нужно добавить ограничение на уникальность
        addProperty(null, new LP(property.getConstrainedProperty(checkChange)));

        return mapLGProp(group, persistent, property, listImplements);
    }

//    protected static <T extends PropertyInterface<T>> AggregateGroupProperty create(String sID, String caption, Property<T> property, T aggrInterface, Collection<PropertyMapImplement<?, T>> groupProps) {

    protected LP addAGProp(String sID, String caption, LP... props) {
        return addAGProp(null, sID, caption, props);
    }

    protected LP addAGProp(AbstractGroup group, String sID, String caption, LP... props) {
        ClassWhere<Integer> classWhere = ClassWhere.<Integer>STATIC(true);
        for (LP<?> prop : props)
            classWhere = classWhere.and(prop.getClassWhere());
        return addAGProp(group, sID, caption, (CustomClass) BaseUtils.singleValue(classWhere.getCommonParent(Collections.singleton(1))), props);
    }

    protected LP addAGProp(String sID, String caption, CustomClass customClass, LP... props) {
        return addAGProp(null, sID, caption, customClass, props);
    }

    protected LP addAGProp(AbstractGroup group, String sID, String caption, CustomClass customClass, LP... props) {
        return addAGProp(group, false, sID, false, caption, customClass, props);
    }

    protected LP addAGProp(AbstractGroup group, boolean checkChange, String sID, boolean persistent, String caption, CustomClass customClass, LP... props) {
        return addAGProp(group, checkChange, sID, persistent, caption, is(customClass), 1, getUParams(props, 0));
    }

    protected <T extends PropertyInterface<T>> LP addAGProp(String sID, String caption, LP<T> lp, int aggrInterface, Object... props) {
        return addAGProp(sID, false, caption, lp, aggrInterface, props);
    }

    protected <T extends PropertyInterface> LP addAGProp(AbstractGroup group, String sID, String caption, LP<T> lp, int aggrInterface, Object... props) {
        return addAGProp(group, false, sID, false, caption, lp, aggrInterface, props);
    }

    protected <T extends PropertyInterface<T>> LP addAGProp(String sID, boolean persistent, String caption, LP<T> lp, int aggrInterface, Object... props) {
        return addAGProp(null, false, sID, persistent, caption, lp, aggrInterface, props);
    }

    protected <T extends PropertyInterface<T>> LP addAGProp(AbstractGroup group, String sID, boolean persistent, String caption, LP<T> lp, int aggrInterface, Object... props) {
        return addAGProp(group, false, sID, persistent, caption, lp, aggrInterface, props);
    }

    protected <T extends PropertyInterface<T>> LP addAGProp(AbstractGroup group, boolean checkChange, String sID, boolean persistent, String caption, LP<T> lp, int aggrInterface, Object... props) {
        List<PropertyInterfaceImplement<T>> listImplements = readImplements(lp.listInterfaces, props);

        return addAGProp(group, checkChange, persistent, AggregateGroupProperty.create(sID, caption, lp.property, lp.listInterfaces.get(aggrInterface - 1), (List<PropertyMapImplement<?, T>>) (List<?>) listImplements), BaseUtils.mergeList(listImplements, BaseUtils.removeList(lp.listInterfaces, aggrInterface - 1)));
    }

    // чисто для generics
    private <T extends PropertyInterface<T>, J extends PropertyInterface> LP addAGProp(AbstractGroup group, boolean checkChange, boolean persistent, AggregateGroupProperty<T, J> property, List<PropertyInterfaceImplement<T>> listImplements) {
        // нужно добавить ограничение на уникальность
        addProperty(null, new LP(property.getConstrainedProperty(checkChange)));

        return mapLGProp(group, persistent, property, DerivedProperty.mapImplements(listImplements, property.getMapping()));
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LP addDGProp(int orders, boolean ascending, LP<T> groupProp, Object... params) {
        return addDGProp(baseLM.privateGroup, "sys", orders, ascending, groupProp, params);
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LP addDGProp(AbstractGroup group, String caption, int orders, boolean ascending, LP<T> groupProp, Object... params) {
        return addDGProp(group, genSID(), caption, orders, ascending, groupProp, params);
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LP addDGProp(AbstractGroup group, String sID, String caption, int orders, boolean ascending, LP<T> groupProp, Object... params) {
        return addDGProp(group, sID, false, caption, orders, ascending, groupProp, params);
    }

    protected <T extends PropertyInterface> LP addDGProp(AbstractGroup group, String sID, boolean persistent, String caption, int orders, boolean ascending, LP<T> groupProp, Object... params) {
        List<PropertyInterfaceImplement<T>> listImplements = readImplements(groupProp.listInterfaces, params);
        int intNum = listImplements.size();
        LP result = addSGProp(group, sID, persistent, false, caption, groupProp, listImplements.subList(0, intNum - orders - 1));
        result.setDG(ascending, listImplements.subList(intNum - orders - 1, intNum));
        return result;
    }

    protected LP addUProp(AbstractGroup group, String sID, String caption, Union unionType, Object... params) {
        return addUProp(group, sID, false, caption, unionType, params);
    }

    protected LP addUProp(AbstractGroup group, String sID, boolean persistent, String caption, Union unionType, Object... params) {

        int intNum = ((LP) params[unionType == Union.SUM ? 1 : 0]).listInterfaces.size();

        UnionProperty property = null;
        int extra = 0;
        switch (unionType) {
            case MAX:
                property = new MaxUnionProperty(sID, caption, intNum);
                break;
            case SUM:
                property = new SumUnionProperty(sID, caption, intNum);
                extra = 1;
                break;
            case OVERRIDE:
                property = new OverrideUnionProperty(sID, caption, intNum);
                break;
            case XOR:
                property = new XorUnionProperty(sID, caption, intNum);
                break;
            case EXCLUSIVE:
                property = new ExclusiveUnionProperty(sID, caption, intNum);
                break;
        }

        LP listProperty = new LP<UnionProperty.Interface>(property);

        for (int i = 0; i < params.length / (intNum + 1 + extra); i++) {
            Integer offs = i * (intNum + 1 + extra);
            LP<?> opImplement = (LP) params[offs + extra];
            PropertyMapImplement operand = new PropertyMapImplement(opImplement.property);
            for (int j = 0; j < intNum; j++)
                operand.mapping.put(opImplement.listInterfaces.get(((Integer) params[offs + 1 + extra + j]) - 1), listProperty.listInterfaces.get(j));

            switch (unionType) {
                case MAX:
                    ((MaxUnionProperty) property).operands.add(operand);
                    break;
                case SUM:
                    ((SumUnionProperty) property).operands.put(operand, (Integer) params[offs]);
                    break;
                case OVERRIDE:
                    ((OverrideUnionProperty) property).operands.add(operand);
                    break;
                case XOR:
                    ((XorUnionProperty) property).operands.add(operand);
                    break;
                case EXCLUSIVE:
                    ((ExclusiveUnionProperty) property).operands.add(operand);
                    break;
            }
        }

        return addProperty(group, persistent, listProperty);
    }

    protected LP addCaseUProp(AbstractGroup group, String sID, boolean persistent, String caption, Object... params) {
        List<LI> list = readLI(params);
        int intNum = ((LMI)list.get(1)).lp.listInterfaces.size(); // берем количество интерфейсов у первого case'а

        CaseUnionProperty caseProp = new CaseUnionProperty(sID, caption, intNum);
        LP<UnionProperty.Interface> listProperty = new LP<UnionProperty.Interface>(caseProp);
        List<PropertyMapImplement<?, UnionProperty.Interface>> mapImplements = (List<PropertyMapImplement<?, UnionProperty.Interface>>)(List<?>)mapLI(list, listProperty.listInterfaces);
        for(int i=0;i<mapImplements.size()/2;i++)
            caseProp.addCase(mapImplements.get(2*i), mapImplements.get(2*i+1));
        if(mapImplements.size()%2!=0)
            caseProp.addCase(new PropertyMapImplement<PropertyInterface, UnionProperty.Interface>(baseLM.vtrue.property), mapImplements.get(mapImplements.size()-1));
        return addProperty(group, persistent, listProperty);
    }

    // объединение классовое (непересекающихся) свойств

    protected LP addCUProp(LP... props) {
        return addCUProp(baseLM.privateGroup, "sys", props);
    }

    protected LP addCUProp(String caption, LP... props) {
        return addCUProp((AbstractGroup) null, caption, props);
    }

    protected LP addCUProp(AbstractGroup group, String caption, LP... props) {
        return addCUProp(group, genSID(), caption, props);
    }

    protected LP addCUProp(String sID, String caption, LP... props) {
        return addCUProp(sID, false, caption, props);
    }

    protected LP addCUProp(String sID, boolean persistent, String caption, LP... props) {
        return addCUProp(null, sID, persistent, caption, props);
    }

    protected LP addCUProp(AbstractGroup group, String sID, String caption, LP... props) {
        return addCUProp(group, sID, false, caption, props);
    }

    protected LP addCUProp(AbstractGroup group, String sID, boolean persistent, String caption, LP... props) {
        assert baseLM.checkCUProps.add(props);
        return addXSUProp(group, sID, persistent, caption, props);
    }

    // разница

    protected LP addDUProp(LP prop1, LP prop2) {
        return addDUProp(baseLM.privateGroup, "sys", prop1, prop2);
    }

    protected LP addDUProp(String caption, LP prop1, LP prop2) {
        return addDUProp((AbstractGroup) null, caption, prop1, prop2);
    }

    protected LP addDUProp(AbstractGroup group, String caption, LP prop1, LP prop2) {
        return addDUProp(group, genSID(), caption, prop1, prop2);
    }

    protected LP addDUProp(String sID, String caption, LP prop1, LP prop2) {
        return addDUProp(null, sID, caption, prop1, prop2);
    }

    protected LP addDUProp(String sID, boolean persistent, String caption, LP prop1, LP prop2) {
        return addDUProp(null, sID, persistent, caption, prop1, prop2);
    }

    protected LP addDUProp(AbstractGroup group, String sID, String caption, LP prop1, LP prop2) {
        return addDUProp(group, sID, false, caption, prop1, prop2);
    }

    protected LP addDUProp(AbstractGroup group, String sID, boolean persistent, String caption, LP prop1, LP prop2) {
        int intNum = prop1.listInterfaces.size();
        Object[] params = new Object[2 * (2 + intNum)];
        params[0] = 1;
        params[1] = prop1;
        for (int i = 0; i < intNum; i++)
            params[2 + i] = i + 1;
        params[2 + intNum] = -1;
        params[3 + intNum] = prop2;
        for (int i = 0; i < intNum; i++)
            params[4 + intNum + i] = i + 1;
        return addUProp(group, sID, persistent, caption, Union.SUM, params);
    }

    protected LP addNUProp(LP prop) {
        return addNUProp(baseLM.privateGroup, genSID(), "sys", prop);
    }

    protected LP addNUProp(AbstractGroup group, String sID, String caption, LP prop) {
        return addNUProp(group, sID, false, caption, prop);
    }

    protected LP addNUProp(AbstractGroup group, String sID, boolean persistent, String caption, LP prop) {
        int intNum = prop.listInterfaces.size();
        Object[] params = new Object[2 + intNum];
        params[0] = -1;
        params[1] = prop;
        for (int i = 0; i < intNum; i++)
            params[2 + i] = i + 1;
        return addUProp(group, sID, persistent, caption, Union.SUM, params);
    }

    protected LP addLProp(LP lp, ValueClass... classes) {
        return addDCProp("LG_" + lp.property.sID, "Лог " + lp.property, baseLM.object1, BaseUtils.add(BaseUtils.add(directLI(lp), new Object[]{addJProp(baseLM.equals2, 1, baseLM.currentSession), lp.listInterfaces.size() + 1}), classes));
    }

    // XOR

    protected LP addXorUProp(LP prop1, LP prop2) {
        return addXorUProp(baseLM.privateGroup, genSID(), "sys", prop1, prop2);
    }

    protected LP addXorUProp(AbstractGroup group, String sID, String caption, LP prop1, LP prop2) {
        return addXorUProp(group, sID, false, caption, prop1, prop2);
    }

    protected LP addXorUProp(AbstractGroup group, String sID, boolean persistent, String caption, LP... props) {
        return addUProp(group, sID, persistent, caption, Union.XOR, getUParams(props, 0));
//        int intNum = prop1.listInterfaces.size();
//        Object[] params = new Object[2 * (1 + intNum)];
//        params[0] = prop1;
//        for (int i = 0; i < intNum; i++)
//            params[1 + i] = i + 1;
//        params[1 + intNum] = prop2;
//        for (int i = 0; i < intNum; i++)
//            params[2 + intNum + i] = i + 1;
//        return addXSUProp(group, sID, persistent, caption, addJProp(andNot1, getUParams(new LP[]{prop1, prop2}, 0)), addJProp(andNot1, getUParams(new LP[]{prop2, prop1}, 0)));
    }

    // IF и IF ELSE

    protected LP addIfProp(LP prop, boolean not, LP ifProp, Object... params) {
        return addIfProp(baseLM.privateGroup, genSID(), "sys", prop, not, ifProp, params);
    }

    protected LP addIfProp(AbstractGroup group, String sID, String caption, LP prop, boolean not, LP ifProp, Object... params) {
        return addIfProp(group, sID, false, caption, prop, not, ifProp, params);
    }

    protected LP addIfProp(AbstractGroup group, String sID, boolean persistent, String caption, LP prop, boolean not, LP ifProp, Object... params) {
        return addJProp(group, sID, persistent, caption, and(not), BaseUtils.add(getUParams(new LP[]{prop}, 0), BaseUtils.add(new LP[]{ifProp}, params)));
    }

    protected LP addIfElseUProp(LP prop1, LP prop2, LP ifProp, Object... params) {
        return addIfElseUProp(baseLM.privateGroup, "sys", prop1, prop2, ifProp, params);
    }

    protected LP addIfElseUProp(AbstractGroup group, String caption, LP prop1, LP prop2, LP ifProp, Object... params) {
        return addIfElseUProp(group, genSID(), caption, prop1, prop2, ifProp, params);
    }

    protected LP addIfElseUProp(AbstractGroup group, String sID, String caption, LP prop1, LP prop2, LP ifProp, Object... params) {
        return addIfElseUProp(group, sID, false, caption, prop1, prop2, ifProp, params);
    }

    protected LP addIfElseUProp(AbstractGroup group, String sID, boolean persistent, String caption, LP prop1, LP prop2, LP ifProp, Object... params) {
        return addXSUProp(group, sID, persistent, caption, addIfProp(prop1, false, ifProp, params), addIfProp(prop2, true, ifProp, params));
    }

    // объединение пересекающихся свойств

    protected LP addSUProp(Union unionType, LP... props) {
        return addSUProp(baseLM.privateGroup, "sys", unionType, props);
    }

    protected LP addSUProp(String caption, Union unionType, LP... props) {
        return addSUProp((AbstractGroup) null, caption, unionType, props);
    }

    protected LP addSUProp(AbstractGroup group, String caption, Union unionType, LP... props) {
        return addSUProp(group, genSID(), caption, unionType, props);
    }

    protected LP addSUProp(String sID, String caption, Union unionType, LP... props) {
        return addSUProp(sID, false, caption, unionType, props);
    }

    protected LP addSUProp(String sID, boolean persistent, String caption, Union unionType, LP... props) {
        return addSUProp(null, sID, persistent, caption, unionType, props);
    }

    // объединяет разные по классам св-ва

    protected LP addSUProp(AbstractGroup group, String sID, String caption, Union unionType, LP... props) {
        return addSUProp(group, sID, false, caption, unionType, props);
    }

    protected LP addSUProp(AbstractGroup group, String sID, boolean persistent, String caption, Union unionType, LP... props) {
        assert baseLM.checkSUProps.add(props);
        return addUProp(group, sID, persistent, caption, unionType, getUParams(props, (unionType == Union.SUM ? 1 : 0)));
    }

    protected LP addXSUProp(AbstractGroup group, String caption, LP... props) {
        return addXSUProp(group, genSID(), caption, props);
    }

    // объединяет заведомо непересекающиеся но не классовые свойства

    protected LP addXSUProp(AbstractGroup group, String sID, String caption, LP... props) {
        return addXSUProp(group, sID, false, caption, props);
    }

    protected LP addXSUProp(AbstractGroup group, String sID, boolean persistent, String caption, LP... props) {
        return addUProp(group, sID, persistent, caption, Union.EXCLUSIVE, getUParams(props, 0));
    }

    protected LP[] addMUProp(AbstractGroup group, String[] ids, String[] captions, int extra, LP... props) {
        int propNum = props.length / (1 + extra);
        LP[] maxProps = Arrays.copyOfRange(props, 0, propNum);

        LP[] result = new LP[extra + 1];
        int i = 0;
        do {
            result[i] = addUProp(group, ids[i], captions[i], Union.MAX, getUParams(maxProps, 0));
            if (i < extra) { // если не последняя
                for (int j = 0; j < propNum; j++)
                    maxProps[j] = addJProp(baseLM.and1, BaseUtils.add(directLI(props[(i + 1) * propNum + j]), directLI( // само свойство
                            addJProp(baseLM.equals2, BaseUtils.add(directLI(maxProps[j]), directLI(result[i])))))); // только те кто дает предыдущий максимум
            }
        } while (i++ < extra);
        return result;
    }

    protected LP addAProp(ActionProperty property) {
        return addAProp(baseLM.actionGroup, property);
    }

    protected LP addAProp(AbstractGroup group, ActionProperty property) {
        return addProperty(group, new LP<ClassPropertyInterface>(property));
    }

    protected LP addBAProp(ConcreteCustomClass customClass, LP add) {
        return addAProp(baseLM.new AddBarcodeActionProperty(customClass, add.property, genSID()));
    }

    protected LP addSAProp(LP lp) {
        return addSAProp(baseLM.privateGroup, "sys", lp);
    }

    protected LP addSAProp(AbstractGroup group, String caption, LP lp) {
        return addProperty(group, new LP<ClassPropertyInterface>(new BaseLogicsModule.SeekActionProperty(genSID(), caption, new ValueClass[]{baseLM.baseClass}, lp == null ? null : lp.property)));
    }

    protected LP addMAProp(String message, String caption) {
        return addMAProp(message, null, caption);
    }

    protected LP addMAProp(String message, AbstractGroup group, String caption) {
        return addProperty(group, new LP<ClassPropertyInterface>(new BaseLogicsModule.MessageActionProperty(message, genSID(), caption)));
    }

    /**
     * Добавляет action для запуска свойств с мэппингом по порядку, т.е. на входы и выход каждого свойства мэппятся интерфейсы результирующего по порядку
     */
    protected LP addEPAProp(LP... lps) {
        return addEPAProp(false, lps);
    }

    /**
     * Добавляет action для запуска свойств с мэппингом по порядку, т.е. на входы и выход каждого свойства мэппятся интерфейсы результирующего по порядку
     * @param writeDefaultValues Если == true, то мэппятся только входы, без выхода
     */
    protected LP addEPAProp(boolean writeDefaultValues, LP... lps) {
        int[][] mapInterfaces = new int[lps.length][];
        for (int i = 0; i < lps.length; ++i) {
            LP lp = lps[i];
            mapInterfaces[i] = consecutiveInts(lp.listInterfaces.size() + (writeDefaultValues ? 0 : 1));
        }

        return addEPAProp(writeDefaultValues, lps, mapInterfaces);
    }

    /**
     * Добавляет action для запуска других свойств.
     *
     * Мэппиг задаётся перечислением свойств с указанием после каждого номеров интерфейсов результирующего свойства,
     * которые пойдут на входы и выход данных свойств
     * Пример 1: addEPAProp(true, userLogin, 1, inUserRole, 1, 2)
     * Пример 2: addEPAProp(false, userLogin, 1, 3, inUserRole, 1, 2, 4)
     *
     * @param writeDefaultValues использовать ли значения по умолчанию для записи в свойства.
     * Если значение этого параметра false, то мэпиться должны не только выходы, но и вход, номер интерфейса, который пойдёт на вход, должен быть указан последним
     */
    protected LP addEPAProp(boolean writeDefaultValues, Object... params) {
        List<LP> lps = new ArrayList<LP>();
        List<int[]> mapInterfaces = new ArrayList<int[]>();

        int pi = 0;
        while (pi < params.length) {
            assert params[pi] instanceof LP;

            LP lp = (LP) params[pi++];

            int[] propMapInterfaces = new int[lp.listInterfaces.size() + (writeDefaultValues ? 0 : 1)];
            for (int j = 0; j < propMapInterfaces.length; ++j) {
                propMapInterfaces[j] = (Integer) params[pi++] - 1;
            }

            lps.add(lp);
            mapInterfaces.add(propMapInterfaces);
        }
        return addEPAProp(writeDefaultValues, lps.toArray(new LP[lps.size()]), mapInterfaces.toArray(new int[mapInterfaces.size()][]));
    }

    private LP addEPAProp(boolean writeDefaultValues, LP[] lps, int[][] mapInterfaces) {
        return addAProp(new ExecutePropertiesActionProperty(genSID(), "sys", writeDefaultValues, lps, mapInterfaces));
    }

    protected LP addLFAProp(AbstractGroup group, String caption, LP lp) {
        return addProperty(group, new LP<ClassPropertyInterface>(new BaseLogicsModule.LoadActionProperty(genSID(), caption, lp)));
    }

    protected LP addOFAProp(AbstractGroup group, String caption, LP lp) { // обернем сразу в and
        LP<ClassPropertyInterface> openAction = new LP<ClassPropertyInterface>(new BaseLogicsModule.OpenActionProperty(genSID(), caption, lp));
        return addJProp(group, caption, baseLM.and1, getUParams(new LP[]{openAction, lp}, 0));
    }


    // params - по каким входам группировать
    protected LP addIAProp(LP dataProperty, Integer... params) {
        return addAProp(new BaseLogicsModule.IncrementActionProperty(genSID(), "sys", dataProperty,
                addMGProp(dataProperty, params),
                params));
    }

    protected LP addAAProp(CustomClass customClass, LP... properties) {
        return addAAProp(customClass, null, null, false, properties);
    }

    /** Пример использования:
    fileActPricat = addAAProp(pricat, filePricat.property, FileActionClass.getCustomInstance(true));
    pricat - добавляемый класс
    filePricat.property - свойство, которое изменяется
    FileActionClass.getCustomInstance(true) - класс
     неявный assertion, что класс свойства должен быть совместим с классом Action
     */
    protected LP addAAProp(ValueClass cls, Property propertyValue, DataClass dataClass) {
        return addAProp(new AddObjectActionProperty(genSID(), (CustomClass) cls, propertyValue, dataClass));
    }

    protected LP addAAProp(CustomClass customClass, LP barcode, LP barcodePrefix, boolean quantity, LP... properties) {
        return addAProp(new AddObjectActionProperty(genSID(),
                (barcode != null) ? barcode.property : null, (barcodePrefix != null) ? barcodePrefix.property : null,
                quantity, customClass, LP.toPropertyArray(properties), null, null));
    }

    @IdentityLazy
    protected LP getAddObjectAction(ValueClass cls) {
        return addAProp(new AddObjectActionProperty(genSID(), (CustomClass) cls));
    }

    @IdentityLazy
    protected LP getAddObjectActionWithClassCheck(ValueClass baseClass, ValueClass checkClass) {
        LP addObjectAction = getAddObjectAction(baseClass);
        return addJProp(addObjectAction.property.caption, baseLM.and1, addObjectAction, is(checkClass), 1);
    }

    @IdentityLazy
    protected LP getImportObjectAction(ValueClass cls) {
        return addAProp(new ImportFromExcelActionProperty(genSID(), (CustomClass) cls));
    }


    /**
     * Нужно для скрытия свойств при соблюдении какого-то критерия
     * <p/>
     * <pre>
     * Пример использования:
     *       Скроем свойство policyDescription, если у текущего user'а логин - "Admin"
     * <p/>
     *       Вводим свойство критерия:
     * <p/>
     *         LP hideUserPolicyDescription = addJProp(diff2, userLogin, 1, addCProp(StringClass.get(30), "Admin"));
     * <p/>
     *       Вводим свойство которое будет использовано в качестве propertyCaption для policyDescription:
     * <p/>
     *         policyDescriptorCaption = addHideCaptionProp(null, "Policy caption", policyDescription, hideUserPolicyDescription);
     * <p/>
     *       Далее в форме указываем соответсвующий propertyCaption:
     * <p/>
     *         PropertyDrawEntity descriptionDraw = getPropertyDraw(policyDescription, objPolicy.groupTo);
     *         PropertyDrawEntity descriptorCaptionDraw = addPropertyDraw(policyDescriptorCaption, objUser);
     *         descriptionDraw.setPropertyCaption(descriptorCaptionDraw.propertyObject);
     * </pre>
     *
     * @param group        ...
     * @param caption      ...
     * @param original     свойство, к которому будет применятся критерий сокрытия
     * @param hideProperty критерий
     * @return свойство, которое должно использоваться в качестве propertyCaption для скрываемого свойства
     */
    protected LP addHideCaptionProp(AbstractGroup group, String caption, LP original, LP hideProperty) {
        LP originalCaption = addCProp(StringClass.get(100), original.property.caption);
        LP result = addJProp(group, caption, baseLM.and1, BaseUtils.add(new Object[]{originalCaption}, directLI(hideProperty)));
        return result;
    }

    protected LP addProp(Property<? extends PropertyInterface> prop) {
        return addProp(null, prop);
    }

    public LP addProp(AbstractGroup group, Property<? extends PropertyInterface> prop) {
        return addProperty(group, new LP(prop));
    }

    protected <T extends LP<?>> T addProperty(AbstractGroup group, T lp) {
        return addProperty(group, false, lp);
    }

    private <T extends LP<?>> T addProperty(AbstractGroup group, boolean persistent, T lp) {
        baseLM.registerProperty(lp);
        if (group != null) {
            group.add(lp.property);
        } else {
            baseLM.privateGroup.add(lp.property);
        }
        if (persistent) {
            addPersistent(lp);
        }
        return lp;
    }

    public void addIndex(LP<?>... lps) {
        baseLM.addIndex(lps);
    }

    private void addPersistent(AggregateProperty property) {
        assert !baseLM.idSet.contains(property.sID);
        property.stored = true;

        baseLM.logger.debug("Initializing stored property...");
        property.markStored(baseLM.tableFactory);
    }

    protected void addPersistent(LP lp) {
        addPersistent((AggregateProperty) lp.property);
    }

    protected void addConstraint(LP<?> lp, boolean checkChange) {
        lp.property.setConstraint(checkChange);
    }

    protected <L extends PropertyInterface, T extends PropertyInterface> void follows(LP<T> first, LP<L> second, int... mapping) {
        Map<L, T> mapInterfaces = new HashMap<L, T>();
        for (int i = 0; i < second.listInterfaces.size(); i++) {
            mapInterfaces.put(second.listInterfaces.get(i), first.listInterfaces.get(mapping[i] - 1));
        }
        addProp(first.property.addFollows(new PropertyMapImplement<L, T>(second.property, mapInterfaces)));
    }

    protected void followed(LP first, LP... lps) {
        for (LP lp : lps) {
            int[] mapping = new int[lp.listInterfaces.size()];
            for (int i = 0; i < mapping.length; i++) {
                mapping[i] = i + 1;
            }
            follows(lp, first, mapping);
        }
    }

    protected void setNotNull(LP property) {

        ValueClass[] values = property.getMapClasses();

        LP checkProp = addCProp(LogicalClass.instance, true, values);

        Map mapInterfaces = new HashMap();
        for (int i = 0; i < property.listInterfaces.size(); i++) {
            mapInterfaces.put(property.listInterfaces.get(i), checkProp.listInterfaces.get(i));
        }
        addProp(checkProp.property.addFollows(new PropertyMapImplement(property.property, mapInterfaces), "Свойство " + property.property.sID + " не задано", PropertyFollows.RESOLVE_FALSE));
    }

    // получает свойство is
    // для множества классов есть CProp
    protected LP is(ValueClass valueClass) {
        LP isProp = baseLM.is.get(valueClass);
        if (isProp == null) {
            isProp = addCProp(valueClass.toString() + "(пр.)", LogicalClass.instance, true, valueClass);
            baseLM.is.put(valueClass, isProp);
        }
        return isProp;
    }

    public LP object(ValueClass valueClass) {
        LP objectProp = baseLM.object.get(valueClass);
        if (objectProp == null) {
            objectProp = addJProp(valueClass.toString(), baseLM.and1, 1, is(valueClass), 1);
            baseLM.object.put(valueClass, objectProp);
        }
        return objectProp;
    }

    protected LP and(boolean... nots) {
        return addAFProp(nots);
    }

    @IdentityLazy
    protected LP dumb(int interfaces) {
        ValueClass params[] = new ValueClass[interfaces];
        for (int i = 0; i < interfaces; ++i) {
            params[i] = baseLM.baseClass;
        }
        return addCProp("dumbProperty" + interfaces, StringClass.get(0), "", params);
    }

    protected <T extends FormEntity> T addFormEntity(T form) {
        form.richDesign = form.createDefaultRichDesign();
        return form;
    }

    public void addObjectActions(FormEntity form, ObjectEntity object) {
        addObjectActions(form, object, false);
    }

    protected void addObjectActions(FormEntity form, ObjectEntity object, ObjectEntity checkObject) {
        addObjectActions(form, object, checkObject, null);
    }

    protected void addObjectActions(FormEntity form, ObjectEntity object, ObjectEntity checkObject, ValueClass checkObjectClass) {
        addObjectActions(form, object, false, checkObject, checkObjectClass);
    }

    public void addObjectActions(FormEntity form, ObjectEntity object, boolean actionImport) {
        addObjectActions(form, object, actionImport, null, null);
    }

    protected void addObjectActions(FormEntity form, ObjectEntity object, boolean actionImport, ObjectEntity checkObject, ValueClass checkObjectClass) {
        form.addPropertyDraw(baseLM.delete, object);
        if (actionImport)
            form.forceDefaultDraw.put(form.addPropertyDraw(getImportObjectAction(object.baseClass)), object.groupTo);

        PropertyDrawEntity actionAddPropertyDraw;
        if (checkObject == null) {
            actionAddPropertyDraw = form.addPropertyDraw(getAddObjectAction(object.baseClass));
        } else {
            actionAddPropertyDraw = form.addPropertyDraw(
                    getAddObjectActionWithClassCheck(object.baseClass, checkObjectClass != null ? checkObjectClass : checkObject.baseClass),
                    checkObject);

            actionAddPropertyDraw.shouldBeLast = true;
            actionAddPropertyDraw.forceViewType = ClassViewType.PANEL;
        }
        form.forceDefaultDraw.put(actionAddPropertyDraw, object.groupTo);
    }

}
