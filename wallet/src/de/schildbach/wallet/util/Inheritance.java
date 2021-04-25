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
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.TransactionWitness;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.bitcoinj.script.ScriptOpCodes.OP_CHECKSEQUENCEVERIFY;
import static org.bitcoinj.script.ScriptOpCodes.OP_CHECKSIG;
import static org.bitcoinj.script.ScriptOpCodes.OP_DROP;
import static org.bitcoinj.script.ScriptOpCodes.OP_ELSE;
import static org.bitcoinj.script.ScriptOpCodes.OP_ENDIF;
import static org.bitcoinj.script.ScriptOpCodes.OP_IF;

public class Inheritance {

    public static Transaction getWithdrawTxFromInterimAddress(
            ECKey ownerKey,
            ECKey heirKey,
            int blocks,
            Wallet wallet,
            Transaction txToInterimAddress // TODO Check if the tx to interim address can be used instead of watching the interim address to get its info afterwards
    ) throws Exception {
        Address ownerAddress = Address.fromKey(wallet.getParams(), ownerKey, Script.ScriptType.P2WPKH);
        Address heirAddress = Address.fromKey(wallet.getParams(), heirKey, Script.ScriptType.P2WPKH);
        Address addressToWidthrawTo = getAddressToWithdrawTo(ownerAddress, heirAddress, wallet);
        Script redeemScript = inheritanceScriptWithCSV(ownerKey, heirKey, blocks);

        Transaction tx = new Transaction(wallet.getNetworkParameters());
        tx.setVersion(2);

        InterimAddressInfo interimAddressInfo = getInterimAddressInfo(ownerKey, heirKey, blocks, wallet);
        List<TransactionInput> inputs = getAllInputsToWithdrawFromInterimAddress(
            interimAddressInfo.unspendTransactionOutputs,
            wallet.getParams(),
            redeemScript
        );

        tx.addOutput(
            getMaxBalanceToWithdrawExceptFee(tx, interimAddressInfo.balance, wallet),
            addressToWidthrawTo
        );

        for(int i = 0; i < inputs.size(); i++) {
            TransactionWitness witness = new TransactionWitness(3);
            tx.addInput(inputs.get(i));
            if (addressToWidthrawTo == heirAddress) {
                tx.getInput(0).setSequenceNumber(blocks);
            }

            TransactionSignature txSig = tx.calculateWitnessSignature(
                    i,
                    wallet.findKeyFromAddress(addressToWidthrawTo),
                    redeemScript,
                    inputs.get(i).getValue(),
                    Transaction.SigHash.ALL,
                    false
            );

            byte[] preRedeemOpcode = addressToWidthrawTo == ownerAddress ? new byte[]{(byte)0x01} : new byte[]{};
            witness.setPush(0, txSig.encodeToBitcoin());
            witness.setPush(1, preRedeemOpcode);
            witness.setPush(2, redeemScript.getProgram());

            tx.getInput(0).setWitness(witness);
        }

        return tx;
    }

    public static void broadcastTxViaSendRequest(Transaction tx, Wallet wallet) {
        try {
            wallet.sendCoins(SendRequest.forTx(tx));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static void broadcastTx(Transaction tx, PeerGroup peerGroup) {
        peerGroup.broadcastTransaction(tx);
    }

    public static InterimAddressInfo getInterimAddressInfo(
            ECKey ownerKey,
            ECKey heirKey,
            int blocks,
            Wallet wallet
    ) {
        InterimAddressInfo interimAddressInfo = new InterimAddressInfo();
        Address interimAddress = getInterimInheritanceAddressP2WSH(ownerKey, heirKey, blocks, wallet);
        //TODO make sure if there is a better way to get address balance than AddressBalance class from Internet
        AddressBalance addressBalance = new AddressBalance(interimAddress);
        interimAddressInfo.balance = wallet.getBalance(addressBalance);
        interimAddressInfo.blockTillDeadline = 0;  //TODO define real blockTillDeadline
        interimAddressInfo.unspendTransactionOutputs = addressBalance.select(
                wallet.getNetworkParameters().MAX_MONEY,
                wallet.getWatchedOutputs(true)
        ).gathered;

        return interimAddressInfo;
    }

    public static Address getInterimInheritanceAddressP2WSH(
        ECKey ownerKey,
        ECKey heirKey,
        int blocks,
        Wallet wallet
    ) {
        Script redeemScript = inheritanceScriptWithCSV(ownerKey, heirKey, blocks);
        Script script = ScriptBuilder.createP2WSHOutputScript(redeemScript);
        NetworkParameters params = wallet.getParams();
        return script.getToAddress(params);
    }

    public static Transaction signInheritanceTx(
            ECKey ownerKey,
            ECKey heirKey,
            int blocks,
            Wallet wallet
    ) throws Exception {
        Address interimAddress = getInterimInheritanceAddressP2WSH(ownerKey, heirKey, blocks, wallet);
        //TODO inherit all available balance minus fee
        Coin allAvailableBalance = wallet.getBalance();
        Coin valueToInherit = allAvailableBalance.divide(10);

        try {
            return  wallet.createSend(interimAddress, valueToInherit);
        } catch (InsufficientMoneyException exception)  {
            throw new Exception(exception.getMessage());
        }
    }

    public static Script inheritanceScriptWithCSV(ECKey ownerKey, ECKey heirKey, int blocks) {
        ScriptBuilder builder = new ScriptBuilder();

        builder.op(OP_IF);
        builder.data(ownerKey.getPubKey());
        builder.op(OP_ELSE);
        builder.data(ByteBuffer.allocate(4).putInt(blocks).array());
        builder.op(OP_CHECKSEQUENCEVERIFY);
        builder.op(OP_DROP);
        builder.data(heirKey.getPubKey());
        builder.op(OP_ENDIF);
        builder.op(OP_CHECKSIG);

        return builder.build();
    }

    public static byte[] convertHexToByte(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }

    private static Coin getMaxBalanceToWithdrawExceptFee(Transaction tx, Coin balance, Wallet wallet) {
        return balance.subtract(Coin.valueOf(10000)); //TODO substract real fee based on size and satoshis per byte
    }

    private static List<TransactionInput> getAllInputsToWithdrawFromInterimAddress(
            Collection<TransactionOutput> outputs,
            NetworkParameters networkParameters,
            Script redeemScript
    ) {
        List<TransactionInput> inputs = new ArrayList<>();

        for(TransactionOutput output : outputs) {

            TransactionInput input = new TransactionInput(
                    networkParameters,
                    output.getParentTransaction(),
                    redeemScript.getProgram(),
                    new TransactionOutPoint(networkParameters, output.getIndex(), output.getParentTransaction()),
                    output.getValue()
            );
            input.clearScriptBytes();
            inputs.add(input);
        }

        return inputs;
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
