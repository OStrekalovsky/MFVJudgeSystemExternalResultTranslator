package ost.iuexternalresults;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Воркер для скачивания результатов их преобразования.
 * <p/>
 * Author: Oleg Strekalovsky
 * Date: 17.04.2015
 */
public class Worker {

    public static final String HTML_FILE_EXTENSION = ".html";
    private final String baseUrl;

    public static final String RESULT_PAGE = "results_external.pl";
    private final String resultPagePrefix;

    public Worker(String baseUrl, String resultPagePrefix) {
        this.baseUrl = baseUrl;
        this.resultPagePrefix = resultPagePrefix;
    }

    public void run(String targetDir, String resultPagePrefix) throws Exception {
        for (PageStyle pageStyle : PageStyle.values()) {
            for (PageRefreshRate refreshRate : PageRefreshRate.values()) {
                String requestParams = buildRequest(pageStyle, refreshRate);
                Content originPageContent = getPage(requestParams);
                saveTo(targetDir, buildSavedPageName(resultPagePrefix, HTML_FILE_EXTENSION, pageStyle, refreshRate),
                        mutate(originPageContent, pageStyle, refreshRate).toCharArray());
            }
        }
    }

    private String mutate(Content original, PageStyle currentPageStyle, PageRefreshRate currentPageRefreshRate) {
        String mutated = new String(original.getBytes());
        String contestState = getContestState(mutated);
        String links = buildLinks(currentPageStyle, currentPageRefreshRate);
        mutated = trimHeader(mutated);
        mutated = addContestState(contestState + "<br/>" + links, mutated);
        mutated = center(mutated);
        String htmlHeader = addHTMLHeader(currentPageRefreshRate);
        mutated = htmlHeader + mutated;
        return mutated;
    }

    private String addHTMLHeader(PageRefreshRate currentPageRefreshRate) {
        String header = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html;charset=windows-1251\">" +
                "<meta name=\"Author\" content=\"Fyodor Menshikov <mfv@mail.ru>\"> <link rel=\"stylesheet\" href=\"gh-buttons.css\">";
        if (currentPageRefreshRate.getRefreshSec() != 0) {
            header += "<meta http-equiv=\"refresh\" content=\"" + currentPageRefreshRate.getRefreshSec() + "\" />";
        }
        header += "</head><body>";
        return header;
    }

    private String buildHref(String url, String content, boolean isCurrent) {
        return "<a href=" + url + " class=\"button " + (isCurrent ? "active" : "") + "\">" + content + "</a>";
    }

    private String buildLinks(PageStyle currentPageStyle, PageRefreshRate currentRefreshRate) {
        String fastPageLink = buildHref(buildSavedPageName(resultPagePrefix, HTML_FILE_EXTENSION, PageStyle.SLIM, currentRefreshRate),
                "Fast", currentPageStyle == PageStyle.SLIM);
        String detailPageLink = buildHref(buildSavedPageName(resultPagePrefix, HTML_FILE_EXTENSION, PageStyle.DETAIL, currentRefreshRate),
                "Detail", currentPageStyle == PageStyle.DETAIL);
        String coloredPageLink = buildHref(buildSavedPageName(resultPagePrefix, HTML_FILE_EXTENSION, PageStyle.COLORED, currentRefreshRate),
                "Colored", currentPageStyle == PageStyle.COLORED);
        String neverUpdatePageLink = buildHref(buildSavedPageName(resultPagePrefix, HTML_FILE_EXTENSION, currentPageStyle, PageRefreshRate.NEVER),
                "Never", currentRefreshRate == PageRefreshRate.NEVER);
        String fiveSecUpdatePageLink = buildHref(buildSavedPageName(resultPagePrefix, HTML_FILE_EXTENSION, currentPageStyle, PageRefreshRate.SEC5),
                "5 sec", currentRefreshRate == PageRefreshRate.SEC5);
        String oneMinUpdatePageLink = buildHref(buildSavedPageName(resultPagePrefix, HTML_FILE_EXTENSION, currentPageStyle, PageRefreshRate.MINUTE),
                "1 min", currentRefreshRate == PageRefreshRate.MINUTE);
        String tenMinUpdatePageLink = buildHref(buildSavedPageName(resultPagePrefix, HTML_FILE_EXTENSION, currentPageStyle, PageRefreshRate.TEN_MINUTES),
                "10 min", currentRefreshRate == PageRefreshRate.TEN_MINUTES);
        String styleLinks = "<div class=\"button-group\">" + concat(" ", fastPageLink, detailPageLink, coloredPageLink) + "</div>";
        String updateRateLinks = "<div class=\"button-group\">" + concat(" ", neverUpdatePageLink, fiveSecUpdatePageLink, oneMinUpdatePageLink, tenMinUpdatePageLink) + "</div>";
        return styleLinks + "<br/>" + updateRateLinks;
    }

    private String concat(String delimiter, String... strings) {
        StringBuilder sb = new StringBuilder();
        for (String val : strings) {
            sb.append(val).append(delimiter);
        }
        sb.setLength(sb.length() - delimiter.length());
        return sb.toString();
    }

    private String getContestState(String mutated) {
        final String startTag = "<pre>";
        int start = mutated.indexOf(startTag);
        final String endTag = "</pre>";
        int end = mutated.indexOf(endTag);
        String fullState = mutated.substring(start + startTag.length(), end);
        String newLine = "\r\n";
        int i = fullState.indexOf(newLine);
        i = fullState.indexOf(newLine, i + 1);
        i = fullState.indexOf(newLine, i + 1);
        int cutPos = i + newLine.length();
        int endCutPos = fullState.indexOf(newLine, i + 1);
        return fullState.substring(cutPos, endCutPos);
    }

    private String addContestState(String contestState, String suffix) {
        return contestState + suffix;
    }

    private String center(String mutated) {
        return "<center>" + mutated + "</center>";
    }

    private String trimHeader(String mutated) {
        final String str = "<br><br>";
        int headerEnd = mutated.lastIndexOf(str);
        return mutated.substring(headerEnd + str.length());
    }

    private Content getPage(String requestParams) throws IOException {
        URL url = new URL(baseUrl + "/" + RESULT_PAGE + "?" + requestParams);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "latin1"));
            char[] content = new char[1000000];
            int byteRead;
            int size = 0;
            while ((byteRead = reader.read()) != -1) {
                content[size] = (char) byteRead;
                size++;
            }
            return new Content(content, size);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private void saveTo(String targetDir, String pageName, char[] chars) throws IOException {
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(targetDir + "/" + pageName);
            for (int ch : chars) {
                outputStream.write(ch);
            }
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }

    private String buildRequest(RequestParameter... parameters) {
        String request = "";
        for (RequestParameter parameter : parameters) {
            request += "&" + parameter.getRequestParameter();
        }
        if (request.startsWith("&")) {
            request = request.substring(1);
        }
        return request;
    }

    private String buildSavedPageName(String prefix, String suffix, PageView... views) {
        String pageName = prefix;
        for (PageView view : views) {
            pageName += view.getViewName();
        }
        return pageName + suffix;
    }
}

class Content {
    char bytes[];
    int len;

    public Content(char[] bytes, int len) {
        this.bytes = new char[len];
        System.arraycopy(bytes, 0, this.bytes, 0, len);
        this.len = len;
    }

    public char[] getBytes() {
        return bytes.clone();
    }

    public int getLen() {
        return len;
    }
}

interface PageView {
    public String getViewName();
}

interface RequestParameter {

    public String getRequestParameter();
}

enum PageStyle implements RequestParameter, PageView {
    SLIM {
        @Override
        public String getViewName() {
            return "-slim";
        }

        @Override
        public String getRequestParameter() {
            return "style=slim";
        }
    }, DETAIL {
        @Override
        public String getViewName() {
            return "-detail";
        }

        @Override
        public String getRequestParameter() {
            return "style=detail";
        }
    }, COLORED {
        @Override
        public String getViewName() {
            return "-color";
        }

        @Override
        public String getRequestParameter() {
            return "style=color";
        }
    };
}

enum PageRefreshRate implements RequestParameter, PageView {
    NEVER {
        @Override
        public int getRefreshSec() {
            return 0;
        }

        @Override
        public String getViewName() {
            return "-no-updates";
        }

        @Override
        public String getRequestParameter() {
            return "refresh=0";
        }
    }, SEC5 {
        @Override
        public int getRefreshSec() {
            return 5;
        }

        @Override
        public String getViewName() {
            return "-update5sec";
        }

        @Override
        public String getRequestParameter() {
            return "refresh=5";
        }
    }, MINUTE {
        @Override
        public int getRefreshSec() {
            return 60;
        }

        @Override
        public String getViewName() {
            return "-update1min";
        }

        @Override
        public String getRequestParameter() {
            return "refresh=60";
        }
    }, TEN_MINUTES {
        @Override
        public int getRefreshSec() {
            return 600;
        }

        @Override
        public String getViewName() {
            return "-update10min";
        }

        @Override
        public String getRequestParameter() {
            return "refresh=600";
        }
    };

    public abstract int getRefreshSec();
}

