package lsfusion.server.logics.property.actions.flow;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.Settings;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.context.ExecutorFactory;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.logics.property.*;

import java.sql.SQLException;
import java.util.concurrent.ScheduledExecutorService;

public class NewExecutorActionProperty extends AroundAspectActionProperty {
    ScheduledExecutorService executor;
    private final CalcPropertyMapImplement threadsProp;

    public <I extends PropertyInterface> NewExecutorActionProperty(String caption, ImOrderSet<I> innerInterfaces,
                                                                   ActionPropertyMapImplement<?, I> action,
                                                                   CalcPropertyMapImplement<?, I> threadsProp) {
        super(caption, innerInterfaces, action);

        this.threadsProp = threadsProp;

        finalizeInit();
    }

    @Override
    protected ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        return super.aspectChangeExtProps().replaceValues(true);
    }

    @Override
    public ImMap<CalcProperty, Boolean> aspectUsedExtProps() {
        return super.aspectUsedExtProps().replaceValues(true);
    }

    @Override
    public CalcPropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        return IsClassProperty.getMapProperty(
                super.calcWhereProperty().mapInterfaceClasses(ClassType.wherePolicy));
    }

    @Override
    protected FlowResult aroundAspect(final ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            Integer nThreads = (Integer) threadsProp.read(context, context.getKeys());;
            executor = ExecutorFactory.createNewThreadService(context, nThreads);
            return proceed(context.override(executor));
        } finally {
            if(executor != null)
                executor.shutdown();
        }
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        return type == ChangeFlowType.NEWSESSION || (!(type == ChangeFlowType.APPLY || type == ChangeFlowType.CANCEL) && super.hasFlow(type));
    }

    @Override
    public CustomClass getSimpleAdd() {
        return aspectActionImplement.property.getSimpleAdd();
    }

    @Override
    public PropertyInterface getSimpleDelete() {
        return aspectActionImplement.property.getSimpleDelete();
    }
}
