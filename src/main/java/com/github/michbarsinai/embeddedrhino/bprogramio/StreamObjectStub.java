package com.github.michbarsinai.embeddedrhino.bprogramio;

import com.github.michbarsinai.embeddedrhino.EmbeddedRhino;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * Placeholders to put in the application stream. Used to break connections from the
 * JavaScript serialized context to the Java context.
 * 
 * 
 * @author michael
 */
public class StreamObjectStub implements java.io.Serializable {
    
    public static final StreamObjectStub BP_PROXY = new StreamObjectStub("bp-proxy");
    public static final StreamObjectStub BP_SOUT = new StreamObjectStub("sout");
    
    private final String name;

    public StreamObjectStub(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "[Stub: " + name + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( ! (obj instanceof StreamObjectStub) ) return false;
        return ((StreamObjectStub)obj).name.equals(name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
    private Object readResolve() {
        if ( name.equals(BP_PROXY.name) ) return BP_PROXY;
        return this;
    }    
}

class OptionalStub extends StreamObjectStub implements java.io.Serializable {

    public static final OptionalStub EMPTY = new OptionalStub(null);
    
    static OptionalStub getFor(Optional<?> optional) {
        return optional.map(e->new OptionalStub(e)).orElse(EMPTY);
    }
    
    public final Object value;

    public OptionalStub(Object value) {
        super("OptionalStub");
        this.value = value;
    }
    
    private Object readResolve() {
        return Optional.ofNullable(value);
    }

    @Override
    public int hashCode() {
        return 5*Objects.hashCode(this.value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final OptionalStub other = (OptionalStub) obj;
        return Objects.equals(this.value, other.value);
    }
}

class NativeSetStub extends StreamObjectStub implements java.io.Serializable {
    
    public final Set<Object> items;

    public NativeSetStub(Set<Object> someItems) {
        super("NativeSetStub");
        items = someItems;
    }
    
    private Object readResolve(){
        
        String src = "let ns = new Set(); javaSet.forEach(e=>ns.add(e)); ns";
        
        try (Context cx = Context.enter()) {
            Scriptable tlScope = EmbeddedRhino.makeSubScope();
            tlScope.put("javaSet", tlScope, items);
            cx.evaluateString( tlScope, src, "", 1, null);
            return tlScope.get("ns",tlScope);
        }
    }
    
    @Override
    public int hashCode() {
        return 7*Objects.hashCode(this.items);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NativeSetStub other = (NativeSetStub) obj;
        return Objects.equals(this.items, other.items);
    }
}