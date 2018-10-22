package com.leonovus.lbc.engine.basic;

import com.leonovus.lbc.engine.api.Engine;
import com.leonovus.lbc.engine.api.Ledger;
import com.leonovus.lbc.engine.api.Resource;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class BasicEngine implements Engine {

    //private static final Logger LOG = LoggerFactory.getLogger(BasicEngine.class);
    private TxHandler txHandler;

    @Override
    public void init(Ledger ledger) {
        //initial pool
        UTXOPool pool;

        if(ledger != null) {
            pool = (UTXOPool)ledger;
        }
        else {
            pool = new UTXOPool();
        }
        txHandler = new TxHandler(pool);
    }

    @Override
    public void onResourceCreated(Resource resource) {

        BasicResource basicResource = (BasicResource) resource;

        Transaction [] proposedTxs = basicResource.getProposedTransactions();

        if(proposedTxs == null || proposedTxs.length == 0) {
            throw new RuntimeException("Transaction is empty or invalid");
        }

        //good transaction to be consumed by somebody?
        Transaction [] goodTxs = txHandler.handleTxs(proposedTxs);
    }

    @Override
    public void printLedger() {
        UTXOPool pool = txHandler.getUTXOPool();

        System.out.println("Ledger : " );

        for(UTXO utxo : pool.getAllUTXO()) {
            Transaction.Output output = pool.getTxOutput(utxo);
            //use hash for hash (address)
            System.out.println(output.value + " received by " + output.address.hashCode());
        }
    }

    @Override
    public void destroy() {

    }
}
