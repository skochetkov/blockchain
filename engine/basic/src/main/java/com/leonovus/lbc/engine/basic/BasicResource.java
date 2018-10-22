package com.leonovus.lbc.engine.basic;

        import com.leonovus.lbc.engine.api.Resource;

public class BasicResource implements Resource {
    private Transaction [] proposedTansactions;

    public void setProposedTransactions(Transaction [] txs) {
        this.proposedTansactions = txs;
    }

    public Transaction[] getProposedTransactions() {
        return proposedTansactions;
    }
}
