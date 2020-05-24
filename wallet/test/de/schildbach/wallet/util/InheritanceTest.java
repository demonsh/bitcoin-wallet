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
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.Wallet;
import org.junit.Test;

import java.security.SecureRandom;

import static org.junit.Assert.assertEquals;

public class InheritanceTest {
    private NetworkParameters TESTNET = TestNet3Params.get();
    private ECKey ownerKey = DumpedPrivateKey.fromBase58(TESTNET, "cTd6fawai4faiGwZ9e4YaVjLDuaZrYa8uaNvvVauzEBH7cxZxbmk").getKey();
    private ECKey heirKey = DumpedPrivateKey.fromBase58(TESTNET, "cU7PGUMQR19z6JoD1paHZNcwRETchK1u2J9dG7UJiV8LyjBwvHgq").getKey();
    private Address ownerAddress = Address.fromKey(TESTNET, ownerKey, Script.ScriptType.P2WPKH);
    private Address heirAddress = Address.fromKey(TESTNET, heirKey, Script.ScriptType.P2WPKH);

    private SecureRandom secureRandom = new SecureRandom();
    private DeterministicSeed deterministicSeed = new DeterministicSeed(secureRandom, 32 * 8, "Pa$$w0rd");
    private Wallet wallet = Wallet.fromSeed(TestNet3Params.get(), deterministicSeed,  Script.ScriptType.P2WPKH);

    @Test
    public void getInterimInheritanceAddress() throws Exception {
        //TODO check correct address to compare
        Address expectedAddress = Address.fromString(TESTNET,"tb1qvhk5zndy9a2wlr7l5n26h2f2vxfpl49qk4vn2xxkramtysmtrxvqy99yj3");
        Address actualAddress = Inheritance.getInterimInheritanceAddressP2WSH(ownerAddress, heirAddress, 6, wallet);
        assertEquals(expectedAddress, actualAddress);
    }

    @Test
    public void inheritanceScriptWithCSV() throws Exception {
        //TODO check if parameter of OP_CHECKSEQUENCEVERIFY (0xb2) should be 1 or more bytes (6 = 0x56 or 6 blocks = 0x06000000)
        String expectedRedeem = "6376A9141A670BCB2A28CC3ED9B60FFABEE71BC3C743EEEC88AC670406000000B27576A914660A3C5321765B2968485FA901299741243E1D4988AC68";
        byte[] actualRedeemBytes = Inheritance.inheritanceScriptWithCSV(ownerAddress, heirAddress, 6).getProgram();
        StringBuilder sb = new StringBuilder();
        for (byte b : actualRedeemBytes) {
            sb.append(String.format("%02X", b));
        }
        String actualRedeem = sb.toString();

        //TODO check correct redeem to compare
        assertEquals(expectedRedeem.toUpperCase(), actualRedeem.toUpperCase());
    }

    @Test
    public void signInheritanceTx() throws Exception {
        System.out.println(wallet.currentReceiveAddress().toString());
        //TODO find a way to test
//        Transaction tx = Inheritance.signInheritanceTx(ownerAddress, heirAddress, 6, wallet);
//        System.out.println(tx.toString());
    }
}
