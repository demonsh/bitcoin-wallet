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
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.net.discovery.MultiplexingDiscovery;
import org.bitcoinj.net.discovery.PeerDiscovery;
import org.bitcoinj.net.discovery.PeerDiscoveryException;
import org.bitcoinj.params.RegTestParams;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.schildbach.wallet.Constants;

import static org.junit.Assert.assertEquals;


public class InheritanceTest {
    final private int blocks = 6;

//    private NetworkParameters networkParameters = TestNet3Params.get();
    private NetworkParameters networkParameters = RegTestParams.get();
    private SPVBlockStore spvBlockStore;

    private WalletAppKit ownerWalletAppKit;
    private WalletAppKit heirWalletAppKit;

    private Address ownerAddress;
    private Address heirAddress;
    private String artifactsFolderPath = "./tmp/bitcoinj/";

    private WalletAppKit initWallet(String walletName, String mnemonic) throws IOException {
        String walletFilePath = artifactsFolderPath + walletName + ".wallet";
        File walletFile = new File(walletFilePath);
        if (!walletFile.exists()) {
            createWalletFromMnemonic(mnemonic).saveToFile(walletFile);
        }

        return createWalletAppKit(walletName);
    }

    private WalletAppKit createWalletAppKit(String walletName) {
        final WalletAppKit kit = new WalletAppKit(
                networkParameters,
                Script.ScriptType.P2WPKH,
                null,
                new File(artifactsFolderPath),
                walletName
        );

        kit.connectToLocalHost();
        kit.setAutoSave(true).startAsync();
        kit.awaitRunning();
        //TODO make sure the listener works
        kit.chain().addNewBestBlockListener(Threading.SAME_THREAD, new NewBestBlockListener() {
            @Override
            public void notifyNewBestBlock(StoredBlock block) throws VerificationException {
                int height = block.getHeight();
                if (height % (networkParameters.getInterval() / 1 /*2016*/) == 0) {
                    printBlockParams(block);
                }
            }
        });

        return kit;
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
    public void initAll() throws IOException, InterruptedException {
        List<ChildNumber> path = new ArrayList<ChildNumber>();
        path.add(new ChildNumber(1, true));
        path.add(new ChildNumber(0));
        path.add(new ChildNumber(0));

        InheritanceTestUtils.runShellCommand("/usr/local/bin/docker-compose -f ./test/de/schildbach/wallet/util/docker-compose.yml down --remove-orphans");
        InheritanceTestUtils.runShellCommand("/usr/local/bin/docker-compose -f ./test/de/schildbach/wallet/util/docker-compose.yml up -d");

        TimeUnit.SECONDS.sleep(3);

        try {
            InheritanceTestUtils.runShellCommand("cd ./tmp/bitcoinj/ && rm ownerRegtest.* && rm heirRegtest.*");
        } catch (Exception e) {}

        InheritanceTestUtils.runShellCommand("docker exec --user bitcoin bitcoin-server bitcoin-cli -regtest -rpcuser=user -rpcpassword=pass generatetoaddress 1 bcrt1qcfk7djjjk2ww6ghczyfmttke3mq5tpaw6wsssd", false);
        InheritanceTestUtils.runShellCommand("docker exec --user bitcoin bitcoin-server bitcoin-cli -regtest -rpcuser=user -rpcpassword=pass generatetoaddress 150 mmW7cFg5iVXmApyMGwnnvmnZeHLUL1K4tn", false);

        String mnemonicOwner = "afraid hint enforce alert opinion wrong emotion volume reason ecology garlic galaxy";
        ownerWalletAppKit = initWallet("ownerRegtest", mnemonicOwner);
        Wallet ownerWallet = ownerWalletAppKit.wallet();
        DeterministicKey ownerKey = ownerWallet.getKeyByPath(path);
        ownerAddress = Address.fromKey(networkParameters, ownerKey, Script.ScriptType.P2WPKH);

        String mnemonicHeir = "beef elbow expire soccer jar appear dentist below bulk runway invite clever";
        heirWalletAppKit = initWallet("heirRegtest", mnemonicHeir);
        Wallet heirWallet = heirWalletAppKit.wallet();
        DeterministicKey heirKey = heirWallet.getKeyByPath(path);
        heirAddress = Address.fromKey(networkParameters, heirKey, Script.ScriptType.P2WPKH);

        Address interimAddress = Inheritance.getInterimInheritanceAddressP2WSH(ownerAddress, heirAddress, blocks, ownerWallet);
        ownerWallet.addWatchedAddress(interimAddress);
        heirWallet.addWatchedAddress(interimAddress);
    }

    @After
    public void shutdownAll() {

//        peerGroup.stop();
//        ownerWallet.shutdownAutosaveAndWait();
    }

    @Test
    public void getInterimInheritanceAddress() {
        //TODO check correct address to compare
//        Address expectedAddress = Address.fromString(networkParameters, "tb1qnmsx8pey94d46twe4m3xtrgzynve979gj08r35u949smn990g9xqz73taq");
        Address expectedAddress = Address.fromString(networkParameters, "bcrt1qnmsx8pey94d46twe4m3xtrgzynve979gj08r35u949smn990g9xq08mdg6");
        Address actualAddress = Inheritance.getInterimInheritanceAddressP2WSH(
                ownerAddress,
                heirAddress,
                blocks,
                ownerWalletAppKit.wallet()
        );
        assertEquals(expectedAddress, actualAddress);
    }

    @Test
    public void inheritanceScriptWithCSV() {
        //TODO check if parameter of OP_CHECKSEQUENCEVERIFY (0xb2) should be 1 or more bytes (6 = 0x56 or 6 blocks = 0x06000000)
//        String expectedRedeem = "6376A914C26DE6CA52B29CED22F81113B5AED98EC14587AE88AC670406000000B27576A91424E821FFF252985DC86845551FA99125FB9BCD7588AC68";
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
        Wallet wallet = ownerWalletAppKit.wallet();
        System.out.println(wallet.getBalance());
        Transaction tx = Inheritance.signInheritanceTx(
                ownerAddress, heirAddress, blocks, ownerWalletAppKit.wallet()
        );
        String actualHexTx = InheritanceTestUtils.convertByteToHex(tx.bitcoinSerialize());
//        String expectedHexTx = "010000000001034095D19E21F3DFEE1E9573AC9B3ACE3C392CC69BE281310DFF923B665CF738410000000000FFFFFFFF7242D10B374BCB2850B7CC06170C2B93EBB6B7A3A1FF13362AA53B4BFE2A309B0000000000FFFFFFFF2CB7E97017416A4BFDAC6E757ABE3950477E907FDB23B4E97550FE93EE2F16E90000000000FFFFFFFF020606639B000000001600145C6D22EFE3C7B2C0168B85E87FD4AB6D013988B48E00AEE2020000002200209EE06387242D5B5D2DD9AEE2658D0224D992F8A893CE38D385A961B994AF414C024830450221009409A2BFC27599239F10E6A5A308EAAB5F4AA9FCCA58151A16879A6BF818861B022023E6301BF2D38CD5F1C10DDE88B85C87A725EC494A1E4082CBA451EE9BEB02350121023DBFFA8FF2FDFF7FE009D689967E0CE7AD3CB20C2D8967AB9833620CF46B257B0248304502210098F9EAE28D47B35688E1D000D57940365C37DC3885CECE6EECAD5F75BA8E2ACF02206D5A3957E5FD9258E946BCC18D4C38FCF0A9B9A20D12627E9CA1E094FD177F160121023DBFFA8FF2FDFF7FE009D689967E0CE7AD3CB20C2D8967AB9833620CF46B257B02473044022068D523A7196D40C84E27501ABB985851B29F4B9E8C1EE70A4C97D069D5A2558702206A68B555FC9C0564BF89AC9A8670F4E83A32D770B8D8F54D5AD53A4BCE4AEE020121023DBFFA8FF2FDFF7FE009D689967E0CE7AD3CB20C2D8967AB9833620CF46B257B00000000";
//        assertEquals(expectedHexTx, actualHexTx.toUpperCase());
        Inheritance.broadcastTxViaSendRequest(tx, wallet);
        System.out.println(tx.toString());
    }

    @Test
    public void withdrawFromInterimAddress() throws Exception {
//        for (byte b : ownerKey.getPubKey()) {
//            String st = String.format("%02X", b);
//            System.out.print(st);
//        }


        Transaction txOwner = Inheritance.getWithdrawTxFromInterimAddress(
                ownerAddress,
                heirAddress,
                blocks,
                ownerWalletAppKit.wallet()
        );

//        Transaction txHeir = Inheritance.getWithdrawTxFromInterimAddress(
//            ownerAddress,
//            heirAddress,
//            blocks,
//            heirWalletAppKit.wallet()
//        );

        Inheritance.broadcastTx(txOwner, ownerWalletAppKit.peerGroup());
//        Inheritance.broadcastTx(txHeir, heirWalletAppKit.peerGroup());
    }

    @Test
    public void signInheritanceTxAndWithdraw() throws Exception {
        Wallet ownerWallet = ownerWalletAppKit.wallet();
        Wallet heirWallet = heirWalletAppKit.wallet();

        Transaction tx = Inheritance.signInheritanceTx(
                ownerAddress, heirAddress, blocks, ownerWallet
        );
        Inheritance.broadcastTxViaSendRequest(tx, ownerWallet);
//        System.out.println(tx.toString());
        InheritanceTestUtils.runShellCommand(
                "docker exec --user bitcoin bitcoin-server bitcoin-cli -regtest -rpcuser=user -rpcpassword=pass generatetoaddress "
                            + blocks
                            + " mmW7cFg5iVXmApyMGwnnvmnZeHLUL1K4tn",
                false
        );

        Transaction txOwner = Inheritance.getWithdrawTxFromInterimAddress(
                ownerAddress, heirAddress,blocks, heirWallet
        );

//        System.out.println("Input 0 witness: " + txOwner.toString());
        InheritanceTestUtils.runShellCommand("docker exec --user bitcoin bitcoin-server bitcoin-cli -regtest -rpcuser=user -rpcpassword=pass decoderawtransaction " +
            InheritanceTestUtils.convertByteToHex(txOwner.bitcoinSerialize()));

//        Inheritance.broadcastTx(txOwner, ownerWalletAppKit.peerGroup());
        InheritanceTestUtils.runShellCommand("docker exec --user bitcoin bitcoin-server bitcoin-cli -regtest -rpcuser=user -rpcpassword=pass sendrawtransaction " +
            InheritanceTestUtils.convertByteToHex(txOwner.bitcoinSerialize()));
        InheritanceTestUtils.runShellCommand("docker exec --user bitcoin bitcoin-server bitcoin-cli -regtest -rpcuser=user -rpcpassword=pass generatetoaddress 1 mmW7cFg5iVXmApyMGwnnvmnZeHLUL1K4tn", false);
    }
}
