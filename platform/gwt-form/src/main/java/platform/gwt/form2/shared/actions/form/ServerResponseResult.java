package platform.gwt.form2.shared.actions.form;

import net.customware.gwt.dispatch.shared.Result;
import platform.gwt.form2.shared.view.actions.GAction;

public class ServerResponseResult implements Result {
    public GAction[] actions;
    public boolean resumeInvocation;
    public boolean pendingRemoteChanges;

    public ServerResponseResult() {}

    public ServerResponseResult(GAction[] actions, boolean resumeInvocation, boolean pendingRemoteChanges) {
        this.actions = actions;
        this.resumeInvocation = resumeInvocation;
        this.pendingRemoteChanges = pendingRemoteChanges;
    }
}
