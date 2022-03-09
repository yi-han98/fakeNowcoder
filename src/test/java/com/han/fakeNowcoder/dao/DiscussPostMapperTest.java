package com.han.fakeNowcoder.dao;

import com.han.fakeNowcoder.FakeNowcoderApplication;
import com.han.fakeNowcoder.entity.DiscussPost;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = FakeNowcoderApplication.class)
class DiscussPostMapperTest {

  @Autowired DiscussPostMapper discussPostMapper;

  @Test
  void selectDiscussPosts() {
    List<DiscussPost> discussPosts = discussPostMapper.selectDiscussPosts(149, 0, 10);
    for (DiscussPost post : discussPosts) {
      System.out.println(post);
    }
  }

  @Test
  void selectDiscussPostRows() {
    int rows = discussPostMapper.selectDiscussPostRows(149);
    System.out.println(rows);
  }
}
