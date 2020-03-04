package com.webank.fabric.front.api.sdk;

import com.webank.fabric.front.commons.pojo.sdk.PeerVO;
import com.webank.fabric.front.commons.utils.FrontUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.impl.GatewayImpl;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

/**
 * service of fabric sdk.
 */
@Slf4j
@Service
public class SdkService {
    private static Channel channel;
    private static Network network;

    @Qualifier(value = "localNetwork")
    @Autowired
    public void setChannel(Network network) {
        Objects.requireNonNull(network, "init SdkService fail. network is null");
        this.network = network;
        this.channel = network.getChannel();
    }

    /**
     * get channel.
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * get HfClient.
     */
    public HFClient getClient() {
        GatewayImpl gateway = (GatewayImpl) network.getGateway();
        return gateway.getClient();
    }

    /**
     * get peers.
     */
    public Collection<Peer> getPeers() {
        Collection<Peer> peersOnChannel = channel.getPeers();
        return FrontUtils.removeDuplicatePeers(peersOnChannel);
    }

    /**
     * get peers by roles.
     */
    public Collection<Peer> getPeers(EnumSet<Peer.PeerRole> roles) {
        Collection<Peer> peersOnChannel = channel.getPeers(roles);
        return FrontUtils.removeDuplicatePeers(peersOnChannel);
    }


    /**
     * get PeerVOs by roles.
     */
    public Collection<PeerVO> getPeerVOs() {
        Collection<PeerVO> peers = Lists.newArrayList();
        //query peers
        Collection<Peer> peersOnChannel = FrontUtils.removeDuplicatePeers(channel.getPeers());
        //foreach peers
        peersOnChannel.stream().forEach(p -> peers.add(createPeerVO(p)));
        return peers;
    }


    /**
     * create object of PeerVO by Peer.
     */
    private PeerVO createPeerVO(Peer peer) {
        String[] protocolDomainPort = peer.getUrl().split(":");
        String domain = FrontUtils.getDomainOfPeer(peer);
        PeerVO peerVO = PeerVO.builder()
                .peerUrl(peer.getUrl())
                .peerIp(FrontUtils.isIP(domain) ? domain : null)
                .peerPort(Integer.valueOf(protocolDomainPort[2]))
                .peerName(peer.getName().split(":")[0])
                .build();
        return peerVO;
    }


    /**
     * get transaction by txId.
     */
    public byte[] getTransactionByTxId(String txId) throws InvalidArgumentException, ProposalException {
        TransactionInfo transactionInfo = channel.queryTransactionByID(txId);
        return transactionInfo.getProcessedTransaction().toByteArray();
    }


    /**
     * get blockInfo by block height.
     */
    public byte[] queryBlockByNumber(Long blockNumber) throws ProposalException, InvalidArgumentException {
        BlockInfo blockInfo = channel.queryBlockByNumber(blockNumber);
        return blockInfo.getBlock().toByteArray();
    }

    /**
     * get blockInfo by block height.
     */
    public byte[] queryBlockByHash(String blockHash) throws ProposalException, InvalidArgumentException {
        BlockInfo blockInfo = channel.queryBlockByHash(blockHash.getBytes());
        return blockInfo.getBlock().toByteArray();
    }


    /**
     * get latest block height of peer.
     */
    public BigInteger getPeerBlockNumber(String peerUrl) throws ProposalException, InvalidArgumentException {
        Collection<Peer> peers = channel.getPeers();
        Peer peer = peers.stream().filter(p -> peerUrl.equals(p.getUrl())).findFirst().get();
        return getPeerBlockHeight(peer);
    }

    /**
     * get latest block number of channel.
     */
    public BigInteger getChannelBlockNumber() throws ProposalException, InvalidArgumentException {
        BlockchainInfo blockchainInfo = channel.queryBlockchainInfo();
        return BigInteger.valueOf(blockchainInfo.getHeight());

    }

    /**
     * get latest block height of peer.
     */
    public BigInteger getPeerBlockHeight(Peer peer) throws ProposalException, InvalidArgumentException {
        BlockchainInfo blockchainInfo = channel.queryBlockchainInfo(peer);
        return BigInteger.valueOf(blockchainInfo.getHeight());

    }

    /**
     * get blockInfo by transactionId.
     */
    public byte[] queryBlockByTransactionId(String transactionId) throws ProposalException, InvalidArgumentException {
        Optional<BlockInfo> blockInfoOptional = Optional.ofNullable(channel.queryBlockByTransactionID(transactionId));
        if (!blockInfoOptional.isPresent())
            return null;
        BlockInfo blockInfo = channel.queryBlockByTransactionID(transactionId);
        return blockInfo.getBlock().toByteArray();
    }

    /**
     * get name of current channel.
     */
    public String getChannelName() {
        return channel.getName();
    }

    /**
     * get chainCodeName list.
     */
    public Collection<String> getDiscoveredChainCodeNames() {
        return channel.getDiscoveredChaincodeNames();
    }
}
