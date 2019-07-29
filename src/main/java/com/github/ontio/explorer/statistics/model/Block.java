/*
 * Copyright (C) 2018 The ontology Authors
 * This file is part of The ontology library.
 * The ontology is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * The ontology is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with The ontology.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.ontio.explorer.statistics.model;

import javax.persistence.*;

@Table(name = "tbl_block")
public class Block {
    /**
     * 区块高度
     */
    @Id
    @Column(name = "block_height")
    private Integer blockHeight;

    /**
     * 区块hash值
     */
    @Column(name = "block_hash")
    private String blockHash;

    /**
     * 区块里交易的merklroot
     */
    @Column(name = "txs_root")
    private String txsRoot;

    /**
     * 区块时间戳
     */
    @Column(name = "block_time")
    private Integer blockTime;

    /**
     * 区块共识数据
     */
    @Column(name = "consensus_data")
    private String consensusData;

    /**
     * 区块的bookkeepers
     */
    private String bookkeepers;

    /**
     * 区块里的交易数量
     */
    @Column(name = "tx_count")
    private Integer txCount;

    /**
     * 区块大小，单位b
     */
    @Column(name = "block_size")
    private Integer blockSize;

    /**
     * 获取区块高度
     *
     * @return block_height - 区块高度
     */
    public Integer getBlockHeight() {
        return blockHeight;
    }

    /**
     * 设置区块高度
     *
     * @param blockHeight 区块高度
     */
    public void setBlockHeight(Integer blockHeight) {
        this.blockHeight = blockHeight;
    }

    /**
     * 获取区块hash值
     *
     * @return block_hash - 区块hash值
     */
    public String getBlockHash() {
        return blockHash;
    }

    /**
     * 设置区块hash值
     *
     * @param blockHash 区块hash值
     */
    public void setBlockHash(String blockHash) {
        this.blockHash = blockHash == null ? null : blockHash.trim();
    }

    /**
     * 获取区块里交易的merklroot
     *
     * @return txs_root - 区块里交易的merklroot
     */
    public String getTxsRoot() {
        return txsRoot;
    }

    /**
     * 设置区块里交易的merklroot
     *
     * @param txsRoot 区块里交易的merklroot
     */
    public void setTxsRoot(String txsRoot) {
        this.txsRoot = txsRoot == null ? null : txsRoot.trim();
    }

    /**
     * 获取区块时间戳
     *
     * @return block_time - 区块时间戳
     */
    public Integer getBlockTime() {
        return blockTime;
    }

    /**
     * 设置区块时间戳
     *
     * @param blockTime 区块时间戳
     */
    public void setBlockTime(Integer blockTime) {
        this.blockTime = blockTime;
    }

    /**
     * 获取区块共识数据
     *
     * @return consensus_data - 区块共识数据
     */
    public String getConsensusData() {
        return consensusData;
    }

    /**
     * 设置区块共识数据
     *
     * @param consensusData 区块共识数据
     */
    public void setConsensusData(String consensusData) {
        this.consensusData = consensusData == null ? null : consensusData.trim();
    }

    /**
     * 获取区块的bookkeepers
     *
     * @return bookkeepers - 区块的bookkeepers
     */
    public String getBookkeepers() {
        return bookkeepers;
    }

    /**
     * 设置区块的bookkeepers
     *
     * @param bookkeepers 区块的bookkeepers
     */
    public void setBookkeepers(String bookkeepers) {
        this.bookkeepers = bookkeepers == null ? null : bookkeepers.trim();
    }

    /**
     * 获取区块里的交易数量
     *
     * @return tx_count - 区块里的交易数量
     */
    public Integer getTxCount() {
        return txCount;
    }

    /**
     * 设置区块里的交易数量
     *
     * @param txCount 区块里的交易数量
     */
    public void setTxCount(Integer txCount) {
        this.txCount = txCount;
    }

    /**
     * 获取区块大小，单位b
     *
     * @return block_size - 区块大小，单位b
     */
    public Integer getBlockSize() {
        return blockSize;
    }

    /**
     * 设置区块大小，单位b
     *
     * @param blockSize 区块大小，单位b
     */
    public void setBlockSize(Integer blockSize) {
        this.blockSize = blockSize;
    }
}