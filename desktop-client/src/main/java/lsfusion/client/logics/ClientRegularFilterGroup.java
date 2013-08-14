package lsfusion.client.logics;

import lsfusion.base.context.ApplicationContext;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientRegularFilterGroup extends ClientComponent {

    public List<ClientRegularFilter> filters = new ArrayList<ClientRegularFilter>();

    public int defaultFilterIndex = -1;

    public ClientGroupObject groupObject;

    public ClientRegularFilterGroup() {

    }

    public ClientRegularFilterGroup(int ID, ApplicationContext context) {
        super(ID, context);
    }

    @Override
    protected void initDefaultConstraints() {
//        constraints.insetsSibling = new Insets(0, 4, 2, 4);
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.serializeCollection(outStream, filters);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        filters = pool.deserializeList(inStream);

        defaultFilterIndex = inStream.readInt();

        groupObject = pool.deserializeObject(inStream);
    }

    @Override
    public String getCaption() {
        return ClientResourceBundle.getString("descriptor.filter");
    }

    @Override
    public String toString() {
        return filters.toString() + "[sid:" + getSID() + "]";
    }

}
