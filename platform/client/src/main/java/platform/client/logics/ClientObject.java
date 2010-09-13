package platform.client.logics;

import platform.client.logics.classes.ClientClass;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;

public class ClientObject implements Serializable {

    private Integer ID = 0;

    public Integer getID() {
        return ID;
    }

    public String caption;
    public boolean addOnTransaction;

    // вручную заполняется
    public ClientGroupObject groupObject;

    public ClientClass baseClass;

    public ClientClassChooser classChooser;

    public ClientObject(DataInputStream inStream, Collection<ClientContainer> containers, ClientGroupObject iGroupObject) throws ClassNotFoundException, IOException {

        groupObject = iGroupObject;

        ID = inStream.readInt();
        caption = inStream.readUTF();
        addOnTransaction = inStream.readBoolean();

        baseClass = ClientClass.deserialize(inStream);

        classChooser = new ClientClassChooser(inStream,containers);
    }

    public String toString() { return caption; }
}
