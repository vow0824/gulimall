package com.vow.gulimall.search.service;

import com.vow.gulimall.search.vo.SearchParam;
import com.vow.gulimall.search.vo.SearchResult;

public interface MallSearchService {

    /**
     *
     * @param searchParam 检索的所有参数
     * @return  检索的结果
     */
    SearchResult search(SearchParam searchParam);
}
