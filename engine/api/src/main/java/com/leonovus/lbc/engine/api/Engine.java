package com.leonovus.lbc.engine.api;

public interface Engine{

    void init(Ledger ledger);

    void onResourceCreated(Resource resource);

    void printLedger();

    void destroy();
}
