package lsfusion.server.form.instance;

import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.Where;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.stat.StaticDataGenerator;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.Modifier;

import java.sql.SQLException;

public interface FormDataInterface {

    FormEntity getFormEntity();

    BaseClass getBaseClass();
    QueryEnvironment getQueryEnv();
    DataSession getSession();
    Modifier getModifier();

    StaticDataGenerator.Hierarchy getHierarchy(boolean isReport);
    ImMap<ObjectEntity, ? extends ObjectValue> getObjectValues(ImSet<GroupObjectEntity> valueGroups);
    Where getWhere(GroupObjectEntity groupObject, ImSet<GroupObjectEntity> valueGroups, ImMap<ObjectEntity, Expr> mapExprs) throws SQLException, SQLHandledException;
    Where getValueWhere(GroupObjectEntity groupObject, ImSet<GroupObjectEntity> valueGroups, ImMap<ObjectEntity, Expr> mapExprs) throws SQLException, SQLHandledException;
    ImOrderMap<CompareEntity, Boolean> getOrders(GroupObjectEntity groupObject, ImSet<GroupObjectEntity> valueGroups);
}