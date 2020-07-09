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
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.core.listeners.NewBestBlockListener;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.net.discovery.MultiplexingDiscovery;
import org.bitcoinj.net.discovery.PeerDiscovery;
import org.bitcoinj.net.discovery.PeerDiscoveryException;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.WalletExtension;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.schildbach.wallet.Constants;

import static org.junit.Assert.assertEquals;

public class InheritanceTest {
    private NetworkParameters networkParameters = TestNet3Params.get();
    private SPVBlockStore spvBlockStore;

    private Wallet ownerWallet;
    private Wallet heirWallet;

    private BlockChain chain;
    private PeerGroup peerGroup;

    private Address ownerAddress;
    private Address heirAddress;
    private String artifactsFolderPath = "./tmp/bitcoinj/";

    private Wallet initWallet(String walletName, String mnemonic) throws IOException {
        String walletFilePath = artifactsFolderPath + walletName + ".wallet";
        File walletFile = new File(walletFilePath);
        Wallet wallet = null;
        if (walletFile.exists()) {
            wallet = createWalletViaAppKit(walletName);
        } else {
            wallet = createWalletFromMnemonic(mnemonic);
            wallet.saveToFile(walletFile);
            wallet = createWalletViaAppKit(walletName);
        }
        return wallet;
    }

    private Wallet createWalletViaAppKit(
            String walletName
    ) {
        final WalletAppKit kit = new WalletAppKit(
                networkParameters,
                Script.ScriptType.P2WPKH,
                null,
                new File(artifactsFolderPath),
                walletName
        );

        kit.setAutoSave(true).startAsync();
        kit.awaitRunning();
        //TODO make sure the listener works
        kit.chain().addNewBestBlockListener(Threading.SAME_THREAD, new NewBestBlockListener() {
            @Override
            public void notifyNewBestBlock(StoredBlock block) throws VerificationException {
                int height = block.getHeight();
                if (height % (networkParameters.getInterval() / 2016) == 0) {
                    printBlockParams(block);
                }
            }
        });

        return kit.wallet();
    }

    private Wallet createWalletFromMnemonic(String mnemonic) {
        DeterministicSeed deterministicSeed = null;
        try {
            deterministicSeed = new DeterministicSeed(mnemonic, null, "", 0);
        } catch (UnreadableWalletException ex) {
            ex.printStackTrace();
        }
        return Wallet.fromSeed(networkParameters, deterministicSeed, Script.ScriptType.P2WPKH);
    }

    private void printBlockParams(StoredBlock block) {
        System.out.println(String.format("Checkpointing block %s at height %d, time %s, now %s",
                block.getHeader().getHash(),
                block.getHeight(),
                Utils.dateTimeFormat(block.getHeader().getTime()),
                Utils.dateTimeFormat(new Date())));
    }

    private Wallet createWallet(String walletName, String mnemonic) throws BlockStoreException, UnreadableWalletException {
        String path = "./tmp/bitcoinj/";

        String spvChainFilePath = path + walletName + ".spvchain";
        SPVBlockStore spvBlockStore = new SPVBlockStore(networkParameters, new File(spvChainFilePath));

        String walletFilePath = path + walletName + ".wallet";
        File walletFile = new File(walletFilePath);

        Wallet wallet;
        if (walletFile.exists()) {
            wallet = Wallet.loadFromFile(walletFile, (WalletExtension) null);
        } else {
            wallet = createWalletFromMnemonic(mnemonic);
        }

        BlockChain chain = new BlockChain(networkParameters, wallet, spvBlockStore);
        PeerGroup peerGroup = new PeerGroup(networkParameters, chain);
        peerGroup.addWallet(wallet);
        addPeersToGroup(peerGroup);

        long days = 7;
        long now = new Date().getTime() / 1000;
        final long timeAgo = now - (86400 * days);
        peerGroup.setFastCatchupTimeSecs(timeAgo);

        chain.addNewBestBlockListener(Threading.SAME_THREAD, new NewBestBlockListener() {
            @Override
            public void notifyNewBestBlock(StoredBlock block) throws VerificationException {
                int height = block.getHeight();
                if (height % (networkParameters.getInterval() / 16) == 0 /*&& block.getHeader().getTimeSeconds() <= timeAgo*/) {
                    System.out.println(String.format("Checkpointing block %s at height %d, time %s, now %s",
                            block.getHeader().getHash(),
                            block.getHeight(),
                            Utils.dateTimeFormat(block.getHeader().getTime()),
                            Utils.dateTimeFormat(new Date())));
                }
            }
        });

        peerGroup.start();
        peerGroup.downloadBlockChain();
        wallet.autosaveToFile(walletFile, 500, TimeUnit.MICROSECONDS, null);
        return wallet;
    }

    private void addPeersToGroup(PeerGroup peerGroup) {
        peerGroup.addPeerDiscovery(new PeerDiscovery() {
            private final PeerDiscovery normalPeerDiscovery = MultiplexingDiscovery
                    .forServices(Constants.NETWORK_PARAMETERS, 0);

            @Override
            public InetSocketAddress[] getPeers(final long services, final long timeoutValue,
                                                final TimeUnit timeoutUnit) throws PeerDiscoveryException {
                final List<InetSocketAddress> peers =
                        new LinkedList<>(Arrays.asList(normalPeerDiscovery.getPeers(services, timeoutValue, timeoutUnit)));
                return peers.toArray(new InetSocketAddress[0]);
            }

            @Override
            public void shutdown() {
                normalPeerDiscovery.shutdown();
            }
        });
    }

    @Before
    public void initAll() throws TimeoutException, IOException {
        String mnemonicOwner = "afraid hint enforce alert opinion wrong emotion volume reason ecology garlic galaxy";
        ownerWallet = initWallet("owner", mnemonicOwner);
        ownerAddress = ownerWallet.currentReceiveAddress();

        String mnemonicHeir = "beef elbow expire soccer jar appear dentist below bulk runway invite clever";
        heirWallet = initWallet("heir", mnemonicHeir);
        heirAddress = heirWallet.currentReceiveAddress();
    }

    @After
    public void shutdownAll() {

//        peerGroup.stop();
//        ownerWallet.shutdownAutosaveAndWait();
    }

    @Test
    public void getInterimInheritanceAddress() {
        //TODO check correct address to compare
        Address expectedAddress = Address.fromString(networkParameters, "tb1qnmsx8pey94d46twe4m3xtrgzynve979gj08r35u949smn990g9xqz73taq");
        Address actualAddress = Inheritance.getInterimInheritanceAddressP2WSH(ownerAddress, heirAddress, 6, ownerWallet);
        assertEquals(expectedAddress, actualAddress);
    }

    @Test
    public void inheritanceScriptWithCSV() {
        //TODO check if parameter of OP_CHECKSEQUENCEVERIFY (0xb2) should be 1 or more bytes (6 = 0x56 or 6 blocks = 0x06000000)
        String expectedRedeem = "6376A914C26DE6CA52B29CED22F81113B5AED98EC14587AE88AC670406000000B27576A91424E821FFF252985DC86845551FA99125FB9BCD7588AC68";
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
        System.out.println(ownerWallet.currentReceiveAddress().toString());
        System.out.println(ownerWallet.getBalance());
        Transaction tx = Inheritance.signInheritanceTx(ownerAddress, heirAddress, 6, ownerWallet);
        Inheritance.broadcastTx(tx, ownerWallet);
        System.out.println(tx.toString());
    }

}
