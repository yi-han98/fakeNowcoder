package com.han.fakeNowcoder.controller;

import com.google.code.kaptcha.Producer;
import com.han.fakeNowcoder.entity.User;
import com.han.fakeNowcoder.service.UserService;
import com.han.fakeNowcoder.util.CommunityCostant;
import com.han.fakeNowcoder.util.CommunityUtil;
import com.han.fakeNowcoder.util.MailClient;
import com.han.fakeNowcoder.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author imhan
 */
@Controller
public class LoginController implements CommunityCostant {

  private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

  @Value("${nowcoderCustom.path.domain}")
  private String domain;

  @Value("${server.servlet.context-path}")
  private String contextPath;

  @Autowired private UserService userService;

  @Autowired private TemplateEngine templateEngine;

  @Autowired private MailClient mailClient;

  @Autowired private Producer kaptchaProducer;

  @Autowired private RedisTemplate redisTemplate;

  @RequestMapping(path = "/login", method = RequestMethod.GET)
  public String getLoginPage() {
    return "/site/login";
  }

  @RequestMapping(path = "/login", method = RequestMethod.POST)
  public String login(
      Model model,
      String username,
      String password,
      String code,
      boolean rememberMe,
      /*HttpSession session,*/
      HttpServletResponse response,
      @CookieValue("kaptchaOwner") String kaptchaOwner) {
    // ???????????????
    /*String kaptcha = (String) session.getAttribute("kaptcha");*/
    String kaptcha = null;
    if (StringUtils.isNotBlank(kaptchaOwner)) {
      String kaptchaKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
      kaptcha = (String) redisTemplate.opsForValue().get(kaptchaKey);
    }

    if (StringUtils.isBlank(kaptcha)
        || StringUtils.isBlank(code)
        || !kaptcha.equalsIgnoreCase(code)) {
      model.addAttribute("codeMsg", "?????????????????????");
      return "/site/login";
    }
    long expiredSeconds = rememberMe ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
    Map<String, Object> map = userService.login(username, password, expiredSeconds);
    if (map.containsKey("ticket")) {
      Cookie cookie = new Cookie("ticket", (String) map.get("ticket"));
      cookie.setPath(contextPath);
      response.addCookie(cookie);
      return "redirect:/index";
    } else {
      model.addAttribute("usernameMsg", map.get("usernameMsg"));
      model.addAttribute("passwordMsg", map.get("passwordMsg"));
      return "/site/login";
    }
  }

  @RequestMapping(path = "/logout", method = RequestMethod.GET)
  public String logout(Model model, @CookieValue("ticket") String ticket) {
    userService.logout(ticket);
    SecurityContextHolder.clearContext();
    return "redirect:/login";
  }

  @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
  public void getKaptcha(HttpServletResponse response /*, HttpSession session*/) {
    // ???????????????
    String text = kaptchaProducer.createText();
    BufferedImage image = kaptchaProducer.createImage(text);

    // ???????????????Session
    /*session.setAttribute("kaptcha", text);*/

    // ??????????????????
    String kaptchaOwner = CommunityUtil.generateUUID();
    Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner);
    cookie.setMaxAge(60);
    cookie.setPath(contextPath);
    response.addCookie(cookie);

    // ???????????????Redis
    String kaptchaKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
    redisTemplate.opsForValue().set(kaptchaKey, text, 60, TimeUnit.SECONDS);

    // ????????????????????????
    response.setContentType("image/png");
    try (OutputStream outputStream = response.getOutputStream()) {
      ImageIO.write(image, "png", outputStream);
    } catch (IOException e) {
      logger.error("?????????????????????" + e.getMessage());
    }
  }

  @RequestMapping(path = "/register", method = RequestMethod.GET)
  public String getRegisterPage() {
    return "/site/register";
  }

  @RequestMapping(path = "/register", method = RequestMethod.POST)
  public String register(Model model, User user) {
    Map<String, Object> map = userService.register(user);
    if (map == null || map.isEmpty()) {
      model.addAttribute("msg", "?????????????????????????????????????????????????????????");
      model.addAttribute("target", "/index");
      return "/site/operate-result";
    } else {
      model.addAttribute("usernameMsg", map.get("usernameMsg"));
      model.addAttribute("passwordMsg", map.get("passwordMsg"));
      model.addAttribute("emailMsg", map.get("emailMsg"));
      return "/site/register";
    }
  }

  /**
   * @param model Spring MVC Model
   * @param userId ????????????
   * @param activationCode ????????????
   * @return ??????????????????????????????????????????????????????
   *     <p>// http://locahost:8080/nowcoder/activation/101/code
   */
  @RequestMapping(path = "/activation/{userId}/{activationCode}", method = RequestMethod.GET)
  public String activation(
      Model model,
      @PathVariable("userId") int userId,
      @PathVariable("activationCode") String activationCode) {
    int result = userService.activation(userId, activationCode);
    if (result == ACTIVATION_SUCCESS) {
      model.addAttribute("msg", "??????????????????????????????????????????");
      model.addAttribute("target", "/login");
    } else if (result == ACTIVATION_REPEAT) {
      model.addAttribute("msg", "??????????????????????????????????????????");
      model.addAttribute("target", "/index");
    } else {
      model.addAttribute("msg", "?????????????????????");
      model.addAttribute("target", "/index");
    }
    return "/site/operate-result";
  }

  @RequestMapping(path = "/forget", method = RequestMethod.GET)
  public String getForgetPage() {
    return "/site/forget";
  }

  // ???????????????
  @RequestMapping(path = "/forget/code", method = RequestMethod.GET)
  @ResponseBody
  public String getForgetCode(String email, HttpSession session) {
    if (StringUtils.isBlank(email)) {
      return CommunityUtil.getJSONString(1, "?????????????????????");
    }

    // ????????????
    Context context = new Context();
    context.setVariable("email", email);
    String code = CommunityUtil.generateUUID().substring(0, 4);
    context.setVariable("verifyCode", code);
    String content = templateEngine.process("/mail/forget", context);
    mailClient.sendMail(email, "????????????", content);

    // ???????????????
    session.setAttribute("verifyCode", code);

    return CommunityUtil.getJSONString(0);
  }

  // ????????????
  @RequestMapping(path = "/forget/password", method = RequestMethod.POST)
  public String resetPassword(
      String email, String verifyCode, String password, Model model, HttpSession session) {
    String code = (String) session.getAttribute("verifyCode");
    if (StringUtils.isBlank(verifyCode)
        || StringUtils.isBlank(code)
        || !code.equalsIgnoreCase(verifyCode)) {
      model.addAttribute("codeMsg", "???????????????!");
      return "/site/forget";
    }

    Map<String, Object> map = userService.resetPassword(email, password);
    if (map.containsKey("user")) {
      return "redirect:/login";
    } else {
      model.addAttribute("emailMsg", map.get("emailMsg"));
      model.addAttribute("passwordMsg", map.get("passwordMsg"));
      return "/site/forget";
    }
  }
}
