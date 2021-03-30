package com.bean;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.utils.URIBuilder;
import org.springframework.stereotype.Service;

import fr.plaisance.bitly.Bit;
import fr.plaisance.bitly.Bitly;

@Service
public class UnshorterService {

	public static final List<String> amazonAffialteIdList = new ArrayList<String>();
	public static final List<String> flipkartAffialteIdList = new ArrayList<String>();
	public static String currentBitlyToken = null;
	public static int currentBitlyIndex = 0;
	public static List<String> bitlyTokens = GetGoogleSheetContent.getBitlyTokensList();
	public static Map<String, String> replaceSheetMap = GetGoogleSheetContent.getReplaceSheetMap();
	GenerateFlipkartShortLinks generateFlipkartShortLinks = new GenerateFlipkartShortLinks();

	static {
		amazonAffialteIdList.add("dealsalert13-21");
		flipkartAffialteIdList.add("vishalkum20");
	}

	public String getDomainName(String urlString) {
		URL url;
		try {
			url = new URL(urlString);
			String protocol = url.getProtocol();
			String host = url.getHost();
			int port = url.getPort();

			// if the port is not explicitly specified in the input, it will be -1.
			if (port == -1) {
				return String.format("%s://%s", protocol, host);
			} else {
				return String.format("%s://%s:%d", protocol, host, port);
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

	}

	public Map<String, String> unshortURL(List<String> shortenURLList) {
		Map<String, String> output = new HashMap<>();

		for (String s : shortenURLList) {
			try {
				String unshortedURL = s;
				if (!s.contains("amazon") && !s.contains("flipkart")) {
					unshortedURL = unshortenUrl(s);
					if (unshortedURL.contains("earnkaro")) {
						unshortedURL = unshortedURL.split("&dl=")[1];
						unshortedURL = URLDecoder.decode(unshortedURL);
					} else if (unshortedURL.contains("paisawapas.com")) {
						unshortedURL = unshortedURL.split("&url=")[1];
						unshortedURL = URLDecoder.decode(unshortedURL);
					} else if (unshortedURL.contains("linksredirect")) {
						unshortedURL = unshortedURL.split("&url=")[1];
						unshortedURL = URLDecoder.decode(unshortedURL);
					}
				}
				String shortURL = chnageToOurAffliate(unshortedURL);
				// String shortURL = shortURL(unshortedURL);
				output.put(s, shortURL);

				// Now short url
				// Then put it in a map, then we will replace each previous url by key and new
				// url by value

			} catch (IOException | URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return output;
	}

	private String shortURL(String unshortedURL) {
		if (currentBitlyToken == null) {
			currentBitlyToken = bitlyTokens.get(currentBitlyIndex);
		}
		int index = 0;
		Bitly bitly = Bit.ly(currentBitlyToken);
		String shortURL = null;
		if (unshortedURL != null && unshortedURL.length() > 0) {
			try {
				shortURL = bitly.shorten(unshortedURL);
			} catch (Exception e) {
				for (int i = index + 1; i < bitlyTokens.size(); i++) {
					currentBitlyToken = bitlyTokens.get(i);
					return shortURL(unshortedURL);
				}
			}
		}
		return shortURL;
	}

	private String chnageToOurAffliate(String unshortenUrl) throws URISyntaxException, UnsupportedEncodingException {
		Map<String, List<String>> queryParams = splitQuery(unshortenUrl);

		unshortenUrl = replaceWords(unshortenUrl);
		String domain = getDomainName(unshortenUrl);
		boolean isAmazonDeal = false, isFlipkartDeal = false;
		if (domain != null && domain.contains("amazon")) {
			isAmazonDeal = true;
		} else if (domain != null && domain.contains("flipkart")) {
			isFlipkartDeal = true;
		}

		if (isAmazonDeal) {
			//return shortURL(changeAmazonDealLink(unshortenUrl, queryParams));
			return generateFlipkartShortLinks
					.generateAmazonShortLinks(unshortenUrl);
		} else if (isFlipkartDeal) {
			// return changeFlipkartDealLink(unshortenUrl, queryParams);
			return generateFlipkartShortLinks
					.generateShortURL(generateFlipkartShortLinks.getFullFlipkartURL(unshortenUrl));
		}

		return null;

	}

	private String replaceWords(String unshortenUrl) {

		for (Entry<String, String> e : replaceSheetMap.entrySet()) {
			unshortenUrl = org.apache.commons.lang3.StringUtils.replaceIgnoreCase(unshortenUrl, e.getKey(),
					e.getValue());
		}
		return unshortenUrl;
	}

	private String changeFlipkartDealLink(String unshortenUrl, Map<String, List<String>> queryParams)
			throws URISyntaxException {
		if (queryParams.containsKey("affid")) {
			queryParams.put("affid", flipkartAffialteIdList);
		} else {
			queryParams.put("affid", flipkartAffialteIdList);
		}

		String originalURL = unshortenUrl.split("\\?")[0];

		Iterator<String> it = queryParams.keySet().iterator();
		URIBuilder b = new URIBuilder(originalURL);

		while (it.hasNext()) {
			String key = it.next();
			if (key.contains("flipkart")) {
				continue;
			}
			List<String> value = queryParams.get(key);
			if (!value.isEmpty()) {
				// b.addParameter(key, value);
				value.stream().forEach(s -> b.addParameter(key, s));
			}
		}

		if (b.toString().contains("affid=" + flipkartAffialteIdList.get(0))) {
			return URLDecoder.decode(b.toString());
		} else {
			return null;
		}
	}

	private String changeAmazonDealLink(String unshortenUrl, Map<String, List<String>> queryParams)
			throws URISyntaxException {

		if (queryParams.containsKey("tag")) {
			queryParams.put("tag", amazonAffialteIdList);
		} else {
			queryParams.put("tag", amazonAffialteIdList);
		}

		if (queryParams.containsKey("keyword")) {
			queryParams.remove("keyword");
		}

		if (queryParams.containsKey("keywords") && queryParams.get("keywords") != null
				&& queryParams.get("keywords").size() > 0
				&& (queryParams.get("keywords").contains("copied") || queryParams.get("keywords").contains("copy")
						|| queryParams.get("keywords").get(0).contains("telegram")
						|| queryParams.get("keywords").get(0).toLowerCase().contains("telegram"))) {
			queryParams.remove("keywords");
		}

		String originalURL = unshortenUrl.split("\\?")[0];

		Iterator<String> it = queryParams.keySet().iterator();
		URIBuilder b = new URIBuilder(originalURL);

		while (it.hasNext()) {
			String key = it.next();
			if (key.contains("amazon.in")) {
				continue;
			}
			List<String> value = queryParams.get(key);
			if (!value.isEmpty()) {
				// b.addParameter(key, value);
				value.stream().forEach(s -> b.addParameter(key, s));
			}
		}

		if (b.toString().contains("tag=" + amazonAffialteIdList.get(0)) && !b.toString().contains("afflepay1007-21")) {
			return URLDecoder.decode(b.toString());
		} else {
			return null;
		}
	}

	public String changedDeal(String deal) {

		deal = replaceWords(deal);
		List<String> urls = extractUrls(deal);
		Map<String, String> changedURLMap = unshortURL(urls);

		for (Map.Entry<String, String> entry : changedURLMap.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			while (deal.contains(key)) {
				if (null != value) {
					deal = deal.replace(key, value);
				} else {
					deal = deal.replace(key, "Unable to create");
				}
			}
		}

		return deal;
	}

	public static Map<String, List<String>> splitQuery(String url) throws UnsupportedEncodingException {
		url = url.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
		url = url.replaceAll("\\+", "%2B");
		final Map<String, List<String>> query_pairs = new LinkedHashMap<String, List<String>>();
		String[] pairs = null;
		if (url.contains("?")) {
			pairs = url.split("\\?")[1].split("&");
		} else {
			pairs = url.split("&");
		}
		for (String pair : pairs) {
			final int idx = pair.indexOf("=");
			final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
			if (!query_pairs.containsKey(key)) {
				query_pairs.put(key, new LinkedList<String>());
			}
			final String value = idx > 0 && pair.length() > idx + 1
					? URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
					: null;
			query_pairs.get(key).add(value);
		}
		return query_pairs;
	}

	static Pattern URL_PATTERN = Pattern.compile(
			"((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/!;$()~_?\\+-=\\\\\\.&\\[\\]]*)",
			Pattern.CASE_INSENSITIVE);

	static List<String> extractUrls(final String text) {
		List<String> urls = new ArrayList<>();
		Matcher matcher = URL_PATTERN.matcher(text);
		while (matcher.find()) {
			urls.add(text.substring(matcher.start(0), matcher.end(0)));
		}
		return urls;
	}

	public static String unshortenUrl(String shortUrl) throws IOException {
		shortUrl = shortUrl.replace("\\", "%5C");
		HttpURLConnection connection = (HttpURLConnection) new URL(shortUrl).openConnection();
		connection.setRequestProperty("User-Agent",
				"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");

		connection.setInstanceFollowRedirects(false);
		connection.setRequestMethod("HEAD");
		int responseCode = connection.getResponseCode();
		String url = connection.getHeaderField("location");
		if (responseCode / 100 == 3 && url != null) {
			if (url.contains("flipkart") || url.contains("amazon")) {
				return url;
			}
			String expandedUrl = unshortenUrl(url);
			if (Objects.equals(expandedUrl, url))
				return url;
			else {
				return expandedUrl;
			}
		}
		return shortUrl;
	}
}
