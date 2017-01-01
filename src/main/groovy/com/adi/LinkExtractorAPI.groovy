package com.adi

import static com.adi.Constants.FAILURE

/**
 * @author adithya
 */
class LinkExtractorAPI {
    static void main(String[] args) {
        if (args) {
            LinkUtils linkUtils = new LinkUtils(args[0])
            def response = linkUtils.getDownloadableLink()
            if (response == FAILURE) {
                println "Url contains content which is private to some channel. Hence cannot get the downlodable link"
            } else {
                println "Downloadable link is : ${response}"
            }
        } else {
            println("Url is mandatory")
        }
    }
}
