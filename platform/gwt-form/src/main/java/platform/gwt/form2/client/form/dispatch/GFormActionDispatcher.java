package platform.gwt.form2.client.form.dispatch;

import com.google.gwt.user.client.rpc.AsyncCallback;
import platform.gwt.form2.client.events.OpenFormEvent;
import platform.gwt.form2.client.form.ui.classes.ClassChosenHandler;
import platform.gwt.form2.client.form.ui.GFormController;
import platform.gwt.form2.client.form.ui.dialog.DialogBoxHelper;
import platform.gwt.form2.client.form.ui.dialog.WindowHiddenHandler;
import platform.gwt.form2.shared.actions.form.ServerResponseResult;
import platform.gwt.form2.shared.view.actions.*;
import platform.gwt.form2.shared.view.classes.GObjectClass;

public class GFormActionDispatcher extends GwtActionDispatcher {
    protected final GFormController form;

    public GFormActionDispatcher(GFormController form) {
        this.form = form;
    }

    @Override
    protected void postDispatchResponse(ServerResponseResult response) {
        if (response.pendingRemoteChanges) {
            form.processRemoteChanges();
        }
    }

    @Override
    protected void continueServerInvocation(Object[] actionResults, AsyncCallback<ServerResponseResult> callback) {
        form.contiueServerInvocation(actionResults, callback);
    }

    @Override
    protected void throwInServerInvocation(Exception ex) {
        form.throwInServerInvocation(ex);
    }

    @Override
    public void execute(GReportAction action) {
        super.execute(action);
    }

    @Override
    public void execute(GFormAction action) {
        if (action.isModal) {
            pauseDispatching();
            form.showModalForm(action.form, new WindowHiddenHandler() {
                @Override
                public void onHidden() {
                    continueDispatching();
                }
            });
        } else {
            OpenFormEvent.fireEvent(action.form);
        }
    }

    @Override
    public void execute(GDialogAction action) {
        pauseDispatching();
        form.showModalDialog(action.dialog, new WindowHiddenHandler() {
            @Override
            public void onHidden() {
                continueDispatching();
            }
        });
    }

    @Override
    public Object execute(GChooseClassAction action) {
        pauseDispatching();
        form.showClassDialog(action.baseClass, action.defaultClass, action.concreate, new ClassChosenHandler() {
            @Override
            public void onClassChosen(GObjectClass chosenClass) {
                continueDispatching(chosenClass == null ? null : chosenClass.ID);
            }
        });
        return null;
    }

    @Override
    public void execute(GMessageAction action) {
        pauseDispatching();
        form.blockingMessage(action.caption, action.message, new DialogBoxHelper.CloseCallback() {
            @Override
            public void closed(DialogBoxHelper.OptionType chosenOption) {
                continueDispatching();
            }
        });
    }

    @Override
    public int execute(GConfirmAction action) {
        pauseDispatching();
        form.blockingConfirm(action.caption, action.message, new DialogBoxHelper.CloseCallback() {
            @Override
            public void closed(DialogBoxHelper.OptionType chosenOption) {
                continueDispatching(chosenOption.asInteger());
            }
        });

        return 0;
    }

    @Override
    public void execute(GLogMessageAction action) {
        pauseDispatching();
        form.blockingMessage(action.failed, "", action.message, new DialogBoxHelper.CloseCallback() {
            @Override
            public void closed(DialogBoxHelper.OptionType chosenOption) {
                continueDispatching();
            }
        });
    }

    @Override
    public void execute(GRunPrintReportAction action) {
        form.runPrintReport(action.getReportSID());
    }

    @Override
    public void execute(GRunOpenInExcelAction action) {
        form.runOpenInExcel(action.getReportSID());
    }

    @Override
    public void execute(GHideFormAction action) {
        form.hideForm();
    }

    @Override
    public void execute(GProcessFormChangesAction action) {
        form.applyRemoteChanges(action.formChanges);
    }
}
