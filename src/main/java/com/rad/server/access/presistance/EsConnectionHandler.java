package com.rad.server.access.presistance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rad.server.access.componenets.EsProperties;
import com.rad.server.access.entities.Event;
import com.rad.server.access.entities.SToken;
import com.rad.server.access.entities.settings.Settings;
import org.apache.http.HttpHost;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class EsConnectionHandler {
    @Autowired
    private static EsProperties prop;

    //The config parameters for the connection
    private static final String HOST = "localhost";
    private static final int PORT_ONE = 9200;
    private static final String SCHEME = "http";

    private static RestHighLevelClient restHighLevelClient;
    private static ObjectMapper objectMapper = new ObjectMapper();

    private static final String SETTINGS_INDEX = "settings";
    private static final String EVENTS_INDEX = "events";
    private static final String BLACKLIST_INDEX = "blacklist";


    //***********************************************************************
    //                          Connection finctions
    //***********************************************************************
    public static synchronized RestHighLevelClient makeConnection() {
        //System.out.println("************\n\n\n HOST= "+HOST + "PORT = "+PORT_ONE+"\n\n\n\n************");
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
    //***********************************************************************
    //                          Settings APIs
    //***********************************************************************
    public static Settings saveSettings(Settings data){
        String dataMap = null;
        dataMap = data.toJson();
        saveOnEs(dataMap, SETTINGS_INDEX);
        return data;
    }

    public static void deleteSettings(){
        deleteFromEs(SETTINGS_INDEX);

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


    //***********************************************************************
    //                          Events APIs
    //***********************************************************************
    public static void saveEvent(String eventAsString) {
        saveOnEs(eventAsString, EVENTS_INDEX);


    }

    public static List<Event> loadEventsByTenant(String tenant) {
        SearchSourceBuilder builder = new SearchSourceBuilder().size(1000)
                .query(QueryBuilders.boolQuery()
                        .must(QueryBuilders
                                .matchQuery("tenant",tenant)));

        SearchRequest searchRequest = new SearchRequest(EVENTS_INDEX);
        searchRequest.searchType(SearchType.DFS_QUERY_THEN_FETCH);
        searchRequest.source(builder);
        SearchResponse response = null;
        try {
            response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        catch (ElasticsearchException e){
            e.getDetailedMessage();
            return new ArrayList<>();
        }
        SearchHit[] searchHits = response.getHits().getHits();
        List<Event> results =
                Arrays.stream(searchHits)
                        .map(hit -> {
                            try {
                                Map<String,Object> map = hit.getSourceAsMap();
                                //TODO:not elegant but working..
                                return  new Event(
                                        (String)map.get("clientId"),
                                        (String)map.get("time"),
                                        (String)map.get("tenant"),
                                        (String)map.get("type"),
                                        (String)map.get("error"),
                                        (String)map.get("ip"),
                                        (String)map.get("details")

                                );
                                //               return objectMapper.readValue(hit.getSourceAsString(),Event.class);
                            } catch (Exception e) {
                                e.printStackTrace();
                                return null;
                            }
                        })
                        .collect(Collectors.toList());
        return results;
    }


    //***********************************************************************
    //                          BlackLists APIs
    //***********************************************************************

    public static void saveToken(SToken tokenBlackList) {
        String dataMap = null;
        try {
            dataMap = objectMapper.writeValueAsString(tokenBlackList);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        saveOnEs(dataMap, BLACKLIST_INDEX);

    }

    public static void deleteList() {
        deleteFromEs(BLACKLIST_INDEX);
    }

    public static Set<SToken> loadBlacklist(){
        SearchRequest searchRequest = new SearchRequest(BLACKLIST_INDEX);
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
            Set<SToken> results =
                    Arrays.stream(searchHits)
                            .map(hit -> {
                                try {
                                    Map<String,Object> map = hit.getSourceAsMap();
                                    return new SToken(map.get("token"));
                                    //return objectMapper.readValue(hit.getSourceAsString(),SToken.class);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    return null;
                                }
                            })
                            .collect(Collectors.toSet());
            return results;
        }
        else
            return null;


    }


    //***********************************************************************
    //                          General Functions
    //***********************************************************************
    private static void saveOnEs(String objAsString, String index) {
        IndexRequest indexRequest = new IndexRequest(index);
        indexRequest.source(objAsString, XContentType.JSON);
        try {
            IndexResponse response = restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
            System.out.println(response.toString());
        } catch(ElasticsearchException e) {
            System.out.println(e.getDetailedMessage());
        } catch (IOException ex){
            ex.getLocalizedMessage();
        }
    }
    private static void deleteFromEs(String index) {
        DeleteIndexRequest request = new DeleteIndexRequest(index);
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

    
}
