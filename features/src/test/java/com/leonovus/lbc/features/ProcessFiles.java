package com.leonovus.lbc.features;

import java.math.BigInteger;
import java.security.*;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;

import com.leonovus.lbc.engine.api.Engine;
import com.leonovus.lbc.engine.api.EngineFactory;
import com.leonovus.lbc.engine.api.Resource;
import com.leonovus.lbc.engine.basic.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ProcessFiles {

    private List<EngineFactory> engineFactories;

    private void loadEngineFactories() {

        engineFactories = new ArrayList<>();
        engineFactories.add(new BasicEngineFactory());
    }

    @Before
    public void setUp() {
        loadEngineFactories();
    }

    @Test
    @Ignore
    public void canRunNonExistingEngine() {
        EngineFactory factory = getEngineFactory("someFancyEngine");
        Engine engine = factory.createEngine();

        assertThat(engine, nullValue());
        engine.destroy();
    }

    @Test
    public void canRunExistingEngine() {
        EngineFactory factory = getEngineFactory("basic");
        Engine engine = factory.createEngine();

        assertThat(engine, notNullValue());

        engine.destroy();
    }

    /**
     * Case is that blockchain engine is initialized with empty transaction which is not acceptable
     */
    @Test
    @Ignore
    public void canNotRunBasicEngineWithEmptyTransaction() {
        EngineFactory factory = getEngineFactory("basic");
        Engine basicEngine = factory.createEngine();
        basicEngine.init(null);

        BasicResource resource = new BasicResource();

        basicEngine.onResourceCreated(resource);

        basicEngine.destroy();
    }

    /**
     * Bob will initialize genesis tokens and give them to Alice
     * @throws NoSuchAlgorithmException
     * @throws SignatureException
     */
    @Test
    public void canRunBasicEngineWithSimpleGoodTransaction() throws NoSuchAlgorithmException, SignatureException {
        EngineFactory factory = getEngineFactory("basic");
        Engine basicEngine = factory.createEngine();

        assertThat(factory, notNullValue());
        assertThat(basicEngine, notNullValue());
        /*
         * Generate key pairs, for Bob & Alice.
         */
        KeyPair pk_bob = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        KeyPair pk_alice   = KeyPairGenerator.getInstance("RSA").generateKeyPair();

        // initiate first (root) transaction: Bob creates first 10 tokens and gives them to Alice
        Transaction tx = new Transaction();
        byte [] initialHash = BigInteger.valueOf(0).toByteArray();
        tx.addInput(initialHash, 0);
        tx.addOutput(10, pk_bob.getPublic());
        signTx(tx, pk_bob.getPrivate(), 0);

        // The transaction output of the root transaction is the initial unspent output.
        UTXOPool ledger = new UTXOPool();
        UTXO utxo = new UTXO(tx.getHash(),0);
        ledger.addUTXO(utxo, tx.getOutput(0));

        basicEngine.init(ledger);

        basicEngine.printLedger();

        /*
         * Set up a first test Transaction
         */
        Transaction tx1 = new Transaction();

        // the Transaction.Output of tx at position 0 has a value of 10
        tx1.addInput(tx.getHash(), 0);

        // Token of value 10 is split into 3 coins and sent all of them for simplicity to
        // the same address (Alice)
        tx1.addOutput(5, pk_alice.getPublic());
        tx1.addOutput(3, pk_alice.getPublic());
        tx1.addOutput(2, pk_alice.getPublic());
        // Note that in the real world fixed-point types would be used for the values, not doubles.
        // Doubles exhibit floating-point rounding errors. This type should be for example BigInteger
        // and denote the smallest coin fractions (Satoshi in Bitcoin).

        // There is only one (at position 0) Transaction.Input in tx2
        // and it contains the coin from Bob, therefore I have to sign with the private key from Bob
        signTx(tx1, pk_bob.getPrivate(), 0);

        Transaction [] txs = new Transaction[1];
        txs[0] = tx1;

        BasicResource resource = new BasicResource();
        resource.setProposedTransactions(txs);

        basicEngine.onResourceCreated(resource);

        basicEngine.printLedger();

        /*
         * Set up a second test Transaction
         */
        Transaction tx2 = new Transaction();

        // the Transaction.Output of tx at position 0 has a value of 3
        tx2.addInput(tx1.getHash(), 1);

        // Token of value 5 is split into 2 coins and sent to two new persons: Tim and Tam
        KeyPair pk_tim   = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        KeyPair pk_tam   = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        tx2.addOutput(2, pk_tim.getPublic());
        tx2.addOutput(1, pk_tam.getPublic());

        // There is only one (at position 0) Transaction.Input in tx2
        // and it contains the coin from Alice, therefore I have to sign with the private key from Alice
        signTx(tx2, pk_alice.getPrivate(), 0);

        txs = new Transaction[1];
        txs[0] = tx2;

        resource = new BasicResource();
        resource.setProposedTransactions(txs);

        basicEngine.onResourceCreated(resource);

        basicEngine.printLedger();

        basicEngine.destroy();
    }
    /**
     * Test Utilities
     */
    private EngineFactory getEngineFactory(String engineName) {
        return engineFactories.stream().filter(e -> e.getName().toLowerCase().equals(engineName.toLowerCase()))
                .findFirst()
                .orElseThrow(() -> {
                    final String engineNames = engineFactories.stream().map(EngineFactory::getName)
                            .collect(Collectors.joining(","));
                    return new RuntimeException(engineName + " is not found. Available engines are: " + engineNames);
                });
    }

    public void signTx(Transaction tx, PrivateKey sk, int input) throws SignatureException {
        Signature sig;
        try {
            sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(sk);
            sig.update(tx.getRawDataToSign(input));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
        tx.addSignature(sig.sign(),input);
        // Note that this method is incorrectly named, and should not in fact override the Java
        // object finalize garbage collection related method.
        tx.finalize();
    }
}
