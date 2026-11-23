package com.fairfold.assessment;

import com.fairfold.assessment.dto.LinkCheckResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * Checks curated learning-resource links. HEAD is used first, GET when the
 * server does not support HEAD. Only public http(s) URLs are fetched: the
 * checker refuses localhost and private/link-local addresses so an admin
 * cannot make the server reach its own network (SSRF).
 */
@Service
public class ResourceLinkChecker {

    private static final Logger log = LoggerFactory.getLogger(ResourceLinkChecker.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @FunctionalInterface
    interface LinkFetcher {
        int fetch(String url) throws IOException, InterruptedException;
    }

    private final LearningResourceRepository resourceRepository;
    private final Clock clock;
    private final LinkFetcher fetcher;

    @Autowired
    public ResourceLinkChecker(LearningResourceRepository resourceRepository, Clock clock) {
        this(resourceRepository, clock, ResourceLinkChecker::fetchWithHttpClient);
    }

    ResourceLinkChecker(LearningResourceRepository resourceRepository, Clock clock, LinkFetcher fetcher) {
        this.resourceRepository = resourceRepository;
        this.clock = clock;
        this.fetcher = fetcher;
    }

    /** Weekly job; configured by {@code app.resources.link-check-cron}. */
    @Scheduled(cron = "${app.resources.link-check-cron:0 0 3 * * SUN}")
    public void scheduledCheck() {
        LinkCheckResult result = checkAll();
        log.info("Resource link check finished: {} checked, {} broken",
                result.checked(), result.broken());
    }

    @Transactional
    public LinkCheckResult checkAll() {
        List<LearningResource> resources = resourceRepository.findAll();
        int broken = 0;
        for (LearningResource resource : resources) {
            boolean ok = isReachable(resource.getUrl());
            resource.setBroken(!ok);
            resource.setLastCheckedAt(clock.instant());
            resourceRepository.save(resource);
            if (!ok) {
                broken++;
            }
        }
        return new LinkCheckResult(resources.size(), broken);
    }

    boolean isReachable(String url) {
        if (!isPublicHttpUrl(url)) {
            return false;
        }
        try {
            int status = fetcher.fetch(url);
            return status >= 200 && status < 400;
        } catch (Exception ex) {
            return false;
        }
    }

    /** Scheme check plus DNS-based blocking of localhost, private and link-local hosts. */
    boolean isPublicHttpUrl(String url) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (host == null
                    || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                return false;
            }
            String lowerHost = host.toLowerCase();
            if (lowerHost.equals("localhost") || lowerHost.endsWith(".localhost")) {
                return false;
            }
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (isForbiddenAddress(address)) {
                    return false;
                }
            }
            return true;
        } catch (IllegalArgumentException | UnknownHostException ex) {
            return false;
        }
    }

    private boolean isForbiddenAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int first = bytes[0] & 0xff;
            int second = bytes[1] & 0xff;
            // 100.64.0.0/10 carrier-grade NAT
            if (first == 100 && second >= 64 && second <= 127) {
                return true;
            }
            // 198.18.0.0/15 benchmarking, 192.0.0.0/24 special purpose
            if (first == 198 && (second == 18 || second == 19)) {
                return true;
            }
            if (first == 192 && second == 0 && (bytes[2] & 0xff) == 0) {
                return true;
            }
        } else if (bytes.length == 16) {
            // fc00::/7 unique local addresses
            if ((bytes[0] & 0xfe) == 0xfc) {
                return true;
            }
        }
        return false;
    }

    private static int fetchWithHttpClient(String url) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpResponse<Void> response = client.send(request(url, "HEAD"), HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() == 405 || response.statusCode() == 501) {
            response = client.send(request(url, "GET"), HttpResponse.BodyHandlers.discarding());
        }
        return response.statusCode();
    }

    private static HttpRequest request(String url, String method) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .method(method, HttpRequest.BodyPublishers.noBody())
                .header("User-Agent", "FairFold-LinkChecker/1.0")
                .build();
    }
}
