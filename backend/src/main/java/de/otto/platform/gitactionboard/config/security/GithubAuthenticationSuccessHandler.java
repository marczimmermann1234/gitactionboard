package de.otto.platform.gitactionboard.config.security;

import static java.nio.charset.StandardCharsets.UTF_8;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

@Configuration
@Lazy
@Slf4j
@SuppressFBWarnings("EI_EXPOSE_REP2")
public class GithubAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private static final String LOGIN = "login";
  private static final String AVATAR_URL = "avatar_url";
  private static final String NAME = "name";
  private static final String USERNAME = "username";
  private static final String ACCESS_TOKEN = "access_token";
  private static final String REFRESH_TOKEN = "refresh_token";

  private static final int ONE_DAY = Math.toIntExact(Duration.ofDays(1).toSeconds());
  private static final int SEVEN_HOURS = Math.toIntExact(Duration.ofHours(7).toSeconds());
  private static final int FIVE_MONTHS = Math.toIntExact(Duration.ofDays(150).toSeconds());

  private final OAuth2AuthorizedClientService clientService;
  private final String contextPath;

  @Autowired
  public GithubAuthenticationSuccessHandler(
      OAuth2AuthorizedClientService clientService,
      @Qualifier("servletContextPath") String contextPath) {
    this.clientService = clientService;
    this.contextPath = contextPath;
  }

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws ServletException, IOException {
    final DefaultOAuth2User authenticationPrincipal =
        (DefaultOAuth2User) authentication.getPrincipal();

    final Optional<OAuth2AuthorizedClient> optionalAuthorisedClient =
        Optional.ofNullable(getAuthorisedClient(authentication));

    final Optional<OAuth2AccessToken> optionalAccessToken =
        optionalAuthorisedClient.map(OAuth2AuthorizedClient::getAccessToken);
    final Optional<OAuth2RefreshToken> optionalRefreshToken =
        optionalAuthorisedClient.map(OAuth2AuthorizedClient::getRefreshToken);

    Stream.of(
            createCookie(authenticationPrincipal, USERNAME, LOGIN),
            createCookie(authenticationPrincipal, AVATAR_URL, AVATAR_URL),
            createCookie(authenticationPrincipal, NAME, NAME),
            optionalAccessToken.map(this::createAccessTokenCookie),
            optionalRefreshToken.map(this::createRefreshTokenCookie))
        .flatMap(Optional::stream)
        .forEach(response::addCookie);

    response.sendRedirect("/#/workflow-jobs");
  }

  private Cookie createAccessTokenCookie(OAuth2AccessToken oAuth2AccessToken) {
    return createCookie(
        ACCESS_TOKEN, "token %s".formatted(oAuth2AccessToken.getTokenValue()), SEVEN_HOURS);
  }

  private Cookie createRefreshTokenCookie(OAuth2RefreshToken oAuth2RefreshToken) {
    return createCookie(REFRESH_TOKEN, oAuth2RefreshToken.getTokenValue(), FIVE_MONTHS);
  }

  private OAuth2AuthorizedClient getAuthorisedClient(Authentication authentication) {
    if (OAuth2AuthenticationToken.class.isAssignableFrom(authentication.getClass())) {
      final OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
      final String clientRegistrationId = oauthToken.getAuthorizedClientRegistrationId();
      return clientService.loadAuthorizedClient(clientRegistrationId, oauthToken.getName());
    }

    return null;
  }

  private Optional<Cookie> createCookie(
      DefaultOAuth2User authenticationPrincipal, String cookieName, String attributeName) {
    return Optional.ofNullable(authenticationPrincipal.getAttribute(attributeName))
        .map(Object::toString)
        .map(value -> createCookie(cookieName, value, ONE_DAY));
  }

  private Cookie createCookie(String cookieName, String value, int maxAge) {
    final Cookie cookie = new Cookie(cookieName, URLEncoder.encode(value, UTF_8));
    cookie.setMaxAge(maxAge);
    cookie.setPath(contextPath);
    return cookie;
  }
}
