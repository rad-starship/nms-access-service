package com.rad.server.access.presistance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rad.server.access.entities.settings.Settings;
import org.apache.http.HttpHost;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.*;

public class EsConnectionHandler {

    //The config parameters for the connection
    private static final String HOST = "localhost";
    private static final int PORT_ONE = 9200;
    private static final String SCHEME = "http";

    private static RestHighLevelClient restHighLevelClient;
    private static ObjectMapper objectMapper = new ObjectMapper();

    private static final String SETTINGS_INDEX = "settings";



    public static synchronized RestHighLevelClient makeConnection() {

        if(restHighLevelClient == null) {
            restHighLevelClient = new RestHighLevelClient(
                    RestClient.builder(
                            new HttpHost(HOST, PORT_ONE, SCHEME)));
        }

        return restHighLevelClient;
    }

    public static synchronized void closeConnection() throws IOException {
        restHighLevelClient.close();
        restHighLevelClient = null;
    }

    public static Settings saveSettings(Settings data){
        String dataMap = null;
        dataMap = data.getJson();
        IndexRequest indexRequest = new IndexRequest(SETTINGS_INDEX);
        indexRequest.source(dataMap, XContentType.JSON);
        try {
            IndexResponse response = restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
            System.out.println(response.toString());
        } catch(ElasticsearchException e) {
            e.getDetailedMessage();
        } catch (java.io.IOException ex){
            ex.getLocalizedMessage();
        }
        return data;
    }

    public static void deleteSettings(){
        DeleteIndexRequest request = new DeleteIndexRequest(SETTINGS_INDEX);
        try {
            restHighLevelClient.indices().delete(request, RequestOptions.DEFAULT);
        } catch (ElasticsearchException exception) {
            if (exception.status() == RestStatus.NOT_FOUND) {
                System.out.println("NotFound Settings");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static Map<String, Object> loadSettings(){
        SearchRequest searchRequest = new SearchRequest(SETTINGS_INDEX);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        searchRequest.source(searchSourceBuilder);
        SearchResponse response = null;
        try {
            response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        catch (ElasticsearchException e){
            e.getDetailedMessage();
            return null;
        }
        SearchHit[] searchHits = response.getHits().getHits();
        if(searchHits.length > 0){
            return searchHits[0].getSourceAsMap();
        }
        else
            return null;



    }
}
