package com.techbloghub.output.rss.parser;

import com.techbloghub.domain.model.RssFeed;
import com.techbloghub.domain.port.out.FetchRssPort.RssFetchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class RssContentParserTest {

    private RssContentParser parser;

    @BeforeEach
    void setUp() {
        parser = new RssContentParser();
    }

    @Test
    void parseRssContent_validRssXml_shouldReturnRssFeed() {
        // given
        String rssXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0">
                    <channel>
                        <title>Test Blog</title>
                        <description>Test Description</description>
                        <item>
                            <title>Test Post</title>
                            <description>Test Content</description>
                            <link>https://example.com/post1</link>
                            <author>Test Author</author>
                            <pubDate>Mon, 01 Jan 2024 12:00:00 GMT</pubDate>
                        </item>
                    </channel>
                </rss>
                """;
        InputStream inputStream = new ByteArrayInputStream(rssXml.getBytes());
        String rssUrl = "https://example.com/rss/";

        // when
        RssFeed result = parser.parseRssContent(inputStream, rssUrl);

        // then
        assertNotNull(result);
        assertEquals(rssUrl, result.getUrl());
        assertEquals("Test Blog", result.getTitle());
        assertEquals("Test Description", result.getDescription());
        assertEquals(1, result.getEntries().size());
        assertEquals("Test Post", result.getEntries().get(0).getTitle());
        assertEquals("Test Content", result.getEntries().get(0).getContent());
        assertEquals("https://example.com/post1", result.getEntries().get(0).getUrl());
        assertEquals("Test Author", result.getEntries().get(0).getAuthor());
        assertNotNull(result.getEntries().get(0).getPublishedAt());
    }

    @Test
    void parseRssContent_gzipCompressedXml_shouldDecompressAndParse() throws Exception {
        // given
        String rssXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0">
                    <channel>
                        <title>Compressed Blog</title>
                        <description>Compressed Description</description>
                    </channel>
                </rss>
                """;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
            gzipOut.write(rssXml.getBytes("UTF-8"));
        }

        InputStream inputStream = new ByteArrayInputStream(baos.toByteArray());
        String rssUrl = "https://example.com/rss/";

        // when
        RssFeed result = parser.parseRssContent(inputStream, rssUrl);

        // then
        assertNotNull(result);
        assertEquals("Compressed Blog", result.getTitle());
        assertEquals("Compressed Description", result.getDescription());
    }

    @Test
    void parseRssContent_xmlWithBOM_shouldRemoveBOMAndParse() {
        // given
        String rssXml = "\uFEFF<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<rss version=\"2.0\">\n" +
                "    <channel>\n" +
                "        <title>BOM Test Blog</title>\n" +
                "        <description>Test Description</description>\n" +
                "    </channel>\n" +
                "</rss>";
        InputStream inputStream = new ByteArrayInputStream(rssXml.getBytes());
        String rssUrl = "https://example.com/rss/";

        // when
        RssFeed result = parser.parseRssContent(inputStream, rssUrl);

        // then
        assertNotNull(result);
        assertEquals("BOM Test Blog", result.getTitle());
    }

    @Test
    void parseRssContent_nonXmlContent_shouldThrowException() {
        // given
        String nonXmlContent = "This is not XML content";
        InputStream inputStream = new ByteArrayInputStream(nonXmlContent.getBytes());
        String rssUrl = "https://example.com/rss/";

        // when & then
        RssFetchException exception = assertThrows(RssFetchException.class, () ->
                parser.parseRssContent(inputStream, rssUrl)
        );

        assertTrue(exception.getMessage().contains("Response doesn't appear to be XML content"));
        assertTrue(exception.getMessage().contains(rssUrl));
    }

    @Test
    void parseRssContent_invalidXml_shouldThrowException() {
        // given
        String invalidXml = "<?xml version=\"1.0\"?><invalid><unclosed>";
        InputStream inputStream = new ByteArrayInputStream(invalidXml.getBytes());
        String rssUrl = "https://example.com/rss/";

        // when & then
        RssFetchException exception = assertThrows(RssFetchException.class, () ->
                parser.parseRssContent(inputStream, rssUrl)
        );

        assertTrue(exception.getMessage().contains("Failed to parse RSS content"));
        assertTrue(exception.getMessage().contains(rssUrl));
    }

    @Test
    void parseRssContent_atomFeed_shouldParseSuccessfully() {
        // given
        String atomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <feed xmlns="http://www.w3.org/2005/Atom">
                    <title>Atom Test Feed</title>
                    <subtitle>Atom Description</subtitle>
                    <entry>
                        <title>Atom Post</title>
                        <content>Atom Content</content>
                        <link href="https://example.com/atom-post"/>
                        <author><name>Atom Author</name></author>
                        <published>2024-01-01T12:00:00Z</published>
                    </entry>
                </feed>
                """;
        InputStream inputStream = new ByteArrayInputStream(atomXml.getBytes());
        String rssUrl = "https://example.com/atom/";

        // when
        RssFeed result = parser.parseRssContent(inputStream, rssUrl);

        // then
        assertNotNull(result);
        assertEquals("Atom Test Feed", result.getTitle());
        assertEquals("Atom Description", result.getDescription());
        assertEquals(1, result.getEntries().size());
        assertEquals("Atom Post", result.getEntries().get(0).getTitle());
    }

    @Test
    void parseRssContent_htmlInContent_shouldCleanHtmlTags() {
        // given
        String rssXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0">
                    <channel>
                        <title><![CDATA[Blog with <strong>HTML</strong>]]></title>
                        <description><![CDATA[Description with <em>emphasis</em>]]></description>
                        <item>
                            <title><![CDATA[Post with <h1>HTML</h1> tags]]></title>
                            <description><![CDATA[Content with <p>paragraphs</p> and <a href="#">links</a>]]></description>
                            <link>https://example.com/post1</link>
                        </item>
                    </channel>
                </rss>
                """;
        InputStream inputStream = new ByteArrayInputStream(rssXml.getBytes());
        String rssUrl = "https://example.com/rss/";

        // when
        RssFeed result = parser.parseRssContent(inputStream, rssUrl);

        // then
        assertEquals("Blog with HTML", result.getTitle());
        assertEquals("Description with emphasis", result.getDescription());
        assertEquals("Post with HTML tags", result.getEntries().get(0).getTitle());
        assertEquals("Content with paragraphs and links", result.getEntries().get(0).getContent());
    }
}