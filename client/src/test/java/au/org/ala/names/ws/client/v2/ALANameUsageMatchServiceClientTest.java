package au.org.ala.names.ws.client.v2;

import au.org.ala.bayesian.Trace;
import au.org.ala.names.ws.api.SearchStyle;
import au.org.ala.names.ws.api.v2.NameSearch;
import au.org.ala.names.ws.api.v2.NameUsageMatch;
import au.org.ala.names.ws.client.v2.ALANameUsageMatchServiceClient;
import au.org.ala.util.TestUtils;
import au.org.ala.ws.ClientConfiguration;
import au.org.ala.ws.ClientException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import retrofit2.HttpException;

import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;

public class ALANameUsageMatchServiceClientTest extends TestUtils {
    private MockWebServer server;
    private ClientConfiguration configuration;
    private ALANameUsageMatchServiceClient client;

    @Before
    public void setUp() throws Exception {
        this.server = new MockWebServer();
        server.start();

        this.configuration = ClientConfiguration.builder().baseUrl(server.url("").url()).build();
        this.client = new ALANameUsageMatchServiceClient(configuration);
    }

    @After
    public void tearDown() throws Exception {
        this.server.shutdown();
        this.client.close();
    }

    /** Simple request/response */
    @Test
    public void testMatchNameSearch1() throws Exception {
        String request = this.getResource("request-1.json");
        String response = this.getResource("response-1.json");

        server.enqueue(new MockResponse().setBody(response));
        NameSearch search = NameSearch.builder().scientificName("Acacia dealbata").build();
        NameUsageMatch match = client.match(search, Trace.TraceLevel.NONE);

        assertTrue(match.isSuccess());
        assertEquals("Acacia dealbata", match.getScientificName());
        assertEquals("species", match.getRank());
        assertEquals("https://id.biodiversity.org.au/taxon/apni/51286863", match.getTaxonConceptID());
        assertEquals("Plantae", match.getKingdom());
        assertEquals(Collections.singletonList("http://ala.org.au/issues/1.0/acceptedAndSynonym"), match.getIssues());
        assertEquals(1, server.getRequestCount());
        RecordedRequest req = server.takeRequest();
        assertEquals("/api/v2/taxonomy/searchByClassification?trace=NONE", req.getPath());
        assertEquals(request, req.getBody().readUtf8());
    }

    /** Fail on unresolved homonym */
    @Test
    public void testMatchNameSearch2() throws Exception {
        String request = this.getResource("request-2.json");
        String response = this.getResource("response-2.json");

        server.enqueue(new MockResponse().setBody(response));
        NameSearch search = NameSearch.builder().scientificName("Macropus").build();
        NameUsageMatch match = client.match(search, Trace.TraceLevel.NONE);

        assertFalse(match.isSuccess());
        assertNull(match.getKingdom());
        assertEquals(Collections.singletonList("homonym"), match.getIssues());
        assertEquals(1, server.getRequestCount());
        RecordedRequest req = server.takeRequest();
        assertEquals("/api/v2/taxonomy/searchByClassification?trace=NONE", req.getPath());
        assertEquals(request, req.getBody().readUtf8());
    }

    /** Supply hints */
    @Test
    public void testMatchNameSearch3() throws Exception {
        String request = this.getResource("request-3.json");
        String response = this.getResource("response-3.json");

        server.enqueue(new MockResponse().setBody(response));
        Map<String, List<String>> hints = new HashMap<>();
        hints.put("kingdom", Arrays.asList("Animalia"));
        NameSearch search = NameSearch.builder().scientificName("Acacia dealbata").hints(hints).build();
        NameUsageMatch match = client.match(search, Trace.TraceLevel.NONE);

        assertTrue(match.isSuccess());
        assertEquals("Acacia dealbata", match.getScientificName());
        assertEquals(Collections.singletonList("http://ala.org.au/issues/1.0/acceptedAndSynonym"), match.getIssues());
        assertEquals(1, server.getRequestCount());
        RecordedRequest req = server.takeRequest();
        assertEquals("/api/v2/taxonomy/searchByClassification?trace=NONE", req.getPath());
        assertEquals(request, req.getBody().readUtf8());
    }

    @Test
    public void testMatchNameSearch4() throws Exception {
        String request = this.getResource("request-4.json");
        String response = this.getResource("response-3.json");

        server.enqueue(new MockResponse().setBody(response));
        Map<String, List<String>> hints = new HashMap<>();
        hints.put("kingdom", Arrays.asList("Animalia"));
        NameSearch search = NameSearch.builder().scientificName("Acacia dealbata").hints(hints).style(SearchStyle.STRICT).build();
        NameUsageMatch match = client.match(search, Trace.TraceLevel.NONE);

        assertTrue(match.isSuccess());
        assertEquals("Acacia dealbata", match.getScientificName());
        assertEquals(Collections.singletonList("http://ala.org.au/issues/1.0/acceptedAndSynonym"), match.getIssues());
        assertEquals(1, server.getRequestCount());
        RecordedRequest req = server.takeRequest();
        assertEquals("/api/v2/taxonomy/searchByClassification?trace=NONE", req.getPath());
        assertEquals(request, req.getBody().readUtf8());
    }

    /** Multiple search */
    @Test
    public void testMatchAllNameSearch1() throws Exception {
        String request = this.getResource("request-all-1.json");
        String response = this.getResource("response-all-1.json");

        server.enqueue(new MockResponse().setBody(response));
        List<NameSearch> searches = new ArrayList<>();
        searches.add(NameSearch.builder().scientificName("Acacia dealbata").build());
        searches.add(NameSearch.builder().scientificName("Osphranter rufus").build());
        List<NameUsageMatch> matches = client.matchAll(searches, Trace.TraceLevel.NONE);

        assertNotNull(matches);
        assertEquals(2, matches.size());
        NameUsageMatch match = matches.get(0);
        assertTrue(match.isSuccess());
        assertEquals("Acacia dealbata", match.getScientificName());
        assertEquals("species", match.getRank());
        assertEquals("https://id.biodiversity.org.au/taxon/apni/51286863", match.getTaxonConceptID());
        match = matches.get(1);
        assertTrue(match.isSuccess());
        assertEquals("Osphranter rufus", match.getScientificName());
        assertEquals("species", match.getRank());
        assertEquals("https://biodiversity.org.au/afd/taxa/7e6e134b-2bc7-43c4-b23a-6e3f420f57ad", match.getTaxonConceptID());

        assertEquals(1, server.getRequestCount());
        RecordedRequest req = server.takeRequest();
        assertEquals("/api/v2/taxonomy/searchAllByClassification?trace=NONE", req.getPath());
        assertEquals(request, req.getBody().readUtf8());
    }

    /** Ignored. Does not cache on POST request */
    @Test
    @Ignore
    public void testMatchNameSearchCache1() throws Exception {
        String response = this.getResource("response-1.json");

        server.enqueue(new MockResponse().addHeader("Cache-Control: max-age=60").setBody(response));
        server.enqueue(new MockResponse().setResponseCode(500));

        NameSearch search = NameSearch.builder().scientificName("Acacia dealbata").build();
        NameUsageMatch match = client.match(search, Trace.TraceLevel.NONE);
        search = NameSearch.builder().scientificName("Acacia dealbata").build();
        match = client.match(search, Trace.TraceLevel.NONE);
        search = NameSearch.builder().scientificName("Acacia dealbata").build();
        match = client.match(search, Trace.TraceLevel.NONE);

        assertTrue(match.isSuccess());
        assertEquals("Acacia dealbata", match.getScientificName());
        assertEquals("species", match.getRank());
        assertEquals("https://id.biodiversity.org.au/taxon/apni/51286863", match.getTaxonConceptID());
        assertEquals(1, server.getRequestCount());
    }


    /** Simple request/response */
    @Test
    public void testMatchNameParams1() throws Exception {
        String response = this.getResource("response-1.json");

        server.enqueue(new MockResponse().setBody(response));
        NameUsageMatch match = client.match("Acacia dealbata", "Plantae", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        assertTrue(match.isSuccess());
        assertEquals("Acacia dealbata", match.getScientificName());
        assertEquals("species", match.getRank());
        assertEquals("https://id.biodiversity.org.au/taxon/apni/51286863", match.getTaxonConceptID());
        assertEquals(1, server.getRequestCount());
        RecordedRequest req = server.takeRequest();
        assertEquals("/api/v2/taxonomy/searchByClassification?scientificName=Acacia%20dealbata&kingdom=Plantae", req.getPath());
    }
    /** Simple name match with caching */
    @Test
    public void testMatchParamsCache1() throws Exception {
        String response = this.getResource("response-1.json");

        server.enqueue(new MockResponse().addHeader("Cache-Control: max-age=60").setBody(response));
        server.enqueue(new MockResponse().setResponseCode(500));

        NameUsageMatch match = client.match("Acacia dealbata", "Plantae", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        match = client.match("Acacia dealbata", "Plantae", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        match = client.match("Acacia dealbata", "Plantae", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        assertTrue(match.isSuccess());
        assertEquals("Acacia dealbata", match.getScientificName());
        assertEquals("species", match.getRank());
        assertEquals("https://id.biodiversity.org.au/taxon/apni/51286863", match.getTaxonConceptID());
        assertEquals(1, server.getRequestCount());
    }

    /** Simple name match */
    @Test
    public void testMatchName1() throws Exception {
        String response = this.getResource("response-1.json");

        server.enqueue(new MockResponse().setBody(response));
        NameUsageMatch match = client.match("Acacia dealbata", null, null);

        assertTrue(match.isSuccess());
        assertEquals("Acacia dealbata", match.getScientificName());
        assertEquals("species", match.getRank());
        assertEquals("https://id.biodiversity.org.au/taxon/apni/51286863", match.getTaxonConceptID());
        assertEquals(1, server.getRequestCount());
        RecordedRequest req = server.takeRequest();
        assertEquals("/api/v2/taxonomy/search?q=Acacia%20dealbata", req.getPath());
    }

    /** Simple name match with caching */
    @Test
    public void testMatchNameCache1() throws Exception {
        String response = this.getResource("response-1.json");

        server.enqueue(new MockResponse().addHeader("Cache-Control: max-age=60").setBody(response));
        server.enqueue(new MockResponse().setResponseCode(500));
        NameUsageMatch match = client.match("Acacia dealbata", null, null);
        match = client.match("Acacia dealbata", null, null);
        match = client.match("Acacia dealbata", null, null);

        assertTrue(match.isSuccess());
        assertEquals("Acacia dealbata", match.getScientificName());
        assertEquals("species", match.getRank());
        assertEquals("https://id.biodiversity.org.au/taxon/apni/51286863", match.getTaxonConceptID());
        assertEquals(1, server.getRequestCount());
        RecordedRequest req = server.takeRequest();
        assertEquals("/api/v2/taxonomy/search?q=Acacia%20dealbata", req.getPath());
    }

    /** Simple vernacular name match */
    @Test
    public void testMatchVernacular1() throws Exception {
        String response = this.getResource("response-1.json");

        server.enqueue(new MockResponse().setBody(response));
        NameUsageMatch match = client.matchVernacular("Silver Wattle", Trace.TraceLevel.NONE);

        assertTrue(match.isSuccess());
        assertEquals("Acacia dealbata", match.getScientificName());
        assertEquals("species", match.getRank());
        assertEquals("https://id.biodiversity.org.au/taxon/apni/51286863", match.getTaxonConceptID());
        assertEquals(1, server.getRequestCount());
        RecordedRequest req = server.takeRequest();
        assertEquals("/api/v2/taxonomy/searchByVernacularName?vernacularName=Silver%20Wattle&trace=NONE", req.getPath());
    }

    /** Respond to error */
    @Test
    public void testError1() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("Unable to connect to index"));
        try {
            NameUsageMatch match = client.match("Acacia dealbata", null, null);
            fail("Expecting HttpException");
        } catch (HttpException ex) {
            assertEquals(500, ex.code());
            assertEquals("Unable to connect to index", ex.response().errorBody().string());
        }
        assertEquals(1, server.getRequestCount());
        RecordedRequest req = server.takeRequest();
        assertEquals("/api/v2/taxonomy/search?q=Acacia%20dealbata", req.getPath());
    }

    /** Respond to client error */
    @Test
    public void testError2() throws Exception {
        try {
            ClientConfiguration errorConfiguration = ClientConfiguration.builder().baseUrl(new URL("http://nothing.nowhere")).build();
            this.client = new ALANameUsageMatchServiceClient(errorConfiguration);
            NameUsageMatch match = client.match("Acacia dealbata", null, null);
            fail("Expecting ClientException");
        } catch (ClientException ex) {
        }
        assertEquals(0, server.getRequestCount());
    }
}
