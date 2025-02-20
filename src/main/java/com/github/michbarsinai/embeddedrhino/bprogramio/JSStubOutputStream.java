package com.github.michbarsinai.embeddedrhino.bprogramio;

import com.github.michbarsinai.embeddedrhino.EmbeddedRhino;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.serialize.ScriptableOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeSet;

/**
 * Output stream for {@link BThreadSyncSnapshot} objects. Creates stubs for
 * objects that can link back to the java context, such as the {@code XJsProxy}
 * classes.
 *
 * @author michael
 */
public class JSStubOutputStream extends ScriptableOutputStream {

    @SuppressWarnings("OverridableMethodCallInConstructor")
    public JSStubOutputStream(OutputStream out, Scriptable scope) throws IOException {
        super(out, scope);
    }

    @Override
    protected Object replaceObject(Object obj) throws IOException {
        if (obj == null) {
            return null;
        } else if ( obj instanceof PrintStream ) {
            return StreamObjectStub.BP_SOUT;
            
        } else if (obj instanceof Optional) {
            return OptionalStub.getFor((Optional) obj);

        } else if (obj instanceof NativeSet) {
            NativeSet ns = (NativeSet) obj;
            return stubify(ns);

        } else {
            return super.replaceObject(obj);
        }
    }

    /**
     * NativeSet does not have good Java accessors, so we use a JavaScript code
     * to extract its members.
     *
     * @param ns a NativeSet
     * @return a NativeSetStub with the values of ns.
     */
    private NativeSetStub stubify(NativeSet ns) {
        System.out.println("*********** Native Set Stubbing ***********");
        String code = "ns.forEach(e=>javaSet.add(e))";

        try ( Context cx = Context.enter()) {
            Scriptable tlScope = EmbeddedRhino.makeSubScope();
//            ScriptableObject tlScope = cx.initStandardObjects();
            Set<Object> javaSet = new HashSet<>();
            tlScope.put("ns", tlScope, ns);
            tlScope.put("javaSet", tlScope, javaSet);
            cx.evaluateString(tlScope, code, "", 1, null);

            return new NativeSetStub(javaSet);
        } finally {
            System.out.println("*********** DONE Native Set Stubbing ***********");
        }
    }
}
