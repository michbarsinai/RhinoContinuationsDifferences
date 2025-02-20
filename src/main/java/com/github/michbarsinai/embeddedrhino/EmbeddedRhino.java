package com.github.michbarsinai.embeddedrhino;

import com.github.michbarsinai.embeddedrhino.bprogramio.ProgramIO;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import static java.util.stream.Collectors.joining;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ContinuationPending;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.NativeContinuation;
import org.mozilla.javascript.NativeFunction;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 *
 * @author michael
 */
public class EmbeddedRhino {
        
    static final String PROG_ADD_DATA = "let s = new Set();\n"+
        "['a','b','c','dee'].forEach( itm=> { \n" +
        "  s.add(itm); sout.println(`added ${itm}`);});";
    
    
    
    static final String PROG_EXTRACT_DATA = "ns.forEach(e=>javaSet.add(e));";
    
    static ScriptableObject topScope;
    
    public static void main(String[] args) throws Exception {
        
        ContextFactory.initGlobal( new RhinoContextFactory() );
        
        try ( var cx = Context.enter() ) {
            topScope = cx.initStandardObjects(new ImporterTopLevel(cx), true);
        }
        System.out.println("Simple Read/Write");
        simpleReadWrite();
        System.out.println("---");
        System.out.println("Read/Write with serialization");
        withSerialization();
        System.out.println("---");

        try {
            System.out.println("Read/Write with continuation and serialization");
            withContinuation();
            System.out.println("---");

        } catch ( Exception e ) {
            e.printStackTrace(System.err);
        }
        
    }
    
    public static Scriptable makeSubScope(){
        try ( var cx = Context.enter() ) {
            Scriptable retVal = cx.newObject(topScope);
            retVal.setPrototype(topScope);
            retVal.setParentScope(null);
            return retVal;
        }
    }
    
    static void withContinuation() throws Exception {
        String source = readString("ContinuationCode.js");
        byte[] serialized = null;
        
        try ( var cx = Context.enter() ) {
            Scriptable scope1 = makeSubScope();
            ProgramIO ssi = new ProgramIO(scope1);
//            ProgramIO ssi = new ProgramIO(getGlobalScope());
            scope1.put("sout", scope1, System.out);
            scope1.put("capt", scope1, new ContinuationCaptureer());
            cx.evaluateString(scope1, source, "ContinuationCode.js", 0, null);
            NativeFunction mainFunc = (NativeFunction) scope1.get("main", scope1);
            try {
                Object ret = cx.callFunctionWithContinuations(mainFunc, mainFunc, new Object[]{});
                System.out.println("ret = " + ret);
                
            } catch (ContinuationPending pnd) {
                System.out.println("Continuation captured. State: = " + pnd.getApplicationState());
                NativeContinuation cnt = (NativeContinuation) pnd.getContinuation();
    
                serialized = ssi.serialize(cnt);                
            }
        }
        
        try ( var cx2 = Context.enter() ) {
            final Scriptable scope2 = makeSubScope();
            ProgramIO ssi = new ProgramIO(scope2);
            NativeContinuation nc = (NativeContinuation) ssi.deserialize(serialized);
            cx2.resumeContinuation(nc, scope2, 0);
        }
    }
    
    static void withSerialization() {
        
        System.out.println("withSerialization");
        try {
            Object nativeSet;

            ProgramIO ssi = new ProgramIO(getGlobalScope());

            byte[] serialized;
            // Create a native set, fill it with data.
            try ( var cx = Context.enter() ) {
                Scriptable scope1 = makeSubScope();
                scope1.put("sout", scope1, System.out);
                cx.evaluateString(scope1, PROG_ADD_DATA, "var PROG_ADD_DATA", 0, null);
                nativeSet = scope1.get("s", scope1);
                System.out.println("nativeSet = " + nativeSet);
                serialized = ssi.serialize(scope1);
            }

            // extract the data from the native set using Rhino
            try ( Context cx = Context.enter() ) {
                Scriptable scope1Unserialized = ssi.deserialize(serialized);
                Object nativeSetUnSer = scope1Unserialized.get("s", scope1Unserialized);
                System.out.println("nativeSet (unser) = " + nativeSet);

                Scriptable scope2 = makeSubScope();
                Set<Object> javaSet = new HashSet<>();
                scope2.put("ns", scope2, nativeSetUnSer);
                scope2.put("javaSet", scope2, javaSet);
                cx.evaluateString(scope2, PROG_EXTRACT_DATA, "var PROG_EXTRACT_DATA", 1, null);

                System.out.println("extracted set size (post-serialization): " + javaSet.size());
                javaSet.forEach( o -> System.out.println(o) );
            }        
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }
    
    static void simpleReadWrite() {
        System.out.println("simpleReadWrite ");
        Object nativeSet;
        // Create a native set, fill it with data.
        try ( var cx = Context.enter() ) {
            Scriptable scope1 = makeSubScope();
            scope1.put("sout", scope1, System.out);
            cx.evaluateString(scope1, PROG_ADD_DATA, "var PROG_ADD_DATA", 0, null);
            nativeSet = scope1.get("s", scope1);
            System.out.println("nativeSet = " + nativeSet);
        }
        
        // extract the data from the native set using Rhino
        try ( Context cx = Context.enter() ) {
            Scriptable scope2 = makeSubScope();
            Set<Object> javaSet = new HashSet<>();
            scope2.put("ns", scope2, nativeSet);
            scope2.put("javaSet", scope2, javaSet);
            cx.evaluateString(scope2, PROG_EXTRACT_DATA, "var PROG_EXTRACT_DATA", 1, null);
            System.out.println("Extracted Java Set Size: " + javaSet.size());
            javaSet.forEach( o -> System.out.println(o) );
        }        
    }

    public static Scriptable getGlobalScope() {
        return topScope;
    }
    
    public static String readString( String address ) {
        final URL resourceUrl = Thread.currentThread().getContextClassLoader().getResource(address);
        if ( resourceUrl == null ) {
            throw new RuntimeException("Missing resource: '" + address + "'");
        }
        try (InputStream strm = Thread.currentThread().getContextClassLoader().getResourceAsStream(address);
             BufferedReader rdr = new BufferedReader(new InputStreamReader(strm,StandardCharsets.UTF_8)) 
            ) {
            return rdr.lines().collect( joining("\n") );
            
        } catch (IOException ex) {
            throw new RuntimeException("IOError reading String resource '" + address + "'", ex);
        }
    }
    
}
