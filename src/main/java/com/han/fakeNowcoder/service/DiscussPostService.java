package com.han.fakeNowcoder.service;

import com.han.fakeNowcoder.dao.DiscussPostMapper;
import com.han.fakeNowcoder.entity.DiscussPost;
import com.han.fakeNowcoder.util.SensitiveFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

/**
 * @author imhan
 */
@Service
public class DiscussPostService {

  public static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);

  @Autowired private DiscussPostMapper discussPostMapper;

  @Autowired private SensitiveFilter sensitiveFilter;

  public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode) {
    return discussPostMapper.selectDiscussPosts(userId, offset, limit, orderMode);
  }

  public int findDiscussPostRows(int userId) {
    return discussPostMapper.selectDiscussPostRows(userId);
  }

  public int addDiscussPost(DiscussPost discussPost) {
    if (discussPost == null) {
      logger.info("增加帖子，参数为空");
      throw new IllegalArgumentException("参数不能为空");
    }

    // 转义html标记
    discussPost.setTitle(HtmlUtils.htmlEscape(discussPost.getTitle()));
    discussPost.setContent(HtmlUtils.htmlEscape(discussPost.getContent()));

    // 过滤敏感词
    discussPost.setTitle(sensitiveFilter.filter(discussPost.getTitle()));
    discussPost.setContent(sensitiveFilter.filter(discussPost.getContent()));

    return discussPostMapper.insertDiscussPost(discussPost);
  }

  public DiscussPost findDiscussPostById(int id) {
    return discussPostMapper.selectDiscussPostById(id);
  }

  public int updateCommentCount(int id, int commentCount) {
    return discussPostMapper.updateCommentCount(id, commentCount);
  }

  public int updateType(int id, int type) {
    return discussPostMapper.updateType(id, type);
  }

  public int updateStatus(int id, int status) {
    return discussPostMapper.updateStatus(id, status);
  }

  public int updateScore(int id, double score) {
    return discussPostMapper.updateScore(id, score);
  }
}
