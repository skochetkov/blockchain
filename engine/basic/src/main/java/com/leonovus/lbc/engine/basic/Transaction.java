package com.leonovus.lbc.engine.basic;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

public class Transaction {
    /**
     * Input of the transaction is output of a previous transaction.
     * Definition
     * An output in a transaction which contains two fields:
     * a value field for transferring zero or more satoshis and a pubkey script for indicating what conditions must be fulfilled for those satoshis to be further spent.
     */
    public class Input {
        /** hash of the Transaction whose output is being used */
        public byte[] prevTxHash;
        /** used output's index in the previous transaction */
        public int outputIndex;
        /** the signature produced to check validity */
        public byte[] signature;

        public Input(byte[] prevHash, int index) {
            if (prevHash == null)
                prevTxHash = null;
            else
                prevTxHash = Arrays.copyOf(prevHash, prevHash.length);
            outputIndex = index;
        }

        public void addSignature(byte[] sig) {
            if (sig == null)
                signature = null;
            else
                signature = Arrays.copyOf(sig, sig.length);
        }
    }

    /**
     * Definition
     * An output in a transaction which contains two fields: a value field for transferring zero or more satoshis and a pubkey script
     * for indicating what conditions must be fulfilled for those satoshis to be further spent.
     *
     * A single transaction can create multiple outputs, as would be the case when sending to multiple addresses,
     * but each output of a particular transaction can only be used as an input once in the block chain.
     * Any subsequent reference is a forbidden double spend—an attempt to spend the same satoshis twice.
     *
     * Outputs are tied to transaction identifiers (TXIDs), which are the hashes of signed transactions.
     */
    public class Output {
        /** value in bitcoins of the output */
        public double value;
        /** the address or public key of the recipient */
        public PublicKey address;

        public Output(double v, PublicKey addr) {
            value = v;
            address = addr;
        }
    }

    /** hash of the transaction, its unique id */
    private byte[] hash;
    private ArrayList<Input> inputs;
    private ArrayList<Output> outputs;

    public Transaction() {
        inputs = new ArrayList<Input>();
        outputs = new ArrayList<Output>();
    }

    public Transaction(Transaction tx) {
        hash = tx.hash.clone();
        inputs = new ArrayList<Input>(tx.inputs);
        outputs = new ArrayList<Output>(tx.outputs);
    }

    public void addInput(byte[] prevTxHash, int outputIndex) {
        Input in = new Input(prevTxHash, outputIndex);
        inputs.add(in);
    }

    public void addOutput(double value, PublicKey address) {
        Output op = new Output(value, address);
        outputs.add(op);
    }

    public void removeInput(int index) {
        inputs.remove(index);
    }

    public void removeInput(UTXO ut) {
        for (int i = 0; i < inputs.size(); i++) {
            Input in = inputs.get(i);
            UTXO u = new UTXO(in.prevTxHash, in.outputIndex);
            if (u.equals(ut)) {
                inputs.remove(i);
                return;
            }
        }
    }

    public byte[] getRawDataToSign(int index) {
        // ith input and all outputs
        ArrayList<Byte> sigData = new ArrayList<Byte>();

        if (index > inputs.size()) {
            return null;
        }

        Input in = inputs.get(index);
        byte[] prevTxHash = in.prevTxHash;

        ByteBuffer b = ByteBuffer.allocate(Integer.SIZE / 8);
        b.putInt(in.outputIndex);
        byte[] outputIndex = b.array();

        // The idea of signature is to compose previous output hash of belonging transaction with index (converted to bytes)
        // plus values and addresses (in bytes)
        if (prevTxHash != null) {
            for (int i = 0; i < prevTxHash.length; i++) {
                sigData.add(prevTxHash[i]);
            }
        }

        for (int i = 0; i < outputIndex.length; i++) {
            sigData.add(outputIndex[i]);
        }

        for (Output op : outputs) {
            ByteBuffer bo = ByteBuffer.allocate(Double.SIZE / 8);
            bo.putDouble(op.value);
            byte[] value = bo.array();
            byte[] addressBytes = op.address.getEncoded();

            for (int i = 0; i < value.length; i++) {
                sigData.add(value[i]);
            }

            for (int i = 0; i < addressBytes.length; i++) {
                sigData.add(addressBytes[i]);
            }
        }

        byte[] sigD = new byte[sigData.size()];
        int i = 0;
        for (Byte sb : sigData) {
            sigD[i++] = sb;
        }

        return sigD;
    }

    public void addSignature(byte[] signature, int index) {
        inputs.get(index).addSignature(signature);
    }

    /**
     * Unique ID for the transaction. This is combination of all inputs (index + prev_tx_hash + output_index + signature) and
     * outputs (value + address)
     */
    public byte[] getRawTx() {
        ArrayList<Byte> rawTx = new ArrayList<Byte>();

        for (Input in : inputs) {
            byte[] prevTxHash = in.prevTxHash;
            ByteBuffer b = ByteBuffer.allocate(Integer.SIZE / 8);
            b.putInt(in.outputIndex);
            byte[] outputIndex = b.array();
            byte[] signature = in.signature;

            if (prevTxHash != null) {
                for (int i = 0; i < prevTxHash.length; i++) {
                    rawTx.add(prevTxHash[i]);
                }
            }

            for (int i = 0; i < outputIndex.length; i++) {
                rawTx.add(outputIndex[i]);
            }

            if (signature != null) {
                for (int i = 0; i < signature.length; i++) {
                    rawTx.add(signature[i]);
                }
            }
        }

        for (Output op : outputs) {
            ByteBuffer b = ByteBuffer.allocate(Double.SIZE / 8);
            b.putDouble(op.value);
            byte[] value = b.array();
            byte[] addressBytes = op.address.getEncoded();

            for (int i = 0; i < value.length; i++) {
                rawTx.add(value[i]);
            }

            for (int i = 0; i < addressBytes.length; i++) {
                rawTx.add(addressBytes[i]);
            }
        }

        byte[] tx = new byte[rawTx.size()];
        int i = 0;
        for (Byte b : rawTx)
            tx[i++] = b;
        return tx;
    }

    public void finalize() {
        try {
            // MessageDigest class provides applications the functionality of a message digest algorithm, such as SHA-1 or SHA-256.
            // Message digests are secure one-way hash functions that take arbitrary-sized data and output a fixed-length hash value.
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(getRawTx());
            hash = md.digest();
        } catch (NoSuchAlgorithmException x) {
            x.printStackTrace(System.err);
        }
    }

    public void setHash(byte[] h) {
        hash = h;
    }

    public byte[] getHash() {
        return hash;
    }

    public ArrayList<Input> getInputs() {
        return inputs;
    }

    public ArrayList<Output> getOutputs() {
        return outputs;
    }

    public Input getInput(int index) {
        if (index < inputs.size()) {
            return inputs.get(index);
        }
        return null;
    }

    public Output getOutput(int index) {
        if (index < outputs.size()) {
            return outputs.get(index);
        }
        return null;
    }

    public int numInputs() {
        return inputs.size();
    }

    public int numOutputs() {
        return outputs.size();
    }
}
