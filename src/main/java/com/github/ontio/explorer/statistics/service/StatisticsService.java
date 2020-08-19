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

package com.github.ontio.explorer.statistics.service;

import com.alibaba.fastjson.JSON;
import com.github.ontio.explorer.statistics.common.ParamsConfig;
import com.github.ontio.explorer.statistics.common.Constants;
import com.github.ontio.explorer.statistics.mapper.*;
import com.github.ontio.explorer.statistics.model.*;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@NoArgsConstructor
@Service("StatisticsService")
public class StatisticsService {
    private BlockMapper blockMapper;
    private TxDetailTmpMapper txDetailTmpMapper;
    private ContractMapper contractMapper;
    private TxDetailDailyMapper txDetailDailyMapper;
    private DailySummaryMapper dailySummaryMapper;
    private OntidTxDetailMapper ontidTxDetailMapper;
    private AddressDailySummaryMapper addrDailySummaryMapper;
    private ContractDailySummaryMapper contractDailySummaryMapper;
    private ParamsConfig paramsConfig;
    private NodeOverviewHistoryMapper nodeOverviewHistoryMapper;
    private OngSupplyMapper ongSupplyMapper;
    private OntSdkService ontSdkService;

    private static final Integer TIMESTAMP_20200707000000_UTC = 1594080000;

    @Autowired
    public StatisticsService(BlockMapper blockMapper, TxDetailTmpMapper txDetailTmpMapper,
                             ContractMapper contractMapper, TxDetailDailyMapper txDetailDailyMapper,
                             DailySummaryMapper dailySummaryMapper, OntidTxDetailMapper ontidTxDetailMapper,
                             AddressDailySummaryMapper addrDailySummaryMapper, ContractDailySummaryMapper contractDailySummaryMapper,
                             ParamsConfig paramsConfig, NodeOverviewHistoryMapper nodeOverviewHistoryMapper, OngSupplyMapper ongSupplyMapper,
                             OntSdkService ontSdkService) {
        this.blockMapper = blockMapper;
        this.txDetailTmpMapper = txDetailTmpMapper;
        this.contractMapper = contractMapper;
        this.txDetailDailyMapper = txDetailDailyMapper;
        this.dailySummaryMapper = dailySummaryMapper;
        this.ontidTxDetailMapper = ontidTxDetailMapper;
        this.addrDailySummaryMapper = addrDailySummaryMapper;
        this.contractDailySummaryMapper = contractDailySummaryMapper;
        this.paramsConfig = paramsConfig;
        this.nodeOverviewHistoryMapper = nodeOverviewHistoryMapper;
        this.ongSupplyMapper = ongSupplyMapper;
        this.ontSdkService = ontSdkService;
    }

    public void updateDailySummary() {
        try {
            int dailySumTaskBeginTime = getDailySumTaskBeginTime();
            log.info("Current update daily summary task begin time: {}", dailySumTaskBeginTime);
            int currBlockTime = getCurrBlockTime();
            log.info("Current block time: {}", currBlockTime);
            while (currBlockTime > dailySumTaskBeginTime + Constants.ONE_DAY_IN_SEC) {
                cleanTxDetailTmpTbl();
                updateTxDetailTmp(dailySumTaskBeginTime);
                Map<String, Integer> addrCountMap = updateAddrSumTbl(dailySumTaskBeginTime);
                updateDailySumTbl(dailySumTaskBeginTime, addrCountMap);
                dailySumTaskBeginTime += Constants.ONE_DAY_IN_SEC;
            }
            log.info("Current update daily summary task end time: {}", dailySummaryMapper.selectMaxTime());
        } catch (Exception e) {
            log.error("An error occur: ", e);
        }
    }

    public void updateApprovedContractInfo() {
        try {
            int currDailySumEndTime = getDailySumTblMaxTime();
            log.info("Current max time in daily summary table: {}", currDailySumEndTime);
            int taskEndTime = currDailySumEndTime + Constants.ONE_DAY_IN_SEC;
            cleanTxDetailDailyTbl(taskEndTime);
            updateApprovedContract();
        } catch (Exception e) {
            log.error("An error occur: ", e);
        }
    }

    private void cleanTxDetailDailyTbl(int endTime) {
        if (txDetailDailyMapper.selectiveByEndTime(endTime) != 0) {
            int result = txDetailDailyMapper.deleteByEndTime(endTime);
            log.info("Delete {} data in tx detail daily table before: {}", result, endTime);
        }
    }

    private void cleanTxDetailTmpTbl() {
        int result = txDetailTmpMapper.deleteAll();
        log.info("Delete tx detail in tmp table: {}", result);
    }

    private void updateTxDetailTmp(int beginTime) {
        int endTime = beginTime + Constants.ONE_DAY_IN_SEC;
        int result = txDetailTmpMapper.InsertSelectiveFromDetailTable(beginTime, endTime);
        log.info("Insert data from tx detail table: {}", result);
    }

    private int getCurrBlockTime() {
        return blockMapper.selectBlockMaxTime();
    }

    private int getDailySumTblMaxTime() {
        Integer tblMaxTime = dailySummaryMapper.selectMaxTime();
        if (tblMaxTime == null) {
            tblMaxTime = Constants.GENESIS_TIME;
        }
        return tblMaxTime;
    }

    private int getDailySumTaskBeginTime() {
        int summaryTaskBeginTime = getDailySumTblMaxTime();
        if (summaryTaskBeginTime != Constants.GENESIS_TIME) {
            summaryTaskBeginTime += Constants.ONE_DAY_IN_SEC;
        }
        return summaryTaskBeginTime;
    }

    private int getDailyTxSum() {
        return txDetailTmpMapper.selectTxCountInOneDay();
    }

    private void updateDailySumTbl(Integer dailySumTaskBeginTime, Map<String, Integer> addrCountMap) {

        int dailyBlockSum = getDailyBlockSum(dailySumTaskBeginTime);
        int dailyOntIdSum = getDailyOntIdSum(dailySumTaskBeginTime);
        int dailyActiveOntIdSum = getDailyActiveOntIdSum(dailySumTaskBeginTime);

        DailySummary dailySummary = DailySummary.builder()
                .time(dailySumTaskBeginTime)
                .blockCount(dailyBlockSum)
                .txCount(getDailyTxSum())
                .activeOntidCount(dailyActiveOntIdSum)
                .newOntidCount(dailyOntIdSum)
                .ontSum(getDailyOntSum())
                .ongSum(getDailyOngSum())
                .activeAddressCount(addrCountMap.get("dailyActiveAddrCount"))
                .newAddressCount(addrCountMap.get("dailyNewAddrCount"))
                .build();
        dailySummaryMapper.insert(dailySummary);
    }

    private BigDecimal getDailyOntSum() {
        BigDecimal dailyOntAmount = txDetailTmpMapper.selectOntAmountInOneDay();
        return dailyOntAmount == null ? new BigDecimal(0) : dailyOntAmount;
    }

    private BigDecimal getDailyOngSum() {
        BigDecimal dailyOngAmount = txDetailTmpMapper.selectOngAmountInOneDay();
        return dailyOngAmount == null ? new BigDecimal(0) : dailyOngAmount.divide(Constants.ONG_TOTAL, 9, RoundingMode.HALF_DOWN);
    }

    private BigDecimal getDailyOntSum(String contractHash) {
        BigDecimal ontCount = txDetailDailyMapper.selectContractAssetAmount(contractHash, Constants.ONT);
        return ontCount == null ? new BigDecimal(0) : ontCount;
    }

    private BigDecimal getOneDayOntSum(String contractHash) {
        BigDecimal ontCount = txDetailTmpMapper.selectContractAssetAmount(contractHash, Constants.ONT);
        return ontCount == null ? new BigDecimal(0) : ontCount;
    }

    private BigDecimal getOneDayOngSum(String contractHash) {
        BigDecimal ongCount = txDetailTmpMapper.selectContractAssetAmount(contractHash, Constants.ONG);
        return ongCount == null ? new BigDecimal(0) : ongCount.divide(Constants.ONG_TOTAL, 9, RoundingMode.HALF_DOWN);
    }


    private BigDecimal getDailyOngSum(String contractHash) {
        BigDecimal ongCount = txDetailDailyMapper.selectContractAssetAmount(contractHash, Constants.ONG);
        return ongCount == null ? new BigDecimal(0) : ongCount.divide(Constants.ONG_TOTAL, 9, RoundingMode.HALF_DOWN);
    }

    private int getDailyTxSum(String contractHash) {
        return txDetailTmpMapper.selectContractTxSum(contractHash);
    }

    private int getDailyOntIdSum(int beginTime) {
        return ontidTxDetailMapper.selectOntIdCountInOneDay(beginTime, beginTime + Constants.ONE_DAY_IN_SEC, "Register%");
    }

    private int getDailyBlockSum(int beginTime) {
        return blockMapper.selectBlockCountInOneDay(beginTime, beginTime + Constants.ONE_DAY_IN_SEC);
    }

    private int getDailyActiveOntIdSum(int beginTime) {
        return ontidTxDetailMapper.selectActiveOntIdCountInOneDay(beginTime, beginTime + Constants.ONE_DAY_IN_SEC);
    }

    private ContractDailySummary getContractDailySummary(String contractHash) {
        ContractDailySummary contractDailySummary = contractDailySummaryMapper.selectContractSummary(contractHash);
        if (contractDailySummary == null) {
            contractDailySummary = ContractDailySummary.builder()
                    .txCount(0)
                    .newAddressCount(0)
                    .ongSum(Constants.ZERO)
                    .ontSum(Constants.ZERO)
                    .build();
        }
        return contractDailySummary;
    }

    private List<String> getAddrListFromTxDetailTbl4Dapp(String contractHash) {
        return txDetailDailyMapper.selectContractAddr4Dapp(contractHash);
    }

    private List<String> getAddrListFromTxDetailTbl(String contractHash) {
        return txDetailDailyMapper.selectContractAddr(contractHash);
    }

    private List<String> getAddrListFromAddrDailySumTbl(String contractHash) {
        return addrDailySummaryMapper.selectDistinctAddressByContract(contractHash);
    }

    private int getDailyContractNewAddrCount(String contractHash, int dappStoreFlag) {
        List<String> contractAddrList = new ArrayList<>();
        //dapp类型合约,根据from_address+payer计算地址
        if (dappStoreFlag == 1) {
            contractAddrList = getAddrListFromTxDetailTbl4Dapp(contractHash);
        } else {
            //其他类型合约,根据from_address+to_address计算地址
            contractAddrList = getAddrListFromTxDetailTbl(contractHash);
        }
        List<String> fullAddrList = getAddrListFromAddrDailySumTbl(contractHash);
        contractAddrList.removeAll(fullAddrList);
        return contractAddrList.size();
    }

    private String getOepTokenSum(String contractHash, String type) {
        List<Map> oepTokenSum;
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("contractHash", contractHash);
        if (type.equalsIgnoreCase("oep5")) {
            paramMap.put("oep5", "oep5");
            oepTokenSum = txDetailDailyMapper.selectContractTokenAllSum(paramMap);
            if (!isEmptyOrNull(oepTokenSum) && oepTokenSum.size() != 0) {
                Map oep8Map = oepTokenSum.get(0);
                if (!isEmptyOrNull(oep8Map)) {
                    oep8Map.put("asset_name", ((String) oep8Map.get("asset_name")).split(":")[0]);
                }
            }
        } else {
            oepTokenSum = txDetailDailyMapper.selectContractTokenAllSum(paramMap);
        }
        oepTokenSum = oepTokenSum == null ? new ArrayList<>() : oepTokenSum;
        return JSON.toJSONString(oepTokenSum);
    }

    private Integer getDailyTxCount(String contractHash) {
        return txDetailDailyMapper.selectTxCount(contractHash);
    }

    private void updateApprovedContract() {
        List<Contract> contractList = contractMapper.selectAllApprovedContract();
        for (Contract contract : contractList) {
            String type = contract.getType();
            String contractHash = contract.getContractHash();
            int dappStoreFlag = contract.getDappstoreFlag();
            log.info("Staring handle {} contract {} named {} which dApp store flag is {}", type, contractHash, contract.getName(), dappStoreFlag);

            ContractDailySummary contractDailySummary = getContractDailySummary(contractHash);
            contract.setTxCount(contractDailySummary.getTxCount() + getDailyTxCount(contractHash));
            contract.setOntSum(contractDailySummary.getOntSum().add(getDailyOntSum(contractHash)));
            contract.setOngSum(contractDailySummary.getOngSum().add(getDailyOngSum(contractHash)));
            contract.setTokenSum(getOepTokenSum(contractHash, type));
            contract.setAddressCount(contractDailySummary.getNewAddressCount() + getDailyContractNewAddrCount(contractHash, dappStoreFlag));
            contractMapper.updateByPrimaryKeySelective(contract);
        }
    }

    private static Boolean isEmptyOrNull(Object... params) {
        if (params != null) {
            for (Object val : params) {
                if ("".equals(val) || val == null) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private List<AddressDailySummary> updateDailyContractInfoAndContractNewAddress(Integer beginTime) {
        List<Contract> contractList;
        //测试网只更新审核后的合约
        if (paramsConfig.getIsTestNet()) {
            contractList = contractMapper.selectAllApprovedContract();
        } else {
            contractList = contractMapper.selectAll();
        }
        if (contractList.isEmpty()) {
            return new ArrayList<>();
        }
        List<ContractDailySummary> contractSummaryList = new ArrayList<>();
        List<AddressDailySummary> contractAddrSummaryList = new ArrayList<>();
        //记录每个合约当天的统计数据，并将每个合约的新地址插入地址汇总表
        for (Contract contract : contractList) {
            String contractHash = contract.getContractHash();
            BigDecimal dailyOntAmount = getOneDayOntSum(contractHash);
            BigDecimal dailyOngAmount = getOneDayOngSum(contractHash);
            int dailyTxCount = getDailyTxSum(contractHash);

            List<String> dailyActiveAddress;
            //dapp类型合约,根据from_address+payer计算地址
            if (contract.getDappstoreFlag() == 1) {
                dailyActiveAddress = txDetailTmpMapper.selectContractAddr4Dapp(contractHash);
            } else {
                //其他类型合约,根据from_address+to_address计算地址
                dailyActiveAddress = txDetailTmpMapper.selectContractAddr(contractHash);
            }
            int dailyActiveAddrCount = dailyActiveAddress.size();

            List<String> allAddrList = addrDailySummaryMapper.selectDistinctAddressByContract(contractHash);
            dailyActiveAddress.removeAll(allAddrList);
            int dailyNewAddrCount = dailyActiveAddress.size();

            ContractDailySummary contractSummary = ContractDailySummary.builder()
                    .time(beginTime)
                    .dappName(contract.getDappName())
                    .contractHash(contractHash)
                    .ontSum(dailyOntAmount)
                    .ongSum(dailyOngAmount)
                    .txCount(dailyTxCount)
                    .activeAddressCount(dailyActiveAddrCount)
                    .newAddressCount(dailyNewAddrCount)
                    .build();
            contractSummaryList.add(contractSummary);
            for (String address : dailyActiveAddress) {
                AddressDailySummary addressSummary = AddressDailySummary.builder()
                        .time(beginTime)
                        .contractHash(contractHash)
                        .address(address)
                        .build();
                contractAddrSummaryList.add(addressSummary);
            }
        }
        int insertResult = contractDailySummaryMapper.batchInsertSelective(contractSummaryList);
        log.info("Batch insert contract in contract daily summary table: {}", insertResult);
        return contractAddrSummaryList;
    }

    private Map<String, Integer> updateAddrSumTbl(Integer dailySumTaskBeginTime) {

        Map<String, Integer> map = new HashMap<>();

        List<String> dailyAddrRecords = txDetailTmpMapper.selectAddressInOneDay();
        map.put("dailyActiveAddrCount", dailyAddrRecords.size());

        List<String> addressRecords = addrDailySummaryMapper.selectDistinctAddressByContract(Constants.ADDR_DAILY_SUMMARY_NATIVETYPE);
        dailyAddrRecords.removeAll(addressRecords);

        map.put("dailyNewAddrCount", dailyAddrRecords.size());

        List<AddressDailySummary> contractAddrSumList = updateDailyContractInfoAndContractNewAddress(dailySumTaskBeginTime);
        List<AddressDailySummary> addrSummaryList = new ArrayList<>();
        for (String address : dailyAddrRecords) {
            AddressDailySummary addrDailySummary = AddressDailySummary.builder()
                    .time(dailySumTaskBeginTime)
                    .contractHash(Constants.ADDR_DAILY_SUMMARY_NATIVETYPE)
                    .address(address)
                    .build();
            addrSummaryList.add(addrDailySummary);
        }

        addrSummaryList.addAll(contractAddrSumList);
        if (!addrSummaryList.isEmpty()) {
            int insertResult = addrDailySummaryMapper.batchInsertSelective(addrSummaryList);
            log.info("Batch insert address in address daily summary table: {}", insertResult);
        }

        return map;
    }

    public void updateTotalOngSupply() {
        List<OngSupply> ongSupplies = ongSupplyMapper.selectAll();
        if (CollectionUtils.isEmpty(ongSupplies)) {
            // first calculate and save
            List<NodeOverviewHistory> rounds = nodeOverviewHistoryMapper.queryRoundByTimeStamp(TIMESTAMP_20200707000000_UTC);
            Integer cycle = 0;
            BigDecimal roundsOngSupply = BigDecimal.ZERO;
            for (NodeOverviewHistory history : rounds) {
                Integer rndStartTime = history.getRndStartTime();
                Integer rndEndTime = history.getRndEndTime();
                if (rndEndTime != null) {
                    cycle = history.getCycle();
                    Integer startTime = rndStartTime > TIMESTAMP_20200707000000_UTC ? rndStartTime : TIMESTAMP_20200707000000_UTC;
                    int roundOngSupply = rndEndTime - startTime;
                    BigDecimal roundOngSupplyDecimal = new BigDecimal(roundOngSupply);
                    roundsOngSupply = roundsOngSupply.add(roundOngSupplyDecimal);
                }
            }
            if (cycle != 0) {
                OngSupply ongSupply = new OngSupply();
                ongSupply.setOngSupply(roundsOngSupply);
                ongSupply.setCycle(cycle);
                ongSupplyMapper.insertSelective(ongSupply);
            }
        } else {
            // update
            OngSupply ongSupply = ongSupplies.get(0);
            Integer cycle = ongSupply.getCycle();
            BigDecimal ongSupplied = ongSupply.getOngSupply();
            List<NodeOverviewHistory> rounds = nodeOverviewHistoryMapper.queryRoundByCycle(cycle);
            for (NodeOverviewHistory history : rounds) {
                Integer rndStartTime = history.getRndStartTime();
                Integer rndEndTime = history.getRndEndTime();
                if (rndEndTime != null) {
                    cycle = history.getCycle();
                    Integer startTime = rndStartTime > TIMESTAMP_20200707000000_UTC ? rndStartTime : TIMESTAMP_20200707000000_UTC;
                    int roundOngSupply = rndEndTime - startTime;
                    BigDecimal roundOngSupplyDecimal = new BigDecimal(roundOngSupply);
                    ongSupplied = ongSupplied.add(roundOngSupplyDecimal);
                }
            }
            ongSupply.setOngSupply(ongSupplied);
            ongSupply.setCycle(cycle);
            ongSupplyMapper.updateByPrimaryKeySelective(ongSupply);
        }
    }

    public void getBlockHeight() {
        int blockHeight = ontSdkService.getBlockHeight();
        log.info("current block height:{}", blockHeight);
    }
}

