package com.github.michbarsinai.embeddedrhino.bprogramio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * Serialized and de-serializes {@link BProgramSyncSnapshot}s.
 *
 * @author michael
 */
public class ProgramIO {

    private final Scriptable serializationLimit;

    public ProgramIO(Scriptable serializationLimit) {
        this.serializationLimit = serializationLimit;
    }
    
    public byte[] serialize(Scriptable obj) throws IOException {
        try (Context cx = Context.enter()) {
            try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                JSStubOutputStream outs = new JSStubOutputStream(bytes, serializationLimit)) {
                outs.writeObject(obj);
                outs.flush();
                return bytes.toByteArray();
            }
        }
    }

    public Scriptable deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (Context cx = Context.enter()) {
            try (JSStubInputStream in = new JSStubInputStream(new ByteArrayInputStream(bytes), 
                                                    serializationLimit,
                                                    getStubProvider())
            ) {
                return (Scriptable) in.readObject();
            }
        }
    }
    
    private StubProvider getStubProvider() {
       return (StreamObjectStub stub) -> {
           if ( stub.equals(StreamObjectStub.BP_SOUT) ) {
               return System.out;
           } else {
               throw new RuntimeException("Unknown stub.");
           }
       };
    }
    
}
