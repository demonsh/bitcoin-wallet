/*
 * Copyright the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.schildbach.wallet.util;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptError;
import org.bitcoinj.script.ScriptException;
import org.bitcoinj.signers.LocalTransactionSigner;
import org.bitcoinj.signers.TransactionSigner;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;

import java.nio.ByteBuffer;

import static org.bitcoinj.script.ScriptOpCodes.OP_CHECKSEQUENCEVERIFY;
import static org.bitcoinj.script.ScriptOpCodes.OP_CHECKSIG;
import static org.bitcoinj.script.ScriptOpCodes.OP_DROP;
import static org.bitcoinj.script.ScriptOpCodes.OP_DUP;
import static org.bitcoinj.script.ScriptOpCodes.OP_ELSE;
import static org.bitcoinj.script.ScriptOpCodes.OP_ENDIF;
import static org.bitcoinj.script.ScriptOpCodes.OP_EQUALVERIFY;
import static org.bitcoinj.script.ScriptOpCodes.OP_HASH160;
import static org.bitcoinj.script.ScriptOpCodes.OP_IF;

public class Inheritance {
    //TODO int blocks should be in internal class state

    private static Address getHeirAddress() {
        //TODO network params should be parametrized depending on mainnet/testnet wallet mode
        NetworkParameters networkParameters = new TestNet3Params();
        //TODO return address from storage or (may be better) from internal class state
        return Address.fromString(networkParameters, "tb1qj6jh32uhuy6jn8muryl77pysqscy7cr86m5vxv");
    }

    private static Address getOwnerAddress() {
        //TODO network params should be parametrized depending on mainnet/testnet wallet mode
        NetworkParameters networkParameters = new TestNet3Params();
        //TODO return address from storage or (may be better) from internal class state
        return Address.fromString(networkParameters, "tb1qj6jh32uhuy6jn8muryl77pysqscy7cr86m5vxv");
    }

    public static void withdrawFromInterimAddress(
            Address ownerAddress,
            Address heirAddress,
            int blocks,
            Wallet wallet
    ) throws Exception {
        Address addressToWidthrawTo = getAddressToWithdrawTo(ownerAddress, heirAddress, wallet);
        Coin totalInheritedBalance = getInterimAddressInfo(ownerAddress, heirAddress, blocks, wallet).balance;

        //TODO switch to real transaction when local storage is ready
//        Transaction inheritnaceTx = getInheritanceTransactionFromLocalStorage();
        Transaction inheritanceTx = signInheritanceTx(ownerAddress, heirAddress, blocks, wallet);

        Transaction tx = new Transaction(wallet.getNetworkParameters());

        TransactionInput input = createInputToWithdrawFromInterimAddress(inheritanceTx, ownerAddress, heirAddress, blocks, wallet);
        tx.addInput(input);

        tx.addOutput(Coin.ZERO, addressToWidthrawTo);
        Coin balanceToWithdraw = getMaxBalanceToWithdrawExceptFee(tx, totalInheritedBalance, wallet);
        tx.getOutput(0).setValue(balanceToWithdraw);

        TransactionSigner.ProposedTransaction proposedTransaction = new TransactionSigner.ProposedTransaction(tx);
        LocalTransactionSigner localTransactionSigner = new LocalTransactionSigner();
        localTransactionSigner.signInputs(proposedTransaction, wallet);

        broadcastTx(tx, wallet);
    }

    public static void broadcastTx(Transaction tx, Wallet wallet) {
        try {
            wallet.sendCoins(SendRequest.forTx(tx));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static InterimAddressInfo getInterimAddressInfo(
            Address ownerAddress,
            Address heirAddress,
            int blocks,
            Wallet wallet
    ){
        InterimAddressInfo interimAddressInfo = new InterimAddressInfo();
        Address interimAddress = getInterimInheritanceAddressP2WSH(ownerAddress, heirAddress, blocks, wallet);
        //TODO make sure if there is a better way to get address balance than AddressBalance class from Internet
        interimAddressInfo.balance = wallet.getBalance(new AddressBalance(interimAddress));
        interimAddressInfo.blockTillDeadline = 0;  //TODO define real blockTillDeadline
        return interimAddressInfo;
    }

    public static Address getInterimInheritanceAddressP2WSH(Address ownerAddress, Address heirAddress, int blocks, Wallet wallet) {
        Script redeemScript = inheritanceScriptWithCSV(ownerAddress, heirAddress, blocks);
        Script script = ScriptBuilder.createP2WSHOutputScript(redeemScript);
        NetworkParameters params = wallet.getParams();
        return script.getToAddress(params);
    }

    public static Transaction signInheritanceTx(
            Address ownerAddress,
            Address heirAddress,
            int blocks,
            Wallet wallet
    ) throws Exception {
        Address interimAddress = getInterimInheritanceAddressP2WSH(ownerAddress, heirAddress, blocks, wallet);
        //TODO inherit all available balance minus fee
        Coin allAvailableBalance = wallet.getBalance();
        Coin valueToInherit = allAvailableBalance.divide(10);

        try {
            return  wallet.createSend(interimAddress, valueToInherit);
        } catch (InsufficientMoneyException exception)  {
            throw new Exception(exception.getMessage());
        }
    }

    public static Script inheritanceScriptWithCSV(Address ownerAddress, Address heirAddress, int blocks) {
        ScriptBuilder builder = new ScriptBuilder();

        builder.op(OP_IF);
        builder.op(OP_DUP);
        builder.op(OP_HASH160);
        builder.data(ownerAddress.getHash());
        builder.op(OP_EQUALVERIFY);
        builder.op(OP_CHECKSIG);
        builder.op(OP_ELSE);
        builder.data(encodeBip68Sequence(blocks)); //TODO keep in mind that number of bytes added here may be wrong
        builder.op(OP_CHECKSEQUENCEVERIFY);
        builder.op(OP_DROP);
        builder.op(OP_DUP);
        builder.op(OP_HASH160);
        builder.data(heirAddress.getHash());
        builder.op(OP_EQUALVERIFY);
        builder.op(OP_CHECKSIG);
        builder.op(OP_ENDIF);

        return builder.build();
    }

    private static byte[] encodeBip68Sequence(int blocks) {
        int SEQUENCE_LOCKTIME_MASK = 0x0000ffff;
        if (blocks > SEQUENCE_LOCKTIME_MASK) throw new ScriptException(ScriptError.SCRIPT_ERR_UNSATISFIED_LOCKTIME, "Exceeded max number of blocks!");

        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.putInt(blocks);
        byte[] sequenceBytes = buffer.array();
        byte[] reversedSequenceBytes = new byte[sequenceBytes.length];
        for (int i = 0; i < sequenceBytes.length; i++) {
            reversedSequenceBytes[i] = sequenceBytes[sequenceBytes.length - i - 1];
        }

        return reversedSequenceBytes;
    }

    private static Coin getMaxBalanceToWithdrawExceptFee(Transaction tx, Coin balance, Wallet wallet) {
        return balance.subtract(Coin.valueOf(10000)); //TODO substract real fee based on size and satoshis per byte
    }

    private static TransactionInput createInputToWithdrawFromInterimAddress(
            Transaction inheritanceTx,
            Address ownerAddress,
            Address heirAddress,
            int blocks,
            Wallet wallet
    ) {
        Script redeemScript = inheritanceScriptWithCSV(ownerAddress, heirAddress, blocks);
        NetworkParameters params = wallet.getParams();
        TransactionOutPoint outpoint = new TransactionOutPoint(params, 0, inheritanceTx);
        TransactionInput input = new TransactionInput(params, inheritanceTx, redeemScript.getProgram(), outpoint);

        input.setSequenceNumber(blocks); //TODO blocks, probably, should be encoded into sequence before
        return input;
    }

    private static Address getAddressToWithdrawTo(Address ownerAddress, Address heirAddress, Wallet wallet) throws Exception {
        Address addressToWithdrawTo;
        if (wallet.isAddressMine(ownerAddress)) {
            addressToWithdrawTo = ownerAddress;
        } else if (wallet.isAddressMine(heirAddress)) {
            addressToWithdrawTo = heirAddress;
        } else {
            throw new Exception("Wrong owner or heir address");
        }
        return addressToWithdrawTo;
    }
}
