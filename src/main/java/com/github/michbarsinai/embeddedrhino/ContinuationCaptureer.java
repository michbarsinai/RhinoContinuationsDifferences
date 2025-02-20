package com.github.michbarsinai.embeddedrhino;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContinuationPending;

/**
 *
 * @author michael
 */
public class ContinuationCaptureer implements java.io.Serializable {
    public void capture(String value) {
      try (var cx = Context.enter()) {
         ContinuationPending pnd = cx.captureContinuation();
         pnd.setApplicationState("Application State: " + value);
         throw pnd;
      }
    }
}
