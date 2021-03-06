package com.han.fakeNowcoder.dao.elasticsearch;

import com.alibaba.fastjson.JSONObject;
import com.han.fakeNowcoder.FakeNowcoderApplication;
import com.han.fakeNowcoder.dao.DiscussPostMapper;
import com.han.fakeNowcoder.entity.DiscussPost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = FakeNowcoderApplication.class)
class DiscussPostRepositoryTest {

  @Autowired private DiscussPostMapper discussPostMapper;

  @Autowired private DiscussPostRepository discussPostRepository;

  @Autowired
  @Qualifier("client")
  private RestHighLevelClient restHighLevelClient;

  @Test
  public void testInsert() {
    discussPostRepository.save(discussPostMapper.selectDiscussPostById(241));
    discussPostRepository.save(discussPostMapper.selectDiscussPostById(242));
    discussPostRepository.save(discussPostMapper.selectDiscussPostById(243));
  }

  @Test
  public void testInsertList() {
    for (int i = 1; i < 200; i++) {
      discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(i, 0, 100, 0));
    }
  }

  @Test
  public void testupdate() {
    DiscussPost discussPost = discussPostMapper.selectDiscussPostById(112);
    discussPost.setContent("????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????");
    discussPostRepository.save(discussPost);
  }

  @Test
  public void testDelete() {
    discussPostRepository.deleteById(112);
  }

  //  @Test
  //  public void testSearchByRepository() {
  //    BaseQuery searchQuery =
  //          new NativeSearchQueryBuilder()
  //              .withQuery(QueryBuilders.multiMatchQuery("???????????????", "title", "content"))
  //              .withSorts(SortBuilders.fieldSort("type").order(SortOrder.DESC))
  //              .withSorts(SortBuilders.fieldSort("score").order(SortOrder.DESC))
  //              .withSorts(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
  //              .withPageable(PageRequest.of(0, 10))
  //              .withHighlightFields(
  //                  new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
  //                  new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>"))
  //              .build();
  //    discussPostRepository.search(searchQuery);
  //  }

  // ?????????????????????
  @Test
  public void noHighlightQuery() throws IOException {
    SearchRequest searchRequest = new SearchRequest("discusspost"); // discusspost???????????????????????????

    // ??????????????????
    SearchSourceBuilder searchSourceBuilder =
        new SearchSourceBuilder()
            // ???discusspost?????????title???content???????????????????????????????????????
            .query(QueryBuilders.multiMatchQuery("offer", "title", "content"))
            // matchQuery????????????????????????key???????????????searchSourceBuilder.query(QueryBuilders.matchQuery(key,value));
            // termQuery??????????????????searchSourceBuilder.query(QueryBuilders.termQuery(key,value));
            .sort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
            .sort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
            .sort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
            // ??????????????????????????????????????????????????????searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
            .from(0) // ???????????????????????????
            .size(10); // ??????????????????????????????

    searchRequest.source(searchSourceBuilder);
    SearchResponse searchResponse =
        restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

    System.out.println(searchResponse.getHits().getTotalHits().value);

    //        System.out.println(JSONObject.toJSON(searchResponse));

    List<DiscussPost> list = new LinkedList<>();
    for (SearchHit hit : searchResponse.getHits().getHits()) {
      DiscussPost discussPost = JSONObject.parseObject(hit.getSourceAsString(), DiscussPost.class);
      //            System.out.println(discussPost);
      list.add(discussPost);
    }
    System.out.println(list.size());
    for (DiscussPost post : list) {
      System.out.println(post);
    }
  }

  // ???????????? ?????????????????????????????????????????????
  @Test
  public void highlightQuery() throws Exception {
    SearchRequest searchRequest = new SearchRequest("discusspost"); // discusspost???????????????????????????
    Map<String, Object> res = new HashMap<>();

    // ??????
    HighlightBuilder highlightBuilder = new HighlightBuilder();
    highlightBuilder.field("title");
    highlightBuilder.field("content");
    highlightBuilder.requireFieldMatch(false);
    highlightBuilder.preTags("<span style='color:red'>");
    highlightBuilder.postTags("</span>");

    // ??????????????????
    SearchSourceBuilder searchSourceBuilder =
        new SearchSourceBuilder()
            .query(QueryBuilders.multiMatchQuery("???????????????", "title", "content"))
            .sort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
            .sort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
            .sort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
            .from(0) // ???????????????????????????
            .size(10) // ??????????????????????????????
            .highlighter(highlightBuilder); // ??????

    searchRequest.source(searchSourceBuilder);

    SearchResponse searchResponse =
        restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
    List<DiscussPost> list = new ArrayList<>();

    long total = searchResponse.getHits().getTotalHits().value;

    for (SearchHit hit : searchResponse.getHits().getHits()) {
      DiscussPost discussPost = JSONObject.parseObject(hit.getSourceAsString(), DiscussPost.class);

      // ???????????????????????????
      HighlightField titleField = hit.getHighlightFields().get("title");

      if (titleField != null) {
        discussPost.setTitle(titleField.getFragments()[0].toString());
      }

      HighlightField contentField = hit.getHighlightFields().get("content");

      if (contentField != null) {
        discussPost.setContent(contentField.getFragments()[0].toString());
      }

      //            System.out.println(discussPost);
      list.add(discussPost);
    }
    res.put("list", list);
    res.put("total", total);
    if (res.get("list") != null) {
      for (DiscussPost post : list = (List<DiscussPost>) res.get("list")) {
        System.out.println(post);
      }
      System.out.println(res.get("total"));
    }
  }
}
