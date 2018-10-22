package com.leonovus.lbc.engine.basic;

import java.util.ArrayList;
import java.util.List;

public class TxHandler {

    private UTXOPool utxoPool;
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = utxoPool;
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        List<Transaction.Input> txInputList = tx.getInputs();

        for(Transaction.Input itx : txInputList) {
            if(!utxoPool.contains(new UTXO(itx.prevTxHash, itx.outputIndex))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {

        List<Transaction> txList = new ArrayList<>();
        //Create UTXOPool for the proposed transactions
        UTXOPool proposedUTXOPool = new UTXOPool();

        //Sort out invalid transactions
        for(Transaction tx : possibleTxs) {
            //TODO - we assume that this is somehow valid for a while
            if(isValidTx(tx)){
                txList.add(tx);
            }
        }

        //create and add unspent transactions outputs to the proposed pool (supposed to be additions to updated ledger)
        for(Transaction tx : txList) {
            //TODO - handle index
            int index = 0;
            for(Transaction.Output output: tx.getOutputs()) {
                UTXO utxo = new UTXO(tx.getHash(), index);
                proposedUTXOPool.addUTXO(utxo, tx.getOutput(index));
                index++;
            }
        }
        //Update internal pool
        utxoPool = new UTXOPool(proposedUTXOPool);

        return txList.stream().toArray(Transaction[] ::new);
    }

    public UTXOPool getUTXOPool() {
        return utxoPool;
    }
}
