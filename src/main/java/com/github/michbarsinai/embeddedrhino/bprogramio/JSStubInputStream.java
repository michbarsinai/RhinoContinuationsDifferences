package com.github.michbarsinai.embeddedrhino.bprogramio;

import java.io.IOException;
import java.io.InputStream;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.serialize.ScriptableInputStream;

/**
 *
 * @author michael
 */
public class JSStubInputStream extends ScriptableInputStream {

    private final StubProvider stubProvider;

    public JSStubInputStream(InputStream in, Scriptable scope, StubProvider aProvider) throws IOException {
        super(in, scope);
        stubProvider = aProvider;
    }

    @Override
    protected Object resolveObject(Object obj) throws IOException {
        return ( obj instanceof StreamObjectStub )
            ? stubProvider.get((StreamObjectStub) obj)
            : super.resolveObject(obj);
    }

    @Override
    protected Object readObjectOverride() throws IOException, ClassNotFoundException {
        return super.readObjectOverride();
    }
    
}
