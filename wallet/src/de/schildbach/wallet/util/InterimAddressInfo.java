package de.schildbach.wallet.util;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.TransactionOutput;

import java.util.Collection;

public class InterimAddressInfo {
    public Coin balance;
    public int blockTillDeadline;
    public Collection<TransactionOutput> unspendTransactionOutputs;
}
