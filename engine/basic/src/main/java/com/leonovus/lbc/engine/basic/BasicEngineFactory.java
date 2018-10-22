package com.leonovus.lbc.engine.basic;

import com.leonovus.lbc.engine.api.Engine;
import com.leonovus.lbc.engine.api.EngineFactory;

public class BasicEngineFactory implements EngineFactory {
    @Override
    public String getName() {
        return "basic";
    }

    @Override
    public Engine createEngine() {
        //We do not want the engine instance to be reassigned (precautionary).
        final BasicEngine engine = new BasicEngine();
        //TODO Do we really want to initialize the engine here?
        //engine.init(null);

        return engine;
    }
}
