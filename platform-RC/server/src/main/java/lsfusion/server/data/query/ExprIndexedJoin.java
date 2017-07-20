package lsfusion.server.data.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.SFunctionSet;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.SymmAddValue;
import lsfusion.base.col.interfaces.mutable.add.MAddMap;
import lsfusion.interop.Compare;
import lsfusion.server.Settings;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.NullableExprInterface;
import lsfusion.server.data.expr.query.QueryJoin;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.expr.query.StatType;
import lsfusion.server.data.query.innerjoins.AbstractUpWhere;
import lsfusion.server.data.query.innerjoins.UpWhere;
import lsfusion.server.data.query.innerjoins.UpWheres;
import lsfusion.server.data.query.stat.*;
import lsfusion.server.data.translator.MapTranslate;

import java.util.List;

public class ExprIndexedJoin extends ExprJoin<ExprIndexedJoin> {

    private final Compare compare;
    private final Expr compareExpr;
    private boolean not;
    private boolean isOrderTop;

    @Override
    public String toString() {
        return baseExpr + " " + compare + " " + compareExpr + " " + not;
    }

    public ExprIndexedJoin(BaseExpr baseExpr, Compare compare, Expr compareExpr, boolean not, boolean isOrderTop) {
        super(baseExpr);
        assert !compare.equals(Compare.EQUALS);
        assert compareExpr.isValue();
        assert baseExpr.isIndexed();
        this.compareExpr = compareExpr;
        this.compare = compare;
        this.not = not;
        this.isOrderTop = isOrderTop;
    }

    public boolean isOrderTop() {
        return isOrderTop;
    }

    public StatKeys<Integer> getStatKeys(KeyStat keyStat, StatType type, boolean oldMech) {
        if(oldMech) {
            if(not)
                return new StatKeys<>(SetFact.<Integer>EMPTY(), Stat.ONE);
            else {
                if (compare.equals(Compare.EQUALS) && !givesNoKeys()) { // если не дает ключей, нельзя уменьшать статистику, так как паковка может съесть другие join'ы и тогда будет висячий ключ
                    assert false; // так как !compare.equals(Compare.EQUALS)
                    return new StatKeys<>(SetFact.singleton(0), Stat.ONE);
                } else
                    return new StatKeys<>(SetFact.singleton(0), baseExpr.getTypeStat(keyStat, true));
            }
        }
        throw new UnsupportedOperationException(); // так как вырезается в buildGraph
    }

    @Override
    public Cost getPushedCost(KeyStat keyStat, StatType type, Cost pushCost, Stat pushStat, ImMap<Integer, Stat> pushKeys, ImMap<Integer, Stat> pushNotNullKeys, ImMap<BaseExpr, Stat> pushProps, Result<ImSet<Integer>> rPushedKeys, Result<ImSet<BaseExpr>> rPushedProps) {
        throw new UnsupportedOperationException(); // так как вырезается в buildGraph
    }

    //    @Override
//    protected Stat getStat(KeyStat keyStat) {
//        assert !not;
//
//        if (compare.equals(Compare.EQUALS) && !givesNoKeys()) { // если не дает ключей, нельзя уменьшать статистику, так как паковка может съесть другие join'ы и тогда будет висячий ключ
//            assert false; // так как !compare.equals(Compare.EQUALS)
//            return Stat.ONE;
//        } else
//            return baseExpr.getTypeStat(keyStat, true);
//    }

    protected int hash(HashContext hashContext) {
        return 31 * (31 * super.hash(hashContext) + compare.hashCode()) + compareExpr.hashOuter(hashContext) + 13 + (not ? 1 : 0) + (isOrderTop ? 3 : 0);
    }

    protected ExprIndexedJoin translate(MapTranslate translator) {
        return new ExprIndexedJoin(baseExpr.translateOuter(translator), compare, compareExpr, not, isOrderTop);
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return super.calcTwins(o) && compare.equals(((ExprIndexedJoin)o).compare) && compareExpr.equals(((ExprIndexedJoin)o).compareExpr) && not == ((ExprIndexedJoin)o).not && isOrderTop == ((ExprIndexedJoin)o).isOrderTop;
    }

    @Override
    public ImSet<NullableExprInterface> getExprFollows(boolean includeInnerWithoutNotNull, boolean recursive) {
        if(not)
            return SetFact.EMPTY();
        return super.getExprFollows(includeInnerWithoutNotNull, recursive);
    }

    @Override
    public ImMap<Integer, BaseExpr> getJoins() {
        if(not)
            return MapFact.EMPTY();
        return super.getJoins();
    }

    @Override
    public InnerJoins getInnerJoins() {
        if(not)
            return InnerJoins.EMPTY;
        return super.getInnerJoins();
    }

    public boolean givesNoKeys() {
        return not || super.givesNoKeys();
    }

    public KeyExpr getKeyExpr() {
        if(baseExpr instanceof KeyExpr) {
            assert !not;
            return (KeyExpr) baseExpr;
        }
        return null;
    }


    private enum IntervalType {
        LEFT, RIGHT, FULL;

        public IntervalType and(IntervalType type) {
            if(this == type)
                return this;
            return FULL;
        }

    }

    private static IntervalType getIntervalType(Compare compare) {
        assert !compare.equals(Compare.EQUALS);
        if(compare == Compare.GREATER || compare == Compare.GREATER_EQUALS)
            return IntervalType.LEFT;
        if(compare == Compare.LESS || compare == Compare.LESS_EQUALS)
            return IntervalType.RIGHT;
        if(compare == Compare.LIKE)
            return IntervalType.FULL;

        return null;
    }
    // определяет замыкающиеся диапазоны
    public static ImSet<ExprIndexedJoin> getIntervals(WhereJoin[] wheres) {
        MAddMap<BaseExpr, IntervalType> intervals = MapFact.mAddMap(new SymmAddValue<BaseExpr, IntervalType>() {
            public IntervalType addValue(BaseExpr key, IntervalType prevValue, IntervalType newValue) {
                return prevValue.and(newValue);
            }
        });
        boolean hasKeyExprs = false; // оптимизация
        for(WhereJoin where : wheres) {
            if(where instanceof ExprIndexedJoin) {
                ExprIndexedJoin eiJoin = (ExprIndexedJoin) where;
                boolean hasKeyExpr = false;
                if(!eiJoin.givesNoKeys() || (hasKeyExpr = (eiJoin.getKeyExpr() != null))) { // по идее эта проверка не нужна, но тогда могут появляться висячие ключи, хотя строго говоря потом можно наоборот поддержать эти случаи, тогда a>=1 AND a<=5 будет работать
                    hasKeyExprs = hasKeyExprs || hasKeyExpr;
                    IntervalType type = getIntervalType(eiJoin.compare);
                    if (type != null)
                        intervals.add(eiJoin.baseExpr, type);
                }
            }
        }

        MExclSet<ExprIndexedJoin> mResult = SetFact.mExclSetMax(wheres.length);
        for(WhereJoin where : wheres) {
            if (where instanceof ExprIndexedJoin) {
                ExprIndexedJoin eiJoin = (ExprIndexedJoin) where;
                IntervalType type = intervals.get(eiJoin.baseExpr);
                if(type != null && type == IntervalType.FULL)
                    mResult.exclAdd(eiJoin);
            }
        }
        ImSet<ExprIndexedJoin> result = mResult.immutable();

        if(result.size() > 0) {
            if(hasKeyExprs) { // оптимизация
                // по идее эта обработка не нужна, но тогда могут появляться висячие ключи, хотя строго говоря потом можно наоборот поддержать эти случаи, тогда a>=1 AND a<=5 будет работать
                final ImSet<KeyExpr> innerKeys = getInnerKeys(wheres, null);
                result = result.filterFn(new SFunctionSet<ExprIndexedJoin>() {
                    public boolean contains(ExprIndexedJoin element) {
                        KeyExpr keyExpr = element.getKeyExpr();
                        if(keyExpr != null && !innerKeys.contains(keyExpr)) // висячий ключ
                            return false;
                        return true;
                    }
                });
            }
        }

        return result;
    }

    // excludeQueryJoin нужен чтобы она не считала его источником ключей => бесконечное проталкивание
    private static ImSet<KeyExpr> getInnerKeys(WhereJoin[] wheres, QueryJoin excludeQueryJoin) {
        MSet<KeyExpr> mInnerKeys = SetFact.mSet();
        for(WhereJoin<?, ?> where : wheres) {
            if (where instanceof InnerJoin && (excludeQueryJoin == null || !BaseUtils.hashEquals(where, excludeQueryJoin))) {
                ImSet<BaseExpr> whereKeys = where.getJoins().values().filterCol(new SFunctionSet<BaseExpr>() {
                    public boolean contains(BaseExpr element) {
                        return element instanceof KeyExpr;
                    }
                }).toSet();
                mInnerKeys.addAll(BaseUtils.<ImSet<KeyExpr>>immutableCast(whereKeys));
            }
        }
        return mInnerKeys.immutable();
    }

    public static void fillIntervals(ImSet<ExprIndexedJoin> exprs, List<WhereJoin> mResult, Result<UpWheres<WhereJoin>> upAdjWheres, WhereJoin[] wheres, QueryJoin excludeQueryJoin) {
        ImMap<BaseExpr, ImSet<ExprIndexedJoin>> exprIndexedJoins = exprs.group(new BaseUtils.Group<BaseExpr, ExprIndexedJoin>() {
            public BaseExpr group(ExprIndexedJoin key) {
                return key.baseExpr;
            }});

        MMap<WhereJoin, UpWhere> mUpIntervalWheres = null;
        if(upAdjWheres != null)
            mUpIntervalWheres = MapFact.mMapMax(exprIndexedJoins.size(), AbstractUpWhere.<WhereJoin>and());

        int intStat = Settings.get().getAverageIntervalStat();
        ImSet<KeyExpr> innerKeys = null;
        for(int i=0,size=exprIndexedJoins.size();i<size;i++) {
            ImSet<ExprIndexedJoin> joins = exprIndexedJoins.getValue(i);
            BaseExpr expr = exprIndexedJoins.getKey(i);

            boolean fixedInterval = true;
            if(intStat <= 0)
                fixedInterval = false;

            if(fixedInterval) {
                ExprIndexedJoin.IntervalType result = null;
                for (ExprIndexedJoin join : joins) {
                    IntervalType joinType = getIntervalType(join.compare);
                    if (result == null)
                        result = joinType;
                    else
                        result = result.and(joinType);
                }
                fixedInterval = result == IntervalType.FULL;
            }

            if(fixedInterval && expr instanceof KeyExpr) { // по идее эта обработка не нужна, но тогда могут появляться висячие ключи (так как a>=1 AND a<=5 будет убивать другие join'ы), хотя строго говоря потом можно наоборот поддержать эти случаи, тогда a>=1 AND a<=5 будет работать
                if(innerKeys == null)
                    innerKeys = getInnerKeys(wheres, excludeQueryJoin);
                if(!innerKeys.contains((KeyExpr)expr)) // висячий ключ
                    fixedInterval = false;
            }

            ExprStatJoin adjJoin = new ExprStatJoin(expr, fixedInterval ? new Stat(intStat, true) : Stat.ALOT);
            mResult.add(adjJoin);

            if(upAdjWheres != null)
                for(ExprIndexedJoin join : joins)
                    mUpIntervalWheres.add(adjJoin, upAdjWheres.result.get(join));
        }

        if(upAdjWheres != null)
            upAdjWheres.set(new UpWheres<WhereJoin>(upAdjWheres.result.addExcl(mUpIntervalWheres.immutable())));
    }
}