package org.sonar.issuesreport.provider;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.sonar.api.CoreProperties;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.config.Configuration;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleParam;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.Rules;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.WsResponse;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
@ScannerSide
public class RuleProvider {

  private final WsClient wsClient;
  private Cache<RuleKey, Rule> ruleCache = CacheBuilder.newBuilder().build();

  public RuleProvider(Configuration settings) {
    HttpConnector httpConnector = HttpConnector.newBuilder()
        .url(settings.get(CoreProperties.SERVER_BASE_URL).orElse(CoreProperties.SERVER_BASE_URL_DEFAULT_VALUE))
        .credentials(settings.get(CoreProperties.LOGIN).orElse(null),
                     settings.get(CoreProperties.PASSWORD).orElse(null))
        .build();

    wsClient = WsClientFactories.getDefault().newClient(httpConnector);
  }

  public Rule getRule(RuleKey ruleKey) {
    try {
      return ruleCache.get(ruleKey, () -> {
        Rules.ShowResponse showResponse = showRule(ruleKey);
        return toRule(showResponse);
      });
    } catch (Exception e) {
      throw new IllegalStateException("Failed to get rule " + ruleKey, e);
    }
  }

  private Rules.ShowResponse showRule(RuleKey ruleKey) {
    GetRequest
        getRequest =
        new GetRequest("api/rules/show").setParam("key", ruleKey.toString()).setMediaType(MediaTypes.PROTOBUF);

    WsResponse wsResponse = wsClient.wsConnector().call(getRequest);

    if (wsResponse.code() != 200) {
      throw new HttpException(wsClient.wsConnector().baseUrl() + toString(getRequest), wsResponse.code(),
                              wsResponse.content());
    }

    try {
      return Rules.ShowResponse.parseFrom(wsResponse.contentStream());
    } catch (IOException e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  private String toString(GetRequest getRequest) {
    String
        params =
        getRequest.getParams().entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey))
            .map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining("&"));
    return getRequest.getPath() + "?" + params;
  }

  private Rule toRule(Rules.ShowResponse showResponse) {
    Rules.Rule rule = showResponse.getRule();
    if (rule == null) {
      return new RuleBuilder().build();
    }

    final RuleKey ruleKey = RuleKey.parse(rule.getKey());
    final Rule response = new RuleBuilder()
        .key(ruleKey.rule())
        .repository(ruleKey.repository())
        .name(rule.getName())
        .description(rule.getHtmlDesc())
        .build();

    rule.getParams().getParamsList().forEach(
        param -> {
          final RuleParam ruleParam = response.createParameter();
          ruleParam.setKey(param.getKey());
          ruleParam.setDescription(param.getHtmlDesc());
        });

    return response;
  }

  static class RuleBuilder {

    private Rule rule;

    RuleBuilder() {
      this.rule = Rule.create();
    }

    RuleBuilder key(String key) {
      this.rule.setKey(key);
      return this;
    }

    RuleBuilder name(String name) {
      this.rule.setName(name);
      return this;
    }

    Rule build() {
      return rule;
    }

    RuleBuilder description(String htmlDesc) {
      this.rule.setDescription(htmlDesc);
      return this;
    }

    public RuleBuilder repository(String repo) {
      this.rule.setRepositoryKey(repo);
      return this;
    }
  }

}
