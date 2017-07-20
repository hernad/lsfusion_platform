package lsfusion.server.data.query.innerjoins;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.server.Settings;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.query.StatType;
import lsfusion.server.data.query.InnerJoin;
import lsfusion.server.data.query.InnerJoins;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.query.stat.WhereJoin;
import lsfusion.server.data.query.stat.WhereJoins;
import lsfusion.server.data.where.Where;

public class GroupJoinsWhere extends GroupWhere<GroupJoinsWhere> {

    public final WhereJoins joins;

    public final UpWheres<WhereJoin> upWheres;

    public GroupJoinsWhere(KeyEqual keyEqual, WhereJoins joins, UpWheres<WhereJoin> upWheres, Where where, ImOrderSet<Expr> orderTop) {
        this(keyEqual, joins, where, upWheres);

        assert !orderTop.isEmpty() || where.getKeyEquals().singleKey().isEmpty(); // из-за symmetricWhere в groupNotJoinsWheres
    }

    // конструктор паковки для assertion'а
    public GroupJoinsWhere(KeyEqual keyEqual, WhereJoins joins, Where where, UpWheres<WhereJoin> upWheres) {
        super(keyEqual, where);
        this.joins = joins;
        this.upWheres = upWheres;
    }

    public <K extends BaseExpr> StatKeys<K> getStatKeys(ImSet<K> groups, StatType type, StatKeys<KeyExpr> pushStatKeys) {
        return joins.pushStatKeys(pushStatKeys).getStatKeys(groups, where, type, keyEqual);
    }
    public <K extends BaseExpr> StatKeys<K> getStatKeys(ImSet<K> groups, StatType type) {
        return getStatKeys(groups, type, StatKeys.<KeyExpr>NOPUSH());
    }

    public boolean isComplex() {
        return getComplexity(false) > Settings.get().getLimitWhereJoinPack();
    }

    @Override
    public boolean calcTwins(TwinImmutableObject o) {
        return super.calcTwins(o) && joins.equals(((GroupJoinsWhere) o).joins) && upWheres.equals(((GroupJoinsWhere) o).upWheres);
    }

    @Override
    public int immutableHashCode() {
        return 31 * (31 * super.immutableHashCode() + joins.hashCode()) + upWheres.hashCode();
    }
    
    private static int recGetLevelJoins(WhereJoin join, MAddExclMap<WhereJoin, Integer> proceeded, int maxLevel, MExclSet<WhereJoin> resultSet) {
        Integer joinLevel;
        if((joinLevel = proceeded.get(join))!=null)
            return joinLevel;            

        InnerJoins joinFollows = join.getJoinFollows(new Result<UpWheres<InnerJoin>>(), null);

        int result = 0;
        for(InnerJoin followJoin : joinFollows.it()) {
            joinLevel = recGetLevelJoins(followJoin, proceeded, maxLevel, resultSet);
            result = BaseUtils.max(result, joinLevel + 1);
            if(result > maxLevel)
                break;
        }
        
        if(result == maxLevel)
            resultSet.exclAdd(join);

        proceeded.exclAdd(join, result);
        return result;
    }
    // эвристика для группировок, получает join'ы до заданного уровня
    public ImSet<WhereJoin> getLevelJoins(int maxLevel) {
        MAddExclMap<WhereJoin, Integer> proceeded = MapFact.mAddExclMap(); 
        MExclSet<WhereJoin> mResultSet = SetFact.mExclSet();
        for(WhereJoin join : joins.it()) {
            if(recGetLevelJoins(join, proceeded, maxLevel, mResultSet) < maxLevel)
                mResultSet.exclAdd(join);
        }
        return mResultSet.immutable();            
    }
}