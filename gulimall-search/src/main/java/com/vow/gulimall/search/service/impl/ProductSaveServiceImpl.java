package com.vow.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.vow.common.to.es.SkuEsModel;
import com.vow.gulimall.search.Constant.EsConstant;
import com.vow.gulimall.search.config.GulimallElasticSearchConfig;
import com.vow.gulimall.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductSaveServiceImpl implements ProductSaveService {

    @Autowired
    RestHighLevelClient restHighLevelClient;

    @Override
    public boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException {

        // 保存到ES中
        // 1、给es中建立索引，product，建立好映射关系
        // 2、给es中保存这些数据
        BulkRequest bulkRequest = new BulkRequest();
        for (SkuEsModel skuEsModel : skuEsModels) {
            // 构造保存请求
            IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX);
            indexRequest.id(skuEsModel.getSkuId().toString());
            indexRequest.source(JSON.toJSONString(skuEsModel), XContentType.JSON);
            bulkRequest.add(indexRequest);
        }
        BulkResponse bulkResponse = restHighLevelClient.bulk(bulkRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);

        // 如果批量错误
        boolean hasFailures = bulkResponse.hasFailures();
        List<String> errorItems = Arrays.asList(bulkResponse.getItems()).stream().map(item -> {
            return item.getId();
        }).collect(Collectors.toList());
        log.info("商品上架完成：{}", errorItems);

        return hasFailures;
    }
}
