
import groovy.json.JsonSlurper

@Grab(group='org.apache.commons', module='commons-lang3', version='3.1')
import org.apache.commons.lang3.StringEscapeUtils
import org.apache.commons.lang3.StringUtils

@Grab(group='com.google.api-client', module='google-api-client', version='1.6.0-beta')
import com.google.api.client.googleapis.GoogleHeaders
import com.google.api.client.googleapis.GoogleUrl
import com.google.api.client.googleapis.json.JsonCParser
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.client.util.Key

version = 0.1

maxResults = 999
vidFormats = 18

set 'port', 4242

parser = new JsonCParser(new JacksonFactory())
header = new GoogleHeaders(gdataVersion: '2')

String.metaClass.getAsXml << { -> StringEscapeUtils.escapeXml delegate }

factory = new NetHttpTransport().createRequestFactory(
    { request ->

        request.readTimeout = 4000
        request.connectTimeout = 2000

        request.headers = header
        request.addParser parser

    } as HttpRequestInitializer
)

class YouTubeUrl extends GoogleUrl {

//    @Key("start-index") int startIndex = 1
//    @Key("max-results") int maxResults = 50

    YouTubeUrl(String id) {
        super("https://gdata.youtube.com/feeds/api/$id")
        alt = 'jsonc'
    }
}

class UploadsUrl extends YouTubeUrl {
    UploadsUrl(String id) { super("users/$id/uploads") }
}

class VideoFeed {

    String author
    String title
    String link

    @Key List<VideoItem> items
}

class VideoItem {

    @Key String id

    @Key String title
    @Key DateTime uploaded

    @Key String uploader
    @Key int duration

    @Key List<String> tags
    @Key String description
}

get('/feeds/uploads/:id') {

    def getRequest = factory.buildGetRequest(new UploadsUrl(urlparams.id))
    def videoFeed = getRequest.execute().parseAs(VideoFeed.class)

    videoFeed.author = urlparams.id
    videoFeed.title = "${urlparams.id} YouTube Uploads"
    videoFeed.link = "http://www.youtube.com/user/${urlparams.id}"

    contentType 'application/rss+xml'
    render 'Template.rss', [feed: videoFeed]
}

get('/video/:id.mp4') {

    def videoSrc = new URL("http://www.youtube.com/watch?v=${urlparams.id}").text
    def jsonConf = StringUtils.substringBetween(videoSrc, "'PLAYER_CONFIG':", '});')

    def urlsList = new JsonSlurper().parseText(jsonConf).args.url_encoded_fmt_stream_map

    def dirtyUrl = urlsList.split('url=').find { it.contains "itag%3D$vidFormats" }
    def cleanUrl = URLDecoder.decode(dirtyUrl, 'UTF-8').split(';')[0]

    response.sendRedirect(response.encodeRedirectURL(cleanUrl)); 'DONE'
}
