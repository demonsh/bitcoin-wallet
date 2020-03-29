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

import java.io.IOException;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Andreas Schildbach
 */
public class WalletUtilsTest {
    @Test
    public void restoreWalletFromProtobufOrBase58() throws Exception {
        WalletUtils.restoreWalletFromProtobuf(getClass().getResourceAsStream("backup-protobuf-testnet"),
                TestNet3Params.get());
    }

    @Test(expected = IOException.class)
    public void restoreWalletFromProtobuf_wrongNetwork() throws Exception {
        WalletUtils.restoreWalletFromProtobuf(getClass().getResourceAsStream("backup-protobuf-testnet"),
                MainNetParams.get());
    }

    @Test
    public void getInterimInheritanceAddress() throws Exception {
        NetworkParameters TESTNET = TestNet3Params.get();
        ECKey ownerKey = DumpedPrivateKey.fromBase58(TESTNET, "cTd6fawai4faiGwZ9e4YaVjLDuaZrYa8uaNvvVauzEBH7cxZxbmk").getKey();
        ECKey heirKey = DumpedPrivateKey.fromBase58(TESTNET, "cU7PGUMQR19z6JoD1paHZNcwRETchK1u2J9dG7UJiV8LyjBwvHgq").getKey();
        byte[] ownerPubKey = ownerKey.getPubKey();
        byte[] heirPubKey = heirKey.getPubKey();

        //TODO check if parameter of OP_CHECKSEQUENCEVERIFY (0xb2) should can be 1 or more bytes (6 = 0x56 or 6 blocks = 0x06000000)
        String expectedRedeem = "6321039ffefe0a744dc21d4d54018e19076563a3b1397c377910820971533658118756ac6756b275210269d80dd300f507c60adc9928290ef9b94ee2362ffabc44da37f3adde3c227b00ac68";
        byte[] actualRedeemBytes = WalletUtils.inheritanceScriptWithCSV(ownerPubKey, heirPubKey, 6).getProgram();
        StringBuilder sb = new StringBuilder();
        for (byte b : actualRedeemBytes) {
            sb.append(String.format("%02X", b));
        }
        String actualRedeem = sb.toString();

        assertEquals(expectedRedeem.toUpperCase(), actualRedeem.toUpperCase());

        Address expectedAddress = Address.fromString(TESTNET,"2MvpqRfKPUnBv5p6QrKoFPvKC6gWw45qvqj");
        Address actualAddress = WalletUtils.getInterimInheritanceAddress(ownerPubKey, heirPubKey, 6);

        assertEquals(expectedAddress, actualAddress);
    }

    @Test
    public void getInterimInheritanceAddress2() throws Exception {
        NetworkParameters TESTNET = TestNet3Params.get();
        ECKey ownerKey = DumpedPrivateKey.fromBase58(TESTNET, "cTd6fawai4faiGwZ9e4YaVjLDuaZrYa8uaNvvVauzEBH7cxZxbmk").getKey();
        ECKey heirKey = DumpedPrivateKey.fromBase58(TESTNET, "cU7PGUMQR19z6JoD1paHZNcwRETchK1u2J9dG7UJiV8LyjBwvHgq").getKey();
        Address ownerAddress = Address.fromKey(TESTNET, ownerKey, Script.ScriptType.P2PKH);
        Address heirAddress = Address.fromKey(TESTNET, heirKey, Script.ScriptType.P2PKH);

        String expectedRedeem = "6321039ffefe0a744dc21d4d54018e19076563a3b1397c377910820971533658118756ac6756b275210269d80dd300f507c60adc9928290ef9b94ee2362ffabc44da37f3adde3c227b00ac68";
        byte[] actualRedeemBytes = WalletUtils.inheritanceScriptWithCSV(ownerAddress, heirAddress, 6).getProgram();
        StringBuilder sb = new StringBuilder();
        for (byte b : actualRedeemBytes) {
            sb.append(String.format("%02X", b));
        }
        String actualRedeem = sb.toString();

//        assertEquals(expectedRedeem.toUpperCase(), actualRedeem.toUpperCase());

        //TODO get correct address to compare
        Address expectedAddress = Address.fromString(TESTNET,"2MvpqRfKPUnBv5p6QrKoFPvKC6gWw45qvqj");
        Address actualAddress = WalletUtils.getInterimInheritanceAddress(ownerAddress, heirAddress, 6);

        assertEquals(expectedAddress, actualAddress);
    }
}
