package lsfusion.server.session;

import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.data.*;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.PropertyInterface;

import java.sql.SQLException;

public class SinglePropertyTableUsage<K> extends SessionTableUsage<K, String> {

    public SinglePropertyTableUsage(String debugInfo, ImOrderSet<K> keys, Type.Getter<K> keyType, final Type propertyType) {
        super(debugInfo, keys, SetFact.singletonOrder("value"), keyType, new Type.Getter<String>() {
            public Type getType(String key) {
                return propertyType;
            }
        });
    }

    public ModifyResult modifyRecord(SQLSession session, ImMap<K, DataObject> keyFields, ObjectValue propertyValue, Modify type, OperationOwner owner) throws SQLException, SQLHandledException {
        return modifyRecord(session, keyFields, MapFact.singleton("value", propertyValue), type, owner);
    }

    public ModifyResult modifyRows(SQLSession session, ImRevMap<K, KeyExpr> mapKeys, Expr expr, Where where, BaseClass baseClass, Modify type, QueryEnvironment env, boolean updateClasses) throws SQLException, SQLHandledException {
        return modifyRows(session, new Query<>(mapKeys, expr, "value", where), baseClass, type, env, updateClasses);
    }

    public static <P extends PropertyInterface> PropertyChange<P> getChange(SinglePropertyTableUsage<P> table) {
        ImRevMap<P, KeyExpr> mapKeys = table.getMapKeys();
        Join<String> join = table.join(mapKeys);
        return new PropertyChange<>(mapKeys, join.getExpr("value"), join.getWhere());
    }

    public <B> ClassWhere<B> getClassWhere(ImRevMap<K, ? extends B> remapKeys, B mapProp) {
        return getClassWhere("value", remapKeys, mapProp);
    }

    public void fixKeyClasses(ClassWhere<K> classes) {
        table = table.fixKeyClasses(classes.remap(mapKeys.reverse()));
    }

    public void updateAdded(SQLSession sql, BaseClass baseClass, Pair<Integer,Integer>[] shifts, OperationOwner owner) throws SQLException, SQLHandledException {
        updateAdded(sql, baseClass, "value", shifts, owner);
    }

    public void checkClasses(SQLSession session, BaseClass baseClass, boolean updateClasses, OperationOwner owner) throws SQLException, SQLHandledException {
        table = table.checkClasses(session, baseClass, updateClasses, owner);
    }
}