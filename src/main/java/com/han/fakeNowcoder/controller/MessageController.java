package com.han.fakeNowcoder.controller;

import com.han.fakeNowcoder.entity.Message;
import com.han.fakeNowcoder.entity.Page;
import com.han.fakeNowcoder.entity.User;
import com.han.fakeNowcoder.service.MessageService;
import com.han.fakeNowcoder.service.UserService;
import com.han.fakeNowcoder.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(path = "/message")
public class MessageController {

  @Autowired private MessageService messageService;

  @Autowired private HostHolder hostHolder;

  @Autowired private UserService userService;

  @RequestMapping(path = "/list", method = RequestMethod.GET)
  public String getMessageList(Model model, Page page) {
    // 用户信息
    User user = hostHolder.getUser();

    // 分页信息
    page.setLimit(5);
    page.setPath("/message/list");
    page.setRows(messageService.findConversationsCount(user.getId()));

    //  会话列表
    List<Message> conversations =
        messageService.findConversations(user.getId(), page.getOffset(), page.getLimit());
    List<Map<String, Object>> conversationList = new ArrayList<>();
    if (conversations != null) {
      for (Message conversation : conversations) {
        Map<String, Object> map = new HashMap<>();
        map.put("conversation", conversation);
        map.put("messageCount", messageService.findMessagesCount(conversation.getConversationId()));
        map.put(
            "unreadMessageCount",
            messageService.findUnreadMessagesCount(user.getId(), conversation.getConversationId()));
        // 此处的conversation是每个对话的最后一条message，如果当前用户是from，则对方是to，否则相反
        //        int targetId =
        //            user.getId() == conversation.getFromId()
        //                ? conversation.getToId()
        //                : conversation.getFromId();
        //        map.put("target", userService.findUserById(targetId));
        map.put("target", getMessageTarget(conversation.getConversationId()));
        conversationList.add(map);
      }
    }
    model.addAttribute("conversationList", conversationList);

    // 未读消息总数
    int allUnreadMessageCount = messageService.findUnreadMessagesCount(user.getId(), null);

    model.addAttribute("allUnreadMessageCount", allUnreadMessageCount);

    return "/site/letter";
  }

  @RequestMapping(path = "/detail/{conversationId}", method = RequestMethod.GET)
  public String getMessageDetail(
      @PathVariable("conversationId") String conversationId, Model model, Page page) {
    // 用户信息
    User user = hostHolder.getUser();

    // 分页信息
    page.setLimit(5);
    page.setPath("/message/detail/" + conversationId);
    page.setRows(messageService.findMessagesCount(conversationId));

    List<Message> messages =
        messageService.findMessages(conversationId, page.getOffset(), page.getLimit());

    List<Map<String, Object>> messageList = new ArrayList<>();
    if (messages != null) {
      for (Message message : messages) {
        Map<String, Object> map = new HashMap<>();
        map.put("message", message);
        map.put("fromUser", userService.findUserById(message.getFromId()));
        messageList.add(map);
      }
    }
    model.addAttribute("messageList", messageList);
    model.addAttribute("target", getMessageTarget(conversationId));

    return "/site/letter-detail";
  }

  private User getMessageTarget(String conversationId) {
    String[] s = conversationId.split("_");
    int userId0 = Integer.parseInt(s[0]);
    int userId1 = Integer.parseInt(s[1]);

    if (hostHolder.getUser().getId() == userId0) {
      return userService.findUserById(userId1);
    } else {
      return userService.findUserById(userId0);
    }
  }
}