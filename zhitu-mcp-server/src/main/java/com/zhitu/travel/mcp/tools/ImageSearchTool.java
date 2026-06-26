package com.zhitu.travel.mcp.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ImageSearchTool {

    private static final String API_KEY = "改为你的 API Key";
    private static final String API_URL = "https://api.pexels.com/v1/search";

    @Tool(description = "搜索景点/美食/旅行相关图片")
    public String searchTravelImage(
            @ToolParam(description = "搜索关键词，如'北京故宫'、'西安兵马俑'、'成都火锅'") String query) {
        try {
            List<String> images = searchMediumImages(query, 5);
            if (images.isEmpty()) {
                return "未找到相关图片";
            }
            return String.join("\n", images);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "按城市搜索旅游景点图片")
    public String searchCityImages(
            @ToolParam(description = "城市名称，如'北京'、'西安'、'成都'") String city,
            @ToolParam(description = "图片类别，如'景点'、'美食'、'酒店'，可选") String category) {
        String query = city;
        if (StrUtil.isNotBlank(category)) {
            query = city + " " + category;
        }
        try {
            List<String> images = searchMediumImages(query, 8);
            if (images.isEmpty()) {
                return "未找到" + city + "的相关图片";
            }
            return String.join("\n", images);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public List<String> searchMediumImages(String query, int perPage) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", API_KEY);

        Map<String, Object> params = new HashMap<>();
        params.put("query", query);
        params.put("per_page", perPage);

        String response = HttpUtil.createGet(API_URL)
                .addHeaders(headers)
                .form(params)
                .execute()
                .body();

        return JSONUtil.parseObj(response)
                .getJSONArray("photos")
                .stream()
                .map(photoObj -> (JSONObject) photoObj)
                .map(photoObj -> photoObj.getJSONObject("src"))
                .map(photo -> photo.getStr("medium"))
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }
}
