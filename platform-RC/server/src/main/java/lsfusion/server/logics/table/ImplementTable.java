package lsfusion.server.logics.table;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.ProgressBar;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;
import lsfusion.interop.Compare;
import lsfusion.server.SystemProperties;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.classes.*;
import lsfusion.server.data.*;
import lsfusion.server.data.expr.*;
import lsfusion.server.data.expr.query.*;
import lsfusion.server.data.query.IQuery;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.query.stat.TableStatKeys;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.ReflectionLogicsModule;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.mutables.NFFact;
import lsfusion.server.logics.mutables.NFLazy;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.mutables.interfaces.NFOrderSet;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.IsClassField;
import lsfusion.server.logics.property.ObjectClassField;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.Modifier;
import lsfusion.server.session.PropertyChanges;
import lsfusion.server.stack.StackProgress;

import java.sql.SQLException;
import java.util.*;

public class ImplementTable extends GlobalTable { // последний интерфейс assert что isFull
    private static double topCoefficient = 0.8;

    private final ImMap<KeyField, ValueClass> mapFields;

    public ImMap<KeyField, ValueClass> getMapFields() {
        return mapFields;
    }

    // для обеспечения детерминированности mapping'a (связано с CalcProperty.getOrderTableInterfaceClasses)
    private final ImOrderMap<KeyField, ValueClass> orderMapFields;
    public ImOrderMap<KeyField, ValueClass> getOrderMapFields() {
        return orderMapFields;
    }

    private TableStatKeys statKeys = null;
    private ImMap<PropertyField, PropStat> statProps = null;
    private ImSet<PropertyField> indexedProps = SetFact.<PropertyField>EMPTY();
    private ImSet<ImOrderSet<Field>> indexes = SetFact.EMPTY();

    public boolean markedFull;

    private IsClassField fullField = null; // поле которое всегда не null, и свойство которого обеспечивает , возможно временно потом совместиться с логикой classExpr
    @Override
    public boolean isFull() {
        return fullField != null;
    }
    public IsClassField getFullField() {
        return fullField;
    }
    private void setFullField(IsClassField field) {
        fullField = field;

        ValueClass fieldClass;
        ImMap<KeyField, ValueClass> mapFields = getMapFields();
        if(mapFields.size() == 1 && (fieldClass = mapFields.singleValue()) instanceof CustomClass) {
            ((CustomClass)fieldClass).setIsClassField(field);
        }
    }
    public void setFullField(final PropertyField field) {
        setFullField(new IsClassField() {
            public PropertyField getField() {
                return field;
            }
            public BaseExpr getFollowExpr(BaseExpr joinExpr) {
                return (BaseExpr) joinAnd(MapFact.singleton(keys.single(), joinExpr)).getExpr(field);
            }
            public Where getIsClassWhere(SingleClassExpr expr, ObjectValueClassSet set, boolean inconsistent) {
                assert isFull();
                assert !inconsistent;
                assert getClasses().getCommonClass(keys.single()).containsAll(set, false) && set.containsAll(getClasses().getCommonClass(keys.single()), false);
                return joinAnd(MapFact.singleton(keys.single(), expr)).getWhere();
            }
        });
    }
    public void setFullField(ObjectClassField isClassField) {
        setFullField((IsClassField) isClassField);
    }

    @Override
    protected boolean isIndexed(PropertyField field) {
        return indexedProps.contains(field);
    }

    public ImplementTable(String name, final ValueClass... implementClasses) {
        super(name);

        ImOrderSet<KeyField> keys;
        keys = SetFact.toOrderExclSet(implementClasses.length, new GetIndex<KeyField>() {
            public KeyField getMapValue(int i) {
                return new KeyField("key"+i,implementClasses[i].getType());
            }});
        ImMap<KeyField, ValueClass> mapFields;
        mapFields = keys.mapOrderValues(new GetIndex<ValueClass>() {
            public ValueClass getMapValue(int i) {
                return implementClasses[i];
            }});

        parents = NFFact.orderSet();
        classes = classes.or(new ClassWhere<>(mapFields, true));
        this.keys = keys;
        this.mapFields = mapFields;
        orderMapFields = keys.mapOrderMap(mapFields);
    }

    public <P extends PropertyInterface> IQuery<KeyField, CalcProperty> getReadSaveQuery(ImSet<CalcProperty> properties, Modifier modifier) {
        return getReadSaveQuery(properties, modifier.getPropertyChanges());
    }

    public <P extends PropertyInterface> IQuery<KeyField, CalcProperty> getReadSaveQuery(ImSet<CalcProperty> properties, PropertyChanges propertyChanges) {
        QueryBuilder<KeyField, CalcProperty> changesQuery = new QueryBuilder<>(this);
        WhereBuilder changedWhere = new WhereBuilder();
        for (CalcProperty<P> property : properties)
            changesQuery.addProperty(property, property.getIncrementExpr(property.mapTable.mapKeys.join(changesQuery.getMapExprs()), propertyChanges, changedWhere));
        changesQuery.and(changedWhere.toWhere());
        return changesQuery.getQuery();
    }

    public void moveColumn(SQLSession sql, PropertyField field, Table prevTable, ImMap<KeyField, KeyField> mapFields, PropertyField prevField) throws SQLException, SQLHandledException {
        QueryBuilder<KeyField, PropertyField> moveColumn = new QueryBuilder<>(this);
        Expr moveExpr = prevTable.join(mapFields.join(moveColumn.getMapExprs())).getExpr(prevField);
        moveColumn.addProperty(field, moveExpr);
        moveColumn.and(moveExpr.getWhere());
        sql.modifyRecords(new ModifyQuery(this, moveColumn.getQuery(), OperationOwner.unknown, TableOwner.global));
    }

    @NFLazy
    public void addField(PropertyField field,ClassWhere<Field> classes) { // кривовато конечно, но пока другого варианта нет
        properties = properties.addExcl(field);
        propertyClasses = propertyClasses.addExcl(field, classes);
    }

    @IdentityLazy
    protected ImSet<ImOrderSet<Field>> getIndexes() {
        return super.getIndexes().addExcl(indexes);
    }

    @NFLazy
    public void addIndex(ImOrderSet<Field> index) { // кривовато конечно, но пока другого варианта нет
        indexes = indexes.addExcl(index);

        Field field = index.get(0);
        if(field instanceof PropertyField && !indexedProps.contains((PropertyField) field)) // временно
            indexedProps = indexedProps.addExcl((PropertyField) field);
    }

    private NFOrderSet<ImplementTable> parents;
    public Iterable<ImplementTable> getParentsIt() {
        return parents.getIt();
    }
    public Iterable<ImplementTable> getParentsListIt() {
        return parents.getListIt();
    }

    // operation на что сравниваем
    // 0 - не ToParent
    // 1 - ToParent
    // 2 - равно
    private final static int IS_CHILD = 0;
    private final static int IS_PARENT = 1;
    private final static int IS_EQUAL = 2;

    private <T> boolean recCompare(int operation, ImOrderMap<T, ValueClass> toCompare,int iRec,Map<T,KeyField> mapTo) {
        ImOrderMap<KeyField, ValueClass> orderMapFields = getOrderMapFields();
        if(iRec>=orderMapFields.size()) return true;

        KeyField proceedItem = orderMapFields.getKey(iRec);
        ValueClass proceedClass = orderMapFields.getValue(iRec);
        for(int i=0,size=toCompare.size();i<size;i++) {
            T key = toCompare.getKey(i); ValueClass compareClass = toCompare.getValue(i);
            if(!mapTo.containsKey(key) &&
               ((operation==IS_PARENT && compareClass.isCompatibleParent(proceedClass)) ||
               (operation==IS_CHILD && proceedClass.isCompatibleParent(compareClass)) ||
               (operation==IS_EQUAL && compareClass == proceedClass))) {
                    // если parent - есть связь и нету ключа, гоним рекурсию дальше
                    mapTo.put(key,proceedItem);
                    // если нашли карту выходим
                    if(recCompare(operation, toCompare,iRec + 1, mapTo)) return true;
                    mapTo.remove(key);
            }
        }

        return false;
    }

    private final static int COMPARE_DIFF = 0;
    private final static int COMPARE_DOWN = 1;
    private final static int COMPARE_UP = 2;
    private final static int COMPARE_EQUAL = 3;

    // также возвращает карту если не Diff
    private <T> int compare(ImOrderMap<T, ValueClass> toCompare,Result<ImRevMap<T,KeyField>> mapTo) {

        Integer result = null;
        
        Map<T, KeyField> mMapTo = MapFact.mAddRemoveMap();
        if(recCompare(IS_EQUAL,toCompare,0,mMapTo))
            result = COMPARE_EQUAL;
        else
        if(recCompare(IS_CHILD,toCompare,0,mMapTo))
            result = COMPARE_UP;
        else
        if(recCompare(IS_PARENT,toCompare,0,mMapTo))
            result = COMPARE_DOWN;

        if(result!=null) {
            mapTo.set(MapFact.fromJavaRevMap(mMapTo));
            return result;
        }

        return COMPARE_DIFF;
    }

    public <T> boolean equalClasses(ImOrderMap<T, ValueClass> mapClasses) {
        int compResult = compare(mapClasses, new Result<ImRevMap<T, KeyField>>());
        return compResult == COMPARE_EQUAL || compResult == COMPARE_UP;
    }

    public void include(NFOrderSet<ImplementTable> tables, Version version, boolean toAdd, Set<ImplementTable> checks, ImplementTable debugItem) {
        ImList<ImplementTable> current = tables.getNFList(Version.CURRENT);
        
        Iterator<ImplementTable> i = current.iterator();
        boolean wasRemove = false; // для assertiona
        while(i.hasNext()) {
            ImplementTable item = i.next();
            int relation = item.compare(getOrderMapFields(),new Result<ImRevMap<KeyField, KeyField>>());
            if(relation==COMPARE_DOWN) { // снизу в дереве, добавляем ее как промежуточную
                if(checkSiblings(item, parents, this))
                    parents.add(item, version);
                
                if(toAdd) {
                    wasRemove = true;
                    tables.remove(item, Version.CURRENT); // последняя версия нужна, так как в противном случае удаление может пойти до добавления 
                }
            } else { // сверху в дереве или никак не связаны, передаем дальше
                if(!checks.contains(item)) { // для детерменированности эту проверку придется убрать 
                    checks.add(item);
                    include(item.parents, version, relation==COMPARE_UP,checks, item);
                }
                if(relation==COMPARE_UP) {
                    assert !wasRemove; // так как не может быть одновременно down и up
                    toAdd = false;
                }
            }
        }

        // если снизу добавляем Childs
        if(toAdd) {
            assert checkSiblings(this, tables, debugItem);
            tables.add(this, version);
        }
    }

    private boolean checkSiblings(ImplementTable item, NFOrderSet<ImplementTable> tables, ImplementTable debugItem) {
        for(ImplementTable siblingTable : tables.getNFList(Version.CURRENT)) {
            if(BaseUtils.hashEquals(item, siblingTable))
                return false;
            int compare = siblingTable.compare(item.getOrderMapFields(), new Result<ImRevMap<KeyField, KeyField>>());
            if(compare==COMPARE_UP || compare == COMPARE_DOWN)
                return false;
        }
        return true;
    }

    private interface MapTableType {
        boolean skipParents(ImplementTable table);
        boolean skipResult(ImplementTable table);
        boolean onlyFirstParent(ImplementTable table);
    }

    // поиск таблицы для классов
    private final static MapTableType findTable = new MapTableType() {
        public boolean skipParents(ImplementTable table) {
            return false;
        }

        public boolean skipResult(ImplementTable table) {
            return false;
        }

        public boolean onlyFirstParent(ImplementTable table) {
            return true;
        }
    };

    private final static MapTableType findClassTable = new MapTableType() {
        public boolean skipParents(ImplementTable table) {
            return !skipResult(table);
        }

        public boolean skipResult(ImplementTable table) {
            return !table.markedFull;
        }

        public boolean onlyFirstParent(ImplementTable table) {
            return true;
        }
    };

    // поиск сгенерированной таблицы
    private final static MapTableType findIncludedTable = new MapTableType() {
        public boolean skipParents(ImplementTable table) {
            return true;
        }

        public boolean skipResult(ImplementTable table) {
            return false;
        }

        public boolean onlyFirstParent(ImplementTable table) {
            throw new UnsupportedOperationException();
        }
    };

    // поиск full таблиц
    private final static class FindFullTables implements MapTableType {

        private final ImplementTable skipTable;

        public FindFullTables(ImplementTable skipTable) {
            this.skipTable = skipTable;
        }

        public boolean skipParents(ImplementTable table) {
            return skipTable(table);
        }

        private boolean skipTable(ImplementTable table) {
            return skipTable != null && BaseUtils.hashEquals(table, skipTable);
        }

        public boolean skipResult(ImplementTable table) {
            return !table.isFull() || skipTable(table);
        }

        public boolean onlyFirstParent(ImplementTable table) {
            return false;
        }
    }

    public <T> MapKeysTable<T> getSingleMapTable(ImOrderMap<T, ValueClass> findItem, boolean included) {
        ImSet<MapKeysTable<T>> tables = getMapTables(findItem, included ? findIncludedTable : findTable);
        if(tables.isEmpty())
            return null;
        return tables.single();
    }

    public <T> MapKeysTable<T> getClassMapTable(ImOrderMap<T, ValueClass> findItem) {
        ImSet<MapKeysTable<T>> tables = getMapTables(findItem, findClassTable);
        if(tables.isEmpty())
            tables = getMapTables(findItem, findTable);
        if(tables.isEmpty())
            return null;
        return tables.single();
    }

    public <T> ImSet<MapKeysTable<T>> getFullMapTables(ImOrderMap<T, ValueClass> findItem, ImplementTable skipTable) {
        return getMapTables(findItem, new FindFullTables(skipTable));
    }

    public <T> ImSet<MapKeysTable<T>> getMapTables(ImOrderMap<T, ValueClass> findItem, MapTableType type) {
        Result<ImRevMap<T,KeyField>> mapCompare = new Result<>();
        int relation = compare(findItem,mapCompare);
        // если внизу или отличается то не туда явно зашли
        if(relation==COMPARE_DOWN || relation==COMPARE_DIFF) return SetFact.EMPTY();

        if(!type.skipParents(this)) {
            MSet<MapKeysTable<T>> mResult = SetFact.mSet();
            for(ImplementTable item : getParentsListIt()) {
                ImSet<MapKeysTable<T>> parentTables = item.getMapTables(findItem, type);
                if(type.onlyFirstParent(this) && !parentTables.isEmpty()) {
                    assert parentTables.size() == 1;
                    return parentTables;
                }
                mResult.addAll(parentTables);
            }
            ImSet<MapKeysTable<T>> result = mResult.immutable();
            if(!result.isEmpty())
                return result;
        }

        if(type.skipResult(this)) return SetFact.EMPTY();

        return SetFact.singleton(new MapKeysTable<>(this, mapCompare.result));
    }

    public <T> MapKeysTable<T> getMapKeysTable(ImOrderMap<T, ValueClass> classes) {
        Result<ImRevMap<T,KeyField>> mapCompare = new Result<>();
        int relation = compare(classes, mapCompare);
        if(relation==COMPARE_DOWN || relation==COMPARE_DIFF)
            return null;
        return new MapKeysTable<>(this, mapCompare.result);
    }

    void fillSet(MSet<ImplementTable> tableImplements) {
        if(tableImplements.add(this)) return;
        for(ImplementTable parent : getParentsIt()) 
            parent.fillSet(tableImplements);
    }

    public TableStatKeys getTableStatKeys() {
        if(statKeys!=null)
            return statKeys;
        else
            return SerializedTable.getStatKeys(this);
    }

    public ImMap<PropertyField,PropStat> getStatProps() {
        if(statProps!=null)
            return statProps;
        else
            return SerializedTable.getStatProps(this);
    }

    private Object readCount(DataSession session, Where where) throws SQLException, SQLHandledException {
        return readCount(session, where, 0, false);
    }

    private Object readCount(DataSession session, Where where, int total, boolean useCoefficient) throws SQLException, SQLHandledException {
        QueryBuilder<Object, Object> query = new QueryBuilder<>(SetFact.EMPTY());
        ValueExpr one = new ValueExpr(1, IntegerClass.instance);
        query.addProperty("count", GroupExpr.create(MapFact.<Integer, Expr>EMPTY(), one,
                where, GroupType.SUM, MapFact.<Integer, Expr>EMPTY()));
        Integer count = (Integer) query.execute(session).singleValue().singleValue();
        return count == null ? Math.min(total, 1) : (useCoefficient ? (int) Math.min(total, (count / topCoefficient) + 1) : count);
    }

    private DataObject safeReadClasses(DataSession session, LCP lcp, DataObject... objects) throws SQLException, SQLHandledException {
        ObjectValue value = lcp.readClasses(session, objects);
        if(value instanceof DataObject)
            return (DataObject) value;
//        ServerLoggers.assertLog(false, "SHOULD BE SYNCHRONIZED : " + lcp + ", keys : " + Arrays.toString(objects));
        return null;
    }

    public void calculateStat(ReflectionLogicsModule reflectionLM, DataSession session) throws SQLException, SQLHandledException {
        calculateStat(reflectionLM, session, null, false);
        calculateStat(reflectionLM, session, null, true);
    }

    public ImMap<String, Pair<Integer, Integer>> calculateStat(ReflectionLogicsModule reflectionLM, DataSession session, ImMap<PropertyField, String> props, boolean top) throws SQLException, SQLHandledException {
        ImMap<String, Pair<Integer, Integer>> propStats = MapFact.EMPTY();
        if (!SystemProperties.doNotCalculateStats) {
            ImRevMap<KeyField, KeyExpr> mapKeys = getMapKeys();
            lsfusion.server.data.query.Join<PropertyField> join = join(mapKeys);

            MExclMap<Object, Object> mResult = MapFact.mExclMap();
            MExclMap<Object, Object> mNotNulls = MapFact.mExclMap();

            Where inWhere = join.getWhere();
            KeyExpr countKeyExpr = new KeyExpr("count");

            Integer total = (Integer) readCount(session, inWhere);

            for (KeyField key : keys) {
                ImMap<Object, Expr> map = MapFact.<Object, Expr>singleton(0, mapKeys.get(key));
                mResult.exclAdd(key, readCount(session, getCountWhere(session.sql, GroupExpr.create(map, inWhere, map, true),
                            GroupExpr.create(map, inWhere, map, false), mapKeys.get(key), total, top && keys.size() > 1), total, top && keys.size() > 1));
            }

            ImSet<PropertyField> propertyFieldSet = props == null ? properties : props.keys();

            for (PropertyField prop : propertyFieldSet) {
                Integer notNullCount = (Integer) readCount(session, join.getExpr(prop).getWhere());
                mNotNulls.exclAdd(prop, notNullCount);

                if (props != null ? props.containsKey(prop) : !(prop.type instanceof DataClass && !((DataClass) prop.type).calculateStat())) {
                    mResult.exclAdd(prop, readCount(session, getCountWhere(session.sql,
                            GroupExpr.create(MapFact.singleton(0, join.getExpr(prop)), Where.TRUE, MapFact.singleton(0, countKeyExpr), true),
                            GroupExpr.create(MapFact.singleton(0, join.getExpr(prop)), Where.TRUE, MapFact.singleton(0, countKeyExpr), false),
                            countKeyExpr, notNullCount, top), notNullCount, top));
                }
            }

            mResult.exclAdd(0, total);
            ImMap<Object, Object> result = mResult.immutable();

            DataObject tableObject = safeReadClasses(session, reflectionLM.tableSID, new DataObject(getName()));
            if(tableObject == null && getName() != null) {
                tableObject = session.addObject(reflectionLM.table);
                reflectionLM.sidTable.change(getName(), session, tableObject);
            }
            reflectionLM.rowsTable.change(BaseUtils.nvl(result.get(0), 0), session, (DataObject) tableObject);

            for (KeyField key : keys) {
                DataObject keyObject = safeReadClasses(session, reflectionLM.tableKeySID, new DataObject(getName() + "." + key.getName()));
                if (keyObject == null) {
                    keyObject = session.addObject(reflectionLM.tableKey);
                    reflectionLM.sidTableKey.change(getName() + "." + key.getName(), session, keyObject);
                }
                (top ? reflectionLM.quantityTopTableKey : reflectionLM.quantityTableKey).change(BaseUtils.nvl(result.get(key), 0), session, keyObject);
            }

            ImMap<Object, Object> notNulls = mNotNulls.immutable();

            for (PropertyField property : propertyFieldSet) {
                DataObject propertyObject = safeReadClasses(session, reflectionLM.propertyTableSID, new DataObject(getName()), new DataObject(property.getName()));
                if(propertyObject == null && props != null) {
                    String canonicalName = props.get(property);
                    propertyObject = safeReadClasses(session, reflectionLM.propertyCanonicalName, new DataObject(canonicalName));
                    if(propertyObject == null) {
                        propertyObject = session.addObject(reflectionLM.property);
                        reflectionLM.canonicalNameProperty.change(canonicalName, session, propertyObject);
                    }
                    reflectionLM.storedProperty.change(true, session, propertyObject);
                    reflectionLM.dbNameProperty.change(property.getName(), session, propertyObject);
                    reflectionLM.tableSIDProperty.change(getName(), session, propertyObject);
                }
                if (propertyObject != null) {
                    (top ? reflectionLM.quantityTopProperty : reflectionLM.quantityProperty).change(BaseUtils.nvl(result.get(property), 0), session, propertyObject);

                    Object notNull = BaseUtils.nvl(notNulls.get(property), 0);
                    Object quantity = BaseUtils.nvl(result.get(property), 0);
                    reflectionLM.notNullQuantityProperty.change(notNull, session, propertyObject);
                    propStats = propStats.addExcl(getName() + "." + property.getName(), Pair.create((Integer) quantity, (Integer) notNull));
                }
            }
        }
        return propStats;
    }

    private Where getCountWhere(SQLSession session, Expr quantityTopExpr, Expr quantityNotTopExpr, KeyExpr keyExpr, Integer total, boolean top) throws SQLException, SQLHandledException {
        if (top) {
            ImList<Expr> exprs = ListFact.singleton(quantityTopExpr);
            ImOrderMap<Expr, Boolean> orders = MapFact.toOrderMap(quantityTopExpr, true, keyExpr, false);
            ImSet<Expr> partitions = SetFact.EMPTY();
            ImMap<KeyExpr, KeyExpr> group = MapFact.singleton(keyExpr, keyExpr);
            Expr partitionExpr = PartitionExpr.create(PartitionType.SUM, exprs, orders, true, partitions, group);

            return partitionExpr.compare(new DataObject(Math.ceil((total == null ? 0 : total) * topCoefficient)), Compare.LESS_EQUALS);
        }
        else
            return quantityNotTopExpr.getWhere();
    }

    @StackProgress
    public boolean overCalculateStat(ReflectionLogicsModule reflectionLM, DataSession session, MSet<Integer> propertiesSet, @StackProgress ProgressBar progressBar) throws SQLException, SQLHandledException {
        boolean found = overCalculateStat(reflectionLM, session, propertiesSet, progressBar, false);
        overCalculateStat(reflectionLM, session, propertiesSet, progressBar, true);
        return found;
    }

    @StackProgress
    public boolean overCalculateStat(ReflectionLogicsModule reflectionLM, DataSession session, MSet<Integer> propertiesSet, @StackProgress ProgressBar progressBar, boolean top) throws SQLException, SQLHandledException {
        boolean found = false;
        if (!SystemProperties.doNotCalculateStats) {

            ImRevMap<KeyField, KeyExpr> mapKeys = getMapKeys();
            lsfusion.server.data.query.Join<PropertyField> join = join(mapKeys);

            MExclMap<Object, Object> mResult = MapFact.mExclMap();
            MExclMap<Object, Object> mNotNulls = MapFact.mExclMap();

            KeyExpr countKeyExpr = new KeyExpr("count");

            for(PropertyField prop : properties) {
                Integer notNullCount = (Integer) readCount(session, join.getExpr(prop).getWhere());
                mNotNulls.exclAdd(prop, notNullCount);

                if (!(prop.type instanceof DataClass && !((DataClass) prop.type).calculateStat())) {
                    mResult.exclAdd(prop, readCount(session, getCountWhere(session.sql,
                            GroupExpr.create(MapFact.singleton(0, join.getExpr(prop)), Where.TRUE, MapFact.singleton(0, countKeyExpr), true),
                            GroupExpr.create(MapFact.singleton(0, join.getExpr(prop)), Where.TRUE, MapFact.singleton(0, countKeyExpr), false),
                            countKeyExpr, notNullCount, top), notNullCount, top));
                }
            }
            ImMap<Object, Object> result = mResult.immutable();
            ImMap<Object, Object> notNulls = mNotNulls.immutable();

            for (PropertyField property : properties) {
                DataObject propertyObject = safeReadClasses(session, reflectionLM.propertyTableSID, new DataObject(getName()), new DataObject(property.getName()));

                if (propertyObject != null && propertiesSet.contains((Integer) propertyObject.getValue())) {
                    (top ? reflectionLM.quantityTopProperty : reflectionLM.quantityProperty).change(BaseUtils.nvl(result.get(property), 0), session, propertyObject);
                    reflectionLM.notNullQuantityProperty.change(BaseUtils.nvl(notNulls.get(property), 0), session, propertyObject);
                    found = true;
                }
            }
        }
        return found;
    }

    public void updateStat(ImMap<String, Integer> tableStats, ImMap<String, Integer> keyStats, ImMap<String, Pair<Integer, Integer>> propStats,
                           boolean statDefault, ImSet<PropertyField> props) throws SQLException {
        Integer rowCount;
        int defaultCount = Stat.DEFAULT.getCount();
        if (!tableStats.containsKey(name))
            rowCount = defaultCount;
        else
            rowCount = BaseUtils.nvl(tableStats.get(name), 0);

        if(props == null) {
            ImSet<KeyField> tableKeys = getTableKeys();
            ImValueMap<KeyField, Integer> mvDistinctKeys = tableKeys.mapItValues(); // exception есть
            for (int i = 0, size = tableKeys.size(); i < size; i++) {
                String keySID = getName() + "." + tableKeys.get(i).getName();
                Integer keyCount;
                if (!keyStats.containsKey(keySID))
                    keyCount = defaultCount;
                else {
                    keyCount = keyStats.get(keySID);
                    if(keyCount == null)
                        keyCount = rowCount;
                }
                mvDistinctKeys.mapValue(i, BaseUtils.min(keyCount, rowCount));
            }
            statKeys = TableStatKeys.createForTable(rowCount, mvDistinctKeys.immutableValue());
        }

        ImSet<PropertyField> propertyFieldSet = props == null ? properties : props;

        Stat rowStat = statKeys.getRows();
        ImValueMap<PropertyField, PropStat> mvUpdateStatProps = propertyFieldSet.mapItValues();
        for(int i=0,size=propertyFieldSet.size();i<size;i++) {
            PropertyField prop = propertyFieldSet.get(i);
            Stat distinctStat;
            Stat notNullStat;
            if(propStats.containsKey(getName() + "." + prop.getName())) {
                Pair<Integer, Integer> propStat = propStats.get(getName() + "." + prop.getName());
                notNullStat = propStat.second != null ? new Stat(propStat.second).min(rowStat) : rowStat;
                distinctStat = propStat.first != null ? new Stat(propStat.first).min(notNullStat) : notNullStat;
            } else {
                distinctStat = null;
                notNullStat = null;
            }

            PropStat propStat;
            if (prop.type instanceof DataClass && !((DataClass)prop.type).calculateStat()) {
                if (distinctStat==null) {
                    Stat typeStat = ((DataClass) prop.type).getTypeStat(false).min(rowStat);
                    propStat = new PropStat(typeStat);
                } else
                    propStat = new PropStat(notNullStat, notNullStat);
            } else {
                if (distinctStat==null) {
                    Stat defaultStat = Stat.DEFAULT.min(rowStat);
                    propStat = new PropStat(defaultStat);
                }
                else
                    propStat = new PropStat(distinctStat, notNullStat);
            }
            mvUpdateStatProps.mapValue(i, propStat);
        }
        ImMap<PropertyField, PropStat> updateStatProps = mvUpdateStatProps.immutableValue();
        if(props == null)
            statProps = updateStatProps;
        else {
            assert statProps.keys().containsAll(updateStatProps.keys());
            statProps = MapFact.replaceValues(statProps, updateStatProps);
        }

//        assert statDefault || correctStatProps();
    }

    private boolean correctStatProps() {
        for(PropStat stat : statProps.valueIt()) {
            assert stat.distinct.lessEquals(statKeys.getRows());
        }
        return true;
    }

    public static class InconsistentTable extends GlobalTable {

        private final TableStatKeys statKeys;
        private final ImMap<PropertyField, PropStat> statProps;

        private InconsistentTable(String name, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, BaseClass baseClass, TableStatKeys statKeys, ImMap<PropertyField, PropStat> statProps) {
            super(name, keys, properties, null, null);
            initBaseClasses(baseClass);
            this.statKeys = statKeys;
            this.statProps = statProps;
        }

        public TableStatKeys getTableStatKeys() {
            return statKeys;
        }

        public ImMap<PropertyField, PropStat> getStatProps() {
            return statProps;
        }
    }

    public Table getInconsistent(BaseClass baseClass) {
        return new InconsistentTable(name, keys, properties, baseClass, statKeys, statProps);
//        return new SerializedTable(name, keys, properties, baseClass);
    }
}