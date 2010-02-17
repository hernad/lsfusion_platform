package platform.server.data.expr.query;

import net.jcip.annotations.Immutable;
import platform.server.data.expr.*;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;
import platform.server.data.where.DataWhere;
import platform.server.data.query.InnerJoin;
import platform.server.data.query.InnerWhere;
import platform.server.data.query.HashContext;
import platform.server.data.query.SourceJoin;
import platform.server.data.expr.query.GroupExpr;

import java.util.Map;
import java.util.Set;

@Immutable
public class GroupJoin extends QueryJoin<BaseExpr, GroupJoin.Query> implements InnerJoin {

    public static class Query implements TranslateContext<Query> {
        private final Where where;
        private final InnerWhere innerWhere;

        public Query(Where where, InnerWhere innerWhere) {
            this.where = where;
            this.innerWhere = innerWhere;
        }

        public int hashContext(HashContext hashContext) {
            return where.hashContext(hashContext) * 31 + innerWhere.hashContext(hashContext);
        }

        public Query translateDirect(KeyTranslator translator) {
            return new Query(where.translateDirect(translator),innerWhere.translateDirect(translator));
        }

        public SourceJoin[] getEnum() {
            throw new RuntimeException("not supported");
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof Query && innerWhere.equals(((Query) o).innerWhere) && where.equals(((Query) o).where);
        }

        @Override
        public int hashCode() {
            return 31 * where.hashCode() + innerWhere.hashCode();
        }
    }

    public DataWhereSet getJoinFollows() {
        return InnerExpr.getExprFollows(group);
    }

    // дублируем аналогичную логику GroupExpr'а
    private GroupJoin(GroupJoin join, KeyTranslator translator) {
        super(join, translator);
    }

    public InnerJoin translateDirect(KeyTranslator translator) {
        return new GroupJoin(this, translator);
    }

    public GroupJoin(Set<KeyExpr> keys, Set<ValueExpr> values, Where where, InnerWhere innerWhere, Map<BaseExpr, BaseExpr> group) {
        super(keys,values,new Query(where,innerWhere),group);
    }

    public int hashContext(HashContext hashContext) {
        return hashes.hashContext(hashContext);
    }

    public boolean isIn(DataWhereSet set) {
        for(int i=0;i<set.size;i++) {
            DataWhere where = set.get(i);
            if(where instanceof GroupExpr.NotNull && equals(((GroupExpr.NotNull)where).getGroupJoin()))
                return true;
        }
        return false;
    }
}
