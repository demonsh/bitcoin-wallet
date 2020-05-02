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
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InheritanceTest {

    @Test
    public void getInterimInheritanceAddress() throws Exception {
        NetworkParameters TESTNET = TestNet3Params.get();
        ECKey ownerKey = DumpedPrivateKey.fromBase58(TESTNET, "cTd6fawai4faiGwZ9e4YaVjLDuaZrYa8uaNvvVauzEBH7cxZxbmk").getKey();
        ECKey heirKey = DumpedPrivateKey.fromBase58(TESTNET, "cU7PGUMQR19z6JoD1paHZNcwRETchK1u2J9dG7UJiV8LyjBwvHgq").getKey();
        byte[] ownerPubKey = ownerKey.getPubKey();
        byte[] heirPubKey = heirKey.getPubKey();
        Address ownerAddress = Address.fromKey(TESTNET, ownerKey, Script.ScriptType.P2WPKH);
        Address heirAddress = Address.fromKey(TESTNET, heirKey, Script.ScriptType.P2WPKH);
        Address expectedAddress = Address.fromString(TESTNET,"2MvpqRfKPUnBv5p6QrKoFPvKC6gWw45qvqj");

        Address actualAddress = Inheritance.getInterimInheritanceAddressP2SH(ownerPubKey, heirPubKey, 6);

        assertEquals(expectedAddress, actualAddress);

        //TODO check correct address to compare
        expectedAddress = Address.fromString(TESTNET,"tb1qvhk5zndy9a2wlr7l5n26h2f2vxfpl49qk4vn2xxkramtysmtrxvqy99yj3");

        actualAddress = Inheritance.getInterimInheritanceAddressP2WSH(ownerAddress, heirAddress, 6);

        assertEquals(expectedAddress, actualAddress);
    }

    @Test
    public void inheritanceScriptWithCSV() throws Exception {
        NetworkParameters TESTNET = TestNet3Params.get();
        ECKey ownerKey = DumpedPrivateKey.fromBase58(TESTNET, "cTd6fawai4faiGwZ9e4YaVjLDuaZrYa8uaNvvVauzEBH7cxZxbmk").getKey();
        ECKey heirKey = DumpedPrivateKey.fromBase58(TESTNET, "cU7PGUMQR19z6JoD1paHZNcwRETchK1u2J9dG7UJiV8LyjBwvHgq").getKey();
        byte[] ownerPubKey = ownerKey.getPubKey();
        byte[] heirPubKey = heirKey.getPubKey();
        Address ownerAddress = Address.fromKey(TESTNET, ownerKey, Script.ScriptType.P2WPKH);
        Address heirAddress = Address.fromKey(TESTNET, heirKey, Script.ScriptType.P2WPKH);

        //TODO check if parameter of OP_CHECKSEQUENCEVERIFY (0xb2) should be 1 or more bytes (6 = 0x56 or 6 blocks = 0x06000000)
        String expectedRedeem = "6321039ffefe0a744dc21d4d54018e19076563a3b1397c377910820971533658118756ac6756b275210269d80dd300f507c60adc9928290ef9b94ee2362ffabc44da37f3adde3c227b00ac68";
        byte[] actualRedeemBytes = Inheritance.inheritanceScriptWithCSV(ownerPubKey, heirPubKey, 6).getProgram();
        StringBuilder sb = new StringBuilder();
        for (byte b : actualRedeemBytes) {
            sb.append(String.format("%02X", b));
        }
        String actualRedeem = sb.toString();

        //TODO check correct redeem to compare
        assertEquals(expectedRedeem.toUpperCase(), actualRedeem.toUpperCase());

        //TODO check if parameter of OP_CHECKSEQUENCEVERIFY (0xb2) should be 1 or more bytes (6 = 0x56 or 6 blocks = 0x06000000)
        expectedRedeem = "6376A9141A670BCB2A28CC3ED9B60FFABEE71BC3C743EEEC88AC670406000000B27576A914660A3C5321765B2968485FA901299741243E1D4988AC68";
        actualRedeemBytes = Inheritance.inheritanceScriptWithCSV(ownerAddress, heirAddress, 6).getProgram();
        sb = new StringBuilder();
        for (byte b : actualRedeemBytes) {
            sb.append(String.format("%02X", b));
        }
        actualRedeem = sb.toString();

        //TODO check correct redeem to compare
        assertEquals(expectedRedeem.toUpperCase(), actualRedeem.toUpperCase());
    }
}
