package com.adi

import groovy.json.JsonSlurper
import groovy.json.StringEscapeUtils
import java.util.regex.Matcher
import java.util.regex.Pattern
import static com.adi.Constants.DROPBOX
import static com.adi.Constants.YOUTUBE
import static com.adi.Constants.ERROR_CODE
import static com.adi.Constants.FAILURE

/**
 * @author adithya
 */
class LinkUtils {

    def address

    LinkUtils(address) {
        this.address = address
    }
    /**
     * This method returns downloadable link for a given url
     * @param address String address of the url where content is available
     * @return address String actual downloadable link of the url
     */
    def getDownloadableLink() {
        address = getFinalLocation(this.address)
        URL url = new URL(address)
        if (url.getHost()?.contains(DROPBOX)) {
            println("Host is dropbox")
            if (address.indexOf("?") == -1) {
                address = address + "?dl=1"  //if there are no url parameters add dl=1
            } else if (address.contains("dl=0")) {
                address = address.replace("dl=0", "dl=1") //if there is dl=0 replace it with dl=1
            } else if (!address.contains("dl=")) {
                address = address + "&dl=1" //if there is no dl add dl=1
            } else {
                address = address.substring(0, address.indexOf("dl=")) + "dl=1" + address.substring(address.indexOf("dl=") + 4)
                // if there is a dl with some other number replace dl=* with dl=0
            }
        } else if (url.getHost().contains(YOUTUBE)) {
            println("Host is youtube")
            def videoId = extractVideoIdFromUrl(address)
            address = getYouTubeDownloadableLink(videoId)
        }
        return address
    }

    /**
     * This method is used to get the final location of a url after multiple redirects
     * @param address String url link of the content
     * @return address String actual address of the url in-case of  multiple redirects
     */
    def getFinalLocation(address) {
        URL url = new URL(address)
        HttpURLConnection conn = (HttpURLConnection) url.openConnection()
        int status = conn.getResponseCode()
        if (status != HttpURLConnection.HTTP_OK) {
            if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER) {
                String newLocation = conn.getHeaderField("Location")
                return getFinalLocation(newLocation)
            }
        }
        return address
    }

    /**
     * This method returns the downloadable link for a given videoId of a youtube url from video info
     * @param id String id of the youtube
     * @return url String downloadable link of youtube video
     */
    def getYouTubeDownloadableLink(id) {
        String url = "http://www.youtube.com/get_video_info?video_id=${id}"
        URL obj = new URL(url)
        HttpURLConnection con = (HttpURLConnection) obj.openConnection()
        StringBuffer response = new StringBuffer()
        BufferedReader bi = new BufferedReader(new InputStreamReader(con.getInputStream()))
        String str = ""
        while ((str = bi.readLine()) != null) {
            response.append(str)
        }
        bi.close()
        url = extractUrlEncodedVideos(URLDecoder.decode(response.toString()))
        if (url && url.indexOf(",") >= 0) {
            url = url.substring(0, url.indexOf(","))
        }
        return url
    }

    /**
     * This method parses the video information for a given youtube url and returns video url based on itag
     * @param youtubeurl String youtube url which will be having unparsed data
     * @return videoUrl String video url in the highest quality that is available
     */
    def extractUrlEncodedVideos(String youtubeurl) {
        String[] urlStrings = youtubeurl.split("url=")
        String videoUrl
        for (String urlString : urlStrings) {
            urlString = StringEscapeUtils.unescapeJava(urlString)
            if (urlString.contains(ERROR_CODE)) {
                return FAILURE
            }
            String url = null
            Pattern link = Pattern.compile("([^&]*)&")
            Matcher linkMatch = link.matcher(urlString)
            if (linkMatch.find()) {
                url = linkMatch.group(1)
                url = URLDecoder.decode(url, "UTF-8")
            }
            String itag = null
            link = Pattern.compile("itag=(\\d+)")
            linkMatch = link.matcher(urlString)
            if (linkMatch.find()) {
                itag = linkMatch.group(1)
            }
            if (url && url.contains("=")) {
                if (getItag(url) == "22" || getItag(url) == "18") {
                    videoUrl = url
                }
            }
            if (videoUrl) {
                break
            }
        }
        return videoUrl
    }

    /**
     * This method returns itag for a given url
     * @param url String youtube url
     * @return itag String itag for the url
     */
    def getItag(url) {
        def map = [:]
        url.split("&").each {
            def object = it.split("=")
            map.put(object[0], object[1])
        }
        return map["itag"]
    }

    /**
     * This method returns the video Id from a given Url
     * @param url String youtube url
     * @return videoId String videoId or watchId from the url
     */
    def extractVideoIdFromUrl(String url) {
        def videoIdRegex = ["\\?vi?=([^&]*)", "watch\\?.*v=([^&]*)", "(?:embed|vi?)/([^/?]*)", "^([A-Za-z0-9\\-]*)"]
        String youTubeLinkWithoutProtocolAndDomain = youTubeLinkWithoutProtocolAndDomain(url)
        for (String regex : videoIdRegex) {
            Pattern compiledPattern = Pattern.compile(regex)
            Matcher matcher = compiledPattern.matcher(youTubeLinkWithoutProtocolAndDomain)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        return null
    }

    /**
     * This method returns youtube link by removing protocol and domain
     * @param url String youtube url
     * @return url String url without domain and protocol
     */
    def youTubeLinkWithoutProtocolAndDomain(String url) {
        def youTubeUrlRegEx = "^(https?)?(://)?(www.)?(m.)?((youtube.com)|(youtu.be))/"
        Pattern compiledPattern = Pattern.compile(youTubeUrlRegEx)
        Matcher matcher = compiledPattern.matcher(url)
        if (matcher.find()) {
            return url.replace(matcher.group(), "")
        }
        return url
    }
}
