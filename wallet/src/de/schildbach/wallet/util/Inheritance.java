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
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptError;
import org.bitcoinj.script.ScriptException;
import org.bitcoinj.wallet.Wallet;

import java.nio.ByteBuffer;

import static org.bitcoinj.script.ScriptOpCodes.OP_6;
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
    private static Address getHeirAddress() {
        NetworkParameters networkParameters = new TestNet3Params();
        return Address.fromString(networkParameters, "tb1qj6jh32uhuy6jn8muryl77pysqscy7cr86m5vxv");
    }

    private static Address getOwnerAddress() {
        NetworkParameters networkParameters = new TestNet3Params();
        return Address.fromString(networkParameters, "tb1qj6jh32uhuy6jn8muryl77pysqscy7cr86m5vxv");
    }

    static byte[] encodeBip68Sequence(int blocks) {
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

    public static Address getInterimInheritanceAddressP2WSH(Address ownerAddress, Address heirAddress, int blocks) {
        Script redeemScript = inheritanceScriptWithCSV(ownerAddress, heirAddress, blocks);
        Script script = ScriptBuilder.createP2WSHOutputScript(redeemScript);
        //TODO network params should be parametrized depending on mainnet/testnet wallet mode
        NetworkParameters params = new TestNet3Params();
        return script.getToAddress(params);
    }

    public static Address getInterimInheritanceAddressP2SH(byte[] ownerPubKey, byte[] heirPubKey, int blocks) {
        Script redeemScript = inheritanceScriptWithCSV(ownerPubKey, heirPubKey, blocks);
        Script script = ScriptBuilder.createP2SHOutputScript(redeemScript);
        //TODO network params should be parametrized depending on mainnet/testnet wallet mode
        NetworkParameters params = new TestNet3Params();
        return script.getToAddress(params);
    }

    public static Transaction signInheritanceTx(
        Address ownerAddress,
        Address heirAddress,
        int blocks,
        Wallet wallet
    ) throws Exception {
        Address interimAddress = getInterimInheritanceAddressP2WSH(ownerAddress, heirAddress, blocks);
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
            builder.data(encodeBip68Sequence(blocks));
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

    public static Script inheritanceScriptWithCSV(byte[] ownerPubKey, byte[] heirPubKey, int blocks) {
        ScriptBuilder builder = new ScriptBuilder();

        builder.op(OP_IF);
            builder.data(ownerPubKey);
            builder.op(OP_CHECKSIG);
        builder.op(OP_ELSE);
//            builder.data(encodeBip68Sequence(blocks));
        //TODO get rid of this hardcode. May be change encodeBip68Sequence function to work with builder
            builder.op(OP_6); //OP_6 means push single byte with 0x06 value onto the stack
            builder.op(OP_CHECKSEQUENCEVERIFY);
            builder.op(OP_DROP);
            builder.data(heirPubKey);
            builder.op(OP_CHECKSIG);
        builder.op(OP_ENDIF);

        return builder.build();
    }
}
