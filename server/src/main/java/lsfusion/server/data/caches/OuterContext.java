package lsfusion.server.data.caches;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.value.Value;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.ParamExpr;
import lsfusion.server.data.expr.StaticValueExpr;
import lsfusion.server.data.pack.PackInterface;
import lsfusion.server.data.query.ExprEnumerator;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.TranslateContext;

public interface OuterContext<T extends OuterContext<T>> extends PackInterface<T>, TranslateContext<T> {

    ImSet<ParamExpr> getOuterKeys();

    ImSet<Value> getOuterValues();

    ImSet<StaticValueExpr> getOuterStaticValues();

    int hashOuter(HashContext hashContext);

    ImSet<OuterContext> getOuterDepends();

    T translateOuter(MapTranslate translator);

    boolean enumerate(ExprEnumerator enumerator);
}