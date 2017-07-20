package lsfusion.server.data.expr.formula;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyType;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.query.MStaticExecuteEnvironment;
import lsfusion.server.data.query.StaticExecuteEnvironment;
import lsfusion.server.data.sql.SQLSyntax;

public abstract class ListExprSource extends ContextListExprType implements ExprSource {

    public ListExprSource(ImList<? extends Expr> exprs) {
        super(exprs);
    }

    public abstract CompileSource getCompileSource();

    public String getSource(int i) {
        return exprs.get(i).getSource(getCompileSource());
    }

    public SQLSyntax getSyntax() {
        return getCompileSource().syntax;
    }

    public MStaticExecuteEnvironment getMEnv() {
        return getCompileSource().env;
    }

    public KeyType getKeyType() {
        return getCompileSource().keyType;
    }

    public boolean isToString() {
        return false;
    }
}