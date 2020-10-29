package com.vow.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.vow.common.to.es.SkuEsModel;
import com.vow.gulimall.search.Constant.EsConstant;
import com.vow.gulimall.search.config.GulimallElasticSearchConfig;
import com.vow.gulimall.search.service.MallSearchService;
import com.vow.gulimall.search.vo.SearchParam;
import com.vow.gulimall.search.vo.SearchResult;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MallSearchServiceImpl implements MallSearchService {

    @Autowired
    RestHighLevelClient restHighLevelClient;

    @Override
    public SearchResult search(SearchParam searchParam) {
        // 1、动态构建出查询需要的DSL语句
        SearchResult searchResult = null;
        // 1、准备检索请求
        SearchRequest searchRequest = buildSearchRequest(searchParam);

        try {
            // 2、执行检索请求
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);

            //3、分析响应数据
            searchResult = buildSearchResult(searchResponse, searchParam);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return searchResult;
    }


    /**
     * 准备检索请求
     * 模糊匹配、过滤（按照属性，分类、品牌、价格区间、库存），排序，分页，高亮，聚合分析
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParam searchParam) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();    // 构建DSL语句
        /**
         * 模糊匹配、过滤（按照属性，分类、品牌、价格区间、库存）
         */
        // 1、构建 bool-query
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // 1.1、must
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            boolQueryBuilder.must(QueryBuilders.matchQuery("skuTitle", searchParam.getKeyword()));
        }
        // 1.2、filter 按照三级分类id过滤
        if (searchParam.getCatalog3Id() != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("catalogId", searchParam.getCatalog3Id()));
        }
        // 1.3 filter 按照品牌id过滤
        if (searchParam.getBrandIds() != null && searchParam.getBrandIds().size() > 0) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("brandId", searchParam.getBrandIds()));
        }
        // 1.4 filter 按照属性过滤
        if (searchParam.getAttrs() != null && searchParam.getAttrs().size() > 0) {
            for (String attrStr : searchParam.getAttrs()) {
                BoolQueryBuilder nestedBoolQuery = QueryBuilders.boolQuery();
                // attr = 1_5寸:8寸
                String[] s = attrStr.split("_");
                String attrId = s[0];   // 检索的属性id
                String[] attrValues = s[1].split(":");  // 检索的属性值
                nestedBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                nestedBoolQuery.must(QueryBuilders.termQuery("attrs.attrValue", attrValues));
                // 每一个必须都得生成一个nested查询
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedBoolQuery, ScoreMode.None);
                boolQueryBuilder.filter();
            }
        }
        // 1.5 filter 按照库存过滤
        if (searchParam.getHasStock() != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("hasStock", searchParam.getHasStock() == 1));
        }
        // 1.6 filter 按照价格区间过滤
        if (!StringUtils.isEmpty(searchParam.getSkuPrice())) {
            //1_500/_500/500_
            RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("skuPrice");
            String[] priceRange = searchParam.getSkuPrice().split("_");
            if (priceRange.length == 2) {
                rangeQueryBuilder.gte(priceRange[0]).lte(priceRange[1]);
            } else if (priceRange.length == 1) {
                if (searchParam.getSkuPrice().startsWith("_")) {
                    rangeQueryBuilder.lte(priceRange[0]);
                }
                if (searchParam.getSkuPrice().endsWith("_")) {
                    rangeQueryBuilder.gte(priceRange[0]);
                }
            }
            boolQueryBuilder.filter(rangeQueryBuilder);
        }

        searchSourceBuilder.query(boolQueryBuilder);

        /**
         * 排序，分页，高亮
         */
        // 2.1、排序
        if (!StringUtils.isEmpty(searchParam.getSort())) {
            String sort = searchParam.getSort();
            String[] sorts = sort.split("_");
            searchSourceBuilder.sort(sorts[0], sorts[1].equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC);
        }
        // 2.2、分页 pageSize: 5
        // pageNum:1    from:0  size:5
        // pageNum:2    from:5  size:5
        // from = (pageNum - 1) * pageSize
        if (searchParam.getPageNum() != null) {
            searchSourceBuilder.from((searchParam.getPageNum() - 1) * EsConstant.PRODUCT_PAGESIZE);
            searchSourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);
        }
        // 2.3、高亮
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuTitle");
            highlightBuilder.preTags("<b style='color:red'>");
            highlightBuilder.postTags("</b>");
            searchSourceBuilder.highlighter(highlightBuilder);
        }

        /**
         * 聚合分析
         */
        //TODO 3.1、品牌聚合
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg");
        brand_agg.field("brandId").size(50);
        // 品牌聚合的子聚合
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        searchSourceBuilder.aggregation(brand_agg);
        //TODO 3.2、分类聚合
        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
        // 分类聚合的子聚合
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        searchSourceBuilder.aggregation(catalog_agg);
        //TODO 3.3、属性聚合
        NestedAggregationBuilder attr_agg = AggregationBuilders.nested("attr_agg", "attrs");
        // 聚合出当前所有的attrId
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId").size(50);
        // 聚合分析出当前attrId对应的属性名attrName
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        // 聚合当前attrId对应的所有可能的属性值attrValue
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        attr_agg.subAggregation(attr_id_agg);
        searchSourceBuilder.aggregation(attr_agg);

        System.out.println(searchSourceBuilder.toString());

        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, searchSourceBuilder);

        return searchRequest;
    }

    /**
     * 构建检索数据
     * @param searchResponse
     * @return
     */
    private SearchResult buildSearchResult(SearchResponse searchResponse, SearchParam searchParam) {
        SearchResult searchResult = new SearchResult();
        // 1、返回的所有查询到的商品
        SearchHits hits = searchResponse.getHits();
        List<SkuEsModel> esModels = new ArrayList<>();
        if (hits != null && hits.getHits().length > 0) {
            for (SearchHit hit : hits.getHits()) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel skuEsModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
                if (!StringUtils.isEmpty(searchParam.getKeyword())) {
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String highlightSkuTitle = skuTitle.getFragments()[0].string();
                    skuEsModel.setSkuTitle(highlightSkuTitle);
                }
                esModels.add(skuEsModel);
            }
        }
        searchResult.setProducts(esModels);

        // 2、当前所有商品涉及到的所有属性信息
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        ParsedNested attr_agg = searchResponse.getAggregations().get("attr_agg");
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");
        for (Terms.Bucket bucket : attr_id_agg.getBuckets()) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            // 得到属性的id
            long attrId = bucket.getKeyAsNumber().longValue();
            // 得到属性的名称
            String attrName = ((ParsedStringTerms) bucket.getAggregations().get("attr_name_agg")).getBuckets().get(0).getKeyAsString();
            // 得到所有属性的值
            List<String> attrValues = ((ParsedStringTerms) bucket.getAggregations().get("attr_value_agg")).getBuckets().stream().map(item -> {
                String keyAsString = item.getKeyAsString();
                return keyAsString;
            }).collect(Collectors.toList());

            attrVo.setAttrId(attrId);
            attrVo.setAttrName(attrName);
            attrVo.setAttrValue(attrValues);

            attrVos.add(attrVo);
        }
        searchResult.setAttrs(attrVos);

        // 3、当前所有商品所涉及到的所有品牌信息
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        ParsedLongTerms brand_agg = searchResponse.getAggregations().get("brand_agg");
        for (Terms.Bucket bucket : brand_agg.getBuckets()) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            // 得到品牌的id
            long brandId = bucket.getKeyAsNumber().longValue();
            // 得到品牌的名称
            String brandName = ((ParsedStringTerms) bucket.getAggregations().get("brand_name_agg")).getBuckets().get(0).getKeyAsString();
            // 得到品牌的图片
            String brandImg = ((ParsedStringTerms) bucket.getAggregations().get("brand_img_agg")).getBuckets().get(0).getKeyAsString();

            brandVo.setBrandId(brandId);
            brandVo.setBrandName(brandName);
            brandVo.setBrandImg(brandImg);

            brandVos.add(brandVo);
        }
        searchResult.setBrands(brandVos);

        // 4、当前商品所涉及到的所有分类信息
        ParsedLongTerms catalog_agg = searchResponse.getAggregations().get("catalog_agg");
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        for (Terms.Bucket bucket : catalog_agg.getBuckets()) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            String keyAsString = bucket.getKeyAsString();
            // 得到分类Id
            catalogVo.setCatalogId(Long.valueOf(keyAsString));
            // 得到分类名
            ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
            String catalogName = catalog_name_agg.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(catalogName);
            catalogVos.add(catalogVo);
        }
        searchResult.setCatalogs(catalogVos);

        // 5、分页信息-页码
        searchResult.setPageNum(searchParam.getPageNum());
        // 6、分页信息-总记录数
        long total = hits.getTotalHits().value;
        searchResult.setTotal(total);
        // 7、分页信息-总页码
        int totalPages = total/EsConstant.PRODUCT_PAGESIZE == 0 ? (int)total/EsConstant.PRODUCT_PAGESIZE : ((int)total/EsConstant.PRODUCT_PAGESIZE) + 1;
        searchResult.setTotalPages(totalPages);
        return searchResult;
    }
}
