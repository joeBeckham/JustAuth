package me.zhyd.oauth.request;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSONObject;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.exception.AuthException;
import me.zhyd.oauth.model.AuthSource;
import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.model.AuthUserGender;
import me.zhyd.oauth.utils.StringUtils;
import me.zhyd.oauth.utils.UrlBuilder;

/**
 * qq登录
 *
 * @author yadong.zhang (yadong.zhang0415(a)gmail.com)
 * @version 1.0
 * @since 1.8
 */
public class AuthQqRequest extends BaseAuthRequest {
    public AuthQqRequest(AuthConfig config) {
        super(config, AuthSource.QQ);
    }

    @Override
    protected AuthToken getAccessToken(String code) {
        String accessTokenUrl = UrlBuilder.getQqAccessTokenUrl(config.getClientId(), config.getClientSecret(), code, config.getRedirectUri());
        HttpResponse response = HttpRequest.post(accessTokenUrl).execute();
        JSONObject accessTokenObject = JSONObject.parseObject(response.body());
        if (!accessTokenObject.containsKey("access_token")) {
            throw new AuthException("Unable to get token from qq using code [" + code + "]");
        }
        return AuthToken.builder()
                .accessToken(accessTokenObject.getString("access_token"))
                .build();
    }

    @Override
    protected AuthUser getUserInfo(AuthToken authToken) {
        String accessToken = authToken.getAccessToken();
        String openId = this.getOpenId(accessToken);
        HttpResponse response = HttpRequest.get(UrlBuilder.getQqUserInfoUrl(accessToken, openId)).execute();
        JSONObject object = JSONObject.parseObject(response.body());
        if (object.getIntValue("ret") != 0) {
            throw new AuthException(object.getString("msg"));
        }
        String avatar = object.getString("figureurl_qq_2");
        if (StringUtils.isEmpty(avatar)) {
            avatar = object.getString("figureurl_qq_1");
        }
        return AuthUser.builder()
                .username(object.getString("nickname"))
                .nickname(object.getString("nickname"))
                .avatar(avatar)
                .gender(AuthUserGender.getRealGender(object.getString("gender")))
                .accessToken(accessToken)
                .source(AuthSource.QQ)
                .build();
    }

    private String getOpenId(String accessToken) {
        HttpResponse response = HttpRequest.get(UrlBuilder.getQqOpenidUrl("https://graph.qq.com/oauth2.0/me", accessToken)).execute();
        if (response.isOk()) {
            JSONObject object = JSONObject.parseObject(response.body());
            if (object.containsKey("openid")) {
                return object.getString("openid");
            }
            throw new AuthException("Invalid openId");
        }
        throw new AuthException("Invalid openId");
    }
}
