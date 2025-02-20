package com.github.michbarsinai.embeddedrhino;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrapFactory;

/**
 * A context factory for all BPjs' contexts.
 *
 * @author michael
 */
public class RhinoContextFactory extends ContextFactory {
    
    @Override
    protected boolean hasFeature(Context cx, int featureIndex) {
        switch (featureIndex) {

            case Context.FEATURE_LOCATION_INFORMATION_IN_ERROR:
                return true;

            case Context.FEATURE_INTEGER_WITHOUT_DECIMAL_PLACE:
                return true;

            case Context.FEATURE_ENABLE_JAVA_MAP_ACCESS:
                return true;
            
            case Context.FEATURE_THREAD_SAFE_OBJECTS:
                return true;
        }
        // defaults
        return super.hasFeature(cx, featureIndex);
    }

    @Override
    protected Context makeContext() {
        Context cx = super.makeContext();
        
// Un-comment for 1.8.0
//        cx.setInterpretedMode(true); // must use interpreter mode for continuations to work
//        if (cx.getLanguageVersion() != Context.VERSION_ECMASCRIPT) {
//            cx.setLanguageVersion(Context.VERSION_ECMASCRIPT);
//        }
        
// Un-comment for 1.7.XX
        cx.setOptimizationLevel(-1);
        if ( cx.getLanguageVersion() != Context.VERSION_ES6 ) {
            cx.setLanguageVersion(Context.VERSION_ES6);
        }
      
        cx.setWrapFactory(BPjsWrapFactory.INSTANCE);
        return cx;
    }
}

/**
 * Returns java objects for primitive types, to allow easier state-based comparison.
 * 
 * @author michael
 */
class BPjsWrapFactory extends WrapFactory {
    
    static final BPjsWrapFactory INSTANCE = new BPjsWrapFactory();
    
    @Override
    public Object wrap(Context cx, Scriptable scope, Object obj, Class<?> staticType) {
        if (obj instanceof String || obj instanceof Number || obj instanceof Boolean) {
            return obj;

        } else if (obj instanceof Character) {
            char[] a = {((Character) obj)};
            return new String(a);

        }
        return super.wrap(cx, scope, obj, staticType);
    }
}
