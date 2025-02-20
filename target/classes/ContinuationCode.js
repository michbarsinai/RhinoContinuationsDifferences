/* global sout, capt */

let globalVar = "I'm a global var";

function main() {
    
    sout.println(globalVar);
    let messages = new Set(["a","b","c","c","Dee"]);
    messages.forEach( msg => sout.println(`PreCatch: ${msg}`));
    
    capt.capture("capture 1");

    sout.println(globalVar);
    messages.forEach( msg => sout.println(`PostCatch: ${msg}`));
}
