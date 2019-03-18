package lsfusion.client.form;

import lsfusion.client.form.object.ClientObject;
import lsfusion.client.logics.ClientPropertyDraw;

import java.util.List;

public interface LogicsSupplier {

    List<ClientObject> getObjects();
    List<ClientPropertyDraw> getPropertyDraws();
}
