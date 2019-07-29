package com.github.ontio.explorer.statistics.model;

import com.github.ontio.core.governance.PeerPoolItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tbl_node_info_on_chain")
public class NodeInfoOnChain {
    @Id
    @Column(name = "node_rank")
    @GeneratedValue(generator = "JDBC")
    private Integer nodeRank;

    private String name;

    @Column(name = "current_stake")
    private Long currentStake;

    private String progress;

    @Column(name = "detail_url")
    private String detailUrl;

    @Column(name = "public_key")
    private String publicKey;

    private String address;

    private Integer status;

    @Column(name = "init_pos")
    private Long initPos;

    @Column(name = "total_pos")
    private Long totalPos;

    @Column(name = "max_authorize")
    private Long maxAuthorize;

    @Column(name = "node_proportion")
    private String nodeProportion;

    // Self-defined construct method.
    public NodeInfoOnChain(PeerPoolItem item) {
        this.name = "";
        publicKey = item.peerPubkey;
        address = item.address.toBase58();
        status = item.status;
        initPos = item.initPos;
        totalPos = item.totalPos;
        nodeProportion = "";
    }
}