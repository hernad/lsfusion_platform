package lsfusion.server.base.version.interfaces;

import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.NF;

public interface NFMapCol<K, V> extends NF {
    
    void addAll(K key, Iterable<V> it, Version version);
    void removeAll(K key, Version version);
    
}
